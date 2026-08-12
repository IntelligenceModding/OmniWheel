package de.artemis.omniwheel.client.runtime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class PlaceholderResolver {
    private PlaceholderResolver() {
    }

    public static String resolve(String raw, Minecraft minecraft) {
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
        replacements.put("{dimension}", minecraft.level.dimension().identifier().toString());
        replacements.put("{server}", currentServer != null ? currentServer.name : "singleplayer");
        replacements.put("{profile}", OmniWheelClientRuntime.getInstance().activeProfile().displayName());
        replacements.put("{gamemode}", minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() != null
                ? minecraft.gameMode.getPlayerMode().name().toLowerCase(Locale.ROOT)
                : "unknown");

        String resolved = raw;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            resolved = resolved.replace(entry.getKey(), entry.getValue());
        }
        return resolved;
    }
}
