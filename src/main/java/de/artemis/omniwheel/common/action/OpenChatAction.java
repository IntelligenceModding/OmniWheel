package de.artemis.omniwheel.common.action;

import java.util.Objects;

public record OpenChatAction(String initialText) implements WheelAction {
    public OpenChatAction {
        initialText = Objects.requireNonNull(initialText, "initialText");
    }
}
