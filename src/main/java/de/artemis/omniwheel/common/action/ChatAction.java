package de.artemis.omniwheel.common.action;

import java.util.Objects;

public record ChatAction(String message, boolean requireConfirmation, boolean preset) implements WheelAction {
    public ChatAction(String message, boolean requireConfirmation) {
        this(message, requireConfirmation, false);
    }

    public ChatAction {
        message = Objects.requireNonNull(message, "message").trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message must not be empty");
        }
    }

    @Override
    public boolean requiresConfirmation() {
        return requireConfirmation;
    }
}
