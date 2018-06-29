package pt.um.tf.taskmux.server.messaging

import pt.um.tf.taskmux.commons.task.Task

class DeltaCompleteMessage(val sender : String = "",
                           val tasks : Set<Task<out Any>> = emptySet()) : StateMessage
