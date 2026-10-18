package de.artemis.omniwheel.client.tutorial;

import java.util.List;

/**
 * Content order and presentation targets for the two read-only guides.
 */
public final class TutorialGuide {

    public static final int REVISION = 3;

    public enum Focus {

        ALL,
        PROFILES,
        ENTRIES,
        CREATE,
        APPEARANCE,
        COLORS,
        BROWSER,
        IMPORTS,
        ACTION,
        DATA,
        SHORTCUTS,
        PREVIEW
    }

    public enum Scene {

        EDITOR,
        COLORS,
        ICONS,
        CHAT,
        COMMANDS,
        DATA,
        KEYBINDS,
        PREVIEW,
        SUBMENU
    }

    public final static class Step {

        private final String id;

        private final Focus focus;

        private final Scene scene;

        public String titleKey(TutorialType type) {
            return "omniwheel.tutorial." + type.name().toLowerCase(java.util.Locale.ROOT) + "." + id + ".title";
        }

        public String bodyKey(TutorialType type) {
            return "omniwheel.tutorial." + type.name().toLowerCase(java.util.Locale.ROOT) + "." + id + ".body";
        }

        public Step(String id, Focus focus, Scene scene) {
            this.id = id;
            this.focus = focus;
            this.scene = scene;
        }

        public String id() {
            return id;
        }

        public Focus focus() {
            return focus;
        }

        public Scene scene() {
            return scene;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value)
                return true;
            if (value == null || value.getClass() != getClass())
                return false;
            Step other = (Step) value;
            return true && java.util.Objects.equals(id, other.id) && java.util.Objects.equals(focus, other.focus) && java.util.Objects.equals(scene, other.scene);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, focus, scene);
        }

        @Override
        public String toString() {
            return "Step[" + "id=" + id + ", focus=" + focus + ", scene=" + scene + "]";
        }
    }

    public static final List<Step> MANAGER = de.artemis.omniwheel.common.Java8.list(new Step("overview", Focus.ALL, Scene.EDITOR), new Step("profiles", Focus.PROFILES, Scene.EDITOR), new Step("entries", Focus.ENTRIES, Scene.EDITOR), new Step("create", Focus.CREATE, Scene.EDITOR), new Step("appearance", Focus.APPEARANCE, Scene.EDITOR), new Step("colors", Focus.COLORS, Scene.COLORS), new Step("icons", Focus.BROWSER, Scene.ICONS), new Step("custom_icons", Focus.IMPORTS, Scene.ICONS), new Step("actions", Focus.ACTION, Scene.CHAT), new Step("commands", Focus.ACTION, Scene.COMMANDS), new Step("data", Focus.DATA, Scene.DATA), new Step("keybinds", Focus.ACTION, Scene.KEYBINDS), new Step("shortcuts", Focus.SHORTCUTS, Scene.EDITOR), new Step("preview", Focus.PREVIEW, Scene.PREVIEW), new Step("moving", Focus.PREVIEW, Scene.SUBMENU), new Step("finish", Focus.ALL, Scene.EDITOR));

    public static final List<Step> RADIAL = de.artemis.omniwheel.common.Java8.list(new Step("opening", Focus.ALL, Scene.EDITOR), new Step("selection", Focus.ALL, Scene.EDITOR), new Step("submenus", Focus.ENTRIES, Scene.EDITOR), new Step("center", Focus.ACTION, Scene.EDITOR), new Step("pages", Focus.ALL, Scene.EDITOR), new Step("keyboard", Focus.ALL, Scene.EDITOR), new Step("management", Focus.ALL, Scene.EDITOR));

    private TutorialGuide() {
    }

    public static List<Step> steps(TutorialType type) {
        if (type == TutorialType.MANAGER) {
            return MANAGER;
        } else {
            return RADIAL;
        }
    }

    public static Step step(TutorialType type, int index) {
        java.util.List<de.artemis.omniwheel.client.tutorial.TutorialGuide.Step> steps = steps(type);
        return steps.get(de.artemis.omniwheel.common.Numbers.clamp(index, 0, steps.size() - 1));
    }
}
