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
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        FunctionAction other = (FunctionAction) value;
        return true && java.util.Objects.equals(function, other.function);
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
