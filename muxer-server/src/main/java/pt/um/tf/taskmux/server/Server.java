package pt.um.tf.taskmux.server;

import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.haslab.ekit.Spread;
import pt.um.tf.taskmux.commons.error.DuplicateException;
import pt.um.tf.taskmux.commons.error.MissingExecutorException;
import pt.um.tf.taskmux.commons.error.UnknownClientException;
import pt.um.tf.taskmux.commons.messaging.*;
import pt.um.tf.taskmux.server.messaging.*;
import pt.um.tf.taskmux.commons.task.DummyResult;
import pt.um.tf.taskmux.commons.task.DummyTask;
import pt.um.tf.taskmux.commons.task.EmptyResult;
import pt.um.tf.taskmux.commons.task.Task;
import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
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
        } catch (SpreadException e) {
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
            trackedMessages = null;
        } else {
            quality = Quality.initNotReady();
            trackedMessages = new TrackedMessages();
        }
        taskQueues = new TaskQueues();
        trackedGroups = new TrackedGroups();
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
            if (quality == Quality.LEADER) {
                spread.leave(mainGroup);
            }
            spread.leave(serverGroup);
            spread.close();
            threadContext.close();
            LOGGER.info("I'm here");
        }
    }

    private void register() {
        sr.register(ClearMessage.class);
        sr.register(DeltaCompleteMessage.class);
        sr.register(DeltaGetMessage.class);
        sr.register(DeltaNewMessage.class);
        sr.register(InboundMessage.class);
        sr.register(OutboundMessage.class);

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
        spread.join("servers");
        if(quality == Quality.LEADER) {
            spread.join("service");
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
              .handler(URIMessage.class, this::handler);
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
                var s = "Leader sent Clear of : " + m.getClear();
                LOGGER.info(s);
                break;
            case FOLLOWER:
                m.getBackToInbound().forEach(taskQueues::addToFrontInbound);
                taskQueues.clearTasks(m.getClear());
                LOGGER.info("Got clear of : " + m.getClear());
                break;
            case NOT_READY:
                LOGGER.info("Tracking clear of : " + m.getClear());
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
                LOGGER.info("Got delta complete from : " + m.getSender());
                break;
            case NOT_READY:
                LOGGER.info("Tracking delta complete from : " + m.getSender());
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
                taskQueues.replaceAfterGet(m.getIn(), m.getSender(), m.getCount());
                LOGGER.info("Got delta get from : " + m.getSender());
                break;
            case NOT_READY:
                trackedMessages.add(m);
                LOGGER.info("Tracking delta get from : " + m.getSender());
                break;
        }
    }

    /**
     * Part of state transfer. Add n tasks to inbound.
     * @param m DeltaNewMessage carries the tasks to add to inbound.
     */
    private void handler(DeltaNewMessage m) {
        switch (quality) {
            case LEADER:
                // I sent it, I know it.
                var s = "Leader sent Delta New from : " + m.getSender();
                LOGGER.info(s);
                break;
            case FOLLOWER:
                taskQueues.addAllToBackInbound(m.getIn());
                LOGGER.info("Got delta new from : " + m.getSender());
                break;
            case NOT_READY:
                trackedMessages.add(m);
                LOGGER.info("Tracking delta new from : " + m.getSender());
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


    private void sendToEveryone(SpreadMessage sm, Collection<Task> out) {
        if (!out.isEmpty()) {
            var spm = new SpreadMessage();
            spm.addGroup(serverGroup);
            spm.setSafe();
            var dgm = new DeltaGetMessage(sm.getSender().toString(),
                                          out,
                                          1);
            spread.multicast(spm, dgm);
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
                break;
            case NOT_READY:
                if (im.getSequence()==0) {
                    leaderGroup = sm.getSender();
                }
                if(!sm.getSender().equals(leaderGroup)) {
                    LOGGER.error("Inbound has wrong leader group");
                }
                addUpdate(im);
                break;
        }
    }



    private void sendNext(InboundMessage im) {
        if(!im.hasMore()) {
            var spm = new SpreadMessage();
            spm.addGroup(im.getReceiver());
            spm.setSafe();
            OutboundMessage om;
            if(!taskQueues.outboundIsEmpty()) {
                var it = taskQueues.getOutboundIterator(im.getReceiver()).next();
                om = new OutboundMessage(new ArrayList<>(it.getValue().values()),
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
        } else {
            LOGGER.error("Partial send of Inbound not implemented");
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
                if(im.getTask() != null) {
                    taskQueues.addAllToBackInbound(im.getTask());
                }
                LOGGER.info("Got Inbound : " + im.getReceiver());
            }
        }
    }

    //Handle membership info.
    private void handler(SpreadMessage sm, MembershipInfo m) {
        switch (quality) {
            case LEADER:
                if (m.isCausedByLeave()) {
                    leaderOnLeave(sm, m);

                }
                //Assumption that joined is always a singular group.
                else if (m.isCausedByJoin()) {
                    if(m.getJoined().equals(spread.getPrivateGroup())) {
                        leaderSelfJoin(sm, m);
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
                        serverGroup = sm.getSender();
                        //Assumedly i am a member of the group.
                        trackedGroups.registerKnown(Arrays.asList(m.getMembers()));
                    } else {
                        trackedGroups.registerTracked(m.getJoined());
                    }
                }
                break;
        }
    }

    private void leaderSelfJoin(SpreadMessage sm, MembershipInfo m) {
        if(privateGroup == null) {
            privateGroup = spread.getPrivateGroup();
            if(quality == Quality.LEADER) {
                leaderGroup = privateGroup;
            }
        }
        if(sm.getSender().toString().equals("servers")) {
            trackedGroups.registerKnown(privateGroup);
            serverGroup = sm.getSender();
        }
        else if (sm.getSender().toString().equals("service")) {
            mainGroup = sm.getSender();
        }
    }


    /**
     * Kick all tasks assigned to a leaving client back into the inbound queue.
     *
     * Wipe the outbound queue of all client info.
     * @param m Message with leaving client.
     */
    private void leaderOnLeave(SpreadMessage sm, MembershipInfo m) {
        if(sm.getSender().equals(mainGroup)) {
            var mem = m.getLeft();
            Optional<ArrayList<Task>> backToInbound = taskQueues.backToInbound(mem.toString());
            backToInbound.ifPresent(l -> sendClearMessage(l, mem));
        }
    }

    private void sendClearMessage(List<Task> backToInbound, SpreadGroup left) {
        if(!backToInbound.isEmpty()) {
            var spm = new SpreadMessage();
            spm.setSafe();
            spm.addGroup(serverGroup);
            var cm = new ClearMessage(backToInbound, left.toString());
            spread.multicast(spm, cm);
        }
    }


    private void leaderOnJoin(MembershipInfo m) {
        if(m.getGroup().equals(serverGroup)) {
            trackedGroups.registerTracked(m.getJoined());
            sendInbound(m.getJoined().toString());
        }
        else if (m.getGroup().equals(mainGroup)) {
            LOGGER.info("Client joined: " + m.getJoined());
        }
        else {
            LOGGER.error("Unknown group: " + m.getJoined());
        }
    }

    private void sendInbound(String s) {
        var spm = new SpreadMessage();
        spm.addGroup(serverGroup);
        spm.setSafe();
        InboundMessage im;
        if (taskQueues.inboundIsEmpty()) {
            im = new InboundMessage(null, false, 0, s);
        }
        else {
            im = new InboundMessage(taskQueues.getInbound(), false, 0, s);
        }
        spread.multicast(spm, im);
        LOGGER.info("Partial send to :" + im.getReceiver() + " number " + im.getSequence());
    }


    private void followerOnJoin(MembershipInfo m) {
        trackedGroups.registerTracked(m.getJoined());
        LOGGER.info( "New server joined : " + m.getJoined());
    }


    private void followerOnLeave(MembershipInfo m) {
        //Assumption that left group is always size 1.
        if (leaderGroup.equals(m.getLeft())) {
            trackedGroups.purgeKnown(m.getLeft());
            var smallest = trackedGroups.getMinKnown();
            if(privateGroup.toString()
                           .compareTo(smallest.toString()) == 0) {
                quality = quality.rise();
                mainGroup = spread.join("service");
                LOGGER.info("Rose to leadership :" + privateGroup);
                leaderGroup = privateGroup;
                //Resend inbounds to everyone still not up-to-date
                trackedGroups.getTracked().forEach(this::sendInbound);
                //Leaders can only fail crash
            }
            else {
                //smallest is now a Leader, no longer a follower.
                leaderGroup = smallest;
                LOGGER.info("New leader :" + smallest);
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
                    var out = taskQueues.getOutboundIterator(om.getReceiver());
                    if (out.hasNext()) {
                        var it = out.next();
                        nom = new OutboundMessage(new ArrayList<>(it.getValue().values()),
                                                  true,
                                                  om.getSequence()+1,
                                                  om.getReceiver(),
                                                  it.getKey());
                    }
                    else {
                        taskQueues.purgeIterator(om.getReceiver());
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
                LOGGER.info("Outbound for : " + om.getReceiver());
                if (!om.hasMore()) {
                    trackedGroups.registerKnown(trackedGroups.getTracked(om.getReceiver()));
                    trackedGroups.purgeTracked(om.getReceiver());
                }
                break;
            case NOT_READY:
                if(!om.getReceiver().equals(privateGroup.toString())) {
                    LOGGER.info("Outbound for : " + om.getReceiver());
                    if(!om.hasMore()) {
                        trackedGroups.registerKnown(trackedGroups.getTracked(om.getReceiver()));
                        trackedGroups.purgeTracked(om.getReceiver());
                    }
                }
                else {
                    if(om.getTask() != null && !om.getTask().isEmpty()) {
                        taskQueues.replaceAfterGet(om.getTask(), om.getSender(), 0);
                        LOGGER.info("Primed set : " + om.getSequence());
                    }
                    if(!om.hasMore()) {
                        quality = quality.follow();
                        trackedMessages.handleAll(this::handler);
                        trackedMessages.wipe();
                        LOGGER.info("Rose to follower : " + privateGroup);
                    }
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


    private void handler(SpreadMessage sm, URIMessage m) {
        switch (quality) {
            case LEADER:
                var out = taskQueues.purgeOutbound(sm.getSender().toString(),
                                                   m.getURI());
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