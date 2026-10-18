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

    @Test
    public void everyMenuIsReachableAndFitsOnePageWithRealVanillaIcons() {
        de.artemis.omniwheel.common.profile.WheelProfile profile = DefaultProfiles.createDefaultProfile();
        java.util.HashSet<java.lang.String> reached = new HashSet<String>();
        visit(profile, profile.rootWheelId(), reached);
        assertEquals(profile.wheels().keySet(), reached);
        assertEquals(6, reached.size());
        for (de.artemis.omniwheel.common.wheel.WheelDefinition wheel : profile.wheels().values()) {
            assertTrue(wheel.entries().size() > 0 && wheel.entries().size() <= WheelLayout.PAGE_SIZE);
            for (de.artemis.omniwheel.common.wheel.WheelEntry entry : wheel.entries()) {
                assertTrue(entry.glyph().matches("minecraft:[a-z0-9_]+"));
                assertTrue(entry.active());
                assertTrue(de.artemis.omniwheel.common.Java8.isBlank(entry.shortcut()));
            }
        }
    }

    private static void visit(WheelProfile profile, String id, Set<String> reached) {
        assertTrue("No circular submenus", reached.add(id));
        for (de.artemis.omniwheel.common.wheel.WheelEntry entry : profile.wheel(id).entries()) if (entry.action() instanceof OpenWheelAction)
            visit(profile, ((OpenWheelAction) (entry.action())).wheelId(), reached);
    }

    @Test
    public void containsOnlyOrdinaryVanillaActionsAndEditableChatPresets() {
        de.artemis.omniwheel.common.profile.WheelProfile profile = DefaultProfiles.createDefaultProfile();
        java.util.Set<de.artemis.omniwheel.common.action.GameplayFunction> allowedFunctions = de.artemis.omniwheel.common.Java8.set(GameplayFunction.OPEN_INVENTORY, GameplayFunction.OPEN_ADVANCEMENTS, GameplayFunction.TAKE_SCREENSHOT, GameplayFunction.OPEN_CHAT, GameplayFunction.SWAP_OFFHAND, GameplayFunction.PICK_BLOCK, GameplayFunction.HOTBAR_PREVIOUS, GameplayFunction.HOTBAR_NEXT);
        java.util.Set<java.lang.String> allowedBindings = de.artemis.omniwheel.common.Java8.set("key.togglePerspective", "key.toggleGui", "key.smoothCamera", "key.fullscreen", "key.debug.overlay", "key.debug.showHitboxes", "key.debug.showChunkBorders", "key.debug.showAdvancedTooltips");
        for (de.artemis.omniwheel.common.wheel.WheelDefinition wheel : profile.wheels().values()) for (de.artemis.omniwheel.common.wheel.WheelEntry entry : wheel.entries()) if (java.util.Objects.requireNonNull(entry.action()) instanceof FunctionAction) {
            assertTrue(allowedFunctions.contains(((FunctionAction) (java.util.Objects.requireNonNull(entry.action()))).function()));
        } else if (entry.action() instanceof KeybindAction) {
            assertTrue(allowedBindings.contains(((KeybindAction) (entry.action())).keybindId()));
        } else if (entry.action() instanceof CommandAction) {
            assertEquals(de.artemis.omniwheel.common.Java8.list("/help"), ((CommandAction) (entry.action())).commands());
            assertTrue(((CommandAction) (entry.action())).preset());
        } else if (entry.action() instanceof OpenChatAction) {
            assertEquals("/tell ", ((OpenChatAction) (entry.action())).initialText());
        } else if (entry.action() instanceof ChatAction) {
            assertTrue("Chat examples must be editable before sending", ((ChatAction) (entry.action())).preset());
        } else if (entry.action() instanceof CopyTextAction) {
        } else if (entry.action() instanceof LocalMessageAction) {
        } else if (entry.action() instanceof OpenWheelAction) {
        } else {
            fail("Unexpected default action: " + entry.action());
        }
    }

    @Test
    public void allDefaultNamesAndTemplatesExistAndUseSupportedLiveData() throws Exception {
        JsonObject language = new JsonParser().parse(de.artemis.omniwheel.common.Java8.readString(java.nio.file.Paths.get("src/main/resources/assets/omniwheel/lang/en_us.json"))).getAsJsonObject();
        de.artemis.omniwheel.common.profile.WheelProfile profile = DefaultProfiles.createDefaultProfile();
        Set<String> tokens = new HashSet<>();
        for (de.artemis.omniwheel.client.runtime.LiveDataValues.Value value : LiveDataValues.ALL) tokens.add(value.token());
        assertTrue(language.has(profile.displayName()));
        for (de.artemis.omniwheel.common.wheel.WheelDefinition wheel : profile.wheels().values()) {
            assertTrue(language.has(wheel.title()));
            assertTrue(language.has(wheel.description()));
            for (de.artemis.omniwheel.common.wheel.WheelEntry entry : wheel.entries()) {
                assertTrue(language.has(entry.label()));
                assertTrue(language.has(entry.description()));
                String template = (java.util.Objects.requireNonNull(entry.action()) instanceof ChatAction ? (((ChatAction) (java.util.Objects.requireNonNull(entry.action()))).message()) : entry.action() instanceof LocalMessageAction ? (((LocalMessageAction) (entry.action())).message()) : entry.action() instanceof CopyTextAction ? (((CopyTextAction) (entry.action())).text()) : (""));
                if (template.startsWith("omniwheel.")) {
                    assertTrue(language.has(template));
                    template = language.get(template).getAsString();
                }
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{[^}]+}").matcher(template);
                while (matcher.find()) assertTrue("Known token " + matcher.group(), tokens.contains(matcher.group()));
            }
        }
    }

    @Test
    public void replacesOldDefaultCompletelyOnceAndPreservesOtherProfiles() {
        de.artemis.omniwheel.common.wheel.WheelDefinition oldRoot = new WheelDefinition("main", "Old", "", 8, de.artemis.omniwheel.common.Java8.list(new WheelEntry("spawn", "Spawn", "", "S", new CommandAction("/spawn", false)), new WheelEntry("home", "Home", "", "H", new CommandAction("/home", false))));
        de.artemis.omniwheel.common.profile.WheelProfile oldDefault = new WheelProfile("default", "Default", "main", de.artemis.omniwheel.common.Java8.map("main", oldRoot));
        de.artemis.omniwheel.common.profile.WheelProfile custom = new WheelProfile("custom", "My Profile", "main", de.artemis.omniwheel.common.Java8.map("main", oldRoot));
        de.artemis.omniwheel.client.profile.store.ProfileCollection result = ProfileUpgrader.upgrade(new ProfileCollection("custom", de.artemis.omniwheel.common.Java8.list(custom, oldDefault)));
        assertEquals("custom", result.activeProfileId());
        assertSame(custom, result.profiles().get(0));
        assertEquals(DefaultProfiles.createDefaultProfile(), result.profiles().get(1));
        assertFalse(result.profiles().get(1).wheels().containsKey("main"));
        assertSame(result, ProfileUpgrader.upgrade(result));
    }

    @Test
    public void editingNewDefaultDoesNotRestoreDeletedEntriesOnReload() {
        de.artemis.omniwheel.common.profile.WheelProfile profile = DefaultProfiles.createDefaultProfile();
        de.artemis.omniwheel.common.wheel.WheelDefinition root = profile.wheel(profile.rootWheelId());
        java.util.HashMap<java.lang.String, de.artemis.omniwheel.common.wheel.WheelDefinition> wheels = new HashMap<>(profile.wheels());
        wheels.put(root.id(), new WheelDefinition(root.id(), "My layout", "", 8, de.artemis.omniwheel.common.Java8.list()));
        de.artemis.omniwheel.common.profile.WheelProfile edited = new WheelProfile(profile.id(), "My Default", root.id(), wheels);
        de.artemis.omniwheel.client.profile.store.ProfileCollection collection = new ProfileCollection(edited.id(), de.artemis.omniwheel.common.Java8.list(edited));
        assertSame(collection, ProfileUpgrader.upgrade(collection));
    }
}
