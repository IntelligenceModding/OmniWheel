package de.artemis.omniwheel.client.render;

import de.artemis.omniwheel.client.render.GuiGraphics;

/** Shared lift and layered shadow for management rows and wheel-preview drags. */
public final class LiftedRowRenderer {
    private LiftedRowRenderer() {}

    public static void render(GuiGraphics graphics, int x, int y, int width, int height, Runnable row) {
        graphics.flush();
        shadow(graphics, x, y, width, height);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(1.025F, 1.025F, 1.0F);
        row.run();
        graphics.pose().popPose();
    }

    public static void shadow(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 3, y + 7, x + width + 9, y + height + 13, 0x18000000);
        graphics.fill(x - 1, y + 5, x + width + 6, y + height + 9, 0x30000000);
        graphics.fill(x + 2, y + 4, x + width + 4, y + height + 6, 0x60000000);
    }
}
