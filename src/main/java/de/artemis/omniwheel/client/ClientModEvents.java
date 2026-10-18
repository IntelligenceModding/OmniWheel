package de.artemis.omniwheel.client;

import de.artemis.omniwheel.client.input.LegacyKeyMappings;
import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.render.EntryIconRenderer;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.event.TickEvent;

public final class ClientModEvents {
    private ClientModEvents() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            OmniWheelKeyMappings.register();
            LegacyKeyMappings.register();
            ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager())
                    .registerReloadListener((ResourceManagerReloadListener) manager -> EntryIconRenderer.clearResourceCaches());
        });
    }

    public static void onRenderGui(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        var graphics = new de.artemis.omniwheel.client.render.GuiGraphics(Minecraft.getInstance(), event.getMatrixStack());
        graphics.flush();
        graphics.pose().pushPose();
        // Render after the HUD so chat cannot dim item icons. Stay within 1.18.2's GUI depth range.
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
