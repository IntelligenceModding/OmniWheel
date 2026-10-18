package de.artemis.omniwheel.client;

import de.artemis.omniwheel.client.input.LegacyKeyMappings;
import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.render.EntryIconRenderer;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RenderGuiEvent;
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

    public static void onRenderGui(RenderGuiEvent.Post event) {
        var graphics = new de.artemis.omniwheel.client.render.GuiGraphics(Minecraft.getInstance(), event.getPoseStack());
        graphics.flush();
        graphics.pose().pushPose();
        // Render after the HUD so chat cannot dim item icons. Stay within 1.19.2's GUI depth range.
        graphics.pose().translate(0, 0, 400);
        OmniWheelClientRuntime.getInstance().renderOverlay(graphics);
        graphics.flush();
        graphics.pose().popPose();
    }

    public static void onClientTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        OmniWheelClientRuntime.getInstance().keybindController().startTick(Minecraft.getInstance());
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LegacyKeyMappings.tick(minecraft);
        while (OmniWheelKeyMappings.OPEN_MANAGER.consumeClick()) {
            OmniWheelClientRuntime.getInstance().openProfileManagerShortcut();
        }
        OmniWheelClientRuntime.getInstance().tick();
    }
}
