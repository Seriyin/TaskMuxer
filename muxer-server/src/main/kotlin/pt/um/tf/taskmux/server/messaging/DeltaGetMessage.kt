package pt.um.tf.taskmux.server.messaging

import pt.um.tf.taskmux.commons.task.Task


class DeltaGetMessage(val sender : String = "",
                      val inSet : Set<Task<out Any>> = emptySet(),
                      val count : Int = -1) : StateMessage