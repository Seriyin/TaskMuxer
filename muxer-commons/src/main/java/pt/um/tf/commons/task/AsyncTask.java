package pt.um.tf.commons.task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class AsyncTask<T> extends Task<CompletableFuture<Result<T>>> {
    private ExecutorService executorService;

    public AsyncTask(String id) {
        super(id);
    }

    protected AsyncTask() {}

    public void setExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
