package pt.um.tf.taskmux.commons.error

import io.atomix.catalyst.buffer.BufferInput
import io.atomix.catalyst.buffer.BufferOutput
import io.atomix.catalyst.serializer.CatalystSerializable
import io.atomix.catalyst.serializer.Serializer

class MissingExecutorException : Exception(), CatalystSerializable {
    override val message: String?
        get() = "Missing Executor to execute task in."


    override fun writeObject(buffer: BufferOutput<*>?, serializer: Serializer?) {}

    override fun readObject(buffer: BufferInput<*>?, serializer: Serializer?) {}

}
