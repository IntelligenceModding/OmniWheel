package de.artemis.omniwheel.client.profile.store;

import de.artemis.omniwheel.client.profile.DefaultProfiles;
import de.artemis.omniwheel.common.profile.WheelProfile;
import java.util.ArrayList;

final class ProfileUpgrader {

    private ProfileUpgrader() {
    }

    static ProfileCollection upgrade(ProfileCollection collection) {
        boolean changed = false;
        java.util.ArrayList<de.artemis.omniwheel.common.profile.WheelProfile> profiles = new ArrayList<WheelProfile>();
        for (WheelProfile profile : collection.profiles()) {
            if (profile.id().equals("default") && !profile.wheels().containsKey(DefaultProfiles.ROOT_WHEEL_ID)) {
                profiles.add(DefaultProfiles.createDefaultProfile());
                changed = true;
            } else {
                // Custom profiles, including user-created Back entries, are not stock content to replace.
                profiles.add(profile);
            }
        }
        if (changed) {
            return new ProfileCollection(collection.activeProfileId(), profiles);
        } else {
            return collection;
        }
    }
}
