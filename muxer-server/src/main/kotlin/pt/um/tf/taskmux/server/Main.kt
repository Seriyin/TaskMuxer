package pt.um.tf.taskmux.server

import io.atomix.catalyst.concurrent.SingleThreadContext
import io.atomix.catalyst.serializer.Serializer
import mu.KLogging
import pt.haslab.ekit.Spread
import pt.um.tf.taskmux.commons.error.DuplicateException
import pt.um.tf.taskmux.commons.error.MissingExecutorException
import pt.um.tf.taskmux.commons.error.UnknownClientException
import pt.um.tf.taskmux.commons.messaging.*
import pt.um.tf.taskmux.commons.task.DummyResult
import pt.um.tf.taskmux.commons.task.DummyTask
import pt.um.tf.taskmux.commons.task.EmptyResult
import pt.um.tf.taskmux.commons.task.Task
import pt.um.tf.taskmux.server.messaging.*
import spread.MembershipInfo
import spread.SpreadGroup
import spread.SpreadMessage
import java.net.URI
import java.util.*

fun main(args : Array<String>) {
    if (args.isNotEmpty()) {
        Server(args[0].toBoolean())
    } else {
        Server(false)
    }.run()
}

class Server(s : Boolean) {
    companion object : KLogging()
    private val me = "srv-${UUID.randomUUID()}"
    private val sr = Serializer()
    private val threadContext = SingleThreadContext("srv-%d", sr)
    private val spread = Spread(me, true)
    private var quality = if (s) Quality.first() else Quality.initNotReady()
    private val taskQueues = TaskQueues()
    private val trackedMessages = if (s) TrackedMessages() else null
    private val trackedGroups = TrackedGroups()
    private lateinit var serverGroup : SpreadGroup
    private lateinit var mainGroup : SpreadGroup
    private var leaderGroup : SpreadGroup? = null

    internal fun run() {
        register()
        threadContext.execute(this::openAndJoin)
                     .thenRun(this::handlers)
                     .exceptionally {
                        logger.error("", it)
                        return@exceptionally null
                     }
        while(readLine() == null);
        if (quality == Quality.LEADER) {
            spread.leave(mainGroup)
        }
        spread.leave(serverGroup)
        spread.close()
        threadContext.close()
        logger.info("I'm here")
    }

    private fun register() {
        sr.register(ClearMessage::class.java)
        sr.register(DeltaCompleteMessage::class.java)
        sr.register(DeltaGetMessage::class.java)
        sr.register(DeltaNewMessage::class.java)
        sr.register(InboundMessage::class.java)
        sr.register(OutboundMessage::class.java)

        sr.register(GetTaskMessage::class.java)
        sr.register(NewTaskMessage::class.java)
        sr.register(ResultMessage::class.java)
        sr.register(TaskMessage::class.java)
        sr.register(URIMessage::class.java)

        sr.register(DummyTask::class.java)
        sr.register(DummyResult::class.java)
        sr.register(EmptyResult::class.java)

        sr.register(DuplicateException::class.java)
        sr.register(Exception::class.java)
        sr.register(MissingExecutorException::class.java)
        sr.register(UnknownClientException::class.java)

        sr.register(URI::class.java)
    }

    private fun openAndJoin() {
        spread.open()
        spread.join("servers")
        if(quality == Quality.LEADER) {
            spread.join("service")
        }
    }


    private fun handlers() {
        logger.info("Handling connection")
        spread.handler(ClearMessage::class.java) { _, m -> handler(m)}
              .handler(DeltaCompleteMessage::class.java) { _, m -> handler(m)}
              .handler(DeltaGetMessage::class.java) { _, m -> handler(m)}
              .handler(DeltaNewMessage::class.java) { _, m -> handler(m)}
              .handler(GetTaskMessage::class.java, this::handler)
              .handler(InboundMessage::class.java, this::handler)
              .handler(MembershipInfo::class.java, this::handler)
              .handler(NewTaskMessage::class.java, this::handler)
              .handler(OutboundMessage::class.java, this::handler)
              .handler(ResultMessage::class.java, this::handler)
              .handler(TaskMessage::class.java, this::handler)
              .handler(URIMessage::class.java, this::handler)
    }
    

