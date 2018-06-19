package pt.um.tf.taskmux.server;

import pt.um.tf.taskmux.commons.task.Result;
import pt.um.tf.taskmux.commons.task.Task;

public class TaskResult extends Result<Task> {
    private boolean success;
    private Task t;
    private Exception e;

    public TaskResult(boolean success, Task t, Exception e) {
        this.success = success;
        this.t = t;
        this.e = e;
    }

    protected TaskResult() {
    }

    @Override
    public Task completeWithResult() {
        return t;
    }

    @Override
    public boolean completedSuccessfully() {
        return success;
    }

    @Override
    public Exception completedWithException() {
        return e;
    }

    /*
    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        buffer.writeBoolean(success);
        serializer.writeObject(t);
        serializer.writeObject(e);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        success = buffer.readBoolean();
        t = serializer.readObject(buffer);
        e = serializer.readObject(buffer);
    }
    */
}
