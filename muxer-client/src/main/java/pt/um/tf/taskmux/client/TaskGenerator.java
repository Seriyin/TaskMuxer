package pt.um.tf.taskmux.client;

import pt.haslab.ekit.Spread;
import pt.um.tf.commons.messaging.NewTaskMessage;
import pt.um.tf.commons.messaging.TaskMessage;
import pt.um.tf.commons.task.DummyTask;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.TimerTask;

public class TaskGenerator extends TimerTask {
    private final Spread spread;
    private final SpreadGroup mainGroup;
    private final String me;

    public TaskGenerator(Spread spread,
                         SpreadGroup mainGroup,
                         String me) {

        this.spread = spread;
        this.mainGroup = mainGroup;
        this.me = me;
    }

    public void run() {
        var spm = new SpreadMessage();
        spm.setSafe();
        spm.addGroup(mainGroup);
        spread.multicast(spm, new NewTaskMessage(new DummyTask(me)));
    }
}
