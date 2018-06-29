package pt.um.tf.taskmux.client;

import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.haslab.ekit.Spread;
import pt.um.tf.taskmux.commons.URIGenerator;
import pt.um.tf.taskmux.commons.error.DuplicateException;
import pt.um.tf.taskmux.commons.error.MissingExecutorException;
import pt.um.tf.taskmux.commons.error.NoAssignableTasksException;
import pt.um.tf.taskmux.commons.error.UnknownClientException;
import pt.um.tf.taskmux.commons.messaging.*;
import pt.um.tf.taskmux.commons.task.DummyResult;
import pt.um.tf.taskmux.commons.task.DummyTask;
import pt.um.tf.taskmux.commons.task.EmptyResult;
import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class Client {
    private static Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private final String me;
    private final Serializer sr;
    private final ThreadContext threadContext;
    private final Spread spread;
    private final Queue<CommonMessage> outbound;
    private SpreadGroup mainGroup;
    private SpreadGroup leaderGroup;
    private SpreadGroup privateGroup;
    private TaskRunner runner;
    private Timer timer;

    public static void main(String[] args) {
        try {
            var main = new Client();
            main.run();
        } catch (SpreadException e) {
            LOGGER.error("", e.getMessage());
        }
    }

    private Client() throws SpreadException {
        me = "cli-" + UUID.randomUUID();
        sr = new Serializer();
        threadContext = new SingleThreadContext("srv-%d", sr);
        spread = new Spread(me, true);
        runner = new TaskRunner(ForkJoinPool.commonPool(),
                                ForkJoinPool.getCommonPoolParallelism(),
                                spread);
        outbound = new ArrayDeque<>();
    }

    private void run() {
        register();
        threadContext.execute(this::openAndJoin)
                     .thenRun(this::handlers).exceptionally(t -> {
                         LOGGER.error("", t);
                         return null;
                     });
        var bf = new BufferedReader(new InputStreamReader(System.in));
        try{
            while(bf.readLine() == null);
        }
        catch (IOException e) {
            LOGGER.error("", e);
        }
        finally{
            if(mainGroup != null) {
                spread.leave(mainGroup);
            }
            if(timer != null) {
                timer.cancel();
                timer.purge();
            }
            spread.close();
            threadContext.close();
            LOGGER.info("I'm here");
        }
    }

    private void register() {
        sr.register(GetTaskMessage.class);
        sr.register(NewTaskMessage.class);
        sr.register(ResultMessage.class);
        sr.register(TaskMessage.class);
        sr.register(URIMessage.class);

        sr.register(DummyTask.class);
        sr.register(DummyResult.class);
        sr.register(EmptyResult.class);

        sr.register(DuplicateException.class);
        sr.register(Exception.class);
        sr.register(MissingExecutorException.class);
        sr.register(UnknownClientException.class);

        sr.register(URI.class);
    }

    private void openAndJoin() {
        spread.open();
        spread.join("service");
    }


    private void handlers() {
        LOGGER.info("Handling connection");
        spread.handler(GetTaskMessage.class, this::handler)
              .handler(spread.MembershipInfo.class, this::handler)
              .handler(NewTaskMessage.class, this::handler)
              .handler(ResultMessage.class, this::handler)
              .handler(TaskMessage.class, this::handler)
              .handler(URIMessage.class, this::handler);
    }


    private void handler(SpreadMessage sm, GetTaskMessage gm) {
        if(leaderGroup == null && sm.getSender().equals(privateGroup)) {
            var s = "GetTaskMessage added to outbound, leader unavailable.";
            LOGGER.info(s);
            outbound.offer(gm);
        }
        else {
            LOGGER.info("Successful dispatch of task get");
        }
    }

    private void handler(SpreadMessage sm, MembershipInfo m) {
        if(m.isCausedByLeave()) {
            if (m.getLeft().equals(leaderGroup)) {
                LOGGER.info("Leader left");
                leaderGroup = null;
            }
            LOGGER.info("Left : " + m.getLeft());
        }
        else if (m.isCausedByJoin()) {
            if(m.getJoined().equals(spread.getPrivateGroup())) {
                //I just joined.
                privateGroup = spread.getPrivateGroup();
                mainGroup = sm.getSender();
                leaderGroup = Arrays.stream(m.getMembers())
                                    .filter(s -> !s.toString().substring(1,5).equals("srv-"))
                                    .findAny().orElse(null);
                runner.setMaingroup(mainGroup);
                initTimer();
            }
            if(m.getGroup().toString().equals("service") &&
               m.getJoined().toString().substring(1,5).equals("srv-")) {
                LOGGER.info("New leader : " + m.getJoined());
                leaderGroup = m.getJoined();
                if(!outbound.isEmpty()) {
                    outbound.forEach(this::resend);
                    outbound.clear();
                }
            }
            else {
                LOGGER.info("Join : " + m.getJoined());
            }
        }
    }

    private void initTimer() {
        timer = new Timer();
        var u = new URIGenerator(me);
        var random = new Random(me.chars().sum());
        var tg = new TaskGenerator(spread, mainGroup, u);
        var ta = new TaskAssigner(spread, mainGroup, runner);
        timer.scheduleAtFixedRate(tg,0, random.nextInt(30000)+10000);
        timer.scheduleAtFixedRate(ta, 0, random.nextInt(10000)+2000);
    }

    private void resend(CommonMessage cm) {
        var spm = new SpreadMessage();
        spm.addGroup(mainGroup);
        spm.setSafe();
        spread.multicast(spm, cm);
    }

    private void handler(SpreadMessage sm, NewTaskMessage nm) {
        if(leaderGroup == null && sm.getSender().equals(privateGroup)) {
            var s = "NewTaskMessage added to outbound, leader unavailable : " + sm.getSender();
            LOGGER.info(s);
            outbound.offer(nm);
        }
        else {
            LOGGER.info("New task posted by : " + sm.getSender());
        }
    }

    private void handler(SpreadMessage sm, ResultMessage rm) {
        var err = rm.getResult().completedWithException();
        if (err instanceof NoAssignableTasksException) {
            LOGGER.info("", err);
        }
        else {
            LOGGER.error("", err);
        }
    }

    private void handler(SpreadMessage sm, TaskMessage tm) {
        if (Arrays.stream(sm.getGroups())
                  .allMatch(sg -> sg.equals(privateGroup))) {
            LOGGER.info("Got new Task :" + privateGroup);
            if (!runner.runTask(tm.getTask())) {
                LOGGER.error("Past barrier for multi-threading");
            }
        }
        else {
            var gps = Arrays.stream(sm.getGroups())
                            .map(SpreadGroup::toString)
                            .collect(Collectors.joining());
            LOGGER.error("Got new Task destined to :" + gps);
        }
    }

    private void handler(SpreadMessage sm, URIMessage um) {
        if(leaderGroup == null && sm.getSender().equals(privateGroup)) {
            var s = "URLMessage added to outbound, leader unavailable : " + um.getURI();
            LOGGER.info(s);
            outbound.offer(um);
        }
        else {
            LOGGER.info("Successful dispatch of task completion");
        }
    }

}
