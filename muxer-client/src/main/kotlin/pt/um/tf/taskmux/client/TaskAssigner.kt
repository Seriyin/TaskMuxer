package pt.um.tf.taskmux.client;

import mu.KLogging
import pt.haslab.ekit.Spread
import pt.um.tf.taskmux.commons.messaging.GetTaskMessage
import spread.SpreadGroup
import spread.SpreadMessage
import java.util.*

class TaskAssigner(private val spread : Spread,
                   private val mainGroup : SpreadGroup,
                   private val runner : TaskRunner) : TimerTask() {
    companion object : KLogging()

    override fun run() {
        if (!runner.isBackedUp()) {
            val spm = SpreadMessage()
            spm.addGroup(mainGroup)
            spm.setSafe()
            spread.multicast(spm, GetTaskMessage())
        }
        else {
            logger.info("Runner is backed up")
        }
    }
}
