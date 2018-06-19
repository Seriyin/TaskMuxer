package pt.um.tf.taskmux.commons.messaging;

import pt.um.tf.taskmux.commons.task.Result;

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

    /*
    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(result);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        result = serializer.readObject(buffer);
    }
    */
}
