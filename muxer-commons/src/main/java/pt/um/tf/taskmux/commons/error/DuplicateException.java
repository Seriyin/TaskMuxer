package pt.um.tf.taskmux.commons.error;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class DuplicateException extends Exception implements CatalystSerializable {
    @Override
    public String getMessage() {
        return "Duplicate task present, retry get";
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {}

}
