package pt.um.tf.commons.task;

import io.atomix.catalyst.serializer.CatalystSerializable;

public abstract class Result<T> implements CatalystSerializable {
    abstract public T completeWithResult();
    abstract public boolean completedSuccessfully();
    abstract public Exception completedWithException();
}
