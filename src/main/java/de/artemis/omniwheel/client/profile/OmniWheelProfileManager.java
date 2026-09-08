package de.artemis.omniwheel.client.profile;

import de.artemis.omniwheel.client.profile.store.OmniWheelProfileRepository;
import de.artemis.omniwheel.client.profile.store.ProfileCollection;
import de.artemis.omniwheel.common.OmniWheelText;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OmniWheelProfileManager {
    private final OmniWheelProfileRepository repository = new OmniWheelProfileRepository();

    private ProfileCollection profiles;
    private Map<String, WheelProfile> byId = Map.of();
    private String observedChangeToken;

    public void ensureLoaded() {
        if (profiles == null) {
            replaceProfiles(repository.loadOrCreate());
            observedChangeToken = repository.currentChangeToken();
        }
    }

    public WheelProfile getActiveProfile() {
        ensureLoaded();
        return profiles.activeProfile();
    }

    public List<WheelProfile> getProfiles() {
        ensureLoaded();
        return profiles.profiles();
    }

    public Path storagePath() {
        return repository.storagePath();
    }

    public void setActiveProfile(String profileId) {
        ensureLoaded();
        WheelProfile profile = byId.get(Objects.requireNonNull(profileId, "profileId"));
        if (profile == null) {
            throw new IllegalArgumentException("Unknown profile: " + profileId);
        }

        replaceProfiles(new ProfileCollection(profile.id(), profiles.profiles()));
        repository.save(profiles);
        observedChangeToken = repository.currentChangeToken();
    }

    public void reload() {
        replaceProfiles(repository.loadOrCreate());
        observedChangeToken = repository.currentChangeToken();
    }

    public void resetToDefaults() {
        replaceProfiles(repository.resetToDefaults());
        observedChangeToken = repository.currentChangeToken();
    }

    public void saveProfile(WheelProfile updatedProfile) {
        ensureLoaded();
        Objects.requireNonNull(updatedProfile, "updatedProfile");

        List<WheelProfile> updatedProfiles = new ArrayList<>(profiles.profiles().size());
        boolean replaced = false;
        for (WheelProfile profile : profiles.profiles()) {
            if (profile.id().equals(updatedProfile.id())) {
                updatedProfiles.add(updatedProfile);
                replaced = true;
            } else {
                updatedProfiles.add(profile);
            }
        }

        if (!replaced) {
            throw new IllegalArgumentException("Unknown profile: " + updatedProfile.id());
        }

        replaceProfiles(new ProfileCollection(profiles.activeProfileId(), updatedProfiles));
        repository.save(profiles);
        observedChangeToken = repository.currentChangeToken();
    }

    public WheelProfile createProfile() {
        ensureLoaded();
        String profileId = nextProfileId();
        String displayName = nextDisplayName();
        WheelDefinition rootWheel = new WheelDefinition("main", "omniwheel.default.wheel.main", "", 8, List.of());
        WheelProfile createdProfile = new WheelProfile(
                profileId,
                displayName,
                rootWheel.id(),
                new LinkedHashMap<>(Map.of(rootWheel.id(), rootWheel))
        );

        List<WheelProfile> updatedProfiles = new ArrayList<>(profiles.profiles().size() + 1);
        updatedProfiles.addAll(profiles.profiles());
        updatedProfiles.add(createdProfile);

        replaceProfiles(new ProfileCollection(profiles.activeProfileId(), updatedProfiles));
        repository.save(profiles);
        observedChangeToken = repository.currentChangeToken();
        return createdProfile;
    }

    public WheelProfile renameProfile(String profileId, String displayName) {
        ensureLoaded();
        profileId = Objects.requireNonNull(profileId, "profileId");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be empty");
        }

        WheelProfile profile = byId.get(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown profile: " + profileId);
        }
        if (profile.displayName().equals(displayName)) {
            return profile;
        }

        WheelProfile updatedProfile = new WheelProfile(profile.id(), displayName, profile.rootWheelId(), profile.wheels());
        saveProfile(updatedProfile);
        return updatedProfile;
    }

    public void moveProfile(String profileId, int targetIndex) {
        ensureLoaded();
        profileId = Objects.requireNonNull(profileId, "profileId");
        WheelProfile profile = byId.get(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown profile: " + profileId);
        }

        List<WheelProfile> reorderedProfiles = new ArrayList<>(profiles.profiles());
        int currentIndex = -1;
        for (int index = 0; index < reorderedProfiles.size(); index++) {
            if (reorderedProfiles.get(index).id().equals(profileId)) {
                currentIndex = index;
                break;
            }
        }
        if (currentIndex < 0) {
            throw new IllegalArgumentException("Unknown profile: " + profileId);
        }

        WheelProfile movedProfile = reorderedProfiles.remove(currentIndex);
        int clampedTargetIndex = Math.max(0, Math.min(targetIndex, reorderedProfiles.size()));
        reorderedProfiles.add(clampedTargetIndex, movedProfile);

        replaceProfiles(new ProfileCollection(profiles.activeProfileId(), reorderedProfiles));
        repository.save(profiles);
        observedChangeToken = repository.currentChangeToken();
    }

    public WheelProfile deleteProfile(String profileId) {
        ensureLoaded();
        profileId = Objects.requireNonNull(profileId, "profileId");
        if (!byId.containsKey(profileId)) {
            throw new IllegalArgumentException("Unknown profile: " + profileId);
        }
        if ("default".equals(profileId)) {
            throw new IllegalStateException("Cannot delete the default profile");
        }
        if (profiles.profiles().size() <= 1) {
            throw new IllegalStateException("Cannot delete the last remaining profile");
        }

        List<WheelProfile> updatedProfiles = new ArrayList<>(profiles.profiles().size() - 1);
        for (WheelProfile profile : profiles.profiles()) {
            if (!profile.id().equals(profileId)) {
                updatedProfiles.add(profile);
            }
        }

        String nextActiveProfileId = profiles.activeProfileId();
        if (nextActiveProfileId.equals(profileId)) {
            nextActiveProfileId = updatedProfiles.stream()
                    .filter(profile -> profile.id().equals("default"))
                    .findFirst()
                    .or(() -> updatedProfiles.stream().findFirst())
                    .orElseThrow()
                    .id();
        }

        replaceProfiles(new ProfileCollection(nextActiveProfileId, updatedProfiles));
        repository.save(profiles);
        observedChangeToken = repository.currentChangeToken();
        return getActiveProfile();
    }

    public boolean pollExternalChanges() {
        ensureLoaded();
        String currentChangeToken = repository.currentChangeToken();
        if (Objects.equals(currentChangeToken, observedChangeToken)) {
            return false;
        }

        replaceProfiles(repository.loadOrCreate());
        observedChangeToken = repository.currentChangeToken();
        return true;
    }

    private String nextProfileId() {
        int index = 1;
        while (byId.containsKey("custom_" + index)) {
            index++;
        }
        return "custom_" + index;
    }

    private String nextDisplayName() {
        int index = 1;
        while (hasDisplayName(OmniWheelText.translate("omniwheel.manager.created.custom_profile", index))) {
            index++;
        }
        return OmniWheelText.translate("omniwheel.manager.created.custom_profile", index);
    }

    private boolean hasDisplayName(String displayName) {
        for (WheelProfile profile : profiles.profiles()) {
            if (profile.displayName().equals(displayName)) {
                return true;
            }
        }
        return false;
    }

    private void replaceProfiles(ProfileCollection newProfiles) {
        this.profiles = Objects.requireNonNull(newProfiles, "newProfiles");
        this.byId = newProfiles.byId();
    }
}
