package de.artemis.omniwheel.client.tutorial;

import java.util.List;

/** Content order and presentation targets for the two read-only guides. */
public final class TutorialGuide {
    public static final int REVISION = 3;
    public enum Focus { ALL, PROFILES, ENTRIES, CREATE, APPEARANCE, COLORS, BROWSER, IMPORTS, ACTION, DATA, SHORTCUTS, PREVIEW }
    public enum Scene { EDITOR, COLORS, ICONS, CHAT, COMMANDS, DATA, KEYBINDS, PREVIEW, SUBMENU }
    public record Step(String id, Focus focus, Scene scene) {
        public String titleKey(TutorialType type) { return "omniwheel.tutorial." + type.name().toLowerCase(java.util.Locale.ROOT) + "." + id + ".title"; }
        public String bodyKey(TutorialType type) { return "omniwheel.tutorial." + type.name().toLowerCase(java.util.Locale.ROOT) + "." + id + ".body"; }
    }
    public static final List<Step> MANAGER = List.of(
        new Step("overview", Focus.ALL, Scene.EDITOR),
        new Step("profiles", Focus.PROFILES, Scene.EDITOR),
        new Step("entries", Focus.ENTRIES, Scene.EDITOR),
        new Step("create", Focus.CREATE, Scene.EDITOR),
        new Step("appearance", Focus.APPEARANCE, Scene.EDITOR),
        new Step("colors", Focus.COLORS, Scene.COLORS),
        new Step("icons", Focus.BROWSER, Scene.ICONS),
        new Step("custom_icons", Focus.IMPORTS, Scene.ICONS),
        new Step("actions", Focus.ACTION, Scene.CHAT),
        new Step("commands", Focus.ACTION, Scene.COMMANDS),
        new Step("data", Focus.DATA, Scene.DATA),
        new Step("keybinds", Focus.ACTION, Scene.KEYBINDS),
        new Step("shortcuts", Focus.SHORTCUTS, Scene.EDITOR),
        new Step("preview", Focus.PREVIEW, Scene.PREVIEW),
        new Step("moving", Focus.PREVIEW, Scene.SUBMENU),
        new Step("finish", Focus.ALL, Scene.EDITOR)
    );
    public static final List<Step> RADIAL = List.of(
        new Step("opening", Focus.ALL, Scene.EDITOR),
        new Step("selection", Focus.ALL, Scene.EDITOR),
        new Step("submenus", Focus.ENTRIES, Scene.EDITOR),
        new Step("center", Focus.ACTION, Scene.EDITOR),
        new Step("pages", Focus.ALL, Scene.EDITOR),
        new Step("keyboard", Focus.ALL, Scene.EDITOR),
        new Step("management", Focus.ALL, Scene.EDITOR)
    );
    private TutorialGuide() {}
    public static List<Step> steps(TutorialType type) { return type == TutorialType.MANAGER ? MANAGER : RADIAL; }
    public static Step step(TutorialType type, int index) {
        var steps = steps(type);
        return steps.get(Math.clamp(index, 0, steps.size() - 1));
    }
}
