package de.artemis.omniwheel.client;

import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.render.GuiGraphics;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientModEvents {
    private ClientModEvents() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(OmniWheelKeyMappings::register);
    }

    public static void onChatOverlay(RenderGameOverlayEvent.Chat event) {
        OmniWheelClientRuntime.getInstance().renderOverlay(new GuiGraphics(event.getMatrixStack()));
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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

    public static void onMouseScroll(InputEvent.MouseScrollEvent event) {
        if (OmniWheelClientRuntime.getInstance().handleMouseScroll(0.0D, event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    public static void onMouseButton(InputEvent.RawMouseEvent event) {
        if (OmniWheelClientRuntime.getInstance().handleMouseButton(event.getButton(), event.getAction())) {
            event.setCanceled(true);
        }
    }
}
