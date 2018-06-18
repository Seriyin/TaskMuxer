package pt.um.tf.taskmuxer.commons.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.Serializer;

public class GetTaskMessage implements CommonMessage {
    public GetTaskMessage() {}

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {}

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {}
}
