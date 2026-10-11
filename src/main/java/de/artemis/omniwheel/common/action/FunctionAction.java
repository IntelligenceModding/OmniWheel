package de.artemis.omniwheel.common.action;

import java.util.Objects;

public final class FunctionAction implements WheelAction {
    private final GameplayFunction function;

    public FunctionAction(GameplayFunction function) {

        function = Objects.requireNonNull(function, "function");

        this.function = function;
    }

    public GameplayFunction function() {
        return function;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FunctionAction)) return false;
        FunctionAction that = (FunctionAction) other;
        return java.util.Objects.equals(function, that.function);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(function);
    }

    @Override
    public String toString() {
        return "FunctionAction[" + "function=" + function + "]";
    }
}
