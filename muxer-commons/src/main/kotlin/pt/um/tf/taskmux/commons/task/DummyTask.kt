package pt.um.tf.taskmux.commons.task

import pt.um.tf.taskmux.commons.URIGenerator
import pt.um.tf.taskmux.commons.error.MissingExecutorException

import java.util.Random
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

class DummyTask(u : URIGenerator = URIGenerator()) : AsyncTask<Long>(u) {
    private companion object {
       val LOOPS_TO_DO = 16384
    }
    private var cp : CompletableFuture<Result<Long>>? = null
    private var started : Boolean = false

    override fun start(): CompletableFuture<Result<Long>> {
        if(!started) {
            started = true
            startUpTask()
        }
        return cp as CompletableFuture<Result<Long>>
    }

    private fun startUpTask() {
        cp = if(executor == null || executor!!.isTerminated) {
            CompletableFuture.failedFuture(MissingExecutorException())
        }
        else {
            CompletableFuture.supplyAsync(dummyTask(), executor)
        }
    }

    private fun dummyTask() : Supplier<Result<Long>> {
        return Supplier {
            val r = Random(uri.path?.chars()?.asLongStream()?.sum() ?: 0)
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
        return started && cp != null && !cp!!.isCancelled && !cp!!.isDone
    }

    override fun cancel() {
        if(started) {
            if (cp != null && !cp!!.isDone){
                cp?.cancel(true)
            }
        }
    }
}
