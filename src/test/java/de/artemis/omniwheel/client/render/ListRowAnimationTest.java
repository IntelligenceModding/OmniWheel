package de.artemis.omniwheel.client.render;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ListRowAnimationTest {
    @Test public void addingAndDeletingRowsMovesNeighborsContinuously() {
        var animation = new ListRowAnimation<String>();
        long start = 1_000_000_000L;
        animation.update("list", List.of("a", "c"), value -> value, start, false);
        var added = animation.update("list", List.of("a", "b", "c"), value -> value, start + 1, false);
        assertEquals(0, added.get(1).size(), 0);
        assertEquals(36, added.get(2).offset(), 0);
        var midway = animation.update("list", List.of("a", "b", "c"), value -> value, start + 90_000_001, false);
        assertEquals(.5F, midway.get(1).size(), .001F);
        assertEquals(54, midway.get(2).offset(), .001F);
        var removed = animation.update("list", List.of("a", "c"), value -> value, start + 90_000_001, false);
        assertTrue(removed.get(1).removing());
        assertEquals(midway.get(2).offset(), removed.get(2).offset(), 0);
        var finished = animation.update("list", List.of("a", "c"), value -> value, start + 300_000_000, false);
        assertEquals(List.of("a", "c"), finished.stream().map(ListRowAnimation.Row::value).toList());
        assertEquals(36, finished.get(1).offset(), 0);
    }

    @Test public void switchingProfilesAndDraggingDoNotAnimateUnrelatedRows() {
        var animation = new ListRowAnimation<String>();
        animation.update("first", List.of("a", "b"), value -> value, 1, false);
        var switched = animation.update("second", List.of("c"), value -> value, 2, false);
        assertEquals(1, switched.size()); assertEquals(1, switched.get(0).size(), 0);
        var dragging = animation.update("second", List.of("d", "c"), value -> value, 3, true);
        assertEquals(1, dragging.get(0).size(), 0);
        assertEquals(36, dragging.get(1).offset(), 0);
    }
}
