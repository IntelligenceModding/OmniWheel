package de.artemis.omniwheel.client.runtime;

import de.artemis.omniwheel.client.profile.store.ProfileCollection;
import de.artemis.omniwheel.client.profile.store.ProfileJsonCodec;
import de.artemis.omniwheel.common.action.LocalMessageAction;
import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public final class WheelLayoutConsistencyTest {

    private WheelEntry entry(String id, boolean active) {
        return new WheelEntry(id, id, "", "X", WheelEntry.DEFAULT_COLOR, new LocalMessageAction(id), active);
    }

    private WheelProfile profile() {
        List<WheelEntry> entries = new ArrayList<>();
        entries.add(entry("inactive", false));
        entries.add(new WheelEntry("missing", "Missing", "", "X", new OpenWheelAction("missing-wheel")));
        entries.add(new WheelEntry("empty", "Empty", "", "X", new OpenWheelAction("child")));
        for (int i = 3; i < 10; i++) entries.add(entry("e" + i, true));
        return new WheelProfile("test", "Test", "root", de.artemis.omniwheel.common.Java8.map("root", new WheelDefinition("root", "Root", "", 8, entries), "child", new WheelDefinition("child", "Child", "", 1, de.artemis.omniwheel.common.Java8.list())));
    }

    @Test
    public void unavailableEntriesDoNotChangePositionsOrPageBoundaries() {
        de.artemis.omniwheel.client.runtime.WheelAvailabilityResolver resolver = new WheelAvailabilityResolver();
        WheelProfile profile = profile();
        List<WheelEntry> entries = resolver.layoutEntries(profile, "root");
        assertEquals(profile.wheel("root").entries(), entries);
        assertEquals(2, WheelLayout.pageCount(entries.size()));
        assertEquals(de.artemis.omniwheel.common.Java8.list("e8", "e9"), WheelLayout.page(entries, 1).stream().map(WheelEntry::id).collect(de.artemis.omniwheel.common.Java8.toList()));
        boolean[] enabled = resolver.enabledEntries(profile, "root", entries, new ArrayDeque<>(de.artemis.omniwheel.common.Java8.list("root")));
        assertFalse(enabled[0]);
        assertFalse(enabled[1]);
        assertTrue("empty submenu remains usable in its configured slot", enabled[2]);
        assertTrue(enabled[9]);
    }

    @Test
    public void inactiveWheelKeepsItsLayoutButCannotExecuteEntries() {
        WheelProfile original = profile();
        java.util.LinkedHashMap<java.lang.String, de.artemis.omniwheel.common.wheel.WheelDefinition> wheels = new LinkedHashMap<>(original.wheels());
        WheelDefinition root = wheels.get("root");
        wheels.put("root", new WheelDefinition(root.id(), root.title(), root.description(), 8, root.entries(), false));
        WheelProfile profile = new WheelProfile("test", "Test", "root", wheels);
        de.artemis.omniwheel.client.runtime.WheelAvailabilityResolver resolver = new WheelAvailabilityResolver();
        assertEquals(root.entries(), resolver.layoutEntries(profile, "root"));
        for (boolean enabled : resolver.enabledEntries(profile, "root", root.entries(), new ArrayDeque<>(de.artemis.omniwheel.common.Java8.list("root")))) assertFalse(enabled);
    }

    @Test
    public void previewMovesAndAppearanceSurviveSavingAndUseSameLivePages() {
        WheelProfile profile = profile();
        profile = WheelEntryMover.moveRadial(profile, "root", "e9", "root", 0).profile();
        profile = WheelEntryMover.moveRadial(profile, "root", "e8", "child", 0).profile();
        ProfileJsonCodec codec = new ProfileJsonCodec();
        WheelProfile reloaded = codec.decode(codec.encode(new ProfileCollection("test", de.artemis.omniwheel.common.Java8.list(profile)))).profiles().get(0);
        assertEquals(profile, reloaded);
        de.artemis.omniwheel.client.runtime.WheelAvailabilityResolver resolver = new WheelAvailabilityResolver();
        for (String wheel : de.artemis.omniwheel.common.Java8.list("root", "child")) {
            List<WheelEntry> preview = profile.wheel(wheel).radialEntries();
            List<WheelEntry> live = resolver.layoutEntries(reloaded, wheel);
            for (int page = 0; page < WheelLayout.pageCount(preview.size()); page++) assertEquals(WheelLayout.page(preview, page), WheelLayout.page(live, page));
        }
    }
}
