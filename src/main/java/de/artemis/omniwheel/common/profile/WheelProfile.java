package de.artemis.omniwheel.common.profile;

import de.artemis.omniwheel.common.wheel.WheelDefinition;

import java.util.Map;
import java.util.Objects;

public final class WheelProfile {
    private final String id;
    private final String displayName;
    private final String rootWheelId;
    private final Map<String, WheelDefinition> wheels;

    public WheelProfile(String id, String displayName, String rootWheelId, Map<String, WheelDefinition> wheels) {

        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        rootWheelId = requireText(rootWheelId, "rootWheelId");
        wheels = com.google.common.collect.ImmutableMap.copyOf(Objects.requireNonNull(wheels, "wheels"));
        if (!wheels.containsKey(rootWheelId)) {
            throw new IllegalArgumentException("rootWheelId must exist in wheels");
        }

        this.id = id;
        this.displayName = displayName;
        this.rootWheelId = rootWheelId;
        this.wheels = wheels;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String rootWheelId() {
        return rootWheelId;
    }

    public Map<String, WheelDefinition> wheels() {
        return wheels;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WheelProfile)) return false;
        WheelProfile that = (WheelProfile) other;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(rootWheelId, that.rootWheelId) && java.util.Objects.equals(wheels, that.wheels);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, displayName, rootWheelId, wheels);
    }

    @Override
    public String toString() {
        return "WheelProfile[" + "id=" + id + ", " + "displayName=" + displayName + ", " + "rootWheelId=" + rootWheelId + ", " + "wheels=" + wheels + "]";
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
