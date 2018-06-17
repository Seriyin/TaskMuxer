package pt.um.tf.commons.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;

public class HeyMessage implements CatalystSerializable {
    private Address address;

    protected HeyMessage() {}

    public HeyMessage(Address address) {
        this.address = address;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(buffer);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        address = serializer.readObject(buffer);
    }
}
