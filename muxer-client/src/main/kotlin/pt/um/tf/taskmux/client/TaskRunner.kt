package pt.um.tf.taskmux.client

import mu.KLogging
import pt.haslab.ekit.Spread
import pt.um.tf.taskmux.commons.messaging.URIMessage
import pt.um.tf.taskmux.commons.task.DummyTask
import pt.um.tf.taskmux.commons.task.Result
import pt.um.tf.taskmux.commons.task.Task
import spread.SpreadGroup
import spread.SpreadMessage
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.function.Consumer

/**
 * The main runner of tasks.
 * Informs completion of tasks directly through to spread.
 */
class TaskRunner(private val taskPool : ExecutorService,
                 private val barrier : Int,
                 private val spread : Spread) {
    companion object : KLogging()
    private val runningTasks : Queue<Task<out Any>> = ArrayDeque(barrier)
    private var maingroup : SpreadGroup? = null

    /**
     * Joins aren't immediately effective.
     * @param maingroup
     */
    fun setMaingroup(maingroup : SpreadGroup) {
        this.maingroup = maingroup
    }

    fun runTask(t : Task<out Any>) : Boolean {
        var res = false
        if (!isBackedUp()) {
            if (t is DummyTask) {
                t.executor = taskPool
                t.start()
                 .thenAcceptAsync(this.accept(t), taskPool)
                 .exceptionally {
                     logger.error("", it)
                     runningTasks.remove()
                     return@exceptionally null
                 }
                runningTasks.add(t)
                res = true
            }
            else {
                logger.error("Unknown task type")
            }
        }
        return res
    }

    private fun accept(dt: DummyTask) : Consumer<Result<Long>> {
        return Consumer {
            handler(dt, it)
        }
    }

    private fun handler(dt : DummyTask, longResult : Result<Long>) {
        //Result doesn't really matter.
        if(!longResult.completedSuccessfully()) {
            logger.error("Failed task :", longResult.completedWithException())
        }
        logger.info("Completed task : " + longResult.completeWithResult())
        runningTasks.remove()
        sendURLMessage(dt)
    }

    private fun sendURLMessage(ran : Task<out Any>) {
        var spm = SpreadMessage()
        spm.addGroup(maingroup)
        spm.setSafe()
        spread.multicast(spm, URIMessage(ran.uri))
    }

    fun isBackedUp() : Boolean {
        return runningTasks.size >= barrier
    }

}
