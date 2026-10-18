package de.artemis.omniwheel.client.navigation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Layout-based focus traversal. Coordinates and scroll visibility come from the UI, not control names. */
public final class ManagementNavigation {
    private ManagementNavigation() {}

    public enum Direction { LEFT, RIGHT, UP, DOWN }

    public record Bounds(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public double centerX() { return x + width / 2.0; }
        public double centerY() { return y + height / 2.0; }
    }

    public record Node(String id, int panel, String group, Bounds bounds, boolean visible, double order) {}

    public static Node move(List<Node> nodes, Node current, Direction direction) {
        boolean vertical = direction == Direction.UP || direction == Direction.DOWN;
        // A scroll container is traversed in full before leaving it, including its offscreen rows.
        if (vertical && !current.group().isEmpty()) {
            Node next = nearest(nodes, current, direction, true, false);
            if (next != null) return next;
        }
        Node next = nearest(nodes, current, direction, false, !vertical);
        if (next != null) {
            if (direction == Direction.DOWN && !next.group().isEmpty() && !next.group().equals(current.group())) {
                String group = next.group();
                return readingOrder(nodes).stream().filter(node -> node.panel() == current.panel() && node.group().equals(group)).findFirst().orElse(next);
            }
            if (direction == Direction.UP && !current.group().isEmpty() && !current.group().equals(next.group())) {
                int headerY = next.bounds().y();
                return readingOrder(nodes).stream().filter(node -> node.panel() == current.panel() && node.visible()
                        && node.bounds().y() == headerY && !node.group().equals(current.group())).findFirst().orElse(next);
            }
            return next;
        }
        if (vertical) return current;

        int delta = direction == Direction.LEFT ? -1 : 1;
        int panel = current.panel() + delta;
        return nodes.stream().filter(node -> node.panel() == panel && node.visible())
                .min(Comparator.comparingDouble((Node node) -> Math.abs(node.bounds().centerY() - current.bounds().centerY()))
                        .thenComparingDouble(Node::order)).orElse(current);
    }

    private static Node nearest(List<Node> nodes, Node current, Direction direction, boolean sameGroup, boolean requireLane) {
        return nodes.stream().filter(node -> !node.id().equals(current.id()) && node.panel() == current.panel())
                .filter(node -> sameGroup ? Objects.equals(node.group(), current.group()) : node.visible())
                .filter(node -> inDirection(current.bounds(), node.bounds(), direction))
                .filter(node -> !requireLane || overlaps(current.bounds(), node.bounds(), direction))
                .min(Comparator.comparingInt((Node node) -> overlaps(current.bounds(), node.bounds(), direction) ? 0 : 1)
                        .thenComparingDouble(node -> primary(current.bounds(), node.bounds(), direction))
                        .thenComparingDouble(node -> secondary(current.bounds(), node.bounds(), direction))
                        .thenComparingDouble(Node::order)).orElse(null);
    }

    private static boolean inDirection(Bounds from, Bounds to, Direction direction) {
        return switch (direction) {
            case LEFT -> to.centerX() < from.centerX() - 1 && to.right() <= from.x() + 1;
            case RIGHT -> to.centerX() > from.centerX() + 1 && to.x() >= from.right() - 1;
            case UP -> to.centerY() < from.centerY() - 1;
            case DOWN -> to.centerY() > from.centerY() + 1;
        };
    }

    private static boolean overlaps(Bounds a, Bounds b, Direction direction) {
        return switch (direction) {
            case LEFT, RIGHT -> a.y() < b.bottom() && b.y() < a.bottom();
            case UP, DOWN -> a.x() < b.right() && b.x() < a.right();
        };
    }

    private static double primary(Bounds a, Bounds b, Direction direction) {
        return switch (direction) {
            case LEFT -> Math.max(0, a.x() - b.right());
            case RIGHT -> Math.max(0, b.x() - a.right());
            case UP -> Math.max(0, a.y() - b.bottom());
            case DOWN -> Math.max(0, b.y() - a.bottom());
        };
    }

    private static double secondary(Bounds a, Bounds b, Direction direction) {
        return switch (direction) {
            case LEFT, RIGHT -> Math.abs(a.centerY() - b.centerY());
            case UP, DOWN -> Math.abs(a.centerX() - b.centerX());
        };
    }

    public static List<Node> readingOrder(List<Node> nodes) {
        return nodes.stream().sorted(Comparator.comparingInt(Node::panel).thenComparingDouble(Node::order)).toList();
    }

    public static Node tab(List<Node> nodes, Node current, boolean reverse) {
        List<Node> ordered = readingOrder(nodes);
        if (ordered.isEmpty()) return null;
        int index = current == null ? -1 : ordered.indexOf(current);
        if (index < 0) return reverse ? ordered.getLast() : ordered.getFirst();
        return ordered.get(Math.floorMod(index + (reverse ? -1 : 1), ordered.size()));
    }
}
