package pt.um.tf.taskmux.commons.task;

import io.atomix.catalyst.serializer.CatalystSerializable;

import java.io.Serializable;

public abstract class Result<T> implements Serializable { //implements CatalystSerializable
    abstract public T completeWithResult();
    abstract public boolean completedSuccessfully();
    abstract public Exception completedWithException();
}
