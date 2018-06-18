package pt.um.tf.taskmux.server.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.Serializer;
import pt.um.tf.taskmuxer.commons.task.Task;

import java.util.List;

public class ClearMessage implements StateMessage {
    private List<Task> backToInbound;
    private String clear;

    public ClearMessage(List<Task> backToInbound, String clear) {
        this.backToInbound = backToInbound;
        this.clear = clear;
    }

    protected ClearMessage() {
    }

    public List<Task> getBackToInbound() {
        return backToInbound;
    }

    public String getClear() {
        return clear;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(backToInbound);
        buffer.writeString(clear);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        backToInbound = serializer.readObject(buffer);
        clear = buffer.readString();
    }
}
