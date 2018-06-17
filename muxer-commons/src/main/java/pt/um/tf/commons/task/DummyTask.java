package pt.um.tf.commons.task;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import pt.um.tf.commons.Utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DummyTask implements AsyncTask<Long> {
    private static Logger LOGGER = Logger.getLogger(DummyTask.class.getName());
    private static long LOOPS_TO_DO = 65536;
    private URL url;
    private Address address;
    private CompletableFuture<Result<Long>> cp;
    private boolean started;
    private ExecutorService executorService;

    public DummyTask(Address address) {
        this.address = address;
        generateURL();
        started = false;
    }

    protected DummyTask() {}

    @Override
    public URL getURL() {
        return url;
    }

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
        var r = new Random(url.getPath().chars().asLongStream().sum());
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

    private void generateURL() {
        var u = new Utils();
        try {
            url = new URI("tcp",
                          null,
                          address.host(),
                          address.port(),
                          "/dummytask" + u.getSHA256(u.generateRandomUrlPostfix()),
                          null,
                          null).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
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
        serializer.writeObject(url, buffer);
        serializer.writeObject(address, buffer);
        buffer.writeBoolean(started);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        url = serializer.readObject(buffer);
        address = serializer.readObject(buffer);
        started = buffer.readBoolean();
        executorService = ForkJoinPool.commonPool();
    }

    @Override
    public void setExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

}
