package de.artemis.omniwheel.client.render;

import java.util.*;
import java.util.function.Function;

/** Visual snapshots let deleted rows collapse after the data has already been saved. */
public final class ListRowAnimation<T> {
    private static final long DURATION = 180_000_000L;
    private final List<Node<T>> nodes = new ArrayList<>();
    private String context;
    private static final class Node<T> {
        String id; T value; long changed; float from = 1; boolean removing;
        Node(String id, T value, long changed, float from) { this.id=id; this.value=value; this.changed=changed; this.from=from; }
        float size(long now) {
            float progress = (float) de.artemis.omniwheel.common.Numbers.clamp((now-changed)/(double)DURATION, 0, 1);
            float ease = progress*progress*(3-2*progress);
            return from + ((removing ? 0 : 1)-from)*ease;
        }
    }
    public record Row<T>(T value, String id, float offset, float size, boolean removing) {}

    public List<Row<T>> update(String view, List<T> values, Function<T,String> key, long now, boolean immediate) {
        boolean reset = immediate || !Objects.equals(context, view);
        if (reset) { nodes.clear(); context=view; }
        Map<String,T> current = new LinkedHashMap<>();
        for (T value : values) current.put(key.apply(value),value);
        for (Node<T> node : nodes) {
            boolean removing = !current.containsKey(node.id);
            if (removing != node.removing) {
                node.from=node.size(now); node.changed=now; node.removing=removing;
            }
            if (!removing) node.value=current.get(node.id);
        }
        nodes.removeIf(node -> node.removing && node.size(now) <= .001F);
        List<Node<T>> ordered = new ArrayList<>();
        for (var entry : current.entrySet()) {
            Node<T> node=nodes.stream().filter(n -> n.id.equals(entry.getKey())).findFirst().orElse(null);
            if (node==null) node=new Node<>(entry.getKey(),entry.getValue(),now,reset ? 1 : 0);
            ordered.add(node);
        }
        for (int index=0;index<nodes.size();index++) {
            Node<T> node=nodes.get(index);
            if (node.removing) ordered.add(Math.min(index,ordered.size()),node);
        }
        nodes.clear(); nodes.addAll(ordered);
        List<Row<T>> result=new ArrayList<>();
        float offset=0;
        for (Node<T> node : nodes) {
            float size=node.size(now);
            result.add(new Row<>(node.value,node.id,offset,size,node.removing));
            offset+=36*size;
        }
        return result;
    }
}
