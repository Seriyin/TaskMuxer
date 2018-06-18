package pt.um.tf.commons.task;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.Serializer;

public class EmptyResult extends Result<Void> {
    private boolean success;
    private Exception e;

    public EmptyResult(boolean success, Exception e) {
        this.success = success;
        this.e = e;
    }

    public EmptyResult(boolean success) {
        this.success = success;
    }

    protected EmptyResult() {}

    @Override
    public Void completeWithResult() {
        return null;
    }

    @Override
    public boolean completedSuccessfully() {
        return success;
    }

    @Override
    public Exception completedWithException() {
        return e;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        buffer.writeBoolean(success);
        serializer.writeObject(e);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        success = buffer.readBoolean();
        e = serializer.readObject(buffer);

    }
}
