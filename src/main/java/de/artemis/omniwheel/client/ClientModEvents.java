package de.artemis.omniwheel.client;

import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.render.EntryIconRenderer;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import static de.artemis.omniwheel.OmniWheel.MOD_ID;

public final class ClientModEvents {
    private ClientModEvents() {
    }

    public static void register() {
        OmniWheelKeyMappings.register();
        ResourceLoader resources = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        Identifier iconValidation = Identifier.fromNamespaceAndPath(MOD_ID, "icon_validation");
        resources.registerReloader(iconValidation, new SimpleResourceReloader<Void>() {
            @Override
            protected Void prepare(PreparableReloadListener.SharedState state) {
                return null;
            }

            @Override
            protected void apply(Void unused, PreparableReloadListener.SharedState state) {
                EntryIconRenderer.clearResourceCaches();
            }
        });
        resources.addReloaderOrdering(ResourceReloaderKeys.Client.ATLAS, iconValidation);
        resources.addReloaderOrdering(ResourceReloaderKeys.Client.MODELS, iconValidation);
        resources.addReloaderOrdering(ResourceReloaderKeys.Client.PARTICLES, iconValidation);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "wheel"), (graphics, deltaTracker) ->
                OmniWheelClientRuntime.getInstance().renderOverlay(graphics));
        ClientTickEvents.START_CLIENT_TICK.register(minecraft ->
                OmniWheelClientRuntime.getInstance().keybindController().startTick(minecraft));
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
