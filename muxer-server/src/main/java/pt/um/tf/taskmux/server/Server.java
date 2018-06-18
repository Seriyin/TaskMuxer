package pt.um.tf.taskmux.server;

import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.haslab.ekit.Spread;
import pt.um.tf.taskmuxer.commons.error.DuplicateException;
import pt.um.tf.taskmuxer.commons.error.UnknownClientException;
import pt.um.tf.taskmux.server.messaging.*;
import pt.um.tf.taskmuxer.commons.messaging.*;
import pt.um.tf.taskmuxer.commons.task.DummyResult;
import pt.um.tf.taskmuxer.commons.task.DummyTask;
import pt.um.tf.taskmuxer.commons.task.EmptyResult;
import pt.um.tf.taskmuxer.commons.task.Task;
import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class Server {
    private static Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private final String me;
    private final Serializer sr;
    private final ThreadContext threadContext;
    private final Spread spread;
    private Quality quality;
    private SpreadGroup serverGroup;
    private SpreadGroup mainGroup;
    private SpreadGroup leaderGroup;
    private SpreadGroup privateGroup;
    private final TaskQueues taskQueues;
    private final TrackedMessages trackedMessages;
    private final TrackedGroups trackedGroups;

    public static void main(String[] args) {
        var leader = false;
        if (args.length > 0)
            leader = Boolean.parseBoolean(args[0]);
        Server main;
        try {
            main = new Server(leader);
            main.run();
        } catch (SpreadException | IOException e) {
            LOGGER.error("", e.getMessage());
        }
    }

    private Server(boolean s) throws SpreadException {
        me = "srv-" + UUID.randomUUID();
        sr = new Serializer();
        threadContext = new SingleThreadContext("srv-%d", sr);
        spread = new Spread(me, true);
        if (s) {
            quality = Quality.first();
            trackedMessages = new TrackedMessages();
        } else {
            quality = Quality.initNotReady();
            trackedMessages = null;
        }
        taskQueues = new TaskQueues();
        trackedGroups = new TrackedGroups();
    }

    private void run() throws IOException {
        register();
        threadContext.execute(this::openAndJoin)
                     .thenRun(this::handlers).exceptionally(t -> {
                         LOGGER.error("", t);
                         return null;
                     });
        var bf = new BufferedReader(new InputStreamReader(System.in));
        while(bf.readLine() == null);
        if (quality == Quality.LEADER) {
            spread.leave(mainGroup);
        }
        spread.leave(serverGroup);
        spread.close();
        threadContext.close();
        LOGGER.info("I'm here");
    }

    private void register() {
        sr.register(ClearMessage.class);
        sr.register(DeltaGetMessage.class);
        sr.register(DeltaNewMessage.class);

        sr.register(GetTaskMessage.class);
        sr.register(NewTaskMessage.class);
        sr.register(ResultMessage.class);
        sr.register(TaskMessage.class);
        sr.register(InboundMessage.class);
        sr.register(UrlMessage.class);

        sr.register(DummyTask.class);
        sr.register(DummyResult.class);
        sr.register(DuplicateException.class);
        sr.register(Exception.class);
        sr.register(UnknownClientException.class);
    }

    private void openAndJoin() {
        spread.open();
        serverGroup = spread.join("banks");
        if(quality == Quality.LEADER) {
            mainGroup = spread.join("service");
        }
    }


    private void handlers() {
        LOGGER.info("Handling connection");
        spread.handler(ClearMessage.class, (sm, m) -> handler(m))
              .handler(DeltaCompleteMessage.class, (sm, m) -> handler(m))
              .handler(DeltaGetMessage.class, (sm, m) -> handler(m))
              .handler(DeltaNewMessage.class, (sm, m) -> handler(m))
              .handler(GetTaskMessage.class, this::handler)
              .handler(InboundMessage.class, this::handler)
              .handler(spread.MembershipInfo.class, this::handler)
              .handler(NewTaskMessage.class, this::handler)
              .handler(OutboundMessage.class, this::handler)
              .handler(ResultMessage.class, this::handler)
              .handler(TaskMessage.class, this::handler)
              .handler(UrlMessage.class, this::handler);
    }
    

    /**
     * Part of state transfer. Clear a client's tasks and add back into
     * inbound queue the needed tasks.
     * @param m ClearMessage carries the tasks and client to wipe.
     */
    private void handler(ClearMessage m) {
        switch (quality) {
            case LEADER:
                // I sent it, I know it.
                var s = "Leader sent Clear for : " + m.getClear();
                LOGGER.info(s);
                break;
            case FOLLOWER:
                m.getBackToInbound().forEach(taskQueues::addToFrontInbound);
                taskQueues.clearTasks(m.getClear());
                break;
            case NOT_READY:
                trackedMessages.add(m);
                break;
        }
    }


    /**
     * Part of state transfer. Remove n tasks and assign tasks to client.
     * @param m DeltaCompleteMessage carries the tasks remaining for client.
     */
    private void handler(DeltaCompleteMessage m) {
        switch (quality) {
            case LEADER:
                // I sent it, I know it.
                var s = "Leader sent Delta Complete for : " + m.getSender();
                LOGGER.info( s);
                break;
            case FOLLOWER:
                taskQueues.replaceAfterCompleted(m.getSender(), m.getTasks());
                break;
            case NOT_READY:
                trackedMessages.add(m);
                break;
        }
    }


    /**
     * Part of state transfer. Remove n tasks and assign tasks to client.
     * @param m DeltaGetMessage carries the tasks and number to wipe.
     */
    private void handler(DeltaGetMessage m) {
        switch (quality) {
            case LEADER:
                // I sent it, I know it.
                var s = "Leader sent Delta Get for : " + m.getSender();
                LOGGER.info( s);
                break;
            case FOLLOWER:
                taskQueues.removeAndSet(m.getIn(), m.getSender(), m.getCount());
                break;
            case NOT_READY:
                trackedMessages.add(m);
                break;
        }
    }

    /**
     * Part of state transfer. Add n tasks and to inbound.
     * @param m DeltaGetMessage carries the tasks and number to add.
     */
    private void handler(DeltaNewMessage m) {
        switch (quality) {
            case LEADER:
                // I sent it, I know it.
                var s = "Leader sent Delta New for : " + m.getSender();
                LOGGER.info( s);
                break;
            case FOLLOWER:
                taskQueues.addAllToBackInbound(m.getIn());
                break;
            case NOT_READY:
                trackedMessages.add(m);
                break;
        }
    }

    private void handler(SpreadMessage sm, GetTaskMessage m) {
        switch (quality) {
            case LEADER:
                //Send to outbound
                var result = taskQueues.sendToOutbound(sm.getSender().toString());
                var spm = new SpreadMessage();
                spm.addGroup(sm.getSender());
                spm.setSafe();
                if(result.completedSuccessfully()) {
                    taskQueues.getOutbound(sm.getSender().toString())
                              .ifPresentOrElse(out -> sendToEveryone(sm, out),
                                               () -> LOGGER.error("Private group wasn't present"));
                    //TODO: ACKs first.
                    var tm = new TaskMessage(result.completeWithResult());
                    spread.multicast(spm, tm);
                }
                else {
                    var r = new EmptyResult(result.completedSuccessfully(),
                                            result.completedWithException());
                    var em = new ResultMessage(r);
                    spread.multicast(spm, em);
                }
                break;
            case FOLLOWER:
                //Stay still, no sudden movements.
            case NOT_READY:
                var s = "Non-leader got client-specific get task message!!";
                LOGGER.error(s);
                break;
        }
    }


    private void handler(SpreadMessage sm, InboundMessage im) {
        switch (quality) {
            case LEADER :
                if (sm.getSender().equals(leaderGroup)) {
                    LOGGER.info( "Leader got update");
                    sendNext(im);
                }
                else {
                    LOGGER.error("Leader got update from follower!!");
                }
                break;
            case FOLLOWER:
                trackUpdates(im);
                break;
            case NOT_READY:
                if (im.getSequence()==0) {
                    leaderGroup = sm.getSender();
                }
                if(im.getReceiver().equals(privateGroup.toString())) {
                    LOGGER.error("Inbound has wrong leader group");
                    addUpdate(im);
                }
                break;
        }
    }



    private void sendNext(InboundMessage im) {
        var spm = new SpreadMessage();
        spm.addGroup(im.getReceiver());
        spm.setSafe();
        OutboundMessage om;
        if(taskQueues.outboundIsEmpty()) {
            Map.Entry<String, Map<URL, Task>> it = taskQueues.getOutboundIterator(im.getReceiver()).next();
            om = new OutboundMessage(it.getValue().values(),
                                     false,
                                     im.getSequence()+1,
                                     im.getReceiver(),
                                     it.getKey());
        }
        else {
            om = new OutboundMessage(null,
                                     false,
                                     im.getSequence()+1,
                                     im.getReceiver(),
                                     null);
        }
        spread.multicast(spm, om);
        LOGGER.info("Partial send to :" + om.getReceiver() + " number " + om.getSequence());
    }

    private void trackUpdates(InboundMessage im) {
        if(im.hasMore()) {
            trackedGroups.registerTracked(trackedGroups.getKnown(im.getReceiver()));
        }
        else {
            trackedGroups.purgeTracked(im.getReceiver());
        }
    }

    private void addUpdate(InboundMessage im) {
        if (im.hasMore()) {
            //Finer grain deltas aren't implemented yet.
            LOGGER.error("Inbounds should be entire list!!");
        }
        else {
            if(!im.getReceiver().equals(privateGroup.toString())) {
                LOGGER.info("Inbound for : " + im.getReceiver());
            }
            else {
                taskQueues.addAllToBackInbound(im.getTask());
                LOGGER.info("Got Inbound : " + im.getReceiver());
            }
        }
    }

    private void sendToEveryone(SpreadMessage sm, Collection<Task> out) {
        var spm = new SpreadMessage();
        spm.addGroup(sm.getSender());
        spm.setSafe();
        var dgm = new DeltaGetMessage(sm.getSender().toString(),
                                      out,
                                      1);
        spread.multicast(spm, dgm);
    }


    //Handle membership info.
    private void handler(SpreadMessage sm, MembershipInfo m) {
        switch (quality) {
            case LEADER:
                if (m.isCausedByLeave()) {
                    leaderOnLeave(m);

                }
                //Assumption that joined is always a singular group.
                else if (m.isCausedByJoin()) {
                    if(m.getJoined().equals(spread.getPrivateGroup())) {
                        privateGroup = spread.getPrivateGroup();
                        leaderGroup = privateGroup;
                        trackedGroups.registerKnown(privateGroup);
                    }
                    else {
                        leaderOnJoin(m);
                    }
                }
            case FOLLOWER:
                if (m.isCausedByLeave()) {
                    followerOnLeave(m);
                }
                else if (m.isCausedByJoin()) {
                    followerOnJoin(m);
                }
                break;
            case NOT_READY:
                //Check if it's me
                if (m.isCausedByLeave()) {
                    if(leaderGroup != null &&
                       m.getLeft().equals(leaderGroup)) {
                        //Clear everything. We can't guarantee we're up to date.
                        trackedMessages.wipe();
                        taskQueues.wipe();
                    }
                    trackedGroups.purgeKnown(m.getLeft());
                }
                else if (m.isCausedByJoin()) {
                    if(m.getJoined().equals(spread.getPrivateGroup())) {
                        privateGroup = spread.getPrivateGroup();
                        //Assumedly i am a member of the group.
                        trackedGroups.registerKnown(Arrays.asList(m.getMembers()));
                    }
                    trackedGroups.registerKnown(m.getJoined());
                }
                break;
        }
    }


    /**
     * Kick all tasks assigned to a leaving client back into the inbound queue.
     *
     * Wipe the outbound queue of all client info.
     * @param m Message with leaving client.
     */
    private void leaderOnLeave(MembershipInfo m) {
        if(m.getLeft().toString().substring(1,5).equals("cli-")) {
            var mem = m.getLeft();
            Optional<ArrayList<Task>> backToInbound = taskQueues.backToInbound(mem.toString());
            backToInbound.ifPresent(l -> sendClearMessage(l, mem));
        }
    }

    private void sendClearMessage(List<Task> backToInbound, SpreadGroup left) {
        var spm = new SpreadMessage();
        spm.setSafe();
        spm.addGroup(serverGroup);
        var cm = new ClearMessage(backToInbound, left.toString());
        spread.multicast(spm, cm);
    }


    private void leaderOnJoin(MembershipInfo m) {
        if(m.getGroup().equals(serverGroup)) {
            var spm = new SpreadMessage();
            spm.addGroup(serverGroup);
            spm.setSafe();
            trackedGroups.registerTracked(m.getJoined());
            InboundMessage im;
            if (taskQueues.inboundIsEmpty()) {
                im = new InboundMessage(null,
                                        false,
                                        0,
                                        m.getJoined().toString());
            }
            else {
                im = new InboundMessage(taskQueues.getInbound(),
                                        false,
                                        0,
                                        m.getJoined().toString());
            }
            spread.multicast(spm, im);
            LOGGER.info("Partial send to :" + im.getReceiver() + " number " + im.getSequence());
        }
        else if (m.getGroup().equals(mainGroup)) {
            LOGGER.info("Client joined: " + m.getJoined());
        }
        else {
            LOGGER.error("Unknown group: " + m.getJoined());
        }
    }



    private void followerOnJoin(MembershipInfo m) {
        trackedGroups.registerTracked(m.getJoined());
        LOGGER.info( "New server joined : " + m.getJoined());
    }


    private void followerOnLeave(MembershipInfo m) {
        //Assumption that left group is always size 1.
        if (leaderGroup == m.getLeft()) {
            var smallest = trackedGroups.getMinKnown();
            if(privateGroup.toString()
                           .compareTo(smallest.toString()) == 0) {
                quality = quality.rise();
                mainGroup = spread.join("service");
                //Leaders can only fail crash
            }
            else {
                trackedGroups.purgeKnown(m.getLeft());
                //smallest is now a Leader, no longer a follower.
                leaderGroup = smallest;
            }
        }
    }


    private void handler(SpreadMessage sm, NewTaskMessage m) {
        switch (quality) {
            case LEADER:
                taskQueues.sendToInbound(m.getTask());
                var spm = new SpreadMessage();
                spm.addGroup(serverGroup);
                spm.setSafe();
                var dnm = new DeltaNewMessage(sm.getSender().toString(),
                                              m.getTask());
                spread.multicast(spm, dnm);
                break;
            case FOLLOWER:
            case NOT_READY:
                var s = "Non-leader got client-specific new task message!!";
                LOGGER.error(s);
                break;
        }
    }

    private void handler(SpreadMessage sm, OutboundMessage om) {
        switch (quality) {
            case LEADER:
                var spm = new SpreadMessage();
                spm.addGroup(serverGroup);
                spm.setSafe();
                OutboundMessage nom;
                if (om.hasMore()) {
                    Iterator<Map.Entry<String, Map<URL, Task>>> out = taskQueues.getOutboundIterator(om.getReceiver());
                    if (out.hasNext()) {
                        Map.Entry<String, Map<URL, Task>> it = out.next();
                        nom = new OutboundMessage(it.getValue().values(),
                                                  true,
                                                  om.getSequence()+1,
                                                  om.getReceiver(),
                                                  it.getKey());
                    }
                    else {
                        nom = new OutboundMessage(null,
                                                  false,
                                                  om.getSequence()+1,
                                                  om.getReceiver(),
                                                  null);
                    }
                    spread.multicast(spm, nom);
                    LOGGER.info("Partial send to :" + om.getReceiver() + " number " + nom.getSequence());
                } else {
                    LOGGER.info("Is up-to-date : " + om.getReceiver());
                }
                break;
            case FOLLOWER:
                if (!om.hasMore()) {
                    trackedGroups.registerKnown(privateGroup);
                }
                break;
            case NOT_READY:
                if(om.getTask() != null) {
                    taskQueues.removeAndSet(om.getTask(), om.getSender(), 0);
                }
                if(!om.hasMore()) {
                    quality = quality.follow();
                    trackedMessages.handleAll(this::handler);
                }
                break;
        }
    }


    private void handler(SpreadMessage sm, ResultMessage m) {
        switch (quality) {
            case LEADER:
                LOGGER.error("Duplicate exception!!");
                break;
            case FOLLOWER:
            case NOT_READY:
                LOGGER.error("Non-leader got client-specific result message!!");
                break;
        }
    }

    private void handler(SpreadMessage sm, TaskMessage m) {
        switch (quality) {
            case LEADER:
                //I know it, I sent it. Should reach only the client.
                LOGGER.error("Got Task message back : " + sm.getSender());
                break;
            case FOLLOWER:
            case NOT_READY:
                LOGGER.error("Non-leader got client-specific task message!!");
                break;
        }
    }


    private void handler(SpreadMessage sm, UrlMessage m) {
        switch (quality) {
            case LEADER:
                var out = taskQueues.purgeOutbound(sm.getSender().toString(),
                                                   m.getUrl());
                out.ifPresent(tasks -> {
                    var spm = new SpreadMessage();
                    spm.setSafe();
                    spm.addGroup(serverGroup);
                    var dcm = new DeltaCompleteMessage(sm.getSender().toString(),
                                                       tasks);
                    spread.multicast(spm, dcm);
                });
                break;
            case FOLLOWER:
            case NOT_READY:
                var s = "Non-leader got client-specific URL message!!";
                LOGGER.error(s);
                break;
        }
    }

    private void handler(StateMessage sm) {
        if(sm instanceof ClearMessage) {
            handler((ClearMessage) sm);
        }
        else if(sm instanceof DeltaCompleteMessage) {
            handler((DeltaCompleteMessage) sm);
        }
        else if(sm instanceof DeltaGetMessage) {
            handler((DeltaGetMessage) sm);
        }
        else if(sm instanceof DeltaNewMessage) {
            handler((DeltaNewMessage) sm);
        }
        else {
            LOGGER.error("Unexpected unrecognized StateMessage!!");
        }
    }


}