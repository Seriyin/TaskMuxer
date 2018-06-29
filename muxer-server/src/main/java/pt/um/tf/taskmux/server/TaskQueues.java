package pt.um.tf.taskmux.server;

import pt.um.tf.taskmux.commons.IndexedDeque;
import pt.um.tf.taskmux.commons.error.DuplicateException;
import pt.um.tf.taskmux.commons.error.NoAssignableTasksException;
import pt.um.tf.taskmux.commons.task.Result;
import pt.um.tf.taskmux.commons.task.Task;

import java.net.URI;
import java.util.*;
import java.util.stream.IntStream;

public class TaskQueues {
    private final IndexedDeque<Task> inbound;
    private final Map<String, Map<URI, Task>> outbound;
    private final Map<String, Iterator<Map.Entry<String, Map<URI, Task>>>> iteratorMap;

    public TaskQueues() {
        inbound = new IndexedDeque<>();
        outbound = new HashMap<>();
        iteratorMap = new HashMap<>();
    }


    public void addToFrontInbound(Task task) {
        inbound.addFirst(task);
    }

    public void addAllToBackInbound(Collection<Task> tasks) {
        tasks.forEach(inbound::addLast);
    }

    public void clearTasks(String toClear) {
        if (outbound.containsKey(toClear)) {
            outbound.get(toClear).clear();
            outbound.remove(toClear);
        }
    }

    public void replaceAfterCompleted(String sender, Collection<Task> tasks) {
        var map = outbound.get(sender);
        outbound.put(sender, tasks.stream()
                                  .collect(HashMap::new,
                                           (t, r) -> t.put(r.getURI(), r),
                                           HashMap::putAll));
    }

    public void replaceAfterGet(Collection<Task> in, String sender, int count) {
        Map<URI, Task> nMap = in.stream()
                                .collect(HashMap::new,
                                         (h, t) -> h.put(t.getURI(), t),
                                         HashMap::putAll);
        outbound.put(sender, nMap);
        IntStream.range(0, count)
                 .forEach(i -> inbound.removeFirst());
    }

    public Result<Task> sendToOutbound(String sender) {
        Result<Task> taskResult;
        if (!outbound.containsKey(sender)) {
            outbound.put(sender, new HashMap<>());
        }
        if(inboundIsEmpty()) {
            taskResult = new TaskResult(false, null, new NoAssignableTasksException());
        }
        else {
            var in = inbound.removeFirst();
            var out = outbound.get(sender);
            if (!out.containsKey(in.getURI())) {
                out.put(in.getURI(), in);
                taskResult = new TaskResult(true, in, null);
            }
            else {
                taskResult = new TaskResult(false, null, new DuplicateException());
            }
        }
        return taskResult;
    }

    public Optional<ArrayList<Task>> backToInbound(String mem) {
        Optional<ArrayList<Task>> backToInbound = Optional.empty();
        if (outbound.containsKey(mem)) {
            var out = outbound.get(mem);
            if(!out.isEmpty()) {
                var newArr = new ArrayList<Task>(out.size());
                out.forEach((a, task) -> {
                    inbound.addFirst(task);
                    newArr.add(task);
                });
                backToInbound = Optional.of(newArr);
                out.clear();
            }
            outbound.remove(mem);
        }
        return backToInbound;
    }

    public void sendToInbound(Task task) {
        inbound.addLast(task);
    }

    public boolean AreBothEmpty() {
        return inbound.isEmpty() && outbound.isEmpty();
    }

    public boolean inboundIsEmpty() {
        return inbound.isEmpty();
    }

    public boolean outboundIsEmpty() {
        return outbound.isEmpty();
    }

    public Iterator<Map.Entry<String, Map<URI, Task>>> getOutboundIterator(String receiver) {
        Iterator<Map.Entry<String, Map<URI, Task>>> it;
        if(!iteratorMap.containsKey(receiver)) {
            it = outbound.entrySet().iterator();
            iteratorMap.put(receiver, it);
        }
        else {
            it = iteratorMap.get(receiver);
        }
        return it;
    }

    public Optional<Collection<Task>> purgeOutbound(String sender, URI url) {
        Optional<Collection<Task>> tasks = Optional.empty();
        if (outbound.containsKey(sender)) {
            var out = outbound.get(sender);
            if (out.remove(url) != null) {
                tasks = Optional.of(new ArrayList<>(outbound.get(sender).values()));
            }
        }
        return tasks;
    }

    public Collection<Task> getInbound() {
        return new ArrayList<>(inbound);
    }

    public Optional<Collection<Task>> getOutbound(String sender) {
        Optional<Collection<Task>> t = Optional.empty();
        if(outbound.containsKey(sender)) {
            t = Optional.of(new ArrayList<>(outbound.get(sender).values()));
        }
        return t;
    }

    public void wipe() {
        inbound.clear();
        outbound.clear();
    }

    public void purgeIterator(String receiver) {
        iteratorMap.remove(receiver);
    }

}
