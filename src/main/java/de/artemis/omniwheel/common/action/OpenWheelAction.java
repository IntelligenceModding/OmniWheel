package de.artemis.omniwheel.common.action;

import java.util.Objects;

public final class OpenWheelAction implements WheelAction {
    private final String wheelId;

    public OpenWheelAction(String wheelId) {

        wheelId = Objects.requireNonNull(wheelId, "wheelId").trim();
        if (wheelId.isEmpty()) {
            throw new IllegalArgumentException("wheelId must not be empty");
        }

        this.wheelId = wheelId;
    }

    public String wheelId() {
        return wheelId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof OpenWheelAction)) return false;
        OpenWheelAction that = (OpenWheelAction) other;
        return java.util.Objects.equals(wheelId, that.wheelId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(wheelId);
    }

    @Override
    public String toString() {
        return "OpenWheelAction[" + "wheelId=" + wheelId + "]";
    }
}
