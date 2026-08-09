package de.artemis.omniwheel.client.profile.store;

import de.artemis.omniwheel.common.profile.WheelProfile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ProfileCollection(String activeProfileId, List<WheelProfile> profiles) {
    public ProfileCollection {
        activeProfileId = requireText(activeProfileId, "activeProfileId");
        profiles = List.copyOf(Objects.requireNonNull(profiles, "profiles"));
        if (profiles.isEmpty()) {
            throw new IllegalArgumentException("profiles must not be empty");
        }
        if (profiles.stream().map(WheelProfile::id).noneMatch(activeProfileId::equals)) {
            throw new IllegalArgumentException("activeProfileId must refer to a known profile");
        }
    }

    public WheelProfile activeProfile() {
        return profiles.stream()
                .filter(profile -> profile.id().equals(activeProfileId))
                .findFirst()
                .orElseThrow();
    }

    public Map<String, WheelProfile> byId() {
        return profiles.stream().collect(Collectors.toUnmodifiableMap(WheelProfile::id, Function.identity()));
    }

    private static String requireText(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }
}
