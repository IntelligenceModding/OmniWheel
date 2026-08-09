package de.artemis.omniwheel.common.action;

import java.util.Objects;

public record SwitchProfileAction(String profileId) implements WheelAction {
    public SwitchProfileAction {
        profileId = Objects.requireNonNull(profileId, "profileId").trim();
        if (profileId.isEmpty()) {
            throw new IllegalArgumentException("profileId must not be empty");
        }
    }
}
