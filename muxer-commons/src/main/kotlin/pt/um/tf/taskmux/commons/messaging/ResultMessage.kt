package pt.um.tf.taskmux.commons.messaging

import pt.um.tf.taskmux.commons.task.Result

class ResultMessage(private val result : Result<out Any>? = null) : CommonMessage {
    fun getResult() : Result<out Any> {
        return result as Result<out Any>
    }
}
