package pt.um.tf.taskmux.server.messaging

import pt.um.tf.taskmux.commons.task.Task


class DeltaNewMessage(val sender : String = "",
                      val inSet : Set<Task<out Any>> = emptySet()) : StateMessage