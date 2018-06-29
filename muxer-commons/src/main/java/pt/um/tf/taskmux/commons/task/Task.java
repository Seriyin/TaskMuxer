package pt.um.tf.taskmux.commons.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.um.tf.taskmux.commons.URIGenerator;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

public abstract class Task<T> implements Serializable {//implements CatalystSerializable {
    private final static Logger LOGGER = LoggerFactory.getLogger(Task.class);
    private URI uri;

    public Task(URIGenerator u) {
        try{
            uri = new URI("tcp",
                          null,
                          "localhost",
                          4803,
                          "/dummytask" + u.generateURLPostFix(),
                          null,
                          null);
        } catch (URISyntaxException e) {
            LOGGER.error("", e);
        }
    }

    protected Task() {}

    public URI getURI() {
        return uri;
    }

    protected void setURI(URI url) {
        this.uri = url;
    }


    abstract public T start();
    abstract public boolean completed();
    abstract public void cancel();

}
