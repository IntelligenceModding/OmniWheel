package de.artemis.omniwheel.common.wheel;

import java.util.List;
import java.util.Objects;

public record WheelDefinition(String id, String title, String description, int segmentCount, List<WheelEntry> entries, boolean active) {
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
    }

    private static String requireText(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }
}
