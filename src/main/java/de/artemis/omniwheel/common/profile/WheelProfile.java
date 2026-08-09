package de.artemis.omniwheel.common.profile;

import de.artemis.omniwheel.common.wheel.WheelDefinition;

import java.util.Map;
import java.util.Objects;

public record WheelProfile(String id, String displayName, String rootWheelId, Map<String, WheelDefinition> wheels) {
    public WheelProfile {
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        rootWheelId = requireText(rootWheelId, "rootWheelId");
        wheels = Map.copyOf(Objects.requireNonNull(wheels, "wheels"));
        if (!wheels.containsKey(rootWheelId)) {
            throw new IllegalArgumentException("rootWheelId must exist in wheels");
        }
    }

    public WheelDefinition wheel(String wheelId) {
        WheelDefinition wheel = wheels.get(wheelId);
        if (wheel == null) {
            throw new IllegalArgumentException("Unknown wheel id: " + wheelId);
        }
        return wheel;
    }

    private static String requireText(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }
}
