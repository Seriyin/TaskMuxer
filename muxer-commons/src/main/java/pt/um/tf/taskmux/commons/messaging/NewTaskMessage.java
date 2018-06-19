package pt.um.tf.taskmux.commons.messaging;

import pt.um.tf.taskmux.commons.task.Task;

public class NewTaskMessage implements CommonMessage {
    private Task task;


    public NewTaskMessage(Task task) {
        this.task = task;
    }

    protected NewTaskMessage() {}



    public Task getTask() {
        return task;
    }

    /*
    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(task);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        task = serializer.readObject(buffer);
    }
    */
}
