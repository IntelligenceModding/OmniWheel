package de.artemis.omniwheel.client.render;

import java.util.*;
import java.util.function.Function;

/**
 * Visual snapshots let deleted rows collapse after the data has already been saved.
 */
public final class ListRowAnimation<T> {

    private static final long DURATION = 180_000_000L;

    private final List<Node<T>> nodes = new ArrayList<>();

    private String context;

    private static final class Node<T> {

        String id;

        T value;

        long changed;

        float from = 1;

        boolean removing;

        Node(String id, T value, long changed, float from) {
            this.id = id;
            this.value = value;
            this.changed = changed;
            this.from = from;
        }

        float size(long now) {
            float progress = (float) de.artemis.omniwheel.common.Numbers.clamp((now - changed) / (double) DURATION, 0, 1);
            float ease = progress * progress * (3 - 2 * progress);
            return from + ((removing ? 0 : 1) - from) * ease;
        }
    }

    public final static class Row<T> {

        private final T value;

        private final String id;

        private final float offset;

        private final float size;

        private final boolean removing;

        public Row(T value, String id, float offset, float size, boolean removing) {
            this.value = value;
            this.id = id;
            this.offset = offset;
            this.size = size;
            this.removing = removing;
        }

        public T value() {
            return value;
        }

        public String id() {
            return id;
        }

        public float offset() {
            return offset;
        }

        public float size() {
            return size;
        }

        public boolean removing() {
            return removing;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value)
                return true;
            if (value == null || value.getClass() != getClass())
                return false;
            Row other = (Row) value;
            return true && java.util.Objects.equals(value, other.value) && java.util.Objects.equals(id, other.id) && java.util.Objects.equals(offset, other.offset) && java.util.Objects.equals(size, other.size) && java.util.Objects.equals(removing, other.removing);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(value, id, offset, size, removing);
        }

        @Override
        public String toString() {
            return "Row[" + "value=" + value + ", id=" + id + ", offset=" + offset + ", size=" + size + ", removing=" + removing + "]";
        }
    }

    public List<Row<T>> update(String view, List<T> values, Function<T, String> key, long now, boolean immediate) {
        boolean reset = immediate || !Objects.equals(context, view);
        if (reset) {
            nodes.clear();
            context = view;
        }
        Map<String, T> current = new LinkedHashMap<>();
        for (T value : values) current.put(key.apply(value), value);
        for (Node<T> node : nodes) {
            boolean removing = !current.containsKey(node.id);
            if (removing != node.removing) {
                node.from = node.size(now);
                node.changed = now;
                node.removing = removing;
            }
            if (!removing)
                node.value = current.get(node.id);
        }
        nodes.removeIf(node -> node.removing && node.size(now) <= .001F);
        List<Node<T>> ordered = new ArrayList<>();
        for (java.util.Map.Entry<java.lang.String, T> entry : current.entrySet()) {
            Node<T> node = nodes.stream().filter(n -> n.id.equals(entry.getKey())).findFirst().orElse(null);
            if (node == null)
                node = new Node<>(entry.getKey(), entry.getValue(), now, reset ? 1 : 0);
            ordered.add(node);
        }
        for (int index = 0; index < nodes.size(); index++) {
            Node<T> node = nodes.get(index);
            if (node.removing)
                ordered.add(Math.min(index, ordered.size()), node);
        }
        nodes.clear();
        nodes.addAll(ordered);
        List<Row<T>> result = new ArrayList<>();
        float offset = 0;
        for (Node<T> node : nodes) {
            float size = node.size(now);
            result.add(new Row<>(node.value, node.id, offset, size, node.removing));
            offset += 36 * size;
        }
        return result;
    }
}
