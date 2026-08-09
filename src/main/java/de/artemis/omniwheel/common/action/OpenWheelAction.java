package de.artemis.omniwheel.common.action;

import java.util.Objects;

public record OpenWheelAction(String wheelId) implements WheelAction {
    public OpenWheelAction {
        wheelId = Objects.requireNonNull(wheelId, "wheelId").trim();
        if (wheelId.isEmpty()) {
            throw new IllegalArgumentException("wheelId must not be empty");
        }
    }
}
