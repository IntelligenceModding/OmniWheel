package de.artemis.omniwheel.client.render;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExpansionAnimationTest {
    @Test public void quickReversalsContinueFromTheVisiblePosition() {
        var animation = new ExpansionAnimation(false);
        long start = 1_000_000_000L;
        animation.setExpanded(true, start);
        assertEquals(0, animation.amount(start), 0);
        float midway = animation.amount(start + 90_000_000L);
        assertEquals(.5F, midway, .00001F);
        animation.setExpanded(false, start + 90_000_000L);
        assertEquals(midway, animation.amount(start + 90_000_000L), 0);
        assertTrue(animation.amount(start + 135_000_000L) < midway);
        assertEquals(0, animation.amount(start + 270_000_000L), 0);
    }

    @Test public void repeatedLayoutQueriesDoNotRestartTheMotion() {
        var animation = new ExpansionAnimation(true);
        long start = 1_000_000_000L;
        assertEquals(1, animation.amount(start), 0);
        animation.setExpanded(false, start);
        animation.setExpanded(false, start + 90_000_000L);
        assertEquals(.5F, animation.amount(start + 90_000_000L), .00001F);
        assertEquals(0, animation.amount(start + 180_000_000L), 0);
    }
}
