package de.artemis.omniwheel.client.overlay;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.client.render.EntryIconRenderer;
import de.artemis.omniwheel.client.runtime.EntryShortcutMatcher;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import de.artemis.omniwheel.client.tutorial.TutorialType;
import de.artemis.omniwheel.common.action.ChatAction;
import de.artemis.omniwheel.common.action.CommandAction;
import de.artemis.omniwheel.common.action.CopyTextAction;
import de.artemis.omniwheel.common.action.FunctionAction;
import de.artemis.omniwheel.common.action.GameplayFunction;
import de.artemis.omniwheel.common.action.LocalMessageAction;
import de.artemis.omniwheel.common.action.OpenChatAction;
import de.artemis.omniwheel.common.action.OpenScreenAction;
import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.action.ScreenTarget;
import de.artemis.omniwheel.common.action.WheelAction;
import de.artemis.omniwheel.common.OmniWheelText;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.function.IntConsumer;

public final class OmniWheelProfilesOverlay {
    private static final String COMMAND_VALUE_SEPARATOR = " ;; ";
    private static final int PANEL = 0xF0181F28;
    private static final int PANEL_EDGE = 0xFF2C4B63;
    private static final int PANEL_EDGE_ACTIVE = 0xFF4F7793;
    private static final int ROW = 0xE6232C36;
    private static final int ROW_SELECTED = 0xF03B5369;
    private static final int ROW_DISABLED = 0xB01A222B;
    private static final int TEXT = 0xFFF1F4F8;
    private static final int TEXT_SECONDARY = 0xFF95A4B2;
    private static final int WARNING = 0xFFF0B36A;
    private static final int ACTION = 0xFF8BDAF8;
    private static final int ACTION_DISABLED = 0xFF53606C;
    private static final int FIELD = 0xFF111820;
    private static final int FIELD_EDGE = 0xFF314252;
    private static final int FIELD_FOCUSED = 0xFF8BDAF8;
    private static final int PROFILE_ROW_HEIGHT = 28;
    private static final int PROFILE_ROW_STEP = 36;
    private static final int LIST_SCROLL_STEP = 36;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 6;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 18;
    private static final int ICON_PICKER_CELL_SIZE = 22;
    private static final int ICON_PICKER_CELL_GAP = 5;
    private static final int ICON_PICKER_CONTENT_PADDING = 8;
    private static final int MANAGER_TUTORIAL_STEP_COUNT = 7;
    private static final float PROFILE_DRAG_THRESHOLD = 4.0F;
    private static final long KEY_REPEAT_DELAY_MS = 400L;
    private static final long KEY_REPEAT_INTERVAL_MS = 35L;
    private static final int REFERENCE_UI_WIDTH = 960;
    private static final int REFERENCE_UI_HEIGHT = 540;
    private static final int[] ENTRY_NAME_COLORS = {
            WheelEntry.DEFAULT_COLOR,
            0xFFFFD36A,
            0xFF7FE3FF,
            0xFF8EE89A,
            0xFFFF9AB5,
            0xFFD1A0FF,
            0xFFFFB36A,
            0xFFA7B6C6
    };
    private static final int[] ENTRY_GUI_COLORS = {
            WheelEntry.DEFAULT_GUI_COLOR,
            0xFFFFD36A,
            0xFF7FE3FF,
            0xFF8EE89A,
            0xFFFF9AB5,
            0xFFD1A0FF,
            0xFFFFB36A,
            0xFFA7B6C6
    };
    private static final int[] POLLED_KEYS = createPolledKeys();
    private static Map<ResourceLocation, String> CREATIVE_TAB_SEARCH_TERMS = Map.of();
    private static boolean CREATIVE_TAB_SEARCH_TERMS_LOADED;

    private final OmniWheelClientRuntime runtime;
    private final List<ClickAction> actions = new ArrayList<>();
    private final List<TooltipRegion> tooltipRegions = new ArrayList<>();
    private final Map<EditorField, TextFieldState> textFields = createTextFields();
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Map<Integer, Long> nextKeyRepeatAt = new HashMap<>();
    private final Set<String> expandedWheelIds = new HashSet<>();
    private NavigationList activeList = NavigationList.PROFILES;
    private EditBox commandInput;
    private CommandSuggestions commandSuggestions;
    private Screen commandSuggestionsScreen;
    private boolean syncingCommandInput;
    private int commandSuggestionsYOffset;

    private boolean open;
    private boolean mouseReleased;
    private boolean leftMouseDown;
    private boolean escapeDown;
    private boolean screenHosted;
    private boolean textEntryActive;
    private double lastFreeRawMouseX;
    private double lastFreeRawMouseY;
    private EditorField focusedField;
    private int focusedCommandFieldIndex = -1;
    private int focusedActionIndex = -1;
    private int entriesScrollOffset;
    private int profilesScrollOffset;
    private int wheelsScrollOffset;
    private int iconPickerScrollOffset;
    private int functionListScrollOffset;
    private int entriesAreaX;
    private int entriesAreaY;
    private int entriesAreaWidth;
    private int entriesAreaHeight;
    private int profilesPanelX;
    private int profilesPanelY;
    private int profilesPanelWidth;
    private int profilesPanelHeight;
    private int profilesAreaX;
    private int profilesAreaY;
    private int profilesAreaWidth;
    private int profilesAreaHeight;
    private int profilesActionY;
    private int profilesListWidth;
    private int profilesScrollbarX;
    private int profilesScrollbarY;
    private int profilesScrollbarHeight;
    private int profilesScrollbarThumbY;
    private int profilesScrollbarThumbHeight;
    private int wheelsAreaX;
    private int wheelsAreaY;
    private int wheelsAreaWidth;
    private int wheelsAreaHeight;
    private int wheelsPanelX;
    private int wheelsPanelY;
    private int wheelsPanelWidth;
    private int wheelsPanelHeight;
    private int wheelsActionY;
    private int wheelsListWidth;
    private int wheelsScrollbarX;
    private int wheelsScrollbarY;
    private int wheelsScrollbarHeight;
    private int wheelsScrollbarThumbY;
    private int wheelsScrollbarThumbHeight;
    private int iconPickerListWidth;
    private int iconPickerScrollbarX;
    private int iconPickerScrollbarY;
    private int iconPickerScrollbarHeight;
    private int iconPickerScrollbarThumbY;
    private int iconPickerScrollbarThumbHeight;
    private int iconPickerX;
    private int iconPickerY;
    private int iconPickerWidth;
    private int iconPickerHeight;
    private int iconPickerBackButtonX;
    private int iconPickerBackButtonY;
    private int iconPickerBackButtonWidth;
    private int iconPickerBackButtonHeight;
    private int editorPanelX;
    private int editorPanelY;
    private int editorPanelWidth;
    private int editorPanelHeight;
    private int iconButtonX;
    private int iconButtonY;
    private int iconButtonWidth;
    private int iconButtonHeight;
    private int iconToggleButtonX;
    private int iconToggleButtonY;
    private int iconToggleButtonWidth;
    private int iconToggleButtonHeight;
    private int labelToggleButtonX;
    private int labelToggleButtonY;
    private int labelToggleButtonWidth;
    private int labelToggleButtonHeight;
    private int shortcutToggleButtonX;
    private int shortcutToggleButtonY;
    private int shortcutToggleButtonWidth;
    private int shortcutToggleButtonHeight;
    private int shortcutButtonX;
    private int shortcutButtonY;
    private int shortcutButtonWidth;
    private int shortcutButtonHeight;
    private int addCommandButtonX;
    private int addCommandButtonY;
    private int addCommandButtonWidth;
    private int addCommandButtonHeight;
    private int nameColorPaletteX;
    private int nameColorPaletteY;
    private int nameColorPaletteWidth;
    private int nameColorPaletteHeight;
    private int iconPickerGridX;
    private int iconPickerGridY;
    private int iconPickerGridWidth;
    private int iconPickerGridHeight;
    private int functionListX;
    private int functionListY;
    private int functionListWidth;
    private int functionListHeight;
    private int functionListScrollbarX;
    private int functionListScrollbarY;
    private int functionListScrollbarHeight;
    private int functionListScrollbarThumbY;
    private int functionListScrollbarThumbHeight;
    private int commandListX;
    private int commandListY;
    private int commandListWidth;
    private int commandListHeight;
    private int commandListScrollOffset;
    private int commandListScrollbarX;
    private int commandListScrollbarY;
    private int commandListScrollbarHeight;
    private int commandListScrollbarThumbY;
    private int commandListScrollbarThumbHeight;
    private int managerTutorialStepIndex = -1;
    private int tutorialNextButtonX;
    private int tutorialNextButtonY;
    private int tutorialNextButtonWidth;
    private int tutorialNextButtonHeight;
    private int tutorialSkipAllButtonX;
    private int tutorialSkipAllButtonY;
    private int tutorialSkipAllButtonWidth;
    private int tutorialSkipAllButtonHeight;
    private boolean draggingProfilesScrollbar;
    private boolean draggingWheelsScrollbar;
    private boolean draggingIconPickerScrollbar;
    private boolean draggingFunctionListScrollbar;
    private boolean draggingCommandListScrollbar;
    private int profilesScrollbarDragOffset;
    private int wheelsScrollbarDragOffset;
    private int iconPickerScrollbarDragOffset;
    private int functionListScrollbarDragOffset;
    private int commandListScrollbarDragOffset;

    private String selectedProfileId;
    private String selectedWheelId;
    private String selectedEntryId;
    private String renamingProfileId;
    private String renamingWheelId;
    private String draggedProfileId;
    private int draggedProfileStartIndex = -1;
    private int draggedProfileTargetIndex = -1;
    private float draggedProfileMouseStartX;
    private float draggedProfileMouseStartY;
    private float draggedProfileAnchorOffsetY;
    private float draggedProfileVisualX;
    private float draggedProfileVisualY;
    private boolean draggedProfileActive;
    private String draggedWheelId;
    private String draggedWheelOwnerWheelId;
    private String draggedWheelSubmenuTargetId;
    private boolean draggedWheelWasSelected;
    private int draggedWheelStartIndex = -1;
    private int draggedWheelTargetIndex = -1;
    private float draggedWheelMouseStartX;
    private float draggedWheelMouseStartY;
    private float draggedWheelAnchorOffsetY;
    private float draggedWheelVisualX;
    private float draggedWheelVisualY;
    private boolean draggedWheelActive;

    private String wheelTitleDraft = "";
    private String profileNameDraft = "";

    private String entryLabelDraft = "";
    private String entryDescriptionDraft = "";
    private String entryShortcutDraft = "";
    private String entryGlyphDraft = "";
    private String entryValueDraft = "";
    private String iconPickerSearchDraft = "";
    private String pendingIconPickerFocusId;
    private final List<String> commandValueDrafts = new ArrayList<>();
    private final List<TextFieldState> commandValueFields = new ArrayList<>();
    private int entryColorDraft = WheelEntry.DEFAULT_COLOR;
    private int entryGuiColorDraft = WheelEntry.DEFAULT_GUI_COLOR;
    private boolean entryShowIconDraft = true;
    private boolean entryShowLabelDraft = true;
    private boolean entryShowShortcutDraft = true;
    private DraftActionType entryActionTypeDraft = DraftActionType.COMMAND;
    private GameplayFunction entryFunctionDraft = GameplayFunction.OPEN_INVENTORY;
    private String entrySubmenuWheelId;
    private ScreenTarget entryScreenTargetDraft = ScreenTarget.PROFILE_MANAGER;
    private boolean iconPickerOpen;
    private List<IconPickerOption> iconPickerOptions = List.of();

    public OmniWheelProfilesOverlay(OmniWheelClientRuntime runtime) {
        this.runtime = runtime;
    }

    public void open() {
        Minecraft minecraft = Minecraft.getInstance();
        runtime.profileManager().ensureLoaded();
        selectedProfileId = runtime.activeProfile().id();
        syncSelection(false);
        open = true;
        screenHosted = false;
        leftMouseDown = minecraft.mouseHandler.isLeftPressed();
        escapeDown = isEscapeDown(minecraft);
        lastFreeRawMouseX = minecraft.mouseHandler.xpos();
        lastFreeRawMouseY = minecraft.mouseHandler.ypos();
        pressedKeys.clear();
        nextKeyRepeatAt.clear();
        clearProfileDrag();
        clearWheelDrag();
        activeList = NavigationList.PROFILES;
        textEntryActive = false;
        expandedWheelIds.clear();
        suppressGameplayInput(minecraft);
        ensureMouseReleased(minecraft);
    }

