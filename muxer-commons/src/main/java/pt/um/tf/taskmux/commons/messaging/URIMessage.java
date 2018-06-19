package pt.um.tf.taskmux.commons.messaging;

import java.net.URI;

public class URIMessage implements CommonMessage {
    private URI url;

    public URIMessage(URI url) {
        this.url = url;
    }

    protected URIMessage() {}

    public URI getURI() {
        return url;
    }

    /*
    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(url);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        url = serializer.readObject(buffer);
    }
    */
}
