package pt.um.tf.commons.error;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class MissingExecutorException extends Exception implements CatalystSerializable {
    @Override
    public String getMessage() {
        return "Missing Executor to execute task in.";
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {}

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {}
}
