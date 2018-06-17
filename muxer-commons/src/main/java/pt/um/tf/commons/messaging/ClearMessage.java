package pt.um.tf.commons.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class ClearMessage implements CatalystSerializable {
    private String left;

    public ClearMessage(String left) {

    }

    public String getLeft() {
        return left;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        buffer.writeString(left);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        left = buffer.readString();
    }
}