    public void close() {
        if (screenHosted) {
            closeForScreen();
            return;
        }

        if (!open && !mouseReleased) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        open = false;
        actions.clear();
        tooltipRegions.clear();
        leftMouseDown = false;
        escapeDown = false;
        clearFocus();
        clearProfileDrag();
        clearWheelDrag();
        closeIconPicker();
        pressedKeys.clear();
        nextKeyRepeatAt.clear();
        restoreGameplayInput(minecraft);

        if (mouseReleased) {
            mouseReleased = false;
            if (minecraft.screen == null && minecraft.player != null && minecraft.level != null) {
                minecraft.mouseHandler.grabMouse();
            }
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void openForScreen() {
        runtime.profileManager().ensureLoaded();
        selectedProfileId = runtime.activeProfile().id();
        syncSelection(true);
        open = true;
        screenHosted = true;
        mouseReleased = false;
        leftMouseDown = false;
        escapeDown = false;
        pressedKeys.clear();
        nextKeyRepeatAt.clear();
        clearProfileDrag();
        clearWheelDrag();
        closeIconPicker();
        clearCommandInput();
        activeList = NavigationList.PROFILES;
        textEntryActive = false;
        expandedWheelIds.clear();
        if (!runtime.tutorialProgressStore().isCompleted(TutorialType.MANAGER)) {
            managerTutorialStepIndex = runtime.tutorialProgressStore().currentStep(TutorialType.MANAGER);
        }
    }

    public void closeForScreen() {
        persistPendingEdits();
        open = false;
        screenHosted = false;
        actions.clear();
        tooltipRegions.clear();
        clearFocus();
        clearProfileDrag();
        clearWheelDrag();
        closeIconPicker();
        clearCommandInput();
        pressedKeys.clear();
        nextKeyRepeatAt.clear();
        mouseReleased = false;
        leftMouseDown = false;
        escapeDown = false;
        textEntryActive = false;
        managerTutorialStepIndex = -1;
    }

    public void onProfilesReloaded() {
        if (!open) {
            return;
        }
        commitProfileRename();
        commitWheelRename();
        clearProfileDrag();
        clearWheelDrag();
        closeIconPicker();
        syncSelection(true);
    }

    public void tick() {
        if (!open) {
            return;
        }

        if (screenHosted) {
            syncSelection(false);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            close();
            return;
        }

        ensureMouseReleased(minecraft);
        suppressGameplayInput(minecraft);
        syncSelection(false);

        boolean leftPressed = minecraft.mouseHandler.isLeftPressed();
        boolean escapePressed = isEscapeDown(minecraft);

        if (escapePressed && !escapeDown) {
            if (iconPickerOpen) {
                closeIconPicker();
                escapeDown = true;
                return;
            }
            if (isRenamingProfile()) {
                commitProfileRename();
                escapeDown = true;
                return;
            }
            close();
            return;
        }

        updateProfileDrag(currentMouseX(minecraft), currentMouseY(minecraft), leftPressed);
        if (leftPressed && !leftMouseDown) {
            handleMouseClick(currentMouseX(minecraft), currentMouseY(minecraft));
        }
        handleKeyboardInput(minecraft);

        lastFreeRawMouseX = minecraft.mouseHandler.xpos();
        lastFreeRawMouseY = minecraft.mouseHandler.ypos();
        leftMouseDown = leftPressed;
        escapeDown = escapePressed;
    }

    public void render(GuiGraphics graphics) {
        if (!open) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        syncSelection(false);
        actions.clear();
        tooltipRegions.clear();

        int width = uiWidth(minecraft);
        int height = uiHeight(minecraft);
        float renderScale = (float) uiScale(minecraft);
        graphics.pose().pushPose();
        graphics.pose().scale(renderScale, renderScale, 1.0F);

        int leftX = 18;
        int topY = 18;
        int panelHeight = height - 36;
        int sidePanelWidth = 176;
        int profileWidth = sidePanelWidth;
        int wheelWidth = sidePanelWidth;
        int wheelX = leftX + profileWidth + 12;
        int editorX = wheelX + wheelWidth + 12;
        int editorWidth = width - editorX - 18;
        profilesPanelX = leftX;
        profilesPanelY = topY;
        profilesPanelWidth = profileWidth;
        profilesPanelHeight = panelHeight;
        wheelsPanelX = wheelX;
        wheelsPanelY = topY;
        wheelsPanelWidth = wheelWidth;
        wheelsPanelHeight = panelHeight;
        editorPanelX = editorX;
        editorPanelY = topY;
        editorPanelWidth = editorWidth;
        editorPanelHeight = panelHeight;

        boolean tutorialActive = isManagerTutorialActive();
        boolean showProfilesPanel = !tutorialActive || managerTutorialStepIndex == 0;
        boolean showEntriesPanel = !tutorialActive || managerTutorialStepIndex == 1;
        boolean showEditorPanel = !tutorialActive || managerTutorialStepIndex >= 2;

        if (showProfilesPanel) {
            drawPanel(graphics, leftX, topY, profileWidth, panelHeight, !tutorialActive && activeList == NavigationList.PROFILES);
            graphics.drawString(minecraft.font, text("omniwheel.manager.profiles"), leftX + 12, topY + 10, TEXT, false);
            drawProfilesPanel(graphics, minecraft, leftX + 10, topY + 30, profileWidth - 20, panelHeight - 40);
        }
        if (showEntriesPanel) {
            drawPanel(graphics, wheelX, topY, wheelWidth, panelHeight, !tutorialActive && activeList == NavigationList.WHEELS);
            graphics.drawString(minecraft.font, text("omniwheel.manager.entries"), wheelX + 12, topY + 10, TEXT, false);
            drawWheelsPanel(graphics, minecraft, wheelX + 10, topY + 30, wheelWidth - 20, panelHeight - 40);
        }
        if (showEditorPanel) {
            drawPanel(graphics, editorX, topY, editorWidth, panelHeight, !tutorialActive && activeList == NavigationList.EDITOR);
            graphics.drawString(minecraft.font, text(iconPickerOpen ? "omniwheel.manager.icon_browser" : "omniwheel.manager.editor"), editorX + 12, topY + 10, TEXT, false);
            drawEditorPanel(graphics, minecraft, editorX + 12, topY + 30, editorWidth - 24, panelHeight - 40);
        }
        if (!tutorialActive && !iconPickerOpen && textEntryActive && focusedCommandFieldIndex >= 0 && commandSuggestions != null) {
            boolean renderWrappedUsage = shouldRenderWrappedCommandUsage();
            alignCommandUsageToField();
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, commandSuggestionsYOffset, 200.0F);
            if (!renderWrappedUsage) {
                commandSuggestions.render(
                        graphics,
                        (int) currentMouseX(minecraft),
                        (int) currentMouseY(minecraft) - commandSuggestionsYOffset
                );
            }
            graphics.pose().popPose();
            if (renderWrappedUsage) {
                renderWrappedCommandUsage(graphics, minecraft);
            }
        }
        drawEditorButtonTooltip(graphics, minecraft);
        drawManagerTutorial(graphics, minecraft);
        graphics.pose().popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open || button != 0) {
            return false;
        }
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        if (handleManagerTutorialClick((float) mouseX, (float) mouseY)) {
            leftMouseDown = true;
            return true;
        }
        if (!iconPickerOpen && handleCommandSuggestionsClick(mouseX, mouseY, button)) {
            leftMouseDown = true;
            return true;
        }
        TextFieldState renameField = textFields.get(EditorField.PROFILE_NAME);
        if (isRenamingProfile() && (renameField == null || !renameField.contains(mouseX, mouseY))) {
            commitProfileRename();
        }
        TextFieldState wheelRenameField = textFields.get(EditorField.WHEEL_TITLE);
        if (isRenamingWheel() && (wheelRenameField == null || !wheelRenameField.contains(mouseX, mouseY))) {
            commitWheelRename();
        }
        boolean handled = handleMouseClick((float) mouseX, (float) mouseY);
        if (handled) {
            leftMouseDown = true;
        }
        return handled;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!open || button != 0) {
            return false;
        }
        if (isManagerTutorialActive()) {
            leftMouseDown = false;
            return true;
        }
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        if (iconPickerOpen) {
            draggingIconPickerScrollbar = false;
            iconPickerScrollbarDragOffset = 0;
            leftMouseDown = false;
            return true;
        }
        if (draggingProfilesScrollbar || draggingWheelsScrollbar || draggingIconPickerScrollbar || draggingFunctionListScrollbar || draggingCommandListScrollbar) {
            draggingProfilesScrollbar = false;
            draggingWheelsScrollbar = false;
            draggingIconPickerScrollbar = false;
            draggingFunctionListScrollbar = false;
            draggingCommandListScrollbar = false;
            leftMouseDown = false;
            return true;
        }
        if (draggedProfileId != null) {
            finishProfileDrag();
            leftMouseDown = false;
            return true;
        }
        if (draggedWheelId != null) {
            finishWheelDrag();
            leftMouseDown = false;
            return true;
        }
        leftMouseDown = false;
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!open || button != 0) {
            return false;
        }
        if (isManagerTutorialActive()) {
            leftMouseDown = true;
            return true;
        }
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        if (iconPickerOpen) {
            if (draggingIconPickerScrollbar) {
                updateIconPickerScrollbarDrag((float) mouseY);
            }
            leftMouseDown = true;
            return true;
        }
        if (draggingProfilesScrollbar) {
            updateProfilesScrollbarDrag((float) mouseY);
            leftMouseDown = true;
            return true;
        }
        if (draggingWheelsScrollbar) {
            updateWheelsScrollbarDrag((float) mouseY);
            leftMouseDown = true;
            return true;
        }
        if (draggingIconPickerScrollbar) {
            updateIconPickerScrollbarDrag((float) mouseY);
            leftMouseDown = true;
            return true;
        }
        if (draggingFunctionListScrollbar) {
            updateFunctionListScrollbarDrag((float) mouseY);
            leftMouseDown = true;
            return true;
        }
        if (draggingCommandListScrollbar) {
            updateCommandListScrollbarDrag((float) mouseY);
            leftMouseDown = true;
            return true;
        }
        if (draggedProfileId != null) {
            updateProfileDrag((float) mouseX, (float) mouseY, true);
            leftMouseDown = true;
            return true;
        }
        if (draggedWheelId != null) {
            updateWheelDrag((float) mouseX, (float) mouseY, true);
            leftMouseDown = true;
            return true;
        }
        return false;
    }

    private boolean handleMouseClick(float mouseX, float mouseY) {
        if (iconPickerOpen) {
            if (tryStartIconPickerScrollbarDrag(mouseX, mouseY)) {
                return true;
            }
            handleIconPickerClick(mouseX, mouseY);
            return true;
        }
        boolean insideMainPanel = updateActiveListFromMouse(mouseX, mouseY);
        if (tryStartProfilesScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        if (tryStartWheelsScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        if (tryStartFunctionListScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        if (tryStartCommandListScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        if (tryStartProfileDrag(mouseX, mouseY)) {
            return true;
        }
        if (tryStartWheelDrag(mouseX, mouseY)) {
            return true;
        }
        if (clickFocusedTextField(mouseX, mouseY)) {
            return true;
        }

        clearFocus();
        for (ClickAction action : actions) {
            if (action.enabled && action.contains(mouseX, mouseY)) {
                activeList = navigationListForAction(action);
                action.handler.run();
                syncSelection(false);
                return true;
            }
        }
        if (insideMainPanel) {
            anchorActivePanelSelection();
        }
        return insideMainPanel;
    }

    private boolean updateActiveListFromMouse(float mouseX, float mouseY) {
        if (iconPickerOpen) {
            activeList = NavigationList.EDITOR;
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int width = uiWidth(minecraft);
        int height = uiHeight(minecraft);
        int leftX = 18;
        int topY = 18;
        int panelHeight = height - 36;
        int sidePanelWidth = 176;
        int profileWidth = sidePanelWidth;
        int wheelWidth = sidePanelWidth;
        int wheelX = leftX + profileWidth + 12;
        int editorX = wheelX + wheelWidth + 12;
        int editorWidth = width - editorX - 18;

        if (contains(mouseX, mouseY, leftX, topY, profileWidth, panelHeight)) {
            activeList = NavigationList.PROFILES;
            return true;
        }
        if (contains(mouseX, mouseY, wheelX, topY, wheelWidth, panelHeight)) {
            activeList = NavigationList.WHEELS;
            return true;
        }
        if (contains(mouseX, mouseY, editorX, topY, editorWidth, panelHeight)) {
            activeList = NavigationList.EDITOR;
            return true;
        }
        return false;
    }

    private void anchorActivePanelSelection() {
        switch (activeList) {
            case PROFILES -> {
                if (selectedProfileId == null) {
                    selectProfileByIndex(0);
                }
            }
            case WHEELS -> {
                List<EntryListRow> rows = visibleEntryRows();
                if (rows.isEmpty()) {
                    focusFirstActionInZone(NavigationList.WHEELS);
                } else if (selectedEntryRowIndex(rows) < 0) {
                    selectEntryRowByIndex(0);
                }
            }
            case EDITOR -> {
                if (currentFocusTarget() == null) {
                    focusFirstVisibleTarget();
                }
            }
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open) {
            return false;
        }
        if (isManagerTutorialActive()) {
            return true;
        }
        if (shouldSuppressDirectionalNavigation(keyCode)) {
            return true;
        }
        normalizeFocusedActionIndex();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (iconPickerOpen) {
                closeIconPicker();
                return true;
            }
            if (currentFocusTarget() != null) {
                clearFocus();
                return true;
            }
        }
        if (iconPickerOpen && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeIconPicker();
            return true;
        }
        if (iconPickerOpen) {
            if (isDirectionalNavigationKey(keyCode) && handleIconPickerDirectionalKey(keyCode)) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                cycleFocus((modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                return true;
            }

            TextFieldState field = currentFocusedField();
            if (field == null) {
                return handleActionOrListKeyPressed(keyCode);
            }
            if (!textEntryActive) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                    activateTextEntry();
                    return true;
                }
                if (isDirectionalNavigationKey(keyCode) && navigateEditorFocus(keyCode)) {
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_UP) {
                    cycleFocus(true);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_DOWN) {
                    cycleFocus(false);
                    return true;
                }
                return false;
            }

            if (isControlModifierDown()) {
                if (keyCode == GLFW.GLFW_KEY_A) {
                    field.selectAll();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_V) {
                    field.insertText(clipboardValue());
                    applyFocusedFieldDraft();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_C) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(field.copyText());
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_X) {
                    String copied = field.copyText();
                    if (!copied.isEmpty()) {
                        Minecraft.getInstance().keyboardHandler.setClipboard(copied);
                        field.deleteSelection();
                        applyFocusedFieldDraft();
                    }
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_LEFT) {
                    field.moveCursorByWord(-1, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                    field.moveCursorByWord(1, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    field.deleteWordBackward();
                    applyFocusedFieldDraft();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_DELETE) {
                    field.deleteWordForward();
                    applyFocusedFieldDraft();
                    return true;
                }
            }

            return handleEditorNavigationKey(keyCode);
        }
        if (isRenamingProfile() && (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            commitProfileRename();
            return true;
        }
        if (isRenamingWheel() && (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            commitWheelRename();
            return true;
        }
        if (handleCommandFieldKeyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            cycleFocus((modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
            return true;
        }

        TextFieldState field = currentFocusedField();
        if (field == null) {
            if (handleActionOrListKeyPressed(keyCode)) {
                return true;
            }
            return false;
        }
        if (!textEntryActive) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                activateTextEntry();
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) && clearHighlightedShortcutField()) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT && shouldStayOnEditorFieldOnRight()) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT && handlePanelSwitchKeyPressed(keyCode)) {
                return true;
            }
            if (isDirectionalNavigationKey(keyCode) && navigateEditorFocus(keyCode)) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                cycleFocus(true);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                cycleFocus(false);
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) && handlePanelSwitchKeyPressed(keyCode)) {
                return true;
            }
            return false;
        }

        if (focusedField == EditorField.ENTRY_SHORTCUT) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                textFields.get(EditorField.ENTRY_SHORTCUT).setValue("");
                entryShortcutDraft = "";
                autosaveCurrentEntryDraft();
                return true;
            }
            String capturedShortcut = EntryShortcutMatcher.captureShortcutText(
                    resolveWindowHandleStatic(Minecraft.getInstance().getWindow()),
                    keyCode,
                    modifiers
            );
            if (capturedShortcut != null) {
                textFields.get(EditorField.ENTRY_SHORTCUT).setValue(capturedShortcut);
                entryShortcutDraft = capturedShortcut;
                autosaveCurrentEntryDraft();
                return true;
            }
        }

        if (isControlModifierDown()) {
            if (keyCode == GLFW.GLFW_KEY_A) {
                field.selectAll();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_V) {
                field.insertText(clipboardValue());
                applyFocusedFieldDraft();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_C) {
                Minecraft.getInstance().keyboardHandler.setClipboard(field.copyText());
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_X) {
                String copied = field.copyText();
                if (!copied.isEmpty()) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(copied);
                    field.deleteSelection();
                    applyFocusedFieldDraft();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                field.moveCursorByWord(-1, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                field.moveCursorByWord(1, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                field.deleteWordBackward();
                applyFocusedFieldDraft();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                field.deleteWordForward();
                applyFocusedFieldDraft();
                return true;
            }
        }

        return handleEditorNavigationKey(keyCode);
    }

    private boolean handleActionOrListKeyPressed(int keyCode) {
        if (iconPickerOpen && handleIconPickerKeyPressed(keyCode)) {
            return true;
        }
        if (handleCommandListKeyPressed(keyCode)) {
            return true;
        }
        if (handleFunctionListKeyPressed(keyCode)) {
            return true;
        }
        if (isControlModifierDown() && (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN)) {
            return handleMoveShortcut(keyCode);
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            return handleDeleteShortcut();
        }
        if (activeList == NavigationList.EDITOR
                && focusedField == null
                && focusedCommandFieldIndex < 0
                && focusedActionIndex < 0) {
            if (isDirectionalNavigationKey(keyCode)) {
                if (focusFirstVisibleTarget()) {
                    return true;
                }
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                return focusFirstVisibleTarget();
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                List<FocusTarget> targets = visibleFocusTargets();
                if (!targets.isEmpty()) {
                    focusTarget(targets.getLast());
                    return true;
                }
            }
        }
        if (focusedActionIndex >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                return runFocusedAction();
            }
            if (isPagingNavigationKey(keyCode) && handleFocusedActionPaging(keyCode)) {
                return true;
            }
            if (isDirectionalNavigationKey(keyCode) && navigateActionFocus(keyCode)) {
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) && handlePanelSwitchKeyPressed(keyCode)) {
                return true;
            }
        }
        if ((keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) && handlePanelSwitchKeyPressed(keyCode)) {
            return true;
        }
        if (handleListNavigationKey(keyCode)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            return activateCurrentSelection();
        }
        return false;
    }

    private boolean handleMoveShortcut(int keyCode) {
        int delta = keyCode == GLFW.GLFW_KEY_UP ? -1 : 1;
        return switch (activeList) {
            case PROFILES -> moveSelectedProfileBy(delta);
            case WHEELS -> moveSelectedEntryBy(delta);
            case EDITOR -> false;
        };
    }

    private boolean handlePanelSwitchKeyPressed(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (activeList == NavigationList.EDITOR) {
                clearFocus();
                activeList = NavigationList.WHEELS;
                return true;
            }
            if (activeList == NavigationList.WHEELS) {
                clearFocus();
                activeList = NavigationList.PROFILES;
                return true;
            }
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (activeList == NavigationList.PROFILES) {
                clearFocus();
                activeList = NavigationList.WHEELS;
                if (selectedEntry() == null && !selectEntryRowByIndex(0)) {
                    focusFirstActionInZone(NavigationList.WHEELS);
                }
                return true;
            }
            if (activeList == NavigationList.WHEELS) {
                activeList = NavigationList.EDITOR;
                return focusFirstVisibleTarget() || true;
            }
        }
        return false;
    }

    private boolean handleIconPickerKeyPressed(int keyCode) {
        if (focusedActionIndex >= actions.size()) {
            focusedActionIndex = -1;
        }
        if (focusedActionIndex >= 0 && !isActionInIconPicker(actions.get(focusedActionIndex))) {
            return false;
        }

        int pageStep = Math.max(1, Math.max(1, iconPickerGridHeight - (ICON_PICKER_CONTENT_PADDING * 2)));
        return switch (keyCode) {
            case GLFW.GLFW_KEY_PAGE_UP -> scrollIconPickerBy(-pageStep);
            case GLFW.GLFW_KEY_PAGE_DOWN -> scrollIconPickerBy(pageStep);
            case GLFW.GLFW_KEY_HOME -> scrollIconPickerTo(0);
            case GLFW.GLFW_KEY_END -> scrollIconPickerTo(maxIconPickerScrollOffset());
            default -> false;
        };
    }

    private boolean handleFunctionListKeyPressed(int keyCode) {
        if (entryActionTypeDraft != DraftActionType.FUNCTION) {
            return false;
        }
        if (focusedActionIndex >= actions.size()) {
            focusedActionIndex = -1;
        }
        if (focusedActionIndex >= 0 && !isActionInFunctionList(actions.get(focusedActionIndex))) {
            return false;
        }

        int pageStep = Math.max(1, Math.max(1, functionListHeight - 12));
        return switch (keyCode) {
            case GLFW.GLFW_KEY_PAGE_UP -> scrollFunctionListBy(-pageStep);
            case GLFW.GLFW_KEY_PAGE_DOWN -> scrollFunctionListBy(pageStep);
            case GLFW.GLFW_KEY_HOME -> scrollFunctionListTo(0);
            case GLFW.GLFW_KEY_END -> scrollFunctionListTo(maxFunctionListScrollOffset());
            default -> false;
        };
    }

    private boolean handleCommandListKeyPressed(int keyCode) {
        if (entryActionTypeDraft != DraftActionType.COMMAND) {
            return false;
        }
        if (focusedActionIndex >= actions.size()) {
            focusedActionIndex = -1;
        }
        if (focusedActionIndex >= 0 && !isActionInCommandList(actions.get(focusedActionIndex))) {
            return false;
        }
        if (focusedCommandFieldIndex < 0 && focusedActionIndex < 0) {
            return false;
        }

        int pageStep = Math.max(1, Math.max(1, commandListHeight - 12));
        return switch (keyCode) {
            case GLFW.GLFW_KEY_PAGE_UP -> scrollCommandListBy(-pageStep);
            case GLFW.GLFW_KEY_PAGE_DOWN -> scrollCommandListBy(pageStep);
            case GLFW.GLFW_KEY_HOME -> scrollCommandListTo(0);
            case GLFW.GLFW_KEY_END -> scrollCommandListTo(maxCommandListScrollOffset());
            default -> false;
        };
    }

    private boolean handleDeleteShortcut() {
        if (iconPickerOpen) {
            return false;
        }
        if (activeList == NavigationList.PROFILES) {
            if (canDeleteSelectedProfile()) {
                WheelProfile remainingProfile = runtime.profileManager().deleteProfile(selectedProfileId);
                selectedProfileId = remainingProfile.id();
                selectedWheelId = remainingProfile.rootWheelId();
                selectedEntryId = null;
                syncSelection(true);
                return true;
            }
            return false;
        }
        if (canDeleteSelection()) {
            deleteSelection();
            return true;
        }
        return false;
    }

    private void normalizeFocusedActionIndex() {
        if (focusedActionIndex >= actions.size()) {
            focusedActionIndex = -1;
        }
    }

    private boolean activateCurrentSelection() {
        if (activeList == NavigationList.PROFILES) {
            if (selectedProfileId != null && !selectedProfileId.equals(runtime.activeProfile().id())) {
                runtime.profileManager().setActiveProfile(selectedProfileId);
                return true;
            }
            return false;
        }

        if (activeList == NavigationList.EDITOR) {
            List<FocusTarget> targets = visibleFocusTargets();
            if (!targets.isEmpty()) {
                focusTarget(targets.getFirst());
                return true;
            }
            return false;
        }

        WheelEntry entry = selectedEntry();
        if (entry == null) {
            List<FocusTarget> targets = visibleFocusTargets();
            if (!targets.isEmpty()) {
                focusTarget(targets.getFirst());
                return true;
            }
            return false;
        }

        if (entry.action() instanceof OpenWheelAction openWheelAction) {
            toggleExpandedSubmenu(selectedWheelId, entry, openWheelAction.wheelId());
            return true;
        }

        activeList = NavigationList.EDITOR;
        List<FocusTarget> targets = visibleFocusTargets();
        if (!targets.isEmpty()) {
            focusTarget(targets.getFirst());
            return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!open) {
            return false;
        }
        if (isManagerTutorialActive()) {
            return true;
        }
        if (!textEntryActive) {
            return false;
        }
        if (focusedField == EditorField.ENTRY_SHORTCUT) {
            return true;
        }
        if (iconPickerOpen) {
            TextFieldState field = currentFocusedField();
            if (field == null || Character.isISOControl(codePoint)) {
                return false;
            }
            field.insertText(Character.toString(codePoint));
            applyFocusedFieldDraft();
            return true;
        }
        if (handleCommandFieldCharTyped(codePoint, modifiers)) {
            return true;
        }
        TextFieldState field = currentFocusedField();
        if (field == null || Character.isISOControl(codePoint)) {
            return false;
        }
        field.insertText(Character.toString(codePoint));
        applyFocusedFieldDraft();
        return true;
    }


    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!open || scrollY == 0.0D) {
            return false;
        }
        if (isManagerTutorialActive()) {
            return true;
        }
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        if (iconPickerOpen) {
            if (mouseX >= iconPickerGridX
                    && mouseX <= iconPickerGridX + iconPickerGridWidth
                    && mouseY >= iconPickerGridY
                    && mouseY <= iconPickerGridY + iconPickerGridHeight) {
                return scrollIconPickerBy(-((int) Math.signum(scrollY) * LIST_SCROLL_STEP)) || true;
            }
            return mouseX >= iconPickerX
                    && mouseX <= iconPickerX + iconPickerWidth
                    && mouseY >= iconPickerY
                    && mouseY <= iconPickerY + iconPickerHeight;
        }
        if (handleCommandSuggestionsScroll(scrollY)) {
            return true;
        }
        if (entryActionTypeDraft == DraftActionType.COMMAND
                && mouseX >= commandListX
                && mouseX <= commandListX + commandListWidth
                && mouseY >= commandListY
                && mouseY <= commandListY + commandListHeight) {
            activeList = NavigationList.EDITOR;
            commandListScrollOffset = clamp(
                    commandListScrollOffset - ((int) Math.signum(scrollY) * LIST_SCROLL_STEP),
                    0,
                    maxCommandListScrollOffset()
            );
            ensureFocusedActionStillVisible();
            return true;
        }
        if (entryActionTypeDraft == DraftActionType.FUNCTION
                && mouseX >= functionListX
                && mouseX <= functionListX + functionListWidth
                && mouseY >= functionListY
                && mouseY <= functionListY + functionListHeight) {
            activeList = NavigationList.EDITOR;
            functionListScrollOffset = clamp(
                    functionListScrollOffset - ((int) Math.signum(scrollY) * LIST_SCROLL_STEP),
                    0,
                    maxFunctionListScrollOffset()
            );
            return true;
        }
        if (mouseX >= profilesAreaX && mouseX <= profilesAreaX + profilesAreaWidth && mouseY >= profilesAreaY && mouseY <= profilesAreaY + profilesAreaHeight) {
            activeList = NavigationList.PROFILES;
            profilesScrollOffset = clamp(profilesScrollOffset - ((int) Math.signum(scrollY) * LIST_SCROLL_STEP), 0, maxProfilesScrollOffset());
            return true;
        }
        if (mouseX >= wheelsAreaX && mouseX <= wheelsAreaX + wheelsAreaWidth && mouseY >= wheelsAreaY && mouseY <= wheelsAreaY + wheelsAreaHeight) {
            activeList = NavigationList.WHEELS;
            wheelsScrollOffset = clamp(wheelsScrollOffset - ((int) Math.signum(scrollY) * LIST_SCROLL_STEP), 0, maxWheelsScrollOffset());
            return true;
        }
        if (mouseX < entriesAreaX || mouseX > entriesAreaX + entriesAreaWidth || mouseY < entriesAreaY || mouseY > entriesAreaY + entriesAreaHeight) {
            return false;
        }
        activeList = NavigationList.EDITOR;
        entriesScrollOffset = Math.max(0, entriesScrollOffset - ((int) Math.signum(scrollY) * 50));
        return true;
    }

    private void drawProfilesPanel(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        List<WheelProfile> profiles = runtime.profileManager().getProfiles();
        int actionY = y + height - 114;
        int contentHeight = listContentHeight(profiles.size());
        boolean needsScrollbar = contentHeight > Math.max(0, actionY - y - 8);
        profilesAreaX = x;
        profilesAreaY = y;
        profilesAreaWidth = width;
        profilesAreaHeight = Math.max(0, actionY - y - 8);
        profilesActionY = actionY;
        profilesListWidth = width - (needsScrollbar ? (SCROLLBAR_WIDTH + SCROLLBAR_GAP) : 0);
        profilesScrollOffset = clamp(profilesScrollOffset, 0, Math.max(0, contentHeight - profilesAreaHeight));
        updateProfilesScrollbarBounds(contentHeight);
        textFields.get(EditorField.PROFILE_NAME).visible = false;

        enableUiScissor(graphics, x, y, x + profilesListWidth, y + profilesAreaHeight);
        int visibleRowIndex = 0;
        for (WheelProfile profile : profiles) {
            if (draggedProfileActive && profile.id().equals(draggedProfileId)) {
                continue;
            }

            int displayIndex = visibleRowIndex;
            if (draggedProfileActive && visibleRowIndex >= draggedProfileTargetIndex) {
                displayIndex++;
            }

            int rowY = profileRowY(displayIndex);
            if (rowY + PROFILE_ROW_HEIGHT >= y && rowY <= y + profilesAreaHeight) {
                drawProfileRow(graphics, minecraft, profile, x, rowY, profilesListWidth, false);
            }
            visibleRowIndex++;
        }
        graphics.disableScissor();

        if (draggedProfileActive) {
            drawProfileDropIndicator(graphics, x, profileRowY(draggedProfileTargetIndex), profilesListWidth);
            WheelProfile draggedProfile = draggedProfile();
            if (draggedProfile != null) {
                drawProfileRow(
                        graphics,
                        minecraft,
                        draggedProfile,
                        Math.round(draggedProfileVisualX),
                        Math.round(draggedProfileVisualY),
                        profilesListWidth,
                        true
                );
            }
        }
        drawProfilesScrollbar(graphics, needsScrollbar);

        addButton(graphics, minecraft, x, actionY, width, text("omniwheel.manager.new_profile"), true, () -> {
            persistPendingEdits();
            WheelProfile createdProfile = runtime.profileManager().createProfile();
            selectedProfileId = createdProfile.id();
            selectedWheelId = createdProfile.rootWheelId();
            selectedEntryId = null;
            syncSelection(true);
        });
        addButton(graphics, minecraft, x, actionY + 30, width, text("omniwheel.common.rename"), canRenameSelectedProfile(), this::renameSelectedProfile);
        addButton(graphics, minecraft, x, actionY + 60, width, text("omniwheel.common.delete"), canDeleteSelectedProfile(), () -> {
            WheelProfile remainingProfile = runtime.profileManager().deleteProfile(selectedProfileId);
            selectedProfileId = remainingProfile.id();
            selectedWheelId = remainingProfile.rootWheelId();
            selectedEntryId = null;
            syncSelection(true);
        });
        boolean canActivate = selectedProfileId != null && !selectedProfileId.equals(runtime.activeProfile().id());
        addButton(graphics, minecraft, x, actionY + 90, width, text("omniwheel.common.activate"), canActivate, () -> runtime.profileManager().setActiveProfile(selectedProfileId));
    }

    private void drawProfileRow(GuiGraphics graphics, Minecraft minecraft, WheelProfile profile, int x, int y, int width, boolean dragged) {
        boolean selected = profile.id().equals(selectedProfileId);
        boolean activeProfile = profile.id().equals(runtime.activeProfile().id());
        int rowColor = dragged ? 0xF43A4553 : selected ? ROW_SELECTED : ROW;

        if (dragged) {
            graphics.fill(x + 3, y + 4, x + width + 3, y + PROFILE_ROW_HEIGHT + 4, 0x58101820);
            graphics.fill(x - 1, y - 1, x + width + 1, y + PROFILE_ROW_HEIGHT + 1, 0x90384E62);
        }

        graphics.fill(x, y, x + width, y + PROFILE_ROW_HEIGHT, rowColor);
        if (dragged || selected) {
            graphics.fill(x, y, x + width, y + 1, ACTION);
            graphics.fill(x, y + PROFILE_ROW_HEIGHT - 1, x + width, y + PROFILE_ROW_HEIGHT, ACTION);
            graphics.fill(x, y, x + 1, y + PROFILE_ROW_HEIGHT, ACTION);
            graphics.fill(x + width - 1, y, x + width, y + PROFILE_ROW_HEIGHT, ACTION);
        }

        String stateLabel = text(activeProfile ? "omniwheel.manager.profile.active" : selected ? "omniwheel.manager.profile.selected" : "omniwheel.manager.profile.select");
        int stateColor = activeProfile || selected ? ACTION : TEXT_SECONDARY;
        int stateWidth = minecraft.font.width(stateLabel);
        int stateX = x + width - stateWidth - 10;
        int textWidth = Math.max(36, stateX - x - 18);
        boolean renaming = isRenamingProfile(profile.id()) && !dragged;

        if (renaming) {
            drawInlineProfileNameField(graphics, minecraft, x + 8, y + 4, textWidth, 14);
        } else {
            graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, displayText(profile.displayName()), textWidth), x + 10, y + 7, TEXT, false);
        }
        graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, profile.id(), textWidth), x + 10, y + 17, TEXT_SECONDARY, false);
        graphics.drawString(minecraft.font, stateLabel, stateX, y + 11, stateColor, false);
    }

    private void drawProfileDropIndicator(GuiGraphics graphics, int x, int y, int width) {
        int indicatorY = clamp(y - 4, profilesAreaY - 2, profilesActionY - 6);
        graphics.fill(x + 8, indicatorY, x + width - 8, indicatorY + 2, ACTION);
        graphics.fill(x + 8, indicatorY - 3, x + 16, indicatorY + 5, ACTION);
        graphics.fill(x + width - 16, indicatorY - 3, x + width - 8, indicatorY + 5, ACTION);
    }

    private void drawProfilesScrollbar(GuiGraphics graphics, boolean visible) {
        if (!visible) {
            profilesScrollbarX = 0;
            profilesScrollbarY = 0;
            profilesScrollbarHeight = 0;
            profilesScrollbarThumbY = 0;
            profilesScrollbarThumbHeight = 0;
            return;
        }
        graphics.fill(profilesScrollbarX, profilesScrollbarY, profilesScrollbarX + SCROLLBAR_WIDTH, profilesScrollbarY + profilesScrollbarHeight, 0x70101820);
        int thumbColor = draggingProfilesScrollbar ? ACTION : TEXT_SECONDARY;
        graphics.fill(profilesScrollbarX, profilesScrollbarThumbY, profilesScrollbarX + SCROLLBAR_WIDTH, profilesScrollbarThumbY + profilesScrollbarThumbHeight, thumbColor);
    }

    private void drawWheelRow(GuiGraphics graphics, Minecraft minecraft, WheelProfile profile, WheelDefinition wheel, int x, int y, int width, boolean dragged) {
        boolean selected = wheel.id().equals(selectedWheelId);
        boolean root = wheel.id().equals(profile.rootWheelId());
        boolean active = wheel.active();
        int rowColor = dragged ? 0xF43A4553 : selected ? (active ? ROW_SELECTED : 0xC8323E4C) : active ? ROW : ROW_DISABLED;

        if (dragged) {
            graphics.fill(x + 3, y + 4, x + width + 3, y + PROFILE_ROW_HEIGHT + 4, 0x58101820);
            graphics.fill(x - 1, y - 1, x + width + 1, y + PROFILE_ROW_HEIGHT + 1, 0x90384E62);
        }

        graphics.fill(x, y, x + width, y + PROFILE_ROW_HEIGHT, rowColor);
        if (dragged || selected) {
            int accentColor = active ? ACTION : ACTION_DISABLED;
            graphics.fill(x, y, x + width, y + 1, accentColor);
            graphics.fill(x, y + PROFILE_ROW_HEIGHT - 1, x + width, y + PROFILE_ROW_HEIGHT, accentColor);
            graphics.fill(x, y, x + 1, y + PROFILE_ROW_HEIGHT, accentColor);
            graphics.fill(x + width - 1, y, x + width, y + PROFILE_ROW_HEIGHT, accentColor);
        }

        String metaCount = text("omniwheel.manager.wheel_meta", wheel.entries().size(), pageCountForEntries(wheel.entries().size()));
        int titleColor = active ? TEXT : TEXT_SECONDARY;
        int secondaryColor = active ? TEXT_SECONDARY : ACTION_DISABLED;
        int metaColor = active ? (selected ? ACTION : TEXT_SECONDARY) : ACTION_DISABLED;
        int metaWidth = minecraft.font.width(metaCount);
        int metaX = x + width - metaWidth - 10;
        int textWidth = Math.max(36, metaX - x - 18);
        boolean renaming = isRenamingWheel(wheel.id()) && !dragged;

        if (renaming) {
            drawInlineWheelNameField(graphics, minecraft, x + 8, y + 4, textWidth, 14);
        } else {
            graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, displayText(wheel.title()), textWidth), x + 10, y + 7, titleColor, false);
        }
        graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, root ? text("omniwheel.manager.root") : wheel.id(), textWidth), x + 10, y + 17, secondaryColor, false);
        graphics.drawString(minecraft.font, metaCount, metaX, y + 11, metaColor, false);
    }

    private void drawWheelDropIndicator(GuiGraphics graphics, int x, int y, int width) {
        int indicatorY = clamp(y - 4, wheelsAreaY - 2, wheelsActionY - 6);
        graphics.fill(x + 8, indicatorY, x + width - 8, indicatorY + 2, ACTION);
        graphics.fill(x + 8, indicatorY - 3, x + 16, indicatorY + 5, ACTION);
        graphics.fill(x + width - 16, indicatorY - 3, x + width - 8, indicatorY + 5, ACTION);
    }

    private void drawEntryListRow(GuiGraphics graphics, Minecraft minecraft, EntryListRow row, int x, int y, int width, boolean dragged) {
        WheelEntry entry = row.entry();
        boolean selected = isEntryRowSelected(row.ownerWheelId(), entry.id());
        int rowColor = dragged ? 0xF43A4553 : !entry.active() ? ROW_DISABLED : selected ? ROW_SELECTED : ROW;
        int indent = row.depth() * 14;
        if (dragged) {
            graphics.fill(x + 3, y + 4, x + width + 3, y + PROFILE_ROW_HEIGHT + 4, 0x58101820);
            graphics.fill(x - 1, y - 1, x + width + 1, y + PROFILE_ROW_HEIGHT + 1, 0x90384E62);
        }
        graphics.fill(x, y, x + width, y + PROFILE_ROW_HEIGHT, rowColor);
        if (dragged || selected) {
            int accentColor = entry.active() ? ACTION : ACTION_DISABLED;
            graphics.fill(x, y, x + width, y + 1, accentColor);
            graphics.fill(x, y + PROFILE_ROW_HEIGHT - 1, x + width, y + PROFILE_ROW_HEIGHT, accentColor);
            graphics.fill(x, y, x + 1, y + PROFILE_ROW_HEIGHT, accentColor);
            graphics.fill(x + width - 1, y, x + width, y + PROFILE_ROW_HEIGHT, accentColor);
        }

        int arrowOffset = 0;
        if (row.expandable()) {
            graphics.drawString(minecraft.font, row.expanded() ? "v" : ">", x + 8 + indent, y + 10, ACTION, false);
            arrowOffset = 10;
        }

        int glyphX = x + 10 + indent + arrowOffset;
        int iconSlotWidth = 22;
        int iconCenterX = glyphX + (iconSlotWidth / 2);
        int iconCenterY = y + (PROFILE_ROW_HEIGHT / 2);
        int glyphColor = !entry.active() ? ACTION_DISABLED : entry.action() instanceof OpenWheelAction ? ACTION : entry.color();
        if (!EntryIconRenderer.drawIcon(graphics, minecraft, entry.glyph(), iconCenterX, iconCenterY, 0.9F)) {
            graphics.drawCenteredString(minecraft.font, clampPlainTextIcon(entry.glyph()), iconCenterX, y + 10, glyphColor);
        }

        int typeWidth = minecraft.font.width(entryTypeLabel(entry));
        int textStartX = glyphX + iconSlotWidth + 6;
        int textWidth = Math.max(48, x + width - textStartX - typeWidth - 14);
        int labelColor = entry.active() ? entry.color() : ACTION_DISABLED;
        int summaryColor = entry.active() ? TEXT_SECONDARY : ACTION_DISABLED;
        graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, displayText(entry.label()), textWidth), textStartX, y + 7, labelColor, false);
        graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, actionSummary(entry.action()), textWidth), textStartX, y + 17, summaryColor, false);
        graphics.drawString(minecraft.font, entryTypeLabel(entry), x + width - typeWidth - 8, y + 11, !entry.active() ? ACTION_DISABLED : selected ? ACTION : TEXT_SECONDARY, false);
    }

    private void drawWheelsScrollbar(GuiGraphics graphics, boolean visible) {
        if (!visible) {
            wheelsScrollbarX = 0;
            wheelsScrollbarY = 0;
            wheelsScrollbarHeight = 0;
            wheelsScrollbarThumbY = 0;
            wheelsScrollbarThumbHeight = 0;
            return;
        }
        graphics.fill(wheelsScrollbarX, wheelsScrollbarY, wheelsScrollbarX + SCROLLBAR_WIDTH, wheelsScrollbarY + wheelsScrollbarHeight, 0x70101820);
        int thumbColor = draggingWheelsScrollbar ? ACTION : TEXT_SECONDARY;
        graphics.fill(wheelsScrollbarX, wheelsScrollbarThumbY, wheelsScrollbarX + SCROLLBAR_WIDTH, wheelsScrollbarThumbY + wheelsScrollbarThumbHeight, thumbColor);
    }

    private void drawFunctionListScrollbar(GuiGraphics graphics, boolean visible) {
        if (!visible) {
            functionListScrollbarX = 0;
            functionListScrollbarY = 0;
            functionListScrollbarHeight = 0;
            functionListScrollbarThumbY = 0;
            functionListScrollbarThumbHeight = 0;
            return;
        }
        graphics.fill(functionListScrollbarX, functionListScrollbarY, functionListScrollbarX + SCROLLBAR_WIDTH, functionListScrollbarY + functionListScrollbarHeight, 0x70101820);
        int thumbColor = draggingFunctionListScrollbar ? ACTION : TEXT_SECONDARY;
        graphics.fill(functionListScrollbarX, functionListScrollbarThumbY, functionListScrollbarX + SCROLLBAR_WIDTH, functionListScrollbarThumbY + functionListScrollbarThumbHeight, thumbColor);
    }

    private void drawCommandListScrollbar(GuiGraphics graphics, boolean visible) {
        if (!visible) {
            commandListScrollbarX = 0;
            commandListScrollbarY = 0;
            commandListScrollbarHeight = 0;
            commandListScrollbarThumbY = 0;
            commandListScrollbarThumbHeight = 0;
            return;
        }
        graphics.fill(commandListScrollbarX, commandListScrollbarY, commandListScrollbarX + SCROLLBAR_WIDTH, commandListScrollbarY + commandListScrollbarHeight, 0x70101820);
        int thumbColor = draggingCommandListScrollbar ? ACTION : TEXT_SECONDARY;
        graphics.fill(commandListScrollbarX, commandListScrollbarThumbY, commandListScrollbarX + SCROLLBAR_WIDTH, commandListScrollbarThumbY + commandListScrollbarThumbHeight, thumbColor);
    }

    private void drawIconPickerScrollbar(GuiGraphics graphics, boolean visible) {
        if (!visible) {
            iconPickerScrollbarX = 0;
            iconPickerScrollbarY = 0;
            iconPickerScrollbarHeight = 0;
            iconPickerScrollbarThumbY = 0;
            iconPickerScrollbarThumbHeight = 0;
            return;
        }
        graphics.fill(iconPickerScrollbarX, iconPickerScrollbarY, iconPickerScrollbarX + SCROLLBAR_WIDTH, iconPickerScrollbarY + iconPickerScrollbarHeight, 0x70101820);
        int thumbColor = draggingIconPickerScrollbar ? ACTION : TEXT_SECONDARY;
        graphics.fill(iconPickerScrollbarX, iconPickerScrollbarThumbY, iconPickerScrollbarX + SCROLLBAR_WIDTH, iconPickerScrollbarThumbY + iconPickerScrollbarThumbHeight, thumbColor);
    }

    private void drawWheelsPanel(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        WheelDefinition wheel = activeWheelContext();
        List<EntryListRow> visibleRows = visibleEntryRows();
        int actionY = y + height - 114;
        int contentHeight = listContentHeight(visibleRows.size());
        boolean needsScrollbar = contentHeight > Math.max(0, actionY - y - 8);
        wheelsAreaX = x;
        wheelsAreaY = y;
        wheelsAreaWidth = width;
        wheelsAreaHeight = Math.max(0, actionY - y - 8);
        wheelsActionY = actionY;
        wheelsListWidth = width - (needsScrollbar ? (SCROLLBAR_WIDTH + SCROLLBAR_GAP) : 0);
        wheelsScrollOffset = clamp(wheelsScrollOffset, 0, Math.max(0, contentHeight - wheelsAreaHeight));
        updateWheelsScrollbarBounds(contentHeight);
        if (wheel != null) {
            int draggedInsertVisibleIndex = draggedWheelActive ? draggedWheelVisibleInsertIndex(visibleRows) : -1;
            enableUiScissor(graphics, x, y, x + wheelsListWidth, y + wheelsAreaHeight);
            int visibleRowIndex = 0;
            for (int index = 0; index < visibleRows.size(); index++) {
                EntryListRow row = visibleRows.get(index);
                if (draggedWheelActive && shouldSkipDraggedWheelRow(row)) {
                    continue;
                }
                int displayIndex = visibleRowIndex;
                if (draggedWheelActive && visibleRowIndex >= draggedInsertVisibleIndex) {
                    displayIndex++;
                }
                int rowY = wheelRowY(displayIndex);
                if (rowY + PROFILE_ROW_HEIGHT >= y && rowY <= y + wheelsAreaHeight) {
                    drawEntryListRow(graphics, minecraft, row, x, rowY, wheelsListWidth, false);
                }
                visibleRowIndex++;
            }
            graphics.disableScissor();
            if (draggedWheelActive) {
                drawWheelDropIndicator(graphics, x, wheelRowY(draggedInsertVisibleIndex), wheelsListWidth);
                EntryListRow draggedRow = draggedEntryRow();
                if (draggedRow != null) {
                    drawEntryListRow(graphics, minecraft, draggedRow, Math.round(draggedWheelVisualX), Math.round(draggedWheelVisualY), wheelsListWidth, true);
                }
            }
        }
        drawWheelsScrollbar(graphics, needsScrollbar);

        int halfWidth = (width - 8) / 2;
        addButton(graphics, minecraft, x, actionY, halfWidth, text("omniwheel.manager.new_command"), canCreateEntry(), this::createCommandEntry);
        addButton(graphics, minecraft, x + halfWidth + 8, actionY, halfWidth, text("omniwheel.manager.new_chat"), canCreateEntry(), this::createChatEntry);
        addButton(graphics, minecraft, x, actionY + 30, halfWidth, text("omniwheel.manager.new_submenu"), canCreateEntry(), this::createSubmenuEntry);
        addButton(graphics, minecraft, x + halfWidth + 8, actionY + 30, halfWidth, text("omniwheel.manager.new_function"), canCreateEntry(), this::createFunctionEntry);
        addButton(graphics, minecraft, x, actionY + 60, width, text("omniwheel.common.delete"), canDeleteSelection(), this::deleteSelection);
        addButton(graphics, minecraft, x, actionY + 90, width, selectedThingToggleLabel(), canToggleSelection(), this::toggleSelectionActive);
    }

    private void drawEditorPanel(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        if (iconPickerOpen) {
            drawIconPicker(graphics, minecraft, x, y, width, height);
            return;
        }

        WheelProfile profile = selectedProfile();
        WheelDefinition wheel = selectedWheel();
        WheelEntry entry = selectedEntry();

        if (profile == null || wheel == null) {
            graphics.drawString(minecraft.font, text("omniwheel.manager.no_wheel_selected"), x, y, TEXT_SECONDARY, false);
            return;
        }

        if (entry == null) {
            graphics.drawString(minecraft.font, text("omniwheel.manager.no_entry_selected"), x, y, TEXT_SECONDARY, false);
            return;
        }

        int editorY = y - 14;
        int formGap = 8;
        int rightColumnWidth = Math.max(176, Math.min(220, (width * 2) / 5));
        int leftColumnWidth = width - rightColumnWidth - formGap;
        int rightColumnX = x + leftColumnWidth + formGap;
        int iconToggleButtonWidth = 24;
        int iconButtonWidth = 24;
        int shortcutToggleButtonWidth = 24;
        int shortcutButtonWidth = 24;
        int labelToggleButtonWidth = 24;
        int iconToggleButtonGap = 6;
        int iconButtonGap = 6;
        int shortcutToggleButtonGap = 6;
        int shortcutButtonGap = 6;
        int nameButtonGap = 6;
        int nameFieldWidth = Math.max(120, leftColumnWidth - labelToggleButtonWidth - nameButtonGap);
        int iconFieldWidth = Math.max(96, leftColumnWidth - iconToggleButtonWidth - iconToggleButtonGap - iconButtonWidth - iconButtonGap);
        int shortcutFieldWidth = Math.max(96, width - shortcutToggleButtonWidth - shortcutToggleButtonGap - shortcutButtonWidth - shortcutButtonGap);
        int lowerSectionWidth = width;
        boolean tutorialActive = isManagerTutorialActive();
        boolean showNameSection = !tutorialActive || managerTutorialStepIndex >= 2;
        boolean showIconSection = !tutorialActive || managerTutorialStepIndex >= 3;
        boolean showColorSection = !tutorialActive || managerTutorialStepIndex >= 4;
        boolean showLowerSection = !tutorialActive || managerTutorialStepIndex >= 5;
        boolean showDetailsSection = !tutorialActive || managerTutorialStepIndex >= 6;

        textFields.get(EditorField.ENTRY_LABEL).visible = false;
        textFields.get(EditorField.ENTRY_GLYPH).visible = false;
        textFields.get(EditorField.ENTRY_DESCRIPTION).visible = false;
        textFields.get(EditorField.ENTRY_SHORTCUT).visible = false;
        textFields.get(EditorField.ENTRY_VALUE).visible = false;
        for (TextFieldState commandField : commandValueFields) {
            commandField.visible = false;
        }
        if (commandInput != null) {
            commandInput.setVisible(false);
            commandInput.setFocused(false);
        }

        if (showNameSection) {
            drawTextField(graphics, minecraft, EditorField.ENTRY_LABEL, x, editorY + 14, nameFieldWidth, text("omniwheel.manager.field.name"), true);
            drawLabelVisibilityButton(graphics, minecraft, x + nameFieldWidth + nameButtonGap, editorY + 26, labelToggleButtonWidth, 22);
        }
        if (showIconSection) {
            drawTextField(graphics, minecraft, EditorField.ENTRY_GLYPH, x, editorY + 52, iconFieldWidth, text("omniwheel.manager.field.icon"), true);
            drawIconVisibilityButton(graphics, minecraft, x + iconFieldWidth + iconToggleButtonGap, editorY + 64, iconToggleButtonWidth, 22);
            drawIconPickerButton(graphics, minecraft, x + iconFieldWidth + iconToggleButtonGap + iconToggleButtonWidth + iconButtonGap, editorY + 64, iconButtonWidth, 22);
        }
        if (showColorSection) {
            drawColorPalette(graphics, minecraft, text("omniwheel.manager.field.gui_color"), rightColumnX, editorY + 14, rightColumnWidth, ENTRY_GUI_COLORS, WheelEntry.DEFAULT_GUI_COLOR, entryGuiColorDraft, color -> {
                entryGuiColorDraft = color;
                autosaveCurrentEntryDraft();
            });
            drawColorPalette(graphics, minecraft, text("omniwheel.manager.field.name_color"), rightColumnX, editorY + 52, rightColumnWidth, ENTRY_NAME_COLORS, WheelEntry.DEFAULT_COLOR, entryColorDraft, color -> {
                entryColorDraft = color;
                autosaveCurrentEntryDraft();
            });
        }

        if (showDetailsSection) {
            drawTextField(graphics, minecraft, EditorField.ENTRY_DESCRIPTION, x, editorY + 90, width, text("omniwheel.manager.field.description"), true);
            drawTextField(graphics, minecraft, EditorField.ENTRY_SHORTCUT, x, editorY + 128, shortcutFieldWidth, text("omniwheel.manager.field.shortcut"), true);
            drawShortcutVisibilityButton(graphics, minecraft, x + shortcutFieldWidth + shortcutToggleButtonGap, editorY + 140, shortcutToggleButtonWidth, 22);
            drawShortcutControlsButton(graphics, minecraft, x + shortcutFieldWidth + shortcutToggleButtonGap + shortcutToggleButtonWidth + shortcutButtonGap, editorY + 140, shortcutButtonWidth, 22);
            String shortcutConflict = shortcutConflictMessage();
            if (shortcutConflict != null) {
                graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, shortcutConflict, width), x, editorY + 166, WARNING, false);
            }
        }

        boolean readOnlyValue = entryActionTypeDraft == DraftActionType.OPEN_SCREEN;
        String valueLabel = switch (entryActionTypeDraft) {
            case OPEN_SCREEN -> text("omniwheel.manager.field.screen");
            case COMMAND -> text("omniwheel.manager.field.commands");
            case FUNCTION -> text("omniwheel.manager.field.function");
            case CHAT -> text("omniwheel.manager.field.message");
            default -> text("omniwheel.manager.field.value");
        };
        String valueText = switch (entryActionTypeDraft) {
            case OPEN_SCREEN -> screenTargetLabel(entryScreenTargetDraft);
            case FUNCTION -> entryFunctionDraft.displayName();
            default -> entryValueDraft;
        };
        int lowerSectionY = editorY + 184;
        if (showLowerSection) {
            if (entryActionTypeDraft == DraftActionType.COMMAND) {
                drawCommandValueFields(graphics, minecraft, x, lowerSectionY, lowerSectionWidth, Math.max(140, height - 174));
                textFields.get(EditorField.ENTRY_VALUE).visible = false;
            } else if (entryActionTypeDraft == DraftActionType.FUNCTION) {
                drawFunctionSelector(graphics, minecraft, x, lowerSectionY, lowerSectionWidth, Math.max(140, height - 174));
                textFields.get(EditorField.ENTRY_VALUE).visible = false;
            } else if (entryActionTypeDraft == DraftActionType.SUBMENU) {
                textFields.get(EditorField.ENTRY_VALUE).visible = false;
            } else if (readOnlyValue) {
                drawReadOnlyField(graphics, minecraft, x, lowerSectionY, lowerSectionWidth, valueLabel, valueText);
                textFields.get(EditorField.ENTRY_VALUE).visible = false;
            } else {
                drawTextField(graphics, minecraft, EditorField.ENTRY_VALUE, x, lowerSectionY, lowerSectionWidth, valueLabel, true);
            }
        }

    }

    private void drawIconPickerButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        iconButtonX = x;
        iconButtonY = y;
        iconButtonWidth = width;
        iconButtonHeight = height;
        boolean focused = focusedActionIndex == actions.size();
        boolean highlighted = isButtonHighlighted(minecraft, x, y, width, height, focused);
        drawButtonChrome(graphics, x, y, width, height, ROW, ROW_SELECTED, true, highlighted);
        graphics.drawCenteredString(minecraft.font, "...", x + (width / 2), y + 7, highlighted ? TEXT : ACTION);
        actions.add(new ClickAction(x, y, width, height, true, this::toggleIconPicker));
    }

    private void drawColorPalette(
            GuiGraphics graphics,
            Minecraft minecraft,
            String label,
            int x,
            int labelY,
            int width,
            int[] palette,
            int defaultColorValue,
            int selectedColorValue,
            IntConsumer onSelect
    ) {
        graphics.drawString(minecraft.font, label, x, labelY, TEXT_SECONDARY, false);
        int swatchOuterSize = 22;
        int swatchInnerInset = 2;
        int swatchInnerSize = swatchOuterSize - (swatchInnerInset * 2);
        int colorCount = palette.length;
        int swatchGap = colorCount > 1 ? Math.max(0, (width - (colorCount * swatchOuterSize)) / (colorCount - 1)) : 0;
        int swatchGapRemainder = colorCount > 1 ? Math.max(0, width - (colorCount * swatchOuterSize) - (swatchGap * (colorCount - 1))) : 0;
        int swatchX = x;
        int swatchY = labelY + 12;
        if (palette == ENTRY_NAME_COLORS) {
            nameColorPaletteX = x;
            nameColorPaletteY = swatchY;
            nameColorPaletteWidth = width;
            nameColorPaletteHeight = swatchOuterSize;
        }
        for (int colorIndex = 0; colorIndex < colorCount; colorIndex++) {
            int color = palette[colorIndex];
            boolean defaultSwatch = colorIndex == 0;
            boolean selectedSwatch = defaultSwatch ? selectedColorValue == defaultColorValue : color == selectedColorValue;
            boolean focusedSwatch = focusedActionIndex == actions.size();
            boolean hoveredSwatch = !isManagerTutorialActive()
                    && contains(currentMouseX(minecraft), currentMouseY(minecraft), swatchX, swatchY, swatchOuterSize, swatchOuterSize);
            if (selectedSwatch) {
                drawSelectedPaletteOutline(graphics, swatchX, swatchY, swatchOuterSize);
            } else {
                graphics.fill(swatchX + 1, swatchY + 1, swatchX + swatchOuterSize - 1, swatchY + swatchOuterSize - 1, FIELD_EDGE);
            }
            if (defaultSwatch) {
                drawDefaultPaletteSwatch(graphics, swatchX + swatchInnerInset, swatchY + swatchInnerInset, swatchInnerSize);
            } else {
                graphics.fill(
                        swatchX + swatchInnerInset,
                        swatchY + swatchInnerInset,
                        swatchX + swatchInnerInset + swatchInnerSize,
                        swatchY + swatchInnerInset + swatchInnerSize,
                        color
                );
            }
            if (selectedSwatch) {
                graphics.fill(
                        swatchX + swatchInnerInset,
                        swatchY + swatchInnerInset,
                        swatchX + swatchInnerInset + swatchInnerSize,
                        swatchY + swatchInnerInset + 1,
                        0x18FFFFFF
                );
            }
            if (focusedSwatch || hoveredSwatch) {
                drawFocusedActionOutline(graphics, swatchX, swatchY, swatchOuterSize, swatchOuterSize);
            }
            final int swatchColor = defaultSwatch ? defaultColorValue : color;
            actions.add(new ClickAction(swatchX, swatchY, swatchOuterSize, swatchOuterSize, true, () -> onSelect.accept(swatchColor)));
            swatchX += swatchOuterSize;
            if (colorIndex < colorCount - 1) {
                swatchX += swatchGap;
                if (swatchGapRemainder > 0) {
                    swatchX += 1;
                    swatchGapRemainder--;
                }
            }
        }
    }

    private void drawDefaultPaletteSwatch(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF212831);
    }

    private void drawSelectedPaletteOutline(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFFD3DEE8);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF162029);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, FIELD);
    }

    private void drawIconVisibilityButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        iconToggleButtonX = x;
        iconToggleButtonY = y;
        iconToggleButtonWidth = width;
        iconToggleButtonHeight = height;
        int fillColor = entryShowIconDraft ? ROW : ROW_DISABLED;
        int textColor = entryShowIconDraft ? ACTION : ACTION_DISABLED;
        boolean focused = focusedActionIndex == actions.size();
        boolean highlighted = isButtonHighlighted(minecraft, x, y, width, height, focused);
        drawButtonChrome(graphics, x, y, width, height, fillColor, ROW_SELECTED, true, highlighted);
        graphics.drawCenteredString(minecraft.font, text("omniwheel.manager.button.icon_visibility"), x + (width / 2), y + 7, highlighted ? TEXT : textColor);
        actions.add(new ClickAction(x, y, width, height, true, this::toggleEntryIconVisibility));
    }

    private void drawLabelVisibilityButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        labelToggleButtonX = x;
        labelToggleButtonY = y;
        labelToggleButtonWidth = width;
        labelToggleButtonHeight = height;
        int fillColor = entryShowLabelDraft ? ROW : ROW_DISABLED;
        int textColor = entryShowLabelDraft ? ACTION : ACTION_DISABLED;
        boolean focused = focusedActionIndex == actions.size();
        boolean highlighted = isButtonHighlighted(minecraft, x, y, width, height, focused);
        drawButtonChrome(graphics, x, y, width, height, fillColor, ROW_SELECTED, true, highlighted);
        graphics.drawCenteredString(minecraft.font, text("omniwheel.manager.button.label_visibility"), x + (width / 2), y + 7, highlighted ? TEXT : textColor);
        actions.add(new ClickAction(x, y, width, height, true, this::toggleEntryLabelVisibility));
    }

    private void drawShortcutVisibilityButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        shortcutToggleButtonX = x;
        shortcutToggleButtonY = y;
        shortcutToggleButtonWidth = width;
        shortcutToggleButtonHeight = height;
        int fillColor = entryShowShortcutDraft ? ROW : ROW_DISABLED;
        int textColor = entryShowShortcutDraft ? ACTION : ACTION_DISABLED;
        boolean focused = focusedActionIndex == actions.size();
        boolean highlighted = isButtonHighlighted(minecraft, x, y, width, height, focused);
        drawButtonChrome(graphics, x, y, width, height, fillColor, ROW_SELECTED, true, highlighted);
        graphics.drawCenteredString(minecraft.font, text("omniwheel.manager.button.shortcut_visibility"), x + (width / 2), y + 7, highlighted ? TEXT : textColor);
        actions.add(new ClickAction(x, y, width, height, true, this::toggleEntryShortcutVisibility));
    }

    private void drawShortcutControlsButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        shortcutButtonX = x;
        shortcutButtonY = y;
        shortcutButtonWidth = width;
        shortcutButtonHeight = height;
        boolean focused = focusedActionIndex == actions.size();
        boolean highlighted = isButtonHighlighted(minecraft, x, y, width, height, focused);
        drawButtonChrome(graphics, x, y, width, height, ROW, ROW_SELECTED, true, highlighted);
        graphics.drawCenteredString(minecraft.font, text("omniwheel.manager.button.controls"), x + (width / 2), y + 7, highlighted ? TEXT : ACTION);
        actions.add(new ClickAction(x, y, width, height, true, this::openControlsMenu));
    }

    private void drawEditorButtonTooltip(GuiGraphics graphics, Minecraft minecraft) {
        if (iconPickerOpen || isManagerTutorialActive()) {
            return;
        }
        float mouseX = currentMouseX(minecraft);
        float mouseY = currentMouseY(minecraft);
        String tooltip = null;
        if (contains(mouseX, mouseY, iconToggleButtonX, iconToggleButtonY, iconToggleButtonWidth, iconToggleButtonHeight)) {
            tooltip = text(entryShowIconDraft ? "omniwheel.manager.tooltip.icon_off" : "omniwheel.manager.tooltip.icon_on");
        } else if (contains(mouseX, mouseY, iconButtonX, iconButtonY, iconButtonWidth, iconButtonHeight)) {
            tooltip = text("omniwheel.manager.tooltip.icon_picker");
        } else if (contains(mouseX, mouseY, labelToggleButtonX, labelToggleButtonY, labelToggleButtonWidth, labelToggleButtonHeight)) {
            tooltip = text(entryShowLabelDraft ? "omniwheel.manager.tooltip.label_off" : "omniwheel.manager.tooltip.label_on");
        } else if (contains(mouseX, mouseY, shortcutToggleButtonX, shortcutToggleButtonY, shortcutToggleButtonWidth, shortcutToggleButtonHeight)) {
            tooltip = text(entryShowShortcutDraft ? "omniwheel.manager.tooltip.shortcut_off" : "omniwheel.manager.tooltip.shortcut_on");
        } else if (contains(mouseX, mouseY, shortcutButtonX, shortcutButtonY, shortcutButtonWidth, shortcutButtonHeight)) {
            tooltip = text("omniwheel.manager.tooltip.controls");
        } else {
            for (TooltipRegion region : tooltipRegions) {
                if (contains(mouseX, mouseY, region.x(), region.y(), region.width(), region.height())) {
                    tooltip = region.text();
                    break;
                }
            }
        }
        if (tooltip == null) {
            return;
        }
        drawTooltip(graphics, minecraft, tooltip, Math.round(mouseX) + 12, Math.round(mouseY) + 12);
    }

    private void drawTooltip(GuiGraphics graphics, Minecraft minecraft, String text, int x, int y) {
        int paddingX = 8;
        int paddingY = 6;
        int textWidth = minecraft.font.width(text);
        int boxWidth = textWidth + (paddingX * 2);
        int boxHeight = 8 + (paddingY * 2);
        int maxX = uiWidth(minecraft) - boxWidth - 6;
        int maxY = uiHeight(minecraft) - boxHeight - 6;
        int boxX = clamp(x, 6, Math.max(6, maxX));
        int boxY = clamp(y, 6, Math.max(6, maxY));

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 250.0F);
        drawPanel(graphics, boxX, boxY, boxWidth, boxHeight, false);
        graphics.drawString(minecraft.font, text, boxX + paddingX, boxY + paddingY, TEXT, false);
        graphics.pose().popPose();
    }

    private void drawManagerTutorial(GuiGraphics graphics, Minecraft minecraft) {
        TutorialStep tutorialStep = currentManagerTutorialStep();
        if (tutorialStep == null) {
            return;
        }

        int boxWidth = tutorialStep.width();
        List<FormattedCharSequence> bodyLines = minecraft.font.split(Component.literal(tutorialStep.body()), boxWidth - 20);
        int bodyHeight = Math.max(8, bodyLines.size() * 9);
        int buttonHeight = 20;
        int buttonGap = 6;
        boolean lastStep = managerTutorialStepIndex >= MANAGER_TUTORIAL_STEP_COUNT - 1;
        int buttonWidth = (boxWidth - 22 - buttonGap) / 2;
        int boxHeight = 18 + 8 + bodyHeight + 12 + buttonHeight + 12;
        int boxX = Math.max(8, Math.min(tutorialStep.x(), Math.max(8, uiWidth(minecraft) - boxWidth - 8)));
        int boxY = Math.max(8, Math.min(tutorialStep.y(), Math.max(8, uiHeight(minecraft) - boxHeight - 8)));

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 260.0F);
        drawPanel(graphics, boxX, boxY, boxWidth, boxHeight, false);
        String stepLabel = (Math.min(managerTutorialStepIndex, MANAGER_TUTORIAL_STEP_COUNT - 1) + 1) + "/" + MANAGER_TUTORIAL_STEP_COUNT;
        graphics.drawString(minecraft.font, stepLabel, boxX + boxWidth - 10 - minecraft.font.width(stepLabel), boxY + 10, TEXT_SECONDARY, false);
        graphics.drawString(minecraft.font, tutorialStep.title(), boxX + 10, boxY + 10, TEXT, false);
        int lineY = boxY + 26;
        for (FormattedCharSequence line : bodyLines) {
            graphics.drawString(minecraft.font, line, boxX + 10, lineY, TEXT_SECONDARY, false);
            lineY += 9;
        }

        int buttonY = boxY + boxHeight - buttonHeight - 10;
        tutorialNextButtonX = boxX + 8;
        tutorialNextButtonY = buttonY;
        tutorialNextButtonWidth = lastStep ? boxWidth - 16 : buttonWidth;
        tutorialNextButtonHeight = buttonHeight;
        tutorialSkipAllButtonX = tutorialNextButtonX + buttonWidth + buttonGap;
        tutorialSkipAllButtonY = buttonY;
        tutorialSkipAllButtonWidth = lastStep ? 0 : buttonWidth;
        tutorialSkipAllButtonHeight = buttonHeight;

        drawTutorialButton(graphics, minecraft, tutorialNextButtonX, tutorialNextButtonY, tutorialNextButtonWidth, tutorialNextButtonHeight, text(lastStep ? "omniwheel.common.finish" : "omniwheel.common.next"));
        if (!lastStep) {
            drawTutorialButton(graphics, minecraft, tutorialSkipAllButtonX, tutorialSkipAllButtonY, tutorialSkipAllButtonWidth, tutorialSkipAllButtonHeight, text("omniwheel.common.skip_all"));
        }
        graphics.pose().popPose();
    }

    private void drawTutorialButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height, String label) {
        boolean highlighted = contains(currentMouseX(minecraft), currentMouseY(minecraft), x, y, width, height);
        drawButtonChrome(graphics, x, y, width, height, ROW, ROW_SELECTED, true, highlighted);
        graphics.drawCenteredString(minecraft.font, label, x + (width / 2), y + 6, TEXT);
    }

    private boolean handleManagerTutorialClick(float mouseX, float mouseY) {
        if (currentManagerTutorialStep() == null) {
            return false;
        }
        if (contains(mouseX, mouseY, tutorialNextButtonX, tutorialNextButtonY, tutorialNextButtonWidth, tutorialNextButtonHeight)) {
            advanceManagerTutorial();
            return true;
        }
        if (contains(mouseX, mouseY, tutorialSkipAllButtonX, tutorialSkipAllButtonY, tutorialSkipAllButtonWidth, tutorialSkipAllButtonHeight)) {
            completeManagerTutorial();
            return true;
        }
        return isManagerTutorialActive();
    }

    private void advanceManagerTutorial() {
        if (managerTutorialStepIndex < 0) {
            return;
        }
        managerTutorialStepIndex++;
        if (managerTutorialStepIndex >= MANAGER_TUTORIAL_STEP_COUNT) {
            completeManagerTutorial();
            return;
        }
        runtime.tutorialProgressStore().setCurrentStep(TutorialType.MANAGER, managerTutorialStepIndex);
    }

    private void completeManagerTutorial() {
        runtime.tutorialProgressStore().markCompleted(TutorialType.MANAGER);
        managerTutorialStepIndex = -1;
    }

    private boolean isManagerTutorialActive() {
        return managerTutorialStepIndex >= 0 && !runtime.tutorialProgressStore().isCompleted(TutorialType.MANAGER);
    }

    private TutorialStep currentManagerTutorialStep() {
        if (managerTutorialStepIndex < 0 || runtime.tutorialProgressStore().isCompleted(TutorialType.MANAGER)) {
            return null;
        }

        Font font = Minecraft.getInstance().font;
        String title;
        String body;
        int cardWidth;
        return switch (Math.min(managerTutorialStepIndex, MANAGER_TUTORIAL_STEP_COUNT - 1)) {
            case 0 -> {
                title = text("omniwheel.tutorial.manager.profiles.title");
                body = text("omniwheel.tutorial.manager.profiles.body");
                cardWidth = tutorialCardWidth(font, title, body, 180, 248);
                yield new TutorialStep(title, body, profilesPanelX + profilesPanelWidth + 18, profilesPanelY + 42, cardWidth);
            }
            case 1 -> {
                title = text("omniwheel.tutorial.manager.entries.title");
                body = text("omniwheel.tutorial.manager.entries.body");
                cardWidth = tutorialCardWidth(font, title, body, 190, 264);
                yield new TutorialStep(title, body, wheelsPanelX + wheelsPanelWidth + 18, wheelsPanelY + 42, cardWidth);
            }
            case 2 -> {
                title = text("omniwheel.tutorial.manager.name.title");
                body = text("omniwheel.tutorial.manager.name.body");
                cardWidth = tutorialCardWidth(font, title, body, 186, 250);
                yield new TutorialStep(title, body, editorPanelX - cardWidth - 18, editorPanelY + 42, cardWidth);
            }
            case 3 -> {
                title = text("omniwheel.tutorial.manager.icon.title");
                body = text("omniwheel.tutorial.manager.icon.body");
                cardWidth = tutorialCardWidth(font, title, body, 192, 258);
                yield new TutorialStep(title, body, editorPanelX - cardWidth - 18, editorPanelY + 80, cardWidth);
            }
            case 4 -> {
                title = text("omniwheel.tutorial.manager.colors.title");
                body = text("omniwheel.tutorial.manager.colors.body");
                cardWidth = tutorialCardWidth(font, title, body, 196, 266);
                yield new TutorialStep(title, body, editorPanelX - cardWidth - 18, editorPanelY + 42, cardWidth);
            }
            case 5 -> {
                title = text("omniwheel.tutorial.manager.action.title");
                body = text("omniwheel.tutorial.manager.action.body");
                cardWidth = tutorialCardWidth(font, title, body, 202, 276);
                yield new TutorialStep(title, body, editorPanelX - cardWidth - 18, editorPanelY + 208, cardWidth);
            }
            default -> {
                title = text("omniwheel.tutorial.manager.shortcuts.title");
                body = text("omniwheel.tutorial.manager.shortcuts.body");
                cardWidth = tutorialCardWidth(font, title, body, 196, 266);
                yield new TutorialStep(title, body, editorPanelX - cardWidth - 18, editorPanelY + 154, cardWidth);
            }
        };
    }

    private static int tutorialCardWidth(Font font, String title, String body, int minWidth, int maxWidth) {
        int buttonFloorWidth = 166;
        int titleWidth = font.width(title) + 20;
        int bodyWidth = font.width(body) + 20;
        int widestTokenWidth = 0;
        for (String token : body.split("\\s+")) {
            widestTokenWidth = Math.max(widestTokenWidth, font.width(token) + 28);
        }
        int minimum = Math.max(buttonFloorWidth, Math.min(minWidth, Math.max(titleWidth, widestTokenWidth)));
        int preferred = Math.max(titleWidth, Math.max(widestTokenWidth, Math.min(maxWidth, bodyWidth)));
        return Math.max(minimum, Math.min(maxWidth, preferred));
    }

    private static boolean contains(float mouseX, float mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private String shortcutConflictMessage() {
        String shortcut = entryShortcutDraft == null ? "" : entryShortcutDraft.trim();
        if (shortcut.isEmpty()) {
            return null;
        }

        EntryShortcutMatcher.ShortcutSpec spec = EntryShortcutMatcher.parse(shortcut);
        if (spec == null) {
            return text("omniwheel.manager.warning.shortcut_format");
        }

        List<String> conflicts = new ArrayList<>();
        conflicts.addAll(findOmniWheelShortcutConflicts(spec));
        conflicts.addAll(findMinecraftKeyConflicts(spec));
        if (conflicts.isEmpty()) {
            return null;
        }
        return text("omniwheel.manager.warning.shortcut_conflict", summarizeConflictLabels(conflicts));
    }

    private List<String> findOmniWheelShortcutConflicts(EntryShortcutMatcher.ShortcutSpec spec) {
        runtime.profileManager().ensureLoaded();
        List<String> conflicts = new ArrayList<>();
        WheelProfile selectedProfile = selectedProfile();
        String currentWheelId = selectedWheelId;
        String currentEntryId = selectedEntryId;

        for (WheelProfile profile : runtime.profileManager().getProfiles()) {
            for (WheelDefinition wheel : profile.wheels().values()) {
                for (WheelEntry entry : wheel.entries()) {
                    if (selectedProfile != null
                            && profile.id().equals(selectedProfile.id())
                            && Objects.equals(wheel.id(), currentWheelId)
                            && Objects.equals(entry.id(), currentEntryId)) {
                        continue;
                    }
                    EntryShortcutMatcher.ShortcutSpec other = EntryShortcutMatcher.parse(entry.shortcut());
                    if (spec.equals(other)) {
                        conflicts.add(text(
                                "omniwheel.manager.conflict.omniwheel",
                                displayText(profile.displayName()),
                                displayText(entry.label())
                        ));
                    }
                }
            }
        }
        return conflicts;
    }

    private List<String> findMinecraftKeyConflicts(EntryShortcutMatcher.ShortcutSpec spec) {
        Minecraft minecraft = Minecraft.getInstance();
        List<String> conflicts = new ArrayList<>();
        if (minecraft.options == null) {
            return conflicts;
        }

        for (KeyMapping keyMapping : minecraft.options.keyMappings) {
            for (int key : spec.keys()) {
                if (keyMapping.matches(key, -1)) {
                    conflicts.add(Component.translatable(keyMapping.getName()).getString());
                    break;
                }
            }
        }
        return conflicts;
    }

    private static String summarizeConflictLabels(List<String> labels) {
        List<String> unique = new ArrayList<>();
        for (String label : labels) {
            if (!unique.contains(label)) {
                unique.add(label);
            }
        }
        if (unique.isEmpty()) {
            return "";
        }
        if (unique.size() == 1) {
            return unique.getFirst();
        }
        if (unique.size() == 2) {
            return text("omniwheel.manager.list.two", unique.get(0), unique.get(1));
        }
        return text("omniwheel.manager.list.more", unique.get(0), unique.get(1), unique.size() - 2);
    }

    private void openControlsMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = hostScreen();
        if (minecraft.screen == null || minecraft.options == null) {
            return;
        }

        for (String className : List.of(
                "net.minecraft.client.gui.screens.options.controls.ControlsScreen",
                "net.minecraft.client.gui.screens.options.controls.KeyBindsScreen"
        )) {
            try {
                Class<?> screenClass = Class.forName(className);
                for (var constructor : screenClass.getConstructors()) {
                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    if (parameterTypes.length == 2
                            && Screen.class.isAssignableFrom(parameterTypes[0])
                            && parameterTypes[1].isInstance(minecraft.options)) {
                        minecraft.setScreen((Screen) constructor.newInstance(parent, minecraft.options));
                        return;
                    }
                    if (parameterTypes.length == 3
                            && Screen.class.isAssignableFrom(parameterTypes[0])
                            && parameterTypes[1].isInstance(minecraft.options)
                            && Component.class.isAssignableFrom(parameterTypes[2])) {
                        minecraft.setScreen((Screen) constructor.newInstance(parent, minecraft.options, Component.translatable("controls.keybinds.title")));
                        return;
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private void drawCommandValueFields(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        ensureCommandValueFields();
        int rowStep = 38;
        int buttonHeight = 24;
        int contentHeight = (commandValueFields.size() * rowStep) + buttonHeight;
        boolean needsScrollbar = contentHeight > height;
        commandListX = x;
        commandListY = y;
        commandListHeight = height;
        commandListWidth = width - (needsScrollbar ? (SCROLLBAR_WIDTH + SCROLLBAR_GAP) : 0);
        commandListScrollOffset = clamp(commandListScrollOffset, 0, Math.max(0, contentHeight - commandListHeight));
        updateCommandListScrollbarBounds(contentHeight);
        boolean showRemoveButtons = commandValueFields.size() > 1;
        int removeButtonWidth = showRemoveButtons ? 24 : 0;
        int removeButtonGap = showRemoveButtons ? 6 : 0;
        int fieldWidth = Math.max(96, commandListWidth - removeButtonWidth - removeButtonGap);

        enableUiScissor(graphics, commandListX, commandListY, commandListX + commandListWidth, commandListY + commandListHeight);
        for (int index = 0; index < commandValueFields.size(); index++) {
            int fieldY = y + (index * rowStep) - commandListScrollOffset;
            if (fieldY + 34 < commandListY || fieldY > commandListY + commandListHeight) {
                commandValueFields.get(index).visible = false;
                continue;
            }
            if (isUsingVanillaCommandInput(index)) {
                drawActiveCommandField(graphics, minecraft, x, fieldY, fieldWidth, text("omniwheel.manager.command_number", index + 1), index);
            } else {
                drawTextFieldState(
                        graphics,
                        minecraft,
                        commandValueFields.get(index),
                        x,
                        fieldY,
                        fieldWidth,
                        text("omniwheel.manager.command_number", index + 1),
                        true
                );
            }
            if (showRemoveButtons) {
                drawCommandRemoveButton(
                        graphics,
                        minecraft,
                        x + fieldWidth + removeButtonGap,
                        fieldY + 12,
                        removeButtonWidth,
                        22,
                        index
                );
            }
        }
        graphics.disableScissor();
        addCommandButtonX = x;
        addCommandButtonY = y + (commandValueFields.size() * rowStep) - commandListScrollOffset;
        addCommandButtonWidth = commandListWidth;
        addCommandButtonHeight = buttonHeight;
        if (addCommandButtonY + addCommandButtonHeight > commandListY && addCommandButtonY < commandListY + commandListHeight) {
            addButton(graphics, minecraft, addCommandButtonX, addCommandButtonY, addCommandButtonWidth, text("omniwheel.manager.add_command"), true, this::addCommandValueField);
        }
        drawCommandListScrollbar(graphics, needsScrollbar);
    }

    private void drawActiveCommandField(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String label, int index) {
        TextFieldState field = commandValueFields.get(index);
        field.visible = true;
        field.setBounds(x, y + 12, width, 22, 8);

        graphics.drawString(minecraft.font, label, x, y, TEXT_SECONDARY, false);
        graphics.fill(x, y + 12, x + width, y + 34, FIELD);
        graphics.fill(x, y + 12, x + width, y + 13, FIELD_FOCUSED);
        graphics.fill(x, y + 33, x + width, y + 34, FIELD_FOCUSED);
        graphics.fill(x, y + 12, x + 1, y + 34, FIELD_FOCUSED);
        graphics.fill(x + width - 1, y + 12, x + width, y + 34, FIELD_FOCUSED);

        if (ensureCommandInput()) {
            commandInput.setX(x + 8);
            commandInput.setY(y + 19);
            commandInput.setWidth(Math.max(8, width - 16));
            commandInput.setHeight(12);
            commandInput.setVisible(true);
            commandInput.setFocused(true);
            commandSuggestionsYOffset = (y + 39) - 72;
            commandInput.renderWidget(graphics, (int) currentMouseX(minecraft), (int) currentMouseY(minecraft), 0.0F);
        }
    }

    private void drawCommandRemoveButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height, int index) {
        boolean focused = focusedActionIndex == actions.size();
        boolean highlighted = isButtonHighlighted(minecraft, x, y, width, height, focused);
        drawButtonChrome(graphics, x, y, width, height, ROW, ROW_SELECTED, true, highlighted);
        graphics.drawCenteredString(minecraft.font, "-", x + (width / 2), y + 7, highlighted ? TEXT : ACTION);
        tooltipRegions.add(new TooltipRegion(x, y, width, height, text("omniwheel.manager.tooltip.delete_command")));
        actions.add(new ClickAction(x, y, width, height, true, () -> removeCommandValueField(index)));
    }

    private void alignCommandUsageToField() {
        if (commandSuggestions == null || commandInput == null) {
            return;
        }
        try {
            Field field = CommandSuggestions.class.getDeclaredField("commandUsagePosition");
            field.setAccessible(true);
            field.setInt(commandSuggestions, commandInput.getX());
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private boolean shouldRenderWrappedCommandUsage() {
        if (commandSuggestions == null || commandInput == null || commandSuggestions.isVisible()) {
            return false;
        }
        List<FormattedCharSequence> usage = commandUsageLines();
        return !usage.isEmpty() && commandUsageWidth() > commandInput.getWidth();
    }

    private void renderWrappedCommandUsage(GuiGraphics graphics, Minecraft minecraft) {
        if (commandInput == null) {
            return;
        }
        List<FormattedCharSequence> usage = commandUsageLines();
        if (usage.isEmpty()) {
            return;
        }

        List<FormattedCharSequence> wrappedLines = new ArrayList<>();
        int maxWidth = Math.max(8, commandInput.getWidth());
        for (FormattedCharSequence line : usage) {
            if (minecraft.font.width(line) <= maxWidth) {
                wrappedLines.add(line);
                continue;
            }
            List<FormattedCharSequence> split = minecraft.font.split(Component.literal(formattedSequenceToString(line)), maxWidth);
            if (split.isEmpty()) {
                wrappedLines.add(line);
            } else {
                wrappedLines.addAll(split);
            }
        }

        int lineY = commandInput.getY() + 19;
        int lineX = commandInput.getX();
        for (int index = 0; index < wrappedLines.size(); index++) {
            FormattedCharSequence line = wrappedLines.get(index);
            int y = lineY + (index * 12);
            int width = minecraft.font.width(line);
            graphics.fill(lineX - 1, y, lineX + width + 1, y + 12, Integer.MIN_VALUE);
            graphics.drawString(minecraft.font, line, lineX, y + 2, 0xFFFF8080);
        }
    }

    private List<FormattedCharSequence> commandUsageLines() {
        if (commandSuggestions == null) {
            return List.of();
        }
        try {
            Field field = CommandSuggestions.class.getDeclaredField("commandUsage");
            field.setAccessible(true);
            Object value = field.get(commandSuggestions);
            if (value instanceof List<?> list) {
                List<FormattedCharSequence> lines = new ArrayList<>();
                for (Object entry : list) {
                    if (entry instanceof FormattedCharSequence sequence) {
                        lines.add(sequence);
                    }
                }
                return lines;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return List.of();
    }

    private int commandUsageWidth() {
        if (commandSuggestions == null) {
            return 0;
        }
        try {
            Field field = CommandSuggestions.class.getDeclaredField("commandUsageWidth");
            field.setAccessible(true);
            return field.getInt(commandSuggestions);
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }

    private static String formattedSequenceToString(FormattedCharSequence sequence) {
        StringBuilder builder = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    private void drawFunctionSelector(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        List<GameplayFunction> functions = selectableFunctions();
        int rowHeight = 34;
        int rowStep = 38;
        int contentHeight = Math.max(0, functions.isEmpty() ? 0 : (functions.size() * rowStep) - (rowStep - rowHeight));
        boolean needsScrollbar = contentHeight > Math.max(0, Math.max(64, height - 12));
        functionListX = x;
        functionListY = y + 12;
        functionListWidth = width - (needsScrollbar ? (SCROLLBAR_WIDTH + SCROLLBAR_GAP) : 0);
        functionListHeight = Math.max(64, height - 12);
        functionListScrollOffset = clamp(functionListScrollOffset, 0, Math.max(0, contentHeight - functionListHeight));
        updateFunctionListScrollbarBounds(contentHeight);

        graphics.drawString(minecraft.font, text("omniwheel.manager.field.function"), x, y, TEXT_SECONDARY, false);
        graphics.fill(functionListX, functionListY, functionListX + functionListWidth, functionListY + functionListHeight, FIELD);

        enableUiScissor(graphics, functionListX, functionListY, functionListX + functionListWidth, functionListY + functionListHeight);
        for (int index = 0; index < functions.size(); index++) {
            GameplayFunction function = functions.get(index);
            int rowY = functionListY + (index * rowStep) - functionListScrollOffset;
            if (rowY + rowHeight < functionListY || rowY > functionListY + functionListHeight) {
                continue;
            }

            boolean selected = function == entryFunctionDraft;
            boolean focusedRow = focusedActionIndex == actions.size();
            graphics.fill(functionListX + 1, rowY, functionListX + functionListWidth - 1, rowY + rowHeight, selected ? ROW_SELECTED : ROW);
            if (selected) {
                graphics.fill(functionListX + 1, rowY, functionListX + functionListWidth - 1, rowY + 1, ACTION);
                graphics.fill(functionListX + 1, rowY + rowHeight - 1, functionListX + functionListWidth - 1, rowY + rowHeight, ACTION);
            }
            if (focusedRow) {
                drawFocusedActionOutline(graphics, functionListX, rowY, functionListWidth, rowHeight);
            }

            String title = ellipsizeToWidth(minecraft.font, function.displayName(), functionListWidth - 18);
            String detail = ellipsizeToWidth(minecraft.font, function.category().label() + " | " + functionModeLabel(function.mode()), functionListWidth - 18);
            graphics.drawString(minecraft.font, title, functionListX + 8, rowY + 7, TEXT, false);
            graphics.drawString(minecraft.font, detail, functionListX + 8, rowY + 19, selected ? ACTION : TEXT_SECONDARY, false);

            final GameplayFunction selectedFunction = function;
            actions.add(new ClickAction(functionListX, rowY, functionListWidth, rowHeight, true, () -> {
                entryFunctionDraft = selectedFunction;
                autosaveCurrentEntryDraft();
            }));
        }
        graphics.disableScissor();
        graphics.fill(functionListX, functionListY, functionListX + functionListWidth, functionListY + 1, FIELD_EDGE);
        graphics.fill(functionListX, functionListY + functionListHeight - 1, functionListX + functionListWidth, functionListY + functionListHeight, FIELD_EDGE);
        graphics.fill(functionListX, functionListY, functionListX + 1, functionListY + functionListHeight, FIELD_EDGE);
        graphics.fill(functionListX + functionListWidth - 1, functionListY, functionListX + functionListWidth, functionListY + functionListHeight, FIELD_EDGE);
        drawFunctionListScrollbar(graphics, needsScrollbar);
    }

    private void drawIconPicker(GuiGraphics graphics, Minecraft minecraft, int editorX, int editorY, int editorWidth, int editorHeight) {
        ensureIconPickerOptions();
        List<IconPickerOption> visibleOptions = filteredIconPickerOptions();

        int padding = 10;
        int cellSize = ICON_PICKER_CELL_SIZE;
        int cellGap = ICON_PICKER_CELL_GAP;
        int contentPadding = ICON_PICKER_CONTENT_PADDING;
        int closeButtonWidth = 52;
        int closeButtonY = editorY;
        int searchFieldHeight = 22;
        int searchGap = 8;
        int searchWidth = Math.max(96, editorWidth - closeButtonWidth - searchGap);

        iconPickerX = editorX;
        iconPickerY = closeButtonY;
        iconPickerWidth = editorWidth;
        iconPickerHeight = (editorY + editorHeight) - closeButtonY;
        iconPickerGridY = editorY + searchFieldHeight + padding;
        iconPickerGridHeight = Math.max(0, editorHeight - searchFieldHeight - padding);

        int columns;
        int contentHeight;
        int horizontalInset;
        boolean needsScrollbar = false;
        do {
            iconPickerListWidth = editorWidth - (needsScrollbar ? (SCROLLBAR_WIDTH + SCROLLBAR_GAP) : 0);
            iconPickerGridX = editorX;
            iconPickerGridWidth = iconPickerListWidth;
            int usableGridWidth = Math.max(0, iconPickerGridWidth - (contentPadding * 2));
            columns = Math.max(1, (usableGridWidth + cellGap) / (cellSize + cellGap));
            int contentWidth = (columns * cellSize) + ((columns - 1) * cellGap);
            horizontalInset = Math.max(0, (usableGridWidth - contentWidth) / 2);
            int rows = (visibleOptions.size() + columns - 1) / columns;
            contentHeight = rows == 0 ? 0 : (rows * (cellSize + cellGap)) - cellGap;
            boolean nextNeedsScrollbar = contentHeight > Math.max(0, iconPickerGridHeight - (contentPadding * 2));
            if (nextNeedsScrollbar == needsScrollbar) {
                break;
            }
            needsScrollbar = nextNeedsScrollbar;
        } while (true);
        iconPickerScrollOffset = clamp(iconPickerScrollOffset, 0, Math.max(0, contentHeight - Math.max(0, iconPickerGridHeight - (contentPadding * 2))));
        updateIconPickerScrollbarBounds(contentHeight);

        drawCompactTextFieldState(
                graphics,
                minecraft,
                textFields.get(EditorField.ICON_PICKER_SEARCH),
                editorX,
                editorY,
                searchWidth,
                searchFieldHeight,
                text("omniwheel.manager.search_icons"),
                true
        );
        iconPickerBackButtonX = editorX + editorWidth - closeButtonWidth;
        iconPickerBackButtonY = closeButtonY;
        iconPickerBackButtonWidth = closeButtonWidth;
        iconPickerBackButtonHeight = searchFieldHeight;
        addButton(
                graphics,
                minecraft,
                iconPickerBackButtonX,
                iconPickerBackButtonY,
                iconPickerBackButtonWidth,
                text("omniwheel.common.back"),
                iconPickerBackButtonHeight,
                true,
                this::closeIconPicker
        );

        graphics.fill(iconPickerGridX, iconPickerGridY, iconPickerGridX + iconPickerGridWidth, iconPickerGridY + iconPickerGridHeight, 0x70101820);

        String selectedIconId = normalizedIconId(entryGlyphDraft);
        float mouseX = currentMouseX(minecraft);
        float mouseY = currentMouseY(minecraft);

        enableUiScissor(graphics, iconPickerGridX, iconPickerGridY, iconPickerGridX + iconPickerGridWidth, iconPickerGridY + iconPickerGridHeight);
        for (int index = 0; index < visibleOptions.size(); index++) {
            IconPickerOption option = visibleOptions.get(index);
            int row = index / columns;
            int column = index % columns;
            int cellX = iconPickerGridX + contentPadding + horizontalInset + (column * (cellSize + cellGap));
            int cellY = iconPickerGridY + contentPadding + (row * (cellSize + cellGap)) - iconPickerScrollOffset;
            if (cellY + cellSize < iconPickerGridY || cellY > iconPickerGridY + iconPickerGridHeight) {
                continue;
            }

            boolean selected = option.id().equals(selectedIconId);
            boolean hovered = mouseX >= cellX && mouseX <= cellX + cellSize && mouseY >= cellY && mouseY <= cellY + cellSize;
            boolean focused = focusedActionIndex == actions.size();
            int fillColor = selected ? ROW_SELECTED : hovered ? ROW : FIELD;
            int edgeColor = selected ? ACTION : FIELD_EDGE;
            graphics.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, fillColor);
            graphics.fill(cellX, cellY, cellX + cellSize, cellY + 1, edgeColor);
            graphics.fill(cellX, cellY + cellSize - 1, cellX + cellSize, cellY + cellSize, edgeColor);
            graphics.fill(cellX, cellY, cellX + 1, cellY + cellSize, edgeColor);
            graphics.fill(cellX + cellSize - 1, cellY, cellX + cellSize, cellY + cellSize, edgeColor);
            if (focused) {
                drawFocusedActionOutline(graphics, cellX, cellY, cellSize, cellSize);
            }
            if (!EntryIconRenderer.drawIcon(graphics, minecraft, option.id(), cellX + (cellSize * 0.5F), cellY + (cellSize * 0.5F), 1.0F)) {
                graphics.drawCenteredString(minecraft.font, "?", cellX + (cellSize / 2), cellY + 7, TEXT);
            }
            final String selectedIcon = option.id();
            actions.add(new ClickAction(cellX, cellY, cellSize, cellSize, true, () -> selectIconFromPicker(selectedIcon)));
            if (pendingIconPickerFocusId != null && pendingIconPickerFocusId.equals(selectedIcon)) {
                focusedActionIndex = actions.size() - 1;
                pendingIconPickerFocusId = null;
                activeList = NavigationList.EDITOR;
            }
        }
        graphics.disableScissor();
        graphics.fill(iconPickerGridX, iconPickerGridY, iconPickerGridX + iconPickerGridWidth, iconPickerGridY + 1, FIELD_EDGE);
        graphics.fill(iconPickerGridX, iconPickerGridY + iconPickerGridHeight - 1, iconPickerGridX + iconPickerGridWidth, iconPickerGridY + iconPickerGridHeight, FIELD_EDGE);
        graphics.fill(iconPickerGridX, iconPickerGridY, iconPickerGridX + 1, iconPickerGridY + iconPickerGridHeight, FIELD_EDGE);
        graphics.fill(iconPickerGridX + iconPickerGridWidth - 1, iconPickerGridY, iconPickerGridX + iconPickerGridWidth, iconPickerGridY + iconPickerGridHeight, FIELD_EDGE);
        drawIconPickerScrollbar(graphics, needsScrollbar);
    }

    private void drawEntriesGrid(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height, List<WheelEntry> entries) {
        entriesAreaX = x;
        entriesAreaY = y;
        entriesAreaWidth = width;
        entriesAreaHeight = height;

        int columnGap = 8;
        int cardWidth = (width - columnGap) / 2;
        int cardHeight = 42;
        int rowGap = 8;
        int rows = (entries.size() + 1) / 2;
        int contentHeight = Math.max(0, rows == 0 ? 0 : (rows * (cardHeight + rowGap)) - rowGap);
        entriesScrollOffset = clamp(entriesScrollOffset, 0, Math.max(0, contentHeight - height));

        graphics.fill(x, y, x + width, y + height, 0x70101820);
        graphics.fill(x, y, x + width, y + 1, FIELD_EDGE);
        graphics.fill(x, y + height - 1, x + width, y + height, FIELD_EDGE);
        graphics.fill(x, y, x + 1, y + height, FIELD_EDGE);
        graphics.fill(x + width - 1, y, x + width, y + height, FIELD_EDGE);

        for (int index = 0; index < entries.size(); index++) {
            WheelEntry wheelEntry = entries.get(index);
            int row = index / 2;
            int column = index % 2;
            int cardX = x + (column * (cardWidth + columnGap));
            int cardY = y + (row * (cardHeight + rowGap)) - entriesScrollOffset;
            if (cardY + cardHeight < y || cardY > y + height) {
                continue;
            }

            boolean selected = wheelEntry.id().equals(selectedEntryId);
            graphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, selected ? ROW_SELECTED : ROW);
            graphics.fill(cardX + 8, cardY + 9, cardX + 20, cardY + 21, wheelEntry.color());
            graphics.drawString(minecraft.font, shorten(displayText(wheelEntry.label()), Math.max(8, (cardWidth - 42) / 6)), cardX + 28, cardY + 7, TEXT, false);
            graphics.drawString(minecraft.font, shorten(actionSummary(wheelEntry.action()), Math.max(8, (cardWidth - 42) / 6)), cardX + 28, cardY + 20, TEXT_SECONDARY, false);
        if (!EntryIconRenderer.drawIcon(graphics, minecraft, wheelEntry.glyph(), cardX + cardWidth - 9.0F, cardY + 19.0F, 0.75F)) {
            graphics.drawString(minecraft.font, wheelEntry.glyph(), cardX + cardWidth - 14, cardY + 14, wheelEntry.color(), false);
        }

            final String entryId = wheelEntry.id();
            actions.add(new ClickAction(cardX, cardY, cardWidth, cardHeight, true, () -> {
                persistPendingEdits();
                selectedEntryId = entryId;
                loadEntryDraft(selectedEntry());
            }));
        }

        if (contentHeight > height) {
            String scroll = text("omniwheel.manager.scroll");
            graphics.drawString(minecraft.font, scroll, x + width - minecraft.font.width(scroll) - 8, y + height - 11, TEXT_SECONDARY, false);
        }
    }

    private void drawReadOnlyField(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String label, String value) {
        graphics.drawString(minecraft.font, label, x, y, TEXT_SECONDARY, false);
        graphics.fill(x, y + 12, x + width, y + 34, FIELD);
        graphics.fill(x, y + 12, x + width, y + 13, FIELD_EDGE);
        graphics.fill(x, y + 33, x + width, y + 34, FIELD_EDGE);
        graphics.fill(x, y + 12, x + 1, y + 34, FIELD_EDGE);
        graphics.fill(x + width - 1, y + 12, x + width, y + 34, FIELD_EDGE);
        graphics.drawString(minecraft.font, shorten(value, Math.max(8, (width - 16) / 6)), x + 8, y + 19, TEXT, false);
    }

    private void drawInlineProfileNameField(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        TextFieldState field = textFields.get(EditorField.PROFILE_NAME);
        field.visible = true;
        field.setBounds(x, y, width, height, 6);

        graphics.fill(x, y, x + width, y + height, FIELD);
        int edgeColor = field.focused ? FIELD_FOCUSED : FIELD_EDGE;
        graphics.fill(x, y, x + width, y + 1, edgeColor);
        graphics.fill(x, y + height - 1, x + width, y + height, edgeColor);
        graphics.fill(x, y, x + 1, y + height, edgeColor);
        graphics.fill(x + width - 1, y, x + width, y + height, edgeColor);

        int maxWidth = Math.max(8, width - 12);
        drawFieldSelection(graphics, minecraft.font, field, x + 6, y + 3, 10, maxWidth);
        String visibleText = field.visibleText(minecraft.font, maxWidth);
        graphics.drawString(minecraft.font, visibleText, x + 6, y + 4, TEXT, false);

        if (field.focused && textEntryActive && ((System.currentTimeMillis() / 500L) & 1L) == 0L) {
            int cursorX = x + 6 + field.cursorOffset(minecraft.font, maxWidth);
            graphics.fill(cursorX, y + 3, cursorX + 1, y + height - 3, ACTION);
        }
    }

    private void drawInlineWheelNameField(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        TextFieldState field = textFields.get(EditorField.WHEEL_TITLE);
        field.visible = true;
        field.setBounds(x, y, width, height, 6);

        graphics.fill(x, y, x + width, y + height, FIELD);
        int edgeColor = field.focused ? FIELD_FOCUSED : FIELD_EDGE;
        graphics.fill(x, y, x + width, y + 1, edgeColor);
        graphics.fill(x, y + height - 1, x + width, y + height, edgeColor);
        graphics.fill(x, y, x + 1, y + height, edgeColor);
        graphics.fill(x + width - 1, y, x + width, y + height, edgeColor);

        int maxWidth = Math.max(8, width - 12);
        drawFieldSelection(graphics, minecraft.font, field, x + 6, y + 3, 10, maxWidth);
        String visibleText = field.visibleText(minecraft.font, maxWidth);
        graphics.drawString(minecraft.font, visibleText, x + 6, y + 4, TEXT, false);

        if (field.focused && textEntryActive && ((System.currentTimeMillis() / 500L) & 1L) == 0L) {
            int cursorX = x + 6 + field.cursorOffset(minecraft.font, maxWidth);
            graphics.fill(cursorX, y + 3, cursorX + 1, y + height - 3, ACTION);
        }
    }

    private void drawTextField(GuiGraphics graphics, Minecraft minecraft, EditorField fieldId, int x, int y, int width, String label, boolean visible) {
        TextFieldState field = textFields.get(fieldId);
        drawTextFieldState(graphics, minecraft, field, x, y, width, label, visible);
    }

    private void drawTextFieldState(GuiGraphics graphics, Minecraft minecraft, TextFieldState field, int x, int y, int width, String label, boolean visible) {
        field.visible = visible;
        field.setBounds(x, y + 12, width, 22, 8);

        graphics.drawString(minecraft.font, label, x, y, TEXT_SECONDARY, false);
        graphics.fill(x, y + 12, x + width, y + 34, FIELD);
        int edgeColor = field.focused ? FIELD_FOCUSED : FIELD_EDGE;
        graphics.fill(x, y + 12, x + width, y + 13, edgeColor);
        graphics.fill(x, y + 33, x + width, y + 34, edgeColor);
        graphics.fill(x, y + 12, x + 1, y + 34, edgeColor);
        graphics.fill(x + width - 1, y + 12, x + width, y + 34, edgeColor);

        int maxWidth = Math.max(8, width - 16);
        drawFieldSelection(graphics, minecraft.font, field, x + 8, y + 18, 12, maxWidth);
        String visibleText = field.visibleText(minecraft.font, maxWidth);
        graphics.drawString(minecraft.font, visibleText, x + 8, y + 19, TEXT, false);

        if (field.focused && textEntryActive && ((System.currentTimeMillis() / 500L) & 1L) == 0L) {
            int cursorX = x + 8 + field.cursorOffset(minecraft.font, maxWidth);
            graphics.fill(cursorX, y + 18, cursorX + 1, y + 29, ACTION);
        }
    }

    private void drawCompactTextFieldState(GuiGraphics graphics, Minecraft minecraft, TextFieldState field, int x, int y, int width, int height, String placeholder, boolean visible) {
        field.visible = visible;
        field.setBounds(x, y, width, height, 8);

        graphics.fill(x, y, x + width, y + height, FIELD);
        int edgeColor = field.focused ? FIELD_FOCUSED : FIELD_EDGE;
        graphics.fill(x, y, x + width, y + 1, edgeColor);
        graphics.fill(x, y + height - 1, x + width, y + height, edgeColor);
        graphics.fill(x, y, x + 1, y + height, edgeColor);
        graphics.fill(x + width - 1, y, x + width, y + height, edgeColor);

        int maxWidth = Math.max(8, width - 16);
        drawFieldSelection(graphics, minecraft.font, field, x + 8, y + 5, Math.max(10, height - 10), maxWidth);
        String visibleText = field.visibleText(minecraft.font, maxWidth);
        if (visibleText.isEmpty() && !field.focused) {
            graphics.drawString(minecraft.font, placeholder, x + 8, y + ((height - 8) / 2), TEXT_SECONDARY, false);
        } else {
            graphics.drawString(minecraft.font, visibleText, x + 8, y + ((height - 8) / 2), TEXT, false);
        }

        if (field.focused && textEntryActive && ((System.currentTimeMillis() / 500L) & 1L) == 0L) {
            int cursorX = x + 8 + field.cursorOffset(minecraft.font, maxWidth);
            graphics.fill(cursorX, y + 5, cursorX + 1, y + height - 5, ACTION);
        }
    }

    private void drawFieldSelection(GuiGraphics graphics, Font font, TextFieldState field, int textX, int selectionY, int selectionHeight, int contentWidth) {
        if (!field.hasSelection()) {
            return;
        }

        int startOffset = field.selectionStartOffset(font, contentWidth);
        int endOffset = field.selectionEndOffset(font, contentWidth);
        if (endOffset <= startOffset) {
            return;
        }
        graphics.fill(textX + startOffset, selectionY, textX + endOffset, selectionY + selectionHeight, 0x90456B89);
    }

    private void addButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String label, boolean enabled, Runnable handler) {
        addButton(graphics, minecraft, x, y, width, label, 24, enabled, handler);
    }

    private void addButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String label, int height, boolean enabled, Runnable handler) {
        boolean focused = focusedActionIndex == actions.size();
        boolean highlighted = isButtonHighlighted(minecraft, x, y, width, height, focused);
        drawButtonChrome(graphics, x, y, width, height, ROW, ROW_SELECTED, enabled, highlighted);
        graphics.drawCenteredString(minecraft.font, label, x + (width / 2), y + ((height - 8) / 2), enabled ? (highlighted ? TEXT : ACTION) : ACTION_DISABLED);
        actions.add(new ClickAction(x, y, width, height, enabled, handler));
    }

    private boolean isButtonHighlighted(Minecraft minecraft, int x, int y, int width, int height, boolean focused) {
        if (isManagerTutorialActive()) {
            return false;
        }
        return focused || contains(currentMouseX(minecraft), currentMouseY(minecraft), x, y, width, height);
    }

    private void drawButtonChrome(GuiGraphics graphics, int x, int y, int width, int height, int normalFill, int highlightedFill, boolean enabled, boolean highlighted) {
        int fillColor = enabled ? (highlighted ? highlightedFill : normalFill) : ROW_DISABLED;
        int edgeColor = enabled && highlighted ? FIELD_FOCUSED : FIELD_EDGE;
        graphics.fill(x, y, x + width, y + height, fillColor);
        graphics.fill(x, y, x + width, y + 1, edgeColor);
        graphics.fill(x, y + height - 1, x + width, y + height, edgeColor);
        graphics.fill(x, y, x + 1, y + height, edgeColor);
        graphics.fill(x + width - 1, y, x + width, y + height, edgeColor);
        if (enabled && highlighted) {
            drawFocusedActionOutline(graphics, x, y, width, height);
        }
    }

    private void drawFocusedActionOutline(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, FIELD_FOCUSED);
        graphics.fill(x, y + height - 1, x + width, y + height, FIELD_FOCUSED);
        graphics.fill(x, y, x + 1, y + height, FIELD_FOCUSED);
        graphics.fill(x + width - 1, y, x + width, y + height, FIELD_FOCUSED);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, 0x30000000);
        graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, 0x30000000);
    }

    private void runClickAction(float mouseX, float mouseY) {
        for (ClickAction action : actions) {
            if (action.enabled && action.contains(mouseX, mouseY)) {
                action.handler.run();
                syncSelection(false);
                return;
            }
        }
    }

    private void syncSelection(boolean forceReloadDrafts) {
        WheelProfile profile = selectedProfile();
        if (profile == null) {
            selectedProfileId = runtime.activeProfile().id();
            profile = selectedProfile();
        }
        if (profile == null) {
            selectedWheelId = null;
            selectedEntryId = null;
            return;
        }

        if (selectedWheelId == null || !profile.wheels().containsKey(selectedWheelId)) {
            selectedWheelId = profile.rootWheelId();
            forceReloadDrafts = true;
        }

        WheelDefinition wheel = profile.wheel(selectedWheelId);
        if (selectedEntryId != null && wheel.entries().stream().noneMatch(entry -> entry.id().equals(selectedEntryId))) {
            selectedEntryId = null;
            forceReloadDrafts = true;
        }
        if (selectedEntryId == null && !wheel.entries().isEmpty()) {
            selectedEntryId = wheel.entries().getFirst().id();
            forceReloadDrafts = true;
        }

        if (forceReloadDrafts) {
            loadWheelDraft(wheel);
            loadEntryDraft(selectedEntry());
        }

        if (focusedCommandFieldIndex >= 0 && (entryActionTypeDraft != DraftActionType.COMMAND || focusedCommandFieldIndex >= commandValueFields.size())) {
            clearFocus();
        }
        if (focusedField != null && !visibleEditableFields().contains(focusedField)) {
            clearFocus();
        }
    }

    private void loadWheelDraft(WheelDefinition wheel) {
        wheelTitleDraft = displayText(wheel.title());
        textFields.get(EditorField.WHEEL_TITLE).setValue(wheelTitleDraft);
    }

    private void loadEntryDraft(WheelEntry entry) {
        if (entry == null) {
            entryLabelDraft = "";
            entryDescriptionDraft = "";
            entryShortcutDraft = "";
            entryGlyphDraft = "";
            entryValueDraft = "";
            commandValueDrafts.clear();
            commandValueFields.clear();
            entryColorDraft = WheelEntry.DEFAULT_COLOR;
            entryGuiColorDraft = WheelEntry.DEFAULT_GUI_COLOR;
            entryShowIconDraft = true;
            entryShowLabelDraft = true;
            entryShowShortcutDraft = true;
            entryActionTypeDraft = DraftActionType.COMMAND;
            entryFunctionDraft = GameplayFunction.OPEN_INVENTORY;
            entrySubmenuWheelId = null;
            entryScreenTargetDraft = ScreenTarget.PROFILE_MANAGER;
            textFields.get(EditorField.ENTRY_LABEL).setValue("");
            textFields.get(EditorField.ENTRY_DESCRIPTION).setValue("");
            textFields.get(EditorField.ENTRY_SHORTCUT).setValue("");
            textFields.get(EditorField.ENTRY_GLYPH).setValue("");
            textFields.get(EditorField.ENTRY_VALUE).setValue("");
            syncActiveCommandInput();
            return;
        }

        entryLabelDraft = displayText(entry.label());
        entryDescriptionDraft = displayText(entry.description());
        entryShortcutDraft = entry.shortcut();
        entryGlyphDraft = entry.glyph();
        entryColorDraft = entry.color();
        entryGuiColorDraft = entry.guiColor();
        entryShowIconDraft = entry.showIcon();
        entryShowLabelDraft = entry.showLabel();
        entryShowShortcutDraft = entry.showShortcut();
        entryActionTypeDraft = DraftActionType.fromAction(entry.action());
        entryFunctionDraft = entry.action() instanceof FunctionAction functionAction ? functionAction.function() : GameplayFunction.OPEN_INVENTORY;
        entrySubmenuWheelId = entry.action() instanceof OpenWheelAction openWheelAction ? openWheelAction.wheelId() : null;
        entryScreenTargetDraft = entry.action() instanceof OpenScreenAction openScreenAction ? openScreenAction.target() : ScreenTarget.PROFILE_MANAGER;
        entryValueDraft = switch (entry.action()) {
            case CommandAction commandAction -> formatCommandValues(commandAction.commands());
            case ChatAction chatAction -> displayText(chatAction.message());
            case FunctionAction functionAction -> functionAction.function().displayName();
            case CopyTextAction copyTextAction -> copyTextAction.text();
            case LocalMessageAction localMessageAction -> displayText(localMessageAction.message());
            case OpenChatAction openChatAction -> openChatAction.initialText();
            case OpenScreenAction ignored -> entryScreenTargetDraft.name();
            default -> "";
        };
        commandValueDrafts.clear();
        if (entry.action() instanceof CommandAction commandAction) {
            commandValueDrafts.addAll(commandAction.commands());
        }
        syncCommandValueFields();
        textFields.get(EditorField.ENTRY_LABEL).setValue(entryLabelDraft);
        textFields.get(EditorField.ENTRY_DESCRIPTION).setValue(entryDescriptionDraft);
        textFields.get(EditorField.ENTRY_SHORTCUT).setValue(entryShortcutDraft);
        textFields.get(EditorField.ENTRY_GLYPH).setValue(entryGlyphDraft);
        textFields.get(EditorField.ENTRY_VALUE).setValue(entryValueDraft);
        syncActiveCommandInput();
    }

    private void createWheel() {
        WheelProfile profile = selectedProfile();
        if (profile == null) {
            return;
        }

        String wheelId = nextWheelId(profile, "custom_wheel");
        WheelDefinition wheel = new WheelDefinition(wheelId, text("omniwheel.manager.created.wheel"), "", 8, List.of());
        persistProfile(withWheel(profile, wheel));
        selectedWheelId = wheelId;
        selectedEntryId = null;
        syncSelection(true);
        renameSelectedWheel();
    }

    private void saveCurrentWheel() {
        WheelProfile profile = selectedProfile();
        WheelDefinition currentWheel = selectedWheel();
        if (profile == null || currentWheel == null) {
            return;
        }

        List<WheelEntry> entries = currentWheel.entries();
        WheelDefinition updatedWheel = new WheelDefinition(
                currentWheel.id(),
                preservedDraftText(wheelTitleDraft, currentWheel.title()),
                currentWheel.description(),
                storedSegmentCount(entries.size()),
                entries,
                currentWheel.active()
        );
        persistProfile(withWheel(profile, updatedWheel));
        loadWheelDraft(updatedWheel);
    }

    private void autosaveCurrentWheelDraft() {
        WheelDefinition wheel = selectedWheel();
        if (wheel != null && isWheelDraftDirty(wheel)) {
            saveCurrentWheel();
        }
    }

    private void deleteSelectedWheel() {
        WheelProfile profile = selectedProfile();
        if (profile == null || selectedWheelId == null || !canDeleteSelectedWheel()) {
            return;
        }

        Map<String, WheelDefinition> wheels = new LinkedHashMap<>(profile.wheels());
        wheels.remove(selectedWheelId);
        persistProfile(new WheelProfile(profile.id(), profile.displayName(), profile.rootWheelId(), wheels));
        selectedWheelId = profile.rootWheelId();
        selectedEntryId = null;
        syncSelection(true);
    }

    private void createCommandEntry() {
        createEntry(DraftActionType.COMMAND);
    }

    private void createChatEntry() {
        createEntry(DraftActionType.CHAT);
    }

    private void createFunctionEntry() {
        createEntry(DraftActionType.FUNCTION);
    }

    private void createSubmenuEntry() {
        WheelProfile profile = selectedProfile();
        WheelDefinition wheel = activeWheelContext();
        if (profile == null || wheel == null || !canCreateEntry()) {
            return;
        }

        String submenuId = nextWheelId(profile, "submenu");
        WheelDefinition submenu = new WheelDefinition(submenuId, text("omniwheel.manager.created.submenu_wheel"), "", 8, List.of());
        WheelEntry entry = new WheelEntry(
                nextEntryId(wheel),
                text("omniwheel.manager.created.submenu_entry"),
                text("omniwheel.manager.created.submenu_entry.description"),
                "S",
                0xFF8EE1FF,
                new OpenWheelAction(submenuId)
        );

        WheelProfile updatedProfile = withWheel(withEntry(profile, wheel.id(), entry), submenu);
        persistProfile(updatedProfile);
        selectedWheelId = wheel.id();
        selectedEntryId = entry.id();
        syncSelection(true);
        focusField(EditorField.ENTRY_LABEL);
    }

    private void createEntry(DraftActionType actionType) {
        WheelDefinition wheel = activeWheelContext();
        WheelProfile profile = selectedProfile();
        if (profile == null || wheel == null || !canCreateEntry()) {
            return;
        }

        WheelAction action = switch (actionType) {
            case COMMAND -> new CommandAction("/command", false);
            case CHAT -> new ChatAction(text("omniwheel.manager.created.chat_value"), false);
            case FUNCTION -> new FunctionAction(GameplayFunction.OPEN_INVENTORY);
            default -> new CommandAction("/command", false);
        };
        WheelEntry entry = new WheelEntry(
                nextEntryId(wheel),
                switch (actionType) {
                    case COMMAND -> text("omniwheel.manager.created.command_entry");
                    case CHAT -> text("omniwheel.manager.created.message_entry");
                    case FUNCTION -> text("omniwheel.manager.created.function_entry");
                    default -> text("omniwheel.manager.created.entry");
                },
                "",
                switch (actionType) {
                    case COMMAND -> "C";
                    case CHAT -> "M";
                    case FUNCTION -> "F";
                    default -> "E";
                },
                WheelEntry.DEFAULT_COLOR,
                action
        );

        persistProfile(withEntry(profile, wheel.id(), entry));
        selectedEntryId = entry.id();
        syncSelection(true);
        focusField(EditorField.ENTRY_LABEL);
    }

    private void saveCurrentEntry() {
        WheelProfile profile = selectedProfile();
        WheelDefinition wheel = selectedWheel();
        WheelEntry currentEntry = selectedEntry();
        if (profile == null || wheel == null || currentEntry == null) {
            return;
        }

        WheelEntry updatedEntry = new WheelEntry(
                currentEntry.id(),
                preservedDraftText(entryLabelDraft, currentEntry.label()),
                preservedDraftText(entryDescriptionDraft, currentEntry.description()),
                fallbackText(iconFromText(entryGlyphDraft), currentEntry.glyph()),
                entryColorDraft,
                entryGuiColorDraft,
                buildDraftAction(currentEntry.action()),
                currentEntry.active(),
                entryShowIconDraft,
                entryShowLabelDraft,
                entryShowShortcutDraft,
                entryShortcutDraft.trim()
        );

        persistProfile(withEntry(profile, wheel.id(), updatedEntry));
        loadEntryDraft(updatedEntry);
    }

    private void deleteSelectedEntry() {
        WheelProfile profile = selectedProfile();
        WheelDefinition wheel = selectedWheel();
        WheelEntry currentEntry = selectedEntry();
        if (profile == null || wheel == null || currentEntry == null) {
            return;
        }

        List<WheelEntry> updatedEntries = new ArrayList<>(wheel.entries());
        updatedEntries.removeIf(entry -> entry.id().equals(currentEntry.id()));
        WheelDefinition updatedWheel = new WheelDefinition(
                wheel.id(),
                wheel.title(),
                wheel.description(),
                storedSegmentCount(updatedEntries.size()),
                updatedEntries,
                wheel.active()
        );
        persistProfile(withWheel(profile, updatedWheel));
        selectedEntryId = updatedEntries.isEmpty() ? null : updatedEntries.getFirst().id();
        syncSelection(true);
    }

    private void cycleEntryType() {
        entryActionTypeDraft = entryActionTypeDraft.next();
        if (entryActionTypeDraft == DraftActionType.SUBMENU && entrySubmenuWheelId == null) {
            entryActionTypeDraft = DraftActionType.COMMAND;
        }
    }

    private WheelAction buildDraftAction(WheelAction currentAction) {
        return switch (entryActionTypeDraft) {
            case COMMAND -> new CommandAction(normalizedCommandDrafts(), false);
            case CHAT -> new ChatAction(preservedActionValue(currentAction, ChatAction.class, entryValueDraft, text("omniwheel.manager.created.chat_value")), false);
            case FUNCTION -> new FunctionAction(entryFunctionDraft);
            case LOCAL_MESSAGE -> new LocalMessageAction(preservedActionValue(currentAction, LocalMessageAction.class, entryValueDraft, ""));
            case OPEN_CHAT -> new OpenChatAction(fallbackText(entryValueDraft, ""));
            case COPY_TEXT -> new CopyTextAction(preservedActionValue(currentAction, CopyTextAction.class, entryValueDraft, ""));
            case OPEN_SCREEN -> new OpenScreenAction(entryScreenTargetDraft);
            case SUBMENU -> new OpenWheelAction(Objects.requireNonNullElse(entrySubmenuWheelId, selectedWheelId));
        };
    }

    public void persistPendingEdits() {
        commitProfileRename();
        commitWheelRename();
        WheelEntry entry = selectedEntry();
        if (entry != null && isEntryDraftDirty(entry)) {
            saveCurrentEntry();
        }
    }

    private void autosaveCurrentEntryDraft() {
        WheelEntry entry = selectedEntry();
        if (entry != null && isEntryDraftDirty(entry)) {
            saveCurrentEntry();
        }
    }

    private WheelProfile withWheel(WheelProfile profile, WheelDefinition updatedWheel) {
        Map<String, WheelDefinition> wheels = new LinkedHashMap<>();
        boolean inserted = false;
        for (Map.Entry<String, WheelDefinition> entry : profile.wheels().entrySet()) {
            if (entry.getKey().equals(updatedWheel.id())) {
                wheels.put(entry.getKey(), updatedWheel);
                inserted = true;
            } else {
                wheels.put(entry.getKey(), entry.getValue());
            }
        }
        if (!inserted) {
            wheels.put(updatedWheel.id(), updatedWheel);
        }
        return new WheelProfile(profile.id(), profile.displayName(), profile.rootWheelId(), wheels);
    }

    private WheelProfile withWheelOrder(WheelProfile profile, List<WheelDefinition> orderedWheels) {
        Map<String, WheelDefinition> wheels = new LinkedHashMap<>();
        for (WheelDefinition wheel : orderedWheels) {
            wheels.put(wheel.id(), wheel);
        }
        return new WheelProfile(profile.id(), profile.displayName(), profile.rootWheelId(), wheels);
    }

    private WheelProfile withEntry(WheelProfile profile, String wheelId, WheelEntry updatedEntry) {
        WheelDefinition wheel = profile.wheel(wheelId);
        List<WheelEntry> entries = new ArrayList<>(wheel.entries());
        boolean replaced = false;
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).id().equals(updatedEntry.id())) {
                entries.set(index, updatedEntry);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            entries.add(updatedEntry);
        }

        WheelDefinition updatedWheel = new WheelDefinition(
                wheel.id(),
                wheel.title(),
                wheel.description(),
                storedSegmentCount(entries.size()),
                entries,
                wheel.active()
        );
        return withWheel(profile, updatedWheel);
    }

    private void persistProfile(WheelProfile updatedProfile) {
        runtime.profileManager().saveProfile(updatedProfile);
        syncSelection(false);
    }

    private boolean canCreateEntry() {
        return activeWheelContext() != null;
    }

    private boolean canRenameSelectedProfile() {
        return selectedProfileId != null;
    }

    private boolean canDeleteSelectedProfile() {
        return selectedProfileId != null
                && !"default".equals(selectedProfileId)
                && runtime.profileManager().getProfiles().size() > 1;
    }

    private void renameSelectedProfile() {
        WheelProfile profile = selectedProfile();
        if (profile == null) {
            return;
        }
        renamingProfileId = profile.id();
        profileNameDraft = displayText(profile.displayName());
        textFields.get(EditorField.PROFILE_NAME).setValue(profileNameDraft);
        focusField(EditorField.PROFILE_NAME);
        activateTextEntry();
    }

    private boolean canDeleteSelectedWheel() {
        WheelProfile profile = selectedProfile();
        if (profile == null || selectedWheelId == null || selectedWheelId.equals(profile.rootWheelId())) {
            return false;
        }

        for (WheelDefinition wheel : profile.wheels().values()) {
            for (WheelEntry entry : wheel.entries()) {
                if (entry.action() instanceof OpenWheelAction openWheelAction && openWheelAction.wheelId().equals(selectedWheelId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean canToggleSelectedWheel() {
        WheelProfile profile = selectedProfile();
        return profile != null
                && selectedWheelId != null
                && !selectedWheelId.equals(profile.rootWheelId())
                && selectedWheel() != null;
    }

    private boolean canDeleteSelection() {
        return selectedEntry() != null || canDeleteSelectedWheel();
    }

    private void deleteSelection() {
        if (selectedEntry() != null) {
            deleteSelectedEntry();
            return;
        }
        deleteSelectedWheel();
    }

    private boolean canToggleSelection() {
        return selectedEntry() != null || canToggleSelectedWheel();
    }

    private String selectedThingToggleLabel() {
        WheelEntry entry = selectedEntry();
        if (entry != null) {
            return text(entry.active() ? "omniwheel.common.deactivate" : "omniwheel.common.activate");
        }
        return selectedWheelToggleLabel();
    }

    private void toggleSelectionActive() {
        if (selectedEntry() != null) {
            toggleSelectedEntryActive();
            return;
        }
        toggleSelectedWheelActive();
    }

    private void toggleSelectedEntryActive() {
        WheelProfile profile = selectedProfile();
        WheelDefinition wheel = selectedWheel();
        WheelEntry entry = selectedEntry();
        if (profile == null || wheel == null || entry == null) {
            return;
        }

        WheelEntry updatedEntry = new WheelEntry(
                entry.id(),
                entry.label(),
                entry.description(),
                entry.glyph(),
                entry.color(),
                entry.guiColor(),
                entry.action(),
                !entry.active(),
                entry.showIcon(),
                entry.showLabel(),
                entry.showShortcut(),
                entry.shortcut()
        );
        persistProfile(withEntry(profile, wheel.id(), updatedEntry));
        if (!updatedEntry.active() && updatedEntry.action() instanceof OpenWheelAction openWheelAction) {
            expandedWheelIds.remove(openWheelAction.wheelId());
        }
        loadEntryDraft(updatedEntry);
        syncSelection(true);
    }

    private String selectedWheelToggleLabel() {
        WheelDefinition wheel = selectedWheel();
        if (wheel == null) {
            return text("omniwheel.common.deactivate");
        }
        return text(wheel.active() ? "omniwheel.common.deactivate" : "omniwheel.common.activate");
    }

    private void renameSelectedWheel() {
        WheelDefinition wheel = selectedWheel();
        if (wheel == null) {
            return;
        }
        renamingWheelId = wheel.id();
        wheelTitleDraft = displayText(wheel.title());
        textFields.get(EditorField.WHEEL_TITLE).setValue(wheelTitleDraft);
        focusField(EditorField.WHEEL_TITLE);
        activateTextEntry();
    }

    private void toggleSelectedWheelActive() {
        WheelProfile profile = selectedProfile();
        WheelDefinition wheel = selectedWheel();
        if (profile == null || wheel == null || !canToggleSelectedWheel()) {
            return;
        }

        WheelDefinition updatedWheel = new WheelDefinition(
                wheel.id(),
                wheel.title(),
                wheel.description(),
                storedSegmentCount(wheel.entries().size()),
                wheel.entries(),
                !wheel.active()
        );
        persistProfile(withWheel(profile, updatedWheel));
        syncSelection(true);
    }

    private WheelProfile selectedProfile() {
        if (selectedProfileId == null) {
            return null;
        }
        return runtime.profileManager().getProfiles().stream()
                .filter(profile -> profile.id().equals(selectedProfileId))
                .findFirst()
                .orElse(null);
    }

    private List<WheelEntry> currentWheelEntries() {
        WheelDefinition wheel = selectedWheel();
        return wheel == null ? List.of() : wheel.entries();
    }

    private String activeWheelContextId() {
        WheelProfile profile = selectedProfile();
        WheelEntry entry = selectedEntry();
        if (profile != null
                && entry != null
                && entry.action() instanceof OpenWheelAction openWheelAction
                && expandedWheelIds.contains(openWheelAction.wheelId())
                && profile.wheels().containsKey(openWheelAction.wheelId())) {
            return openWheelAction.wheelId();
        }
        return selectedWheelId;
    }

    private WheelDefinition activeWheelContext() {
        WheelProfile profile = selectedProfile();
        String wheelId = activeWheelContextId();
        if (profile == null || wheelId == null || !profile.wheels().containsKey(wheelId)) {
            return null;
        }
        return profile.wheel(wheelId);
    }

    private List<EntryListRow> visibleEntryRows() {
        WheelProfile profile = selectedProfile();
        if (profile == null) {
            return List.of();
        }

        List<EntryListRow> rows = new ArrayList<>();
        buildVisibleEntryRows(profile, profile.rootWheelId(), 0, new HashSet<>(), rows);
        return rows;
    }

    private void buildVisibleEntryRows(WheelProfile profile, String wheelId, int depth, Set<String> path, List<EntryListRow> rows) {
        if (!path.add(wheelId) || !profile.wheels().containsKey(wheelId)) {
            return;
        }

        WheelDefinition wheel = profile.wheel(wheelId);
        for (WheelEntry entry : wheel.entries()) {
            boolean expandable = entry.action() instanceof OpenWheelAction openWheelAction
                    && profile.wheels().containsKey(openWheelAction.wheelId());
            boolean expanded = expandable
                    && entry.action() instanceof OpenWheelAction openWheelAction
                    && expandedWheelIds.contains(openWheelAction.wheelId());
            rows.add(new EntryListRow(entry, wheelId, depth, expandable, expanded));
            if (expanded && entry.action() instanceof OpenWheelAction openWheelAction) {
                buildVisibleEntryRows(profile, openWheelAction.wheelId(), depth + 1, new HashSet<>(path), rows);
            }
        }
    }

    private EntryListRow draggedEntryRow() {
        if (draggedWheelId == null || draggedWheelOwnerWheelId == null) {
            return null;
        }
        for (EntryListRow row : visibleEntryRows()) {
            if (row.ownerWheelId().equals(draggedWheelOwnerWheelId) && row.entry().id().equals(draggedWheelId)) {
                return row;
            }
        }
        WheelProfile profile = selectedProfile();
        if (profile == null || !profile.wheels().containsKey(draggedWheelOwnerWheelId)) {
            return null;
        }
        WheelDefinition owner = profile.wheel(draggedWheelOwnerWheelId);
        return owner.entries().stream()
                .filter(entry -> entry.id().equals(draggedWheelId))
                .findFirst()
                .map(entry -> new EntryListRow(entry, draggedWheelOwnerWheelId, 0, false, false))
                .orElse(null);
    }

    private boolean canOpenSelectedSubmenu() {
        WheelEntry entry = selectedEntry();
        return entry != null
                && entry.action() instanceof OpenWheelAction openWheelAction
                && selectedProfile() != null
                && selectedProfile().wheels().containsKey(openWheelAction.wheelId());
    }

    private void openSelectedSubmenu() {
        if (!canOpenSelectedSubmenu()) {
            return;
        }
        OpenWheelAction action = (OpenWheelAction) Objects.requireNonNull(selectedEntry()).action();
        persistPendingEdits();
        clearFocus();
        selectedWheelId = action.wheelId();
        selectedEntryId = null;
        syncSelection(true);
        activeList = NavigationList.WHEELS;
    }

    private boolean canReturnToParentWheel() {
        return parentWheelId() != null;
    }

    private void selectParentWheel() {
        String parentWheelId = parentWheelId();
        if (parentWheelId == null) {
            return;
        }
        persistPendingEdits();
        clearFocus();
        selectedWheelId = parentWheelId;
        selectedEntryId = null;
        syncSelection(true);
        activeList = NavigationList.WHEELS;
    }

    private String parentWheelId() {
        WheelProfile profile = selectedProfile();
        if (profile == null || selectedWheelId == null || selectedWheelId.equals(profile.rootWheelId())) {
            return null;
        }

        for (WheelDefinition wheel : profile.wheels().values()) {
            for (WheelEntry entry : wheel.entries()) {
                if (entry.action() instanceof OpenWheelAction openWheelAction && openWheelAction.wheelId().equals(selectedWheelId)) {
                    return wheel.id();
                }
            }
        }
        return profile.rootWheelId().equals(selectedWheelId) ? null : profile.rootWheelId();
    }

    private String buildWheelPathLabel(String wheelId) {
        WheelProfile profile = selectedProfile();
        if (profile == null || wheelId == null) {
            return "";
        }

        List<String> titles = new ArrayList<>();
        String currentId = wheelId;
        while (currentId != null) {
            WheelDefinition wheel = profile.wheels().get(currentId);
            if (wheel == null) {
                break;
            }
            titles.add(0, displayText(wheel.title()));
            currentId = currentId.equals(profile.rootWheelId()) ? null : parentWheelIdFor(profile, currentId);
        }
        return String.join(" > ", titles);
    }

    private String parentWheelIdFor(WheelProfile profile, String wheelId) {
        if (wheelId.equals(profile.rootWheelId())) {
            return null;
        }
        for (WheelDefinition wheel : profile.wheels().values()) {
            for (WheelEntry entry : wheel.entries()) {
                if (entry.action() instanceof OpenWheelAction openWheelAction && openWheelAction.wheelId().equals(wheelId)) {
                    return wheel.id();
                }
            }
        }
        return profile.rootWheelId();
    }

    private boolean isEntryRowSelected(String ownerWheelId, String entryId) {
        return Objects.equals(selectedWheelId, ownerWheelId) && Objects.equals(selectedEntryId, entryId);
    }

    private boolean isWheelInSubtree(WheelProfile profile, String wheelId, String subtreeRootWheelId) {
        if (profile == null || wheelId == null || subtreeRootWheelId == null || !profile.wheels().containsKey(wheelId)) {
            return false;
        }
        String currentId = wheelId;
        while (currentId != null) {
            if (currentId.equals(subtreeRootWheelId)) {
                return true;
            }
            currentId = parentWheelIdFor(profile, currentId);
        }
        return false;
    }

    private void toggleExpandedSubmenu(String ownerWheelId, WheelEntry entry, String submenuWheelId) {
        WheelProfile profile = selectedProfile();
        if (profile == null) {
            return;
        }

        boolean expanded = expandedWheelIds.contains(submenuWheelId);
        if (expanded) {
            expandedWheelIds.remove(submenuWheelId);
            if (isWheelInSubtree(profile, selectedWheelId, submenuWheelId)) {
                selectedWheelId = ownerWheelId;
                selectedEntryId = entry.id();
                loadEntryDraft(selectedEntry());
            }
            syncSelection(false);
            return;
        }

        expandedWheelIds.add(submenuWheelId);
        selectedWheelId = ownerWheelId;
        selectedEntryId = entry.id();
        loadEntryDraft(entry);
        syncSelection(false);
    }

    private WheelProfile profileById(String profileId) {
        if (profileId == null) {
            return null;
        }
        return runtime.profileManager().getProfiles().stream()
                .filter(profile -> profile.id().equals(profileId))
                .findFirst()
                .orElse(null);
    }

    private boolean isRenamingProfile() {
        return renamingProfileId != null;
    }

    private boolean isRenamingProfile(String profileId) {
        return renamingProfileId != null && renamingProfileId.equals(profileId);
    }

    private boolean isRenamingWheel() {
        return renamingWheelId != null;
    }

    private boolean isRenamingWheel(String wheelId) {
        return renamingWheelId != null && renamingWheelId.equals(wheelId);
    }

    private void commitProfileRename() {
        if (renamingProfileId == null) {
            return;
        }

        WheelProfile profile = profileById(renamingProfileId);
        String nextDisplayName = profile == null
                ? fallbackText(profileNameDraft == null ? "" : profileNameDraft.trim(), text("omniwheel.manager.created.profile"))
                : preservedDraftText(profileNameDraft, profile.displayName());
        if (profile != null && !nextDisplayName.equals(profile.displayName())) {
            WheelProfile updatedProfile = runtime.profileManager().renameProfile(renamingProfileId, nextDisplayName);
            selectedProfileId = updatedProfile.id();
        }

        renamingProfileId = null;
        profileNameDraft = "";
        TextFieldState profileNameField = textFields.get(EditorField.PROFILE_NAME);
        profileNameField.setValue("");
        if (focusedField == EditorField.PROFILE_NAME) {
            profileNameField.focused = false;
            focusedField = null;
        }
        syncSelection(true);
    }

    private void commitWheelRename() {
        if (renamingWheelId == null) {
            return;
        }

        WheelProfile profile = selectedProfile();
        WheelDefinition wheel = selectedWheel();
        if (profile != null && wheel != null && wheel.id().equals(renamingWheelId)) {
            String nextTitle = preservedDraftText(wheelTitleDraft, wheel.title());
            if (!nextTitle.equals(wheel.title())) {
                WheelDefinition updatedWheel = new WheelDefinition(
                        wheel.id(),
                        nextTitle,
                        wheel.description(),
                        storedSegmentCount(wheel.entries().size()),
                        wheel.entries(),
                        wheel.active()
                );
                persistProfile(withWheel(profile, updatedWheel));
                loadWheelDraft(updatedWheel);
            }
        }

        renamingWheelId = null;
        if (focusedField == EditorField.WHEEL_TITLE) {
            textFields.get(EditorField.WHEEL_TITLE).focused = false;
            focusedField = null;
        }
        syncSelection(true);
    }

    private WheelDefinition selectedWheel() {
        WheelProfile profile = selectedProfile();
        if (profile == null || selectedWheelId == null || !profile.wheels().containsKey(selectedWheelId)) {
            return null;
        }
        return profile.wheel(selectedWheelId);
    }

    private WheelEntry selectedEntry() {
        WheelDefinition wheel = selectedWheel();
        if (wheel == null || selectedEntryId == null) {
            return null;
        }
        return wheel.entries().stream()
                .filter(entry -> entry.id().equals(selectedEntryId))
                .findFirst()
                .orElse(null);
    }

    private String submenuTargetLabel(WheelProfile profile) {
        if (entrySubmenuWheelId == null) {
            return text("omniwheel.manager.no_linked_wheel");
        }
        WheelDefinition target = profile.wheels().get(entrySubmenuWheelId);
        return target != null ? displayText(target.title()) + " (" + target.id() + ")" : entrySubmenuWheelId;
    }

    private String nextWheelId(WheelProfile profile, String prefix) {
        int index = 1;
        while (profile.wheels().containsKey(prefix + "_" + index)) {
            index++;
        }
        return prefix + "_" + index;
    }

    private String nextEntryId(WheelDefinition wheel) {
        int index = 1;
        while (hasEntryId(wheel, "entry_" + index)) {
            index++;
        }
        return "entry_" + index;
    }

    private int siblingEntryIndex(String wheelId, String entryId) {
        WheelProfile profile = selectedProfile();
        if (profile == null || wheelId == null || entryId == null || !profile.wheels().containsKey(wheelId)) {
            return -1;
        }
        List<WheelEntry> entries = profile.wheel(wheelId).entries();
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).id().equals(entryId)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean hasEntryId(WheelDefinition wheel, String entryId) {
        for (WheelEntry entry : wheel.entries()) {
            if (entry.id().equals(entryId)) {
                return true;
            }
        }
        return false;
    }

    private String clipboardValue() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.keyboardHandler.getClipboard().replace("\r", "").replace("\n", " ").trim();
    }

    private String trimmedClipboard() {
        return clipboardValue().trim();
    }

    private static String iconFromText(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.length() <= 2 || EntryIconRenderer.isVisualIcon(trimmed) || looksLikeResourceLocationCandidate(trimmed)) {
            return trimmed;
        }
        return clampPlainTextIcon(trimmed);
    }

    private static boolean looksLikeResourceLocationCandidate(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        boolean structured = text.indexOf(':') >= 0 || text.indexOf('_') >= 0 || text.indexOf('/') >= 0 || text.indexOf('.') >= 0 || text.indexOf('-') >= 0;
        if (!structured) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            boolean valid = character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9'
                    || character == '_'
                    || character == ':'
                    || character == '/'
                    || character == '.'
                    || character == '-';
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    private static String clampPlainTextIcon(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.length() <= 2 ? text : text.substring(0, 2);
    }

    private void toggleIconPicker() {
        if (iconPickerOpen) {
            closeIconPicker();
            return;
        }
        ensureIconPickerOptions();
        iconPickerSearchDraft = "";
        textFields.get(EditorField.ICON_PICKER_SEARCH).setValue("");
        if (commandInput != null) {
            commandInput.setFocused(false);
            commandInput.setVisible(false);
        }
        if (hostScreen() != null && hostScreen().getFocused() == commandInput) {
            hostScreen().setFocused(null);
        }
        iconPickerOpen = true;
        clearFocus();
        focusField(EditorField.ICON_PICKER_SEARCH);
        activateTextEntry();
        scrollIconPickerToSelection();
    }

    private void closeIconPicker() {
        if (focusedField == EditorField.ICON_PICKER_SEARCH) {
            clearFocus();
        }
        iconPickerOpen = false;
        draggingIconPickerScrollbar = false;
        iconPickerScrollbarDragOffset = 0;
        iconPickerX = 0;
        iconPickerY = 0;
        iconPickerWidth = 0;
        iconPickerHeight = 0;
        iconPickerBackButtonX = 0;
        iconPickerBackButtonY = 0;
        iconPickerBackButtonWidth = 0;
        iconPickerBackButtonHeight = 0;
        iconPickerListWidth = 0;
        iconPickerScrollbarX = 0;
        iconPickerScrollbarY = 0;
        iconPickerScrollbarHeight = 0;
        iconPickerScrollbarThumbY = 0;
        iconPickerScrollbarThumbHeight = 0;
        iconPickerGridX = 0;
        iconPickerGridY = 0;
        iconPickerGridWidth = 0;
        iconPickerGridHeight = 0;
        iconPickerScrollOffset = 0;
        iconPickerSearchDraft = "";
        pendingIconPickerFocusId = null;
        textFields.get(EditorField.ICON_PICKER_SEARCH).setValue("");
    }

    private boolean handleIconPickerClick(float mouseX, float mouseY) {
        if (mouseX < iconPickerX || mouseX > iconPickerX + iconPickerWidth || mouseY < iconPickerY || mouseY > iconPickerY + iconPickerHeight) {
            return false;
        }
        TextFieldState searchField = textFields.get(EditorField.ICON_PICKER_SEARCH);
        if (searchField.contains(mouseX, mouseY)) {
            focusField(EditorField.ICON_PICKER_SEARCH);
            searchField.click(Minecraft.getInstance().font, mouseX);
            applyFieldDraft(EditorField.ICON_PICKER_SEARCH, searchField.value);
            return true;
        }
        for (int index = actions.size() - 1; index >= 0; index--) {
            ClickAction action = actions.get(index);
            if (action.enabled && action.contains(mouseX, mouseY)) {
                action.handler.run();
                syncSelection(false);
                return true;
            }
        }
        List<IconPickerOption> visibleOptions = filteredIconPickerOptions();
        int optionIndex = iconPickerOptionIndexAt(mouseX, mouseY);
        if (optionIndex >= 0 && optionIndex < visibleOptions.size()) {
            selectIconFromPicker(visibleOptions.get(optionIndex).id());
            syncSelection(false);
        }
        return true;
    }

    private void ensureIconPickerOptions() {
        if (!iconPickerOptions.isEmpty()) {
            return;
        }
        List<IconPickerOption> options = new ArrayList<>();
        options.addAll(loadParticleIconOptions());
        for (ResourceLocation id : BuiltInRegistries.MOB_EFFECT.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList()) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(null);
            if (effect == null) {
                continue;
            }
            String iconId = "effect:" + id;
            String displayName = Component.translatable(effect.getDescriptionId()).getString();
            String searchText = (iconId + " " + displayName).toLowerCase(java.util.Locale.ROOT);
            options.add(new IconPickerOption(iconId, searchText));
        }
        options.add(new IconPickerOption("symbol:plus", "symbol:plus ui symbol plus add"));
        options.add(new IconPickerOption("symbol:minus", "symbol:minus ui symbol minus remove"));
        options.add(new IconPickerOption("symbol:check", "symbol:check ui symbol check confirm done"));
        options.add(new IconPickerOption("symbol:warning", "symbol:warning ui symbol warning alert"));
        addIconPickerOption(options, "gui:hud/crosshair", "combat ui crosshair target aim");
        addIconPickerOption(options, "gui:hud/armor_full", "survival armor full defense");
        addIconPickerOption(options, "gui:hud/armor_half", "survival armor half defense");
        addIconPickerOption(options, "gui:hud/armor_empty", "survival armor empty defense");
        addIconPickerOption(options, "gui:hud/food_full", "survival food hunger full");
        addIconPickerOption(options, "gui:hud/food_half", "survival food hunger half");
        addIconPickerOption(options, "gui:hud/food_empty", "survival food hunger empty");
        addIconPickerOption(options, "gui:hud/food_full_hunger", "survival food hunger effect full");
        addIconPickerOption(options, "gui:hud/food_half_hunger", "survival food hunger effect half");
        addIconPickerOption(options, "gui:hud/food_empty_hunger", "survival food hunger effect empty");
        addIconPickerOption(options, "gui:hud/air", "survival air bubble underwater breath");
        addIconPickerOption(options, "gui:hud/air_bursting", "survival air bubble bursting underwater breath");
        addIconPickerOption(options, "gui:hud/heart/full", "survival heart health full");
        addIconPickerOption(options, "gui:hud/heart/half", "survival heart health half");
        addIconPickerOption(options, "gui:hud/heart/container", "survival heart health container empty");
        addIconPickerOption(options, "gui:hud/heart/hardcore_full", "survival heart health hardcore full");
        addIconPickerOption(options, "gui:hud/heart/hardcore_half", "survival heart health hardcore half");
        addIconPickerOption(options, "gui:hud/heart/poisoned_full", "survival heart health poisoned full");
        addIconPickerOption(options, "gui:hud/heart/poisoned_half", "survival heart health poisoned half");
        addIconPickerOption(options, "gui:hud/heart/withered_full", "survival heart health withered full");
        addIconPickerOption(options, "gui:hud/heart/withered_half", "survival heart health withered half");
        addIconPickerOption(options, "gui:hud/heart/absorbing_full", "survival heart health absorption full");
        addIconPickerOption(options, "gui:hud/heart/absorbing_half", "survival heart health absorption half");
        addIconPickerOption(options, "gui:hud/heart/frozen_full", "survival heart health frozen full");
        addIconPickerOption(options, "gui:hud/heart/frozen_half", "survival heart health frozen half");
        addIconPickerOption(options, "gui:hud/heart/vehicle_full", "survival heart vehicle mount full");
        addIconPickerOption(options, "gui:hud/heart/vehicle_half", "survival heart vehicle mount half");
        addIconPickerOption(options, "gui:icon/checkmark", "ui icon checkmark confirm");
        addIconPickerOption(options, "gui:icon/search", "ui icon search magnifying glass");
        addIconPickerOption(options, "gui:icon/language", "ui icon language translate");
        addIconPickerOption(options, "gui:icon/accessibility", "ui icon accessibility");
        addIconPickerOption(options, "gui:icon/draft_report", "ui icon report warning note");
        addIconPickerOption(options, "gui:icon/ping_unknown", "ui icon ping unknown network");
        addIconPickerOption(options, "gui:icon/ping_1", "ui icon ping network one");
        addIconPickerOption(options, "gui:icon/ping_2", "ui icon ping network two");
        addIconPickerOption(options, "gui:icon/ping_3", "ui icon ping network three");
        addIconPickerOption(options, "gui:icon/ping_4", "ui icon ping network four");
        addIconPickerOption(options, "gui:icon/ping_5", "ui icon ping network five");
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList()) {
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null || item == Items.AIR) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            String searchText = iconPickerItemSearchText(id, item, stack);
            options.add(new IconPickerOption(id.toString(), searchText));
        }
        iconPickerOptions = options;
    }

    private List<IconPickerOption> loadParticleIconOptions() {
        Minecraft minecraft = Minecraft.getInstance();
        List<IconPickerOption> options = new ArrayList<>();
        Set<String> seenVisualSignatures = new HashSet<>();
        for (ResourceLocation id : BuiltInRegistries.PARTICLE_TYPE.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList()) {
            String iconId = "particle:" + id;
            String path = id.getPath();
            if (path.equals("ash")
                    || path.equals("crimson_spore")
                    || path.equals("cherry_leaves")
                    || path.equals("damage_indicator")
                    || path.equals("dragon_breath")
                    || path.equals("dolphin")
                    || path.equals("dripping_honey")
                    || path.equals("dripping_lava")
                    || path.equals("dripping_obsidian_tear")
                    || path.equals("dripping_water")
                    || path.equals("falling_honey")
                    || path.equals("falling_lava")
                    || path.equals("falling_nectar")
                    || path.equals("falling_obsidian_tear")
                    || path.equals("falling_spore_blossom")
                    || path.equals("falling_water")
                    || path.equals("fishing")
                    || path.equals("flash")
                    || path.equals("infested")
                    || path.equals("crit")
                    || path.equals("landing_honey")
                    || path.equals("landing_lava")
                    || path.equals("landing_obsidian_tear")
                    || path.equals("lava")
                    || path.equals("mycelium")
                    || path.equals("ominous_spawning")
                    || path.equals("rain")
                    || path.equals("splash")
                    || path.equals("spore_blossom_air")
                    || path.equals("underwater")
                    || path.equals("warped_spore")
                    || path.equals("white_ash")
                    || path.contains("dripstone")
                    || path.contains("trial_spawner")
                    || path.contains("vault")) {
                continue;
            }
            if (!EntryIconRenderer.isParticleIcon(iconId)) {
                continue;
            }
            // Only expose sprite-backed particles that actually have animation frames or at least one frame.
            if (!particleDefinitionExists(minecraft, id)) {
                continue;
            }
            String visualSignature = EntryIconRenderer.particleVisualSignature(minecraft, iconId);
            if (visualSignature.isEmpty() || !seenVisualSignatures.add(visualSignature)) {
                continue;
            }
            String pathWords = id.getPath().replace('_', ' ');
            String searchText = (iconId + " particle " + id + " " + pathWords).toLowerCase(java.util.Locale.ROOT);
            options.add(new IconPickerOption(iconId, searchText));
        }
        return options;
    }

    private static boolean particleDefinitionExists(Minecraft minecraft, ResourceLocation particleId) {
        ResourceLocation definitionId = ResourceLocation.tryParse(particleId.getNamespace() + ":particles/" + particleId.getPath() + ".json");
        return definitionId != null && minecraft.getResourceManager().getResource(definitionId).isPresent();
    }

    private static void addIconPickerOption(List<IconPickerOption> options, String id, String searchTerms) {
        options.add(new IconPickerOption(id, (id + " " + searchTerms).toLowerCase(java.util.Locale.ROOT)));
    }

    private static String iconPickerItemSearchText(ResourceLocation id, Item item, ItemStack stack) {
        StringBuilder search = new StringBuilder()
                .append(id)
                .append(' ')
                .append(stack.getHoverName().getString());
        for (TagKey<Item> tag : item.builtInRegistryHolder().tags().toList()) {
            ResourceLocation tagId = tag.location();
            search.append(' ')
                    .append(tagId)
                    .append(' ')
                    .append('#')
                    .append(tagId)
                    .append(' ')
                    .append(".#")
                    .append(tagId)
                    .append(' ')
                    .append('#')
                    .append(tagId.getPath())
                    .append(' ')
                    .append(".#")
                    .append(tagId.getPath());
        }
        String creativeTabTerms = creativeTabSearchTerms(id);
        if (!creativeTabTerms.isBlank()) {
            search.append(' ').append(creativeTabTerms);
        }
        return search.toString().toLowerCase(java.util.Locale.ROOT);
    }

    private static String creativeTabSearchTerms(ResourceLocation itemId) {
        if (!CREATIVE_TAB_SEARCH_TERMS_LOADED) {
            Map<ResourceLocation, String> loadedTerms = loadCreativeTabSearchTerms();
            if (!loadedTerms.isEmpty()) {
                CREATIVE_TAB_SEARCH_TERMS = loadedTerms;
                CREATIVE_TAB_SEARCH_TERMS_LOADED = true;
            }
        }
        return CREATIVE_TAB_SEARCH_TERMS.getOrDefault(itemId, "");
    }

    private static Map<ResourceLocation, String> loadCreativeTabSearchTerms() {
        ensureCreativeTabContentsBuilt();
        Map<ResourceLocation, LinkedHashSet<String>> termsByItem = new HashMap<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            String tabName = tab.getDisplayName().getString();
            if (tabName.isBlank()) {
                continue;
            }
            String normalized = tabName.toLowerCase(java.util.Locale.ROOT);
            for (ItemStack stack : tab.getDisplayItems()) {
                if (stack.isEmpty()) {
                    continue;
                }
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (itemId == null) {
                    continue;
                }
                termsByItem.computeIfAbsent(itemId, ignored -> new LinkedHashSet<>()).add(normalized);
            }
        }

        Map<ResourceLocation, String> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, LinkedHashSet<String>> entry : termsByItem.entrySet()) {
            result.put(entry.getKey(), String.join(" ", entry.getValue()));
        }
        return result;
    }

    private static void ensureCreativeTabContentsBuilt() {
        try {
            Field cachedParametersField = CreativeModeTabs.class.getDeclaredField("CACHED_PARAMETERS");
            cachedParametersField.setAccessible(true);
            Object cachedParameters = cachedParametersField.get(null);
            if (cachedParameters == null) {
                return;
            }

            Method buildAllTabContents = CreativeModeTabs.class.getDeclaredMethod(
                    "buildAllTabContents",
                    cachedParameters.getClass()
            );
            buildAllTabContents.setAccessible(true);
            buildAllTabContents.invoke(null, cachedParameters);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private List<IconPickerOption> filteredIconPickerOptions() {
        String query = iconPickerSearchDraft == null ? "" : iconPickerSearchDraft.trim().toLowerCase(java.util.Locale.ROOT);
        if (query.isEmpty()) {
            return iconPickerOptions;
        }
        String[] terms = query.split("\\s+");
        List<IconPickerOption> filtered = new ArrayList<>();
        for (IconPickerOption option : iconPickerOptions) {
            boolean matches = true;
            for (String term : terms) {
                if (!option.searchText().contains(term)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                filtered.add(option);
            }
        }
        return filtered;
    }

    private void selectIconFromPicker(String iconId) {
        entryGlyphDraft = iconId;
        textFields.get(EditorField.ENTRY_GLYPH).setValue(iconId);
        autosaveCurrentEntryDraft();
        closeIconPicker();
    }

    private void toggleEntryLabelVisibility() {
        entryShowLabelDraft = !entryShowLabelDraft;
        autosaveCurrentEntryDraft();
    }

    private void toggleEntryIconVisibility() {
        entryShowIconDraft = !entryShowIconDraft;
        autosaveCurrentEntryDraft();
    }

    private void toggleEntryShortcutVisibility() {
        entryShowShortcutDraft = !entryShowShortcutDraft;
        autosaveCurrentEntryDraft();
    }

    private int maxIconPickerScrollOffset() {
        List<IconPickerOption> visibleOptions = filteredIconPickerOptions();
        if (iconPickerGridWidth <= 0 || iconPickerGridHeight <= 0 || visibleOptions.isEmpty()) {
            return 0;
        }
        int cellSize = ICON_PICKER_CELL_SIZE;
        int cellGap = ICON_PICKER_CELL_GAP;
        int usableGridWidth = Math.max(0, iconPickerGridWidth - (ICON_PICKER_CONTENT_PADDING * 2));
        int usableGridHeight = Math.max(0, iconPickerGridHeight - (ICON_PICKER_CONTENT_PADDING * 2));
        int columns = Math.max(1, (usableGridWidth + cellGap) / (cellSize + cellGap));
        int rows = (visibleOptions.size() + columns - 1) / columns;
        int contentHeight = rows == 0 ? 0 : (rows * (cellSize + cellGap)) - cellGap;
        return Math.max(0, contentHeight - usableGridHeight);
    }

    private void scrollIconPickerToSelection() {
        String selectedIconId = normalizedIconId(entryGlyphDraft);
        List<IconPickerOption> visibleOptions = filteredIconPickerOptions();
        if (selectedIconId.isEmpty() || visibleOptions.isEmpty()) {
            iconPickerScrollOffset = 0;
            return;
        }
        int selectedIndex = -1;
        for (int index = 0; index < visibleOptions.size(); index++) {
            if (visibleOptions.get(index).id().equals(selectedIconId)) {
                selectedIndex = index;
                break;
            }
        }
        if (selectedIndex < 0 || iconPickerGridWidth <= 0 || iconPickerGridHeight <= 0) {
            iconPickerScrollOffset = 0;
            return;
        }
        int cellSize = ICON_PICKER_CELL_SIZE;
        int cellGap = ICON_PICKER_CELL_GAP;
        int usableGridWidth = Math.max(0, iconPickerGridWidth - (ICON_PICKER_CONTENT_PADDING * 2));
        int usableGridHeight = Math.max(0, iconPickerGridHeight - (ICON_PICKER_CONTENT_PADDING * 2));
        int columns = Math.max(1, (usableGridWidth + cellGap) / (cellSize + cellGap));
        int row = selectedIndex / columns;
        int rowTop = row * (cellSize + cellGap);
        int rowBottom = rowTop + cellSize;
        if (rowTop < iconPickerScrollOffset) {
            iconPickerScrollOffset = rowTop;
        } else if (rowBottom > iconPickerScrollOffset + usableGridHeight) {
            iconPickerScrollOffset = rowBottom - usableGridHeight;
        }
        iconPickerScrollOffset = clamp(iconPickerScrollOffset, 0, maxIconPickerScrollOffset());
    }

    private static String normalizedIconId(String iconText) {
        String trimmed = iconText == null ? "" : iconText.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (EntryIconRenderer.isEffectIcon(trimmed)
                || EntryIconRenderer.isSymbolIcon(trimmed)
                || EntryIconRenderer.isGuiSpriteIcon(trimmed)
                || EntryIconRenderer.isParticleIcon(trimmed)
                || EntryIconRenderer.isTextureIcon(trimmed)) {
            return trimmed;
        }
        if (!trimmed.contains(":")) {
            trimmed = "minecraft:" + trimmed;
        }
        ResourceLocation id = ResourceLocation.tryParse(trimmed);
        if (id == null) {
            return "";
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null || item == Items.AIR) {
            return "";
        }
        return id.toString();
    }

    private int iconPickerOptionIndexAt(float mouseX, float mouseY) {
        List<IconPickerOption> visibleOptions = filteredIconPickerOptions();
        if (mouseX < iconPickerGridX
                || mouseX > iconPickerGridX + iconPickerGridWidth
                || mouseY < iconPickerGridY
                || mouseY > iconPickerGridY + iconPickerGridHeight) {
            return -1;
        }

        int cellSize = ICON_PICKER_CELL_SIZE;
        int cellGap = ICON_PICKER_CELL_GAP;
        int usableGridWidth = Math.max(0, iconPickerGridWidth - (ICON_PICKER_CONTENT_PADDING * 2));
        int columns = Math.max(1, (usableGridWidth + cellGap) / (cellSize + cellGap));
        int contentWidth = (columns * cellSize) + ((columns - 1) * cellGap);
        int horizontalInset = Math.max(0, (usableGridWidth - contentWidth) / 2);
        float relativeX = mouseX - (iconPickerGridX + ICON_PICKER_CONTENT_PADDING + horizontalInset);
        if (relativeX < 0) {
            return -1;
        }
        int columnStride = cellSize + cellGap;
        int column = (int) (relativeX / columnStride);
        if (column < 0 || column >= columns || (relativeX % columnStride) > cellSize) {
            return -1;
        }

        float relativeY = mouseY - (iconPickerGridY + ICON_PICKER_CONTENT_PADDING) + iconPickerScrollOffset;
        if (relativeY < 0) {
            return -1;
        }
        int rowStride = cellSize + cellGap;
        int row = (int) (relativeY / rowStride);
        if ((relativeY % rowStride) > cellSize) {
            return -1;
        }

        int optionIndex = row * columns + column;
        return optionIndex >= visibleOptions.size() ? -1 : optionIndex;
    }

    private static String actionSummary(WheelAction action) {
        return switch (action) {
            case CommandAction commandAction -> text(commandAction.commands().size() > 1 ? "omniwheel.action.commands" : "omniwheel.action.command");
            case ChatAction ignored -> text("omniwheel.action.chat");
            case FunctionAction functionAction -> functionAction.function().displayName();
            case OpenWheelAction ignored -> text("omniwheel.action.submenu");
            case LocalMessageAction ignored -> text("omniwheel.action.local");
            case CopyTextAction ignored -> text("omniwheel.action.clipboard");
            case OpenChatAction ignored -> text("omniwheel.action.prefill");
            case OpenScreenAction ignored -> text("omniwheel.action.screen");
            default -> action.getClass().getSimpleName();
        };
    }

    private static String entryTypeLabel(WheelEntry entry) {
        return switch (entry.action()) {
            case CommandAction ignored -> text("omniwheel.action.type.command");
            case ChatAction ignored -> text("omniwheel.action.type.chat");
            case FunctionAction ignored -> text("omniwheel.action.type.function");
            case OpenWheelAction ignored -> text("omniwheel.action.type.submenu");
            case LocalMessageAction ignored -> text("omniwheel.action.type.local");
            case CopyTextAction ignored -> text("omniwheel.action.type.copy");
            case OpenChatAction ignored -> text("omniwheel.action.type.open");
            case OpenScreenAction ignored -> text("omniwheel.action.type.screen");
            default -> "...";
        };
    }

    private static String screenTargetLabel(ScreenTarget target) {
        return switch (target) {
            case PROFILE_MANAGER -> text("omniwheel.screen.profile_manager");
        };
    }

    private static String functionModeLabel(GameplayFunction.FunctionMode mode) {
        return switch (mode) {
            case TRIGGER -> text("omniwheel.function.mode.trigger");
            case TOGGLE -> text("omniwheel.function.mode.toggle");
        };
    }

    private static String text(String key, Object... args) {
        return OmniWheelText.translate(key, args);
    }

    private static String displayText(String value) {
        return OmniWheelText.resolve(value);
    }

    private static String fallbackText(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static String preservedDraftText(String draft, String stored) {
        String trimmed = draft == null ? "" : draft.trim();
        if (trimmed.isEmpty()) {
            return stored;
        }
        if (OmniWheelText.isTranslationKey(stored) && trimmed.equals(displayText(stored))) {
            return stored;
        }
        return trimmed;
    }

    private static String preservedActionValue(WheelAction currentAction, Class<? extends WheelAction> actionType, String draft, String fallback) {
        String stored = switch (currentAction) {
            case ChatAction chatAction when actionType == ChatAction.class -> chatAction.message();
            case LocalMessageAction localMessageAction when actionType == LocalMessageAction.class -> localMessageAction.message();
            case CopyTextAction copyTextAction when actionType == CopyTextAction.class -> copyTextAction.text();
            default -> fallback;
        };
        return preservedDraftText(draft, stored);
    }

    private static List<String> parseCommandValues(String rawValue) {
        String normalized = fallbackText(rawValue, "/command");
        List<String> commands = new ArrayList<>();
        for (String part : normalized.split("\\s*;;\\s*")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                commands.add(trimmed);
            }
        }
        return commands.isEmpty() ? List.of("/command") : List.copyOf(commands);
    }

    private static String formatCommandValues(List<String> commands) {
        return String.join(COMMAND_VALUE_SEPARATOR, commands);
    }

    private void ensureCommandValueFields() {
        syncCommandValueFields();
        if (commandValueFields.isEmpty()) {
            commandValueDrafts.add("/command");
            syncCommandValueFields();
        }
    }

    private void syncCommandValueFields() {
        while (commandValueFields.size() < commandValueDrafts.size()) {
            commandValueFields.add(new TextFieldState(256));
        }
        while (commandValueFields.size() > commandValueDrafts.size()) {
            commandValueFields.removeLast();
        }
        for (int index = 0; index < commandValueDrafts.size(); index++) {
            commandValueFields.get(index).setValue(commandValueDrafts.get(index));
        }
        syncActiveCommandInput();
    }

    private void addCommandValueField() {
        commandValueDrafts.add("");
        syncCommandValueFields();
        focusCommandField(commandValueDrafts.size() - 1);
    }

    private void removeCommandValueField(int index) {
        if (index < 0 || index >= commandValueDrafts.size()) {
            return;
        }

        clearFocus();
        if (commandValueDrafts.size() <= 1) {
            commandValueDrafts.set(0, "");
            syncCommandValueFields();
            autosaveCurrentEntryDraft();
            return;
        }

        commandValueDrafts.remove(index);
        syncCommandValueFields();
        autosaveCurrentEntryDraft();
    }

    private List<String> normalizedCommandDrafts() {
        List<String> commands = new ArrayList<>();
        for (String draft : commandValueDrafts) {
            String trimmed = draft == null ? "" : draft.trim();
            if (!trimmed.isEmpty()) {
                commands.add(trimmed);
            }
        }
        return commands.isEmpty() ? List.of("/command") : List.copyOf(commands);
    }

    private static String shorten(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static int listContentHeight(int itemCount) {
        return itemCount <= 0 ? 0 : ((itemCount - 1) * PROFILE_ROW_STEP) + PROFILE_ROW_HEIGHT;
    }

    private static String ellipsizeToWidth(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return ellipsis;
        }

        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private static int pageCountForEntries(int entryCount) {
        return Math.max(1, (entryCount + 7) / 8);
    }

    private static int storedSegmentCount(int entryCount) {
        return Math.max(1, Math.min(8, Math.max(entryCount, 1)));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    private static float lerp(float current, float target, float factor) {
        return current + ((target - current) * factor);
    }

    private boolean isWheelDraftDirty(WheelDefinition wheel) {
        return !wheel.title().equals(preservedDraftText(wheelTitleDraft, wheel.title()));
    }

    private boolean isEntryDraftDirty(WheelEntry entry) {
        return !entry.label().equals(preservedDraftText(entryLabelDraft, entry.label()))
                || !entry.description().equals(preservedDraftText(entryDescriptionDraft, entry.description()))
                || !entry.shortcut().equals(entryShortcutDraft.trim())
                || !entry.glyph().equals(fallbackText(iconFromText(entryGlyphDraft), entry.glyph()))
                || entry.color() != entryColorDraft
                || entry.guiColor() != entryGuiColorDraft
                || entry.showIcon() != entryShowIconDraft
                || entry.showLabel() != entryShowLabelDraft
                || entry.showShortcut() != entryShowShortcutDraft
                || !entry.action().equals(buildDraftAction(entry.action()));
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, boolean active) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        int edgeColor = active ? PANEL_EDGE_ACTIVE : PANEL_EDGE;
        graphics.fill(x, y, x + width, y + 1, edgeColor);
        graphics.fill(x, y + height - 1, x + width, y + height, edgeColor);
        graphics.fill(x, y, x + 1, y + height, edgeColor);
        graphics.fill(x + width - 1, y, x + width, y + height, edgeColor);
        if (active && width > 4 && height > 4) {
            int innerEdge = 0x503F6178;
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, innerEdge);
            graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, innerEdge);
            graphics.fill(x + 1, y + 1, x + 2, y + height - 1, innerEdge);
            graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, innerEdge);
        }
    }

    private void ensureMouseReleased(Minecraft minecraft) {
        if (!mouseReleased || minecraft.mouseHandler.isMouseGrabbed()) {
            releaseMouseAt(minecraft, lastFreeRawMouseX, lastFreeRawMouseY);
            mouseReleased = true;
        }
    }

    private void releaseMouseAt(Minecraft minecraft, double rawX, double rawY) {
        minecraft.mouseHandler.releaseMouse();
        InputConstants.grabOrReleaseMouse(resolveWindowHandle(minecraft.getWindow()), 212993, rawX, rawY);
    }

    private long resolveWindowHandle(Object window) {
        for (String methodName : List.of("handle", "getWindow")) {
            try {
                Method method = window.getClass().getMethod(methodName);
                Object value = method.invoke(window);
                if (value instanceof Number number) {
                    return number.longValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        throw new IllegalStateException("Unable to resolve GLFW window handle");
    }

    private static boolean isEscapeDown(Minecraft minecraft) {
        return InputConstants.isKeyDown(resolveWindowHandleStatic(minecraft.getWindow()), GLFW.GLFW_KEY_ESCAPE);
    }

    private static long resolveWindowHandleStatic(Object window) {
        for (String methodName : List.of("handle", "getWindow")) {
            try {
                Method method = window.getClass().getMethod(methodName);
                Object value = method.invoke(window);
                if (value instanceof Number number) {
                    return number.longValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        throw new IllegalStateException("Unable to resolve GLFW window handle");
    }

    private float currentMouseX(Minecraft minecraft) {
        double guiX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        return (float) (guiX / uiScale(minecraft));
    }

    private float currentMouseY(Minecraft minecraft) {
        double guiY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
        return (float) (guiY / uiScale(minecraft));
    }

    private int uiWidth(Minecraft minecraft) {
        return Math.max(1, (int) Math.round(minecraft.getWindow().getGuiScaledWidth() / uiScale(minecraft)));
    }

    private int uiHeight(Minecraft minecraft) {
        return Math.max(1, (int) Math.round(minecraft.getWindow().getGuiScaledHeight() / uiScale(minecraft)));
    }

    private double uiScale(Minecraft minecraft) {
        double widthScale = minecraft.getWindow().getGuiScaledWidth() / (double) REFERENCE_UI_WIDTH;
        double heightScale = minecraft.getWindow().getGuiScaledHeight() / (double) REFERENCE_UI_HEIGHT;
        return Math.max(0.05D, Math.min(widthScale, heightScale));
    }

    private double toUiX(double mouseX) {
        return mouseX / uiScale(Minecraft.getInstance());
    }

    private double toUiY(double mouseY) {
        return mouseY / uiScale(Minecraft.getInstance());
    }

    private void enableUiScissor(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        int edgePadding = Math.max(1, (int) Math.ceil(1.0D / uiScale(Minecraft.getInstance())));
        graphics.enableScissor(x1, y1, x2 + edgePadding, y2 + edgePadding);
    }

    private WheelProfile draggedProfile() {
        if (draggedProfileId == null) {
            return null;
        }
        return runtime.profileManager().getProfiles().stream()
                .filter(profile -> profile.id().equals(draggedProfileId))
                .findFirst()
                .orElse(null);
    }

    private List<WheelDefinition> orderedWheels() {
        WheelProfile profile = selectedProfile();
        return profile == null ? List.of() : new ArrayList<>(profile.wheels().values());
    }

    private int profileRowY(int index) {
        return profilesAreaY + (index * PROFILE_ROW_STEP) - profilesScrollOffset;
    }

    private int wheelRowY(int index) {
        return wheelsAreaY + (index * PROFILE_ROW_STEP) - wheelsScrollOffset;
    }

    private int profileIndexAt(float mouseX, float mouseY) {
        List<WheelProfile> profiles = runtime.profileManager().getProfiles();
        if (mouseX < profilesAreaX || mouseX > profilesAreaX + profilesListWidth || mouseY < profilesAreaY || mouseY >= profilesAreaY + profilesAreaHeight) {
            return -1;
        }

        int index = (int) ((mouseY - profilesAreaY + profilesScrollOffset) / PROFILE_ROW_STEP);
        if (index < 0 || index >= profiles.size()) {
            return -1;
        }

        float localY = mouseY - profileRowY(index);
        return localY <= PROFILE_ROW_HEIGHT ? index : -1;
    }

    private int computeDraggedProfileTargetIndex(float mouseY) {
        if (draggedProfileId == null) {
            return -1;
        }

        List<WheelProfile> profiles = runtime.profileManager().getProfiles();
        int targetIndex = 0;
        for (WheelProfile profile : profiles) {
            if (profile.id().equals(draggedProfileId)) {
                continue;
            }

            float rowCenter = profilesAreaY + (targetIndex * PROFILE_ROW_STEP) - profilesScrollOffset + (PROFILE_ROW_HEIGHT / 2.0F);
            if (mouseY > rowCenter) {
                targetIndex++;
            }
        }
        return clamp(targetIndex, 0, Math.max(0, profiles.size() - 1));
    }

    private int wheelIndexAt(float mouseX, float mouseY) {
        List<EntryListRow> rows = visibleEntryRows();
        if (mouseX < wheelsAreaX || mouseX > wheelsAreaX + wheelsListWidth || mouseY < wheelsAreaY || mouseY >= wheelsAreaY + wheelsAreaHeight) {
            return -1;
        }

        int index = (int) ((mouseY - wheelsAreaY + wheelsScrollOffset) / PROFILE_ROW_STEP);
        if (index < 0 || index >= rows.size()) {
            return -1;
        }

        float localY = mouseY - wheelRowY(index);
        return localY <= PROFILE_ROW_HEIGHT ? index : -1;
    }

    private int computeDraggedWheelTargetIndex(float mouseY) {
        if (draggedWheelId == null || draggedWheelOwnerWheelId == null) {
            return -1;
        }

        List<EntryListRow> rows = visibleEntryRows();
        int targetIndex = 0;
        for (EntryListRow row : rows) {
            if (!row.ownerWheelId().equals(draggedWheelOwnerWheelId)) {
                continue;
            }
            if (row.entry().id().equals(draggedWheelId)) {
                continue;
            }

            int visibleIndex = rows.indexOf(row);
            float rowCenter = wheelsAreaY + (visibleIndex * PROFILE_ROW_STEP) - wheelsScrollOffset + (PROFILE_ROW_HEIGHT / 2.0F);
            if (mouseY > rowCenter) {
                targetIndex++;
            }
        }
        WheelProfile profile = selectedProfile();
        int ownerSize = profile != null && profile.wheels().containsKey(draggedWheelOwnerWheelId)
                ? profile.wheel(draggedWheelOwnerWheelId).entries().size()
                : 0;
        return clamp(targetIndex, 0, Math.max(0, ownerSize - 1));
    }

    private int draggedWheelVisibleInsertIndex(List<EntryListRow> rows) {
        if (draggedWheelId == null || draggedWheelOwnerWheelId == null) {
            return 0;
        }

        int siblingIndex = 0;
        int visibleIndex = 0;
        for (EntryListRow row : rows) {
            if (shouldSkipDraggedWheelRow(row)) {
                continue;
            }
            if (row.ownerWheelId().equals(draggedWheelOwnerWheelId)) {
                if (siblingIndex >= draggedWheelTargetIndex) {
                    return visibleIndex;
                }
                siblingIndex++;
            }
            visibleIndex++;
        }
        return visibleIndex;
    }

    private boolean shouldSkipDraggedWheelRow(EntryListRow row) {
        if (draggedWheelId == null || draggedWheelOwnerWheelId == null) {
            return false;
        }
        if (row.entry().id().equals(draggedWheelId) && row.ownerWheelId().equals(draggedWheelOwnerWheelId)) {
            return true;
        }
        if (draggedWheelSubmenuTargetId == null) {
            return false;
        }
        WheelProfile profile = selectedProfile();
        return profile != null && isWheelInSubtree(profile, row.ownerWheelId(), draggedWheelSubmenuTargetId);
    }

    private int maxProfilesScrollOffset() {
        return Math.max(0, listContentHeight(runtime.profileManager().getProfiles().size()) - profilesAreaHeight);
    }

    private int maxWheelsScrollOffset() {
        return Math.max(0, listContentHeight(visibleEntryRows().size()) - wheelsAreaHeight);
    }

    private int maxCommandListScrollOffset() {
        return Math.max(0, commandListContentHeight() - commandListHeight);
    }

    private int maxFunctionListScrollOffset() {
        int rowHeight = 34;
        int rowStep = 38;
        int contentHeight = Math.max(0, (selectableFunctions().size() * rowStep) - (rowStep - rowHeight));
        return Math.max(0, contentHeight - functionListHeight);
    }

    private List<GameplayFunction> selectableFunctions() {
        return java.util.Arrays.stream(GameplayFunction.values())
                .filter(function -> function != GameplayFunction.OMNI_BACK)
                .toList();
    }

    private int commandListContentHeight() {
        ensureCommandValueFields();
        return (commandValueFields.size() * 38) + 24;
    }

    private boolean scrollFunctionListBy(int delta) {
        return scrollFunctionListTo(functionListScrollOffset + delta);
    }

    private boolean scrollFunctionListTo(int targetOffset) {
        int nextOffset = clamp(targetOffset, 0, maxFunctionListScrollOffset());
        if (nextOffset == functionListScrollOffset) {
            return false;
        }
        functionListScrollOffset = nextOffset;
        ensureFocusedActionStillVisible();
        return true;
    }

    private boolean scrollCommandListBy(int delta) {
        return scrollCommandListTo(commandListScrollOffset + delta);
    }

    private boolean scrollCommandListTo(int targetOffset) {
        int nextOffset = clamp(targetOffset, 0, maxCommandListScrollOffset());
        if (nextOffset == commandListScrollOffset) {
            return false;
        }
        commandListScrollOffset = nextOffset;
        ensureFocusedActionStillVisible();
        return true;
    }

    private boolean scrollIconPickerBy(int delta) {
        return scrollIconPickerTo(iconPickerScrollOffset + delta);
    }

    private boolean scrollIconPickerTo(int targetOffset) {
        preserveFocusedIconPickerOption();
        int nextOffset = clamp(targetOffset, 0, maxIconPickerScrollOffset());
        if (nextOffset == iconPickerScrollOffset) {
            return false;
        }
        iconPickerScrollOffset = nextOffset;
        ensureFocusedActionStillVisible();
        return true;
    }

    private void preserveFocusedIconPickerOption() {
        if (!iconPickerOpen || focusedActionIndex < 0 || focusedActionIndex >= actions.size()) {
            return;
        }
        ClickAction action = actions.get(focusedActionIndex);
        if (!action.enabled || !isActionInIconPicker(action)) {
            return;
        }
        int currentIndex = currentIconPickerOptionIndex();
        List<IconPickerOption> options = filteredIconPickerOptions();
        if (currentIndex < 0 || currentIndex >= options.size()) {
            return;
        }
        pendingIconPickerFocusId = options.get(currentIndex).id();
    }

    private boolean handleListNavigationKey(int keyCode) {
        return switch (resolveNavigationList()) {
            case PROFILES -> navigateProfilesByKey(keyCode);
            case WHEELS -> navigateWheelsByKey(keyCode);
            case EDITOR -> false;
        };
    }

    private boolean navigateProfilesByKey(int keyCode) {
        int pageStep = Math.max(1, profilesAreaHeight / PROFILE_ROW_STEP);
        return switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> navigateProfiles(-1);
            case GLFW.GLFW_KEY_DOWN -> navigateProfilesDown();
            case GLFW.GLFW_KEY_PAGE_UP -> navigateProfiles(-pageStep);
            case GLFW.GLFW_KEY_PAGE_DOWN -> navigateProfiles(pageStep);
            case GLFW.GLFW_KEY_HOME -> selectProfileByIndex(0);
            case GLFW.GLFW_KEY_END -> selectProfileByIndex(runtime.profileManager().getProfiles().size() - 1);
            default -> false;
        };
    }

    private boolean navigateWheelsByKey(int keyCode) {
        int pageStep = Math.max(1, wheelsAreaHeight / PROFILE_ROW_STEP);
        return switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> navigateWheels(-1);
            case GLFW.GLFW_KEY_DOWN -> navigateWheelsDown();
            case GLFW.GLFW_KEY_PAGE_UP -> navigateWheels(-pageStep);
            case GLFW.GLFW_KEY_PAGE_DOWN -> navigateWheels(pageStep);
            case GLFW.GLFW_KEY_HOME -> selectEntryRowByIndex(0);
            case GLFW.GLFW_KEY_END -> selectEntryRowByIndex(visibleEntryRows().size() - 1);
            default -> false;
        };
    }

    private boolean navigateProfilesDown() {
        List<WheelProfile> profiles = runtime.profileManager().getProfiles();
        if (profiles.isEmpty()) {
            return focusFirstActionInZone(NavigationList.PROFILES);
        }
        int currentIndex = selectedProfileIndex();
        if (currentIndex >= profiles.size() - 1) {
            return focusFirstActionInZone(NavigationList.PROFILES);
        }
        return navigateProfiles(1);
    }

    private boolean navigateWheelsDown() {
        List<EntryListRow> rows = visibleEntryRows();
        if (rows.isEmpty()) {
            return focusFirstActionInZone(NavigationList.WHEELS);
        }
        int currentIndex = selectedEntryRowIndex(rows);
        if (currentIndex >= rows.size() - 1) {
            return focusFirstActionInZone(NavigationList.WHEELS);
        }
        return navigateWheels(1);
    }

    private int selectedProfileIndex() {
        List<WheelProfile> profiles = runtime.profileManager().getProfiles();
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).id().equals(selectedProfileId)) {
                return index;
            }
        }
        return -1;
    }

    private int selectedEntryRowIndex(List<EntryListRow> rows) {
        for (int index = 0; index < rows.size(); index++) {
            EntryListRow row = rows.get(index);
            if (row.ownerWheelId().equals(selectedWheelId) && row.entry().id().equals(selectedEntryId)) {
                return index;
            }
        }
        return -1;
    }

    private NavigationList resolveNavigationList() {
        return activeList;
    }

    private boolean navigateProfiles(int delta) {
        List<WheelProfile> profiles = runtime.profileManager().getProfiles();
        if (profiles.isEmpty()) {
            return false;
        }

        int currentIndex = 0;
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).id().equals(selectedProfileId)) {
                currentIndex = index;
                break;
            }
        }

        int nextIndex = clamp(currentIndex + delta, 0, profiles.size() - 1);
        if (nextIndex == currentIndex) {
            ensureProfileVisible(nextIndex);
            activeList = NavigationList.PROFILES;
            return true;
        }

        persistPendingEdits();
        clearFocus();
        selectedProfileId = profiles.get(nextIndex).id();
        syncSelection(true);
        ensureProfileVisible(nextIndex);
        activeList = NavigationList.PROFILES;
        return true;
    }

    private boolean selectProfileByIndex(int index) {
        List<WheelProfile> profiles = runtime.profileManager().getProfiles();
        if (profiles.isEmpty()) {
            return false;
        }
        int nextIndex = clamp(index, 0, profiles.size() - 1);
        persistPendingEdits();
        clearFocus();
        selectedProfileId = profiles.get(nextIndex).id();
        syncSelection(true);
        ensureProfileVisible(nextIndex);
        activeList = NavigationList.PROFILES;
        return true;
    }

    private boolean moveSelectedProfileBy(int delta) {
        List<WheelProfile> profiles = runtime.profileManager().getProfiles();
        if (profiles.isEmpty() || selectedProfileId == null) {
            return false;
        }

        int currentIndex = -1;
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).id().equals(selectedProfileId)) {
                currentIndex = index;
                break;
            }
        }
        if (currentIndex < 0) {
            return false;
        }

        int targetIndex = clamp(currentIndex + delta, 0, profiles.size() - 1);
        if (targetIndex == currentIndex) {
            return false;
        }

        persistPendingEdits();
        runtime.profileManager().moveProfile(selectedProfileId, targetIndex);
        syncSelection(true);
        ensureProfileVisible(targetIndex);
        activeList = NavigationList.PROFILES;
        return true;
    }

    private boolean navigateWheels(int delta) {
        List<EntryListRow> rows = visibleEntryRows();
        if (rows.isEmpty()) {
            return false;
        }

        int currentIndex = 0;
        for (int index = 0; index < rows.size(); index++) {
            EntryListRow row = rows.get(index);
            if (row.entry().id().equals(selectedEntryId) && row.ownerWheelId().equals(selectedWheelId)) {
                currentIndex = index;
                break;
            }
        }

        int nextIndex = clamp(currentIndex + delta, 0, rows.size() - 1);
        if (nextIndex == currentIndex) {
            ensureWheelVisible(nextIndex);
            activeList = NavigationList.WHEELS;
            return true;
        }

        persistPendingEdits();
        clearFocus();
        EntryListRow nextRow = rows.get(nextIndex);
        selectedWheelId = nextRow.ownerWheelId();
        selectedEntryId = nextRow.entry().id();
        loadEntryDraft(selectedEntry());
        syncSelection(false);
        ensureWheelVisible(nextIndex);
        activeList = NavigationList.WHEELS;
        return true;
    }

    private boolean selectEntryRowByIndex(int index) {
        List<EntryListRow> rows = visibleEntryRows();
        if (rows.isEmpty()) {
            return false;
        }
        int nextIndex = clamp(index, 0, rows.size() - 1);
        persistPendingEdits();
        clearFocus();
        EntryListRow nextRow = rows.get(nextIndex);
        selectedWheelId = nextRow.ownerWheelId();
        selectedEntryId = nextRow.entry().id();
        loadEntryDraft(selectedEntry());
        syncSelection(false);
        ensureWheelVisible(nextIndex);
        activeList = NavigationList.WHEELS;
        return true;
    }

    private boolean moveSelectedEntryBy(int delta) {
        WheelProfile profile = selectedProfile();
        WheelDefinition wheel = selectedWheel();
        WheelEntry entry = selectedEntry();
        if (profile == null || wheel == null || entry == null) {
            return false;
        }

        List<WheelEntry> entries = new ArrayList<>(wheel.entries());
        int currentIndex = -1;
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).id().equals(entry.id())) {
                currentIndex = index;
                break;
            }
        }
        if (currentIndex < 0) {
            return false;
        }

        int targetIndex = clamp(currentIndex + delta, 0, entries.size() - 1);
        if (targetIndex == currentIndex) {
            return false;
        }

        persistPendingEdits();
        WheelEntry movedEntry = entries.remove(currentIndex);
        entries.add(targetIndex, movedEntry);
        WheelDefinition updatedWheel = new WheelDefinition(
                wheel.id(),
                wheel.title(),
                wheel.description(),
                storedSegmentCount(entries.size()),
                entries,
                wheel.active()
        );
        persistProfile(withWheel(profile, updatedWheel));
        selectedWheelId = wheel.id();
        selectedEntryId = movedEntry.id();
        syncSelection(true);
        ensureWheelVisible(targetIndex);
        activeList = NavigationList.WHEELS;
        return true;
    }

    private void ensureProfileVisible(int index) {
        int top = index * PROFILE_ROW_STEP;
        int bottom = top + PROFILE_ROW_HEIGHT;
        if (top < profilesScrollOffset) {
            profilesScrollOffset = top;
        } else if (bottom > profilesScrollOffset + profilesAreaHeight) {
            profilesScrollOffset = bottom - profilesAreaHeight;
        }
        profilesScrollOffset = clamp(profilesScrollOffset, 0, maxProfilesScrollOffset());
    }

    private void ensureWheelVisible(int index) {
        int top = index * PROFILE_ROW_STEP;
        int bottom = top + PROFILE_ROW_HEIGHT;
        if (top < wheelsScrollOffset) {
            wheelsScrollOffset = top;
        } else if (bottom > wheelsScrollOffset + wheelsAreaHeight) {
            wheelsScrollOffset = bottom - wheelsAreaHeight;
        }
        wheelsScrollOffset = clamp(wheelsScrollOffset, 0, maxWheelsScrollOffset());
    }

    private void ensureCommandFieldVisible(int index) {
        int top = index * 38;
        int bottom = top + 34;
        if (top < commandListScrollOffset) {
            commandListScrollOffset = top;
        } else if (bottom > commandListScrollOffset + commandListHeight) {
            commandListScrollOffset = bottom - commandListHeight;
        }
        commandListScrollOffset = clamp(commandListScrollOffset, 0, maxCommandListScrollOffset());
    }

    private void ensureActionVisible(ClickAction action) {
        if (isActionInCommandList(action)) {
            int top = action.y + commandListScrollOffset - commandListY;
            int bottom = top + action.height;
            if (top < commandListScrollOffset) {
                commandListScrollOffset = top;
            } else if (bottom > commandListScrollOffset + commandListHeight) {
                commandListScrollOffset = bottom - commandListHeight;
            }
            commandListScrollOffset = clamp(commandListScrollOffset, 0, maxCommandListScrollOffset());
        }
    }

    private void updateProfilesScrollbarBounds(int contentHeight) {
        profilesScrollbarX = profilesAreaX + profilesListWidth + SCROLLBAR_GAP;
        profilesScrollbarY = profilesAreaY;
        profilesScrollbarHeight = profilesAreaHeight;
        int maxOffset = Math.max(0, contentHeight - profilesAreaHeight);
        if (maxOffset <= 0 || profilesAreaHeight <= 0) {
            profilesScrollbarThumbY = profilesAreaY;
            profilesScrollbarThumbHeight = profilesAreaHeight;
            return;
        }
        profilesScrollbarThumbHeight = Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.round((profilesAreaHeight * (float) profilesAreaHeight) / contentHeight));
        int thumbTravel = Math.max(0, profilesAreaHeight - profilesScrollbarThumbHeight);
        profilesScrollbarThumbY = profilesAreaY + Math.round((profilesScrollOffset / (float) maxOffset) * thumbTravel);
    }

    private void updateWheelsScrollbarBounds(int contentHeight) {
        wheelsScrollbarX = wheelsAreaX + wheelsListWidth + SCROLLBAR_GAP;
        wheelsScrollbarY = wheelsAreaY;
        wheelsScrollbarHeight = wheelsAreaHeight;
        int maxOffset = Math.max(0, contentHeight - wheelsAreaHeight);
        if (maxOffset <= 0 || wheelsAreaHeight <= 0) {
            wheelsScrollbarThumbY = wheelsAreaY;
            wheelsScrollbarThumbHeight = wheelsAreaHeight;
            return;
        }
        wheelsScrollbarThumbHeight = Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.round((wheelsAreaHeight * (float) wheelsAreaHeight) / contentHeight));
        int thumbTravel = Math.max(0, wheelsAreaHeight - wheelsScrollbarThumbHeight);
        wheelsScrollbarThumbY = wheelsAreaY + Math.round((wheelsScrollOffset / (float) maxOffset) * thumbTravel);
    }

    private void updateFunctionListScrollbarBounds(int contentHeight) {
        functionListScrollbarX = functionListX + functionListWidth + SCROLLBAR_GAP;
        functionListScrollbarY = functionListY;
        functionListScrollbarHeight = functionListHeight;
        int maxOffset = Math.max(0, contentHeight - functionListHeight);
        if (maxOffset <= 0 || functionListHeight <= 0) {
            functionListScrollbarThumbY = functionListY;
            functionListScrollbarThumbHeight = functionListHeight;
            return;
        }
        functionListScrollbarThumbHeight = Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.round((functionListHeight * (float) functionListHeight) / contentHeight));
        int thumbTravel = Math.max(0, functionListHeight - functionListScrollbarThumbHeight);
        functionListScrollbarThumbY = functionListY + Math.round((functionListScrollOffset / (float) maxOffset) * thumbTravel);
    }

    private void updateCommandListScrollbarBounds(int contentHeight) {
        commandListScrollbarX = commandListX + commandListWidth + SCROLLBAR_GAP;
        commandListScrollbarY = commandListY;
        commandListScrollbarHeight = commandListHeight;
        int maxOffset = Math.max(0, contentHeight - commandListHeight);
        if (maxOffset <= 0 || commandListHeight <= 0) {
            commandListScrollbarThumbY = commandListY;
            commandListScrollbarThumbHeight = commandListHeight;
            return;
        }
        commandListScrollbarThumbHeight = Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.round((commandListHeight * (float) commandListHeight) / contentHeight));
        int thumbTravel = Math.max(0, commandListHeight - commandListScrollbarThumbHeight);
        commandListScrollbarThumbY = commandListY + Math.round((commandListScrollOffset / (float) maxOffset) * thumbTravel);
    }

    private void updateIconPickerScrollbarBounds(int contentHeight) {
        iconPickerScrollbarX = iconPickerGridX + iconPickerListWidth + SCROLLBAR_GAP;
        iconPickerScrollbarY = iconPickerGridY;
        iconPickerScrollbarHeight = iconPickerGridHeight;
        int visibleHeight = Math.max(0, iconPickerGridHeight - (ICON_PICKER_CONTENT_PADDING * 2));
        int maxOffset = Math.max(0, contentHeight - visibleHeight);
        if (maxOffset <= 0 || iconPickerGridHeight <= 0 || visibleHeight <= 0) {
            iconPickerScrollbarThumbY = iconPickerGridY;
            iconPickerScrollbarThumbHeight = iconPickerGridHeight;
            return;
        }
        iconPickerScrollbarThumbHeight = Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.round((iconPickerGridHeight * (float) visibleHeight) / contentHeight));
        int thumbTravel = Math.max(0, iconPickerGridHeight - iconPickerScrollbarThumbHeight);
        iconPickerScrollbarThumbY = iconPickerGridY + Math.round((iconPickerScrollOffset / (float) maxOffset) * thumbTravel);
    }

    private boolean tryStartProfilesScrollbarDrag(float mouseX, float mouseY) {
        if (maxProfilesScrollOffset() <= 0) {
            return false;
        }
        if (mouseX < profilesScrollbarX || mouseX > profilesScrollbarX + SCROLLBAR_WIDTH || mouseY < profilesScrollbarY || mouseY > profilesScrollbarY + profilesScrollbarHeight) {
            return false;
        }
        activeList = NavigationList.PROFILES;
        if (mouseY >= profilesScrollbarThumbY && mouseY <= profilesScrollbarThumbY + profilesScrollbarThumbHeight) {
            draggingProfilesScrollbar = true;
            profilesScrollbarDragOffset = Math.round(mouseY) - profilesScrollbarThumbY;
        } else {
            draggingProfilesScrollbar = true;
            profilesScrollbarDragOffset = profilesScrollbarThumbHeight / 2;
            updateProfilesScrollbarDrag(mouseY);
        }
        return true;
    }

    private boolean tryStartWheelsScrollbarDrag(float mouseX, float mouseY) {
        if (maxWheelsScrollOffset() <= 0) {
            return false;
        }
        if (mouseX < wheelsScrollbarX || mouseX > wheelsScrollbarX + SCROLLBAR_WIDTH || mouseY < wheelsScrollbarY || mouseY > wheelsScrollbarY + wheelsScrollbarHeight) {
            return false;
        }
        activeList = NavigationList.WHEELS;
        if (mouseY >= wheelsScrollbarThumbY && mouseY <= wheelsScrollbarThumbY + wheelsScrollbarThumbHeight) {
            draggingWheelsScrollbar = true;
            wheelsScrollbarDragOffset = Math.round(mouseY) - wheelsScrollbarThumbY;
        } else {
            draggingWheelsScrollbar = true;
            wheelsScrollbarDragOffset = wheelsScrollbarThumbHeight / 2;
            updateWheelsScrollbarDrag(mouseY);
        }
        return true;
    }

    private boolean tryStartFunctionListScrollbarDrag(float mouseX, float mouseY) {
        if (entryActionTypeDraft != DraftActionType.FUNCTION || maxFunctionListScrollOffset() <= 0) {
            return false;
        }
        if (mouseX < functionListScrollbarX || mouseX > functionListScrollbarX + SCROLLBAR_WIDTH || mouseY < functionListScrollbarY || mouseY > functionListScrollbarY + functionListScrollbarHeight) {
            return false;
        }
        activeList = NavigationList.EDITOR;
        if (mouseY >= functionListScrollbarThumbY && mouseY <= functionListScrollbarThumbY + functionListScrollbarThumbHeight) {
            draggingFunctionListScrollbar = true;
            functionListScrollbarDragOffset = Math.round(mouseY) - functionListScrollbarThumbY;
        } else {
            draggingFunctionListScrollbar = true;
            functionListScrollbarDragOffset = functionListScrollbarThumbHeight / 2;
            updateFunctionListScrollbarDrag(mouseY);
        }
        return true;
    }

    private boolean tryStartCommandListScrollbarDrag(float mouseX, float mouseY) {
        if (entryActionTypeDraft != DraftActionType.COMMAND || maxCommandListScrollOffset() <= 0) {
            return false;
        }
        if (mouseX < commandListScrollbarX || mouseX > commandListScrollbarX + SCROLLBAR_WIDTH || mouseY < commandListScrollbarY || mouseY > commandListScrollbarY + commandListScrollbarHeight) {
            return false;
        }
        activeList = NavigationList.EDITOR;
        if (mouseY >= commandListScrollbarThumbY && mouseY <= commandListScrollbarThumbY + commandListScrollbarThumbHeight) {
            draggingCommandListScrollbar = true;
            commandListScrollbarDragOffset = Math.round(mouseY) - commandListScrollbarThumbY;
        } else {
            draggingCommandListScrollbar = true;
            commandListScrollbarDragOffset = commandListScrollbarThumbHeight / 2;
            updateCommandListScrollbarDrag(mouseY);
        }
        return true;
    }

    private boolean tryStartIconPickerScrollbarDrag(float mouseX, float mouseY) {
        if (maxIconPickerScrollOffset() <= 0) {
            return false;
        }
        if (mouseX < iconPickerScrollbarX || mouseX > iconPickerScrollbarX + SCROLLBAR_WIDTH || mouseY < iconPickerScrollbarY || mouseY > iconPickerScrollbarY + iconPickerScrollbarHeight) {
            return false;
        }
        activeList = NavigationList.EDITOR;
        if (mouseY >= iconPickerScrollbarThumbY && mouseY <= iconPickerScrollbarThumbY + iconPickerScrollbarThumbHeight) {
            draggingIconPickerScrollbar = true;
            iconPickerScrollbarDragOffset = Math.round(mouseY) - iconPickerScrollbarThumbY;
        } else {
            draggingIconPickerScrollbar = true;
            iconPickerScrollbarDragOffset = iconPickerScrollbarThumbHeight / 2;
            updateIconPickerScrollbarDrag(mouseY);
        }
        return true;
    }

    private void updateProfilesScrollbarDrag(float mouseY) {
        int maxOffset = maxProfilesScrollOffset();
        int thumbTravel = Math.max(0, profilesAreaHeight - profilesScrollbarThumbHeight);
        if (maxOffset <= 0 || thumbTravel <= 0) {
            profilesScrollOffset = 0;
            return;
        }
        int thumbY = clamp(Math.round(mouseY) - profilesScrollbarDragOffset, profilesAreaY, profilesAreaY + thumbTravel);
        float ratio = (thumbY - profilesAreaY) / (float) thumbTravel;
        profilesScrollOffset = Math.round(ratio * maxOffset);
    }

    private void updateWheelsScrollbarDrag(float mouseY) {
        int maxOffset = maxWheelsScrollOffset();
        int thumbTravel = Math.max(0, wheelsAreaHeight - wheelsScrollbarThumbHeight);
        if (maxOffset <= 0 || thumbTravel <= 0) {
            wheelsScrollOffset = 0;
            return;
        }
        int thumbY = clamp(Math.round(mouseY) - wheelsScrollbarDragOffset, wheelsAreaY, wheelsAreaY + thumbTravel);
        float ratio = (thumbY - wheelsAreaY) / (float) thumbTravel;
        wheelsScrollOffset = Math.round(ratio * maxOffset);
    }

    private void updateFunctionListScrollbarDrag(float mouseY) {
        int maxOffset = maxFunctionListScrollOffset();
        int thumbTravel = Math.max(0, functionListHeight - functionListScrollbarThumbHeight);
        if (maxOffset <= 0 || thumbTravel <= 0) {
            functionListScrollOffset = 0;
            return;
        }
        int thumbY = clamp(Math.round(mouseY) - functionListScrollbarDragOffset, functionListY, functionListY + thumbTravel);
        float ratio = (thumbY - functionListY) / (float) thumbTravel;
        functionListScrollOffset = Math.round(ratio * maxOffset);
    }

    private void updateCommandListScrollbarDrag(float mouseY) {
        int maxOffset = maxCommandListScrollOffset();
        int thumbTravel = Math.max(0, commandListHeight - commandListScrollbarThumbHeight);
        if (maxOffset <= 0 || thumbTravel <= 0) {
            commandListScrollOffset = 0;
            return;
        }
        int thumbY = clamp(Math.round(mouseY) - commandListScrollbarDragOffset, commandListY, commandListY + thumbTravel);
        float ratio = (thumbY - commandListY) / (float) thumbTravel;
        commandListScrollOffset = Math.round(ratio * maxOffset);
    }

    private void updateIconPickerScrollbarDrag(float mouseY) {
        preserveFocusedIconPickerOption();
        int maxOffset = maxIconPickerScrollOffset();
        int thumbTravel = Math.max(0, iconPickerGridHeight - iconPickerScrollbarThumbHeight);
        if (maxOffset <= 0 || thumbTravel <= 0) {
            iconPickerScrollOffset = 0;
            return;
        }
        int thumbY = clamp(Math.round(mouseY) - iconPickerScrollbarDragOffset, iconPickerGridY, iconPickerGridY + thumbTravel);
        float ratio = (thumbY - iconPickerGridY) / (float) thumbTravel;
        iconPickerScrollOffset = Math.round(ratio * maxOffset);
    }

    private boolean tryStartProfileDrag(float mouseX, float mouseY) {
        if (isRenamingProfile() || isRenamingWheel()) {
            return false;
        }
        int profileIndex = profileIndexAt(mouseX, mouseY);
        if (profileIndex < 0) {
            return false;
        }
        activeList = NavigationList.PROFILES;

        List<WheelProfile> profiles = runtime.profileManager().getProfiles();
        if (profileIndex >= profiles.size()) {
            return false;
        }

        persistPendingEdits();
        clearFocus();
        WheelProfile profile = profiles.get(profileIndex);
        selectedProfileId = profile.id();
        syncSelection(true);

        draggedProfileId = profile.id();
        draggedProfileStartIndex = profileIndex;
        draggedProfileTargetIndex = profileIndex;
        draggedProfileMouseStartX = mouseX;
        draggedProfileMouseStartY = mouseY;
        draggedProfileAnchorOffsetY = mouseY - profileRowY(profileIndex);
        draggedProfileVisualX = profilesAreaX;
        draggedProfileVisualY = profileRowY(profileIndex);
        draggedProfileActive = false;
        return true;
    }

    private boolean tryStartWheelDrag(float mouseX, float mouseY) {
        if (isRenamingProfile() || isRenamingWheel()) {
            return false;
        }
        int wheelIndex = wheelIndexAt(mouseX, mouseY);
        if (wheelIndex < 0) {
            return false;
        }
        activeList = NavigationList.WHEELS;

        List<EntryListRow> rows = visibleEntryRows();
        if (wheelIndex >= rows.size()) {
            return false;
        }

        persistPendingEdits();
        clearFocus();
        EntryListRow row = rows.get(wheelIndex);
        draggedWheelWasSelected = isEntryRowSelected(row.ownerWheelId(), row.entry().id());
        selectedWheelId = row.ownerWheelId();
        selectedEntryId = row.entry().id();
        loadEntryDraft(selectedEntry());
        ensureWheelVisible(wheelIndex);
        draggedWheelId = row.entry().id();
        draggedWheelOwnerWheelId = row.ownerWheelId();
        draggedWheelSubmenuTargetId = row.expandable() && row.entry().action() instanceof OpenWheelAction openWheelAction
                ? openWheelAction.wheelId()
                : null;
        draggedWheelStartIndex = wheelIndex;
        draggedWheelTargetIndex = siblingEntryIndex(row.ownerWheelId(), row.entry().id());
        draggedWheelMouseStartX = mouseX;
        draggedWheelMouseStartY = mouseY;
        draggedWheelAnchorOffsetY = mouseY - wheelRowY(wheelIndex);
        draggedWheelVisualX = wheelsAreaX;
        draggedWheelVisualY = wheelRowY(wheelIndex);
        draggedWheelActive = false;
        return true;
    }

    private void updateProfileDrag(float mouseX, float mouseY, boolean leftPressed) {
        if (draggedProfileId == null) {
            return;
        }

        if (!leftPressed) {
            finishProfileDrag();
            return;
        }

        if (!draggedProfileActive) {
            float deltaX = mouseX - draggedProfileMouseStartX;
            float deltaY = mouseY - draggedProfileMouseStartY;
            if ((deltaX * deltaX) + (deltaY * deltaY) < (PROFILE_DRAG_THRESHOLD * PROFILE_DRAG_THRESHOLD)) {
                return;
            }
            draggedProfileActive = true;
            draggedProfileVisualX = profilesAreaX;
            draggedProfileVisualY = profileRowY(draggedProfileStartIndex);
        }

        float targetX = clamp(mouseX - (profilesAreaWidth * 0.45F), profilesAreaX - 4.0F, profilesAreaX + 18.0F);
        float targetY = clamp(mouseY - draggedProfileAnchorOffsetY, profilesAreaY - 2.0F, profilesActionY - PROFILE_ROW_HEIGHT - 6.0F);
        draggedProfileVisualX = lerp(draggedProfileVisualX, targetX, 0.35F);
        draggedProfileVisualY = lerp(draggedProfileVisualY, targetY, 0.35F);
        draggedProfileTargetIndex = computeDraggedProfileTargetIndex(mouseY);
    }

    private void updateWheelDrag(float mouseX, float mouseY, boolean leftPressed) {
        if (draggedWheelId == null) {
            return;
        }

        if (!leftPressed) {
            finishWheelDrag();
            return;
        }

        if (!draggedWheelActive) {
            float deltaX = mouseX - draggedWheelMouseStartX;
            float deltaY = mouseY - draggedWheelMouseStartY;
            if ((deltaX * deltaX) + (deltaY * deltaY) < (PROFILE_DRAG_THRESHOLD * PROFILE_DRAG_THRESHOLD)) {
                return;
            }
            draggedWheelActive = true;
            draggedWheelVisualX = wheelsAreaX;
            draggedWheelVisualY = wheelRowY(draggedWheelStartIndex);
        }

        float targetX = clamp(mouseX - (wheelsAreaWidth * 0.45F), wheelsAreaX - 4.0F, wheelsAreaX + 18.0F);
        float targetY = clamp(mouseY - draggedWheelAnchorOffsetY, wheelsAreaY - 2.0F, wheelsActionY - PROFILE_ROW_HEIGHT - 6.0F);
        draggedWheelVisualX = lerp(draggedWheelVisualX, targetX, 0.35F);
        draggedWheelVisualY = lerp(draggedWheelVisualY, targetY, 0.35F);
        draggedWheelTargetIndex = computeDraggedWheelTargetIndex(mouseY);
    }

    private void finishProfileDrag() {
        if (draggedProfileActive && draggedProfileId != null) {
            runtime.profileManager().moveProfile(draggedProfileId, draggedProfileTargetIndex);
            selectedProfileId = draggedProfileId;
            syncSelection(true);
        }
        clearProfileDrag();
    }

    private void finishWheelDrag() {
        if (!draggedWheelActive && draggedWheelSubmenuTargetId != null && draggedWheelWasSelected) {
            WheelProfile profile = selectedProfile();
            WheelDefinition ownerWheel = profile != null && draggedWheelOwnerWheelId != null && profile.wheels().containsKey(draggedWheelOwnerWheelId)
                    ? profile.wheel(draggedWheelOwnerWheelId)
                    : null;
            WheelEntry draggedEntry = ownerWheel == null ? null : ownerWheel.entries().stream()
                    .filter(entry -> entry.id().equals(draggedWheelId))
                    .findFirst()
                    .orElse(null);
            if (draggedEntry != null) {
                toggleExpandedSubmenu(draggedWheelOwnerWheelId, draggedEntry, draggedWheelSubmenuTargetId);
            }
            clearWheelDrag();
            return;
        }

        if (draggedWheelActive && draggedWheelId != null && draggedWheelOwnerWheelId != null) {
            WheelProfile profile = selectedProfile();
            if (profile != null && profile.wheels().containsKey(draggedWheelOwnerWheelId)) {
                WheelDefinition ownerWheel = profile.wheel(draggedWheelOwnerWheelId);
                List<WheelEntry> entries = new ArrayList<>(ownerWheel.entries());
                int startIndex = siblingEntryIndex(draggedWheelOwnerWheelId, draggedWheelId);
                if (startIndex >= 0) {
                    WheelEntry movedEntry = entries.remove(startIndex);
                    int targetIndex = clamp(draggedWheelTargetIndex, 0, entries.size());
                    entries.add(targetIndex, movedEntry);
                    WheelDefinition updatedWheel = new WheelDefinition(
                            ownerWheel.id(),
                            ownerWheel.title(),
                            ownerWheel.description(),
                            storedSegmentCount(entries.size()),
                            entries,
                            ownerWheel.active()
                    );
                    persistProfile(withWheel(profile, updatedWheel));
                    selectedWheelId = draggedWheelOwnerWheelId;
                    selectedEntryId = draggedWheelId;
                    syncSelection(true);
                }
            }
        }
        clearWheelDrag();
    }

    private void clearProfileDrag() {
        draggedProfileId = null;
        draggedProfileStartIndex = -1;
        draggedProfileTargetIndex = -1;
        draggedProfileMouseStartX = 0.0F;
        draggedProfileMouseStartY = 0.0F;
        draggedProfileAnchorOffsetY = 0.0F;
        draggedProfileVisualX = 0.0F;
        draggedProfileVisualY = 0.0F;
        draggedProfileActive = false;
    }

    private void clearWheelDrag() {
        draggedWheelId = null;
        draggedWheelOwnerWheelId = null;
        draggedWheelSubmenuTargetId = null;
        draggedWheelWasSelected = false;
        draggedWheelStartIndex = -1;
        draggedWheelTargetIndex = -1;
        draggedWheelMouseStartX = 0.0F;
        draggedWheelMouseStartY = 0.0F;
        draggedWheelAnchorOffsetY = 0.0F;
        draggedWheelVisualX = 0.0F;
        draggedWheelVisualY = 0.0F;
        draggedWheelActive = false;
    }

    private boolean clickFocusedTextField(float mouseX, float mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entryActionTypeDraft == DraftActionType.COMMAND) {
            for (int index = 0; index < commandValueFields.size(); index++) {
                TextFieldState field = commandValueFields.get(index);
                if (field.contains(mouseX, mouseY)) {
                    focusCommandField(index);
                    activateTextEntry();
                    if (isUsingVanillaCommandInput(index)) {
                        double clampedX = Math.max(commandInput.getX(), Math.min(mouseX, commandInput.getX() + commandInput.getWidth() - 1));
                        double clampedY = Math.max(commandInput.getY(), Math.min(mouseY, commandInput.getY() + commandInput.getHeight() - 1));
                        commandInput.mouseClicked(clampedX, clampedY, 0);
                    } else {
                        field.click(minecraft.font, mouseX);
                        applyCommandFieldDraft(index, field.value);
                    }
                    return true;
                }
            }
        }
        for (EditorField fieldId : visibleEditableFields()) {
            TextFieldState field = textFields.get(fieldId);
            if (field.contains(mouseX, mouseY)) {
                focusField(fieldId);
                activateTextEntry();
                field.click(minecraft.font, mouseX);
                applyFieldDraft(fieldId, field.value);
                return true;
            }
        }
        return false;
    }

    private void handleKeyboardInput(Minecraft minecraft) {
        long handle = resolveWindowHandle(minecraft.getWindow());
        long now = System.currentTimeMillis();
        for (int key : POLLED_KEYS) {
            boolean down = InputConstants.isKeyDown(handle, key);
            if (down) {
                if (pressedKeys.add(key)) {
                    handleKeyPress(key);
                    nextKeyRepeatAt.put(key, now + KEY_REPEAT_DELAY_MS);
                } else if (now >= nextKeyRepeatAt.getOrDefault(key, Long.MAX_VALUE)) {
                    handleKeyPress(key);
                    nextKeyRepeatAt.put(key, now + KEY_REPEAT_INTERVAL_MS);
                }
            } else {
                pressedKeys.remove(key);
                nextKeyRepeatAt.remove(key);
            }
        }
    }

    private void handleKeyPress(int key) {
        if (shouldSuppressDirectionalNavigation(key)) {
            return;
        }
        if (iconPickerOpen && isDirectionalNavigationKey(key) && handleIconPickerDirectionalKey(key)) {
            return;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            cycleFocus(isShiftModifierDown());
            return;
        }

        TextFieldState field = currentFocusedField();
        if (field == null) {
            return;
        }
        if (!textEntryActive) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
                activateTextEntry();
            } else if ((key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) && clearHighlightedShortcutField()) {
                return;
            } else if (key == GLFW.GLFW_KEY_RIGHT && shouldStayOnEditorFieldOnRight()) {
                return;
            } else if (key == GLFW.GLFW_KEY_LEFT && shouldLeaveEditorToEntriesOnLeft()) {
                handlePanelSwitchKeyPressed(key);
            } else if (key == GLFW.GLFW_KEY_UP) {
                cycleFocus(true);
            } else if (key == GLFW.GLFW_KEY_DOWN) {
                cycleFocus(false);
            }
            return;
        }

        boolean controlDown = isControlModifierDown();
        if (controlDown) {
            if (key == GLFW.GLFW_KEY_A) {
                field.selectAll();
            } else if (key == GLFW.GLFW_KEY_V) {
                field.insertText(clipboardValue());
                applyFocusedFieldDraft();
            } else if (key == GLFW.GLFW_KEY_C) {
                Minecraft.getInstance().keyboardHandler.setClipboard(field.copyText());
            } else if (key == GLFW.GLFW_KEY_X) {
                String copied = field.copyText();
                if (!copied.isEmpty()) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(copied);
                    field.deleteSelection();
                    applyFocusedFieldDraft();
                }
            } else if (key == GLFW.GLFW_KEY_LEFT) {
                field.moveCursorByWord(-1, isShiftModifierDown());
            } else if (key == GLFW.GLFW_KEY_RIGHT) {
                field.moveCursorByWord(1, isShiftModifierDown());
            } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                field.deleteWordBackward();
                applyFocusedFieldDraft();
            } else if (key == GLFW.GLFW_KEY_DELETE) {
                field.deleteWordForward();
                applyFocusedFieldDraft();
            }
            return;
        }

        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                field.backspace();
                applyFocusedFieldDraft();
                return;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                field.deleteForward();
                applyFocusedFieldDraft();
                return;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                field.moveCursor(-1, isShiftModifierDown());
                return;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                field.moveCursor(1, isShiftModifierDown());
                return;
            }
            case GLFW.GLFW_KEY_HOME -> {
                field.moveToStart(isShiftModifierDown());
                return;
            }
            case GLFW.GLFW_KEY_END -> {
                field.moveToEnd(isShiftModifierDown());
                return;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (focusedCommandFieldIndex >= 0) {
                    cycleFocus(false);
                } else {
                    deactivateTextEntry();
                }
                return;
            }
            default -> {
            }
        }

        Character typed = typedCharacterForKey(key, isShiftModifierDown());
        if (typed != null) {
            field.insertText(Character.toString(typed));
            applyFocusedFieldDraft();
        }
    }

    private boolean handleEditorNavigationKey(int key) {
        if (shouldSuppressDirectionalNavigation(key)) {
            return true;
        }
        if (iconPickerOpen && isDirectionalNavigationKey(key) && handleIconPickerDirectionalKey(key)) {
            return true;
        }
        TextFieldState field = currentFocusedField();
        if (field == null) {
            return false;
        }
        if (!textEntryActive) {
            return switch (key) {
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                    activateTextEntry();
                    yield true;
                }
                case GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_DELETE -> clearHighlightedShortcutField();
                case GLFW.GLFW_KEY_LEFT -> {
                    if (shouldLeaveEditorToEntriesOnLeft() && handlePanelSwitchKeyPressed(key)) {
                        yield true;
                    }
                    yield false;
                }
                case GLFW.GLFW_KEY_RIGHT -> shouldStayOnEditorFieldOnRight();
                case GLFW.GLFW_KEY_UP -> {
                    cycleFocus(true);
                    yield true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    cycleFocus(false);
                    yield true;
                }
                default -> false;
            };
        }
        boolean shiftDown = isShiftModifierDown();
        boolean controlDown = isControlModifierDown();

        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (controlDown) {
                    field.deleteWordBackward();
                } else {
                    field.backspace();
                }
                applyFocusedFieldDraft();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (controlDown) {
                    field.deleteWordForward();
                } else {
                    field.deleteForward();
                }
                applyFocusedFieldDraft();
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (controlDown) {
                    field.moveCursorByWord(-1, shiftDown);
                } else {
                    field.moveCursor(-1, shiftDown);
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (controlDown) {
                    field.moveCursorByWord(1, shiftDown);
                } else {
                    field.moveCursor(1, shiftDown);
                }
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                cycleFocus(true);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                cycleFocus(false);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                field.moveToStart(shiftDown);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                field.moveToEnd(shiftDown);
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (focusedCommandFieldIndex >= 0) {
                    cycleFocus(false);
                } else {
                    deactivateTextEntry();
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void applyFocusedFieldDraft() {
        if (focusedCommandFieldIndex >= 0) {
            applyCommandFieldDraft(focusedCommandFieldIndex, commandValueFields.get(focusedCommandFieldIndex).value);
        } else if (focusedField != null) {
            applyFieldDraft(focusedField, textFields.get(focusedField).value);
        }
    }

    private void applyFieldDraft(EditorField field, String value) {
        switch (field) {
            case PROFILE_NAME -> profileNameDraft = value;
            case WHEEL_TITLE -> wheelTitleDraft = value;
            case ENTRY_LABEL -> entryLabelDraft = value;
            case ENTRY_DESCRIPTION -> entryDescriptionDraft = value;
            case ENTRY_SHORTCUT -> {
                entryShortcutDraft = value;
                autosaveCurrentEntryDraft();
            }
            case ICON_PICKER_SEARCH -> {
                iconPickerSearchDraft = value;
                iconPickerScrollOffset = 0;
            }
            case ENTRY_GLYPH -> {
                String sanitized = iconFromText(value);
                if (!Objects.equals(value, sanitized)) {
                    textFields.get(EditorField.ENTRY_GLYPH).setValue(sanitized);
                }
                entryGlyphDraft = sanitized;
                autosaveCurrentEntryDraft();
            }
            case ENTRY_VALUE -> entryValueDraft = value;
        }
    }

    private void focusField(EditorField fieldId) {
        if (focusedField == EditorField.PROFILE_NAME && fieldId != EditorField.PROFILE_NAME) {
            commitProfileRename();
        }
        if (focusedField == EditorField.WHEEL_TITLE && fieldId != EditorField.WHEEL_TITLE) {
            commitWheelRename();
        }
        clearFocus();
        activeList = navigationListForField(fieldId);
        focusedField = fieldId;
        textFields.get(fieldId).focused = true;
    }

    private void focusCommandField(int index) {
        clearFocus();
        activeList = NavigationList.EDITOR;
        focusedCommandFieldIndex = index;
        commandValueFields.get(index).focused = true;
        ensureCommandFieldVisible(index);
        syncActiveCommandInput();
    }

    private void activateTextEntry() {
        textEntryActive = true;
        if (focusedCommandFieldIndex >= 0) {
            syncActiveCommandInput();
            if (commandInput != null) {
                commandInput.setVisible(true);
                commandInput.setFocused(true);
            }
        }
    }

    private void deactivateTextEntry() {
        textEntryActive = false;
        if (commandInput != null) {
            commandInput.setFocused(false);
            commandInput.setVisible(false);
        }
        Screen hostScreen = hostScreen();
        if (hostScreen != null && hostScreen.getFocused() == commandInput) {
            hostScreen.setFocused(null);
        }
    }

    private void clearFocus() {
        if (focusedField != null) {
            textFields.get(focusedField).focused = false;
            focusedField = null;
        }
        if (focusedCommandFieldIndex >= 0 && focusedCommandFieldIndex < commandValueFields.size()) {
            commandValueFields.get(focusedCommandFieldIndex).focused = false;
        }
        focusedCommandFieldIndex = -1;
        if (commandInput != null) {
            commandInput.setFocused(false);
            commandInput.setVisible(false);
        }
        Screen hostScreen = hostScreen();
        if (hostScreen != null && hostScreen.getFocused() == commandInput) {
            hostScreen.setFocused(null);
        }
        focusedActionIndex = -1;
        textEntryActive = false;
    }

    private TextFieldState currentFocusedField() {
        if (focusedCommandFieldIndex >= 0 && focusedCommandFieldIndex < commandValueFields.size()) {
            return commandValueFields.get(focusedCommandFieldIndex);
        }
        return focusedField == null ? null : textFields.get(focusedField);
    }

    private void cycleFocus(boolean reverse) {
        List<FocusTarget> visibleFields = visibleFocusTargets();
        if (visibleFields.isEmpty()) {
            clearFocus();
            return;
        }

        FocusTarget currentTarget = currentFocusTarget();
        if (currentTarget == null || !visibleFields.contains(currentTarget)) {
            focusTarget(reverse ? visibleFields.getLast() : visibleFields.getFirst());
            return;
        }

        int currentIndex = visibleFields.indexOf(currentTarget);
        int nextIndex = reverse ? (currentIndex - 1 + visibleFields.size()) % visibleFields.size() : (currentIndex + 1) % visibleFields.size();
        focusTarget(visibleFields.get(nextIndex));
    }

    private List<EditorField> visibleEditableFields() {
        List<EditorField> visibleFields = new ArrayList<>();
        if (iconPickerOpen) {
            visibleFields.add(EditorField.ICON_PICKER_SEARCH);
            return visibleFields;
        }
        if (renamingProfileId != null) {
            visibleFields.add(EditorField.PROFILE_NAME);
        }
        if (renamingWheelId != null) {
            visibleFields.add(EditorField.WHEEL_TITLE);
        }
        if (selectedEntry() != null) {
            visibleFields.add(EditorField.ENTRY_LABEL);
            visibleFields.add(EditorField.ENTRY_DESCRIPTION);
            visibleFields.add(EditorField.ENTRY_SHORTCUT);
            visibleFields.add(EditorField.ENTRY_GLYPH);
            if (entryActionTypeDraft != DraftActionType.SUBMENU
                    && entryActionTypeDraft != DraftActionType.OPEN_SCREEN
                    && entryActionTypeDraft != DraftActionType.FUNCTION) {
                visibleFields.add(EditorField.ENTRY_VALUE);
            }
        }
        return visibleFields;
    }

    private List<FocusTarget> visibleFocusTargets() {
        return visibleFocusTargets(resolveNavigationList());
    }

    private List<FocusTarget> visibleFocusTargets(NavigationList zone) {
        List<FocusTarget> targets = new ArrayList<>();
        if (iconPickerOpen) {
            targets.add(FocusTarget.forField(EditorField.ICON_PICKER_SEARCH));
            for (int index = 0; index < actions.size(); index++) {
                ClickAction action = actions.get(index);
                if (action.enabled) {
                    targets.add(FocusTarget.forAction(index));
                }
            }
            return targets;
        }
        if (zone == NavigationList.PROFILES && renamingProfileId != null) {
            targets.add(FocusTarget.forField(EditorField.PROFILE_NAME));
        }
        if (zone == NavigationList.WHEELS && renamingWheelId != null) {
            targets.add(FocusTarget.forField(EditorField.WHEEL_TITLE));
        }
        if (zone == NavigationList.EDITOR && selectedEntry() != null) {
            targets.add(FocusTarget.forField(EditorField.ENTRY_LABEL));
            targets.add(FocusTarget.forField(EditorField.ENTRY_DESCRIPTION));
            targets.add(FocusTarget.forField(EditorField.ENTRY_SHORTCUT));
            targets.add(FocusTarget.forField(EditorField.ENTRY_GLYPH));
            if (entryActionTypeDraft == DraftActionType.COMMAND) {
                for (int index = 0; index < commandValueFields.size(); index++) {
                    targets.add(FocusTarget.forCommandField(index));
                }
            } else if (entryActionTypeDraft != DraftActionType.SUBMENU
                    && entryActionTypeDraft != DraftActionType.OPEN_SCREEN
                    && entryActionTypeDraft != DraftActionType.FUNCTION) {
                targets.add(FocusTarget.forField(EditorField.ENTRY_VALUE));
            }
        }
        for (int index = 0; index < actions.size(); index++) {
            ClickAction action = actions.get(index);
            if (action.enabled && navigationListForAction(action) == zone) {
                targets.add(FocusTarget.forAction(index));
            }
        }
        return targets;
    }

    private FocusTarget currentFocusTarget() {
        if (focusedCommandFieldIndex >= 0) {
            return FocusTarget.forCommandField(focusedCommandFieldIndex);
        }
        if (focusedActionIndex >= 0) {
            return FocusTarget.forAction(focusedActionIndex);
        }
        return focusedField == null ? null : FocusTarget.forField(focusedField);
    }

    private void focusTarget(FocusTarget target) {
        if (target.actionIndex() != null) {
            focusAction(target.actionIndex());
        } else if (target.commandFieldIndex() != null) {
            focusCommandField(target.commandFieldIndex());
        } else if (target.field() != null) {
            focusField(target.field());
        }
    }

    private void focusAction(int index) {
        if (index < 0 || index >= actions.size() || !actions.get(index).enabled) {
            return;
        }
        if (focusedField != null) {
            textFields.get(focusedField).focused = false;
            focusedField = null;
        }
        if (focusedCommandFieldIndex >= 0 && focusedCommandFieldIndex < commandValueFields.size()) {
            commandValueFields.get(focusedCommandFieldIndex).focused = false;
        }
        focusedCommandFieldIndex = -1;
        if (commandInput != null) {
            commandInput.setFocused(false);
            commandInput.setVisible(false);
        }
        Screen hostScreen = hostScreen();
        if (hostScreen != null && hostScreen.getFocused() == commandInput) {
            hostScreen.setFocused(null);
        }
        activeList = navigationListForAction(actions.get(index));
        focusedActionIndex = index;
        ensureActionVisible(actions.get(index));
    }

    private void applyCommandFieldDraft(int index, String value) {
        if (index < 0 || index >= commandValueDrafts.size()) {
            return;
        }
        commandValueDrafts.set(index, value);
        if (index < commandValueFields.size()) {
            commandValueFields.get(index).setValue(value);
        }
    }

    private boolean handleCommandFieldKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedCommandFieldIndex < 0) {
            return false;
        }
        if (!textEntryActive) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                activateTextEntry();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT && commandValueFields.size() <= 1) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                if (focusedCommandFieldIndex > 0) {
                    focusCommandField(focusedCommandFieldIndex - 1);
                } else {
                    focusField(EditorField.ENTRY_SHORTCUT);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                if (focusedCommandFieldIndex == commandValueFields.size() - 1) {
                    int addCommandActionIndex = findActionBelowCommandFields();
                    if (addCommandActionIndex >= 0) {
                        focusAction(addCommandActionIndex);
                        return true;
                    }
                }
                cycleFocus(false);
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) && handlePanelSwitchKeyPressed(keyCode)) {
                return true;
            }
            return false;
        }
        if (!ensureCommandInput()) {
            return false;
        }
        if (commandSuggestions.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (commandInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            cycleFocus(false);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            cycleFocus(true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            cycleFocus(false);
            return true;
        }
        return false;
    }

    private boolean handleIconPickerDirectionalKey(int keyCode) {
        if (!iconPickerOpen || !isDirectionalNavigationKey(keyCode)) {
            return false;
        }

        if (focusedField == EditorField.ICON_PICKER_SEARCH) {
            return switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_UP -> true;
                case GLFW.GLFW_KEY_RIGHT -> {
                    deactivateTextEntry();
                    focusIconPickerBackButton();
                    yield true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    deactivateTextEntry();
                    focusIconPickerOptionByIndex(0);
                    yield true;
                }
                default -> false;
            };
        }

        if (focusedActionIndex >= 0) {
            if (isActionAtBounds(
                    focusedActionIndex,
                    iconPickerBackButtonX,
                    iconPickerBackButtonY,
                    iconPickerBackButtonWidth,
                    iconPickerBackButtonHeight
            )) {
                return switch (keyCode) {
                    case GLFW.GLFW_KEY_LEFT -> {
                        focusField(EditorField.ICON_PICKER_SEARCH);
                        yield true;
                    }
                    case GLFW.GLFW_KEY_DOWN -> {
                        focusIconPickerOptionByIndex(0);
                        yield true;
                    }
                    case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_RIGHT -> true;
                    default -> false;
                };
            }
            if (isActionInIconPicker(actions.get(focusedActionIndex))) {
                int currentIndex = currentIconPickerOptionIndex();
                if (currentIndex < 0) {
                    return true;
                }
                List<IconPickerOption> options = filteredIconPickerOptions();
                int columns = iconPickerColumnCount();
                return switch (keyCode) {
                    case GLFW.GLFW_KEY_UP -> {
                        if (currentIndex < columns) {
                            focusField(EditorField.ICON_PICKER_SEARCH);
                            yield true;
                        }
                        focusIconPickerOptionByIndex(currentIndex - columns);
                        yield true;
                    }
                    case GLFW.GLFW_KEY_LEFT -> {
                        if (currentIndex % columns == 0) {
                            yield true;
                        }
                        focusIconPickerOptionByIndex(currentIndex - 1);
                        yield true;
                    }
                    case GLFW.GLFW_KEY_RIGHT -> {
                        int nextIndex = currentIndex + 1;
                        if (nextIndex >= options.size() || (nextIndex / columns) != (currentIndex / columns)) {
                            yield true;
                        }
                        focusIconPickerOptionByIndex(nextIndex);
                        yield true;
                    }
                    case GLFW.GLFW_KEY_DOWN -> {
                        int nextIndex = currentIndex + columns;
                        if (nextIndex >= options.size()) {
                            yield true;
                        }
                        focusIconPickerOptionByIndex(nextIndex);
                        yield true;
                    }
                    default -> false;
                };
            }
        }

        return false;
    }

    private void focusIconPickerBackButton() {
        for (int index = 0; index < actions.size(); index++) {
            if (isActionAtBounds(
                    index,
                    iconPickerBackButtonX,
                    iconPickerBackButtonY,
                    iconPickerBackButtonWidth,
                    iconPickerBackButtonHeight
            )) {
                focusAction(index);
                return;
            }
        }
    }

    private void focusFirstIconPickerIcon() {
        int firstIconActionIndex = firstIconPickerActionIndex();
        if (firstIconActionIndex >= 0) {
            focusAction(firstIconActionIndex);
        }
    }

    private void focusIconPickerOptionByIndex(int optionIndex) {
        List<IconPickerOption> options = filteredIconPickerOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) {
            return;
        }
        ensureIconPickerOptionVisible(optionIndex, options);
        pendingIconPickerFocusId = options.get(optionIndex).id();
        clearFocus();
        activeList = NavigationList.EDITOR;
    }

    private int currentIconPickerOptionIndex() {
        if (focusedActionIndex < 0 || focusedActionIndex >= actions.size()) {
            return -1;
        }
        ClickAction action = actions.get(focusedActionIndex);
        if (!action.enabled || !isActionInIconPicker(action)) {
            return -1;
        }
        List<IconPickerOption> options = filteredIconPickerOptions();
        if (options.isEmpty()) {
            return -1;
        }
        int stride = ICON_PICKER_CELL_SIZE + ICON_PICKER_CELL_GAP;
        int columns = iconPickerColumnCount();
        int usableGridWidth = Math.max(0, iconPickerGridWidth - (ICON_PICKER_CONTENT_PADDING * 2));
        int contentWidth = (columns * ICON_PICKER_CELL_SIZE) + ((columns - 1) * ICON_PICKER_CELL_GAP);
        int horizontalInset = Math.max(0, (usableGridWidth - contentWidth) / 2);
        int column = (action.x - (iconPickerGridX + ICON_PICKER_CONTENT_PADDING + horizontalInset)) / stride;
        int row = (action.y - (iconPickerGridY + ICON_PICKER_CONTENT_PADDING) + iconPickerScrollOffset) / stride;
        int optionIndex = (row * columns) + column;
        return optionIndex >= 0 && optionIndex < options.size() ? optionIndex : -1;
    }

    private int iconPickerColumnCount() {
        int usableGridWidth = Math.max(0, iconPickerGridWidth - (ICON_PICKER_CONTENT_PADDING * 2));
        return Math.max(1, (usableGridWidth + ICON_PICKER_CELL_GAP) / (ICON_PICKER_CELL_SIZE + ICON_PICKER_CELL_GAP));
    }

    private void ensureIconPickerOptionVisible(int optionIndex, List<IconPickerOption> options) {
        if (iconPickerGridWidth <= 0 || iconPickerGridHeight <= 0 || options.isEmpty()) {
            return;
        }
        int columns = iconPickerColumnCount();
        int row = optionIndex / columns;
        int rowTop = row * (ICON_PICKER_CELL_SIZE + ICON_PICKER_CELL_GAP);
        int rowBottom = rowTop + ICON_PICKER_CELL_SIZE;
        int usableGridHeight = Math.max(0, iconPickerGridHeight - (ICON_PICKER_CONTENT_PADDING * 2));
        if (rowTop < iconPickerScrollOffset) {
            iconPickerScrollOffset = rowTop;
        } else if (rowBottom > iconPickerScrollOffset + usableGridHeight) {
            iconPickerScrollOffset = rowBottom - usableGridHeight;
        }
        iconPickerScrollOffset = clamp(iconPickerScrollOffset, 0, maxIconPickerScrollOffset());
    }

    private boolean handleCommandFieldCharTyped(char codePoint, int modifiers) {
        return textEntryActive && focusedCommandFieldIndex >= 0 && ensureCommandInput() && commandInput.charTyped(codePoint, modifiers);
    }

    private boolean handleCommandSuggestionsClick(double mouseX, double mouseY, int button) {
        return focusedCommandFieldIndex >= 0
                && ensureCommandInput()
                && commandSuggestions.mouseClicked(mouseX, mouseY - commandSuggestionsYOffset, button);
    }

    private boolean handleCommandSuggestionsScroll(double scrollY) {
        return focusedCommandFieldIndex >= 0 && ensureCommandInput() && commandSuggestions.mouseScrolled(scrollY);
    }

    private boolean runFocusedAction() {
        if (focusedActionIndex < 0 || focusedActionIndex >= actions.size()) {
            return false;
        }
        ClickAction action = actions.get(focusedActionIndex);
        if (!action.enabled) {
            return false;
        }
        action.handler.run();
        syncSelection(false);
        return true;
    }

    private boolean focusFirstVisibleTarget() {
        List<FocusTarget> targets = visibleFocusTargets();
        if (targets.isEmpty()) {
            return false;
        }
        focusTarget(targets.getFirst());
        return true;
    }

    private boolean focusFirstActionInZone(NavigationList zone) {
        for (int index = 0; index < actions.size(); index++) {
            ClickAction action = actions.get(index);
            if (action.enabled && navigationListForAction(action) == zone) {
                focusAction(index);
                return true;
            }
        }
        return false;
    }

    private boolean navigateEditorFocus(int keyCode) {
        if (activeList != NavigationList.EDITOR || !isDirectionalNavigationKey(keyCode)) {
            return false;
        }

        FocusTarget currentTarget = currentFocusTarget();
        if (currentTarget == null) {
            return false;
        }

        if (iconPickerOpen
                && focusedField == EditorField.ICON_PICKER_SEARCH
                && keyCode == GLFW.GLFW_KEY_RIGHT) {
            for (int index = 0; index < actions.size(); index++) {
                ClickAction action = actions.get(index);
                if (action.enabled && isActionAtBounds(
                        index,
                        iconPickerBackButtonX,
                        iconPickerBackButtonY,
                        iconPickerBackButtonWidth,
                        iconPickerBackButtonHeight
                )) {
                    focusAction(index);
                    return true;
                }
            }
        }
        if (iconPickerOpen
                && focusedField == EditorField.ICON_PICKER_SEARCH
                && keyCode == GLFW.GLFW_KEY_DOWN) {
            int firstIconActionIndex = firstIconPickerActionIndex();
            if (firstIconActionIndex >= 0) {
                focusAction(firstIconActionIndex);
                return true;
            }
        }

        if (focusedCommandFieldIndex >= 0
                && keyCode == GLFW.GLFW_KEY_DOWN
                && focusedCommandFieldIndex == commandValueFields.size() - 1) {
            int addCommandActionIndex = findActionBelowCommandFields();
            if (addCommandActionIndex >= 0) {
                focusAction(addCommandActionIndex);
                return true;
            }
        }
        if (focusedCommandFieldIndex >= 0 && keyCode == GLFW.GLFW_KEY_UP) {
            if (focusedCommandFieldIndex > 0) {
                focusCommandField(focusedCommandFieldIndex - 1);
            } else {
                focusField(EditorField.ENTRY_SHORTCUT);
            }
            return true;
        }
        if (focusedActionIndex >= 0
                && keyCode == GLFW.GLFW_KEY_LEFT
                && isActionAtBounds(focusedActionIndex, shortcutToggleButtonX, shortcutToggleButtonY, shortcutToggleButtonWidth, shortcutToggleButtonHeight)) {
            focusField(EditorField.ENTRY_SHORTCUT);
            return true;
        }
        if (iconPickerOpen
                && focusedActionIndex >= 0
                && keyCode == GLFW.GLFW_KEY_LEFT
                && isActionAtBounds(
                        focusedActionIndex,
                        iconPickerBackButtonX,
                        iconPickerBackButtonY,
                        iconPickerBackButtonWidth,
                        iconPickerBackButtonHeight
                )) {
            focusField(EditorField.ICON_PICKER_SEARCH);
            return true;
        }

        FocusBounds currentBounds = focusBounds(currentTarget);
        if (currentBounds == null) {
            return false;
        }
        boolean currentFieldLike = isFieldLike(currentTarget);

        List<FocusTarget> targets = visibleFocusTargets(NavigationList.EDITOR);
        int bestIndex = -1;
        double bestScore = Double.MAX_VALUE;

        for (int index = 0; index < targets.size(); index++) {
            FocusTarget candidateTarget = targets.get(index);
            if (candidateTarget.equals(currentTarget)) {
                continue;
            }

            FocusBounds candidateBounds = focusBounds(candidateTarget);
            if (candidateBounds == null) {
                continue;
            }
            boolean candidateFieldLike = isFieldLike(candidateTarget);

            float deltaX = candidateBounds.centerX() - currentBounds.centerX();
            float deltaY = candidateBounds.centerY() - currentBounds.centerY();
            if (!isCandidateInDirection(keyCode, deltaX, deltaY)) {
                continue;
            }

            boolean sameLane = switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> candidateBounds.bottom() > currentBounds.top() && candidateBounds.top() < currentBounds.bottom();
                case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> candidateBounds.right() > currentBounds.left() && candidateBounds.left() < currentBounds.right();
                default -> false;
            };

            double primaryDistance = switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> Math.max(0, currentBounds.left() - candidateBounds.right());
                case GLFW.GLFW_KEY_RIGHT -> Math.max(0, candidateBounds.left() - currentBounds.right());
                case GLFW.GLFW_KEY_UP -> Math.max(0, currentBounds.top() - candidateBounds.bottom());
                case GLFW.GLFW_KEY_DOWN -> Math.max(0, candidateBounds.top() - currentBounds.bottom());
                default -> 0.0D;
            };
            double secondaryDistance = switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> Math.abs(deltaY);
                case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> Math.abs(deltaX);
                default -> 0.0D;
            };

            double lanePenalty = sameLane ? 0.0D : 1_000_000.0D;
            double typePenalty = currentFieldLike && !candidateFieldLike ? 100_000.0D : 0.0D;
            double score = lanePenalty + typePenalty + (primaryDistance * 1000.0D) + (secondaryDistance * secondaryDistance);
            if (score < bestScore) {
                bestScore = score;
                bestIndex = index;
            }
        }

        if (bestIndex >= 0) {
            focusTarget(targets.get(bestIndex));
            return true;
        }
        return false;
    }

    private int findActionBelowCommandFields() {
        if (commandValueFields.isEmpty()) {
            return -1;
        }
        TextFieldState lastField = commandValueFields.getLast();
        int bestIndex = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < actions.size(); index++) {
            ClickAction action = actions.get(index);
            if (!action.enabled || navigationListForAction(action) != NavigationList.EDITOR) {
                continue;
            }
            boolean horizontalOverlap = action.x + action.width > lastField.x && action.x < lastField.x + lastField.width;
            if (!horizontalOverlap || action.y < lastField.y + lastField.height) {
                continue;
            }
            int distance = action.y - (lastField.y + lastField.height);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private int firstIconPickerActionIndex() {
        for (int index = 0; index < actions.size(); index++) {
            ClickAction action = actions.get(index);
            if (action.enabled && isActionInIconPicker(action)) {
                return index;
            }
        }
        return -1;
    }

    private boolean isLeftmostIconPickerAction(int actionIndex) {
        if (actionIndex < 0 || actionIndex >= actions.size()) {
            return false;
        }
        ClickAction current = actions.get(actionIndex);
        if (!current.enabled || !isActionInIconPicker(current)) {
            return false;
        }
        int leftmostX = Integer.MAX_VALUE;
        for (ClickAction action : actions) {
            if (action.enabled && isActionInIconPicker(action)) {
                leftmostX = Math.min(leftmostX, action.x);
            }
        }
        return leftmostX != Integer.MAX_VALUE && current.x <= leftmostX;
    }

    private boolean isRightmostIconPickerAction(int actionIndex) {
        if (actionIndex < 0 || actionIndex >= actions.size()) {
            return false;
        }
        ClickAction current = actions.get(actionIndex);
        if (!current.enabled || !isActionInIconPicker(current)) {
            return false;
        }
        int rightmostX = Integer.MIN_VALUE;
        for (ClickAction action : actions) {
            if (action.enabled && isActionInIconPicker(action)) {
                rightmostX = Math.max(rightmostX, action.x);
            }
        }
        return rightmostX != Integer.MIN_VALUE && current.x >= rightmostX;
    }

    private boolean isActionAtBounds(int actionIndex, int x, int y, int width, int height) {
        if (actionIndex < 0 || actionIndex >= actions.size()) {
            return false;
        }
        ClickAction action = actions.get(actionIndex);
        return action.x == x && action.y == y && action.width == width && action.height == height;
    }

    private boolean isActionWithinBounds(int actionIndex, int x, int y, int width, int height) {
        if (actionIndex < 0 || actionIndex >= actions.size()) {
            return false;
        }
        ClickAction action = actions.get(actionIndex);
        return action.x >= x
                && action.y >= y
                && action.x + action.width <= x + width
                && action.y + action.height <= y + height;
    }

    private boolean shouldLeaveEditorToEntriesOnLeft() {
        return focusedCommandFieldIndex < 0 && switch (focusedField) {
            case ENTRY_LABEL, ENTRY_GLYPH, ENTRY_DESCRIPTION, ENTRY_SHORTCUT -> true;
            default -> false;
        };
    }

    private boolean shouldStayOnEditorFieldOnRight() {
        return focusedCommandFieldIndex < 0 && focusedField == EditorField.ENTRY_DESCRIPTION;
    }

    private boolean clearHighlightedShortcutField() {
        if (textEntryActive || focusedCommandFieldIndex >= 0 || focusedField != EditorField.ENTRY_SHORTCUT) {
            return false;
        }
        textFields.get(EditorField.ENTRY_SHORTCUT).setValue("");
        entryShortcutDraft = "";
        autosaveCurrentEntryDraft();
        return true;
    }

    private boolean isFieldLike(FocusTarget target) {
        return target.field() != null || target.commandFieldIndex() != null;
    }

    private boolean focusFirstLowerEditorTarget() {
        if (entryActionTypeDraft == DraftActionType.COMMAND && !commandValueFields.isEmpty()) {
            focusCommandField(0);
            return true;
        }
        if (textFields.get(EditorField.ENTRY_VALUE).visible) {
            focusField(EditorField.ENTRY_VALUE);
            return true;
        }
        return false;
    }

    private FocusBounds focusBounds(FocusTarget target) {
        if (target.field() != null) {
            TextFieldState field = textFields.get(target.field());
            if (field == null || !field.visible) {
                return null;
            }
            return new FocusBounds(field.x, field.y, field.width, field.height);
        }
        if (target.commandFieldIndex() != null) {
            int index = target.commandFieldIndex();
            if (index < 0 || index >= commandValueFields.size()) {
                return null;
            }
            TextFieldState field = commandValueFields.get(index);
            if (!field.visible) {
                return null;
            }
            return new FocusBounds(field.x, field.y, field.width, field.height);
        }
        if (target.actionIndex() != null) {
            int index = target.actionIndex();
            if (index < 0 || index >= actions.size()) {
                return null;
            }
            ClickAction action = actions.get(index);
            if (!action.enabled) {
                return null;
            }
            return new FocusBounds(action.x, action.y, action.width, action.height);
        }
        return null;
    }

    private NavigationList navigationListForField(EditorField fieldId) {
        return switch (fieldId) {
            case PROFILE_NAME -> NavigationList.PROFILES;
            case WHEEL_TITLE -> NavigationList.WHEELS;
            case ICON_PICKER_SEARCH, ENTRY_LABEL, ENTRY_DESCRIPTION, ENTRY_SHORTCUT, ENTRY_GLYPH, ENTRY_VALUE -> NavigationList.EDITOR;
        };
    }

    private NavigationList navigationListForAction(ClickAction action) {
        if (iconPickerOpen || isActionInIconPicker(action) || isActionInFunctionList(action)) {
            return NavigationList.EDITOR;
        }
        if (action.x >= profilesAreaX
                && action.x + action.width <= profilesAreaX + profilesAreaWidth
                && action.y >= profilesAreaY
                && action.y + action.height <= profilesActionY + 114) {
            return NavigationList.PROFILES;
        }
        if (action.x >= wheelsAreaX
                && action.x + action.width <= wheelsAreaX + wheelsAreaWidth
                && action.y >= wheelsAreaY
                && action.y + action.height <= wheelsActionY + 114) {
            return NavigationList.WHEELS;
        }
        return NavigationList.EDITOR;
    }

    private boolean navigateActionFocus(int keyCode) {
        if (!isDirectionalNavigationKey(keyCode) || actions.isEmpty()) {
            return false;
        }
        if (focusedActionIndex < 0 || focusedActionIndex >= actions.size()) {
            for (int index = 0; index < actions.size(); index++) {
                if (actions.get(index).enabled) {
                    focusAction(index);
                    return true;
                }
            }
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_UP
                && activeList == NavigationList.EDITOR
                && isActionAtBounds(focusedActionIndex, addCommandButtonX, addCommandButtonY, addCommandButtonWidth, addCommandButtonHeight)
                && !commandValueFields.isEmpty()) {
            focusCommandField(commandValueFields.size() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT
                && activeList == NavigationList.EDITOR
                && isActionAtBounds(focusedActionIndex, addCommandButtonX, addCommandButtonY, addCommandButtonWidth, addCommandButtonHeight)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN
                && activeList == NavigationList.EDITOR
                && isActionWithinBounds(focusedActionIndex, nameColorPaletteX, nameColorPaletteY, nameColorPaletteWidth, nameColorPaletteHeight)) {
            focusField(EditorField.ENTRY_DESCRIPTION);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT
                && activeList == NavigationList.EDITOR
                && isActionAtBounds(focusedActionIndex, shortcutToggleButtonX, shortcutToggleButtonY, shortcutToggleButtonWidth, shortcutToggleButtonHeight)) {
            focusField(EditorField.ENTRY_SHORTCUT);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP
                && activeList == NavigationList.EDITOR
                && isActionAtBounds(focusedActionIndex, shortcutButtonX, shortcutButtonY, shortcutButtonWidth, shortcutButtonHeight)) {
            focusField(EditorField.ENTRY_DESCRIPTION);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN
                && activeList == NavigationList.EDITOR
                && isActionAtBounds(focusedActionIndex, shortcutButtonX, shortcutButtonY, shortcutButtonWidth, shortcutButtonHeight)
                && focusFirstLowerEditorTarget()) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP
                && activeList == NavigationList.EDITOR
                && isActionAtBounds(focusedActionIndex, shortcutToggleButtonX, shortcutToggleButtonY, shortcutToggleButtonWidth, shortcutToggleButtonHeight)) {
            focusField(EditorField.ENTRY_DESCRIPTION);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN
                && activeList == NavigationList.EDITOR
                && isActionAtBounds(focusedActionIndex, shortcutToggleButtonX, shortcutToggleButtonY, shortcutToggleButtonWidth, shortcutToggleButtonHeight)
                && focusFirstLowerEditorTarget()) {
            return true;
        }
        if (iconPickerOpen
                && keyCode == GLFW.GLFW_KEY_LEFT
                && isActionAtBounds(
                        focusedActionIndex,
                        iconPickerBackButtonX,
                        iconPickerBackButtonY,
                        iconPickerBackButtonWidth,
                        iconPickerBackButtonHeight
                )) {
            focusField(EditorField.ICON_PICKER_SEARCH);
            return true;
        }
        if (iconPickerOpen
                && keyCode == GLFW.GLFW_KEY_DOWN
                && isActionAtBounds(
                        focusedActionIndex,
                        iconPickerBackButtonX,
                        iconPickerBackButtonY,
                        iconPickerBackButtonWidth,
                        iconPickerBackButtonHeight
                )) {
            int firstIconActionIndex = firstIconPickerActionIndex();
            if (firstIconActionIndex >= 0) {
                focusAction(firstIconActionIndex);
            }
            return true;
        }
        if (iconPickerOpen
                && keyCode == GLFW.GLFW_KEY_LEFT
                && focusedActionIndex >= 0
                && isLeftmostIconPickerAction(focusedActionIndex)) {
            return true;
        }
        if (iconPickerOpen
                && keyCode == GLFW.GLFW_KEY_RIGHT
                && focusedActionIndex >= 0
                && isRightmostIconPickerAction(focusedActionIndex)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT
                && activeList == NavigationList.EDITOR
                && isActionAtBounds(focusedActionIndex, labelToggleButtonX, labelToggleButtonY, labelToggleButtonWidth, labelToggleButtonHeight)) {
            focusField(EditorField.ENTRY_LABEL);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN
                && activeList == NavigationList.EDITOR
                && (isActionAtBounds(focusedActionIndex, iconToggleButtonX, iconToggleButtonY, iconToggleButtonWidth, iconToggleButtonHeight)
                || isActionAtBounds(focusedActionIndex, iconButtonX, iconButtonY, iconButtonWidth, iconButtonHeight))) {
            focusField(EditorField.ENTRY_DESCRIPTION);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP
                && activeList == NavigationList.EDITOR
                && isActionAtBounds(focusedActionIndex, iconToggleButtonX, iconToggleButtonY, iconToggleButtonWidth, iconToggleButtonHeight)) {
            focusField(EditorField.ENTRY_LABEL);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT
                && activeList == NavigationList.EDITOR
                && isActionAtBounds(focusedActionIndex, iconToggleButtonX, iconToggleButtonY, iconToggleButtonWidth, iconToggleButtonHeight)) {
            focusField(EditorField.ENTRY_GLYPH);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT && activeList == NavigationList.EDITOR) {
            int commandIndex = findCommandIndexForRemoveAction(focusedActionIndex);
            if (commandIndex >= 0) {
                focusCommandField(commandIndex);
                return true;
            }
        }

        ClickAction current = actions.get(focusedActionIndex);
        if (iconPickerOpen && keyCode == GLFW.GLFW_KEY_UP && isActionInIconPicker(current)) {
            focusField(EditorField.ICON_PICKER_SEARCH);
            return true;
        }
        NavigationList currentZone = navigationListForAction(current);
        float currentCenterX = current.x + (current.width * 0.5F);
        float currentCenterY = current.y + (current.height * 0.5F);
        int currentLeft = current.x;
        int currentRight = current.x + current.width;
        int currentTop = current.y;
        int currentBottom = current.y + current.height;
        int bestIndex = -1;
        double bestScore = Double.MAX_VALUE;

        for (int index = 0; index < actions.size(); index++) {
            if (index == focusedActionIndex) {
                continue;
            }
            ClickAction candidate = actions.get(index);
            if (!candidate.enabled) {
                continue;
            }
              if (!iconPickerOpen && navigationListForAction(candidate) != currentZone) {
                  continue;
              }
              float candidateCenterX = candidate.x + (candidate.width * 0.5F);
              float candidateCenterY = candidate.y + (candidate.height * 0.5F);
              int candidateLeft = candidate.x;
              int candidateRight = candidate.x + candidate.width;
              int candidateTop = candidate.y;
              int candidateBottom = candidate.y + candidate.height;
              float deltaX = candidateCenterX - currentCenterX;
              float deltaY = candidateCenterY - currentCenterY;
              if (!isCandidateInDirection(keyCode, deltaX, deltaY)) {
                  continue;
              }
              boolean sameLane = switch (keyCode) {
                  case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> candidateBottom > currentTop && candidateTop < currentBottom;
                  case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> candidateRight > currentLeft && candidateLeft < currentRight;
                  default -> false;
              };
              double primaryDistance = switch (keyCode) {
                  case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> Math.abs(deltaX);
                  case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> Math.abs(deltaY);
                  default -> 0.0D;
              };
            double secondaryDistance = switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> Math.abs(deltaY);
                  case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> Math.abs(deltaX);
                  default -> 0.0D;
              };
              double lanePenalty = sameLane ? 0.0D : 1_000_000.0D;
              double score = lanePenalty + (primaryDistance * 1000.0D) + (secondaryDistance * secondaryDistance);
              if (score < bestScore) {
                  bestScore = score;
                  bestIndex = index;
              }
          }

        if (bestIndex >= 0) {
            focusAction(bestIndex);
            return true;
        }
        return false;
    }

    private boolean handleFocusedActionPaging(int keyCode) {
        if (focusedActionIndex < 0 || focusedActionIndex >= actions.size()) {
            return false;
        }
        ClickAction action = actions.get(focusedActionIndex);
        if (isActionInIconPicker(action)) {
            int pageStep = Math.max(1, Math.max(1, iconPickerGridHeight - (ICON_PICKER_CONTENT_PADDING * 2)));
            return switch (keyCode) {
                case GLFW.GLFW_KEY_PAGE_UP -> scrollIconPickerBy(-pageStep);
                case GLFW.GLFW_KEY_PAGE_DOWN -> scrollIconPickerBy(pageStep);
                case GLFW.GLFW_KEY_HOME -> scrollIconPickerTo(0);
                case GLFW.GLFW_KEY_END -> scrollIconPickerTo(maxIconPickerScrollOffset());
                default -> false;
            };
        }
        if (isActionInFunctionList(action)) {
            int pageStep = Math.max(1, Math.max(1, functionListHeight - 12));
            return switch (keyCode) {
                case GLFW.GLFW_KEY_PAGE_UP -> scrollFunctionListBy(-pageStep);
                case GLFW.GLFW_KEY_PAGE_DOWN -> scrollFunctionListBy(pageStep);
                case GLFW.GLFW_KEY_HOME -> scrollFunctionListTo(0);
                case GLFW.GLFW_KEY_END -> scrollFunctionListTo(maxFunctionListScrollOffset());
                default -> false;
            };
        }
        return false;
    }

    private int findCommandIndexForRemoveAction(int actionIndex) {
        if (actionIndex < 0 || actionIndex >= actions.size()) {
            return -1;
        }
        ClickAction action = actions.get(actionIndex);
        for (int index = 0; index < commandValueFields.size(); index++) {
            TextFieldState field = commandValueFields.get(index);
            if (!field.visible) {
                continue;
            }
            boolean sameRow = action.y == field.y && action.height == field.height;
            boolean toRightOfField = action.x >= field.x + field.width;
            if (sameRow && toRightOfField) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isDirectionalNavigationKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT
                || keyCode == GLFW.GLFW_KEY_RIGHT
                || keyCode == GLFW.GLFW_KEY_UP
                || keyCode == GLFW.GLFW_KEY_DOWN;
    }

    private static boolean isPagingNavigationKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_PAGE_UP
                || keyCode == GLFW.GLFW_KEY_PAGE_DOWN
                || keyCode == GLFW.GLFW_KEY_HOME
                || keyCode == GLFW.GLFW_KEY_END;
    }

    private boolean shouldSuppressDirectionalNavigation(int keyCode) {
        return isDirectionalNavigationKey(keyCode) && pressedDirectionalKeyCount() > 1;
    }

    private int pressedDirectionalKeyCount() {
        long windowHandle = resolveWindowHandleStatic(Minecraft.getInstance().getWindow());
        int count = 0;
        if (InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT)) {
            count++;
        }
        if (InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT)) {
            count++;
        }
        if (InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_UP)) {
            count++;
        }
        if (InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_DOWN)) {
            count++;
        }
        return count;
    }

    private static boolean isCandidateInDirection(int keyCode, float deltaX, float deltaY) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> deltaX < -1.0F;
            case GLFW.GLFW_KEY_RIGHT -> deltaX > 1.0F;
            case GLFW.GLFW_KEY_UP -> deltaY < -1.0F;
            case GLFW.GLFW_KEY_DOWN -> deltaY > 1.0F;
            default -> false;
        };
    }

    private void ensureFocusedActionStillVisible() {
        if (focusedActionIndex < 0 || focusedActionIndex >= actions.size()) {
            return;
        }
        ClickAction action = actions.get(focusedActionIndex);
        if (isActionFullyVisible(action)) {
            return;
        }
        focusedActionIndex = -1;
    }

    private boolean isActionFullyVisible(ClickAction action) {
        if (isActionInCommandList(action)) {
            return action.y >= commandListY
                    && action.y + action.height <= commandListY + commandListHeight;
        }
        if (isActionInFunctionList(action)) {
            return action.y >= functionListY
                    && action.y + action.height <= functionListY + functionListHeight;
        }
        if (isActionInIconPicker(action)) {
            return action.y >= iconPickerGridY
                    && action.y + action.height <= iconPickerGridY + iconPickerGridHeight;
        }
        return true;
    }

    private boolean isActionInFunctionList(ClickAction action) {
        return action.x >= functionListX
                && action.x + action.width <= functionListX + functionListWidth
                && action.y + action.height > functionListY
                && action.y < functionListY + functionListHeight;
    }

    private boolean isActionInCommandList(ClickAction action) {
        return action.x >= commandListX
                && action.x + action.width <= commandListX + Math.max(0, commandListWidth + SCROLLBAR_GAP + SCROLLBAR_WIDTH)
                && action.y + action.height > commandListY
                && action.y < commandListY + commandListHeight;
    }

    private boolean isActionInIconPicker(ClickAction action) {
        return action.x >= iconPickerGridX
                && action.x + action.width <= iconPickerGridX + iconPickerGridWidth
                && action.y + action.height > iconPickerGridY
                && action.y < iconPickerGridY + iconPickerGridHeight;
    }

    private boolean ensureCommandInput() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen hostScreen = hostScreen();
        if (hostScreen == null) {
            clearCommandInput();
            return false;
        }

        if (commandInput == null) {
            commandInput = new EditBox(minecraft.font, 0, 0, 100, 12, OmniWheelText.component("omniwheel.action.command"));
            commandInput.setMaxLength(256);
            commandInput.setBordered(false);
            commandInput.setTextColor(TEXT);
            commandInput.setTextColorUneditable(TEXT);
            commandInput.setCanLoseFocus(false);
            commandInput.setResponder(this::handleCommandInputChanged);
            commandInput.setVisible(false);
        }

        if (commandSuggestions == null || commandSuggestionsScreen != hostScreen) {
            commandSuggestionsScreen = hostScreen;
            commandSuggestions = new CommandSuggestions(minecraft, hostScreen, commandInput, minecraft.font, true, true, 0, 7, false, Integer.MIN_VALUE);
            commandSuggestions.setAllowSuggestions(true);
        }

        hostScreen.setFocused(commandInput);
        commandInput.setFocused(true);
        syncActiveCommandInput();
        return true;
    }

    private void syncActiveCommandInput() {
        if (focusedCommandFieldIndex < 0 || commandInput == null || focusedCommandFieldIndex >= commandValueDrafts.size()) {
            return;
        }
        String value = commandValueDrafts.get(focusedCommandFieldIndex);
        syncingCommandInput = true;
        if (!Objects.equals(commandInput.getValue(), value)) {
            commandInput.setValue(value);
        }
        syncingCommandInput = false;
        if (commandSuggestions != null) {
            commandSuggestions.updateCommandInfo();
        }
    }

    private void handleCommandInputChanged(String value) {
        if (syncingCommandInput || focusedCommandFieldIndex < 0 || focusedCommandFieldIndex >= commandValueDrafts.size()) {
            return;
        }
        applyCommandFieldDraft(focusedCommandFieldIndex, value);
        if (commandSuggestions != null) {
            commandSuggestions.updateCommandInfo();
        }
    }

    private boolean isUsingVanillaCommandInput(int index) {
        return textEntryActive && focusedCommandFieldIndex == index && ensureCommandInput();
    }

    private Screen hostScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        return screenHosted && minecraft.screen instanceof Screen screen ? screen : null;
    }

    private void clearCommandInput() {
        if (commandInput != null) {
            commandInput.setFocused(false);
            commandInput.setVisible(false);
        }
        commandSuggestionsYOffset = 0;
        commandSuggestions = null;
        commandSuggestionsScreen = null;
    }

    private void suppressGameplayInput(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        for (KeyMapping keyMapping : gameplayKeyMappings(minecraft)) {
            keyMapping.setDown(false);
        }
        minecraft.player.xxa = 0.0F;
        minecraft.player.yya = 0.0F;
        minecraft.player.zza = 0.0F;
        minecraft.player.setJumping(false);
        minecraft.player.setSprinting(false);
    }

    private void restoreGameplayInput(Minecraft minecraft) {
        long handle = resolveWindowHandle(minecraft.getWindow());
        for (KeyMapping keyMapping : gameplayKeyMappings(minecraft)) {
            if (keyMapping.getKey().getType() == InputConstants.Type.KEYSYM) {
                keyMapping.setDown(InputConstants.isKeyDown(handle, keyMapping.getKey().getValue()));
            }
        }
    }

    private static List<KeyMapping> gameplayKeyMappings(Minecraft minecraft) {
        return List.of(
                minecraft.options.keyUp,
                minecraft.options.keyDown,
                minecraft.options.keyLeft,
                minecraft.options.keyRight,
                minecraft.options.keyJump,
                minecraft.options.keyShift,
                minecraft.options.keySprint,
                minecraft.options.keyAttack,
                minecraft.options.keyUse,
                minecraft.options.keyPickItem
        );
    }

    private boolean isShiftModifierDown() {
        Minecraft minecraft = Minecraft.getInstance();
        long handle = resolveWindowHandle(minecraft.getWindow());
        return InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private boolean isControlModifierDown() {
        Minecraft minecraft = Minecraft.getInstance();
        long handle = resolveWindowHandle(minecraft.getWindow());
        return InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static Character typedCharacterForKey(int key, boolean shiftDown) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            char base = (char) ('a' + (key - GLFW.GLFW_KEY_A));
            return shiftDown ? Character.toUpperCase(base) : base;
        }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            return switch (key) {
                case GLFW.GLFW_KEY_0 -> shiftDown ? ')' : '0';
                case GLFW.GLFW_KEY_1 -> shiftDown ? '!' : '1';
                case GLFW.GLFW_KEY_2 -> shiftDown ? '@' : '2';
                case GLFW.GLFW_KEY_3 -> shiftDown ? '#' : '3';
                case GLFW.GLFW_KEY_4 -> shiftDown ? '$' : '4';
                case GLFW.GLFW_KEY_5 -> shiftDown ? '%' : '5';
                case GLFW.GLFW_KEY_6 -> shiftDown ? '^' : '6';
                case GLFW.GLFW_KEY_7 -> shiftDown ? '&' : '7';
                case GLFW.GLFW_KEY_8 -> shiftDown ? '*' : '8';
                case GLFW.GLFW_KEY_9 -> shiftDown ? '(' : '9';
                default -> null;
            };
        }
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE -> ' ';
            case GLFW.GLFW_KEY_APOSTROPHE -> shiftDown ? '"' : '\'';
            case GLFW.GLFW_KEY_COMMA -> shiftDown ? '<' : ',';
            case GLFW.GLFW_KEY_MINUS -> shiftDown ? '_' : '-';
            case GLFW.GLFW_KEY_PERIOD -> shiftDown ? '>' : '.';
            case GLFW.GLFW_KEY_SLASH -> shiftDown ? '?' : '/';
            case GLFW.GLFW_KEY_SEMICOLON -> shiftDown ? ':' : ';';
            case GLFW.GLFW_KEY_EQUAL -> shiftDown ? '+' : '=';
            case GLFW.GLFW_KEY_LEFT_BRACKET -> shiftDown ? '{' : '[';
            case GLFW.GLFW_KEY_BACKSLASH -> shiftDown ? '|' : '\\';
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> shiftDown ? '}' : ']';
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> shiftDown ? '~' : '`';
            default -> null;
        };
    }

    private static Map<EditorField, TextFieldState> createTextFields() {
        Map<EditorField, TextFieldState> fields = new LinkedHashMap<>();
        fields.put(EditorField.PROFILE_NAME, new TextFieldState(64));
        fields.put(EditorField.WHEEL_TITLE, new TextFieldState(64));
        fields.put(EditorField.ICON_PICKER_SEARCH, new TextFieldState(64));
        fields.put(EditorField.ENTRY_LABEL, new TextFieldState(64));
        fields.put(EditorField.ENTRY_DESCRIPTION, new TextFieldState(160));
        fields.put(EditorField.ENTRY_SHORTCUT, new TextFieldState(64));
        fields.put(EditorField.ENTRY_GLYPH, new TextFieldState(128));
        fields.put(EditorField.ENTRY_VALUE, new TextFieldState(256));
        return fields;
    }

    private static int[] createPolledKeys() {
        List<Integer> keys = new ArrayList<>();
        for (int key = GLFW.GLFW_KEY_A; key <= GLFW.GLFW_KEY_Z; key++) {
            keys.add(key);
        }
        for (int key = GLFW.GLFW_KEY_0; key <= GLFW.GLFW_KEY_9; key++) {
            keys.add(key);
        }
        keys.add(GLFW.GLFW_KEY_SPACE);
        keys.add(GLFW.GLFW_KEY_APOSTROPHE);
        keys.add(GLFW.GLFW_KEY_COMMA);
        keys.add(GLFW.GLFW_KEY_MINUS);
        keys.add(GLFW.GLFW_KEY_PERIOD);
        keys.add(GLFW.GLFW_KEY_SLASH);
        keys.add(GLFW.GLFW_KEY_SEMICOLON);
        keys.add(GLFW.GLFW_KEY_EQUAL);
        keys.add(GLFW.GLFW_KEY_LEFT_BRACKET);
        keys.add(GLFW.GLFW_KEY_BACKSLASH);
        keys.add(GLFW.GLFW_KEY_RIGHT_BRACKET);
        keys.add(GLFW.GLFW_KEY_GRAVE_ACCENT);
        keys.add(GLFW.GLFW_KEY_BACKSPACE);
        keys.add(GLFW.GLFW_KEY_DELETE);
        keys.add(GLFW.GLFW_KEY_LEFT);
        keys.add(GLFW.GLFW_KEY_RIGHT);
        keys.add(GLFW.GLFW_KEY_HOME);
        keys.add(GLFW.GLFW_KEY_END);
        keys.add(GLFW.GLFW_KEY_TAB);
        keys.add(GLFW.GLFW_KEY_ENTER);
        keys.add(GLFW.GLFW_KEY_KP_ENTER);
        return keys.stream().mapToInt(Integer::intValue).toArray();
    }

    private enum DraftActionType {
        COMMAND,
        CHAT,
        FUNCTION,
        LOCAL_MESSAGE,
        OPEN_CHAT,
        COPY_TEXT,
        OPEN_SCREEN,
        SUBMENU;

        private DraftActionType next() {
            return switch (this) {
                case COMMAND -> CHAT;
                case CHAT -> FUNCTION;
                case FUNCTION -> SUBMENU;
                case LOCAL_MESSAGE, OPEN_CHAT, COPY_TEXT, OPEN_SCREEN, SUBMENU -> COMMAND;
            };
        }

        private static DraftActionType fromAction(WheelAction action) {
            return switch (action) {
                case CommandAction ignored -> COMMAND;
                case ChatAction ignored -> CHAT;
                case FunctionAction ignored -> FUNCTION;
                case LocalMessageAction ignored -> LOCAL_MESSAGE;
                case OpenChatAction ignored -> OPEN_CHAT;
                case CopyTextAction ignored -> COPY_TEXT;
                case OpenScreenAction ignored -> OPEN_SCREEN;
                case OpenWheelAction ignored -> SUBMENU;
                default -> COMMAND;
            };
        }
    }

    private record ClickAction(int x, int y, int width, int height, boolean enabled, Runnable handler) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private record TooltipRegion(int x, int y, int width, int height, String text) {
    }

    private enum EditorField {
        PROFILE_NAME,
        WHEEL_TITLE,
        ICON_PICKER_SEARCH,
        ENTRY_LABEL,
        ENTRY_DESCRIPTION,
        ENTRY_SHORTCUT,
        ENTRY_GLYPH,
        ENTRY_VALUE
    }

    private enum NavigationList {
        PROFILES,
        WHEELS,
        EDITOR
    }

    private static final class TextFieldState {
        private final int maxLength;
        private String value = "";
        private int cursor;
        private int selectionAnchor = -1;
        private int displayStart;
        private boolean focused;
        private boolean visible;
        private int x;
        private int y;
        private int width;
        private int height;
        private int textInset = 8;

        private TextFieldState(int maxLength) {
            this.maxLength = maxLength;
        }

        private void setBounds(int x, int y, int width, int height, int textInset) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.textInset = textInset;
        }

        private void setValue(String value) {
            this.value = sanitize(value, maxLength);
            this.cursor = this.value.length();
            this.displayStart = 0;
            clearSelection();
        }

        private void insertText(String input) {
            if (input == null || input.isEmpty()) {
                return;
            }
            String sanitized = sanitize(input, maxLength);
            if (sanitized.isEmpty()) {
                return;
            }
            int insertAt = deleteSelectionInternal();
            String next = value.substring(0, insertAt) + sanitized + value.substring(insertAt);
            value = sanitize(next, maxLength);
            cursor = Math.min(value.length(), insertAt + sanitized.length());
            clearSelection();
            ensureCursorVisible(Minecraft.getInstance().font);
        }

        private void backspace() {
            if (deleteSelection()) {
                return;
            }
            if (cursor <= 0 || value.isEmpty()) {
                return;
            }
            value = value.substring(0, cursor - 1) + value.substring(cursor);
            cursor--;
            ensureCursorVisible(Minecraft.getInstance().font);
        }

        private void deleteForward() {
            if (deleteSelection()) {
                return;
            }
            if (cursor >= value.length()) {
                return;
            }
            value = value.substring(0, cursor) + value.substring(cursor + 1);
            ensureCursorVisible(Minecraft.getInstance().font);
        }

        private void moveCursor(int delta, boolean extendSelection) {
            int nextCursor = Math.max(0, Math.min(value.length(), cursor + delta));
            updateSelectionForMovement(nextCursor, extendSelection);
            ensureCursorVisible(Minecraft.getInstance().font);
        }

        private void moveCursorByWord(int direction, boolean extendSelection) {
            int nextCursor = direction < 0 ? previousWordBoundary(cursor) : nextWordBoundary(cursor);
            updateSelectionForMovement(nextCursor, extendSelection);
            ensureCursorVisible(Minecraft.getInstance().font);
        }

        private void moveToStart(boolean extendSelection) {
            updateSelectionForMovement(0, extendSelection);
            ensureCursorVisible(Minecraft.getInstance().font);
        }

        private void moveToEnd(boolean extendSelection) {
            updateSelectionForMovement(value.length(), extendSelection);
            ensureCursorVisible(Minecraft.getInstance().font);
        }

        private boolean contains(double mouseX, double mouseY) {
            return visible && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        private void click(Font font, double mouseX) {
            ensureCursorVisible(font);
            int contentWidth = contentWidth();
            int start = displayStart;
            int end = visibleEnd(font, contentWidth, start);
            int localX = (int) mouseX - (x + textInset);
            int bestCursor = start;
            int bestDistance = Integer.MAX_VALUE;
            for (int index = start; index <= end; index++) {
                int offset = font.width(value.substring(start, index));
                int distance = Math.abs(localX - offset);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestCursor = index;
                }
            }
            cursor = bestCursor;
            clearSelection();
            ensureCursorVisible(font);
        }

        private String visibleText(Font font, int contentWidth) {
            ensureCursorVisible(font);
            int start = displayStart;
            int end = visibleEnd(font, contentWidth, start);
            return value.substring(start, end);
        }

        private int cursorOffset(Font font, int contentWidth) {
            ensureCursorVisible(font);
            int start = displayStart;
            return font.width(value.substring(start, cursor));
        }

        private boolean hasSelection() {
            return selectionAnchor >= 0 && selectionAnchor != cursor;
        }

        private void selectAll() {
            selectionAnchor = 0;
            cursor = value.length();
        }

        private String copyText() {
            if (hasSelection()) {
                return value.substring(selectionStart(), selectionEnd());
            }
            return value;
        }

        private boolean deleteSelection() {
            if (!hasSelection()) {
                return false;
            }
            deleteSelectionInternal();
            clearSelection();
            ensureCursorVisible(Minecraft.getInstance().font);
            return true;
        }

        private int selectionStartOffset(Font font, int contentWidth) {
            ensureCursorVisible(font);
            int start = displayStart;
            int selectionStart = Math.max(start, selectionStart());
            return font.width(value.substring(start, selectionStart));
        }

        private int selectionEndOffset(Font font, int contentWidth) {
            ensureCursorVisible(font);
            int start = displayStart;
            int visibleEnd = visibleEnd(font, contentWidth, start);
            int selectionEnd = Math.min(visibleEnd, selectionEnd());
            return font.width(value.substring(start, selectionEnd));
        }

        private int visibleEnd(Font font, int contentWidth, int start) {
            int end = start;
            while (end < value.length() && font.width(value.substring(start, end + 1)) <= contentWidth) {
                end++;
            }
            return end;
        }

        private void updateSelectionForMovement(int nextCursor, boolean extendSelection) {
            if (extendSelection) {
                if (selectionAnchor < 0) {
                    selectionAnchor = cursor;
                }
            } else {
                clearSelection();
            }
            cursor = nextCursor;
            if (!extendSelection || selectionAnchor == cursor) {
                clearSelection();
            }
        }

        private int selectionStart() {
            return Math.min(selectionAnchor, cursor);
        }

        private int selectionEnd() {
            return Math.max(selectionAnchor, cursor);
        }

        private int deleteSelectionInternal() {
            if (!hasSelection()) {
                return cursor;
            }
            int start = selectionStart();
            int end = selectionEnd();
            value = value.substring(0, start) + value.substring(end);
            cursor = start;
            displayStart = Math.min(displayStart, cursor);
            return start;
        }

        private void clearSelection() {
            selectionAnchor = -1;
        }

        private void deleteWordBackward() {
            if (deleteSelection()) {
                return;
            }
            int target = previousWordBoundary(cursor);
            if (target == cursor) {
                return;
            }
            value = value.substring(0, target) + value.substring(cursor);
            cursor = target;
            ensureCursorVisible(Minecraft.getInstance().font);
        }

        private void deleteWordForward() {
            if (deleteSelection()) {
                return;
            }
            int target = nextWordBoundary(cursor);
            if (target == cursor) {
                return;
            }
            value = value.substring(0, cursor) + value.substring(target);
            ensureCursorVisible(Minecraft.getInstance().font);
        }

        private int previousWordBoundary(int from) {
            int index = Math.max(0, Math.min(from, value.length()));
            while (index > 0 && Character.isWhitespace(value.charAt(index - 1))) {
                index--;
            }
            while (index > 0 && !Character.isWhitespace(value.charAt(index - 1))) {
                index--;
            }
            return index;
        }

        private int nextWordBoundary(int from) {
            int index = Math.max(0, Math.min(from, value.length()));
            while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
                index++;
            }
            while (index < value.length() && !Character.isWhitespace(value.charAt(index))) {
                index++;
            }
            return index;
        }

        private int contentWidth() {
            return Math.max(8, width - (textInset * 2));
        }

        private void ensureCursorVisible(Font font) {
            if (font == null) {
                return;
            }
            int contentWidth = contentWidth();
            cursor = Math.max(0, Math.min(cursor, value.length()));
            displayStart = Math.max(0, Math.min(displayStart, value.length()));
            if (displayStart > cursor) {
                displayStart = cursor;
            }

            while (displayStart < cursor && font.width(value.substring(displayStart, cursor)) > contentWidth) {
                displayStart++;
            }

            if (cursor < value.length()) {
                while (displayStart > 0 && font.width(value.substring(displayStart - 1, cursor)) <= contentWidth) {
                    displayStart--;
                }
            } else {
                while (displayStart > 0 && font.width(value.substring(displayStart - 1, cursor)) <= contentWidth) {
                    displayStart--;
                }
            }
        }

        private static String sanitize(String text, int maxLength) {
            String normalized = Objects.requireNonNullElse(text, "").replace('\r', ' ').replace('\n', ' ');
            return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
        }
    }

    private record EntryListRow(WheelEntry entry, String ownerWheelId, int depth, boolean expandable, boolean expanded) {
    }

    private record FocusTarget(EditorField field, Integer commandFieldIndex, Integer actionIndex) {
        private static FocusTarget forField(EditorField field) {
            return new FocusTarget(field, null, null);
        }

        private static FocusTarget forCommandField(int index) {
            return new FocusTarget(null, index, null);
        }

        private static FocusTarget forAction(int index) {
            return new FocusTarget(null, null, index);
        }
    }

    private record FocusBounds(int left, int top, int width, int height) {
        private int right() {
            return left + width;
        }

        private int bottom() {
            return top + height;
        }

        private float centerX() {
            return left + (width * 0.5F);
        }

        private float centerY() {
            return top + (height * 0.5F);
        }
    }

    private record TutorialStep(String title, String body, int x, int y, int width) {
    }

    private record IconPickerOption(String id, String searchText) {
    }
}
