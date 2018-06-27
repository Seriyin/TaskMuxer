package pt.um.tf.taskmux.commons.task

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

abstract class AsyncTask<T>(id : String? = null) : Task<CompletableFuture<Result<T>>>(id) {
    private var executorService : ExecutorService = ForkJoinPool.commonPool()

    fun getExecutor() : ExecutorService {
        return executorService
    }

    fun setExecutor(executorService : ExecutorService) {
        this.executorService = executorService
    }
}
