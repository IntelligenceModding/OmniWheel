package de.artemis.omniwheel.common.action;

import java.util.Objects;

public record OpenScreenAction(ScreenTarget target) implements WheelAction {
    public OpenScreenAction {
        target = Objects.requireNonNull(target, "target");
    }
}
