package de.artemis.omniwheel.common.action;

import java.util.Objects;

public record FunctionAction(GameplayFunction function) implements WheelAction {
    public FunctionAction {
        function = Objects.requireNonNull(function, "function");
    }
}
