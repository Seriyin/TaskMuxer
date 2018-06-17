package pt.um.tf.commons.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import pt.um.tf.commons.task.Task;

/**
 * Type erase the task.
 * Check concrete type on client.
 */
public class TaskMessage implements CatalystSerializable {
    private Task task;

    public TaskMessage(Task task) {
        this.task = task;
    }

    protected TaskMessage() {}

    public Task getTask() {
        return task;
    }


    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        task.writeObject(buffer, serializer);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        task = serializer.readObject(buffer);
    }
}
