package de.artemis.omniwheel.client;

import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.input.LegacyKeyMappings;
import de.artemis.omniwheel.client.render.EntryIconRenderer;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;

import static de.artemis.omniwheel.OmniWheel.MOD_ID;

public final class ClientModEvents {
    private static final ResourceLocation OVERLAY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "wheel");

    private ClientModEvents() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        OmniWheelKeyMappings.register(event);
        LegacyKeyMappings.register(event);
    }

    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        ResourceLocation iconValidation = ResourceLocation.fromNamespaceAndPath(MOD_ID, "icon_validation");
        event.addListener(iconValidation, (ResourceManagerReloadListener) resources -> EntryIconRenderer.clearResourceCaches());
        event.addDependency(VanillaClientListeners.TEXTURES, iconValidation);
        event.addDependency(VanillaClientListeners.MODELS, iconValidation);
        event.addDependency(VanillaClientListeners.PARTICLE_ENGINE, iconValidation);
        event.addDependency(VanillaClientListeners.GUI_SPRITES, iconValidation);
    }

    public static void onClientTickPre(ClientTickEvent.Pre event) {
        OmniWheelClientRuntime.getInstance().keybindController().startTick(Minecraft.getInstance());
    }


    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, OVERLAY_ID, (graphics, deltaTracker) ->
                OmniWheelClientRuntime.getInstance().renderOverlay(graphics));
    }

    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.CROSSHAIR.equals(event.getName()) && OmniWheelClientRuntime.getInstance().isOverlayUiOpen()) {
            event.setCanceled(true);
        }
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
