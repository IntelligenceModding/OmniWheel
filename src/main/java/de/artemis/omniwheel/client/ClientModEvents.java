package de.artemis.omniwheel.client;

import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.input.LegacyKeyMappings;
import de.artemis.omniwheel.client.render.EntryIconRenderer;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import static de.artemis.omniwheel.OmniWheel.MOD_ID;

public final class ClientModEvents {
    private static final ResourceLocation OVERLAY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "wheel");

    private ClientModEvents() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        OmniWheelKeyMappings.register(event);
        LegacyKeyMappings.register(event);
    }

    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        // This event appends listeners after vanilla's texture, model, and particle loaders.
        event.registerReloadListener((ResourceManagerReloadListener) resources -> EntryIconRenderer.clearResourceCaches());
    }

    public static void onClientTickPre(ClientTickEvent.Pre event) {
        OmniWheelClientRuntime.getInstance().keybindController().startTick(Minecraft.getInstance());
    }


    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.wrapLayer(VanillaGuiLayers.CROSSHAIR, original -> (graphics, deltaTracker) -> {
            if (!OmniWheelClientRuntime.getInstance().isOverlayUiOpen()) {
                original.render(graphics, deltaTracker);
            }
        });
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, OVERLAY_ID, (graphics, deltaTracker) ->
                OmniWheelClientRuntime.getInstance().renderOverlay(graphics));
    }


    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LegacyKeyMappings.tick(minecraft);
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
