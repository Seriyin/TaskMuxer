package pt.um.tf.taskmux.commons.task

import pt.um.tf.taskmux.commons.URIGenerator
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

abstract class AsyncTask<T>(u : URIGenerator = URIGenerator()) : Task<CompletableFuture<Result<T>>>(u) {
    var executor : ExecutorService? = null
}
