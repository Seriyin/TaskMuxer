package pt.um.tf.taskmux.server;

import pt.um.tf.taskmuxer.commons.IndexedDeque;
import pt.um.tf.taskmux.server.messaging.StateMessage;

import java.util.function.Consumer;

public class TrackedMessages {
    private final IndexedDeque<StateMessage> sms;
    private final int lastIndex;

    public TrackedMessages(IndexedDeque<StateMessage> sms, int lastIndex) {
        this.sms = sms;
        this.lastIndex = lastIndex;
    }

    public TrackedMessages() {
        sms = new IndexedDeque<>();
        lastIndex = 0;
    }

    public void add(StateMessage m) {
        sms.addLast(m);
    }

    public void wipe() {
        sms.clear();
    }

    public void handleAll(Consumer<StateMessage> handler) {
        sms.forEach(handler);
        sms.clear();
    }
}
