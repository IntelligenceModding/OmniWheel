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
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof OpenScreenAction)) return false;
        OpenScreenAction that = (OpenScreenAction) other;
        return java.util.Objects.equals(target, that.target);
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
