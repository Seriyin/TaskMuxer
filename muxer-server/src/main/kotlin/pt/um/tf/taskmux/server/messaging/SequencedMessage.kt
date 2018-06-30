package pt.um.tf.taskmux.server.messaging

import pt.um.tf.taskmux.commons.task.Task

interface SequencedMessage : StateMessage {
    val more : Boolean
    val tasks : Set<Task<out Any>>
    val receiver : String
    val sequence : Long
}