package pt.um.tf.taskmux.server.messaging;

import pt.um.tf.taskmux.commons.task.Task;

import java.util.Collection;

public interface SequencedMessage extends StateMessage {
    long getSequence();
    boolean hasMore();
    String getReceiver();
    Collection<Task> getTasks();

}