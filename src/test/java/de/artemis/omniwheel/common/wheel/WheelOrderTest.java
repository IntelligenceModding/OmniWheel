package de.artemis.omniwheel.common.wheel;

import de.artemis.omniwheel.client.profile.store.ProfileCollection;
import de.artemis.omniwheel.client.profile.store.ProfileJsonCodec;
import de.artemis.omniwheel.common.action.LocalMessageAction;
import de.artemis.omniwheel.common.profile.WheelProfile;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public final class WheelOrderTest {

    private WheelEntry entry(String id) {
        return new WheelEntry(id, id, "", "X", new LocalMessageAction(id));
    }

    private WheelProfile profile() {
        return new WheelProfile("test", "Test", "root", de.artemis.omniwheel.common.Java8.map("root", new WheelDefinition("root", "Root", "", 3, de.artemis.omniwheel.common.Java8.list(entry("a"), entry("b"), entry("c"))), "child", new WheelDefinition("child", "Child", "", 1, de.artemis.omniwheel.common.Java8.list(entry("x")))));
    }

    private List<String> editor(WheelProfile profile, String wheel) {
        return profile.wheel(wheel).entries().stream().map(WheelEntry::id).collect(de.artemis.omniwheel.common.Java8.toList());
    }

    private List<String> radial(WheelProfile profile, String wheel) {
        return profile.wheel(wheel).radialOrder();
    }

    @Test
    public void listReorderDoesNotChangeWheelPositions() {
        WheelProfile changed = WheelEntryMover.move(profile(), "root", "a", "root", 3).profile();
        assertEquals(de.artemis.omniwheel.common.Java8.list("b", "c", "a"), editor(changed, "root"));
        assertEquals(de.artemis.omniwheel.common.Java8.list("a", "b", "c"), radial(changed, "root"));
    }

    @Test
    public void previewReorderDoesNotReorganizeEditorRows() {
        WheelProfile changed = WheelEntryMover.move(profile(), "root", "a", "root", 3).profile();
        changed = WheelEntryMover.moveRadial(changed, "root", "a", "root", 2).profile();
        assertEquals(de.artemis.omniwheel.common.Java8.list("b", "c", "a"), editor(changed, "root"));
        assertEquals(de.artemis.omniwheel.common.Java8.list("b", "a", "c"), radial(changed, "root"));
        ProfileJsonCodec codec = new ProfileJsonCodec();
        assertEquals(changed, codec.decode(codec.encode(new ProfileCollection("test", de.artemis.omniwheel.common.Java8.list(changed)))).activeProfile());
    }

    @Test
    public void previewSubmenuMoveUsesChosenRadialSlotAndAppendsEditorRow() {
        WheelProfile changed = WheelEntryMover.moveRadial(profile(), "root", "b", "child", 0).profile();
        assertEquals(de.artemis.omniwheel.common.Java8.list("a", "c"), radial(changed, "root"));
        assertEquals(de.artemis.omniwheel.common.Java8.list("a", "c"), editor(changed, "root"));
        assertEquals(de.artemis.omniwheel.common.Java8.list("x", "b"), editor(changed, "child"));
        assertEquals(de.artemis.omniwheel.common.Java8.list("b", "x"), radial(changed, "child"));
    }

    @Test
    public void legacyProfilesKeepTheirExistingLayoutWithoutRadialOrderField() {
        ProfileJsonCodec codec = new ProfileJsonCodec();
        String saved = codec.encode(new ProfileCollection("test", de.artemis.omniwheel.common.Java8.list(profile())));
        String legacy = saved.replaceAll("(?s),?\\s*\"radialOrder\"\\s*:\\s*\\[[^]]*]", "");
        WheelProfile loaded = codec.decode(legacy).activeProfile();
        assertEquals(editor(loaded, "root"), radial(loaded, "root"));
        assertEquals(editor(loaded, "child"), radial(loaded, "child"));
    }

    @Test
    public void editsAndAddedDeletedEntriesPreserveExistingRadialPositions() {
        WheelDefinition wheel = WheelEntryMover.moveRadial(profile(), "root", "c", "root", 0).profile().wheel("root");
        java.util.ArrayList<de.artemis.omniwheel.common.wheel.WheelEntry> entries = new ArrayList<>(wheel.entries());
        entries.set(0, new WheelEntry("a", "Renamed", "", "X", new LocalMessageAction("changed")));
        entries.removeIf(value -> value.id().equals("b"));
        entries.add(0, entry("new"));
        WheelDefinition edited = new WheelDefinition(wheel.id(), "Renamed wheel", "", 3, entries, wheel.active(), wheel.radialOrder());
        assertEquals(de.artemis.omniwheel.common.Java8.list("new", "a", "c"), edited.entries().stream().map(WheelEntry::id).collect(de.artemis.omniwheel.common.Java8.toList()));
        assertEquals(de.artemis.omniwheel.common.Java8.list("c", "a", "new"), edited.radialOrder());
        assertEquals("Renamed", edited.radialEntries().get(1).label());
    }
}
