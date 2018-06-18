package pt.um.tf.taskmuxer.commons.error;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class UnknownClientException extends Exception implements CatalystSerializable {
    @Override
    public String getMessage() {
        return super.getMessage() + System.lineSeparator() + "Unknown Client";
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {}

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {}
}
