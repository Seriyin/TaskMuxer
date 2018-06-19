package pt.um.tf.taskmux.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.haslab.ekit.Spread;
import pt.um.tf.taskmux.commons.messaging.URIMessage;
import pt.um.tf.taskmux.commons.task.DummyTask;
import pt.um.tf.taskmux.commons.task.Result;
import pt.um.tf.taskmux.commons.task.Task;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

/**
 * The main runner of tasks.
 * Informs completion of tasks directly through to spread.
 */
public class TaskRunner {
    private final static Logger LOGGER = LoggerFactory.getLogger(TaskRunner.class);
    private Queue<Task> runningTasks;
    private ExecutorService taskPool;
    private Spread toReport;
    private SpreadGroup maingroup;
    private int barrier;

    public TaskRunner(ExecutorService taskPool,
                      int barrier,
                      Spread toReport) {
        runningTasks = new ArrayDeque<>(barrier);
        this.taskPool = taskPool;
        this.maingroup = null;
        this.toReport = toReport;
        this.barrier = barrier;
    }

    /**
     * Joins aren't immediately effective.
     * @param maingroup
     */
    public void setMaingroup(SpreadGroup maingroup) {
        this.maingroup = maingroup;
    }

    public boolean runTask(Task t) {
        var res = false;
        if (!isBackedUp()) {
            if (t instanceof DummyTask) {
                var dt = (DummyTask) t;
                dt.setExecutor(taskPool);
                dt.start()
                  .thenAcceptAsync(r -> handler(dt, r), taskPool)
                  .exceptionally(th -> {
                      LOGGER.error("", th);
                      runningTasks.remove();
                      return null;
                  });
                runningTasks.add(t);
                res = true;
            }
            else {
                LOGGER.error("Unknown task type");
            }
        }
        return res;
    }

    private void handler(DummyTask dt, Result<Long> longResult) {
        //Result doesn't really matter.
        if(!longResult.completedSuccessfully()) {
            LOGGER.error("Failed task :", longResult.completedWithException());
        }
        LOGGER.info("Completed task : " + longResult.completeWithResult());
        runningTasks.remove();
        sendURLMessage(dt);
    }

    private void sendURLMessage(Task ran) {
        var spm = new SpreadMessage();
        spm.addGroup(maingroup);
        spm.setSafe();
        toReport.multicast(spm, new URIMessage(ran.getURI()));
    }

    public boolean isBackedUp() {
        return runningTasks.size() >= barrier;
    }

}
