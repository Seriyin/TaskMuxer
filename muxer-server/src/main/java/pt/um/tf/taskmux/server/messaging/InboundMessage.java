package pt.um.tf.taskmux.server.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import pt.um.tf.commons.task.Task;
import spread.SpreadGroup;

import java.util.Collection;
import java.util.List;

public class InboundMessage implements StateMessage {
    private Collection<Task> tasks;
    private boolean more;
    private long sequence;
    private SpreadGroup receiver;

    public InboundMessage(Collection<Task> tasks,
                          boolean more,
                          long sequence,
                          SpreadGroup receiver) {
        this.tasks = tasks;
        this.more = more;
        this.sequence = sequence;
        this.receiver = receiver;
    }

    protected InboundMessage() {
    }

    public Collection<Task> getTask() {
        return tasks;
    }

    public boolean hasMore() {
        return more;
    }

    public long getSequence() {
        return sequence;
    }

    public SpreadGroup getReceiver() {
        return receiver;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        buffer.writeBoolean(more);
        buffer.writeLong(sequence);
        serializer.writeObject(receiver);
        serializer.writeObject(tasks);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        more = buffer.readBoolean();
        sequence = buffer.readLong();
        receiver = serializer.readObject(buffer);
        tasks = serializer.readObject(buffer);
    }
}
