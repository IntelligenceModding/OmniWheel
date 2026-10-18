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
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        OpenWheelAction other = (OpenWheelAction) value;
        return true && java.util.Objects.equals(wheelId, other.wheelId);
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
