package pt.um.tf.taskmux.server.messaging

import pt.um.tf.taskmux.commons.task.Task

class OutboundMessage(override val tasks : Set<Task<out Any>> = emptySet(),
                      override val more : Boolean = false,
                      override val sequence : Long = -1L,
                      override val receiver : String = "",
                      val sender : String = "") : SequencedMessage