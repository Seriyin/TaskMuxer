package pt.um.tf.taskmux.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.haslab.ekit.Spread;
import pt.um.tf.taskmux.commons.messaging.GetTaskMessage;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.TimerTask;

public class TaskAssigner extends TimerTask {
    private final static Logger LOGGER = LoggerFactory.getLogger(TaskAssigner.class);
    private final Spread spread;
    private final SpreadGroup mainGroup;
    private final TaskRunner runner;


    public TaskAssigner(Spread spread, SpreadGroup mainGroup, TaskRunner t) {
        this.spread = spread;
        this.mainGroup = mainGroup;
        runner = t;
    }

    @Override
    public void run() {
        if (!runner.isBackedUp()) {
            var spm = new SpreadMessage();
            spm.addGroup(mainGroup);
            spm.setSafe();
            spread.multicast(spm, new GetTaskMessage());
        }
        else {
            LOGGER.info("Runner is backed up");
        }
    }
}
