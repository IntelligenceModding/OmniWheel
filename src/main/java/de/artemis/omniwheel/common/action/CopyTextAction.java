package de.artemis.omniwheel.common.action;

import java.util.Objects;

public final class CopyTextAction implements WheelAction {

    private final String text;

    public CopyTextAction(String text) {
        text = Objects.requireNonNull(text, "text").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("text must not be empty");
        }
        this.text = text;
    }

    public String text() {
        return text;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        CopyTextAction other = (CopyTextAction) value;
        return true && java.util.Objects.equals(text, other.text);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(text);
    }

    @Override
    public String toString() {
        return "CopyTextAction[" + "text=" + text + "]";
    }
}
