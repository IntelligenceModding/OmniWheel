package de.artemis.omniwheel.client.profile.store;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.artemis.omniwheel.client.profile.DefaultProfiles;
import de.artemis.omniwheel.client.runtime.LiveDataValues;
import de.artemis.omniwheel.common.action.*;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.*;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.Assert.*;

public class VanillaDefaultProfileTest {
    @Test public void everyMenuIsReachableAndFitsOnePageWithRealVanillaIcons() {
        var profile = DefaultProfiles.createDefaultProfile();
        var reached = new HashSet<String>(); visit(profile, profile.rootWheelId(), reached);
        assertEquals(profile.wheels().keySet(), reached);
        assertEquals(6, reached.size());
        for (var wheel : profile.wheels().values()) {
            assertTrue(wheel.entries().size() > 0 && wheel.entries().size() <= WheelLayout.PAGE_SIZE);
            assertEquals("No wheel-only metadata unavailable in the editor", "", wheel.description());
            assertEquals(Math.clamp(wheel.entries().size(), 1, 8), wheel.segmentCount());
            assertTrue(wheel.active());
            assertEquals(wheel.entries().stream().map(WheelEntry::id).toList(), wheel.radialOrder());
            for (var entry : wheel.entries()) {
                assertTrue(entry.glyph().matches("minecraft:[a-z0-9_]+"));
                assertTrue(entry.active());
                assertTrue(entry.shortcut().isBlank());
                assertTrue(entry.showIcon());
                assertTrue(entry.showLabel());
                assertTrue(entry.showShortcut());
                assertEquals(WheelEntry.DEFAULT_COLOR, entry.color());
                assertEquals(WheelEntry.DEFAULT_GUI_COLOR, entry.guiColor());
            }
        }
    }
    private static void visit(WheelProfile profile, String id, Set<String> reached) {
        assertTrue("No circular submenus", reached.add(id));
        for (var entry : profile.wheel(id).entries()) if (entry.action() instanceof OpenWheelAction submenu) visit(profile, submenu.wheelId(), reached);
    }

    @Test public void containsOnlyOrdinaryVanillaActionsAndEditableChatPresets() {
        var profile = DefaultProfiles.createDefaultProfile();
        var allowedFunctions = Set.of(GameplayFunction.OPEN_INVENTORY, GameplayFunction.OPEN_ADVANCEMENTS,
                GameplayFunction.TAKE_SCREENSHOT, GameplayFunction.OPEN_CHAT, GameplayFunction.SWAP_OFFHAND,
                GameplayFunction.PICK_BLOCK, GameplayFunction.HOTBAR_PREVIOUS, GameplayFunction.HOTBAR_NEXT);
        var allowedBindings = Set.of("key.togglePerspective", "key.toggleGui", "key.smoothCamera", "key.fullscreen",
                "key.debug.overlay", "key.debug.showHitboxes", "key.debug.showChunkBorders", "key.debug.showAdvancedTooltips");
        for (var wheel : profile.wheels().values()) for (var entry : wheel.entries()) switch (entry.action()) {
            case FunctionAction function -> assertTrue(allowedFunctions.contains(function.function()));
            case KeybindAction keybind -> assertTrue(allowedBindings.contains(keybind.keybindId()));
            case CommandAction command -> {
                assertEquals(List.of(entry.id().equals("whisper") ? "/tell" : "/help"), command.commands());
                assertTrue(command.preset());
                assertFalse(command.requireConfirmation());
            }
            case ChatAction chat -> {
                assertTrue("Chat examples must be editable before sending", chat.preset());
                assertFalse(chat.requireConfirmation());
            }
            case CopyTextAction ignored -> {}
            case LocalMessageAction ignored -> {}
            case OpenWheelAction ignored -> {}
            default -> fail("Unexpected default action: " + entry.action());
        }
    }

    @Test public void allDefaultNamesAndTemplatesExistAndUseSupportedLiveData() throws Exception {
        JsonObject language = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/omniwheel/lang/en_us.json"))).getAsJsonObject();
        var profile = DefaultProfiles.createDefaultProfile();
        Set<String> tokens = new HashSet<>(); for (var value : LiveDataValues.ALL) tokens.add(value.token());
        assertTrue(language.has(profile.displayName()));
        for (var wheel : profile.wheels().values()) {
            assertTrue(language.has(wheel.title())); assertEquals("", wheel.description());
            for (var entry : wheel.entries()) {
                assertTrue(language.has(entry.label())); assertTrue(language.has(entry.description()));
                String template = switch (entry.action()) {
                    case ChatAction chat -> chat.message(); case LocalMessageAction message -> message.message();
                    case CopyTextAction copy -> copy.text(); default -> "";
                };
                if (template.startsWith("omniwheel.")) { assertTrue(language.has(template)); template = language.get(template).getAsString(); }
                var matcher = java.util.regex.Pattern.compile("\\{[^}]+}").matcher(template);
                while (matcher.find()) assertTrue("Known token " + matcher.group(), tokens.contains(matcher.group()));
            }
        }
    }

