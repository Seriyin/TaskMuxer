package pt.um.tf.commons.task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public interface AsyncTask<T> extends Task<CompletableFuture<Result<T>>> {
    void setExecutor(ExecutorService executorService);
}
