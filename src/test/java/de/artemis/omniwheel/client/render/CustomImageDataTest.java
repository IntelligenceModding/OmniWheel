package de.artemis.omniwheel.client.render;

import org.junit.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import static org.junit.Assert.*;

public class CustomImageDataTest {

    private static byte[] png(BufferedImage image) throws IOException {
        java.io.ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return output.toByteArray();
    }

    @Test
    public void cropsToEveryVisiblePixelIncludingAlmostTransparentEdges() throws Exception {
        java.awt.image.BufferedImage source = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(42, 37, 0x01FF0000);
        source.setRGB(199, 215, 0xFF00FF00);
        de.artemis.omniwheel.client.render.CustomImageData result = CustomImageData.fromPng("Transparent icon", png(source));
        assertEquals(158, result.width());
        assertEquals(179, result.height());
        java.awt.image.BufferedImage cropped = ImageIO.read(new ByteArrayInputStream(result.png()));
        assertEquals(0x01FF0000, cropped.getRGB(0, 0));
        assertEquals(0xFF00FF00, cropped.getRGB(157, 178));
        assertEquals(0, cropped.getRGB(20, 20));
    }

    @Test
    public void preservesColorsAlphaAndAspectRatio() throws Exception {
        java.awt.image.BufferedImage source = new BufferedImage(8, 7, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(2, 1, 0xFF123456);
        source.setRGB(5, 3, 0x80123456);
        source.setRGB(3, 2, 0x4000FF00);
        de.artemis.omniwheel.client.render.CustomImageData result = CustomImageData.fromPng("Color", png(source));
        assertEquals(4, result.width());
        assertEquals(3, result.height());
        java.awt.image.BufferedImage cropped = ImageIO.read(new ByteArrayInputStream(result.png()));
        for (int y = 0; y < 3; y++) for (int x = 0; x < 4; x++) assertEquals(source.getRGB(x + 2, y + 1), cropped.getRGB(x, y));
    }

    @Test
    public void keepsOpaqueEdgesAndSinglePixelImages() throws Exception {
        java.awt.image.BufferedImage source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xFFFFFFFF);
        source.setRGB(2, 1, 0xFFFFFFFF);
        de.artemis.omniwheel.client.render.CustomImageData result = CustomImageData.fromPng("Edges", png(source));
        assertEquals(3, result.width());
        assertEquals(2, result.height());
        source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(1, 1, 0x80123456);
        result = CustomImageData.fromPng("Pixel", png(source));
        assertEquals(1, result.width());
        assertEquals(1, result.height());
    }

    @Test
    public void imageAndNameTravelInIconWithoutOriginalFile() throws Exception {
        java.awt.image.BufferedImage source = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(3, 4, 0xCC123456);
        de.artemis.omniwheel.client.render.CustomImageData result = CustomImageData.fromPng("My icon: Unicode \u2605", png(source));
        de.artemis.omniwheel.client.render.CustomImageData restored = CustomImageData.decode(result.icon());
        assertEquals(result.name(), CustomImageData.displayName(result.icon()));
        assertEquals(result.name(), restored.name());
        assertArrayEquals(result.png(), restored.png());
        assertEquals(result.width(), restored.width());
        assertEquals(result.height(), restored.height());
    }

    @Test(expected = IOException.class)
    public void rejectsFullyTransparentImages() throws Exception {
        CustomImageData.fromPng("Empty", png(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)));
    }

    @Test(expected = IOException.class)
    public void rejectsInvalidFiles() throws Exception {
        CustomImageData.fromPng("Invalid", new byte[] { 1, 2, 3 });
    }

    @Test(expected = IOException.class)
    public void rejectsBrokenEmbeddedImages() throws Exception {
        CustomImageData.decode("image:broken:not-png");
    }
}
