package de.artemis.omniwheel.common.action;

import java.util.Objects;

public final class LocalMessageAction implements WheelAction {
    private final String message;

    public LocalMessageAction(String message) {

        message = Objects.requireNonNull(message, "message").trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message must not be empty");
        }

        this.message = message;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LocalMessageAction)) return false;
        LocalMessageAction that = (LocalMessageAction) other;
        return java.util.Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(message);
    }

    @Override
    public String toString() {
        return "LocalMessageAction[" + "message=" + message + "]";
    }
}
