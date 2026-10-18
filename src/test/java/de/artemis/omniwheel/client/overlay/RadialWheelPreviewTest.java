package de.artemis.omniwheel.client.overlay;

import de.artemis.omniwheel.client.render.RadialLayout;
import de.artemis.omniwheel.common.action.ChatAction;
import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public final class RadialWheelPreviewTest {
    private static WheelEntry entry(String id) { return new WheelEntry(id, id, "", "X", new ChatAction("Never execute", false)); }
    private static WheelEntry folder(String id, String wheel) { return new WheelEntry(id, id, "", "X", new OpenWheelAction(wheel)); }

    private static final class Harness implements RadialWheelPreview.Host {
        WheelProfile profile;
        String owner = "root", selected = "a";
        long now = 1_000_000_000L;
        int saves;
        boolean closed;
        final RadialWheelPreview preview;
        Harness() {
            List<WheelEntry> root = new ArrayList<>(List.of(entry("a"), folder("folder", "child"), entry("b"), folder("emptyFolder", "empty")));
            for (int i = 4; i < 10; i++) root.add(entry("e" + i));
            Map<String, WheelDefinition> wheels = new LinkedHashMap<>();
            wheels.put("root", new WheelDefinition("root", "Root", "", 8, root));
            wheels.put("child", new WheelDefinition("child", "Child", "", 2, List.of(entry("childA"), folder("nested", "grand"))));
            wheels.put("grand", new WheelDefinition("grand", "Grand", "", 1, List.of(entry("grandA"))));
            wheels.put("empty", new WheelDefinition("empty", "Empty", "", 1, List.of()));
            profile = new WheelProfile("test", "Test", "root", wheels);
            preview = new RadialWheelPreview(this, () -> now);
            preview.sync(owner, selected);
            preview.layout(0, 0, 700, 460);
        }
        public WheelProfile profile() { return profile; }
        public void closePreview() { closed = true; }
        public void select(String wheel, String entry) { owner = wheel; selected = entry; }
        public void save(WheelProfile updated, String wheel, String entry) { profile = updated; owner = wheel; selected = entry; saves++; }
        public void button(GuiGraphics g, Minecraft m, int x, int y, int w, int height, String label, boolean enabled, Runnable action, String id) {}
        public void target(int x, int y, int w, int h, String id, Runnable action) {}
        public boolean focused(String id) { return false; }
        public void draggedEntry(GuiGraphics g, Minecraft m, WheelEntry e, String owner, int x, int y, int w) {}
        float[] point(int slot, int count, float radius) {
            double angle = RadialLayout.segmentMidAngle(slot, count);
            float scale = (460 - 136) / 256F;
            return new float[]{350 + (float)Math.cos(angle) * radius * scale, 232 + (float)Math.sin(angle) * radius * scale};
        }
        void press(int slot, int count) { float[] p = point(slot, count, 85); assertTrue(preview.press(p[0], p[1], false)); }
        void over(int slot, int count, float radius) { float[] p = point(slot, count, radius); preview.update(p[0], p[1], true); }
        void drop(int slot, int count, float radius) { float[] p = point(slot, count, radius); assertTrue(preview.release(p[0], p[1])); }
        void hold(int slot, int count) { over(slot, count, 85); now += 700_000_000L; over(slot, count, 85); preview.sync(owner, selected); }
        List<String> ids(String wheel) { return profile.wheel(wheel).radialEntries().stream().map(WheelEntry::id).toList(); }
    }

    @Test public void droppingOnFolderMovesInsideAndSelectsDestination() {
        Harness h = new Harness();
        h.press(0, 8);
        h.over(1, 8, 85);
        assertEquals(0, h.saves);
        h.drop(1, 8, 85);
        assertFalse(h.ids("root").contains("a"));
        assertEquals(List.of("childA", "nested", "a"), h.ids("child"));
        assertEquals("child", h.owner);
        assertEquals("a", h.selected);
        assertEquals(1, h.saves);
    }

    @Test public void heldSubmenuOpensWithoutCommittingAndAllowsExactInsertion() {
        Harness h = new Harness();
        h.press(0, 8);
        h.hold(1, 8);
        assertEquals("child", h.owner);
        assertTrue(h.preview.isDragging());
        assertEquals(0, h.saves);
        assertTrue(h.ids("root").contains("a"));
        h.over(0, 3, 114);
        h.drop(0, 3, 114);
        assertEquals(List.of("a", "childA", "nested"), h.ids("child"));
    }

    @Test public void emptyWheelAcceptsDropInCenterAfterHoverNavigation() {
        Harness h = new Harness();
        h.press(0, 8);
        h.hold(3, 8);
        assertEquals("empty", h.owner);
        assertTrue(h.preview.release(350, 232));
        assertEquals(List.of("a"), h.ids("empty"));
        assertFalse(h.ids("root").contains("a"));
    }

    @Test public void backButtonMovesEntriesUpWithoutExecutingAnything() {
        Harness h = new Harness();
        h.owner = "grand"; h.selected = "grandA"; h.preview.sync(h.owner, h.selected);
        assertTrue(h.preview.press(350, 232, false));
        assertEquals("child", h.owner);
        assertFalse(h.preview.hasDrag());
        assertTrue(h.preview.press(350, 232, false));
        assertEquals("root", h.owner);
        assertEquals(0, h.saves);
        h.owner = "grand"; h.selected = "grandA"; h.preview.sync(h.owner, h.selected);
        h.press(0, 1);
        h.preview.update(674, -14, true);
        h.preview.release(674, -14);
        assertEquals("child", h.owner);
        assertEquals(List.of("childA", "nested", "grandA"), h.ids("child"));
        h.preview.sync(h.owner, h.selected);
        h.press(2, 3);
        h.preview.update(674, -14, true);
        h.preview.release(674, -14);
        assertEquals("root", h.owner);
        assertEquals("grandA", h.ids("root").get(h.ids("root").size() - 1));
        assertTrue(h.ids("grand").isEmpty());
        h.preview.back();
        assertTrue(h.closed);
    }

    @Test public void centerMovesOutAndHoldingBackKeepsTheDragAlive() {
        Harness h = new Harness();
        h.owner = "child"; h.selected = "childA"; h.preview.sync(h.owner, h.selected);
        h.press(0, 2);
        h.preview.update(350, 232, true);
        h.preview.release(350, 232);
        assertEquals("root", h.owner);
        assertEquals("childA", h.ids("root").get(h.ids("root").size() - 1));
        h.owner = "grand"; h.selected = "grandA"; h.preview.sync(h.owner, h.selected);
        h.press(0, 1);
        h.preview.update(674, -14, true);
        h.now += 700_000_000L;
        h.preview.update(674, -14, true);
        h.preview.sync(h.owner, h.selected);
        assertEquals("child", h.owner);
        assertTrue(h.preview.hasDrag());
        h.over(0, 2, 114);
        h.drop(0, 2, 114);
        assertEquals("grandA", h.ids("child").get(0));
        h.preview.sync(h.owner, h.selected);
        h.press(0, 2);
        h.preview.update(350, 232, true);
        h.now += 700_000_000L;
        h.preview.update(350, 232, true);
        assertEquals("root", h.owner);
        assertTrue(h.preview.isDragging());
        h.preview.release(350, 232);
        assertEquals("grandA", h.ids("root").get(h.ids("root").size() - 1));
    }

    @Test public void scrollAndHoverArrowsSupportMovesAcrossPages() {
        Harness h = new Harness();
        h.press(0, 8); h.over(2, 8, 114);
        h.preview.scroll(350, 232, -1);
        h.over(1, 2, 114); h.drop(1, 2, 114);
        assertEquals("a", h.ids("root").get(h.ids("root").size() - 1));
        h.preview.sync(h.owner, h.selected);
        h.press(1, 2);
        h.preview.update(154, 232, true);
        h.now += 600_000_000L;
        h.preview.update(154, 232, true);
        h.over(0, 8, 114); h.drop(0, 8, 114);
        assertEquals("a", h.ids("root").get(0));
    }

    @Test public void navigatingIntoFullSubmenuCanAppendOnItsNewLastPage() {
        Harness h = new Harness();
        var wheels = new LinkedHashMap<>(h.profile.wheels());
        List<WheelEntry> children = new ArrayList<>();
        for (int i = 0; i < 8; i++) children.add(entry("c" + i));
        wheels.put("child", new WheelDefinition("child", "Child", "", 8, children));
        h.profile = new WheelProfile("test", "Test", "root", wheels);
        h.press(0, 8); h.hold(1, 8);
        h.preview.scroll(350, 232, -1);
        h.over(0, 1, 85); h.drop(0, 1, 85);
        assertEquals(9, h.ids("child").size());
        assertEquals("a", h.ids("child").get(h.ids("child").size() - 1));
    }

    @Test public void invalidCyclesAndCancellationLeaveProfileUntouched() {
        Harness h = new Harness();
        var wheels = new LinkedHashMap<>(h.profile.wheels());
        var root = new ArrayList<>(h.profile.wheel("root").entries());
        root.set(3, folder("alias", "grand"));
        wheels.put("root", new WheelDefinition("root", "Root", "", 8, root));
        h.profile = new WheelProfile("test", "Test", "root", wheels);
        WheelProfile before = h.profile;
        h.press(1, 8); h.hold(3, 8); h.drop(3, 8, 85);
        assertSame(before, h.profile);
        assertEquals("root", h.owner);
        assertEquals(0, h.saves);
        h.press(0, 8); h.hold(1, 8);
        assertEquals("child", h.owner);
        h.preview.cancelDrag();
        assertFalse(h.preview.hasDrag());
        assertSame(before, h.profile);
        h.preview.sync(h.owner, h.selected);
        h.press(0, 2); h.preview.update(-100, -100, true); h.preview.release(-100, -100);
        assertSame(before, h.profile);
    }

    @Test public void duplicateIdsInDestinationPreserveBothEntries() {
        Harness h = new Harness();
        var wheels = new LinkedHashMap<>(h.profile.wheels());
        wheels.put("child", new WheelDefinition("child", "Child", "", 1, List.of(entry("a"))));
        h.profile = new WheelProfile("test", "Test", "root", wheels);
        h.press(0, 8); h.over(1, 8, 85); h.drop(1, 8, 85);
        assertEquals(List.of("a", "a_2"), h.ids("child"));
        assertEquals("a_2", h.selected);
    }

    @Test public void movingFolderAcceptsDropAtItsVisiblePositionNearOuterEdge() {
        Harness h = new Harness();
        List<WheelEntry> original = h.profile.wheel("root").entries().subList(0, 8);
        h.preview.animatedAngles(original, -1);
        h.press(0, 8);
        h.over(3, 8, 114);
        var projected = new ArrayList<>(original);
        WheelEntry moved = projected.remove(0);
        projected.add(3, moved);
        h.preview.animatedAngles(projected, 3);
        // Folder is now logically slot 0, but its visible animation is still at slot 1.
        h.over(1, 8, 111);
        h.now += 200_000_000L;
        double[] angles = h.preview.animatedAngles(projected, 3);
        assertTrue("folder continues settling instead of freezing", Math.abs(angles[0]) < Math.PI / 8);
        h.drop(1, 8, 111);
        assertEquals("a", h.ids("child").get(h.ids("child").size() - 1));
        assertFalse(h.ids("root").contains("a"));
    }

    @Test public void clickingAnAnimatingEntrySelectsTheVisibleEntry() {
        Harness h = new Harness();
        h.preview.animatedAngles(h.profile.wheel("root").entries().subList(0, 8), -1);
        var entries = new ArrayList<>(h.profile.wheel("root").entries());
        entries.add(3, entries.remove(0));
        var wheels = new LinkedHashMap<>(h.profile.wheels());
        wheels.put("root", new WheelDefinition("root", "Root", "", 8, entries, true,
                entries.stream().map(WheelEntry::id).toList()));
        h.profile = new WheelProfile("test", "Test", "root", wheels);
        h.preview.animatedAngles(entries.subList(0, 8), -1);
        h.press(1, 8);
        assertEquals("folder", h.selected);
        assertEquals(0, h.saves);
    }

    @Test public void reversingReorderThenEnteringSubmenuLetsOverlappingWedgesSettle() {
        Harness h = new Harness();
        List<WheelEntry> original = h.profile.wheel("root").entries().subList(0, 8);
        h.preview.animatedAngles(original, -1);
        h.press(0, 8);
        h.over(3, 8, 114);
        var shifted = new ArrayList<>(original);
        shifted.add(3, shifted.remove(0));
        h.now += 60_000_000L;
        h.preview.animatedAngles(shifted, 3);
        h.over(0, 8, 114);
        double[] before = h.preview.animatedAngles(original, 0);
        double angle = RadialLayout.segmentMidAngle(1, 8) + before[1];
        float scale = (460 - 136) / 256F;
        float x = 350 + (float)Math.cos(angle) * 85 * scale;
        float y = 232 + (float)Math.sin(angle) * 85 * scale;
        h.preview.update(x, y, true);
        h.now += 200_000_000L;
        double[] after = h.preview.animatedAngles(original, 0);
        assertTrue("transition finishes while hovering inside the folder", Math.abs(after[1]) < Math.abs(before[1]) / 2);
        h.preview.update(x, y, true);
        h.preview.release(x, y);
        assertEquals("a", h.ids("child").get(h.ids("child").size() - 1));
        assertFalse(h.ids("root").contains("a"));
    }
}
