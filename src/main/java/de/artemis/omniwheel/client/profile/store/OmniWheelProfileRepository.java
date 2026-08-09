package de.artemis.omniwheel.client.profile.store;

import de.artemis.omniwheel.OmniWheel;
import de.artemis.omniwheel.client.profile.DefaultProfiles;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class OmniWheelProfileRepository {
    private static final String MISSING_TOKEN = "missing";

    private final ProfileJsonCodec codec = new ProfileJsonCodec();
    private final Path storagePath = FMLPaths.CONFIGDIR.get().resolve("omniwheel").resolve("profiles.json");

    public ProfileCollection loadOrCreate() {
        if (Files.notExists(storagePath)) {
            ProfileCollection defaults = defaultCollection();
            save(defaults);
            return defaults;
        }

        try {
            ProfileCollection loaded = codec.decode(Files.readString(storagePath, StandardCharsets.UTF_8));
            ProfileCollection upgraded = ProfileUpgrader.upgrade(loaded);
            if (upgraded != loaded) {
                save(upgraded);
            }
            return upgraded;
        } catch (Exception exception) {
            OmniWheel.LOGGER.error("Failed to load OmniWheel profiles from {}", storagePath, exception);
            ProfileCollection defaults = defaultCollection();
            backupBrokenFile();
            save(defaults);
            return defaults;
        }
    }

    public void save(ProfileCollection collection) {
        try {
            Files.createDirectories(storagePath.getParent());
            Files.writeString(storagePath, codec.encode(collection), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save OmniWheel profiles to " + storagePath, exception);
        }
    }

    public Path storagePath() {
        return storagePath;
    }

    public String currentChangeToken() {
        try {
            if (Files.notExists(storagePath)) {
                return MISSING_TOKEN;
            }
            return Files.getLastModifiedTime(storagePath).toMillis() + ":" + Files.size(storagePath);
        } catch (IOException exception) {
            return MISSING_TOKEN;
        }
    }

    public ProfileCollection resetToDefaults() {
        ProfileCollection defaults = defaultCollection();
        save(defaults);
        return defaults;
    }

    private ProfileCollection defaultCollection() {
        return new ProfileCollection("default", List.of(DefaultProfiles.createDefaultProfile()));
    }

    private void backupBrokenFile() {
        if (Files.notExists(storagePath)) {
            return;
        }

        try {
            Path backupPath = storagePath.resolveSibling("profiles.broken.json");
            Files.createDirectories(backupPath.getParent());
            Files.copy(storagePath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }
}
