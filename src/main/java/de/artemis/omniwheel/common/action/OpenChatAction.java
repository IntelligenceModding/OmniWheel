package de.artemis.omniwheel.common.action;

import java.util.Objects;

public final class OpenChatAction implements WheelAction {

    private final String initialText;

    public OpenChatAction(String initialText) {
        initialText = Objects.requireNonNull(initialText, "initialText");
        this.initialText = initialText;
    }

    public String initialText() {
        return initialText;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        OpenChatAction other = (OpenChatAction) value;
        return true && java.util.Objects.equals(initialText, other.initialText);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(initialText);
    }

    @Override
    public String toString() {
        return "OpenChatAction[" + "initialText=" + initialText + "]";
    }
}
