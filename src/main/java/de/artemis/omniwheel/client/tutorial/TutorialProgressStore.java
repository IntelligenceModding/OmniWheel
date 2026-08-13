package de.artemis.omniwheel.client.tutorial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class TutorialProgressStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path storagePath = FabricLoader.getInstance().getConfigDir().resolve("omniwheel").resolve("tutorials.json");
    private StoreState state;

    public boolean isCompleted(TutorialType tutorialType) {
        TutorialState current = currentContextState();
        return switch (tutorialType) {
            case RADIAL -> current.radialCompleted;
            case MANAGER -> current.managerCompleted;
        };
    }

    public int currentStep(TutorialType tutorialType) {
        TutorialState current = currentContextState();
        return switch (tutorialType) {
            case RADIAL -> Math.max(0, current.radialStep);
            case MANAGER -> Math.max(0, current.managerStep);
        };
    }

    public void setCurrentStep(TutorialType tutorialType, int step) {
        TutorialState current = currentContextState();
        int normalizedStep = Math.max(0, step);
        switch (tutorialType) {
            case RADIAL -> {
                current.radialCompleted = false;
                current.radialStep = normalizedStep;
            }
            case MANAGER -> {
                current.managerCompleted = false;
                current.managerStep = normalizedStep;
            }
        }
        save(state);
    }

    public void markCompleted(TutorialType tutorialType) {
        TutorialState current = currentContextState();
        switch (tutorialType) {
            case RADIAL -> {
                current.radialCompleted = true;
                current.radialStep = 0;
            }
            case MANAGER -> {
                current.managerCompleted = true;
                current.managerStep = 0;
            }
        }
        save(state);
    }

    private TutorialState currentContextState() {
        StoreState current = ensureLoaded();
        String contextKey = currentContextKey();
        return current.contexts.computeIfAbsent(contextKey, ignored -> new TutorialState());
    }

    private StoreState ensureLoaded() {
        if (state != null) {
            return state;
        }
        if (!Files.exists(storagePath)) {
            state = new StoreState();
            return state;
        }
        try {
            String raw = Files.readString(storagePath, StandardCharsets.UTF_8);
            StoreState loaded = GSON.fromJson(raw, StoreState.class);
            if (loaded == null) {
                loaded = new StoreState();
            }
            if (loaded.contexts == null) {
                loaded.contexts = new LinkedHashMap<>();
            }
            if ((loaded.radialCompleted || loaded.managerCompleted || loaded.radialStep > 0 || loaded.managerStep > 0)
                    && loaded.contexts.isEmpty()) {
                TutorialState legacy = new TutorialState();
                legacy.radialCompleted = loaded.radialCompleted;
                legacy.managerCompleted = loaded.managerCompleted;
                legacy.radialStep = loaded.radialStep;
                legacy.managerStep = loaded.managerStep;
                loaded.contexts.put("legacy", legacy);
            }
            migrateSingleplayerContexts(loaded);
            state = loaded;
            return state;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load OmniWheel tutorial state from " + storagePath, exception);
        }
    }

    private void save(StoreState tutorialState) {
        try {
            Files.createDirectories(storagePath.getParent());
            Files.writeString(storagePath, GSON.toJson(tutorialState), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save OmniWheel tutorial state to " + storagePath, exception);
        }
    }

    private static String currentContextKey() {
        Minecraft minecraft = Minecraft.getInstance();
        String userKey = currentUserKey(minecraft);

        if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null) {
            String worldKey;
            try {
                worldKey = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize().toString();
            } catch (Exception ignored) {
                worldKey = minecraft.getSingleplayerServer().getWorldData().getLevelName();
            }
            return "sp:" + normalizeKey(worldKey);
        }

        ServerData server = minecraft.getCurrentServer();
        if (server != null) {
            String serverKey = !server.ip.isBlank() ? server.ip : server.name;
            return "mp:" + userKey + ":" + normalizeKey(serverKey);
        }

        return "global:" + userKey;
    }

    @SuppressWarnings("ConstantValue")
    private static String currentUserKey(Minecraft minecraft) {
        User user = minecraft.getUser();
        if (user == null) {
            return "unknown";
        }
        var profileId = user.getProfileId();
        if (profileId != null) {
            return normalizeKey(profileId.toString());
        }
        return normalizeKey(user.getName());
    }

    private static void migrateSingleplayerContexts(StoreState storeState) {
        Map<String, TutorialState> migrated = new LinkedHashMap<>();
        for (Map.Entry<String, TutorialState> entry : storeState.contexts.entrySet()) {
            String contextKey = entry.getKey();
            if (contextKey == null || !contextKey.startsWith("sp:")) {
                continue;
            }

            int worldSeparator = contextKey.indexOf(':', 3);
            if (worldSeparator < 0 || worldSeparator >= contextKey.length() - 1) {
                continue;
            }

            String worldScopedKey = "sp:" + contextKey.substring(worldSeparator + 1);
            mergeState(migrated.computeIfAbsent(worldScopedKey, ignored -> new TutorialState()), entry.getValue());
        }

        for (Map.Entry<String, TutorialState> entry : migrated.entrySet()) {
            mergeState(storeState.contexts.computeIfAbsent(entry.getKey(), ignored -> new TutorialState()), entry.getValue());
        }
    }

    private static void mergeState(TutorialState target, TutorialState source) {
        if (source == null) {
            return;
        }
        target.radialCompleted = target.radialCompleted || source.radialCompleted;
        target.managerCompleted = target.managerCompleted || source.managerCompleted;
        if (!target.radialCompleted) {
            target.radialStep = Math.max(target.radialStep, source.radialStep);
        } else {
            target.radialStep = 0;
        }
        if (!target.managerCompleted) {
            target.managerStep = Math.max(target.managerStep, source.managerStep);
        } else {
            target.managerStep = 0;
        }
    }

    private static String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private static final class StoreState {
        Map<String, TutorialState> contexts = new LinkedHashMap<>();

        // Legacy root-level fields kept for backward compatibility while reading older files.
        boolean radialCompleted;
        boolean managerCompleted;
        int radialStep;
        int managerStep;
    }

    private static final class TutorialState {
        boolean radialCompleted;
        boolean managerCompleted;
        int radialStep;
        int managerStep;
    }
}
