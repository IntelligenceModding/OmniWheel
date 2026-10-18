package de.artemis.omniwheel.common.action;

import java.util.Objects;

public final class ChatAction implements WheelAction {

    private final String message;

    private final boolean requireConfirmation;

    private final boolean preset;

    public ChatAction(String message, boolean requireConfirmation) {
        this(message, requireConfirmation, false);
    }

    @Override
    public boolean requiresConfirmation() {
        return requireConfirmation;
    }

    public ChatAction(String message, boolean requireConfirmation, boolean preset) {
        message = Objects.requireNonNull(message, "message").trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message must not be empty");
        }
        this.message = message;
        this.requireConfirmation = requireConfirmation;
        this.preset = preset;
    }

    public String message() {
        return message;
    }

    public boolean requireConfirmation() {
        return requireConfirmation;
    }

    public boolean preset() {
        return preset;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        ChatAction other = (ChatAction) value;
        return true && java.util.Objects.equals(message, other.message) && java.util.Objects.equals(requireConfirmation, other.requireConfirmation) && java.util.Objects.equals(preset, other.preset);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(message, requireConfirmation, preset);
    }

    @Override
    public String toString() {
        return "ChatAction[" + "message=" + message + ", requireConfirmation=" + requireConfirmation + ", preset=" + preset + "]";
    }
}
