package de.artemis.omniwheel.client.runtime;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ContextValueFormatter {
    private static final Style COPY_STYLE = Style.EMPTY
            .withColor(ChatFormatting.AQUA)
            .withUnderlined(true);

    private ContextValueFormatter() {
    }

    public static List<CopyValue> extractCopyValues(String template, Minecraft minecraft) {
        List<CopyValue> values = new ArrayList<>();
        if (minecraft.player == null || minecraft.level == null) {
            return values;
        }

        if (containsCoordinates(template)) {
            values.add(new CopyValue("Coords: " + currentCoordinatesDisplay(minecraft), currentCoordinatesClipboard(minecraft), "Coordinates"));
        }
        if (template.contains("{dimension}")) {
            values.add(new CopyValue("Dimension: " + currentDimensionDisplay(minecraft), currentDimensionRaw(minecraft), "Dimension"));
        }
        return values;
    }

    public static String resolveForDisplay(String template, Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return template;
        }
        return resolveLiteral(template, minecraft);
    }

    public static Component buildInteractiveComponent(String template, Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return Component.literal(template);
        }

        MutableComponent result = Component.empty();
        int cursor = 0;
        while (cursor < template.length()) {
            int coordIndex = findNextCoordinateToken(template, cursor);
            int dimensionIndex = template.indexOf("{dimension}", cursor);
            int nextIndex = minPositive(coordIndex, dimensionIndex);

            if (nextIndex < 0) {
                result.append(Component.literal(resolveLiteral(template.substring(cursor), minecraft)));
                break;
            }

            if (nextIndex > cursor) {
                result.append(Component.literal(resolveLiteral(template.substring(cursor, nextIndex), minecraft)));
            }

            if (nextIndex == coordIndex) {
                result.append(copyableLiteral(currentCoordinatesDisplay(minecraft), currentCoordinatesClipboard(minecraft), "Copy coordinates"));
                cursor = nextIndex + coordinateTokenLength(template, nextIndex);
            } else {
                result.append(copyableLiteral(currentDimensionDisplay(minecraft), currentDimensionRaw(minecraft), "Copy dimension id"));
                cursor = nextIndex + "{dimension}".length();
            }
        }

        return result;
    }

    public static String currentDimensionRaw(Minecraft minecraft) {
        return minecraft.level.dimension().location().toString();
    }

    public static String currentDimensionDisplay(Minecraft minecraft) {
        String raw = currentDimensionRaw(minecraft);
        return formatDimensionId(raw);
    }

    public static String currentCoordinatesDisplay(Minecraft minecraft) {
        return minecraft.player.blockPosition().getX() + ", "
                + minecraft.player.blockPosition().getY() + ", "
                + minecraft.player.blockPosition().getZ();
    }

    public static String currentCoordinatesClipboard(Minecraft minecraft) {
        return minecraft.player.blockPosition().getX() + " "
                + minecraft.player.blockPosition().getY() + " "
                + minecraft.player.blockPosition().getZ();
    }

    private static Component copyableLiteral(String display, String raw, String hoverText) {
        return Component.literal(display).withStyle(COPY_STYLE
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, raw))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText + ": " + raw))));
    }

    private static String resolveLiteral(String raw, Minecraft minecraft) {
        return PlaceholderResolver.resolve(raw, minecraft)
                .replace(currentDimensionRaw(minecraft), currentDimensionDisplay(minecraft));
    }

    private static boolean containsCoordinates(String template) {
        return template.contains("{x}") && template.contains("{y}") && template.contains("{z}");
    }

    private static int findNextCoordinateToken(String template, int fromIndex) {
        int first = template.indexOf("{x}", fromIndex);
        if (first < 0) {
            return -1;
        }

        int tokenLength = coordinateTokenLength(template, first);
        return tokenLength > 0 ? first : -1;
    }

    private static int coordinateTokenLength(String template, int startIndex) {
        String[] variants = {
                "{x}, {y}, {z}",
                "{x},{y},{z}",
                "{x} {y} {z}"
        };
        for (String variant : variants) {
            if (template.startsWith(variant, startIndex)) {
                return variant.length();
            }
        }
        return 0;
    }

    private static int minPositive(int first, int second) {
        if (first < 0) {
            return second;
        }
        if (second < 0) {
            return first;
        }
        return Math.min(first, second);
    }

    private static String formatDimensionId(String raw) {
        return switch (raw) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "The End";
            default -> titleCase(raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw);
        };
    }

    private static String titleCase(String value) {
        String[] words = value.replace('/', ' ').replace('_', ' ').replace('-', ' ').trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) {
                builder.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.isEmpty() ? value : builder.toString();
    }

    public record CopyValue(String display, String raw, String label) {
    }
}
