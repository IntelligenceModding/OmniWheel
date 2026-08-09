package de.artemis.omniwheel.common.wheel;

import de.artemis.omniwheel.common.action.WheelAction;

import java.util.Objects;

public record WheelEntry(String id, String label, String description, String glyph, int color, int guiColor, WheelAction action, boolean active, boolean showIcon, boolean showLabel, boolean showShortcut, String shortcut) {
    public static final int DEFAULT_COLOR = 0xFFD9E0E7;
    public static final int DEFAULT_GUI_COLOR = 0xD8212831;

    public WheelEntry(String id, String label, String description, String glyph, WheelAction action) {
        this(id, label, description, glyph, DEFAULT_COLOR, DEFAULT_GUI_COLOR, action, true, true, true, true, "");
    }

    public WheelEntry(String id, String label, String description, String glyph, int color, WheelAction action) {
        this(id, label, description, glyph, color, DEFAULT_GUI_COLOR, action, true, true, true, true, "");
    }

    public WheelEntry(String id, String label, String description, String glyph, int color, WheelAction action, boolean active) {
        this(id, label, description, glyph, color, DEFAULT_GUI_COLOR, action, active, true, true, true, "");
    }

    public WheelEntry(String id, String label, String description, String glyph, int color, WheelAction action, boolean active, boolean showIcon, boolean showLabel) {
        this(id, label, description, glyph, color, DEFAULT_GUI_COLOR, action, active, showIcon, showLabel, true, "");
    }

    public WheelEntry(String id, String label, String description, String glyph, int color, WheelAction action, boolean active, boolean showLabel, boolean showShortcut, String shortcut) {
        this(id, label, description, glyph, color, DEFAULT_GUI_COLOR, action, active, true, showLabel, showShortcut, shortcut);
    }

    public WheelEntry(String id, String label, String description, String glyph, int color, int guiColor, WheelAction action, boolean active, boolean showIcon, boolean showLabel) {
        this(id, label, description, glyph, color, guiColor, action, active, showIcon, showLabel, true, "");
    }

    public WheelEntry {
        id = requireText(id, "id");
        label = requireText(label, "label");
        description = Objects.requireNonNull(description, "description").trim();
        glyph = requireText(glyph, "glyph");
        if ((color >>> 24) == 0) {
            color |= 0xFF000000;
        }
        if ((guiColor >>> 24) == 0) {
            guiColor |= 0xFF000000;
        }
        action = Objects.requireNonNull(action, "action");
        shortcut = Objects.requireNonNull(shortcut, "shortcut").trim();
    }

    private static String requireText(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }
}
