package pt.um.tf.taskmux.server.messaging

import pt.um.tf.taskmux.commons.task.Task


class ClearMessage(val backToInbound : Set<Task<out Any>> = emptySet(),
                   val clear : String = "") : StateMessage
