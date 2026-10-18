package de.artemis.omniwheel.common.wheel;

import java.util.List;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * Editor rows and radial positions are persisted independently; old profiles inherit their existing order.
 */
public final class WheelDefinition {

    private final String id;

    private final String title;

    private final String description;

    private final int segmentCount;

    private final List<WheelEntry> entries;

    private final boolean active;

    private final List<String> radialOrder;

    public WheelDefinition(String id, String title, String description, int segmentCount, List<WheelEntry> entries, boolean active) {
        this(id, title, description, segmentCount, entries, active, null);
    }

    public WheelDefinition(String id, String title, String description, int segmentCount, List<WheelEntry> entries) {
        this(id, title, description, segmentCount, entries, true);
    }

    public List<WheelEntry> radialEntries() {
        java.util.LinkedHashMap<java.lang.String, de.artemis.omniwheel.common.wheel.WheelEntry> byId = new LinkedHashMap<String, WheelEntry>();
        for (WheelEntry entry : entries) byId.put(entry.id(), entry);
        return radialOrder.stream().map(byId::get).collect(de.artemis.omniwheel.common.Java8.toList());
    }

    private static String requireText(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }

    public WheelDefinition(String id, String title, String description, int segmentCount, List<WheelEntry> entries, boolean active, List<String> radialOrder) {
        id = requireText(id, "id");
        title = requireText(title, "title");
        description = Objects.requireNonNull(description, "description").trim();
        if (segmentCount < 1 || segmentCount > 16) {
            throw new IllegalArgumentException("segmentCount must be between 1 and 16");
        }
        entries = de.artemis.omniwheel.common.Java8.copyList(Objects.requireNonNull(entries, "entries"));
        java.util.LinkedHashSet<java.lang.String> ids = new LinkedHashSet<String>();
        for (WheelEntry entry : entries) {
            if (!ids.add(entry.id()))
                throw new IllegalArgumentException("Duplicate entry ID: " + entry.id());
        }
        java.util.LinkedHashSet<java.lang.String> order = new LinkedHashSet<String>();
        if (radialOrder != null)
            for (String entry : radialOrder) if (ids.contains(entry))
                order.add(entry);
        order.addAll(ids);
        radialOrder = de.artemis.omniwheel.common.Java8.copyList(order);
        this.id = id;
        this.title = title;
        this.description = description;
        this.segmentCount = segmentCount;
        this.entries = entries;
        this.active = active;
        this.radialOrder = radialOrder;
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

    public List<String> radialOrder() {
        return radialOrder;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        WheelDefinition other = (WheelDefinition) value;
        return true && java.util.Objects.equals(id, other.id) && java.util.Objects.equals(title, other.title) && java.util.Objects.equals(description, other.description) && java.util.Objects.equals(segmentCount, other.segmentCount) && java.util.Objects.equals(entries, other.entries) && java.util.Objects.equals(active, other.active) && java.util.Objects.equals(radialOrder, other.radialOrder);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, title, description, segmentCount, entries, active, radialOrder);
    }

    @Override
    public String toString() {
        return "WheelDefinition[" + "id=" + id + ", title=" + title + ", description=" + description + ", segmentCount=" + segmentCount + ", entries=" + entries + ", active=" + active + ", radialOrder=" + radialOrder + "]";
    }
}
