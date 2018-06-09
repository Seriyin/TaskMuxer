package pt.um.tf.commons.task;

import io.atomix.catalyst.serializer.CatalystSerializable;

import java.net.URL;

public interface Task<T> extends CatalystSerializable {
    URL getURL();
    T start();
    boolean completed();
    void cancel();
}
