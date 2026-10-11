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
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof OpenChatAction)) return false;
        OpenChatAction that = (OpenChatAction) other;
        return java.util.Objects.equals(initialText, that.initialText);
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
