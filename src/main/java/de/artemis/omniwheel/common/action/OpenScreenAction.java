package de.artemis.omniwheel.common.action;

import java.util.Objects;

public final class OpenScreenAction implements WheelAction {

    private final ScreenTarget target;

    public OpenScreenAction(ScreenTarget target) {
        target = Objects.requireNonNull(target, "target");
        this.target = target;
    }

    public ScreenTarget target() {
        return target;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        OpenScreenAction other = (OpenScreenAction) value;
        return true && java.util.Objects.equals(target, other.target);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(target);
    }

    @Override
    public String toString() {
        return "OpenScreenAction[" + "target=" + target + "]";
    }
}
