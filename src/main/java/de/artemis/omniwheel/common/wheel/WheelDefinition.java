package de.artemis.omniwheel.common.wheel;

import java.util.List;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/** Editor rows and radial positions are persisted independently; old profiles inherit their existing order. */
public record WheelDefinition(String id, String title, String description, int segmentCount, List<WheelEntry> entries,
                              boolean active, List<String> radialOrder) {
    public WheelDefinition(String id, String title, String description, int segmentCount, List<WheelEntry> entries, boolean active) {
        this(id, title, description, segmentCount, entries, active, null);
    }
    public WheelDefinition(String id, String title, String description, int segmentCount, List<WheelEntry> entries) {
        this(id, title, description, segmentCount, entries, true);
    }

    public WheelDefinition {
        id = requireText(id, "id");
        title = requireText(title, "title");
        description = Objects.requireNonNull(description, "description").trim();
        if (segmentCount < 1 || segmentCount > 16) {
            throw new IllegalArgumentException("segmentCount must be between 1 and 16");
        }
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        var ids = new LinkedHashSet<String>();
        for (WheelEntry entry : entries) {
            if (!ids.add(entry.id())) throw new IllegalArgumentException("Duplicate entry ID: " + entry.id());
        }
        var order = new LinkedHashSet<String>();
        if (radialOrder != null) for (String entry : radialOrder) if (ids.contains(entry)) order.add(entry);
        order.addAll(ids);
        radialOrder = List.copyOf(order);
    }

    public List<WheelEntry> radialEntries() {
        var byId = new LinkedHashMap<String, WheelEntry>();
        for (WheelEntry entry : entries) byId.put(entry.id(), entry);
        return radialOrder.stream().map(byId::get).toList();
    }

    private static String requireText(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }
}
