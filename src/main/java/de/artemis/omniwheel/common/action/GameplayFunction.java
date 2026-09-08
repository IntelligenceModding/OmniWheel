package de.artemis.omniwheel.common.action;

import de.artemis.omniwheel.common.OmniWheelText;

import java.util.Arrays;
import java.util.Locale;

@SuppressWarnings("unused")
public enum GameplayFunction {
    OMNI_BACK(FunctionCategory.OMNIWHEEL, FunctionMode.TRIGGER),
    OMNI_OPEN_PROFILES(FunctionCategory.OMNIWHEEL, FunctionMode.TRIGGER),
    OMNI_OPEN_MANAGER(FunctionCategory.OMNIWHEEL, FunctionMode.TRIGGER),
    OMNI_NEXT_PROFILE(FunctionCategory.OMNIWHEEL, FunctionMode.TRIGGER),
    OMNI_PREVIOUS_PROFILE(FunctionCategory.OMNIWHEEL, FunctionMode.TRIGGER),
    OPEN_INVENTORY(FunctionCategory.SCREEN, FunctionMode.TRIGGER),
    OPEN_CHAT(FunctionCategory.SCREEN, FunctionMode.TRIGGER),
    OPEN_COMMAND_CHAT(FunctionCategory.SCREEN, FunctionMode.TRIGGER),
    OPEN_ADVANCEMENTS(FunctionCategory.SCREEN, FunctionMode.TRIGGER),
    OPEN_SOCIAL_INTERACTIONS(FunctionCategory.SCREEN, FunctionMode.TRIGGER),
    ATTACK(FunctionCategory.INTERACTION, FunctionMode.TRIGGER),
    USE_ITEM(FunctionCategory.INTERACTION, FunctionMode.TRIGGER),
    PICK_BLOCK(FunctionCategory.INTERACTION, FunctionMode.TRIGGER),
    DROP_ITEM(FunctionCategory.INTERACTION, FunctionMode.TRIGGER),
    SWAP_OFFHAND(FunctionCategory.INTERACTION, FunctionMode.TRIGGER),
    JUMP(FunctionCategory.MOVEMENT, FunctionMode.TRIGGER),
    HOTBAR_SLOT_1(FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_2(FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_3(FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_4(FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_5(FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_6(FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_7(FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_8(FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_SLOT_9(FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_NEXT(FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    HOTBAR_PREVIOUS(FunctionCategory.HOTBAR, FunctionMode.TRIGGER),
    TOGGLE_PERSPECTIVE(FunctionCategory.VIEW, FunctionMode.TRIGGER),
    TAKE_SCREENSHOT(FunctionCategory.VIEW, FunctionMode.TRIGGER),
    TOGGLE_FULLSCREEN(FunctionCategory.VIEW, FunctionMode.TRIGGER),
    TOGGLE_SMOOTH_CAMERA(FunctionCategory.VIEW, FunctionMode.TRIGGER),
    TOGGLE_SNEAK_LOCK(FunctionCategory.MOVEMENT, FunctionMode.TOGGLE),
    TOGGLE_SPRINT_LOCK(FunctionCategory.MOVEMENT, FunctionMode.TOGGLE),
    TOGGLE_AUTO_WALK(FunctionCategory.MOVEMENT, FunctionMode.TOGGLE);

    private final FunctionCategory category;
    private final FunctionMode mode;
    private final String id;
    private final String translationKey;
    private final String descriptionKey;

    GameplayFunction(FunctionCategory category, FunctionMode mode) {
        this.category = category;
        this.mode = mode;
        this.id = name().toLowerCase(Locale.ROOT);
        this.translationKey = "omniwheel.function." + id;
        this.descriptionKey = translationKey + ".description";
    }

    public String displayName() {
        return OmniWheelText.translate(translationKey);
    }

    public String description() {
        return OmniWheelText.translate(descriptionKey);
    }

    public String translationKey() {
        return translationKey;
    }

    public String descriptionKey() {
        return descriptionKey;
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
        OMNIWHEEL,
        SCREEN,
        INTERACTION,
        MOVEMENT,
        HOTBAR,
        VIEW;

        private final String translationKey;

        FunctionCategory() {
            this.translationKey = "omniwheel.function.category." + name().toLowerCase(Locale.ROOT);
        }

        public String label() {
            return OmniWheelText.translate(translationKey);
        }
    }

    public enum FunctionMode {
        TRIGGER,
        TOGGLE
    }
}
