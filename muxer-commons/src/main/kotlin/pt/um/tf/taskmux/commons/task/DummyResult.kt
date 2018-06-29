package pt.um.tf.taskmux.commons.task;

class DummyResult(private val id : Long? = null,
                  private val e : Exception? = null) : Result<Long>() {
    private val success : Boolean get() = id != null

    override fun completeWithResult() : Long {
        return id ?: throw NullPointerException()
    }

    override fun completedSuccessfully() : Boolean {
        return success
    }

    override fun completedWithException() : Exception {
        return e ?: throw NullPointerException()
    }
}
