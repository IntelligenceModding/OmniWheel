package de.artemis.omniwheel.common.action;

public interface WheelAction {
    default boolean requiresConfirmation() {
        return false;
    }
}
