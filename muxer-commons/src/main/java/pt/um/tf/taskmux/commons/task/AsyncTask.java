package pt.um.tf.taskmux.commons.task;

import pt.um.tf.taskmux.commons.URIGenerator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class AsyncTask<T> extends Task<CompletableFuture<Result<T>>> {
    private ExecutorService executorService;

    public AsyncTask(URIGenerator u) {
        super(u);
    }

    protected AsyncTask() {}

    public ExecutorService getExecutor() {
        return executorService;
    }

    public void setExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
