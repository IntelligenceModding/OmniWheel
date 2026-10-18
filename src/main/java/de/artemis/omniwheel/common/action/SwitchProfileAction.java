package de.artemis.omniwheel.common.action;

import java.util.Objects;

public final class SwitchProfileAction implements WheelAction {

    private final String profileId;

    public SwitchProfileAction(String profileId) {
        profileId = Objects.requireNonNull(profileId, "profileId").trim();
        if (profileId.isEmpty()) {
            throw new IllegalArgumentException("profileId must not be empty");
        }
        this.profileId = profileId;
    }

    public String profileId() {
        return profileId;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        SwitchProfileAction other = (SwitchProfileAction) value;
        return true && java.util.Objects.equals(profileId, other.profileId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(profileId);
    }

    @Override
    public String toString() {
        return "SwitchProfileAction[" + "profileId=" + profileId + "]";
    }
}
