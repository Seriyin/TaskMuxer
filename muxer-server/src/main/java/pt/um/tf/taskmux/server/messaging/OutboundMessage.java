package pt.um.tf.taskmux.server.messaging;

import pt.um.tf.taskmux.commons.task.Task;

import java.util.Collection;

public class OutboundMessage implements StateMessage {
    private Collection<Task> tasks;
    private boolean more;
    private long sequence;
    private String receiver;
    private String sender;

    public OutboundMessage(Collection<Task> tasks,
                           boolean more,
                           long sequence,
                           String receiver,
                           String sender) {
        this.tasks = tasks;
        this.more = more;
        this.sequence = sequence;
        this.receiver = receiver;
        this.sender = sender;
    }

    protected OutboundMessage() {
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

    public String getReceiver() {
        return receiver;
    }

    public String getSender() {
        return sender;
    }

    /*
    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(tasks);
        buffer.writeBoolean(more);
        buffer.writeLong(sequence);
        buffer.writeString(sender);
        buffer.writeString(receiver);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        tasks = serializer.readObject(buffer);
        more = buffer.readBoolean();
        sequence = buffer.readLong();
        sender = buffer.readString();
        receiver = buffer.readString();
    }
    */
}
