package pt.um.tf.commons.task;

public class DummyResult implements Result<Long> {
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
}
