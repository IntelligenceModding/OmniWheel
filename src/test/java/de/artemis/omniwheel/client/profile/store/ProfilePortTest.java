package de.artemis.omniwheel.client.profile.store;

import de.artemis.omniwheel.client.profile.DefaultProfiles;
import de.artemis.omniwheel.common.action.*;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class ProfilePortTest {

    private final ProfileJsonCodec codec = new ProfileJsonCodec();

    @Test
    public void allStoredActionsAndEntrySettingsRoundTrip() {
        List<WheelAction> actions = Arrays.asList(new CommandAction(Arrays.asList("/spawn", "/say {player}"), true), new ChatAction("Hello {player}", true), new FunctionAction(GameplayFunction.TOGGLE_AUTO_WALK), new LocalMessageAction("Position: {x} {y} {z}"), new OpenChatAction("/msg "), new CopyTextAction("{dimension}"), new OpenWheelAction("submenu"), new OpenScreenAction(ScreenTarget.PROFILE_MANAGER));
        List<WheelEntry> entries = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            entries.add(new WheelEntry("entry_" + i, "Label " + i, "Description", "minecraft:stone", 0xFF123456, 0xDD654321, actions.get(i), i % 2 == 0, false, true, false, "Ctrl+G"));
        }
        Map<String, WheelDefinition> wheels = new LinkedHashMap<>();
        wheels.put("main", new WheelDefinition("main", "Main", "Description", 16, entries, true));
        wheels.put("submenu", new WheelDefinition("submenu", "Submenu", "", 1, Collections.emptyList(), false));
        ProfileCollection collection = new ProfileCollection("custom", Collections.singletonList(new WheelProfile("custom", "Custom \u00e4 \u65e5", "main", wheels)));
        ProfileCollection restored = codec.decode(codec.encode(collection));
        assertEquals(collection, restored);
        assertEquals(collection.hashCode(), restored.hashCode());
        assertEquals(Arrays.asList("main", "submenu"), new ArrayList<>(restored.activeProfile().wheels().keySet()));
        assertTrue(restored.activeProfile().wheel("main").entries().get(0).action().requiresConfirmation());
        assertEquals("/msg ", ((OpenChatAction) restored.activeProfile().wheel("main").entries().get(4).action()).initialText());
    }

    @Test
    public void legacyProfilesKeepDefaultsAndSingleCommands() {
        String legacy = "{\"profiles\":[{\"id\":\"old\",\"displayName\":\"Old\",\"rootWheelId\":\"main\"," + "\"wheels\":[{\"id\":\"main\",\"title\":\"Main\",\"segmentCount\":8,\"entries\":[" + "{\"id\":\"spawn\",\"label\":\"Spawn\",\"glyph\":\"S\"," + "\"action\":{\"type\":\"command\",\"value\":\"/spawn\"}}]}]}]}";
        ProfileCollection restored = codec.decode(legacy);
        WheelEntry entry = restored.activeProfile().wheel("main").entries().get(0);
        assertEquals("old", restored.activeProfileId());
        assertEquals("S", entry.glyph());
        assertTrue(entry.active());
        assertTrue(entry.showIcon());
        assertTrue(entry.showLabel());
        assertTrue(entry.showShortcut());
        assertEquals("", entry.shortcut());
        assertEquals(Collections.singletonList("/spawn"), ((CommandAction) entry.action()).commands());
        assertEquals(restored, codec.decode(codec.encode(restored)));
    }

    @Test
    public void defaultProfilesRemainSerializableAndUpgradeIsIdempotent() {
        ProfileCollection defaults = new ProfileCollection("default", Collections.singletonList(DefaultProfiles.createDefaultProfile()));
        assertEquals(defaults, codec.decode(codec.encode(defaults)));
        ProfileCollection upgraded = ProfileUpgrader.upgrade(defaults);
        assertEquals(upgraded, ProfileUpgrader.upgrade(upgraded));
        assertEquals(upgraded, codec.decode(codec.encode(upgraded)));
        for (WheelDefinition wheel : upgraded.activeProfile().wheels().values()) {
            for (WheelEntry entry : wheel.entries()) {
                if (entry.action() instanceof OpenWheelAction) {
                    assertTrue(upgraded.activeProfile().wheels().containsKey(((OpenWheelAction) entry.action()).wheelId()));
                }
            }
        }
    }

    @Test
    public void modelsDefensivelyCopyCollectionsAndRetainValueEquality() {
        List<String> commands = new ArrayList<>(Arrays.asList(" /spawn ", "", " /home "));
        CommandAction action = new CommandAction(commands, true);
        commands.clear();
        assertEquals(Arrays.asList("/spawn", "/home"), action.commands());
        assertEquals(new CommandAction(Arrays.asList("/spawn", "/home"), true), action);
        assertThrows(UnsupportedOperationException.class, () -> action.commands().add("/say changed"));
        List<WheelEntry> entries = new ArrayList<>();
        WheelDefinition wheel = new WheelDefinition(" main ", " Main ", "", 8, entries);
        entries.add(new WheelEntry("test", "Test", "", "T", action));
        assertTrue(wheel.entries().isEmpty());
        assertEquals("main", wheel.id());
        assertThrows(UnsupportedOperationException.class, () -> wheel.entries().clear());
        Map<String, WheelDefinition> wheels = new LinkedHashMap<>();
        wheels.put("main", wheel);
        WheelProfile profile = new WheelProfile("custom", "Custom", "main", wheels);
        wheels.clear();
        assertEquals(wheel, profile.wheel("main"));
        assertThrows(UnsupportedOperationException.class, () -> profile.wheels().clear());
        assertThrows(IllegalArgumentException.class, () -> new CommandAction(Collections.singletonList(" "), false));
        assertThrows(IllegalArgumentException.class, () -> new WheelDefinition("main", "Main", "", 17, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new WheelProfile("bad", "Bad", "missing", profile.wheels()));
    }

    @Test
    public void malformedProfilesAreRejected() {
        assertThrows(IllegalStateException.class, () -> codec.decode("null"));
        assertThrows(IllegalStateException.class, () -> codec.decode("{\"profiles\":[]}"));
    }
}
