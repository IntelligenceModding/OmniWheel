package de.artemis.omniwheel.common.action;

import java.util.Objects;

public record KeybindAction(String keybindId) implements WheelAction {
    public KeybindAction {
        keybindId = Objects.requireNonNull(keybindId, "keybindId").trim();
        if (keybindId.isEmpty()) {
            throw new IllegalArgumentException("keybindId must not be empty");
        }
    }
}
