package de.artemis.omniwheel.client.runtime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import de.artemis.omniwheel.common.OmniWheelText;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class PlaceholderResolver {
    private PlaceholderResolver() {
    }

    public static String resolve(String raw, Minecraft minecraft) {
        raw = OmniWheelText.resolve(raw);
        if (minecraft.player == null || minecraft.level == null) {
            return raw;
        }

        Map<String, String> replacements = new LinkedHashMap<>();
        BlockPos pos = minecraft.player.blockPosition();
        ServerData currentServer = minecraft.getCurrentServer();

        replacements.put("{player}", minecraft.player.getName().getString());
        replacements.put("{x}", Integer.toString(pos.getX()));
        replacements.put("{y}", Integer.toString(pos.getY()));
        replacements.put("{z}", Integer.toString(pos.getZ()));
        replacements.put("{dimension}", minecraft.level.dimension().location().toString());
        replacements.put("{server}", currentServer != null ? currentServer.name : OmniWheelText.translate("omniwheel.placeholder.singleplayer"));
        replacements.put("{profile}", OmniWheelText.resolve(OmniWheelClientRuntime.getInstance().activeProfile().displayName()));
        replacements.put("{gamemode}", currentGameModeName(minecraft));

        String resolved = raw;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            resolved = resolved.replace(entry.getKey(), entry.getValue());
        }
        return resolved;
    }

    private static String currentGameModeName(Minecraft minecraft) {
        if (minecraft.gameMode == null || minecraft.gameMode.getPlayerMode() == null) {
            return OmniWheelText.translate("omniwheel.placeholder.unknown");
        }

        String id = minecraft.gameMode.getPlayerMode().getName();
        String vanillaKey = "selectWorld.gameMode." + id.toLowerCase(Locale.ROOT);
        return OmniWheelText.isTranslationKey(vanillaKey) ? OmniWheelText.translate(vanillaKey) : id;
    }
}
