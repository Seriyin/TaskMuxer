public interface Result {
    <T> T completeWithResult();
    boolean completedSuccessfully();
    Exception completedExceptionally();
}
