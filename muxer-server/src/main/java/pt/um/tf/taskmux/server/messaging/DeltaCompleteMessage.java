package pt.um.tf.taskmux.server.messaging;

import pt.um.tf.taskmux.commons.task.Task;

import java.util.Collection;

public class DeltaCompleteMessage implements StateMessage {
    private String sender;
    private Collection<Task> tasks;


    public DeltaCompleteMessage(String sender, Collection<Task> urls) {
        this.sender = sender;
        this.tasks = urls;
    }

    protected DeltaCompleteMessage() {}

    public Collection<Task> getTasks() {
        return tasks;
    }

    public String getSender() {
        return sender;
    }

    /*
    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(tasks);
        buffer.writeString(sender);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        tasks = serializer.readObject(buffer);
        sender = serializer.readObject(buffer);
    }
    */
}
