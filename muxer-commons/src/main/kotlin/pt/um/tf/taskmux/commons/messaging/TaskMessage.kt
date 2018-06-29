package pt.um.tf.taskmux.commons.messaging

import pt.um.tf.taskmux.commons.task.EmptyTask
import pt.um.tf.taskmux.commons.task.Task

/**
 * Type erase the task.
 * Check concrete type on client.
 */
class TaskMessage(val task : Task<out Any> = EmptyTask()) : CommonMessage
