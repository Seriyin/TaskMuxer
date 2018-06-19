package pt.um.tf.taskmux.commons.task;

import pt.um.tf.taskmux.commons.error.MissingExecutorException;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class DummyTask extends AsyncTask<Long> {
    private static long LOOPS_TO_DO = 16384;
    private CompletableFuture<Result<Long>> cp;
    private boolean started;

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
        if(getExecutor() == null || getExecutor().isTerminated()) {
            cp = CompletableFuture.failedFuture(new MissingExecutorException());
        }
        else {
            cp = CompletableFuture.supplyAsync(this::dummyTask, getExecutor());
        }
    }

    private Result<Long> dummyTask() {
        var r = new Random(getURI().getPath().chars().asLongStream().sum());
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
                    Thread.sleep(2, 800);
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

    /*
    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(getURI(), buffer).writeBoolean(started);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        setURI(serializer.readObject(buffer));
        started = buffer.readBoolean();
        executorService = ForkJoinPool.commonPool();
    }

    @Override
    public void setExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }
    */
}