    /**
     * Part of state transfer. Clear a client's tasks and add back into
     * inbound queue the needed tasks.
     * @param m ClearMessage carries the tasks and client to wipe.
     */
    private fun handler(m : ClearMessage) {
        when (quality) {
            Quality.LEADER -> {
                val s = "Leader sent Clear of : ${m.clear}"
                logger.info(s)
            }
            Quality.FOLLOWER -> {
                m.backToInbound.forEach(taskQueues::addToFrontInbound)
                taskQueues.clearTasks(m.clear)
                logger.info("Got clear of : ${m.clear}")
            }
            Quality.NOT_READY -> {
                logger.info("Tracking clear of : ${m.clear}")
                trackedMessages?.add(m)
            }
        }
    }


    /**
     * Part of state transfer. Remove n tasks and assign tasks to client.
     * @param m DeltaCompleteMessage carries the tasks remaining for client.
     */
    private fun handler(m : DeltaCompleteMessage) {
        when (quality) {
            Quality.LEADER -> {
                // I sent it, I know it.
                val s = "Leader sent Delta Complete for : ${m.sender}"
                logger.info(s)
            }
            Quality.FOLLOWER -> {
                taskQueues.replaceAfterCompleted(m.sender, m.tasks)
                logger.info("Got delta complete from : ${m.sender}")
            }
            Quality.NOT_READY -> {
                logger.info("Tracking delta complete from : ${m.sender}")
                trackedMessages?.add(m)
            }
        }
    }


    /**
     * Part of state transfer. Remove n tasks and assign tasks to client.
     * @param m DeltaGetMessage carries the tasks and number to wipe.
     */
    private fun handler(m : DeltaGetMessage) {
        when (quality) {
            Quality.LEADER -> {
                // I sent it, I know it.
                val s = "Leader sent Delta Get for : ${m.sender}"
                logger.info(s)
            }
            Quality.FOLLOWER -> {
                taskQueues.replaceAfterGet(m.inSet, m.sender, m.count)
                logger.info("Got delta get from : ${m.sender}")
            }
            Quality.NOT_READY -> {
                logger.info("Tracking delta get from : ${m.sender}")
                trackedMessages?.add(m)
            }
        }
    }

    /**
     * Part of state transfer. Add n tasks to inbound.
     * @param m DeltaNewMessage carries the tasks to add to inbound.
     */
    private fun handler(m : DeltaNewMessage) {
        when (quality) {
            Quality.LEADER -> {
                // I sent it, I know it.
                val s = "Leader sent Delta New from : ${m.sender}"
                logger.info(s)
            }
            Quality.FOLLOWER -> {
                taskQueues.addAllToBackInbound(m.inSet)
                logger.info("Got delta new from : ${m.sender}")
            }
            Quality.NOT_READY -> {
                logger.info("Tracking delta new from : ${m.sender}")
                trackedMessages?.add(m)
            }
        }
    }

    private fun handler(sm : SpreadMessage, m : GetTaskMessage) {
        when (quality) {
            Quality.LEADER -> {
                //Send to outbound
                val result = taskQueues.sendToOutbound(sm.sender.toString())
                val spm = SpreadMessage()
                spm.addGroup(sm.sender)
                spm.setSafe()
                if(result.completedSuccessfully()) {
                    val out = taskQueues.getOutbound(sm.sender.toString())
                    if (out.isNotEmpty()) {
                        sendToEveryone(sm, out)
                    }
                    else {
                        logger.error("Private group wasn't present")
                    }
                    //TODO: ACKs first.
                    val tm = TaskMessage(result.completeWithResult())
                    spread.multicast(spm, tm)
                }
                else {
                    val r = EmptyResult(result.completedWithException())
                    val em = ResultMessage(r)
                    spread.multicast(spm, em)
                }
            }
            Quality.FOLLOWER, Quality.NOT_READY -> {
                //Stay still, no sudden movements.
                val s = "Non-leader got client-specific get task message!!"
                logger.error(s)
            }
        }
    }


    private fun sendToEveryone(sm : SpreadMessage, out : Set<Task<out Any>>) {
            val spm = SpreadMessage()
            spm.addGroup(serverGroup)
            spm.setSafe()
            val dgm = DeltaGetMessage(sm.sender.toString(),
                                      out,
                                     1)
            spread.multicast(spm, dgm)
    }




    private fun handler(sm : SpreadMessage, im : InboundMessage) {
        when (quality) {
            Quality.LEADER -> {
                if (sm.sender == leaderGroup) {
                    logger.info( "Leader got update")
                    if(im.more) {
                        sendNext(im)
                    } else {
                        logger.error { "Partial send of Inbound not implemented" }
                    }
                }
                else {
                    logger.error("Leader got update from follower!!")
                }
            }
            Quality.FOLLOWER -> {}
            Quality.NOT_READY -> {
                if (im.sequence == 0L) {
                    leaderGroup = sm.sender
                }
                if(sm.sender != leaderGroup) {
                    logger.error("Inbound has wrong leader group")
                }
                addUpdate(im)
            }
        }
    }



