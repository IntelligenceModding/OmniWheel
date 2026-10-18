package de.artemis.omniwheel.client.render;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ListRowAnimationTest {

    @Test
    public void addingAndDeletingRowsMovesNeighborsContinuously() {
        de.artemis.omniwheel.client.render.ListRowAnimation<java.lang.String> animation = new ListRowAnimation<String>();
        long start = 1_000_000_000L;
        animation.update("list", de.artemis.omniwheel.common.Java8.list("a", "c"), value -> value, start, false);
        java.util.List<de.artemis.omniwheel.client.render.ListRowAnimation.Row<java.lang.String>> added = animation.update("list", de.artemis.omniwheel.common.Java8.list("a", "b", "c"), value -> value, start + 1, false);
        assertEquals(0, added.get(1).size(), 0);
        assertEquals(36, added.get(2).offset(), 0);
        java.util.List<de.artemis.omniwheel.client.render.ListRowAnimation.Row<java.lang.String>> midway = animation.update("list", de.artemis.omniwheel.common.Java8.list("a", "b", "c"), value -> value, start + 90_000_001, false);
        assertEquals(.5F, midway.get(1).size(), .001F);
        assertEquals(54, midway.get(2).offset(), .001F);
        java.util.List<de.artemis.omniwheel.client.render.ListRowAnimation.Row<java.lang.String>> removed = animation.update("list", de.artemis.omniwheel.common.Java8.list("a", "c"), value -> value, start + 90_000_001, false);
        assertTrue(removed.get(1).removing());
        assertEquals(midway.get(2).offset(), removed.get(2).offset(), 0);
        java.util.List<de.artemis.omniwheel.client.render.ListRowAnimation.Row<java.lang.String>> finished = animation.update("list", de.artemis.omniwheel.common.Java8.list("a", "c"), value -> value, start + 300_000_000, false);
        assertEquals(de.artemis.omniwheel.common.Java8.list("a", "c"), finished.stream().map(ListRowAnimation.Row::value).collect(de.artemis.omniwheel.common.Java8.toList()));
        assertEquals(36, finished.get(1).offset(), 0);
    }

    @Test
    public void switchingProfilesAndDraggingDoNotAnimateUnrelatedRows() {
        de.artemis.omniwheel.client.render.ListRowAnimation<java.lang.String> animation = new ListRowAnimation<String>();
        animation.update("first", de.artemis.omniwheel.common.Java8.list("a", "b"), value -> value, 1, false);
        java.util.List<de.artemis.omniwheel.client.render.ListRowAnimation.Row<java.lang.String>> switched = animation.update("second", de.artemis.omniwheel.common.Java8.list("c"), value -> value, 2, false);
        assertEquals(1, switched.size());
        assertEquals(1, switched.get(0).size(), 0);
        java.util.List<de.artemis.omniwheel.client.render.ListRowAnimation.Row<java.lang.String>> dragging = animation.update("second", de.artemis.omniwheel.common.Java8.list("d", "c"), value -> value, 3, true);
        assertEquals(1, dragging.get(0).size(), 0);
        assertEquals(36, dragging.get(1).offset(), 0);
    }
}
