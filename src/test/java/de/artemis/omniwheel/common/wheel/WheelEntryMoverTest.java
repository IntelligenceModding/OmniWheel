package de.artemis.omniwheel.common.wheel;

import de.artemis.omniwheel.common.action.ChatAction;
import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.profile.WheelProfile;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class WheelEntryMoverTest {

    private static WheelEntry entry(String id) {
        return new WheelEntry(id, id, "description", "X", new ChatAction("Hello", false));
    }

    private static WheelEntry submenu(String id, String wheelId) {
        return new WheelEntry(id, id, "description", "X", new OpenWheelAction(wheelId));
    }

    private static WheelDefinition wheel(String id, WheelEntry... entries) {
        return new WheelDefinition(id, id, "description", de.artemis.omniwheel.common.Numbers.clamp(entries.length, 1, 8), de.artemis.omniwheel.common.Java8.list(entries));
    }

    private static WheelProfile profile(WheelDefinition root, WheelDefinition... children) {
        java.util.LinkedHashMap<java.lang.String, de.artemis.omniwheel.common.wheel.WheelDefinition> wheels = new java.util.LinkedHashMap<String, WheelDefinition>();
        wheels.put(root.id(), root);
        for (de.artemis.omniwheel.common.wheel.WheelDefinition child : children) {
            wheels.put(child.id(), child);
        }
        return new WheelProfile("profile", "Profile", root.id(), wheels);
    }

    @Test
    public void reorderAtEveryBoundary() {
        List<WheelEntry> original = de.artemis.omniwheel.common.Java8.list(entry("a"), entry("b"), entry("c"), entry("d"), entry("e"));
        de.artemis.omniwheel.common.profile.WheelProfile profile = profile(wheel("root", original.toArray(new WheelEntry[0])));
        for (int source = 0; source < original.size(); source++) {
            for (int boundary = 0; boundary <= original.size(); boundary++) {
                de.artemis.omniwheel.common.wheel.WheelEntryMover.Result result = WheelEntryMover.move(profile, "root", original.get(source).id(), "root", boundary);
                java.util.List<de.artemis.omniwheel.common.wheel.WheelEntry> entries = result.profile().wheel("root").entries();
                check(entries.size() == original.size(), "Reordering changed the entry count");
                check(entries.stream().map(WheelEntry::id).distinct().count() == original.size(), "Reordering duplicated an entry");
                int index = boundary - (source < boundary ? 1 : 0);
                check(entries.get(index).equals(original.get(source)), "Entry landed at the wrong boundary");
                java.util.ArrayList<de.artemis.omniwheel.common.wheel.WheelEntry> remaining = new ArrayList<>(entries);
                remaining.remove(index);
                java.util.ArrayList<de.artemis.omniwheel.common.wheel.WheelEntry> expectedRemaining = new ArrayList<>(original);
                expectedRemaining.remove(source);
                check(remaining.equals(expectedRemaining), "Reordering changed the other entries' order");
                check(profile.wheel("root").entries().equals(original), "Reordering mutated the original profile");
            }
        }
    }

    @Test
    public void moveBetweenParents() {
        de.artemis.omniwheel.common.wheel.WheelDefinition nested = wheel("nested", entry("child"));
        de.artemis.omniwheel.common.wheel.WheelDefinition source = wheel("root", submenu("folder", "nested"), entry("same"));
        de.artemis.omniwheel.common.wheel.WheelDefinition destination = wheel("other", entry("same"));
        de.artemis.omniwheel.common.profile.WheelProfile original = profile(source, destination, nested);
        de.artemis.omniwheel.common.wheel.WheelEntryMover.Result moved = WheelEntryMover.move(original, "root", "same", "other", 0);
        check(moved.entryId().equals("same_2"), "ID collision was not resolved");
        check(moved.profile().wheel("other").entries().size() == 2, "Destination entry was lost");
        check(moved.profile().wheel("root").entries().size() == 1, "Source entry was not removed");
        check(moved.profile().wheel("other").entries().get(0).action().equals(source.entries().get(1).action()), "Move changed the action");
        de.artemis.omniwheel.common.wheel.WheelEntryMover.Result folderMoved = WheelEntryMover.move(original, "root", "folder", "other", 1);
        check(folderMoved.profile().wheel("nested").equals(nested), "Moving a submenu lost its contents");
        check(folderMoved.profile().wheel("other").entries().get(1).equals(source.entries().get(0)), "Submenu move changed its entry");
        de.artemis.omniwheel.common.wheel.WheelEntryMover.Result toRoot = WheelEntryMover.move(folderMoved.profile(), "other", "folder", "root", 0);
        check(toRoot.profile().wheel("root").entries().get(0).equals(source.entries().get(0)), "Moving back to root failed");
        de.artemis.omniwheel.common.profile.WheelProfile empty = profile(wheel("root", entry("last")), wheel("empty"));
        de.artemis.omniwheel.common.wheel.WheelEntryMover.Result toEmpty = WheelEntryMover.move(empty, "root", "last", "empty", 0);
        check(toEmpty.profile().wheel("root").entries().isEmpty(), "Source did not become empty");
        check(toEmpty.profile().wheel("empty").entries().get(0).id().equals("last"), "Move into empty submenu failed");
        check(toEmpty.profile().wheel("root").segmentCount() == 1, "Empty source has an invalid segment count");
    }

    @Test
    public void preserveEntrySettingsAndPagination() {
        de.artemis.omniwheel.common.action.ChatAction action = new ChatAction("Preset text", true, true);
        de.artemis.omniwheel.common.wheel.WheelEntry moving = new WheelEntry("same", "Label", "Description", "X", 0xFF112233, 0xFF334455, action, false, false, false, false, "Ctrl+G");
        java.util.ArrayList<de.artemis.omniwheel.common.wheel.WheelEntry> entries = new ArrayList<WheelEntry>();
        entries.add(entry("same"));
        entries.add(entry("same_2"));
        for (int index = 0; index < 10; index++) {
            entries.add(entry("other_" + index));
        }
        de.artemis.omniwheel.common.profile.WheelProfile source = profile(wheel("root", moving), wheel("destination", entries.toArray(new WheelEntry[0])));
        de.artemis.omniwheel.common.wheel.WheelEntryMover.Result result = WheelEntryMover.move(source, "root", "same", "destination", 100);
        de.artemis.omniwheel.common.wheel.WheelDefinition destination = result.profile().wheel("destination");
        de.artemis.omniwheel.common.wheel.WheelEntry expected = new WheelEntry("same_3", moving.label(), moving.description(), moving.glyph(), moving.color(), moving.guiColor(), action, false, false, false, false, "Ctrl+G");
        check(destination.entries().get(destination.entries().size() - 1).equals(expected), "Move lost entry settings or overwrote a colliding ID");
        check(destination.entries().size() == 13 && destination.segmentCount() == 8, "Move lost paginated entries");
        check(destination.active() == source.wheel("destination").active(), "Move changed wheel activation");
    }

    @Test
    public void rejectCycles() {
        de.artemis.omniwheel.common.profile.WheelProfile profile = profile(wheel("root", submenu("a", "a"), submenu("b", "b")), wheel("a", submenu("c", "c")), wheel("b"), wheel("c"));
        check(!WheelEntryMover.canMove(profile, "root", "a", "a"), "Allowed a submenu inside itself");
        check(!WheelEntryMover.canMove(profile, "root", "a", "c"), "Allowed a submenu inside its descendant");
        check(WheelEntryMover.canMove(profile, "root", "b", "c"), "Rejected a valid nested move");
        check(!WheelEntryMover.canMove(profile, "root", "missing", "b"), "Accepted a missing source entry");
        check(!WheelEntryMover.canMove(profile, "root", "a", "missing"), "Accepted a missing destination");
        check(!WheelEntryMover.canMove(profile, null, "a", "b"), "Accepted a null source");
        try {
            WheelEntryMover.move(profile, "root", "a", "c", 0);
            throw new AssertionError("Invalid move was committed");
        } catch (IllegalArgumentException expected) {
            check(profile.wheel("root").entries().size() == 2, "Rejected move modified the profile");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