    private fun sendNext(m : SequencedMessage) {
        val spm = SpreadMessage()
        spm.addGroup(serverGroup)
        spm.setSafe()
        var tasks = emptySet<Task<out Any>>()
        var more = false
        var sender = ""
        if(!taskQueues.outboundIsEmpty()) {
            val it = taskQueues.getOutboundIterator(m.receiver)
            if(it!!.hasNext()) {
                val it = it.next()
                tasks = setOf(*it.value.values.toTypedArray())
                more = true
                sender = it.key
            }
        }
        val om = OutboundMessage(tasks, more,
                                m.sequence + 1, m.receiver, sender)
        spread.multicast(spm, om)
        logger.info("Partial send to : ${om.receiver}, number ${om.sequence}")
    }


    private fun addUpdate(im : InboundMessage) {
        if (im.more) {
            //Finer grain deltas aren't implemented yet.
            logger.error("Inbounds should be entire list!!")
        }
        else {
            if(im.receiver != spread.privateGroup.toString()) {
                logger.info("Inbound for : ${im.receiver}")
            }
            else {
                if(im.tasks.isNotEmpty()) {
                    taskQueues.addAllToBackInbound(im.tasks)
                }
                logger.info("Got Inbound : " + im.receiver)
            }
        }
    }

    //Handle membership info.
    private fun handler(sm : SpreadMessage, m : MembershipInfo) {
        when (quality) {
            Quality.LEADER -> {
                if (m.isCausedByLeave) {
                    leaderOnLeave(sm, m)

                }
                //Assumption that joined is always a singular group.
                else if (m.isCausedByJoin) {
                    if(m.joined == spread.privateGroup) {
                        leaderSelfJoin(sm, m)
                    }
                    else {
                        leaderOnJoin(m)
                    }
                }
            }
            Quality.FOLLOWER -> {
                if (m.isCausedByLeave) {
                    followerOnLeave(m)
                }
                else if (m.isCausedByJoin) {
                    followerOnJoin(m)
                }
            }
            Quality.NOT_READY -> {
                //Check if it's me
                if (m.isCausedByLeave) {
                    if(leaderGroup != null && m.left == leaderGroup) {
                        //Clear everything. We can't guarantee we're up to date.
                        trackedMessages?.wipe()
                        taskQueues.wipe()
                    }
                    trackedGroups.purgeKnown(m.left)
                }
                else if (m.isCausedByJoin) {
                    if(m.joined == spread.privateGroup) {
                        serverGroup = sm.sender
                        //Assume I am a member of the group.
                        trackedGroups.registerKnown(m.members.toSet())
                    } else {
                        trackedGroups.registerTracked(m.joined)
                    }
                }
            }
        }
    }

    private fun leaderSelfJoin(sm : SpreadMessage, m : MembershipInfo) {
        leaderGroup = spread.privateGroup
        if(sm.sender.toString() == "servers") {
            trackedGroups.registerKnown(spread.privateGroup)
            serverGroup = sm.sender
        }
        else if (sm.sender.toString() == "service") {
            mainGroup = sm.sender
        }
    }


    /**
     * Kick all tasks assigned to a leaving client back into the inbound queue.
     *
     * Wipe the outbound queue of all client info.
     * @param m Message with leaving client.
     */
    private fun leaderOnLeave(sm : SpreadMessage, m : MembershipInfo) {
        if(sm.sender == mainGroup) {
            val backToInbound = taskQueues.backToInbound(m.left.toString())
            if (backToInbound.isNotEmpty()) {
                //send ClearMessage
                val spm = SpreadMessage()
                spm.setSafe()
                spm.addGroup(serverGroup)
                val cm = ClearMessage(backToInbound, m.left.toString())
                spread.multicast(spm, cm)

            }
        }
    }

    private fun leaderOnJoin(m : MembershipInfo) {
        when {
            m.group == serverGroup -> {
                sendInbound(m.joined.toString())
                trackedGroups.registerTracked(m.joined)
            }
            m.group == mainGroup -> logger.info("Client joined: ${m.joined}")
            else -> logger.error("Unknown group: ${m.joined}")
        }
    }

    private fun sendInbound(m : String) {
        val spm = SpreadMessage()
        spm.addGroup(serverGroup)
        spm.setSafe()
        val im = InboundMessage(taskQueues.getInbound(),
                false,
                0,
                m)
        spread.multicast(spm, im)
        logger.info("Partial send to : ${im.receiver}, number ${im.sequence}")
    }


