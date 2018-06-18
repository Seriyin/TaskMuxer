package pt.um.tf.taskmux.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.haslab.ekit.Spread;
import pt.um.tf.taskmuxer.commons.messaging.UrlMessage;
import pt.um.tf.taskmuxer.commons.task.DummyTask;
import pt.um.tf.taskmuxer.commons.task.Result;
import pt.um.tf.taskmuxer.commons.task.Task;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * The main runner of tasks.
 * Informs completion of tasks directly through to spread.
 */
public class TaskRunner {
    private final static Logger LOGGER = LoggerFactory.getLogger(TaskRunner.class);
    private Set<Task> runningTasks;
    private ExecutorService taskPool;
    private Spread toReport;
    private SpreadGroup maingroup;
    private int barrier;

    public TaskRunner(ExecutorService taskPool,
                      int barrier,
                      Spread toReport) {
        runningTasks = new HashSet<>();
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
                  .thenAcceptAsync(this::handler, taskPool)
                  .thenRunAsync(() -> sendURLMessage(dt), taskPool)
                  .exceptionally(th -> {
                      LOGGER.error("", th);
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

    private void handler(Result<Long> longResult) {
        //Result doesn't really matter.
        if(!longResult.completedSuccessfully()) {
            LOGGER.error("Failed task :", longResult.completedWithException());
        }
    }

    private void sendURLMessage(Task ran) {
        var spm = new SpreadMessage();
        spm.addGroup(maingroup);
        spm.setSafe();
        toReport.multicast(spm, new UrlMessage(ran.getURL()));
        runningTasks.remove(ran);
    }

    public boolean isBackedUp() {
        return runningTasks.size() >= barrier;
    }

}
