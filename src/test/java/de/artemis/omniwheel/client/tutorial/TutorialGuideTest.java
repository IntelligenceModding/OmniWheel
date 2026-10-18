package de.artemis.omniwheel.client.tutorial;

import de.artemis.omniwheel.client.navigation.ManagementNavigation.Bounds;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import com.google.gson.JsonParser;
import static org.junit.Assert.*;

public class TutorialGuideTest {
    @Test public void catalogCoversCurrentFeaturesAndHasCompleteReadableText() throws Exception {
        var language = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/omniwheel/lang/en_us.json"))).getAsJsonObject();
        for (var type : TutorialType.values()) {
            var ids = new HashSet<String>();
            for (var step : TutorialGuide.steps(type)) {
                assertTrue(ids.add(step.id()));
                assertTrue(language.has(step.titleKey(type)));
                assertTrue(language.has(step.bodyKey(type)));
                assertFalse(language.get(step.titleKey(type)).getAsString().isBlank());
                assertFalse(language.get(step.bodyKey(type)).getAsString().isBlank());
            }
        }
        assertTrue(TutorialGuide.MANAGER.stream().anyMatch(step -> step.scene() == TutorialGuide.Scene.DATA));
        assertTrue(TutorialGuide.MANAGER.stream().anyMatch(step -> step.scene() == TutorialGuide.Scene.COLORS && step.focus() == TutorialGuide.Focus.COLORS));
        assertTrue(TutorialGuide.MANAGER.stream().anyMatch(step -> step.scene() == TutorialGuide.Scene.ICONS));
        assertTrue(TutorialGuide.MANAGER.stream().anyMatch(step -> step.scene() == TutorialGuide.Scene.KEYBINDS));
        assertTrue(TutorialGuide.MANAGER.stream().anyMatch(step -> step.scene() == TutorialGuide.Scene.PREVIEW));
        assertTrue(TutorialGuide.MANAGER.stream().anyMatch(step -> step.scene() == TutorialGuide.Scene.SUBMENU));
        assertTrue(TutorialGuide.RADIAL.stream().anyMatch(step -> step.id().equals("pages")));
    }
    @Test public void dimmingExactlyCoversOutsideFocusWithoutOverlappingIt() {
        verifyPartition(960, 540, new Bounds(394, 44, 548, 126));
        verifyPartition(960, 540, new Bounds(18, 18, 176, 504));
        verifyPartition(320, 200, new Bounds(-40, 30, 140, 300));
        verifyPartition(320, 200, new Bounds(500, 300, 10, 10));
    }
    private static void verifyPartition(int width, int height, Bounds input) {
        var focus = TutorialSpotlight.clipped(width, height, input);
        var dimmed = TutorialSpotlight.dimmedRegions(width, height, input);
        long area = (long) focus.width() * focus.height();
        for (int i = 0; i < dimmed.size(); i++) {
            var region = dimmed.get(i);
            assertTrue(region.x() >= 0 && region.y() >= 0 && region.right() <= width && region.bottom() <= height);
            assertEquals(0, intersectionArea(region, focus));
            area += (long) region.width() * region.height();
            for (int j = i + 1; j < dimmed.size(); j++) assertEquals(0, intersectionArea(region, dimmed.get(j)));
        }
        assertEquals((long) width * height, area);
    }
    private static long intersectionArea(Bounds a, Bounds b) {
        return (long) Math.max(0, Math.min(a.right(), b.right()) - Math.max(a.x(), b.x()))
                * Math.max(0, Math.min(a.bottom(), b.bottom()) - Math.max(a.y(), b.y()));
    }
}
