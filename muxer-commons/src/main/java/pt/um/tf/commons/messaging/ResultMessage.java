package pt.um.tf.commons.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import pt.um.tf.commons.task.Result;

public class ResultMessage implements CommonMessage {
    private Result result;

    public ResultMessage(Result result) {
        this.result = result;
    }

    protected ResultMessage() {
        this.result = result;
    }

    public Result getResult() {
        return result;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(result);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        result = serializer.readObject(buffer);
    }
}
