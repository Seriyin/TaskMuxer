package pt.um.tf.taskmux.commons.task;

import pt.um.tf.taskmux.commons.error.MissingExecutorException;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier

class DummyTask(id : String? = null) : AsyncTask<Long>(id) {
    private companion object {
       val LOOPS_TO_DO = 16384
    }
    private var cp : CompletableFuture<Result<Long>> = CompletableFuture()
    private var started : Boolean = false

    override fun start(): CompletableFuture<Result<Long>> {
        if(!started) {
            started = true
            startUpTask()
        }
        return cp
    }

    private fun startUpTask() {
        if(getExecutor().isTerminated) {
            cp = CompletableFuture.failedFuture(MissingExecutorException())
        }
        else {
            cp = CompletableFuture.supplyAsync(dummyTask(), getExecutor())
        }
    }

    private fun dummyTask() : Supplier<Result<Long>> {
        return Supplier {
            val r = Random(getURI()?.path?.chars()?.asLongStream()?.sum() ?: 0)
            var id = 0L
            //Might intentionally divide by zero.
            //This simulates a possibly throwing long running background task.
            return@Supplier try{
                for(i in 0..LOOPS_TO_DO) {
                    id += r.nextInt()
                    id shl 3
                    id *= 22
                    id += 2
                    id /= r.nextInt()
                    Thread.sleep(2, 800)
                }
                DummyResult(id)
            } catch (e : Exception) {
                DummyResult(e = e)
            }
        }
    }


    override fun completed(): Boolean {
        return started && !cp.isCancelled && cp.isDone
    }

    override fun cancel() {
        if(started) {
            if (!cp.isDone) {
                cp.cancel(true)
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
