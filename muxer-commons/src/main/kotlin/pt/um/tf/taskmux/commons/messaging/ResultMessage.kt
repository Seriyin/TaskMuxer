package pt.um.tf.taskmux.commons.messaging

import pt.um.tf.taskmux.commons.task.EmptyResult
import pt.um.tf.taskmux.commons.task.Result

class ResultMessage(val result : Result<out Any> = EmptyResult()) : CommonMessage