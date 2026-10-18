package de.artemis.omniwheel.client.profile.store;

import de.artemis.omniwheel.common.profile.WheelProfile;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ProfileCollection {

    private final String activeProfileId;

    private final List<WheelProfile> profiles;

    public WheelProfile activeProfile() {
        return profiles.stream().filter(profile -> profile.id().equals(activeProfileId)).findFirst().orElseThrow(java.util.NoSuchElementException::new);
    }

    public Map<String, WheelProfile> byId() {
        return profiles.stream().collect(de.artemis.omniwheel.common.Java8.toUnmodifiableMap(WheelProfile::id, Function.identity()));
    }

    private static String requireText(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }

    public ProfileCollection(String activeProfileId, List<WheelProfile> profiles) {
        activeProfileId = requireText(activeProfileId, "activeProfileId");
        profiles = de.artemis.omniwheel.common.Java8.copyList(Objects.requireNonNull(profiles, "profiles"));
        if (profiles.isEmpty()) {
            throw new IllegalArgumentException("profiles must not be empty");
        }
        if (profiles.stream().map(WheelProfile::id).distinct().count() != profiles.size()) {
            throw new IllegalArgumentException("profiles must have unique IDs");
        }
        if (profiles.stream().map(WheelProfile::id).noneMatch(activeProfileId::equals)) {
            throw new IllegalArgumentException("activeProfileId must refer to a known profile");
        }
        this.activeProfileId = activeProfileId;
        this.profiles = profiles;
    }

    public String activeProfileId() {
        return activeProfileId;
    }

    public List<WheelProfile> profiles() {
        return profiles;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        ProfileCollection other = (ProfileCollection) value;
        return true && java.util.Objects.equals(activeProfileId, other.activeProfileId) && java.util.Objects.equals(profiles, other.profiles);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(activeProfileId, profiles);
    }

    @Override
    public String toString() {
        return "ProfileCollection[" + "activeProfileId=" + activeProfileId + ", profiles=" + profiles + "]";
    }
}
