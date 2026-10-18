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
            for (var entry : wheel.entries()) {
                assertTrue(entry.glyph().matches("minecraft:[a-z0-9_]+"));
                assertTrue(entry.active());
                assertTrue(entry.shortcut().isBlank());
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
        for (var wheel : profile.wheels().values()) for (var entry : wheel.entries()) if (java.util.Objects.requireNonNull(entry.action()) instanceof FunctionAction function) { assertTrue(allowedFunctions.contains(function.function())); } else if (entry.action() instanceof KeybindAction keybind) { assertTrue(allowedBindings.contains(keybind.keybindId())); } else if (entry.action() instanceof CommandAction command) { assertEquals(List.of("/help"), command.commands()); assertTrue(command.preset()); } else if (entry.action() instanceof OpenChatAction chat) { assertEquals("/tell ", chat.initialText()); } else if (entry.action() instanceof ChatAction chat) { assertTrue("Chat examples must be editable before sending", chat.preset()); } else if (entry.action() instanceof CopyTextAction ignored) {} else if (entry.action() instanceof LocalMessageAction ignored) {} else if (entry.action() instanceof OpenWheelAction ignored) {} else { fail("Unexpected default action: " + entry.action()); }
    }

    @Test public void allDefaultNamesAndTemplatesExistAndUseSupportedLiveData() throws Exception {
        JsonObject language = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/omniwheel/lang/en_us.json"))).getAsJsonObject();
        var profile = DefaultProfiles.createDefaultProfile();
        Set<String> tokens = new HashSet<>(); for (var value : LiveDataValues.ALL) tokens.add(value.token());
        assertTrue(language.has(profile.displayName()));
        for (var wheel : profile.wheels().values()) {
            assertTrue(language.has(wheel.title())); assertTrue(language.has(wheel.description()));
            for (var entry : wheel.entries()) {
                assertTrue(language.has(entry.label())); assertTrue(language.has(entry.description()));
                String template = (java.util.Objects.requireNonNull(entry.action()) instanceof ChatAction chat ? (chat.message()) :
                        entry.action() instanceof LocalMessageAction message ? (message.message()) :
                        entry.action() instanceof CopyTextAction copy ? (copy.text()) :
                        (""));
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
        assertEquals("custom", result.activeProfileId()); assertSame(custom, result.profiles().get(0));
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
}
