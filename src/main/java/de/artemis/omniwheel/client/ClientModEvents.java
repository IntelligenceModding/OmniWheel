package de.artemis.omniwheel.client;

import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

import static de.artemis.omniwheel.OmniWheel.MOD_ID;

public final class ClientModEvents {
    private ClientModEvents() {
    }

    public static void register() {
        OmniWheelKeyMappings.register();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "wheel"), (graphics, deltaTracker) ->
                OmniWheelClientRuntime.getInstance().renderOverlay(graphics));
        ClientTickEvents.END_CLIENT_TICK.register(ignored -> onClientTick());
    }

    private static void onClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        while (OmniWheelKeyMappings.OPEN_MANAGER.consumeClick()) {
            OmniWheelClientRuntime.getInstance().openProfileManagerShortcut();
        }
        if (minecraft.player == null || minecraft.level == null) {
            OmniWheelClientRuntime.getInstance().tick();
            return;
        }

        OmniWheelClientRuntime.getInstance().tick();
    }
}
