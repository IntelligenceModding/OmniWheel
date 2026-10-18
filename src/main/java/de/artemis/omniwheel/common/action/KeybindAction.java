package de.artemis.omniwheel.common.action;

import java.util.Objects;

public final class KeybindAction implements WheelAction {

    private final String keybindId;

    public KeybindAction(String keybindId) {
        keybindId = Objects.requireNonNull(keybindId, "keybindId").trim();
        if (keybindId.isEmpty()) {
            throw new IllegalArgumentException("keybindId must not be empty");
        }
        this.keybindId = keybindId;
    }

    public String keybindId() {
        return keybindId;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        KeybindAction other = (KeybindAction) value;
        return true && java.util.Objects.equals(keybindId, other.keybindId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(keybindId);
    }

    @Override
    public String toString() {
        return "KeybindAction[" + "keybindId=" + keybindId + "]";
    }
}
