package pt.um.tf.taskmux.commons.task;

class DummyResult(private val id : Long? = null,
                  private val e : Exception? = null) : Result<Long>() {
    private val success : Boolean = id != null

    override fun completeWithResult() : Long {
        return id ?: throw NullPointerException()
    }

    override fun completedSuccessfully() : Boolean {
        return success
    }

    override fun completedWithException() : Exception {
        return e ?: throw NullPointerException()
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
