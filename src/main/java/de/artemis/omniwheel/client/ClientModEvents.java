package de.artemis.omniwheel.client;

import de.artemis.omniwheel.client.input.LegacyKeyMappings;
import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.render.EntryIconRenderer;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;

public final class ClientModEvents {
    private ClientModEvents() {}

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        OmniWheelKeyMappings.register(event);
        LegacyKeyMappings.register(event);
    }

    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> EntryIconRenderer.clearResourceCaches());
    }

    public static void onChatOverlay(CustomizeGuiOverlayEvent.Chat event) {
        var graphics = event.getGuiGraphics();
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 4000);
        OmniWheelClientRuntime.getInstance().renderOverlay(graphics);
        graphics.flush();
        graphics.pose().popPose();
    }

    public static void onClientTickStart(TickEvent.ClientTickEvent.Pre event) {
        OmniWheelClientRuntime.getInstance().keybindController().startTick(Minecraft.getInstance());
    }

    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LegacyKeyMappings.tick(minecraft);
        while (OmniWheelKeyMappings.OPEN_MANAGER.consumeClick()) {
            OmniWheelClientRuntime.getInstance().openProfileManagerShortcut();
        }
        OmniWheelClientRuntime.getInstance().tick();
    }
}
