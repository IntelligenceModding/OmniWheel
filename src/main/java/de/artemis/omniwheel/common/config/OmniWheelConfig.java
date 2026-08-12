package de.artemis.omniwheel.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.artemis.omniwheel.OmniWheel;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class OmniWheelConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(OmniWheel.MOD_ID)
            .resolve("client.json");

    public static final ClientValues CLIENT = new ClientValues();

    private OmniWheelConfig() {
    }

    public static void load() {
        if (Files.notExists(CONFIG_PATH)) {
            save();
            return;
        }

        try {
            JsonObject root = GSON.fromJson(Files.readString(CONFIG_PATH, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject ui = object(root, "ui");
            CLIENT.wheelRadius.load(ui);
            CLIENT.deadzoneRadius.load(ui);
            CLIENT.segmentGapDegrees.load(ui);
            CLIENT.backgroundAlpha.load(ui);
            save();
        } catch (Exception exception) {
            OmniWheel.LOGGER.error("Failed to load OmniWheel config from {}", CONFIG_PATH, exception);
            backupBrokenFile();
            save();
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        JsonObject ui = new JsonObject();
        root.add("ui", ui);
        CLIENT.wheelRadius.write(ui);
        CLIENT.deadzoneRadius.write(ui);
        CLIENT.segmentGapDegrees.write(ui);
        CLIENT.backgroundAlpha.write(ui);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save OmniWheel config to " + CONFIG_PATH, exception);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null) {
            return new JsonObject();
        }
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonObject()) {
            return new JsonObject();
        }
        return element.getAsJsonObject();
    }

    private static void backupBrokenFile() {
        if (Files.notExists(CONFIG_PATH)) {
            return;
        }

        try {
            Files.copy(CONFIG_PATH, CONFIG_PATH.resolveSibling("client.broken.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    public static final class ClientValues {
        public final DoubleValue wheelRadius = new DoubleValue("wheelRadius", 118.0D, 72.0D, 220.0D);
        public final DoubleValue deadzoneRadius = new DoubleValue("deadzoneRadius", 28.0D, 8.0D, 96.0D);
        public final IntValue segmentGapDegrees = new IntValue("segmentGapDegrees", 2, 0, 12);
        public final IntValue backgroundAlpha = new IntValue("backgroundAlpha", 148, 0, 255);

        private ClientValues() {
        }
    }

    public static final class DoubleValue {
        private final String key;
        private final double min;
        private final double max;
        private double value;

        private DoubleValue(String key, double defaultValue, double min, double max) {
            this.key = key;
            this.min = min;
            this.max = max;
            this.value = defaultValue;
        }

        public Double get() {
            return value;
        }

        private void load(JsonObject parent) {
            JsonElement element = parent.get(key);
            if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                value = clamp(element.getAsDouble(), min, max);
            }
        }

        private void write(JsonObject parent) {
            parent.addProperty(key, value);
        }
    }

    public static final class IntValue {
        private final String key;
        private final int min;
        private final int max;
        private int value;

        private IntValue(String key, int defaultValue, int min, int max) {
            this.key = key;
            this.min = min;
            this.max = max;
            this.value = defaultValue;
        }

        public Integer get() {
            return value;
        }

        private void load(JsonObject parent) {
            JsonElement element = parent.get(key);
            if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                value = (int) Math.round(clamp(element.getAsInt(), min, max));
            }
        }

        private void write(JsonObject parent) {
            parent.addProperty(key, value);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
