package de.artemis.omniwheel.common.action;

import java.util.Objects;

public record CopyTextAction(String text) implements WheelAction {
    public CopyTextAction {
        text = Objects.requireNonNull(text, "text").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("text must not be empty");
        }
    }
}
