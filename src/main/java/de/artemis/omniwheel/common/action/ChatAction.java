package de.artemis.omniwheel.common.action;

import java.util.Objects;

public final class ChatAction implements WheelAction {
    private final String message;
    private final boolean requireConfirmation;

    public ChatAction(String message, boolean requireConfirmation) {

        message = Objects.requireNonNull(message, "message").trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message must not be empty");
        }

        this.message = message;
        this.requireConfirmation = requireConfirmation;
    }

    public String message() {
        return message;
    }

    public boolean requireConfirmation() {
        return requireConfirmation;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ChatAction)) return false;
        ChatAction that = (ChatAction) other;
        return java.util.Objects.equals(message, that.message) && java.util.Objects.equals(requireConfirmation, that.requireConfirmation);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(message, requireConfirmation);
    }

    @Override
    public String toString() {
        return "ChatAction[" + "message=" + message + ", " + "requireConfirmation=" + requireConfirmation + "]";
    }

    @Override
    public boolean requiresConfirmation() {
        return requireConfirmation;
    }
}
