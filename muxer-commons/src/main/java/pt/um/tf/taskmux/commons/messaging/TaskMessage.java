package pt.um.tf.taskmux.commons.messaging;

import pt.um.tf.taskmux.commons.task.Task;

/**
 * Type erase the task.
 * Check concrete type on client.
 */
public class TaskMessage implements CommonMessage {
    private Task task;

    public TaskMessage(Task task) {
        this.task = task;
    }

    protected TaskMessage() {}

    public Task getTask() {
        return task;
    }

    /*
    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(task, buffer);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        task = serializer.readObject(buffer);
    }
    */
}
