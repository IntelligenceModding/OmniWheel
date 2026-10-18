package de.artemis.omniwheel.client.overlay;

import net.minecraft.resources.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class CreativeTabSearchTest {
    @BeforeClass
    public static void initializeRegistries() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void specialCreativeTabsDoNotPreventItemSearchIndexing() throws Exception {
        var loader = OmniWheelProfilesOverlay.class.getDeclaredMethod("loadCreativeTabSearchTerms");
        loader.setAccessible(true);
        var terms = (Map<ResourceLocation, String>) loader.invoke(null);
        assertFalse(terms.getOrDefault(new ResourceLocation("minecraft:stone"), "").isBlank());
        assertFalse(terms.getOrDefault(new ResourceLocation("minecraft:diamond_sword"), "").isBlank());
    }
}
