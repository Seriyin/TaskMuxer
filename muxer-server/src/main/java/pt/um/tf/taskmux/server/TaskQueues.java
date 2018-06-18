package pt.um.tf.taskmux.server;

import pt.um.tf.taskmuxer.commons.IndexedDeque;
import pt.um.tf.taskmuxer.commons.error.DuplicateException;
import pt.um.tf.taskmuxer.commons.task.Result;
import pt.um.tf.taskmuxer.commons.task.Task;

import java.net.URL;
import java.util.*;
import java.util.stream.IntStream;

public class TaskQueues {
    private final IndexedDeque<Task> inbound;
    private final Map<String, Map<URL, Task>> outbound;
    private final Map<String, Iterator<Map.Entry<String, Map<URL, Task>>>> iteratorMap;

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
        outbound.get(toClear).clear();
        outbound.remove(toClear);
    }

    public void replaceAfterCompleted(String sender, Collection<Task> tasks) {
        var map = outbound.get(sender);
        outbound.put(sender, tasks.stream()
                                  .collect(HashMap::new,
                                           (t, r) -> t.put(r.getURL(), r),
                                           HashMap::putAll));
    }

    public void removeAndSet(Collection<Task> in, String sender, int count) {
        var map = outbound.remove(sender);
        Map<URL, Task> nMap = in.stream()
                                .collect(HashMap::new,
                                         (h, t) -> h.put(t.getURL(), t),
                                         HashMap::putAll);
        outbound.put(sender, nMap);
        in.forEach(it -> map.put(it.getURL(), it));
        IntStream.range(0, count)
                 .forEach(i -> inbound.removeFirst());
    }

    public Result<Task> sendToOutbound(String sender) {
        Result<Task> taskResult;
        if (!outbound.containsKey(sender)) {
            outbound.put(sender, new HashMap<>());
        }
        var in = inbound.getFirst();
        var out = outbound.get(sender);
        if (!out.containsKey(in.getURL())) {
            out.put(in.getURL(), in);
            taskResult = new TaskResult(true, in, null);
        }
        else {
            taskResult = new TaskResult(false, null, new DuplicateException());
        }
        return taskResult;
    }

    public Optional<ArrayList<Task>> backToInbound(String mem) {
        Optional<ArrayList<Task>> backToInbound;
        if (outbound.containsKey(mem)) {
            var out = outbound.get(mem);
            backToInbound = Optional.of(new ArrayList<>(out.size()));
            if(!out.isEmpty()) {
                out.forEach((a, task) -> {
                    inbound.addFirst(task);
                    backToInbound.ifPresent(l -> l.add(task));
                });
                out.clear();
            }
            outbound.remove(mem);
        }
        else {
            backToInbound = Optional.empty();
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

    public Iterator<Map.Entry<String, Map<URL, Task>>> getOutboundIterator(String receiver) {
        Iterator<Map.Entry<String, Map<URL, Task>>> it;
        if(!iteratorMap.containsKey(receiver)) {
            it = outbound.entrySet().iterator();
            iteratorMap.put(receiver, it);
        }
        else {
            it = iteratorMap.get(receiver);
        }
        return it;
    }

    public Optional<Collection<Task>> purgeOutbound(String sender, URL url) {
        Optional<Collection<Task>> tasks = Optional.empty();
        if (outbound.containsKey(sender)) {
            var out = outbound.get(sender);
            if (out.remove(url) != null) {
                tasks = Optional.of(outbound.get(sender).values());
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
            t = Optional.of(outbound.get(sender).values());
        }
        return t;
    }

    public void wipe() {
        inbound.clear();
        outbound.clear();
    }
}
