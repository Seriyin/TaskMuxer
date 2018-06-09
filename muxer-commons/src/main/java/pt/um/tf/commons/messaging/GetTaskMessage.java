package pt.um.tf.commons.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import pt.um.tf.commons.task.Task;

public class GetTaskMessage<T> implements CatalystSerializable {
    private Task<T> t;

    public GetTaskMessage(Task<T> t) {
        this.t = t;
    }

    public Task<T> getTask() {
        return t;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        t.writeObject(buffer, serializer);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        t = serializer.readObject(buffer);
    }
}
