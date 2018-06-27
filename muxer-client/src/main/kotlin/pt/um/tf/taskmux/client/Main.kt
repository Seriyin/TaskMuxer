package pt.um.tf.taskmux.client

import io.atomix.catalyst.concurrent.SingleThreadContext
import io.atomix.catalyst.serializer.Serializer
import mu.KLogging
import pt.haslab.ekit.Spread
import pt.um.tf.taskmux.commons.error.DuplicateException
import pt.um.tf.taskmux.commons.error.MissingExecutorException
import pt.um.tf.taskmux.commons.error.NoAssignableTasksException
import pt.um.tf.taskmux.commons.error.UnknownClientException
import pt.um.tf.taskmux.commons.messaging.*
import pt.um.tf.taskmux.commons.task.DummyResult
import pt.um.tf.taskmux.commons.task.DummyTask
import pt.um.tf.taskmux.commons.task.EmptyResult
import spread.MembershipInfo
import spread.SpreadGroup
import spread.SpreadMessage
import java.net.URI
import java.util.*
import java.util.concurrent.ForkJoinPool

fun main(args : Array<String>) {
        val main = Client()
        main.run()
}


class Client {
    companion object : KLogging()
    private val me = "cli-" + UUID.randomUUID()
    private val sr = Serializer()
    private val threadContext = SingleThreadContext("srv-%d", sr);
    private val spread = Spread(me, true)
    private val outbound = ArrayDeque<CommonMessage>()
    private val runner = TaskRunner(ForkJoinPool.commonPool(),
                                    ForkJoinPool.getCommonPoolParallelism(),
                                    spread)
    private var mainGroup : SpreadGroup? = null
    private var leaderGroup : SpreadGroup? = null
    private var timer : Timer? = null


    internal fun run() {
        register();
        threadContext.execute(this::openAndJoin)
                     .thenRun(this::handlers).exceptionally {
                         logger.error("", it)
                         return@exceptionally null
                     }
        while(readLine() == null);
        if(mainGroup != null) {
            spread.leave(mainGroup)
        }
        timer?.cancel()
        timer?.purge()
        spread.close()
        threadContext.close()
        logger.info("I'm here")
    }

    private fun register() {
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
        spread.join("service")
    }


    private fun handlers() {
        logger.info("Handling connection");
        spread.handler(GetTaskMessage::class.java, this::handler)
              .handler(MembershipInfo::class.java, this::handler)
              .handler(NewTaskMessage::class.java, this::handler)
              .handler(ResultMessage::class.java, this::handler)
              .handler(TaskMessage::class.java, this::handler)
              .handler(URIMessage::class.java, this::handler)
    }


    private fun handler(sm : SpreadMessage, gm : GetTaskMessage) {
        if(leaderGroup == null && sm.sender == spread.privateGroup) {
            val s = "GetTaskMessage added to outbound, leader unavailable."
            logger.info(s)
            outbound.offer(gm)
        }
        else {
            logger.info("Successful dispatch of task get")
        }
    }

    private fun handler(sm : SpreadMessage, m : MembershipInfo) {
        if(m.isCausedByLeave) {
            if (m.left == leaderGroup) {
                logger.info("Leader left")
                leaderGroup = null
            }
            logger.info("Left : ${m.left}")
        }
        else if (m.isCausedByJoin) {
            if(m.joined == spread.privateGroup) {
                //I just joined.
                mainGroup = sm.sender
                leaderGroup = m.members.firstOrNull {
                    it.toString().substring(1,5) != "srv-"
                }
                runner.setMaingroup(mainGroup as SpreadGroup)
                initTimer()
            }
            if(m.group.toString() == "service" &&
               m.joined.toString().substring(1,5) == "srv-") {
                logger.info("New leader : ${m.joined}")
                leaderGroup = m.joined
                if(!outbound.isEmpty()) {
                    outbound.forEach(this::resend)
                    outbound.clear()
                }
            }
            else {
                logger.info("Join : ${m.joined}")
            }
        }
    }

    private fun initTimer() {
        timer = Timer()
        val random = Random(me.codePoints().mapToLong{it.toLong()}.sum())
        val tg = TaskGenerator(spread, mainGroup as SpreadGroup, me)
        val ta = TaskAssigner(spread, mainGroup as SpreadGroup, runner)
        timer?.scheduleAtFixedRate(tg, 0,
                random.nextInt(30000).toLong()+10000)
        timer?.scheduleAtFixedRate(ta, 0,
                random.nextInt(10000).toLong()+2000)
    }

    private fun resend(cm : CommonMessage) {
        val spm = SpreadMessage()
        spm.addGroup(mainGroup)
        spm.setSafe()
        spread.multicast(spm, cm)
    }

    private fun handler(sm : SpreadMessage, nm : NewTaskMessage) {
        if(leaderGroup == null && sm.sender == spread.privateGroup) {
            val s = "NewTaskMessage added to outbound, leader unavailable : ${sm.sender}"
            logger.info(s)
            outbound.offer(nm)
        }
        else {
            logger.info("New task posted by : ${sm.sender}")
        }
    }

    private fun handler(sm : SpreadMessage, rm : ResultMessage) {
        val err = rm.result.completedWithException()
        if (err is NoAssignableTasksException) {
            logger.info("", err)
        }
        else {
            logger.error("", err)
        }
    }

    private fun handler(sm : SpreadMessage, tm : TaskMessage) {
        if (sm.groups.all{ it == spread.privateGroup}) {
            logger.info("Got new Task : ${spread.privateGroup}")
            if (!runner.runTask(tm.task)) {
                logger.error("Past barrier for multi-threading")
            }
        }
        else {
            val gps = sm.groups.map(SpreadGroup::toString).joinToString { it }
            logger.error("Got new Task destined to : $gps")
        }
    }

    private fun handler(sm : SpreadMessage, um : URIMessage) {
        if(leaderGroup == null && sm.sender == spread.privateGroup) {
            val s = "URLMessage added to outbound, leader unavailable : ${um.uri}"
            logger.info(s)
            outbound.offer(um)
        }
        else {
            logger.info("Successful dispatch of task completion")
        }
    }

}
