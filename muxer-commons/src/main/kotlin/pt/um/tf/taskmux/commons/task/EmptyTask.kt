package pt.um.tf.taskmux.commons.task

class EmptyTask : Task<Nothing>() {
    override fun start(): Nothing = throw NotImplementedError()

    override fun completed(): Boolean = false
    override fun cancel(){}
}