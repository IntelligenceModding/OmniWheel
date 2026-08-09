package de.artemis.omniwheel.common.action;

import java.util.Objects;

public record LocalMessageAction(String message) implements WheelAction {
    public LocalMessageAction {
        message = Objects.requireNonNull(message, "message").trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message must not be empty");
        }
    }
}
