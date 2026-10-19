package de.artemis.omniwheel.client;

import de.artemis.omniwheel.client.compat.PatchedModListScreen;
import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.render.EntryIconRenderer;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;

import static de.artemis.omniwheel.OmniWheel.MOD_ID;

public final class ClientModEvents {
    private static final Identifier OVERLAY_ID = Identifier.fromNamespaceAndPath(MOD_ID, "wheel");

    private ClientModEvents() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        OmniWheelKeyMappings.register(event);
    }

    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        Identifier iconValidation = Identifier.fromNamespaceAndPath(MOD_ID, "icon_validation");
        event.addListener(iconValidation, (ResourceManagerReloadListener) resources -> EntryIconRenderer.clearResourceCaches());
        event.addDependency(VanillaClientListeners.ATLASES, iconValidation);
        event.addDependency(VanillaClientListeners.MODELS, iconValidation);
        event.addDependency(VanillaClientListeners.PARTICLE_RESOURCES, iconValidation);
    }

    public static void onClientTickPre(ClientTickEvent.Pre event) {
        OmniWheelClientRuntime.getInstance().keybindController().startTick(Minecraft.getInstance());
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof ModListScreen && !(event.getNewScreen() instanceof PatchedModListScreen)) {
            event.setNewScreen(new PatchedModListScreen(event.getCurrentScreen()));
        }
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, OVERLAY_ID, (graphics, deltaTracker) ->
                OmniWheelClientRuntime.getInstance().renderOverlay(graphics));
    }

    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.CROSSHAIR.equals(event.getName()) && OmniWheelClientRuntime.getInstance().isOverlayUiOpen()) {
            event.setCanceled(true);
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
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
