package pt.um.tf.taskmux.client;

import pt.haslab.ekit.Spread;
import pt.um.tf.taskmux.commons.URIGenerator;
import pt.um.tf.taskmux.commons.messaging.NewTaskMessage;
import pt.um.tf.taskmux.commons.task.DummyTask;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.TimerTask;

public class TaskGenerator extends TimerTask {
    private final Spread spread;
    private final SpreadGroup mainGroup;
    private final URIGenerator u;

    public TaskGenerator(Spread spread,
                         SpreadGroup mainGroup,
                         URIGenerator u) {

        this.spread = spread;
        this.mainGroup = mainGroup;
        this.u = u;
    }

    public void run() {
        var spm = new SpreadMessage();
        spm.setSafe();
        spm.addGroup(mainGroup);
        spread.multicast(spm, new NewTaskMessage(new DummyTask(u)));
    }
}
