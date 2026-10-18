package de.artemis.omniwheel.client.navigation;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static de.artemis.omniwheel.client.navigation.ManagementNavigation.*;

public final class ManagementNavigationTest {

    private static Node node(String id, int panel, String group, int x, int y, int width, boolean visible, double order) {
        return new Node(id, panel, group, new Bounds(x, y, width, 22), visible, order);
    }

    @Test
    public void fieldsAndButtonsShareOneSpatialGraph() {
        Node field = node("field", 2, "", 0, 0, 160, true, 0);
        Node toggle = node("toggle", 2, "", 170, 0, 22, true, 1);
        Node next = node("next", 2, "", 0, 38, 160, true, 2);
        List<Node> nodes = de.artemis.omniwheel.common.Java8.list(field, toggle, next);
        assertEquals(toggle, move(nodes, field, Direction.RIGHT));
        assertEquals(field, move(nodes, toggle, Direction.LEFT));
        assertEquals(next, move(nodes, toggle, Direction.DOWN));
        assertEquals(field, move(nodes, next, Direction.UP));
    }

    @Test
    public void scrollingTraversesOffscreenRowsBeforeFooter() {
        Node first = node("first", 0, "profiles", 0, 0, 140, true, 0);
        Node second = node("second", 0, "profiles", 0, 36, 140, false, 1);
        Node footer = node("footer", 0, "", 0, 25, 140, true, 2);
        assertEquals(second, move(de.artemis.omniwheel.common.Java8.list(first, second, footer), first, Direction.DOWN));
    }

    @Test
    public void verticalNavigationStaysInGridColumnAndHandlesShortLastRow() {
        Node a = node("a", 2, "icons", 0, 0, 22, true, 0);
        Node b = node("b", 2, "icons", 30, 0, 22, true, 1);
        Node c = node("c", 2, "icons", 0, 30, 22, false, 2);
        List<Node> nodes = de.artemis.omniwheel.common.Java8.list(a, b, c);
        assertEquals(c, move(nodes, a, Direction.DOWN));
        assertEquals(c, move(nodes, b, Direction.DOWN));
        assertEquals(a, move(nodes, c, Direction.UP));
        assertEquals(b, move(nodes, b, Direction.RIGHT));
    }

    @Test
    public void horizontalEdgesSwitchOnlyToAdjacentPanel() {
        Node left = node("left", 0, "profiles", 0, 0, 140, true, 0);
        Node middle = node("middle", 1, "entries", 170, 0, 140, true, 0);
        Node right = node("right", 2, "", 340, 0, 200, true, 0);
        List<Node> nodes = de.artemis.omniwheel.common.Java8.list(left, middle, right);
        assertEquals(middle, move(nodes, left, Direction.RIGHT));
        assertEquals(right, move(nodes, middle, Direction.RIGHT));
        assertEquals(middle, move(nodes, right, Direction.LEFT));
        assertEquals(left, move(nodes, left, Direction.LEFT));
    }

    @Test
    public void tabTraversesEveryPanelAndWrapsIncludingOffscreenControls() {
        Node first = node("first", 0, "", 0, 0, 22, true, 0);
        Node hiddenRow = node("offscreen", 1, "entries", 0, 900, 22, false, 0);
        Node last = node("last", 2, "", 0, 0, 22, true, 0);
        List<Node> nodes = de.artemis.omniwheel.common.Java8.list(last, hiddenRow, first);
        assertEquals(hiddenRow, tab(nodes, first, false));
        assertEquals(first, tab(nodes, last, false));
        assertEquals(last, tab(nodes, first, true));
        assertEquals(first, tab(nodes, null, false));
        assertNull(tab(de.artemis.omniwheel.common.Java8.list(), null, false));
    }

    @Test
    public void removedFocusRecoversInReadingOrder() {
        Node removed = node("removed", 1, "", 0, 0, 22, true, 0);
        Node remaining = node("remaining", 2, "", 0, 0, 22, true, 0);
        assertEquals(remaining, tab(de.artemis.omniwheel.common.Java8.list(remaining), removed, false));
    }

    @Test
    public void enteringGridStartsAtFirstOptionAndLeavingReturnsToSearch() {
        Node search = node("search", 2, "", 0, 0, 160, true, 0);
        Node back = node("back", 2, "", 170, 0, 22, true, 1);
        Node first = node("first", 2, "icons", 0, 40, 22, true, 2);
        Node middle = node("middle", 2, "icons", 70, 40, 22, true, 3);
        Node last = node("last", 2, "icons", 170, 40, 22, true, 4);
        List<Node> nodes = de.artemis.omniwheel.common.Java8.list(search, back, first, middle, last);
        assertEquals(first, move(nodes, search, Direction.DOWN));
        assertEquals(first, move(nodes, back, Direction.DOWN));
        assertEquals(search, move(nodes, last, Direction.UP));
    }

    @Test
    public void entryListConnectsToEveryFooterButtonAndBack() {
        Node entry = node("entry", 1, "entries", 0, 200, 145, true, 0);
        Node command = node("command", 1, "", 0, 240, 74, true, 1);
        Node chat = node("chat", 1, "", 82, 240, 74, true, 2);
        Node submenu = node("submenu", 1, "", 0, 270, 74, true, 3);
        Node function = node("function", 1, "", 82, 270, 74, true, 4);
        Node keybind = node("keybind", 1, "", 0, 300, 74, true, 5);
        Node delete = node("delete", 1, "", 82, 300, 74, true, 6);
        Node toggle = node("toggle", 1, "", 0, 330, 156, true, 7);
        List<Node> nodes = de.artemis.omniwheel.common.Java8.list(entry, command, chat, submenu, function, keybind, delete, toggle);
        assertEquals(command, move(nodes, entry, Direction.DOWN));
        assertEquals(chat, move(nodes, command, Direction.RIGHT));
        assertEquals(function, move(nodes, chat, Direction.DOWN));
        assertEquals(delete, move(nodes, function, Direction.DOWN));
        assertEquals(keybind, move(nodes, delete, Direction.LEFT));
        assertEquals(submenu, move(nodes, keybind, Direction.UP));
        assertEquals(command, move(nodes, submenu, Direction.UP));
        assertEquals(entry, move(nodes, command, Direction.UP));
        assertEquals(toggle, move(nodes, delete, Direction.DOWN));
    }
}
