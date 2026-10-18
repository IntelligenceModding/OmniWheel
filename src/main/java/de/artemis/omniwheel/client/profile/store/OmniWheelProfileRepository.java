package de.artemis.omniwheel.client.profile.store;

import de.artemis.omniwheel.OmniWheel;
import de.artemis.omniwheel.client.profile.DefaultProfiles;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class OmniWheelProfileRepository {

    private static final String MISSING_TOKEN = "missing";

    private final ProfileJsonCodec codec = new ProfileJsonCodec();

    private final Path storagePath;

    public OmniWheelProfileRepository() {
        this(FMLPaths.CONFIGDIR.get().resolve("omniwheel").resolve("profiles.json"));
    }

    OmniWheelProfileRepository(Path storagePath) {
        this.storagePath = storagePath;
    }

    public ProfileCollection loadOrCreate() {
        if (Files.notExists(storagePath)) {
            ProfileCollection defaults = defaultCollection();
            save(defaults);
            return defaults;
        }
        ProfileCollection loaded;
        String raw;
        try {
            raw = de.artemis.omniwheel.common.Java8.readString(storagePath, StandardCharsets.UTF_8);
            loaded = codec.decode(raw);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read OmniWheel profiles from " + storagePath, exception);
        } catch (Exception exception) {
            OmniWheel.LOGGER.error("Failed to load OmniWheel profiles from {}", storagePath, exception);
            backupBrokenFile();
            throw new IllegalStateException("Cannot load OmniWheel profiles; original file was preserved at " + storagePath, exception);
        }
        ProfileCollection upgraded = ProfileUpgrader.upgrade(loaded);
        if (upgraded != loaded || codec.needsUpgrade(raw)) {
            // Save failures must not be mistaken for corrupt input and reset the user's profiles.
            backupBeforeUpgrade();
            save(upgraded);
        }
        return upgraded;
    }

    public void save(ProfileCollection collection) {
        Path temporary = null;
        try {
            Files.createDirectories(storagePath.getParent());
            temporary = Files.createTempFile(storagePath.getParent(), "profiles-", ".tmp");
            de.artemis.omniwheel.common.Java8.writeString(temporary, codec.encode(collection), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, storagePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, storagePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save OmniWheel profiles to " + storagePath, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void backupBeforeUpgrade() {
        try {
            backupOriginal("profiles.before-upgrade");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to back up OmniWheel profiles before upgrading " + storagePath, exception);
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
        return new ProfileCollection("default", de.artemis.omniwheel.common.Java8.list(DefaultProfiles.createDefaultProfile()));
    }

    private void backupBrokenFile() {
        if (Files.notExists(storagePath)) {
            return;
        }
        try {
            backupOriginal("profiles.broken");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to back up unreadable OmniWheel profiles from " + storagePath, exception);
        }
    }

    private void backupOriginal(String name) throws IOException {
        for (int index = 1; ; index++) {
            Path backup = storagePath.resolveSibling(name + (index == 1 ? "" : "-" + index) + ".json");
            if (Files.notExists(backup)) {
                Files.copy(storagePath, backup);
                return;
            }
            if (de.artemis.omniwheel.common.Java8.mismatch(storagePath, backup) == -1)
                return;
        }
    }
}
