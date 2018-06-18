package pt.um.tf.taskmux.server.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.Serializer;
import pt.um.tf.taskmuxer.commons.task.Task;

import java.util.Collection;

public class DeltaGetMessage implements StateMessage {
    private String sender;
    private Collection<Task> in;
    private int count;

    public DeltaGetMessage(String sender, Collection<Task> in) {
        this.sender = sender;
        this.in = in;
        count = 1;
    }

    public DeltaGetMessage(String sender, Collection<Task> in, int count) {
        this.sender = sender;
        this.in = in;
        this.count = count;
    }

    protected DeltaGetMessage() {}

    public String getSender() {
        return sender;
    }

    public Collection<Task> getIn() {
        return in;
    }

    public int getCount() {
        return count;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        buffer.writeString(sender);
        serializer.writeObject(in);
        buffer.writeInt(count);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        sender = buffer.readString();
        in = serializer.readObject(buffer);
        count = buffer.readInt();
    }
}