    @Test public void replacesOldDefaultCompletelyOnceAndPreservesOtherProfiles() {
        var oldRoot = new WheelDefinition("main", "Old", "", 8, List.of(
                new WheelEntry("spawn", "Spawn", "", "S", new CommandAction("/spawn", false)),
                new WheelEntry("home", "Home", "", "H", new CommandAction("/home", false))));
        var oldDefault = new WheelProfile("default", "Default", "main", Map.of("main", oldRoot));
        var custom = new WheelProfile("custom", "My Profile", "main", Map.of("main", oldRoot));
        var result = ProfileUpgrader.upgrade(new ProfileCollection("custom", List.of(custom, oldDefault)));
        assertEquals("custom", result.activeProfileId()); assertSame(custom, result.profiles().getFirst());
        assertEquals(DefaultProfiles.createDefaultProfile(), result.profiles().get(1));
        assertFalse(result.profiles().get(1).wheels().containsKey("main"));
        assertSame(result, ProfileUpgrader.upgrade(result));
    }

    @Test public void editingNewDefaultDoesNotRestoreDeletedEntriesOnReload() {
        var profile = DefaultProfiles.createDefaultProfile();
        var root = profile.wheel(profile.rootWheelId());
        var wheels = new HashMap<>(profile.wheels());
        wheels.put(root.id(), new WheelDefinition(root.id(), "My layout", "", 8, List.of()));
        var edited = new WheelProfile(profile.id(), "My Default", root.id(), wheels);
        var collection = new ProfileCollection(edited.id(), List.of(edited));
        assertSame(collection, ProfileUpgrader.upgrade(collection));
    }

    @Test public void upgradesPreviousStockSettingsWithoutReplacingCustomizations() {
        var current = DefaultProfiles.createDefaultProfile();
        var oldWheels = new LinkedHashMap<String, WheelDefinition>();
        for (var wheel : current.wheels().values()) {
            var entries = new ArrayList<WheelEntry>();
            for (var entry : wheel.entries()) {
                if (entry.id().equals("whisper")) {
                    entry = new WheelEntry(entry.id(), "My whisper", "My description", "minecraft:paper",
                            0xFF123456, 0xD8654321, new OpenChatAction("/tell "), false, false, true, false, "Ctrl+G");
                }
                entries.add(entry);
            }
            String title = wheel.id().equals(current.rootWheelId()) ? "main" : wheel.id();
            oldWheels.put(wheel.id(), new WheelDefinition(wheel.id(), wheel.title(),
                    "omniwheel.default.wheel." + title + ".description", 8, entries, wheel.active(), wheel.radialOrder()));
        }
        var previous = new WheelProfile(current.id(), "My renamed default", current.rootWheelId(), oldWheels);
        previous = WheelEntryMover.move(previous, current.rootWheelId(), "inventory", current.rootWheelId(), 5).profile();
        previous = WheelEntryMover.moveRadial(previous, current.rootWheelId(), "view", current.rootWheelId(), 0).profile();
        var custom = new WheelProfile("custom", "Keep me", previous.rootWheelId(), previous.wheels());
        var result = ProfileUpgrader.upgrade(new ProfileCollection("default", List.of(previous, custom)));
        var updated = result.activeProfile();

        assertSame(custom, result.profiles().get(1));
        assertEquals(previous.displayName(), updated.displayName());
        for (var wheel : previous.wheels().values()) {
            var upgraded = updated.wheel(wheel.id());
            assertEquals("", upgraded.description());
            assertEquals(Math.clamp(wheel.entries().size(), 1, 8), upgraded.segmentCount());
            assertEquals(wheel.radialOrder(), upgraded.radialOrder());
            assertEquals(wheel.entries().stream().map(WheelEntry::id).toList(), upgraded.entries().stream().map(WheelEntry::id).toList());
            for (int i = 0; i < wheel.entries().size(); i++) {
                var before = wheel.entries().get(i);
                var after = upgraded.entries().get(i);
                if (before.id().equals("whisper")) {
                    assertEquals(new WheelEntry(before.id(), before.label(), before.description(), before.glyph(),
                            before.color(), before.guiColor(), new CommandAction(List.of("/tell"), false, true),
                            before.active(), before.showIcon(), before.showLabel(), before.showShortcut(), before.shortcut()), after);
                } else assertEquals(before, after);
            }
        }
        assertSame(result, ProfileUpgrader.upgrade(result));
    }

    @Test public void preservesCustomWheelDescriptionsAndWhisperDrafts() {
        var entry = new WheelEntry("whisper", "Custom draft", "", "minecraft:paper", new OpenChatAction("/msg Alex "));
        var wheel = new WheelDefinition(DefaultProfiles.ROOT_WHEEL_ID, "Mine", "Custom JSON description", 4, List.of(entry));
        var profile = new WheelProfile("default", "Mine", wheel.id(), Map.of(wheel.id(), wheel));
        var collection = new ProfileCollection(profile.id(), List.of(profile));
        assertSame(collection, ProfileUpgrader.upgrade(collection));
    }

    @Test public void upgradesWhisperPreviouslyTrimmedByTheEditor() {
        var entry = new WheelEntry("whisper", "Whisper", "", "minecraft:name_tag", new OpenChatAction("/tell"));
        var wheel = new WheelDefinition(DefaultProfiles.ROOT_WHEEL_ID, "Main", "", 1, List.of(entry));
        var profile = new WheelProfile("default", "Default", wheel.id(), Map.of(wheel.id(), wheel));
        var updated = ProfileUpgrader.upgrade(new ProfileCollection(profile.id(), List.of(profile)));
        assertEquals(new CommandAction(List.of("/tell"), false, true),
                updated.activeProfile().wheel(wheel.id()).entries().getFirst().action());
        assertSame(updated, ProfileUpgrader.upgrade(updated));
    }
}
