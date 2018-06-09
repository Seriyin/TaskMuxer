package pt.um.tf.commons.task;

public class MissingExecutorException extends Exception {
    @Override
    public String getMessage() {
        return "Missing Executor to execute task in.";
    }
}
