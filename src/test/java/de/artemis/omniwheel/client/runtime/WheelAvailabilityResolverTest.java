package de.artemis.omniwheel.client.runtime;

import net.minecraft.entity.player.ChatVisibility;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WheelAvailabilityResolverTest {
    @Test
    public void singleplayerAllowsMessagesAndCommandsWithOfflineAccount() {
        assertTrue(WheelAvailabilityResolver.canPlayerChat(true, false, ChatVisibility.FULL, false));
        assertTrue(WheelAvailabilityResolver.canPlayerChat(true, false, ChatVisibility.FULL, true));
    }

    @Test
    public void multiplayerRespectsAccountRestrictions() {
        assertFalse(WheelAvailabilityResolver.canPlayerChat(false, false, ChatVisibility.FULL, false));
        assertFalse(WheelAvailabilityResolver.canPlayerChat(false, false, ChatVisibility.FULL, true));
        assertTrue(WheelAvailabilityResolver.canPlayerChat(false, true, ChatVisibility.FULL, false));
        assertTrue(WheelAvailabilityResolver.canPlayerChat(false, true, ChatVisibility.FULL, true));
    }

    @Test
    public void chatVisibilityStillAppliesInSingleplayerAndMultiplayer() {
        for (boolean local : new boolean[]{false, true}) {
            assertFalse(WheelAvailabilityResolver.canPlayerChat(local, true, ChatVisibility.SYSTEM, false));
            assertTrue(WheelAvailabilityResolver.canPlayerChat(local, true, ChatVisibility.SYSTEM, true));
            assertFalse(WheelAvailabilityResolver.canPlayerChat(local, true, ChatVisibility.HIDDEN, false));
            assertFalse(WheelAvailabilityResolver.canPlayerChat(local, true, ChatVisibility.HIDDEN, true));
        }
    }
}
