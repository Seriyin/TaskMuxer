package pt.um.tf.commons.task;

import io.atomix.catalyst.serializer.CatalystSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.um.tf.commons.URLGenerator;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public abstract class Task<T> implements CatalystSerializable {
    private final static Logger LOGGER = LoggerFactory.getLogger(Task.class);
    private URL url;

    public Task(String id) {
        generateUrl(id);
    }

    protected Task() {}

    private void generateUrl(String id) {
        var u = new URLGenerator(id);
        try {
            url = new URI("tcp",
                          null,
                          "localhost",
                          4803,
                          "dummytask" + u.generateURLPostFix(),
                          null,
                          null).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            LOGGER.error("", e.getMessage());
        }
    }

    public URL getURL() {
        return url;
    }

    protected void setUrl(URL url) {
        this.url = url;
    }


    abstract public T start();
    abstract public boolean completed();
    abstract public void cancel();

}
