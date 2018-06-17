package pt.um.tf.commons.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;

public class GetTaskMessage implements CatalystSerializable {
    public GetTaskMessage() {}

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {}

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {}
}