    private fun followerOnJoin(m : MembershipInfo) {
        trackedGroups.registerTracked(m.joined)
        logger.info("New server joined : ${m.joined}")
    }


    private fun followerOnLeave(m : MembershipInfo) {
        //Assumption that left group is always size 1.
        if (leaderGroup == m.left) {
            trackedGroups.purgeKnown(m.left)
            val smallest = trackedGroups.getMinKnown()
            if(spread.privateGroup.toString() == smallest.toString()) {
                quality = quality.rise()
                mainGroup = spread.join("service")
                logger.info("Rose to leadership :${spread.privateGroup}")
                leaderGroup = spread.privateGroup
                //Resend inbounds to everyone
                trackedGroups.tracked.forEach {
                    sendInbound(it)
                }
                //Leaders can only fail crash
            }
            else {
                //smallest is now a Leader, no longer a follower.
                leaderGroup = smallest
                logger.info("New leader :$smallest")
            }
        }
    }


    private fun handler(sm : SpreadMessage, m : NewTaskMessage) {
        when (quality) {
            Quality.LEADER -> {
                taskQueues.sendToInbound(m.task)
                val spm = SpreadMessage()
                spm.addGroup(serverGroup)
                spm.setSafe()
                val dnm = DeltaNewMessage(sm.sender.toString(), setOf(m.task))
                spread.multicast(spm, dnm)
            }
            Quality.FOLLOWER, Quality.NOT_READY -> {
                val s = "Non-leader got client-specific new task message!!"
                logger.error(s)
            }
        }
    }

    private fun handler(sm : SpreadMessage, om : OutboundMessage) {
        when (quality) {
            Quality.LEADER -> {
                if(om.more) {
                    sendNext(om)
                } else {
                    logger.info("Is up-to-date : ${om.receiver}")
                }
            }
            Quality.FOLLOWER -> {
                logger.info("Outbound for : ${om.receiver}")
                setTrackedToKnown(om)
            }
            Quality.NOT_READY -> if (om.receiver != spread.privateGroup.toString()) {
                logger.info("Outbound for : ${om.receiver}")
                setTrackedToKnown(om)
            } else {
                if (om.tasks.isNotEmpty()) {
                    taskQueues.replaceAfterGet(om.tasks, om.sender, 0)
                    logger.info("Primed set : ${om.sequence}")
                }
                if (!om.more) {
                    quality = quality.follow()
                    trackedMessages?.handleAll(this::handler)
                    trackedMessages?.wipe()
                    logger.info("Rose to follower : ${spread.privateGroup}")
                }
            }
        }
    }

    private fun setTrackedToKnown(om: OutboundMessage) {
        if (!om.more) {
            val t = trackedGroups.getTracked(om.receiver) as SpreadGroup
            trackedGroups.registerKnown(t)
            trackedGroups.purgeTracked(om.receiver)
        }
    }


    private fun handler(sm : SpreadMessage, m : ResultMessage) {
        when (quality) {
            Quality.LEADER -> logger.error("Duplicate exception!!")
            Quality.FOLLOWER, Quality.NOT_READY ->
                logger.error("Non-leader got client-specific result message!!")
        }
    }

    private fun handler(sm : SpreadMessage, m : TaskMessage) {
        when (quality) {
            Quality.LEADER ->
                //I know it, I sent it. Should reach only the client.
                logger.error("Got Task message back : ${sm.sender}")
            Quality.FOLLOWER, Quality.NOT_READY ->
                logger.error("Non-leader got client-specific task message!!")
        }
    }


    private fun handler(sm : SpreadMessage, m : URIMessage) {
        when (quality) {
            Quality.LEADER -> {
                val out = taskQueues.purgeOutbound(sm.sender.toString(), m.uri)
                if (out.isNotEmpty()) {
                    val spm = SpreadMessage()
                    spm.setSafe()
                    spm.addGroup(serverGroup)
                    val dcm = DeltaCompleteMessage(sm.sender.toString(), out)
                    spread.multicast(spm, dcm)
                }
            }
            Quality.FOLLOWER, Quality.NOT_READY ->
                logger.error("Non-leader got client-specific URL message!!")
        }
    }

    private fun handler(sm : StateMessage) {
        when (sm) {
            is ClearMessage -> handler(sm)
            is DeltaCompleteMessage -> handler(sm)
            is DeltaGetMessage -> handler(sm)
            is DeltaNewMessage -> handler(sm)
            else -> logger.error("Unexpected unrecognized StateMessage!!")
        }
    }


}