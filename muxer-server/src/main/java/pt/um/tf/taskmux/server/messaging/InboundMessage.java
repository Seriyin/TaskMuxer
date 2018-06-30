package pt.um.tf.taskmux.server.messaging;

import pt.um.tf.taskmux.commons.task.Task;

import java.io.Serializable;
import java.util.Collection;

public class InboundMessage implements SequencedMessage {
    private Collection<Task> tasks;
    private boolean more;
    private long sequence;
    private String receiver;

    public InboundMessage(Collection<Task> tasks,
                          boolean more,
                          long sequence,
                          String receiver) {
        this.tasks = tasks;
        this.more = more;
        this.sequence = sequence;
        this.receiver = receiver;
    }

    protected InboundMessage() {
    }

    @Override
    public Collection<Task> getTasks() {
        return tasks;
    }

    @Override
    public boolean hasMore() {
        return more;
    }

    @Override
    public long getSequence() {
        return sequence;
    }

    @Override
    public String getReceiver() {
        return receiver;
    }

/*
    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {

        buffer.writeString(receiver);
        buffer.writeBoolean(more);
        buffer.writeLong(sequence);
        serializer.writeObject(tasks);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        receiver = buffer.readString();
        more = buffer.readBoolean();
        sequence = buffer.readLong();
        tasks = serializer.readObject(buffer);
    }
*/
}
