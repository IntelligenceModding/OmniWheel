package de.artemis.omniwheel.common.action;

import java.util.Arrays;
import java.util.Locale;

@SuppressWarnings("unused")
public enum GameplayFunction {
    OMNI_BACK("Back", "Go back one radial level, or close at the root.", FunctionCategory.OMNIWHEEL, FunctionMode.TRIGGER),
    OMNI_OPEN_PROFILES("Profiles", "Open the available profile list inside the radial.", FunctionCategory.OMNIWHEEL, FunctionMode.TRIGGER),
    OMNI_OPEN_MANAGER("Manage", "Open the OmniWheel profile manager.", FunctionCategory.OMNIWHEEL, FunctionMode.TRIGGER),
    OMNI_NEXT_PROFILE("Next Profile", "Switch to the next profile.", FunctionCategory.OMNIWHEEL, FunctionMode.TRIGGER),
    OMNI_PREVIOUS_PROFILE("Previous Profile", "Switch to the previous profile.", FunctionCategory.OMNIWHEEL, FunctionMode.TRIGGER),
    OPEN_INVENTORY("Open Inventory", "Open the player inventory.", FunctionCategory.SCREEN, FunctionMode.TRIGGER),
    OPEN_CHAT("Open Chat", "Open the normal chat input.", FunctionCategory.SCREEN, FunctionMode.TRIGGER),
    OPEN_COMMAND_CHAT("Open Command Chat", "Open chat prefilled with '/'.", FunctionCategory.SCREEN, FunctionMode.TRIGGER),
    OPEN_ADVANCEMENTS("Open Advancements", "Open the advancements screen.", FunctionCategory.SCREEN, FunctionMode.TRIGGER),
    OPEN_SOCIAL_INTERACTIONS("Open Social Interactions", "Open the social interactions screen.", FunctionCategory.SCREEN, FunctionMode.TRIGGER),
    ATTACK("Attack", "Perform one attack click.", FunctionCategory.INTERACTION, FunctionMode.TRIGGER),
    USE_ITEM("Use Item", "Perform one use-item click.", FunctionCategory.INTERACTION, FunctionMode.TRIGGER),
    PICK_BLOCK("Pick Block", "Perform one pick-block action.", FunctionCategory.INTERACTION, FunctionMode.TRIGGER),
    DROP_ITEM("Drop Item", "Drop the currently held item.", FunctionCategory.INTERACTION, FunctionMode.TRIGGER),
    SWAP_OFFHAND("Swap Offhand", "Swap the main-hand and offhand items.", FunctionCategory.INTERACTION, FunctionMode.TRIGGER),
    JUMP("Jump", "Perform one jump input.", FunctionCategory.MOVEMENT, FunctionMode.TRIGGER),
    HOTBAR_SLOT_1("Hotbar Slot 1", "Select hotbar slot 1.", FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_2("Hotbar Slot 2", "Select hotbar slot 2.", FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_3("Hotbar Slot 3", "Select hotbar slot 3.", FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_4("Hotbar Slot 4", "Select hotbar slot 4.", FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_5("Hotbar Slot 5", "Select hotbar slot 5.", FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_6("Hotbar Slot 6", "Select hotbar slot 6.", FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_7("Hotbar Slot 7", "Select hotbar slot 7.", FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_8("Hotbar Slot 8", "Select hotbar slot 8.", FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_9("Hotbar Slot 9", "Select hotbar slot 9.", FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_NEXT("Next Hotbar Slot", "Move to the next hotbar slot.", FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_PREVIOUS("Previous Hotbar Slot", "Move to the previous hotbar slot.", FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    TOGGLE_PERSPECTIVE("Toggle Perspective", "Switch between first- and third-person view.", FunctionCategory.VIEW, FunctionMode.TRIGGER),
    TAKE_SCREENSHOT("Take Screenshot", "Take a screenshot.", FunctionCategory.VIEW, FunctionMode.TRIGGER),
    TOGGLE_FULLSCREEN("Toggle Fullscreen", "Toggle fullscreen mode.", FunctionCategory.VIEW, FunctionMode.TRIGGER),
    TOGGLE_SMOOTH_CAMERA("Toggle Smooth Camera", "Toggle smooth camera mode.", FunctionCategory.VIEW, FunctionMode.TRIGGER),
    TOGGLE_SNEAK_LOCK("Toggle Sneak Lock", "Keep sneaking until toggled off.", FunctionCategory.MOVEMENT, FunctionMode.TOGGLE),
    TOGGLE_SPRINT_LOCK("Toggle Sprint Lock", "Keep sprinting until toggled off.", FunctionCategory.MOVEMENT, FunctionMode.TOGGLE),
    TOGGLE_AUTO_WALK("Toggle Auto Walk", "Keep moving forward until toggled off.", FunctionCategory.MOVEMENT, FunctionMode.TOGGLE);

    private final String displayName;
    private final String description;
    private final FunctionCategory category;
    private final FunctionMode mode;
    private final String id;

    GameplayFunction(String displayName, String description, FunctionCategory category, FunctionMode mode) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.mode = mode;
        this.id = name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public FunctionCategory category() {
        return category;
    }

    public FunctionMode mode() {
        return mode;
    }

    public String id() {
        return id;
    }

    public boolean isToggle() {
        return mode == FunctionMode.TOGGLE;
    }

    public static GameplayFunction byId(String id) {
        if (id == null || id.isBlank()) {
            return OPEN_INVENTORY;
        }
        return Arrays.stream(values())
                .filter(function -> function.id.equalsIgnoreCase(id) || function.name().equalsIgnoreCase(id))
                .findFirst()
                .orElse(OPEN_INVENTORY);
    }

    public enum FunctionCategory {
        OMNIWHEEL("OmniWheel"),
        SCREEN("Screen"),
        INTERACTION("Interaction"),
        MOVEMENT("Movement"),
        HOTBAR("Hotbar"),
        VIEW("View");

        private final String label;

        FunctionCategory(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum FunctionMode {
        TRIGGER,
        TOGGLE
    }
}
