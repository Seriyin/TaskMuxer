package pt.um.tf.taskmux.server;

import spread.SpreadGroup;

import java.util.*;

public class TrackedGroups {
    private final TreeMap<String, SpreadGroup> known;
    private final Map<String, SpreadGroup> tracking;

    public TrackedGroups() {
        known = new TreeMap<>();
        tracking = new HashMap<>();
    }

    public void registerKnown(SpreadGroup privateGroup) {
        known.putIfAbsent(privateGroup.toString(), privateGroup);
    }

    public void registerKnown(List<SpreadGroup> privateGroups) {
        privateGroups.forEach(p -> known.putIfAbsent(p.toString(), p));
    }

    public void purgeKnown(String left) {
        known.remove(left);
    }

    public void purgeKnown(SpreadGroup left) {
        known.remove(left.toString());
    }

    /**
     * Every Server will contain at minimum it's own group.
     * @return The minimum id private group.
     */
    public SpreadGroup getMinKnown() {
        return known.firstEntry().getValue();
    }


    public void registerTracked(SpreadGroup receiver) {
        tracking.putIfAbsent(receiver.toString(), receiver);
    }


    public void registerTracked(List<SpreadGroup> receiver) {
        Map<String, SpreadGroup> r = receiver.stream()
                                             .collect(HashMap::new,
                                                      (t, re) -> t.put(re.toString(), re),
                                                      HashMap::putAll);
        tracking.putAll(r);
    }

    public void purgeTracked(String receiver) {
        tracking.remove(receiver);
    }

    public void purgeTracked(SpreadGroup receiver) {
        tracking.remove(receiver.toString());
    }

    public SpreadGroup getKnown(String receiver) {
        return known.getOrDefault(receiver, null);
    }

    public SpreadGroup getTracked(String receiver) {
        return tracking.getOrDefault(receiver, null);
    }

    public boolean isEmptyTracked() {
        return tracking.isEmpty();
    }

    public Set<String> getTracked() {
        return new HashSet<>(tracking.keySet());
    }
}
