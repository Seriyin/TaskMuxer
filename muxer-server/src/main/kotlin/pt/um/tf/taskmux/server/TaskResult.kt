package pt.um.tf.taskmux.server

import pt.um.tf.taskmux.commons.task.Result
import pt.um.tf.taskmux.commons.task.Task

class TaskResult(private val success : Boolean = false,
                 private val t : Task<out Any>?,
                 private val e : Exception?) : Result<Task<out Any>>() {
    override fun completeWithResult(): Task<out Any> {
        return t ?: throw NullPointerException()
    }

    override fun completedSuccessfully(): Boolean {
        return success
    }

    override fun completedWithException(): Exception {
        return e ?: throw NullPointerException()
    }

}
