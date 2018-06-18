package pt.um.tf.taskmux.server.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.Serializer;
import pt.um.tf.commons.task.Task;
import spread.SpreadGroup;

import java.util.List;

public class ClearMessage implements StateMessage {
    private List<Task> backToInbound;
    private SpreadGroup clear;

    public ClearMessage(List<Task> backToInbound, SpreadGroup clear) {
        this.backToInbound = backToInbound;
        this.clear = clear;
    }

    protected ClearMessage() {
    }

    public List<Task> getBackToInbound() {
        return backToInbound;
    }

    public SpreadGroup getClear() {
        return clear;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(backToInbound);
        serializer.writeObject(clear);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        backToInbound = serializer.readObject(buffer);
        clear = serializer.readObject(buffer);
    }
}
