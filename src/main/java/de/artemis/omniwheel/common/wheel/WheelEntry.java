package de.artemis.omniwheel.common.wheel;

import de.artemis.omniwheel.common.action.WheelAction;
import java.util.Objects;

public final class WheelEntry {

    private final String id;

    private final String label;

    private final String description;

    private final String glyph;

    private final int color;

    private final int guiColor;

    private final WheelAction action;

    private final boolean active;

    private final boolean showIcon;

    private final boolean showLabel;

    private final boolean showShortcut;

    private final String shortcut;

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

    private static String requireText(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }

    public WheelEntry(String id, String label, String description, String glyph, int color, int guiColor, WheelAction action, boolean active, boolean showIcon, boolean showLabel, boolean showShortcut, String shortcut) {
        id = requireText(id, "id");
        label = requireText(label, "label");
        description = Objects.requireNonNull(description, "description").trim();
        glyph = Objects.requireNonNull(glyph, "glyph").trim();
        if ((color >>> 24) == 0) {
            color |= 0xFF000000;
        }
        if ((guiColor >>> 24) == 0) {
            guiColor |= 0xFF000000;
        }
        action = Objects.requireNonNull(action, "action");
        shortcut = Objects.requireNonNull(shortcut, "shortcut").trim();
        this.id = id;
        this.label = label;
        this.description = description;
        this.glyph = glyph;
        this.color = color;
        this.guiColor = guiColor;
        this.action = action;
        this.active = active;
        this.showIcon = showIcon;
        this.showLabel = showLabel;
        this.showShortcut = showShortcut;
        this.shortcut = shortcut;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public String glyph() {
        return glyph;
    }

    public int color() {
        return color;
    }

    public int guiColor() {
        return guiColor;
    }

    public WheelAction action() {
        return action;
    }

    public boolean active() {
        return active;
    }

    public boolean showIcon() {
        return showIcon;
    }

    public boolean showLabel() {
        return showLabel;
    }

    public boolean showShortcut() {
        return showShortcut;
    }

    public String shortcut() {
        return shortcut;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        WheelEntry other = (WheelEntry) value;
        return true && java.util.Objects.equals(id, other.id) && java.util.Objects.equals(label, other.label) && java.util.Objects.equals(description, other.description) && java.util.Objects.equals(glyph, other.glyph) && java.util.Objects.equals(color, other.color) && java.util.Objects.equals(guiColor, other.guiColor) && java.util.Objects.equals(action, other.action) && java.util.Objects.equals(active, other.active) && java.util.Objects.equals(showIcon, other.showIcon) && java.util.Objects.equals(showLabel, other.showLabel) && java.util.Objects.equals(showShortcut, other.showShortcut) && java.util.Objects.equals(shortcut, other.shortcut);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, label, description, glyph, color, guiColor, action, active, showIcon, showLabel, showShortcut, shortcut);
    }

    @Override
    public String toString() {
        return "WheelEntry[" + "id=" + id + ", label=" + label + ", description=" + description + ", glyph=" + glyph + ", color=" + color + ", guiColor=" + guiColor + ", action=" + action + ", active=" + active + ", showIcon=" + showIcon + ", showLabel=" + showLabel + ", showShortcut=" + showShortcut + ", shortcut=" + shortcut + "]";
    }
}
