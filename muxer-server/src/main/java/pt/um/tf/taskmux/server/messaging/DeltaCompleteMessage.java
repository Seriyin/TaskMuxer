package pt.um.tf.taskmux.server.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.Serializer;
import pt.um.tf.commons.task.Task;
import spread.SpreadGroup;

import java.util.Collection;

public class DeltaCompleteMessage implements StateMessage {
    private SpreadGroup sender;
    private Collection<Task> tasks;


    public DeltaCompleteMessage(SpreadGroup sender, Collection<Task> urls) {
        this.sender = sender;
        this.tasks = urls;
    }

    protected DeltaCompleteMessage() {}

    public Collection<Task> getTasks() {
        return tasks;
    }

    public SpreadGroup getSender() {
        return sender;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(tasks);
        serializer.writeObject(sender);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        tasks = serializer.readObject(buffer);
        sender = serializer.readObject(buffer);
    }
}
