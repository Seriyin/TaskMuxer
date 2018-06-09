package pt.um.tf.commons.task;

public interface Result<T> {
    T completeWithResult();
    boolean completedSuccessfully();
    Exception completedWithException();
}
