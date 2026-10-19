package de.artemis.omniwheel.client.profile.store;

import de.artemis.omniwheel.client.profile.DefaultProfiles;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import de.artemis.omniwheel.common.wheel.WheelEntryMover;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public final class ProfileOrderUpgradeTest {
    @Test public void defaultProfileUpgradePreservesBothCustomizedOrders() {
        var profile = DefaultProfiles.createDefaultProfile();
        profile = WheelEntryMover.move(profile, DefaultProfiles.ROOT_WHEEL_ID, "inventory", DefaultProfiles.ROOT_WHEEL_ID, 5).profile();
        profile = WheelEntryMover.moveRadial(profile, DefaultProfiles.ROOT_WHEEL_ID, "view", DefaultProfiles.ROOT_WHEEL_ID, 0).profile();
        var editor = profile.wheel(DefaultProfiles.ROOT_WHEEL_ID).entries().stream().map(WheelEntry::id).toList();
        var radial = profile.wheel(DefaultProfiles.ROOT_WHEEL_ID).radialOrder();
        var collection = new ProfileCollection(profile.id(), List.of(profile));
        for (int reload = 0; reload < 2; reload++) {
            collection = ProfileUpgrader.upgrade(collection);
            assertEquals(editor, collection.activeProfile().wheel(DefaultProfiles.ROOT_WHEEL_ID).entries().stream().map(WheelEntry::id).toList());
            assertEquals(radial, collection.activeProfile().wheel(DefaultProfiles.ROOT_WHEEL_ID).radialOrder());
        }
    }
}
