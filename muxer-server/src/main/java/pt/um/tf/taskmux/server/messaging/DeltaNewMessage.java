package pt.um.tf.taskmux.server.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.Serializer;
import pt.um.tf.taskmuxer.commons.task.Task;

import java.util.ArrayList;
import java.util.List;

public class DeltaNewMessage implements StateMessage {
    private String sender;
    private List<Task> in;

    public DeltaNewMessage(String sender, Task in) {
        this.sender = sender;
        this.in = new ArrayList<>();
        this.in.add(in);
    }

    public DeltaNewMessage(String sender, List<Task> in) {
        this.sender = sender;
        this.in = in;
    }

    protected DeltaNewMessage() {}

    public String getSender() {
        return sender;
    }

    public List<Task> getIn() {
        return in;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        buffer.writeString(sender);
        serializer.writeObject(in);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        sender = buffer.readString();
        in = serializer.readObject(buffer);
    }
}
