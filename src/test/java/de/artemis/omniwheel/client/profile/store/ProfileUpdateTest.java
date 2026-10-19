package de.artemis.omniwheel.client.profile.store;

import com.google.gson.JsonParser;
import de.artemis.omniwheel.client.profile.DefaultProfiles;
import de.artemis.omniwheel.common.action.CommandAction;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ProfileUpdateTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final ProfileJsonCodec codec = new ProfileJsonCodec();

    private WheelProfile legacy(String id) {
        var root = new WheelDefinition("main", "Old wheel", "", 8, List.of(
                new WheelEntry("home", "Home", "", "H", new CommandAction("/home", false))));
        return new WheelProfile(id, "My " + id, root.id(), Map.of(root.id(), root));
    }

    private Path writeLegacy(ProfileCollection collection) throws Exception {
        Path path = temporary.getRoot().toPath().resolve("profiles.json");
        var json = JsonParser.parseString(codec.encode(collection)).getAsJsonObject();
        json.remove("formatVersion");
        // Older releases did not store independent wheel order or display/preset flags.
        for (var profile : json.getAsJsonArray("profiles")) {
            for (var wheel : profile.getAsJsonObject().getAsJsonArray("wheels")) {
                wheel.getAsJsonObject().remove("radialOrder");
                for (var entry : wheel.getAsJsonObject().getAsJsonArray("entries")) {
                    var object = entry.getAsJsonObject();
                    object.add("glyph", object.remove("icon"));
                    for (String flag : List.of("showIcon", "showLabel", "showShortcut", "shortcut")) object.remove(flag);
                    object.getAsJsonObject("action").remove("preset");
                }
            }
        }
        Files.writeString(path, json.toString());
        return path;
    }

    @Test public void upgradesSavedLegacyDefaultAndPreservesCustomProfileAndBackup() throws Exception {
        Path path = writeLegacy(new ProfileCollection("custom", List.of(legacy("custom"), legacy("default"))));
        String original = Files.readString(path);
        var oldCustom = codec.decode(original).activeProfile();
        var repository = new OmniWheelProfileRepository(path);
        var updated = repository.loadOrCreate();
        assertEquals("custom", updated.activeProfileId());
        assertEquals(oldCustom, updated.profiles().getFirst());
        assertEquals(2, updated.profiles().size());
        assertEquals(DefaultProfiles.createDefaultProfile(), updated.profiles().get(1));
        assertEquals(original, Files.readString(path.resolveSibling("profiles.before-upgrade.json")));
        assertEquals(updated, codec.decode(Files.readString(path)));
        assertEquals(updated, repository.loadOrCreate());
        assertEquals(original, Files.readString(path.resolveSibling("profiles.before-upgrade.json")));
    }

    @Test public void activeDefaultStaysActiveAndLaterEditsSurviveReload() throws Exception {
        Path path = writeLegacy(new ProfileCollection("default", List.of(legacy("default"))));
        var repository = new OmniWheelProfileRepository(path);
        var updated = repository.loadOrCreate();
        assertEquals("default", updated.activeProfileId());
        var profile = updated.activeProfile();
        var root = new WheelDefinition(profile.rootWheelId(), "Edited", "", 8, List.of());
        var edited = new WheelProfile(profile.id(), "Renamed", root.id(), Map.of(root.id(), root));
        var saved = new ProfileCollection(edited.id(), List.of(edited));
        repository.save(saved);
        assertEquals(saved, repository.loadOrCreate());
    }

    @Test public void doesNotRecreateAnIntentionallyDeletedDefault() throws Exception {
        Path path = writeLegacy(new ProfileCollection("custom", List.of(legacy("custom"))));
        var expected = codec.decode(Files.readString(path));
        assertEquals(expected, new OmniWheelProfileRepository(path).loadOrCreate());
        assertTrue(Files.exists(path.resolveSibling("profiles.before-upgrade.json")));
    }

    @Test public void failedSaveLeavesDestinationIntactAndCleansTemporaryFiles() throws Exception {
        Path path = Files.createDirectory(temporary.getRoot().toPath().resolve("profiles.json"));
        Path existing = path.resolve("existing-data");
        Files.writeString(existing, "preserve");
        var repository = new OmniWheelProfileRepository(path);
        assertThrows(IllegalStateException.class, () -> repository.save(
                new ProfileCollection("default", List.of(DefaultProfiles.createDefaultProfile()))));
        assertEquals("preserve", Files.readString(existing));
        try (var children = Files.list(path.getParent())) {
            assertEquals(List.of(path), children.toList());
        }
    }

    @Test public void failedCorruptFileBackupDoesNotOverwriteOriginalProfiles() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("profiles.json");
        String damaged = "{ damaged profiles";
        Files.writeString(path, damaged);
        Path blockedBackup = Files.createDirectory(path.resolveSibling("profiles.broken.json"));
        Files.writeString(blockedBackup.resolve("existing-data"), "preserve");
        assertThrows(IllegalStateException.class, () -> new OmniWheelProfileRepository(path).loadOrCreate());
        assertEquals(damaged, Files.readString(path));
        assertEquals("preserve", Files.readString(blockedBackup.resolve("existing-data")));
    }

    @Test public void actual110SerializerFixturePreservesEveryCustomFieldAndReplacesOnlyDefault() throws Exception {
        // Generated with the unmodified 1.1.0 codec and default factory from Git revision 86b2eb0.
        String original;
        try (var stream = getClass().getResourceAsStream("/de/artemis/omniwheel/profiles/profiles-v1.1.0.json")) {
            assertNotNull(stream);
            original = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        Path path = temporary.getRoot().toPath().resolve("profiles.json");
        Files.writeString(path, original);
        var custom = codec.decode(original).activeProfile();
        var repository = new OmniWheelProfileRepository(path);
        var migrated = repository.loadOrCreate();
        assertEquals(2, migrated.profiles().size());
        assertEquals("custom", migrated.activeProfileId());
        assertEquals(DefaultProfiles.createDefaultProfile(), migrated.profiles().getFirst());
        assertEquals(custom, migrated.activeProfile());
        assertEquals(18, migrated.activeProfile().wheel("root").entries().size());
        assertEquals(new de.artemis.omniwheel.common.action.FunctionAction(
                de.artemis.omniwheel.common.action.GameplayFunction.OMNI_BACK), custom.wheel("root").entries().get(2).action());
        assertEquals(original, Files.readString(path.resolveSibling("profiles.before-upgrade.json")));
        String saved = Files.readString(path);
        assertEquals(migrated, repository.loadOrCreate());
        assertEquals(saved, Files.readString(path));
    }

    @Test public void previousBackupsArePreservedAndCurrentOriginalGetsItsOwnBackup() throws Exception {
        Path path = writeLegacy(new ProfileCollection("default", List.of(legacy("default"))));
        String original = Files.readString(path);
        Path backup = path.resolveSibling("profiles.before-upgrade.json");
        Files.writeString(backup, "earlier backup");
        new OmniWheelProfileRepository(path).loadOrCreate();
        assertEquals("earlier backup", Files.readString(backup));
        assertEquals(original, Files.readString(path.resolveSibling("profiles.before-upgrade-2.json")));
    }

    @Test public void unreadableUnsupportedAndDuplicateDataNeverGetsReplacedWithDefaults() throws Exception {
        var valid = JsonParser.parseString(codec.encode(new ProfileCollection("custom", List.of(legacy("custom"))))).getAsJsonObject();
        var future = valid.deepCopy(); future.addProperty("formatVersion", ProfileJsonCodec.FORMAT_VERSION + 1);
        var unknown = valid.deepCopy();
        var action = unknown.getAsJsonArray("profiles").get(0).getAsJsonObject().getAsJsonArray("wheels")
                .get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("action");
        action.addProperty("type", "future_action");
        var unknownFunction = unknown.deepCopy();
        var function = unknownFunction.getAsJsonArray("profiles").get(0).getAsJsonObject().getAsJsonArray("wheels")
                .get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("action");
        function.addProperty("type", "function"); function.addProperty("value", "future_function");
        var duplicate = valid.deepCopy(); duplicate.getAsJsonArray("profiles").add(duplicate.getAsJsonArray("profiles").get(0).deepCopy());
        var duplicateWheel = valid.deepCopy();
        var wheels = duplicateWheel.getAsJsonArray("profiles").get(0).getAsJsonObject().getAsJsonArray("wheels");
        wheels.add(wheels.get(0).deepCopy());
        var duplicateEntry = valid.deepCopy();
        var entries = duplicateEntry.getAsJsonArray("profiles").get(0).getAsJsonObject().getAsJsonArray("wheels")
                .get(0).getAsJsonObject().getAsJsonArray("entries");
        entries.add(entries.get(0).deepCopy());
        for (String input : List.of("{ damaged", future.toString(), unknown.toString(), unknownFunction.toString(),
                duplicate.toString(), duplicateWheel.toString(), duplicateEntry.toString())) {
            Path path = temporary.newFolder().toPath().resolve("profiles.json");
            Files.writeString(path, input);
            assertThrows(IllegalStateException.class, () -> new OmniWheelProfileRepository(path).loadOrCreate());
            assertEquals(input, Files.readString(path));
            assertEquals(input, Files.readString(path.resolveSibling("profiles.broken.json")));
        }
    }

    @Test public void missingActiveProfileFallsBackWithoutDiscardingCustomData() {
        var original = new ProfileCollection("custom", List.of(legacy("custom")));
        var json = JsonParser.parseString(codec.encode(original)).getAsJsonObject();
        json.addProperty("activeProfileId", "deleted");
        assertEquals(original, codec.decode(json.toString()));
    }

    @Test public void migratedProfilesCanSaveNewFeaturesWithoutChangingExistingEntries() throws Exception {
        Path path = writeLegacy(new ProfileCollection("custom", List.of(legacy("custom"))));
        var repository = new OmniWheelProfileRepository(path);
        var migrated = repository.loadOrCreate().activeProfile();
        var entries = new java.util.ArrayList<>(migrated.wheel("main").entries());
        entries.add(new WheelEntry("keybind", "Backpack", "", "", 0xFF125693, 0xCC341256,
                new de.artemis.omniwheel.common.action.KeybindAction("mod.backpack.open"), true, false, true, false, "key.mouse.4"));
        entries.add(new WheelEntry("preset", "Preset", "", "custom:image:unchanged-data",
                new CommandAction(List.of("/tell Friend "), true, true)));
        entries.add(new WheelEntry("chat", "Chat preset", "", "custom:item:unchanged-components",
                new de.artemis.omniwheel.common.action.ChatAction("Hello {player}", false, true)));
        var wheel = new WheelDefinition("main", "My wheel", "", 8, entries, true,
                List.of("chat", "keybind", "home", "preset"));
        var profile = new WheelProfile(migrated.id(), migrated.displayName(), "main", Map.of("main", wheel));
        var saved = new ProfileCollection(profile.id(), List.of(profile));
        repository.save(saved);
        assertEquals(saved, repository.loadOrCreate());
        assertEquals(migrated.wheel("main").entries().getFirst(), repository.loadOrCreate().activeProfile().wheel("main").entries().getFirst());
    }

    @Test public void failedUpgradeBackupLeavesOriginalUntouched() throws Exception {
        Path path = writeLegacy(new ProfileCollection("default", List.of(legacy("default"))));
        String original = Files.readString(path);
        Files.createDirectory(path.resolveSibling("profiles.before-upgrade.json"));
        assertThrows(IllegalStateException.class, () -> new OmniWheelProfileRepository(path).loadOrCreate());
        assertEquals(original, Files.readString(path));
    }
}
