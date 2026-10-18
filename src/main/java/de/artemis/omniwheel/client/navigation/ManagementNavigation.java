package de.artemis.omniwheel.client.navigation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Layout-based focus traversal. Coordinates and scroll visibility come from the UI, not control names.
 */
public final class ManagementNavigation {

    private ManagementNavigation() {
    }

    public enum Direction {

        LEFT, RIGHT, UP, DOWN
    }

    public final static class Bounds {

        private final int x;

        private final int y;

        private final int width;

        private final int height;

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public double centerX() {
            return x + width / 2.0;
        }

        public double centerY() {
            return y + height / 2.0;
        }

        public Bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value)
                return true;
            if (value == null || value.getClass() != getClass())
                return false;
            Bounds other = (Bounds) value;
            return true && java.util.Objects.equals(x, other.x) && java.util.Objects.equals(y, other.y) && java.util.Objects.equals(width, other.width) && java.util.Objects.equals(height, other.height);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(x, y, width, height);
        }

        @Override
        public String toString() {
            return "Bounds[" + "x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
        }
    }

    public final static class Node {

        private final String id;

        private final int panel;

        private final String group;

        private final Bounds bounds;

        private final boolean visible;

        private final double order;

        public Node(String id, int panel, String group, Bounds bounds, boolean visible, double order) {
            this.id = id;
            this.panel = panel;
            this.group = group;
            this.bounds = bounds;
            this.visible = visible;
            this.order = order;
        }

        public String id() {
            return id;
        }

        public int panel() {
            return panel;
        }

        public String group() {
            return group;
        }

        public Bounds bounds() {
            return bounds;
        }

        public boolean visible() {
            return visible;
        }

        public double order() {
            return order;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value)
                return true;
            if (value == null || value.getClass() != getClass())
                return false;
            Node other = (Node) value;
            return true && java.util.Objects.equals(id, other.id) && java.util.Objects.equals(panel, other.panel) && java.util.Objects.equals(group, other.group) && java.util.Objects.equals(bounds, other.bounds) && java.util.Objects.equals(visible, other.visible) && java.util.Objects.equals(order, other.order);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, panel, group, bounds, visible, order);
        }

        @Override
        public String toString() {
            return "Node[" + "id=" + id + ", panel=" + panel + ", group=" + group + ", bounds=" + bounds + ", visible=" + visible + ", order=" + order + "]";
        }
    }

    public static Node move(List<Node> nodes, Node current, Direction direction) {
        boolean vertical = direction == Direction.UP || direction == Direction.DOWN;
        // A scroll container is traversed in full before leaving it, including its offscreen rows.
        if (vertical && !current.group().isEmpty()) {
            Node next = nearest(nodes, current, direction, true, false);
            if (next != null)
                return next;
        }
        Node next = nearest(nodes, current, direction, false, !vertical);
        if (next != null) {
            if (direction == Direction.DOWN && !next.group().isEmpty() && !next.group().equals(current.group())) {
                String group = next.group();
                return readingOrder(nodes).stream().filter(node -> node.panel() == current.panel() && node.group().equals(group)).findFirst().orElse(next);
            }
            if (direction == Direction.UP && !current.group().isEmpty() && !current.group().equals(next.group())) {
                int headerY = next.bounds().y();
                return readingOrder(nodes).stream().filter(node -> node.panel() == current.panel() && node.visible() && node.bounds().y() == headerY && !node.group().equals(current.group())).findFirst().orElse(next);
            }
            return next;
        }
        if (vertical)
            return current;
        int delta = direction == Direction.LEFT ? -1 : 1;
        int panel = current.panel() + delta;
        return nodes.stream().filter(node -> node.panel() == panel && node.visible()).min(Comparator.comparingDouble((Node node) -> Math.abs(node.bounds().centerY() - current.bounds().centerY())).thenComparingDouble(Node::order)).orElse(current);
    }

    private static Node nearest(List<Node> nodes, Node current, Direction direction, boolean sameGroup, boolean requireLane) {
        return nodes.stream().filter(node -> !node.id().equals(current.id()) && node.panel() == current.panel()).filter(node -> sameGroup ? Objects.equals(node.group(), current.group()) : node.visible()).filter(node -> inDirection(current.bounds(), node.bounds(), direction)).filter(node -> !requireLane || overlaps(current.bounds(), node.bounds(), direction)).min(Comparator.comparingInt((Node node) -> overlaps(current.bounds(), node.bounds(), direction) ? 0 : 1).thenComparingDouble(node -> primary(current.bounds(), node.bounds(), direction)).thenComparingDouble(node -> secondary(current.bounds(), node.bounds(), direction)).thenComparingDouble(Node::order)).orElse(null);
    }

    private static boolean inDirection(Bounds from, Bounds to, Direction direction) {
        if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.LEFT)) {
            return to.centerX() < from.centerX() - 1 && to.right() <= from.x() + 1;
        } else {
            if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.RIGHT)) {
                return to.centerX() > from.centerX() + 1 && to.x() >= from.right() - 1;
            } else {
                if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.UP)) {
                    return to.centerY() < from.centerY() - 1;
                } else {
                    if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.DOWN)) {
                        return to.centerY() > from.centerY() + 1;
                    } else {
                        return de.artemis.omniwheel.common.Java8.fail(new IllegalStateException("Unexpected switch value"));
                    }
                }
            }
        }
    }

    private static boolean overlaps(Bounds a, Bounds b, Direction direction) {
        if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.LEFT) || java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.RIGHT)) {
            return a.y() < b.bottom() && b.y() < a.bottom();
        } else {
            if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.UP) || java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.DOWN)) {
                return a.x() < b.right() && b.x() < a.right();
            } else {
                return de.artemis.omniwheel.common.Java8.fail(new IllegalStateException("Unexpected switch value"));
            }
        }
    }

    private static double primary(Bounds a, Bounds b, Direction direction) {
        if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.LEFT)) {
            return Math.max(0, a.x() - b.right());
        } else {
            if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.RIGHT)) {
                return Math.max(0, b.x() - a.right());
            } else {
                if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.UP)) {
                    return Math.max(0, a.y() - b.bottom());
                } else {
                    if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.DOWN)) {
                        return Math.max(0, b.y() - a.bottom());
                    } else {
                        return de.artemis.omniwheel.common.Java8.fail(new IllegalStateException("Unexpected switch value"));
                    }
                }
            }
        }
    }

    private static double secondary(Bounds a, Bounds b, Direction direction) {
        if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.LEFT) || java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.RIGHT)) {
            return Math.abs(a.centerY() - b.centerY());
        } else {
            if (java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.UP) || java.util.Objects.equals(direction, de.artemis.omniwheel.client.navigation.ManagementNavigation.Direction.DOWN)) {
                return Math.abs(a.centerX() - b.centerX());
            } else {
                return de.artemis.omniwheel.common.Java8.fail(new IllegalStateException("Unexpected switch value"));
            }
        }
    }

    public static List<Node> readingOrder(List<Node> nodes) {
        return nodes.stream().sorted(Comparator.comparingInt(Node::panel).thenComparingDouble(Node::order)).collect(de.artemis.omniwheel.common.Java8.toList());
    }

    public static Node tab(List<Node> nodes, Node current, boolean reverse) {
        List<Node> ordered = readingOrder(nodes);
        if (ordered.isEmpty())
            return null;
        int index = current == null ? -1 : ordered.indexOf(current);
        if (index < 0)
            if (reverse) {
                return ordered.get(ordered.size() - 1);
            } else {
                return ordered.get(0);
            }
        return ordered.get(Math.floorMod(index + (reverse ? -1 : 1), ordered.size()));
    }
}
