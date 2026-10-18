package de.artemis.omniwheel.client.render;

import de.artemis.omniwheel.client.overlay.OmniWheelOverlay;
import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

public class IconClassificationTest {
    @BeforeClass
    public static void initializeRegistries() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test(timeout = 1000)
    public void embeddedImageIsNeverParsedAsAnItemOrHotkey() throws Exception {
        String image = "image:bmFtZQ:" + "A".repeat(4_000_000);
        var hotkey = OmniWheelOverlay.class.getDeclaredMethod("normalizeHotkeyGlyph", String.class);
        hotkey.setAccessible(true);
        for (int attempt = 0; attempt < 100; attempt++) {
            assertFalse(EntryIconRenderer.isItemIcon(image));
            assertNull(hotkey.invoke(null, image));
        }
    }

    @Test
    public void invalidItemIdsAndNonItemIconsAreRejected() {
        assertFalse(EntryIconRenderer.isItemIcon("NOT AN ITEM"));
        assertFalse(EntryIconRenderer.isItemIcon("effect:minecraft:speed"));
        assertFalse(EntryIconRenderer.isItemIcon("gui:hud/heart/full"));
        assertFalse(EntryIconRenderer.isItemIcon("symbol:plus"));
    }
}
