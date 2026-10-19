package de.artemis.omniwheel.client.render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/** Portable PNG data, trimmed to the exact bounds of pixels with nonzero alpha. */
public record CustomImageData(String name, byte[] png, int width, int height) {
    private static final String PREFIX = "image:";
    private static final int MAX_BYTES = 32 * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;

    public static boolean isImage(String icon) { return icon != null && icon.startsWith(PREFIX); }

    public static CustomImageData read(Path path) throws IOException {
        if (Files.size(path) > MAX_BYTES) throw new IOException("PNG files must be smaller than 32 MB.");
        String name = path.getFileName().toString().replaceFirst("(?i)\\.png$", "");
        return fromPng(name, Files.readAllBytes(path));
    }

    public static CustomImageData fromPng(String name, byte[] bytes) throws IOException {
        if (bytes.length > MAX_BYTES || bytes.length < 8 || bytes[0] != (byte) 137 || bytes[1] != 'P'
                || bytes[2] != 'N' || bytes[3] != 'G' || bytes[4] != 13 || bytes[5] != 10 || bytes[6] != 26 || bytes[7] != 10) {
            throw new IOException("Choose a valid PNG image.");
        }
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("This PNG could not be read.");
            var reader = readers.next();
            try {
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new IOException("PNG images can be at most 4096 x 4096 pixels.");
                }
                BufferedImage image = reader.read(0);
                int left = width, top = height, right = -1, bottom = -1;
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                    if ((image.getRGB(x, y) >>> 24) != 0) {
                        left = Math.min(left, x); top = Math.min(top, y);
                        right = Math.max(right, x); bottom = Math.max(bottom, y);
                    }
                }
                if (right < left) throw new IOException("This PNG is completely transparent.");
                int croppedWidth = right - left + 1, croppedHeight = bottom - top + 1;
                BufferedImage cropped = new BufferedImage(croppedWidth, croppedHeight, BufferedImage.TYPE_INT_ARGB);
                cropped.setRGB(0, 0, croppedWidth, croppedHeight,
                        image.getRGB(left, top, croppedWidth, croppedHeight, null, 0, croppedWidth), 0, croppedWidth);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!ImageIO.write(cropped, "png", output)) throw new IOException("Could not save PNG data.");
                return new CustomImageData(name == null || name.isBlank() ? "Imported image" : name, output.toByteArray(), croppedWidth, croppedHeight);
            } finally {
                reader.dispose();
            }
        }
    }

    public String icon() {
        var base64 = Base64.getUrlEncoder().withoutPadding();
        return PREFIX + base64.encodeToString(name.getBytes(StandardCharsets.UTF_8)) + ":" + base64.encodeToString(png);
    }

    public static CustomImageData decode(String icon) throws IOException {
        if (!isImage(icon) || icon.length() > MAX_BYTES * 2) throw new IOException("Invalid image icon.");
        int separator = icon.indexOf(':', PREFIX.length());
        if (separator < 0) throw new IOException("Invalid image icon.");
        try {
            var base64 = Base64.getUrlDecoder();
            String name = new String(base64.decode(icon.substring(PREFIX.length(), separator)), StandardCharsets.UTF_8);
            return fromPng(name, base64.decode(icon.substring(separator + 1)));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid image icon.", exception);
        }
    }

    public static String displayName(String icon) {
        if (!isImage(icon)) return "Imported image";
        int separator = icon.indexOf(':', PREFIX.length());
        try {
            return new String(Base64.getUrlDecoder().decode(icon.substring(PREFIX.length(), separator)), StandardCharsets.UTF_8);
        } catch (RuntimeException ignored) {
            return "Imported image";
        }
    }
}
