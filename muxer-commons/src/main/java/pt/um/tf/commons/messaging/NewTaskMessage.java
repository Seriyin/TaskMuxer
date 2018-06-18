package pt.um.tf.commons.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import pt.um.tf.commons.task.Task;

public class NewTaskMessage implements CommonMessage {
    private Task task;


    public NewTaskMessage(Task task) {
        this.task = task;
    }

    protected NewTaskMessage() {}



    public Task getTask() {
        return task;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        task.writeObject(buffer, serializer);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        task = serializer.readObject(buffer);
    }
}
