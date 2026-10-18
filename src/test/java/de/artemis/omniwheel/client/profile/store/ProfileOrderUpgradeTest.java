package de.artemis.omniwheel.client.profile.store;

import de.artemis.omniwheel.client.profile.DefaultProfiles;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import de.artemis.omniwheel.common.wheel.WheelEntryMover;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public final class ProfileOrderUpgradeTest {

    @Test
    public void defaultProfileUpgradePreservesBothCustomizedOrders() {
        de.artemis.omniwheel.common.profile.WheelProfile profile = DefaultProfiles.createDefaultProfile();
        profile = WheelEntryMover.move(profile, DefaultProfiles.ROOT_WHEEL_ID, "inventory", DefaultProfiles.ROOT_WHEEL_ID, 5).profile();
        profile = WheelEntryMover.moveRadial(profile, DefaultProfiles.ROOT_WHEEL_ID, "view", DefaultProfiles.ROOT_WHEEL_ID, 0).profile();
        java.util.List<java.lang.String> editor = profile.wheel(DefaultProfiles.ROOT_WHEEL_ID).entries().stream().map(WheelEntry::id).collect(de.artemis.omniwheel.common.Java8.toList());
        java.util.List<java.lang.String> radial = profile.wheel(DefaultProfiles.ROOT_WHEEL_ID).radialOrder();
        de.artemis.omniwheel.client.profile.store.ProfileCollection collection = new ProfileCollection(profile.id(), de.artemis.omniwheel.common.Java8.list(profile));
        for (int reload = 0; reload < 2; reload++) {
            collection = ProfileUpgrader.upgrade(collection);
            assertEquals(editor, collection.activeProfile().wheel(DefaultProfiles.ROOT_WHEEL_ID).entries().stream().map(WheelEntry::id).collect(de.artemis.omniwheel.common.Java8.toList()));
            assertEquals(radial, collection.activeProfile().wheel(DefaultProfiles.ROOT_WHEEL_ID).radialOrder());
        }
    }
}
