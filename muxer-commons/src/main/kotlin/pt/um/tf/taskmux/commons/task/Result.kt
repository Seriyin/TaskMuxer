package pt.um.tf.taskmux.commons.task;

import java.io.Serializable

abstract class Result<T> : Serializable { //implements CatalystSerializable
    abstract fun completeWithResult() : T
    abstract fun completedSuccessfully() : Boolean
    abstract fun completedWithException() : Exception
}
