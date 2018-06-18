package pt.um.tf.commons.task;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.Serializer;
import pt.um.tf.commons.error.MissingExecutorException;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class DummyTask extends AsyncTask<Long> {
    private static long LOOPS_TO_DO = 65536;
    private CompletableFuture<Result<Long>> cp;
    private boolean started;
    private ExecutorService executorService;

    public DummyTask(String id) {
        super(id);
        started = false;
    }

    protected DummyTask() {}

    @Override
    public CompletableFuture<Result<Long>> start() {
        if(!started) {
            started = true;
            startUpTask();
        }
        return cp;
    }

    private void startUpTask() {
        if(executorService == null || executorService.isTerminated()) {
            cp = CompletableFuture.failedFuture(new MissingExecutorException());
        }
        else {
            cp = CompletableFuture.supplyAsync(this::dummyTask, executorService);
        }
    }

    private Result<Long> dummyTask() {
        var r = new Random(getURL().getPath().chars().asLongStream().sum());
        long id = 0;
        //Might intentionally divide by zero.
        //This simulates a possibly throwing long running background task.
        try{
            for(int i = 0; i < LOOPS_TO_DO; i++) {
                id += r.nextLong();
                id >>= 3;
                id *= 22;
                id += 2;
                id /= r.nextLong();
                try {
                    Thread.sleep(60);
                } catch (InterruptedException e) {
                    //Silent night, Holy night.
                }
            }
            return new DummyResult(id);
        }
        catch (ArithmeticException e) {
            return new DummyResult(e);
        }
    }



    @Override
    public boolean completed() {
        return started && !cp.isCancelled() && cp.isDone();
    }

    @Override
    public void cancel() {
        if(started) {
            if (!cp.isDone()) {
                cp.cancel(true);
            }
        }
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(getURL());
        buffer.writeBoolean(started);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        setUrl(serializer.readObject(buffer));
        started = buffer.readBoolean();
        executorService = ForkJoinPool.commonPool();
    }

    @Override
    public void setExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }

}
