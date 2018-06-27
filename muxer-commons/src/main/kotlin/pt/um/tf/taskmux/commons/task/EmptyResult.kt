package pt.um.tf.taskmux.commons.task;

class EmptyResult(private val e : Exception? = null) : Result<Nothing>() {
    private val success : Boolean = e != null

    override fun completeWithResult(): Nothing {
        throw e ?: throw NotImplementedError()
    }

    override fun completedSuccessfully(): Boolean {
        return success
    }

    override fun completedWithException(): Exception {
        return e ?: throw NullPointerException()
    }

    /*
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
    */
}
