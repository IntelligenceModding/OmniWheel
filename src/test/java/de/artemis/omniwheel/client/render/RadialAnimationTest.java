package de.artemis.omniwheel.client.render;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class RadialAnimationTest {

    @Test
    public void submenuAndPageChangesSettleWithoutChangingInitialOpening() {
        de.artemis.omniwheel.client.render.RadialAnimation animation = new RadialAnimation();
        long start = 1_000_000_000L;
        animation.open(start);
        animation.update(start, "root/0", de.artemis.omniwheel.common.Java8.list("a"), -1, false);
        assertEquals(1, animation.viewScale(start), 0);
        assertEquals(.965F, animation.openingScale(start), .00001F);
        long changed = start + 300_000_000L;
        animation.update(changed, "submenu/0", de.artemis.omniwheel.common.Java8.list("b"), -1, true);
        assertEquals(.955F, animation.viewScale(changed), .00001F);
        assertTrue(animation.viewScale(changed + 80_000_000L) > .955F);
        assertEquals(1, animation.viewScale(changed + 160_000_000L), 0);
        float hover = animation.centerSelection();
        animation.update(changed + 20_000_000L, "submenu/0", de.artemis.omniwheel.common.Java8.list("b"), -1, false);
        assertTrue(animation.centerSelection() < hover);
        animation.update(changed + 200_000_000L, "root/0", de.artemis.omniwheel.common.Java8.list("a"), -1, false);
        assertEquals(.955F, animation.viewScale(changed + 200_000_000L), .00001F);
        animation.reset();
        assertEquals(1, animation.viewScale(changed + 200_000_000L), 0);
        assertEquals(0, animation.centerSelection(), 0);
    }

    @Test
    public void selectionTimingDoesNotDependOnFrameRate() {
        RadialAnimation slow = new RadialAnimation();
        RadialAnimation fast = new RadialAnimation();
        long start = 1_000_000_000L;
        slow.open(start);
        fast.open(start);
        for (int frame = 1; frame <= 6; frame++) slow.update(start + frame * 20_000_000L, "root", de.artemis.omniwheel.common.Java8.list("a", "b"), 0);
        for (int frame = 1; frame <= 24; frame++) fast.update(start + frame * 5_000_000L, "root", de.artemis.omniwheel.common.Java8.list("a", "b"), 0);
        assertEquals(slow.selection(0), fast.selection(0), 0.00001F);
        assertTrue(fast.selection(0) > 0.94F);
        assertTrue(fast.selection(0) < 1);
    }

    @Test
    public void fastRetargetingSettlesOldSelectionAndDoesNotLeakIntoNewViews() {
        RadialAnimation animation = new RadialAnimation();
        long start = 1_000_000_000L;
        animation.open(start);
        animation.update(start + 50_000_000L, "root/0", de.artemis.omniwheel.common.Java8.list("a", "b"), 0);
        float old = animation.selection(0);
        animation.update(start + 70_000_000L, "root/0", de.artemis.omniwheel.common.Java8.list("a", "b"), 1);
        assertTrue(animation.selection(0) > 0 && animation.selection(0) < old);
        assertTrue(animation.selection(1) > 0 && animation.selection(1) < 1);
        animation.update(start + 90_000_000L, "root/1", de.artemis.omniwheel.common.Java8.list("c", "d"), -1);
        assertEquals(0, animation.selection(0), 0);
        assertEquals(0, animation.selection(1), 0);
        animation.update(start + 110_000_000L, "submenu", de.artemis.omniwheel.common.Java8.list("a"), 0);
        assertTrue(animation.selection(0) > 0 && animation.selection(0) < 0.5F);
        animation.open(start + 120_000_000L);
        animation.update(start + 120_000_000L, "root/0", de.artemis.omniwheel.common.Java8.list("a", "b"), -1);
        assertEquals(0, animation.selection(0), 0);
    }

    @Test
    public void openingSettlesWithoutOvershootAndColorsKeepTheirEndpoints() {
        RadialAnimation animation = new RadialAnimation();
        long start = 1_000_000_000L;
        animation.open(start);
        assertEquals(0.965F, animation.openingScale(start), 0.00001F);
        assertTrue(animation.openingScale(start + 70_000_000L) > 0.965F);
        assertTrue(animation.openingScale(start + 70_000_000L) < 1);
        assertEquals(1, animation.openingScale(start + 140_000_000L), 0);
        assertEquals(1, animation.openingScale(start + 5_000_000_000L), 0);
        assertEquals(0xD8212831, RadialAnimation.blendColor(0xD8212831, 0xF042556C, 0));
        assertEquals(0xF042556C, RadialAnimation.blendColor(0xD8212831, 0xF042556C, 1));
    }

    @Test
    public void closingIsContinuousFinishesAndCanBeInterruptedByReopening() {
        RadialAnimation animation = new RadialAnimation();
        long start = 1_000_000_000L;
        animation.open(start);
        float scale = animation.openingScale(start + 40_000_000L);
        long closed = start + 40_000_000L;
        animation.close(closed, scale);
        assertEquals(scale, animation.closingScale(closed), 0);
        assertTrue(animation.isClosing(closed));
        assertTrue(animation.closingScale(closed + 70_000_000L) < scale);
        assertTrue(animation.closingScale(closed + 70_000_000L) > 0);
        assertFalse(animation.isClosing(closed + 140_000_000L));
        assertEquals(0, animation.closingScale(closed + 140_000_000L), 0);
        animation.open(closed + 80_000_000L);
        assertFalse(animation.isClosing(closed + 80_000_000L));
        assertEquals(0.965F, animation.openingScale(closed + 80_000_000L), 0.00001F);
    }
}
