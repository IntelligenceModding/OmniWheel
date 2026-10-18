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

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private final ProfileJsonCodec codec = new ProfileJsonCodec();

    private WheelProfile legacy(String id) {
        de.artemis.omniwheel.common.wheel.WheelDefinition root = new WheelDefinition("main", "Old wheel", "", 8, de.artemis.omniwheel.common.Java8.list(new WheelEntry("home", "Home", "", "H", new CommandAction("/home", false))));
        return new WheelProfile(id, "My " + id, root.id(), de.artemis.omniwheel.common.Java8.map(root.id(), root));
    }

    private Path writeLegacy(ProfileCollection collection) throws Exception {
        Path path = temporary.getRoot().toPath().resolve("profiles.json");
        com.google.gson.JsonObject json = new JsonParser().parse(codec.encode(collection)).getAsJsonObject();
        json.remove("formatVersion");
        // Older releases did not store independent wheel order or display/preset flags.
        for (com.google.gson.JsonElement profile : json.getAsJsonArray("profiles")) {
            for (com.google.gson.JsonElement wheel : profile.getAsJsonObject().getAsJsonArray("wheels")) {
                wheel.getAsJsonObject().remove("radialOrder");
                for (com.google.gson.JsonElement entry : wheel.getAsJsonObject().getAsJsonArray("entries")) {
                    com.google.gson.JsonObject object = entry.getAsJsonObject();
                    object.add("glyph", object.remove("icon"));
                    for (String flag : de.artemis.omniwheel.common.Java8.list("showIcon", "showLabel", "showShortcut", "shortcut")) object.remove(flag);
                    object.getAsJsonObject("action").remove("preset");
                }
            }
        }
        de.artemis.omniwheel.common.Java8.writeString(path, json.toString());
        return path;
    }

    @Test
    public void upgradesSavedLegacyDefaultAndPreservesCustomProfileAndBackup() throws Exception {
        Path path = writeLegacy(new ProfileCollection("custom", de.artemis.omniwheel.common.Java8.list(legacy("custom"), legacy("default"))));
        String original = de.artemis.omniwheel.common.Java8.readString(path);
        de.artemis.omniwheel.common.profile.WheelProfile oldCustom = codec.decode(original).activeProfile();
        de.artemis.omniwheel.client.profile.store.OmniWheelProfileRepository repository = new OmniWheelProfileRepository(path);
        de.artemis.omniwheel.client.profile.store.ProfileCollection updated = repository.loadOrCreate();
        assertEquals("custom", updated.activeProfileId());
        assertEquals(oldCustom, updated.profiles().get(0));
        assertEquals(2, updated.profiles().size());
        assertEquals(DefaultProfiles.createDefaultProfile(), updated.profiles().get(1));
        assertEquals(original, de.artemis.omniwheel.common.Java8.readString(path.resolveSibling("profiles.before-upgrade.json")));
        assertEquals(updated, codec.decode(de.artemis.omniwheel.common.Java8.readString(path)));
        assertEquals(updated, repository.loadOrCreate());
        assertEquals(original, de.artemis.omniwheel.common.Java8.readString(path.resolveSibling("profiles.before-upgrade.json")));
    }

    @Test
    public void activeDefaultStaysActiveAndLaterEditsSurviveReload() throws Exception {
        Path path = writeLegacy(new ProfileCollection("default", de.artemis.omniwheel.common.Java8.list(legacy("default"))));
        de.artemis.omniwheel.client.profile.store.OmniWheelProfileRepository repository = new OmniWheelProfileRepository(path);
        de.artemis.omniwheel.client.profile.store.ProfileCollection updated = repository.loadOrCreate();
        assertEquals("default", updated.activeProfileId());
        de.artemis.omniwheel.common.profile.WheelProfile profile = updated.activeProfile();
        de.artemis.omniwheel.common.wheel.WheelDefinition root = new WheelDefinition(profile.rootWheelId(), "Edited", "", 8, de.artemis.omniwheel.common.Java8.list());
        de.artemis.omniwheel.common.profile.WheelProfile edited = new WheelProfile(profile.id(), "Renamed", root.id(), de.artemis.omniwheel.common.Java8.map(root.id(), root));
        de.artemis.omniwheel.client.profile.store.ProfileCollection saved = new ProfileCollection(edited.id(), de.artemis.omniwheel.common.Java8.list(edited));
        repository.save(saved);
        assertEquals(saved, repository.loadOrCreate());
    }

    @Test
    public void doesNotRecreateAnIntentionallyDeletedDefault() throws Exception {
        Path path = writeLegacy(new ProfileCollection("custom", de.artemis.omniwheel.common.Java8.list(legacy("custom"))));
        de.artemis.omniwheel.client.profile.store.ProfileCollection expected = codec.decode(de.artemis.omniwheel.common.Java8.readString(path));
        assertEquals(expected, new OmniWheelProfileRepository(path).loadOrCreate());
        assertTrue(Files.exists(path.resolveSibling("profiles.before-upgrade.json")));
    }

    @Test
    public void failedSaveLeavesDestinationIntactAndCleansTemporaryFiles() throws Exception {
        Path path = Files.createDirectory(temporary.getRoot().toPath().resolve("profiles.json"));
        Path existing = path.resolve("existing-data");
        de.artemis.omniwheel.common.Java8.writeString(existing, "preserve");
        de.artemis.omniwheel.client.profile.store.OmniWheelProfileRepository repository = new OmniWheelProfileRepository(path);
        assertThrows(IllegalStateException.class, () -> repository.save(new ProfileCollection("default", de.artemis.omniwheel.common.Java8.list(DefaultProfiles.createDefaultProfile()))));
        assertEquals("preserve", de.artemis.omniwheel.common.Java8.readString(existing));
        try (java.util.stream.Stream<java.nio.file.Path> children = Files.list(path.getParent())) {
            assertEquals(de.artemis.omniwheel.common.Java8.list(path), children.collect(de.artemis.omniwheel.common.Java8.toList()));
        }
    }

    @Test
    public void failedCorruptFileBackupDoesNotOverwriteOriginalProfiles() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("profiles.json");
        String damaged = "{ damaged profiles";
        de.artemis.omniwheel.common.Java8.writeString(path, damaged);
        Path blockedBackup = Files.createDirectory(path.resolveSibling("profiles.broken.json"));
        de.artemis.omniwheel.common.Java8.writeString(blockedBackup.resolve("existing-data"), "preserve");
        assertThrows(IllegalStateException.class, () -> new OmniWheelProfileRepository(path).loadOrCreate());
        assertEquals(damaged, de.artemis.omniwheel.common.Java8.readString(path));
        assertEquals("preserve", de.artemis.omniwheel.common.Java8.readString(blockedBackup.resolve("existing-data")));
    }

    @Test
    public void actual110SerializerFixturePreservesEveryCustomFieldAndReplacesOnlyDefault() throws Exception {
        // Generated with the unmodified 1.1.0 codec and default factory from Git revision 86b2eb0.
        String original;
        try (java.io.InputStream stream = getClass().getResourceAsStream("/de/artemis/omniwheel/profiles/profiles-v1.1.0.json")) {
            assertNotNull(stream);
            original = new String(de.artemis.omniwheel.common.Java8.readAllBytes(stream), java.nio.charset.StandardCharsets.UTF_8);
        }
        Path path = temporary.getRoot().toPath().resolve("profiles.json");
        de.artemis.omniwheel.common.Java8.writeString(path, original);
        de.artemis.omniwheel.common.profile.WheelProfile custom = codec.decode(original).activeProfile();
        de.artemis.omniwheel.client.profile.store.OmniWheelProfileRepository repository = new OmniWheelProfileRepository(path);
        de.artemis.omniwheel.client.profile.store.ProfileCollection migrated = repository.loadOrCreate();
        assertEquals(2, migrated.profiles().size());
        assertEquals("custom", migrated.activeProfileId());
        assertEquals(DefaultProfiles.createDefaultProfile(), migrated.profiles().get(0));
        assertEquals(custom, migrated.activeProfile());
        assertEquals(18, migrated.activeProfile().wheel("root").entries().size());
        assertEquals(new de.artemis.omniwheel.common.action.FunctionAction(de.artemis.omniwheel.common.action.GameplayFunction.OMNI_BACK), custom.wheel("root").entries().get(2).action());
        assertEquals(original, de.artemis.omniwheel.common.Java8.readString(path.resolveSibling("profiles.before-upgrade.json")));
        String saved = de.artemis.omniwheel.common.Java8.readString(path);
        assertEquals(migrated, repository.loadOrCreate());
        assertEquals(saved, de.artemis.omniwheel.common.Java8.readString(path));
    }

    @Test
    public void previousBackupsArePreservedAndCurrentOriginalGetsItsOwnBackup() throws Exception {
        Path path = writeLegacy(new ProfileCollection("default", de.artemis.omniwheel.common.Java8.list(legacy("default"))));
        String original = de.artemis.omniwheel.common.Java8.readString(path);
        Path backup = path.resolveSibling("profiles.before-upgrade.json");
        de.artemis.omniwheel.common.Java8.writeString(backup, "earlier backup");
        new OmniWheelProfileRepository(path).loadOrCreate();
        assertEquals("earlier backup", de.artemis.omniwheel.common.Java8.readString(backup));
        assertEquals(original, de.artemis.omniwheel.common.Java8.readString(path.resolveSibling("profiles.before-upgrade-2.json")));
    }

    @Test
    public void unreadableUnsupportedAndDuplicateDataNeverGetsReplacedWithDefaults() throws Exception {
        com.google.gson.JsonObject valid = new JsonParser().parse(codec.encode(new ProfileCollection("custom", de.artemis.omniwheel.common.Java8.list(legacy("custom"))))).getAsJsonObject();
        com.google.gson.JsonObject future = new com.google.gson.JsonParser().parse(valid.toString()).getAsJsonObject();
        future.addProperty("formatVersion", ProfileJsonCodec.FORMAT_VERSION + 1);
        com.google.gson.JsonObject unknown = new com.google.gson.JsonParser().parse(valid.toString()).getAsJsonObject();
        com.google.gson.JsonObject action = unknown.getAsJsonArray("profiles").get(0).getAsJsonObject().getAsJsonArray("wheels").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("action");
        action.addProperty("type", "future_action");
        com.google.gson.JsonObject unknownFunction = new com.google.gson.JsonParser().parse(unknown.toString()).getAsJsonObject();
        com.google.gson.JsonObject function = unknownFunction.getAsJsonArray("profiles").get(0).getAsJsonObject().getAsJsonArray("wheels").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("action");
        function.addProperty("type", "function");
        function.addProperty("value", "future_function");
        com.google.gson.JsonObject duplicate = new com.google.gson.JsonParser().parse(valid.toString()).getAsJsonObject();
        duplicate.getAsJsonArray("profiles").add(new com.google.gson.JsonParser().parse(duplicate.getAsJsonArray("profiles").get(0).toString()));
        com.google.gson.JsonObject duplicateWheel = new com.google.gson.JsonParser().parse(valid.toString()).getAsJsonObject();
        com.google.gson.JsonArray wheels = duplicateWheel.getAsJsonArray("profiles").get(0).getAsJsonObject().getAsJsonArray("wheels");
        wheels.add(new com.google.gson.JsonParser().parse(wheels.get(0).toString()));
        com.google.gson.JsonObject duplicateEntry = new com.google.gson.JsonParser().parse(valid.toString()).getAsJsonObject();
        com.google.gson.JsonArray entries = duplicateEntry.getAsJsonArray("profiles").get(0).getAsJsonObject().getAsJsonArray("wheels").get(0).getAsJsonObject().getAsJsonArray("entries");
        entries.add(new com.google.gson.JsonParser().parse(entries.get(0).toString()));
        for (String input : de.artemis.omniwheel.common.Java8.list("{ damaged", future.toString(), unknown.toString(), unknownFunction.toString(), duplicate.toString(), duplicateWheel.toString(), duplicateEntry.toString())) {
            Path path = temporary.newFolder().toPath().resolve("profiles.json");
            de.artemis.omniwheel.common.Java8.writeString(path, input);
            assertThrows(IllegalStateException.class, () -> new OmniWheelProfileRepository(path).loadOrCreate());
            assertEquals(input, de.artemis.omniwheel.common.Java8.readString(path));
            assertEquals(input, de.artemis.omniwheel.common.Java8.readString(path.resolveSibling("profiles.broken.json")));
        }
    }

    @Test
    public void missingActiveProfileFallsBackWithoutDiscardingCustomData() {
        de.artemis.omniwheel.client.profile.store.ProfileCollection original = new ProfileCollection("custom", de.artemis.omniwheel.common.Java8.list(legacy("custom")));
        com.google.gson.JsonObject json = new JsonParser().parse(codec.encode(original)).getAsJsonObject();
        json.addProperty("activeProfileId", "deleted");
        assertEquals(original, codec.decode(json.toString()));
    }

    @Test
    public void migratedProfilesCanSaveNewFeaturesWithoutChangingExistingEntries() throws Exception {
        Path path = writeLegacy(new ProfileCollection("custom", de.artemis.omniwheel.common.Java8.list(legacy("custom"))));
        de.artemis.omniwheel.client.profile.store.OmniWheelProfileRepository repository = new OmniWheelProfileRepository(path);
        de.artemis.omniwheel.common.profile.WheelProfile migrated = repository.loadOrCreate().activeProfile();
        java.util.ArrayList<de.artemis.omniwheel.common.wheel.WheelEntry> entries = new java.util.ArrayList<>(migrated.wheel("main").entries());
        entries.add(new WheelEntry("keybind", "Backpack", "", "", 0xFF125693, 0xCC341256, new de.artemis.omniwheel.common.action.KeybindAction("mod.backpack.open"), true, false, true, false, "key.mouse.4"));
        entries.add(new WheelEntry("preset", "Preset", "", "custom:image:unchanged-data", new CommandAction(de.artemis.omniwheel.common.Java8.list("/tell Friend "), true, true)));
        entries.add(new WheelEntry("chat", "Chat preset", "", "custom:item:unchanged-components", new de.artemis.omniwheel.common.action.ChatAction("Hello {player}", false, true)));
        de.artemis.omniwheel.common.wheel.WheelDefinition wheel = new WheelDefinition("main", "My wheel", "", 8, entries, true, de.artemis.omniwheel.common.Java8.list("chat", "keybind", "home", "preset"));
        de.artemis.omniwheel.common.profile.WheelProfile profile = new WheelProfile(migrated.id(), migrated.displayName(), "main", de.artemis.omniwheel.common.Java8.map("main", wheel));
        de.artemis.omniwheel.client.profile.store.ProfileCollection saved = new ProfileCollection(profile.id(), de.artemis.omniwheel.common.Java8.list(profile));
        repository.save(saved);
        assertEquals(saved, repository.loadOrCreate());
        assertEquals(migrated.wheel("main").entries().get(0), repository.loadOrCreate().activeProfile().wheel("main").entries().get(0));
    }

    @Test
    public void failedUpgradeBackupLeavesOriginalUntouched() throws Exception {
        Path path = writeLegacy(new ProfileCollection("default", de.artemis.omniwheel.common.Java8.list(legacy("default"))));
        String original = de.artemis.omniwheel.common.Java8.readString(path);
        Files.createDirectory(path.resolveSibling("profiles.before-upgrade.json"));
        assertThrows(IllegalStateException.class, () -> new OmniWheelProfileRepository(path).loadOrCreate());
        assertEquals(original, de.artemis.omniwheel.common.Java8.readString(path));
    }
}
