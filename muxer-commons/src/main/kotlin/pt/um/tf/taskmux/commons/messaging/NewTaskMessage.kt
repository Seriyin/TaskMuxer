package pt.um.tf.taskmux.commons.messaging

import pt.um.tf.taskmux.commons.task.Task

class NewTaskMessage(private val task : Task<out Any>? = null) : CommonMessage {
    fun getTask() : Task<out Any> {
        return task as Task<out Any>
    }
}
