package pt.um.tf.taskmux.commons.messaging

import pt.um.tf.taskmux.commons.task.Task

/**
 * Type erase the task.
 * Check concrete type on client.
 */
class TaskMessage(private val task : Task<out Any>? = null) : CommonMessage {
    fun getTask() : Task<out Any> {
        return task as Task<out Any>
    }
}
