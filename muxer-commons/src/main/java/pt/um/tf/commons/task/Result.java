package pt.um.tf.commons.task;

import io.atomix.catalyst.serializer.CatalystSerializable;

public interface Result<T> extends CatalystSerializable {
    T completeWithResult();
    boolean completedSuccessfully();
    Exception completedWithException();
}
