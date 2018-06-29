package pt.um.tf.taskmux.server;

import pt.um.tf.taskmux.commons.IndexedDeque;
import pt.um.tf.taskmux.server.messaging.StateMessage;

import java.util.function.Consumer;

class TrackedMessages {
    private val sms : IndexedDeque<StateMessage> = IndexedDeque()
    private val lastIndex : Int = 0

    fun add(m : StateMessage) = sms.addLast(m)

    fun wipe() = sms.clear()

    fun handleAll(handler : (StateMessage) -> Unit) {
        sms.forEach(handler)
        sms.clear()
    }
}
