package de.artemis.omniwheel.common.wheel;

import java.util.List;
import java.util.Objects;

public final class WheelDefinition {
    private final String id;
    private final String title;
    private final String description;
    private final int segmentCount;
    private final List<WheelEntry> entries;
    private final boolean active;

    public WheelDefinition(String id, String title, String description, int segmentCount, List<WheelEntry> entries, boolean active) {

        id = requireText(id, "id");
        title = requireText(title, "title");
        description = Objects.requireNonNull(description, "description").trim();
        if (segmentCount < 1 || segmentCount > 16) {
            throw new IllegalArgumentException("segmentCount must be between 1 and 16");
        }
        entries = com.google.common.collect.ImmutableList.copyOf(Objects.requireNonNull(entries, "entries"));

        this.id = id;
        this.title = title;
        this.description = description;
        this.segmentCount = segmentCount;
        this.entries = entries;
        this.active = active;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public int segmentCount() {
        return segmentCount;
    }

    public List<WheelEntry> entries() {
        return entries;
    }

    public boolean active() {
        return active;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WheelDefinition)) return false;
        WheelDefinition that = (WheelDefinition) other;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(title, that.title) && java.util.Objects.equals(description, that.description) && java.util.Objects.equals(segmentCount, that.segmentCount) && java.util.Objects.equals(entries, that.entries) && java.util.Objects.equals(active, that.active);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, title, description, segmentCount, entries, active);
    }

    @Override
    public String toString() {
        return "WheelDefinition[" + "id=" + id + ", " + "title=" + title + ", " + "description=" + description + ", " + "segmentCount=" + segmentCount + ", " + "entries=" + entries + ", " + "active=" + active + "]";
    }

    public WheelDefinition(String id, String title, String description, int segmentCount, List<WheelEntry> entries) {
        this(id, title, description, segmentCount, entries, true);
    }

    private static String requireText(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }
}
