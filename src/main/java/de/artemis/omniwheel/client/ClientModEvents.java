package de.artemis.omniwheel.client;

import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.input.LegacyKeyMappings;
import de.artemis.omniwheel.client.render.EntryIconRenderer;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

import static de.artemis.omniwheel.OmniWheel.MOD_ID;

public final class ClientModEvents {
    private ClientModEvents() {
    }

    public static void register() {
        OmniWheelKeyMappings.register();
        LegacyKeyMappings.register();
        ResourceLocation iconValidation = new ResourceLocation(MOD_ID, "icon_validation");
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return iconValidation;
            }

            @Override
            public java.util.Collection<ResourceLocation> getFabricDependencies() {
                return java.util.List.of(ResourceReloadListenerKeys.TEXTURES, ResourceReloadListenerKeys.MODELS);
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                EntryIconRenderer.clearResourceCaches();
            }
        });
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            graphics.flush();
            graphics.pose().pushPose();
            // This callback follows the HUD but is outside vanilla's nested depth layers.
            graphics.pose().translate(0, 0, 4000);
            OmniWheelClientRuntime.getInstance().renderOverlay(graphics);
            graphics.flush();
            graphics.pose().popPose();
        });
        ClientTickEvents.START_CLIENT_TICK.register(minecraft ->
                OmniWheelClientRuntime.getInstance().keybindController().startTick(minecraft));
        ClientTickEvents.END_CLIENT_TICK.register(ignored -> onClientTick());
    }

    private static void onClientTick() {
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
