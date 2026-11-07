package de.artemis.omniwheel.client.render;

import org.junit.Test;
import static org.junit.Assert.*;

public class HighlightAnimationTest {
    @Test public void fadesBothDirectionsAndReversesWithoutJumping() {
        var animation = new HighlightAnimation();
        long start = 1_000_000_000L;
        assertEquals(0, animation.amount("field", true, start), 0);
        float raised = animation.amount("field", true, start + 50_000_000L);
        assertTrue(raised > 0 && raised < 1);
        assertEquals(raised, animation.amount("field", false, start + 50_000_000L), 0);
        float lowered = animation.amount("field", false, start + 100_000_000L);
        assertTrue(lowered > 0 && lowered < raised);
        assertTrue(animation.amount("field", true, start + 150_000_000L) > lowered);
    }

    @Test public void timingDoesNotDependOnFrameRate() {
        var fast = new HighlightAnimation();
        var slow = new HighlightAnimation();
        long start = 1_000_000_000L;
        fast.amount("button", true, start);
        slow.amount("field", true, start);
        float fine = 0;
        for (int i = 1; i <= 10; i++) fine = fast.amount("button", true, start + i * 10_000_000L);
        assertEquals(fine, slow.amount("field", true, start + 100_000_000L), .00001F);
    }

    @Test public void independentControlsAndReopenedViewsDoNotInheritHighlights() {
        var animation = new HighlightAnimation();
        long start = 1_000_000_000L;
        animation.amount("first", true, start);
        assertTrue(animation.amount("first", true, start + 100_000_000L) > 0);
        assertEquals(0, animation.amount("second", false, start + 100_000_000L), 0);
        assertEquals(0, animation.amount("first", true, start + 400_000_000L), 0);
        animation.clear();
        assertEquals(0, animation.amount("first", true, start + 410_000_000L), 0);
    }
}
