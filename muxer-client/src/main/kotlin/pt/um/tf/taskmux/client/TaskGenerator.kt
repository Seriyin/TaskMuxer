package pt.um.tf.taskmux.client;

import pt.haslab.ekit.Spread;
import pt.um.tf.taskmux.commons.messaging.NewTaskMessage;
import pt.um.tf.taskmux.commons.task.DummyTask;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.TimerTask;

class TaskGenerator(private val spread : Spread,
                    private val mainGroup : SpreadGroup,
                    private val me : String) : TimerTask() {

    override fun run() {
        val spm = SpreadMessage()
        spm.setSafe()
        spm.addGroup(mainGroup)
        spread.multicast(spm, NewTaskMessage(DummyTask(me)))
    }

}
