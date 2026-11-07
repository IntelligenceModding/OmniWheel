package de.artemis.omniwheel.client.profile.store;

import de.artemis.omniwheel.client.profile.DefaultProfiles;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.action.CommandAction;
import de.artemis.omniwheel.common.action.OpenChatAction;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

final class ProfileUpgrader {
    private ProfileUpgrader() {}

    static ProfileCollection upgrade(ProfileCollection collection) {
        boolean changed = false;
        var profiles = new ArrayList<WheelProfile>();
        for (WheelProfile profile : collection.profiles()) {
            if (profile.id().equals("default") && !profile.wheels().containsKey(DefaultProfiles.ROOT_WHEEL_ID)) {
                profiles.add(DefaultProfiles.createDefaultProfile());
                changed = true;
            } else {
                WheelProfile updated = profile.id().equals("default") ? upgradeEditorSettings(profile) : profile;
                profiles.add(updated);
                changed |= updated != profile;
            }
        }
        return changed ? new ProfileCollection(collection.activeProfileId(), profiles) : collection;
    }

    /** Replace only the old stock settings; retain user text, appearance, entries, and both orders. */
    private static WheelProfile upgradeEditorSettings(WheelProfile profile) {
        var wheels = new LinkedHashMap<>(profile.wheels());
        boolean changed = false;
        for (WheelDefinition wheel : profile.wheels().values()) {
            String title = wheel.id().equals(DefaultProfiles.ROOT_WHEEL_ID) ? "main" : wheel.id();
            boolean stockDescription = wheel.description().equals("omniwheel.default.wheel." + title + ".description");
            var entries = new ArrayList<WheelEntry>();
            boolean changedEntry = false;
            for (WheelEntry entry : wheel.entries()) {
                if (entry.id().equals("whisper") && entry.action() instanceof OpenChatAction chat
                        && chat.initialText().trim().equals("/tell")) {
                    entries.add(new WheelEntry(entry.id(), entry.label(), entry.description(), entry.glyph(),
                            entry.color(), entry.guiColor(), new CommandAction(List.of("/tell"), false, true),
                            entry.active(), entry.showIcon(), entry.showLabel(), entry.showShortcut(), entry.shortcut()));
                    changedEntry = true;
                } else {
                    entries.add(entry);
                }
            }
            if (stockDescription || changedEntry) {
                wheels.put(wheel.id(), new WheelDefinition(wheel.id(), wheel.title(), stockDescription ? "" : wheel.description(),
                        stockDescription ? de.artemis.omniwheel.common.Numbers.clamp(entries.size(), 1, 8) : wheel.segmentCount(), entries,
                        wheel.active(), wheel.radialOrder()));
                changed = true;
            }
        }
        return changed ? new WheelProfile(profile.id(), profile.displayName(), profile.rootWheelId(), wheels) : profile;
    }
}
