package pt.um.tf.commons.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;

import java.net.URL;

public class UrlMessage implements CommonMessage {
    private URL url;

    public UrlMessage(URL url) {
        this.url = url;
    }

    protected UrlMessage() {}

    public URL getUrl() {
        return url;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(url);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        url = serializer.readObject(buffer);
    }
}
