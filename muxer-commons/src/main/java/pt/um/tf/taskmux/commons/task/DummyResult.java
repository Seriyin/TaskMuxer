package pt.um.tf.taskmux.commons.task;

public class DummyResult extends Result<Long> {
    private long id;
    private Exception e;
    private boolean success;

    public DummyResult(long id) {
        this.id = id;
        success = true;
    }

    public DummyResult(Exception e) {
        this.e = e;
        success = false;
    }

    @Override
    public Long completeWithResult() {
        return id;
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
        buffer.writeLong(id);
        serializer.writeObject(e);
        buffer.writeBoolean(success);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        id = buffer.readLong();
        e = serializer.readObject(buffer);
        success = buffer.readBoolean();
    }
    */
}
