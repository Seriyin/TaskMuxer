import java.net.URL;
import java.util.concurrent.CompletableFuture;

public interface Task {
    URL getURL();
    CompletableFuture<Result> start();
    boolean completed();
    void complete();
}
