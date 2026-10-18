package de.artemis.omniwheel.client.render;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.JsonOps;
import de.artemis.omniwheel.OmniWheel;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stores the Custom library; stack and PNG data also travel with exported profiles. */
public final class CustomItemIcons {
    private static final String PREFIX = "stack:";
    private static final Map<String, ItemStack> DECODED = new LinkedHashMap<>();
    private static Object registryContext;
    private static List<String> library;

    private CustomItemIcons() {}

    public static boolean isCustom(String icon) { return icon != null && icon.startsWith(PREFIX); }

    public static String encode(ItemStack original) {
        if (original.isEmpty()) throw new IllegalArgumentException("Cannot use an empty item as an icon");
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) throw new IllegalStateException("Join a world to capture an item icon");
        // Keep count too: some modded item models depend on it.
        var json = ItemStack.CODEC.encodeStart(connection.registryAccess().createSerializationContext(JsonOps.INSTANCE), original.copy()).getOrThrow();
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static ItemStack decode(String icon) {
        if (!isCustom(icon)) return ItemStack.EMPTY;
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return ItemStack.EMPTY;
        Object context = connection.registryAccess();
        if (registryContext != context) { clearCache(); registryContext = context; }
        ItemStack cached = DECODED.get(icon);
        if (cached != null) return cached;
        try {
            String json = new String(Base64.getUrlDecoder().decode(icon.substring(PREFIX.length())), StandardCharsets.UTF_8);
            ItemStack stack = ItemStack.CODEC.parse(connection.registryAccess().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json)).result().orElse(ItemStack.EMPTY);
            if (DECODED.size() >= 256) DECODED.remove(DECODED.keySet().iterator().next());
            DECODED.put(icon, stack);
            return stack;
        } catch (RuntimeException | LinkageError ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static void clearCache() { DECODED.clear(); registryContext = null; }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("omniwheel/custom-icons.json");
    }

    public static List<String> saved() {
        if (library == null) {
            library = new ArrayList<>();
            if (Files.exists(path())) try {
                List<String> stored = new Gson().fromJson(Files.readString(path()), new TypeToken<List<String>>() {}.getType());
                if (stored != null) for (String icon : stored) if ((isCustom(icon) || CustomImageData.isImage(icon)) && !library.contains(icon)) library.add(icon);
            } catch (Exception exception) {
                OmniWheel.LOGGER.error("Could not load custom icons", exception);
            }
        }
        return List.copyOf(library);
    }

    public static void save(String icon) {
        saved();
        if ((!isCustom(icon) && !CustomImageData.isImage(icon)) || library.contains(icon)) return;
        List<String> next = new ArrayList<>(library);
        next.add(icon);
        try {
            Files.createDirectories(path().getParent());
            Path temporary = Files.createTempFile(path().getParent(), "custom-icons-", ".tmp");
            try {
                Files.writeString(temporary, new Gson().toJson(next));
                try {
                    Files.move(temporary, path(), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, path(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            library = next;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save custom icon", exception);
        }
    }
}
