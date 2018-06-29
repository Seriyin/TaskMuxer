package pt.um.tf.taskmux.commons.messaging

import pt.um.tf.taskmux.commons.task.Task

class NewTaskMessage(private val _task : Task<out Any>? = null) : CommonMessage {
    val task get() = _task as Task<out Any>
}