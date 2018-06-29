package pt.um.tf.taskmux.server.messaging;

import pt.um.tf.taskmux.commons.task.Task


class InboundMessage(val tasks : Set<Task<out Any>> = emptySet(),
                     val more : Boolean = false,
                     val sequence : Long = -1L,
                     val receiver : String = "") : StateMessage