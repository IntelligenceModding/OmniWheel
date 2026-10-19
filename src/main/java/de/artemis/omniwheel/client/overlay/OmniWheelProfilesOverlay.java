package de.artemis.omniwheel.client.overlay;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.client.render.EntryIconRenderer;
import de.artemis.omniwheel.client.render.GeometryRenderer;
import de.artemis.omniwheel.client.render.CustomItemIcons;
import de.artemis.omniwheel.client.render.CustomImageData;
import de.artemis.omniwheel.client.render.LiftedRowRenderer;
import de.artemis.omniwheel.client.navigation.ManagementNavigation;
import de.artemis.omniwheel.client.navigation.ManagementNavigation.Bounds;
import de.artemis.omniwheel.client.navigation.ManagementNavigation.Node;
import de.artemis.omniwheel.client.runtime.EntryShortcutMatcher;
import de.artemis.omniwheel.client.runtime.LiveDataValues;
import de.artemis.omniwheel.client.runtime.LiveDataSnapshot;
import de.artemis.omniwheel.client.runtime.OmniWheelClientRuntime;
import de.artemis.omniwheel.client.tutorial.TutorialType;
import de.artemis.omniwheel.client.tutorial.TutorialGuide;
import de.artemis.omniwheel.client.tutorial.TutorialSpotlight;
import de.artemis.omniwheel.common.action.ChatAction;
import de.artemis.omniwheel.common.action.KeybindAction;
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
import de.artemis.omniwheel.common.wheel.WheelEntryMover;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
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
import de.artemis.omniwheel.client.render.ExpansionAnimation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.function.IntConsumer;

public final class OmniWheelProfilesOverlay {
    private final CustomColorPicker customColorPicker = new CustomColorPicker();
    private static final String COMMAND_VALUE_SEPARATOR = " ;; ";
    private static final int PANEL = 0xF0181F28;
    private static final int PANEL_EDGE = 0xFF2C4B63;
    private static final int PANEL_EDGE_ACTIVE = 0xFF4F7793;
    private static final int ROW = 0xE6232C36;
    private static final int ROW_SELECTED = 0xF03B5369;
    private static final int DRAG_SURFACE = 0xFF202B36;
    private static final int DRAG_EDGE = 0xFF91A7B8;
    private static final int DRAG_FIELD_EDGE = 0xFF60798E;
    private static final int ROW_DISABLED = 0xB01A222B;
    private static final int TEXT = 0xFFF1F4F8;
    private static final int TEXT_SECONDARY = 0xFF95A4B2;
    private static final int WARNING = 0xFFF0B36A;
    private static final int ACTION = 0xFF8BDAF8;
    private static final int ACTION_DISABLED = 0xFF53606C;
    private static final int FIELD = 0xFF111820;
    private static final int FIELD_EDGE = 0xFF314252;
    private static final int FIELD_FOCUSED = 0xFF8BDAF8;
    private static final int SELECTOR_ROW_HEIGHT = 34;
    private static final int SELECTOR_PADDING = 3;
    // The outer padding includes the list's one-pixel border.
    private static final int SELECTOR_ROW_STEP = SELECTOR_ROW_HEIGHT + SELECTOR_PADDING - 1;
    private static final int PROFILE_ROW_HEIGHT = 28;
    private static final int PROFILE_ROW_STEP = 36;
    private static final int LIST_SCROLL_STEP = 36;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 6;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 18;
    private static final int ICON_PICKER_CELL_SIZE = 22;
    private static final int ICON_PICKER_CELL_GAP = 5;
    private static final int ICON_PICKER_CONTENT_PADDING = 8;
    private static final int MANAGER_TUTORIAL_STEP_COUNT = TutorialGuide.MANAGER.size();
    private static final float PROFILE_DRAG_THRESHOLD = 6.0F;
    private static final int DRAG_SCROLL_EDGE = 28;
    private static final long SUBMENU_HOVER_DELAY_NANOS = 500_000_000L;
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
    private final RadialWheelPreview radialPreview;
    private boolean previewOpen;
    private final List<ClickAction> actions = new ArrayList<>();
    private final List<TooltipRegion> tooltipRegions = new ArrayList<>();
    private final Map<EditorField, TextFieldState> textFields = createTextFields();
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Map<Integer, Long> nextKeyRepeatAt = new HashMap<>();
    private final Set<String> expandedWheelIds = new HashSet<>();
    private NavigationList activeList = NavigationList.PROFILES;
    private EditBox commandInput;
    private int commandInputOwner = -1;
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
    private boolean suppressActivationSpace;
    private double lastFreeRawMouseX;
    private double lastFreeRawMouseY;
    private EditorField focusedField;
    private int focusedCommandFieldIndex = -1;
    private int focusedActionIndex = -1;
    private String focusedActionKey;
    private boolean keyboardButtonFocus;
    private float focusPointerX = Float.NaN, focusPointerY = Float.NaN;
    private int tutorialKeyboardButton = 1;
    private boolean tutorialKeyboardFocus;
    private float tutorialFocusMouseX = Float.NaN, tutorialFocusMouseY = Float.NaN;
    private final Map<String, Integer> navigationContentHeights = new HashMap<>();
    private int entriesScrollOffset;
    private int profilesScrollOffset;
    private int wheelsScrollOffset;
    private int iconPickerScrollOffset;
    private int functionListScrollOffset;
    private static final int KEYBIND_CATEGORY_HEIGHT = 24;
    private final Set<String> collapsedKeybindCategories = new HashSet<>(), searchCollapsedKeybindCategories = new HashSet<>();
    private final Set<String> knownKeybindCategories = new HashSet<>();
    private String keybindCategorySelection;
    private final Map<String, ExpansionAnimation> keybindCategoryExpansions = new HashMap<>();
    private String lastKeybindCategorySearch = "";
    private record KeybindGroupLayout(KeybindCategories.Group group, int top, int bodyHeight, float expansion) {}
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
    private int draggedCommandIndex = -1;
    private int draggedCommandTargetIndex;
    private boolean draggedCommandActive, draggedCommandDropValid;
    private float commandDragStartX, commandDragStartY, commandGrabX, commandGrabY;
    private float lastCommandDropX = Float.NaN, lastCommandDropY = Float.NaN;
    private int lastCommandDropScroll = -1, lastCommandDropTop = -1;
    private final Map<String, Float> commandDragRowY = new HashMap<>();
    private int commandListScrollbarX;
    private int commandListScrollbarY;
    private int commandListScrollbarHeight;
    private int commandListScrollbarThumbY;
    private int commandListScrollbarThumbHeight;
    private int managerTutorialStepIndex = -1;
    private int preparedTutorialStep = -1;
    private TutorialSelection tutorialSelection;
    private int tutorialBackButtonX, tutorialBackButtonY, tutorialBackButtonWidth, tutorialBackButtonHeight;
    private record TutorialSelection(String profile, String wheel, String entry, Set<String> expanded, boolean preview,
                                     int profilesScroll, int entriesScroll) {}
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
    private int draggedProfileTargetIndex = -1;
    private float draggedProfileMouseStartX;
    private float draggedProfileMouseStartY;
    private boolean draggedProfileActive;
    private boolean draggedProfileDropValid;
    private float profileGrabX, profileGrabY;
    private float entryGrabX, entryGrabY;
    private float lastEntryDropX = Float.NaN, lastEntryDropY = Float.NaN;
    private float lastProfileDropX = Float.NaN, lastProfileDropY = Float.NaN;
    private int lastEntryDropScroll = -1, lastProfileDropScroll = -1;
    private int lastEntryDropTop = -1, lastProfileDropTop = -1;
    private final Map<String, Float> entryDragRowY = new HashMap<>();
    private final Map<String, Float> profileDragRowY = new HashMap<>();
    private final de.artemis.omniwheel.client.render.ListRowAnimation<WheelProfile> profileRowsAnimation = new de.artemis.omniwheel.client.render.ListRowAnimation<>();
    private final de.artemis.omniwheel.client.render.ListRowAnimation<EntryListRow> entryRowsAnimation = new de.artemis.omniwheel.client.render.ListRowAnimation<>();
    private final Map<String, Bounds> profileRowHits = new HashMap<>(), entryRowHits = new HashMap<>();
    private record ButtonHover(float amount, long frame) {}
    private final Map<Bounds, ButtonHover> buttonHovers = new HashMap<>();
    private long lastDragFrameNanos;
    private float dragMotionAlpha = 1;
    private String draggedWheelId;
    private String draggedWheelOwnerWheelId;
    private String draggedWheelSubmenuTargetId;
    private float draggedWheelMouseStartX;
    private float draggedWheelMouseStartY;
    private boolean draggedWheelActive;
    private EntryDropTarget entryDropTarget;
    private String dragHoveredSubmenuId;
    private long dragHoveredSubmenuSince;

    private String wheelTitleDraft = "";
    private String profileNameDraft = "";

    private String entryLabelDraft = "";
    private String entryDescriptionDraft = "";
    private String entryShortcutDraft = "";
    private String entryGlyphDraft = "";
    private String entryValueDraft = "";
    private String entryKeybindDraft = "key.inventory";
    private boolean entryPresetDraft;
    private String iconPickerSearchDraft = "";
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
    // Text-data selection shares the existing modal browser navigation and scrolling.
    private boolean dataPickerOpen;
    private int dataInsertCommandIndex = -1;
    private boolean dataInsertUsesCommandInput;
    private String dataPickerError = "";
    private static final int DATA_PICKER_ROW_STEP = 50;
    private boolean pickingCustomItem;
    private boolean importingCustomImage;
    private final List<ItemStack> guiIconSources = new ArrayList<>();
    private String customIconError = "";
    private List<IconPickerOption> iconPickerOptions = List.of();
    private final Set<String> collapsedIconCategories = new HashSet<>();
    private final Set<String> searchCollapsedIconCategories = new HashSet<>();
    private String lastIconCategorySearch = "";
    private List<IconCategory> iconCategories = List.of();
    private final Map<String, ExpansionAnimation> categoryExpansions = new HashMap<>();
    private final Map<String, Integer> categoryBodyHeights = new HashMap<>();
    private final Map<String, ExpansionAnimation> submenuExpansions = new HashMap<>();
    private String revealedEditorView;
    private long editorRevealStarted;
    private long uiAnimationFrame;
    private long iconPickerClosingAt;
    private double iconPickerClosingFrom = 1;
    private int editorRevealBottom = Integer.MAX_VALUE;
    private static final int ICON_CATEGORY_HEIGHT = 24;
    private long iconPickerResourceGeneration = -1;

    public OmniWheelProfilesOverlay(OmniWheelClientRuntime runtime) {
        this.runtime = runtime;
        this.radialPreview = new RadialWheelPreview(new RadialWheelPreview.Host() {
            public WheelProfile profile() { return selectedProfile(); }
            public void closePreview() {
                clearFocus();
                clearWheelDrag();
                previewOpen = false;
                activeList = NavigationList.PROFILES;
            }
            public void select(String wheelId, String entryId) {
                persistPendingEdits();
                clearFocus();
                selectedWheelId = wheelId;
                selectedEntryId = entryId;
                expandedWheelIds.add(wheelId);
                loadWheelDraft(selectedWheel());
                loadEntryDraft(selectedEntry());
                activeList = NavigationList.EDITOR;
            }
            public void save(WheelProfile profile, String wheelId, String entryId) {
                runtime.profileManager().saveProfile(profile);
                selectedWheelId = wheelId;
                selectedEntryId = entryId;
                syncSelection(true);
            }
            public void button(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height,
                               String label, boolean enabled, Runnable action, String id) {
                addButton(graphics, minecraft, x, y, width, label, height, enabled, action, id);
            }
            public void target(int x, int y, int width, int height, String id, Runnable action) {
                actions.add(new ClickAction(x, y, width, height, true, action, id, ""));
            }
            public boolean focused(String id) { return isNavigationActionFocused(id); }
            public boolean[] enabledEntries(String wheel, List<WheelEntry> entries) {
                return runtime.availabilityResolver().enabledEntries(selectedProfile(), wheel, entries,
                        new java.util.ArrayDeque<>(List.of(wheel)));
            }
            public void draggedEntry(GuiGraphics graphics, Minecraft minecraft, WheelEntry entry,
                                     String owner, int x, int y, int width) {
                LiftedRowRenderer.render(graphics, x, y, width, PROFILE_ROW_HEIGHT,
                        () -> drawDraggedEntryRow(graphics, minecraft,
                                new EntryListRow(entry, owner, 0, entry.action() instanceof OpenWheelAction, false),
                                width));
            }
        });
    }

    public void open() {
        customColorPicker.close();
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
        clearCommandDrag();
        activeList = NavigationList.PROFILES;
        textEntryActive = false;
        expandedWheelIds.clear();
        suppressGameplayInput(minecraft);
        ensureMouseReleased(minecraft);
    }

    public void close() {
        customColorPicker.close();
        restoreTutorialSelection();
        if (screenHosted) {
            closeForScreen();
            return;
        }

        if (!open && !mouseReleased) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        persistPendingEdits();
        open = false;
        actions.clear();
        tooltipRegions.clear();
        leftMouseDown = false;
        escapeDown = false;
        clearFocus();
        clearProfileDrag();
        clearWheelDrag();
        clearCommandDrag();
        finishCloseIconPicker();
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
        customColorPicker.close();
        tutorialKeyboardFocus = false;
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
        clearCommandDrag();
        finishCloseIconPicker();
        clearCommandInput();
        activeList = NavigationList.PROFILES;
        textEntryActive = false;
        expandedWheelIds.clear();
        if (!runtime.tutorialProgressStore().isCompleted(TutorialType.MANAGER)) {
            managerTutorialStepIndex = runtime.tutorialProgressStore().currentStep(TutorialType.MANAGER);
        }
    }

    public void closeForScreen() {
        customColorPicker.close();
        restoreTutorialSelection();
        persistPendingEdits();
        open = false;
        screenHosted = false;
        actions.clear();
        tooltipRegions.clear();
        clearFocus();
        clearProfileDrag();
        clearWheelDrag();
        clearCommandDrag();
        finishCloseIconPicker();
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
        customColorPicker.close();
        if (!open) {
            return;
        }
        persistPendingEdits();
        clearProfileDrag();
        clearWheelDrag();
        clearCommandDrag();
        finishCloseIconPicker();
        syncSelection(true);
    }

    public void tick() {
        if (!open) {
            return;
        }
        if (isManagerTutorialActive()) {
            if (!screenHosted) suppressGameplayInput(Minecraft.getInstance());
            return;
        }

        if (screenHosted) {
            syncSelection(false);
            Minecraft minecraft = Minecraft.getInstance();
            float mouseX = currentMouseX(minecraft), mouseY = currentMouseY(minecraft);
            // Handled screen clicks never update MouseHandler's gameplay button state.
            // Track the screen's own press/release events until the drag is released.
            boolean pressed = leftMouseDown;
            autoScrollDrags(mouseX, mouseY, pressed);
            updateProfileDrag(mouseX, mouseY, pressed);
            updateWheelDrag(mouseX, mouseY, pressed);
            updateCommandDrag(mouseX, mouseY, pressed);
            if (previewOpen) radialPreview.update(mouseX, mouseY, pressed);
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
            if (!keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0)) close();
            escapeDown = true;
            return;
        }
        autoScrollDrags(currentMouseX(minecraft), currentMouseY(minecraft), leftPressed);
        updateProfileDrag(currentMouseX(minecraft), currentMouseY(minecraft), leftPressed);
        updateWheelDrag(currentMouseX(minecraft), currentMouseY(minecraft), leftPressed);
        updateCommandDrag(currentMouseX(minecraft), currentMouseY(minecraft), leftPressed);
        if (previewOpen) radialPreview.update(currentMouseX(minecraft), currentMouseY(minecraft), leftPressed);
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

        if (iconPickerClosingAt != 0 && System.nanoTime() - iconPickerClosingAt >= 180_000_000L) finishCloseIconPicker();
        Minecraft minecraft = Minecraft.getInstance();
        prepareManagerTutorialStep();
        float pointerX = currentMouseX(minecraft), pointerY = currentMouseY(minecraft);
        if (!Float.isNaN(focusPointerX) && (Math.abs(pointerX - focusPointerX) > .5F || Math.abs(pointerY - focusPointerY) > .5F)) keyboardButtonFocus = false;
        focusPointerX = pointerX; focusPointerY = pointerY;
        syncSelection(false);
        actions.clear();
        tooltipRegions.clear();

        long frameTime = System.nanoTime();
        uiAnimationFrame = frameTime;
        // Rendering registers the current view; controls from a closed view cannot retain focus.
        for (TextFieldState field : textFields.values()) field.visible = false;
        for (TextFieldState field : commandValueFields) field.visible = false;
        dragMotionAlpha = lastDragFrameNanos == 0 ? 1 : (float) (1 - Math.exp(-18 * Math.min(0.1, (frameTime - lastDragFrameNanos) / 1_000_000_000.0)));
        lastDragFrameNanos = frameTime;
        int width = uiWidth(minecraft);
        int height = uiHeight(minecraft);
        float renderScale = (float) uiScale(minecraft);
        graphics.pose().pushMatrix();
        graphics.pose().scale(renderScale, renderScale);

        int leftX = 18;
        int topY = 18;
        int panelHeight = height - 36;
        int sidePanelWidth = 176;
        int profileWidth = sidePanelWidth;
        int wheelWidth = sidePanelWidth;
        int wheelX = leftX + profileWidth + 12;
        int editorX = wheelX + wheelWidth + 12;
        boolean tutorialActive = isManagerTutorialActive();
        boolean previewVisible = previewOpen && !iconPickerOpen;
        if (previewVisible) editorX = wheelX;
        int editorWidth = width - editorX - 18;
        profilesPanelX = leftX;
        profilesPanelY = topY;
        profilesPanelWidth = profileWidth;
        profilesPanelHeight = panelHeight;
        wheelsPanelX = wheelX;
        wheelsPanelY = topY;
        wheelsPanelWidth = previewVisible ? 0 : wheelWidth;
        wheelsPanelHeight = panelHeight;
        editorPanelX = editorX;
        editorPanelY = topY;
        editorPanelWidth = editorWidth;
        editorPanelHeight = panelHeight;

        // Draw the complete normal layout; the tutorial spotlight dims it afterward.
        boolean showProfilesPanel = true;
        boolean showEntriesPanel = !previewVisible;
        boolean showEditorPanel = true;

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
            String editorTitle = dataPickerOpen ? "Insert Data" : text(iconPickerOpen ? "omniwheel.manager.icon_browser"
                    : previewOpen ? "omniwheel.manager.preview.open" : "omniwheel.manager.editor");
            if (previewVisible && selectedProfile() != null) {
                editorTitle += " - " + displayText(selectedProfile().displayName());
                if (!selectedProfile().id().equals(runtime.activeProfile().id())) {
                    editorTitle += " (" + text("omniwheel.manager.preview.profile_not_active") + ")";
                }
            }
            graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, editorTitle, editorWidth - 88),
                    editorX + 12, topY + 10, TEXT, false);
            drawEditorPanel(graphics, minecraft, editorX + 12, topY + 30, editorWidth - 24, panelHeight - 40);
        }
        if (!tutorialActive && !iconPickerOpen && textEntryActive && focusedCommandFieldIndex >= 0 && commandSuggestions != null
                && !LiveDataValues.contains(commandInput.getValue())) {
            boolean renderWrappedUsage = shouldRenderWrappedCommandUsage();
            alignCommandUsageToField();
            graphics.nextStratum();
            graphics.pose().pushMatrix();
            graphics.pose().translate(0.0F, commandSuggestionsYOffset);
            if (!renderWrappedUsage) {
                commandSuggestions.render(
                        graphics,
                        (int) currentMouseX(minecraft),
                        (int) currentMouseY(minecraft) - commandSuggestionsYOffset
                );
            }
            graphics.pose().popMatrix();
            if (renderWrappedUsage) {
                renderWrappedCommandUsage(graphics, minecraft);
            }
        }
        if (!customColorPicker.isOpen() && !isManagerTutorialActive() && !draggedProfileActive && !draggedWheelActive && !draggedCommandActive) {
            drawEditorButtonTooltip(graphics, minecraft);
        }
        if (!iconPickerOpen && !previewOpen) addButton(graphics, minecraft, editorPanelX + editorPanelWidth - 62, editorPanelY + 5,
                52, "Help", 20, !isManagerTutorialActive(), this::startManagerTutorial, "tutorial-help");
        restoreActionFocus();
        drawDragPreview(graphics, minecraft);
        if (customColorPicker.isOpen()) customColorPicker.render(graphics, minecraft, width, height,
                isManagerTutorialActive() ? -10000 : pointerX, isManagerTutorialActive() ? -10000 : pointerY, !isManagerTutorialActive());
        drawManagerTutorial(graphics, minecraft);
        graphics.pose().popMatrix();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mouseClicked(mouseX, mouseY, button, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean doubleClick) {
        if (open && customColorPicker.isOpen() && !isManagerTutorialActive()) return customColorPicker.mouseClicked(toUiX(mouseX), toUiY(mouseY), button);
        keyboardButtonFocus = false;
        if (open && iconPickerClosingAt != 0) return true;
        if (open && isManagerTutorialActive() && button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        if (open && !isManagerTutorialActive() && textEntryActive && focusedField == EditorField.ENTRY_SHORTCUT
                && (button != GLFW.GLFW_MOUSE_BUTTON_LEFT
                || textFields.get(EditorField.ENTRY_SHORTCUT).contains(toUiX(mouseX), toUiY(mouseY)))) {
            String shortcut = EntryShortcutMatcher.captureMouseShortcutText(Minecraft.getInstance().getWindow(), button);
            if (shortcut != null) {
                textFields.get(EditorField.ENTRY_SHORTCUT).setValue(shortcut);
                entryShortcutDraft = shortcut;
                autosaveCurrentEntryDraft();
            }
            return true;
        }
        if (!open || button != 0) {
            return false;
        }
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        if (handleManagerTutorialClick((float) mouseX, (float) mouseY)) {
            leftMouseDown = true;
            return true;
        }
        if (mouseX >= editorPanelX && mouseX < editorPanelX + editorPanelWidth && mouseY >= editorRevealBottom) return true;
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
        boolean handled = handleMouseClick((float) mouseX, (float) mouseY, doubleClick);
        if (handled) {
            leftMouseDown = true;
        }
        return handled;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (open && customColorPicker.isOpen() && !isManagerTutorialActive()) { leftMouseDown = false; return customColorPicker.mouseReleased(); }
        if (open && iconPickerClosingAt != 0) { leftMouseDown = false; return true; }
        if (open && isManagerTutorialActive() && button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        if (!open || button != 0) {
            return false;
        }
        if (isManagerTutorialActive()) {
            leftMouseDown = false;
            return true;
        }
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        if (previewOpen && radialPreview.release((float) mouseX, (float) mouseY)) {
            leftMouseDown = false;
            return true;
        }
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
        if (draggedCommandIndex >= 0) {
            updateCommandDrag((float) mouseX, (float) mouseY, true);
            finishCommandDrag();
            leftMouseDown = false;
            return true;
        }
        if (draggedProfileId != null) {
            updateProfileDrag((float) mouseX, (float) mouseY, true);
            finishProfileDrag();
            leftMouseDown = false;
            return true;
        }
        if (draggedWheelId != null) {
            updateWheelDrag((float) mouseX, (float) mouseY, true);
            finishWheelDrag();
            leftMouseDown = false;
            return true;
        }
        leftMouseDown = false;
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (open && customColorPicker.isOpen() && !isManagerTutorialActive()) return customColorPicker.mouseDragged(toUiX(mouseX), toUiY(mouseY));
        if (open && iconPickerClosingAt != 0) return true;
        if (open && isManagerTutorialActive() && button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        if (!open || button != 0) {
            return false;
        }
        if (isManagerTutorialActive()) {
            leftMouseDown = true;
            return true;
        }
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        if (previewOpen && radialPreview.hasDrag()) {
            radialPreview.update((float) mouseX, (float) mouseY, true);
            leftMouseDown = true;
            return true;
        }
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
        if (draggedCommandIndex >= 0) {
            updateCommandDrag((float) mouseX, (float) mouseY, true);
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
        return handleMouseClick(mouseX, mouseY, false);
    }

    private boolean handleMouseClick(float mouseX, float mouseY, boolean doubleClick) {
        if (handleManagerTutorialClick(mouseX, mouseY)) return true;
        if (customColorPicker.isOpen()) return customColorPicker.mouseClicked(mouseX, mouseY, 0);
        if (iconPickerOpen) {
            if (tryStartIconPickerScrollbarDrag(mouseX, mouseY)) {
                return true;
            }
            handleIconPickerClick(mouseX, mouseY, doubleClick);
            return true;
        }
        boolean insideMainPanel = updateActiveListFromMouse(mouseX, mouseY);
        if (previewOpen && radialPreview.press(mouseX, mouseY, doubleClick)) return true;
        if (tryStartProfilesScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        if (!previewOpen && tryStartWheelsScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        if (!previewOpen && tryStartFunctionListScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        if (!previewOpen && tryStartCommandListScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        if (tryStartProfileDrag(mouseX, mouseY)) {
            return true;
        }
        if (!previewOpen && tryStartWheelDrag(mouseX, mouseY, doubleClick)) {
            return true;
        }
        if (!previewOpen && tryStartCommandDrag(mouseX, mouseY)) return true;
        if (!previewOpen && clickFocusedTextField(mouseX, mouseY)) {
            return true;
        }

        clearFocus();
        for (ClickAction action : actions) {
            if (action.enabled && action.contains(mouseX, mouseY) && isActionMouseAccessible(action, mouseX, mouseY)) {
                focusAction(actions.indexOf(action));
                if (action.navigationId().startsWith("keybind-category:") && !doubleClick && mouseX >= action.x() + 24) return true;
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
        if (previewOpen && contains(mouseX, mouseY, wheelX, topY, width - wheelX - 18, panelHeight)) {
            activeList = NavigationList.EDITOR;
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

    private record NavigationControl(Node node, FocusTarget target, int rowIndex) {}

    private String actionGroup(ClickAction action) {
        if (!action.navigationGroup().isEmpty()) return action.navigationGroup();
        if (entryActionTypeDraft == DraftActionType.COMMAND
                && ("button:" + text("omniwheel.manager.add_command")).equals(action.navigationId())) return "commands";
        return "";
    }

    private String actionNavigationId(ClickAction action) {
        NavigationList panel = navigationListForAction(action);
        String identity = action.navigationId() != null ? action.navigationId() : "control:" + action.x + ":" + action.y;
        return panel + ":" + identity;
    }

    private Bounds navigationViewport(String group) {
        return switch (group) {
            case "profiles" -> new Bounds(profilesAreaX, profilesAreaY, profilesListWidth, profilesAreaHeight);
            case "entries" -> new Bounds(wheelsAreaX, wheelsAreaY, wheelsListWidth, wheelsAreaHeight);
            case "commands" -> new Bounds(commandListX, commandListY, commandListWidth, commandListHeight);
            case "functions" -> new Bounds(functionListX, functionListY, functionListWidth, functionListHeight);
            case "icons" -> new Bounds(iconPickerGridX, iconPickerGridY, iconPickerGridWidth, iconPickerGridHeight);
            default -> null;
        };
    }

    private boolean isActionMouseAccessible(ClickAction action, double x, double y) {
        Bounds viewport = navigationViewport(actionGroup(action));
        return viewport == null || contains((float) x, (float) y, viewport.x(), viewport.y(), viewport.width(), viewport.height());
    }

    private Node navigationNode(String id, NavigationList panel, String group, Bounds bounds) {
        Bounds viewport = navigationViewport(group);
        boolean visible = viewport == null || bounds.bottom() > viewport.y() && bounds.y() < viewport.bottom();
        // A scroll container occupies one place in reading order, regardless of its scroll offset.
        double orderY = bounds.y();
        if (viewport != null) {
            int contentHeight = navigationContentHeights.computeIfAbsent(group, key -> switch (key) {
                case "profiles" -> listContentHeight(runtime.profileManager().getProfiles().size());
                case "entries" -> listContentHeight(visibleEntryRows().size());
                case "commands" -> commandListContentHeight();
                case "functions" -> maxFunctionListScrollOffset() + functionListHeight;
                case "icons" -> maxIconPickerScrollOffset() + iconPickerGridHeight;
                default -> viewport.height();
            });
            double scale = Math.min(1.0, viewport.height() / (double) Math.max(1, contentHeight + 1));
            orderY = viewport.y() + (bounds.y() - viewport.y() + navigationScroll(group)) * scale;
        }
        double order = orderY * 1_000_000.0 + bounds.x();
        return new Node(id, panel.ordinal(), group, bounds, visible, order);
    }

    private int navigationScroll(String group) {
        return switch (group) {
            case "profiles" -> profilesScrollOffset;
            case "entries" -> wheelsScrollOffset;
            case "commands" -> commandListScrollOffset;
            case "functions" -> functionListScrollOffset;
            case "icons" -> iconPickerScrollOffset;
            default -> 0;
        };
    }

    private Bounds navigationActionBounds(ClickAction action) {
        int y = action.y;
        if (action.scrollOffset() >= 0) y += action.scrollOffset() - navigationScroll(actionGroup(action));
        return new Bounds(action.x, y, action.width, action.height);
    }

    private List<NavigationControl> navigationControls() {
        navigationContentHeights.clear();
        List<NavigationControl> controls = new ArrayList<>();
        if (!iconPickerOpen) {
            List<WheelProfile> profiles = runtime.profileManager().getProfiles();
            for (int index = 0; index < profiles.size(); index++) {
                if (profiles.get(index).id().equals(renamingProfileId)) continue;
                controls.add(new NavigationControl(navigationNode("profile:" + profiles.get(index).id(), NavigationList.PROFILES,
                        "profiles", new Bounds(profilesAreaX, profileRowY(index), profilesListWidth, PROFILE_ROW_HEIGHT)), null, index));
            }
            List<EntryListRow> rows = previewOpen ? List.of() : visibleEntryRows();
            for (int index = 0; index < rows.size(); index++) {
                EntryListRow row = rows.get(index);
                controls.add(new NavigationControl(navigationNode("entry:" + row.ownerWheelId() + ":" + row.entry().id(), NavigationList.WHEELS,
                        "entries", new Bounds(wheelsAreaX, wheelRowY(index), wheelsListWidth, PROFILE_ROW_HEIGHT)), null, index));
            }
        }
        for (var item : textFields.entrySet()) {
            TextFieldState field = item.getValue();
            if (!field.visible || iconPickerOpen && item.getKey() != EditorField.ICON_PICKER_SEARCH) continue;
            EditorField id = item.getKey();
            controls.add(new NavigationControl(navigationNode("field:" + id, navigationListForField(id), "",
                    new Bounds(field.x, field.y, field.width, field.height)), FocusTarget.forField(id), -1));
        }
        if (!iconPickerOpen && !previewOpen && entryActionTypeDraft == DraftActionType.COMMAND && selectedEntry() != null) {
            int grip = commandValueFields.size() > 1 ? 18 : 0;
            int fieldWidth = Math.max(96, commandListWidth - grip - (commandValueFields.size() > 1 ? 30 : 0));
            for (int index = 0; index < commandValueFields.size(); index++) {
                controls.add(new NavigationControl(navigationNode("command:" + index, NavigationList.EDITOR, "commands",
                        new Bounds(commandListX + grip, commandRowY(index) + 12, fieldWidth, 22)), FocusTarget.forCommandField(index), -1));
            }
        }
        for (int index = 0; index < actions.size(); index++) {
            ClickAction action = actions.get(index);
            if (!action.enabled || iconPickerOpen && action.x < editorPanelX) continue;
            controls.add(new NavigationControl(navigationNode(actionNavigationId(action), navigationListForAction(action), actionGroup(action),
                    navigationActionBounds(action)), FocusTarget.forAction(index), -1));
        }
        return controls;
    }

    private NavigationControl currentNavigationControl(List<NavigationControl> controls) {
        if (focusedActionKey != null) {
            for (NavigationControl control : controls) {
                if (focusedActionKey.equals(control.node().id())) return control;
            }
        }
        FocusTarget target = currentFocusTarget();
        if (target != null) {
            for (NavigationControl control : controls) {
                if (focusedActionKey != null && target.actionIndex() != null) {
                    if (control.node().id().equals(focusedActionKey)) return control;
                } else if (target.equals(control.target())) return control;
            }
        }
        String selected = activeList == NavigationList.PROFILES ? "profile:" + selectedProfileId
                : activeList == NavigationList.WHEELS ? "entry:" + selectedWheelId + ":" + selectedEntryId : "";
        return controls.stream().filter(control -> control.node().id().equals(selected)).findFirst().orElse(null);
    }

    private void restoreActionFocus() {
        if (focusedActionKey == null) return;
        focusedActionIndex = -1;
        for (int index = 0; index < actions.size(); index++) {
            ClickAction action = actions.get(index);
            if (action.enabled && focusedActionKey.equals(actionNavigationId(action))) {
                focusedActionIndex = index;
                return;
            }
        }
        focusedActionKey = null;
    }

    private void focusNavigationControl(NavigationControl control) {
        if (control == null) return;
        if (control.target() != null) {
            focusTarget(control.target());
        } else if (control.node().panel() == NavigationList.PROFILES.ordinal()) {
            selectProfileByIndex(control.rowIndex());
        } else {
            selectEntryRowByIndex(control.rowIndex());
        }
    }

    private void ensureNavigationVisible(ClickAction action) {
        String group = actionGroup(action);
        Bounds viewport = navigationViewport(group);
        if (viewport == null) return;
        int offset = navigationScroll(group);
        Bounds bounds = navigationActionBounds(action);
        int padding = group.equals("functions") ? SELECTOR_PADDING : group.equals("icons") ? ICON_PICKER_CONTENT_PADDING : 0;
        int next = offset;
        if (bounds.y() < viewport.y() + padding) next += bounds.y() - viewport.y() - padding;
        else if (bounds.bottom() > viewport.bottom() - padding) next += bounds.bottom() - viewport.bottom() + padding;
        switch (group) {
            case "commands" -> commandListScrollOffset = clamp(next, 0, maxCommandListScrollOffset());
            case "functions" -> functionListScrollOffset = clamp(next, 0, maxFunctionListScrollOffset());
            case "icons" -> iconPickerScrollOffset = clamp(next, 0, maxIconPickerScrollOffset());
            default -> { }
        }
    }

    private boolean navigateManagement(int keyCode, boolean reverseTab) {
        List<NavigationControl> controls = navigationControls();
        if (controls.isEmpty()) return true;
        NavigationControl current = currentNavigationControl(controls);
        List<Node> nodes = controls.stream().map(NavigationControl::node).toList();
        Node next;
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            next = ManagementNavigation.tab(nodes, current == null ? null : current.node(), reverseTab);
        } else if (current == null) {
            next = ManagementNavigation.readingOrder(nodes).stream().filter(node -> node.panel() == activeList.ordinal() && node.visible()).findFirst().orElse(null);
        } else if (isPagingNavigationKey(keyCode)) {
            String group = current.node().group();
            List<Node> scope = ManagementNavigation.readingOrder(nodes).stream()
                    .filter(node -> node.panel() == current.node().panel() && (group.isEmpty() || group.equals(node.group()))).toList();
            if (keyCode == GLFW.GLFW_KEY_HOME) next = scope.getFirst();
            else if (keyCode == GLFW.GLFW_KEY_END) next = scope.getLast();
            else {
                boolean down = keyCode == GLFW.GLFW_KEY_PAGE_DOWN;
                Bounds viewport = navigationViewport(group);
                int distance = viewport == null ? 160 : Math.max(1, viewport.height() - current.node().bounds().height());
                double targetY = current.node().bounds().centerY() + (down ? distance : -distance);
                next = scope.stream().filter(node -> down ? node.bounds().centerY() > current.node().bounds().centerY() : node.bounds().centerY() < current.node().bounds().centerY())
                        .min(Comparator.comparingDouble((Node node) -> Math.abs(node.bounds().centerY() - targetY))
                                .thenComparingDouble(node -> Math.abs(node.bounds().centerX() - current.node().bounds().centerX()))).orElse(current.node());
            }
        } else {
            if (navigateEntryTree(current, keyCode)) return true;
            ManagementNavigation.Direction direction = switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> ManagementNavigation.Direction.LEFT;
                case GLFW.GLFW_KEY_RIGHT -> ManagementNavigation.Direction.RIGHT;
                case GLFW.GLFW_KEY_UP -> ManagementNavigation.Direction.UP;
                default -> ManagementNavigation.Direction.DOWN;
            };
            next = ManagementNavigation.move(nodes, current.node(), direction);
            if (next.panel() != current.node().panel()
                    && (current.target() == null || current.target().field() != null || current.target().commandFieldIndex() != null)) {
                String anchor = next.panel() == NavigationList.PROFILES.ordinal() ? "profile:" + selectedProfileId
                        : next.panel() == NavigationList.WHEELS.ordinal() ? "entry:" + selectedWheelId + ":" + selectedEntryId : "field:ENTRY_LABEL";
                next = nodes.stream().filter(node -> node.id().equals(anchor)).findFirst().orElse(next);
            }
        }
        if (next != null) {
            String id = next.id();
            focusNavigationControl(controls.stream().filter(control -> control.node().id().equals(id)).findFirst().orElse(null));
        }
        return true;
    }

    private boolean navigateEntryTree(NavigationControl current, int keyCode) {
        if (current.target() != null || current.node().panel() != NavigationList.WHEELS.ordinal()) return false;
        List<EntryListRow> rows = visibleEntryRows();
        if (current.rowIndex() < 0 || current.rowIndex() >= rows.size()) return false;
        EntryListRow row = rows.get(current.rowIndex());
        if (keyCode == GLFW.GLFW_KEY_RIGHT && row.expandable()) {
            if (!row.expanded()) toggleExpandedSubmenu(row.ownerWheelId(), row.entry(), ((OpenWheelAction) row.entry().action()).wheelId());
            else if (current.rowIndex() + 1 < rows.size() && rows.get(current.rowIndex() + 1).depth() > row.depth()) selectEntryRowByIndex(current.rowIndex() + 1);
            else return false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (row.expandable() && row.expanded()) {
                toggleExpandedSubmenu(row.ownerWheelId(), row.entry(), ((OpenWheelAction) row.entry().action()).wheelId());
                return true;
            }
            for (int index = current.rowIndex() - 1; row.depth() > 0 && index >= 0; index--) {
                if (rows.get(index).depth() < row.depth()) return selectEntryRowByIndex(index);
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open) return false;
        if (customColorPicker.isOpen() && !isManagerTutorialActive()) return customColorPicker.keyPressed(keyCode, modifiers);
        if (iconPickerClosingAt != 0) return true;
        if (keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN
                || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT
                || keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END
                || keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) keyboardButtonFocus = true;
        if (isManagerTutorialActive()) {
            tutorialKeyboardFocus = true;
            if (keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
                tutorialKeyboardButton = Math.floorMod(tutorialKeyboardButton + ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 || keyCode == GLFW.GLFW_KEY_UP ? -1 : 1), 3);
            } else if (keyCode == GLFW.GLFW_KEY_LEFT) previousManagerTutorial();
            else if (keyCode == GLFW.GLFW_KEY_RIGHT) advanceManagerTutorial();
            else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                if (tutorialKeyboardButton == 0) previousManagerTutorial();
                else if (tutorialKeyboardButton == 1) advanceManagerTutorial();
                else completeManagerTutorial();
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) completeManagerTutorial();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (draggedProfileId != null || draggedWheelId != null || draggedCommandIndex >= 0 || radialPreview.hasDrag()) {
                clearProfileDrag();
                clearWheelDrag();
                clearCommandDrag();
                return true;
            }
            if (dataPickerOpen) {
                closeIconPicker();
                return true;
            }
            if (textEntryActive) {
                deactivateTextEntry();
                return true;
            }
            if (iconPickerOpen) {
                backFromIconPicker();
                return true;
            }
            if (currentFocusTarget() != null) {
                clearFocus();
                return true;
            }
            return false;
        }
        if (isReordering()) return true;
        if (keyCode == GLFW.GLFW_KEY_TAB && textEntryActive && focusedCommandFieldIndex >= 0 && ensureCommandInput()
                && commandSuggestions.keyPressed(keyEvent(keyCode, scanCode, modifiers))) return true;
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (textEntryActive) deactivateTextEntry();
            return navigateManagement(keyCode, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
        }
        if (textEntryActive && focusedField == EditorField.ENTRY_SHORTCUT) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                textFields.get(EditorField.ENTRY_SHORTCUT).setValue("");
                entryShortcutDraft = "";
            } else {
                String shortcut = EntryShortcutMatcher.captureShortcutText(Minecraft.getInstance().getWindow(), keyCode, scanCode, modifiers);
                if (shortcut != null) {
                    textFields.get(EditorField.ENTRY_SHORTCUT).setValue(shortcut);
                    entryShortcutDraft = shortcut;
                }
            }
            autosaveCurrentEntryDraft();
            return true;
        }
        if (textEntryActive && focusedCommandFieldIndex >= 0 && ensureCommandInput()) {
            KeyEvent event = keyEvent(keyCode, scanCode, modifiers);
            if (commandSuggestions.keyPressed(event) || commandInput.keyPressed(event)) return true;
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                deactivateTextEntry();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN || isPagingNavigationKey(keyCode)) {
                deactivateTextEntry();
                return navigateManagement(keyCode, false);
            }
            return false;
        }
        TextFieldState field = currentFocusedField();
        if (textEntryActive && field != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                deactivateTextEntry();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN
                    || keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
                deactivateTextEntry();
                return navigateManagement(keyCode, false);
            }
            boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
            boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
            if (control) {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_A -> field.selectAll();
                    case GLFW.GLFW_KEY_C -> Minecraft.getInstance().keyboardHandler.setClipboard(field.copyText());
                    case GLFW.GLFW_KEY_V -> { field.insertText(clipboardValue()); applyFocusedFieldDraft(); }
                    case GLFW.GLFW_KEY_X -> {
                        String copied = field.copyText();
                        if (!copied.isEmpty()) {
                            Minecraft.getInstance().keyboardHandler.setClipboard(copied);
                            field.deleteSelection();
                            applyFocusedFieldDraft();
                        }
                    }
                    case GLFW.GLFW_KEY_LEFT -> field.moveCursorByWord(-1, shift);
                    case GLFW.GLFW_KEY_RIGHT -> field.moveCursorByWord(1, shift);
                    case GLFW.GLFW_KEY_BACKSPACE -> { field.deleteWordBackward(); applyFocusedFieldDraft(); }
                    case GLFW.GLFW_KEY_DELETE -> { field.deleteWordForward(); applyFocusedFieldDraft(); }
                    default -> { return false; }
                }
                return true;
            }
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> field.moveCursor(-1, shift);
                case GLFW.GLFW_KEY_RIGHT -> field.moveCursor(1, shift);
                case GLFW.GLFW_KEY_HOME -> field.moveToStart(shift);
                case GLFW.GLFW_KEY_END -> field.moveToEnd(shift);
                case GLFW.GLFW_KEY_BACKSPACE -> { field.backspace(); applyFocusedFieldDraft(); }
                case GLFW.GLFW_KEY_DELETE -> { field.deleteForward(); applyFocusedFieldDraft(); }
                default -> { return false; }
            }
            return true;
        }
        restoreActionFocus();
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN)) {
            int delta = keyCode == GLFW.GLFW_KEY_UP ? -1 : 1;
            if (focusedCommandFieldIndex >= 0) {
                int source = focusedCommandFieldIndex;
                int target = clamp(source + delta, 0, commandValueDrafts.size() - 1);
                if (source != target) {
                    clearFocus();
                    commandValueDrafts.add(target, commandValueDrafts.remove(source));
                    syncCommandValueFields();
                    autosaveCurrentEntryDraft();
                    focusCommandField(target);
                }
                return true;
            }
            return handleMoveShortcut(keyCode);
        }
        if (isDirectionalNavigationKey(keyCode) || isPagingNavigationKey(keyCode)) return navigateManagement(keyCode, false);
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (field != null) {
                activateTextEntry();
                suppressActivationSpace = keyCode == GLFW.GLFW_KEY_SPACE;
                return true;
            }
            if (focusedActionIndex >= 0) return runFocusedAction();
            return activateCurrentSelection();
        }
        if ((keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) && clearHighlightedShortcutField()) return true;
        if (keyCode == GLFW.GLFW_KEY_DELETE && currentFocusTarget() == null && activeList != NavigationList.EDITOR) return handleDeleteShortcut();
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
        if (open && customColorPicker.isOpen() && !isManagerTutorialActive()) return customColorPicker.charTyped(codePoint);
        if (open && iconPickerClosingAt != 0) return true;
        if (!open) {
            return false;
        }
        if (suppressActivationSpace) {
            suppressActivationSpace = false;
            if (codePoint == ' ') return true;
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
        if (open && customColorPicker.isOpen()) return true;
        if (open && iconPickerClosingAt != 0) return true;
        if (!open || scrollY == 0.0D) {
            return false;
        }
        if (isManagerTutorialActive()) {
            return true;
        }
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        if (previewOpen && radialPreview.scroll((float) mouseX, (float) mouseY, scrollY)) return true;
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
                return true;
        }
        if ((entryActionTypeDraft == DraftActionType.FUNCTION || entryActionTypeDraft == DraftActionType.KEYBIND)
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
        int actionY = y + height - 144;
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
        if (draggedProfileActive) {
            updateProfileDrop(currentMouseX(minecraft), currentMouseY(minecraft));
        }

        enableUiScissor(graphics, x, y, x + profilesListWidth, y + profilesAreaHeight);
        int gap = draggedProfileActive ? profileGapIndex(profiles) : -1;
        if (draggedProfileActive) {
            drawDragPlaceholder(graphics, x, profileRowY(gap), profilesListWidth);
        }
        var animatedProfiles = profileRowsAnimation.update("profiles", profiles, WheelProfile::id, uiAnimationFrame, draggedProfileActive || isManagerTutorialActive());
        if (!draggedProfileActive) drawAnimatedList(graphics, animatedProfiles, x, y, profilesListWidth, profilesAreaHeight,
                profilesScrollOffset, profileRowHits, profileDragRowY,
                (profile, rowY) -> drawProfileRow(graphics, minecraft, profile, x, rowY, profilesListWidth, false));
        int slot = 0;
        for (WheelProfile profile : profiles) {
            if (!draggedProfileActive) break;
            if (draggedProfileActive && profile.id().equals(draggedProfileId)) {
                continue;
            }
            int visualSlot = slot + (draggedProfileActive && slot >= gap ? 1 : 0);
            int rowY = animateDragRow(profileDragRowY, profile.id(), profileRowY(visualSlot), draggedProfileActive);
            if (rowY + PROFILE_ROW_HEIGHT >= y && rowY <= y + profilesAreaHeight) {
                drawProfileRow(graphics, minecraft, profile, x, rowY, profilesListWidth, false);
            }
            slot++;
        }
        graphics.disableScissor();
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
        {
            addButton(graphics, minecraft, x, actionY + 120, width,
                    text(previewOpen ? "omniwheel.manager.preview.edit" : "omniwheel.manager.preview.open"),
                    24, selectedProfile() != null && !iconPickerOpen, () -> {
                        persistPendingEdits();
                        clearFocus();
                        clearWheelDrag();
                        clearCommandDrag();
                        previewOpen = !previewOpen;
                        activeList = NavigationList.PROFILES;
                    }, "preview-toggle");
        }
    }

    private void drawProfileRow(GuiGraphics graphics, Minecraft minecraft, WheelProfile profile, int x, int y, int width, boolean dragged) {
        boolean selected = profile.id().equals(selectedProfileId);
        boolean activeProfile = profile.id().equals(runtime.activeProfile().id());
        int rowColor = dragged ? DRAG_SURFACE : selected ? ROW_SELECTED : ROW;

        if (dragged) {
            graphics.fill(x + 3, y + 4, x + width + 3, y + PROFILE_ROW_HEIGHT + 4, 0x58101820);
            graphics.fill(x - 1, y - 1, x + width + 1, y + PROFILE_ROW_HEIGHT + 1, 0x90384E62);
        }

        graphics.fill(x, y, x + width, y + PROFILE_ROW_HEIGHT, rowColor);
        if (dragged || selected) {
            int edgeColor = dragged ? DRAG_EDGE : ACTION;
            graphics.fill(x, y, x + width, y + 1, edgeColor);
            graphics.fill(x, y + PROFILE_ROW_HEIGHT - 1, x + width, y + PROFILE_ROW_HEIGHT, edgeColor);
            graphics.fill(x, y, x + 1, y + PROFILE_ROW_HEIGHT, edgeColor);
            graphics.fill(x + width - 1, y, x + width, y + PROFILE_ROW_HEIGHT, edgeColor);
        }

        String stateLabel = text(activeProfile ? "omniwheel.manager.profile.active" : selected ? "omniwheel.manager.profile.selected" : "omniwheel.manager.profile.select");
        int stateColor = activeProfile || selected ? ACTION : TEXT_SECONDARY;
        int stateWidth = minecraft.font.width(stateLabel);
        int stateX = x + width - stateWidth - 22;
        int textWidth = Math.max(36, stateX - x - 18);
        boolean renaming = isRenamingProfile(profile.id()) && !dragged;

        if (renaming) {
            drawInlineProfileNameField(graphics, minecraft, x + 8, y + 4, textWidth, 14);
        } else {
            graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, displayText(profile.displayName()), textWidth), x + 10, y + 7, TEXT, false);
        }
        graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, profile.id(), textWidth), x + 10, y + 17, TEXT_SECONDARY, false);
        graphics.drawString(minecraft.font, stateLabel, stateX, y + 11, stateColor, false);
        drawDragHandle(graphics, x + width - 10, y + 8, "omniwheel.manager.drag.profile_hint");
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
        int rowColor = dragged ? DRAG_SURFACE : selected ? (active ? ROW_SELECTED : 0xC8323E4C) : active ? ROW : ROW_DISABLED;

        if (dragged) {
            graphics.fill(x + 3, y + 4, x + width + 3, y + PROFILE_ROW_HEIGHT + 4, 0x58101820);
            graphics.fill(x - 1, y - 1, x + width + 1, y + PROFILE_ROW_HEIGHT + 1, 0x90384E62);
        }

        graphics.fill(x, y, x + width, y + PROFILE_ROW_HEIGHT, rowColor);
        if (dragged || selected) {
            int accentColor = dragged ? DRAG_EDGE : active ? ACTION : ACTION_DISABLED;
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

    private int animateDragRow(Map<String, Float> positions, String id, int targetY, boolean moving) {
        float previous = positions.getOrDefault(id, (float) targetY);
        float y = moving ? previous + (targetY - previous) * dragMotionAlpha : targetY;
        positions.put(id, y);
        return Math.round(y);
    }

    private <T> void drawAnimatedList(GuiGraphics graphics,
            List<de.artemis.omniwheel.client.render.ListRowAnimation.Row<T>> rows,
            int x, int y, int width, int height, int scroll, Map<String, Bounds> hits,
            Map<String, Float> positions, java.util.function.BiConsumer<T, Integer> draw) {
        hits.clear();
        for (var row : rows) {
            int top = y + Math.round(row.offset()) - scroll;
            int visibleHeight = Math.round(PROFILE_ROW_HEIGHT * row.size());
            if (!row.removing()) {
                positions.put(row.id(), (float) top);
                hits.put(row.id(), new Bounds(x, top, width, visibleHeight));
            }
            if (visibleHeight <= 0 || top + visibleHeight < y || top >= y + height) continue;
            enableUiScissor(graphics, x, top, x + width, top + visibleHeight);
            int tooltipsBefore = tooltipRegions.size();
            draw.accept(row.value(), top);
            if (row.removing()) tooltipRegions.subList(tooltipsBefore, tooltipRegions.size()).clear();
            graphics.disableScissor();
        }
    }

    private void drawDragPlaceholder(GuiGraphics graphics, int x, int y, int width) {
        drawDragPlaceholder(graphics, x, y, width, PROFILE_ROW_HEIGHT);
    }

    private void drawDragPlaceholder(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x40101820);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, 0x30314252);
        graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, 0x30314252);
        graphics.fill(x + 2, y + 3, x + 3, y + height - 3, 0x30314252);
        graphics.fill(x + width - 3, y + 3, x + width - 2, y + height - 3, 0x30314252);
    }

    private int profileGapIndex(List<WheelProfile> profiles) {
        if (draggedProfileDropValid) {
            return clamp(draggedProfileTargetIndex, 0, profiles.size() - 1);
        }
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).id().equals(draggedProfileId)) {
                return index;
            }
        }
        return 0;
    }

    private int entryGapBoundary(List<EntryListRow> rows) {
        if (validEntryDrop()) {
            if (entryDropTarget.visibleIndex() < 0) {
                return rows.size();
            }
            int boundary = entryDropTarget.visibleIndex();
            if (entryDropTarget.inside()) {
                int depth = rows.get(boundary).depth();
                boundary++;
                while (boundary < rows.size() && rows.get(boundary).depth() > depth) {
                    boundary++;
                }
            }
            return boundary;
        }
        for (int index = 0; index < rows.size(); index++) {
            EntryListRow row = rows.get(index);
            if (row.ownerWheelId().equals(draggedWheelOwnerWheelId) && row.entry().id().equals(draggedWheelId)) {
                return index;
            }
        }
        return 0;
    }

    private int entryGapSlot(List<EntryListRow> rows) {
        int boundary = entryGapBoundary(rows);
        int slot = 0;
        for (int index = 0; index < Math.min(boundary, rows.size()); index++) {
            if (!shouldSkipDraggedWheelRow(rows.get(index))) {
                slot++;
            }
        }
        return slot;
    }

    private String dragRowKey(EntryListRow row) {
        return selectedProfileId + "/" + row.ownerWheelId() + "/" + row.entry().id();
    }

    private void drawDragHandle(GuiGraphics graphics, int x, int y, String hint) {
        for (int row = 0; row < 3; row++) {
            graphics.fill(x, y + row * 4, x + 2, y + row * 4 + 2, TEXT_SECONDARY);
            graphics.fill(x + 4, y + row * 4, x + 6, y + row * 4 + 2, TEXT_SECONDARY);
        }
        tooltipRegions.add(new TooltipRegion(x - 3, y - 3, 12, 18, text(hint)));
    }

    private void drawEntryDropTarget(GuiGraphics graphics, Minecraft minecraft) {
        if (!draggedWheelActive || entryDropTarget == null || entryDropTarget.visibleIndex() < 0) {
            return;
        }
        EntryDropTarget target = entryDropTarget;
        if (target.inside()) {
            List<EntryListRow> rows = visibleEntryRows();
            if (target.visibleIndex() >= rows.size()) return;
            int y = Math.round(entryDragRowY.getOrDefault(dragRowKey(rows.get(target.visibleIndex())), (float) wheelRowY(target.visibleIndex())));
            int color = validEntryDrop() ? ACTION : WARNING;
            graphics.fill(wheelsAreaX, y, wheelsAreaX + wheelsListWidth, y + PROFILE_ROW_HEIGHT, 0x403B5369);
            graphics.fill(wheelsAreaX, y, wheelsAreaX + wheelsListWidth, y + 2, color);
            graphics.fill(wheelsAreaX, y + PROFILE_ROW_HEIGHT - 2, wheelsAreaX + wheelsListWidth, y + PROFILE_ROW_HEIGHT, color);
            graphics.fill(wheelsAreaX, y, wheelsAreaX + 2, y + PROFILE_ROW_HEIGHT, color);
            graphics.fill(wheelsAreaX + wheelsListWidth - 2, y, wheelsAreaX + wheelsListWidth, y + PROFILE_ROW_HEIGHT, color);
        }
    }

    private void drawDragPreview(GuiGraphics graphics, Minecraft minecraft) {
        if (draggedCommandActive) {
            drawCommandDragPreview(graphics, minecraft);
            return;
        }
        if (!draggedProfileActive && !draggedWheelActive && !draggedCommandActive) {
            return;
        }
        int width = draggedProfileActive ? profilesListWidth : wheelsListWidth;
        float grabX = draggedProfileActive ? profileGrabX : entryGrabX;
        float grabY = draggedProfileActive ? profileGrabY : entryGrabY;
        int x = clamp(Math.round(currentMouseX(minecraft) - grabX + 3), 4, uiWidth(minecraft) - width - 10);
        int y = clamp(Math.round(currentMouseY(minecraft) - grabY - 4), 4, uiHeight(minecraft) - PROFILE_ROW_HEIGHT - 12);
        LiftedRowRenderer.render(graphics, x, y, width, PROFILE_ROW_HEIGHT, () -> {
            if (draggedProfileActive) {
                WheelProfile profile = draggedProfile();
                if (profile != null) {
                    drawProfileRow(graphics, minecraft, profile, 0, 0, width, true);
                }
            } else {
                EntryListRow row = draggedEntryRow();
                if (row != null) {
                    drawDraggedEntryRow(graphics, minecraft, row, width);
                }
            }
        });
        boolean valid = draggedProfileActive ? draggedProfileDropValid : validEntryDrop();
        if (!valid) {
            String reason = draggedWheelActive && entryDropTarget != null
                    ? text(!Objects.equals(draggedWheelOwnerWheelId, entryDropTarget.wheelId())
                            ? "omniwheel.manager.drag.preview_for_submenus" : "omniwheel.manager.drag.invalid_submenu")
                    : text("omniwheel.manager.drag.cancel");
            graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, reason, Math.max(width, 180)),
                    x + 3, y + PROFILE_ROW_HEIGHT + 12, WARNING, true);
        }
    }

    private void drawDraggedEntryRow(GuiGraphics graphics, Minecraft minecraft, EntryListRow row, int width) {
        if (row.expandable()) {
            graphics.fill(4, 5, width + 4, PROFILE_ROW_HEIGHT + 5, FIELD_EDGE);
            graphics.fill(2, 3, width + 2, PROFILE_ROW_HEIGHT + 3, ROW);
        }
        drawEntryListRow(graphics, minecraft, row, 0, 0, width, true);
    }

    private void drawEntryListRow(GuiGraphics graphics, Minecraft minecraft, EntryListRow row, int x, int y, int width, boolean dragged) {
        WheelEntry entry = row.entry();
        boolean selected = isEntryRowSelected(row.ownerWheelId(), entry.id());
        int rowColor = dragged ? DRAG_SURFACE : !entry.active() ? ROW_DISABLED : selected ? ROW_SELECTED : ROW;
        int indent = Math.min(row.depth() * 14, Math.max(0, width - 90));
        if (dragged) {
            graphics.fill(x + 3, y + 4, x + width + 3, y + PROFILE_ROW_HEIGHT + 4, 0x58101820);
            graphics.fill(x - 1, y - 1, x + width + 1, y + PROFILE_ROW_HEIGHT + 1, 0x90384E62);
        }
        graphics.fill(x, y, x + width, y + PROFILE_ROW_HEIGHT, rowColor);
        if (dragged || selected) {
            int accentColor = dragged ? DRAG_EDGE : entry.active() ? ACTION : ACTION_DISABLED;
            graphics.fill(x, y, x + width, y + 1, accentColor);
            graphics.fill(x, y + PROFILE_ROW_HEIGHT - 1, x + width, y + PROFILE_ROW_HEIGHT, accentColor);
            graphics.fill(x, y, x + 1, y + PROFILE_ROW_HEIGHT, accentColor);
            graphics.fill(x + width - 1, y, x + width, y + PROFILE_ROW_HEIGHT, accentColor);
        }

        int arrowOffset = 0;
        if (row.expandable()) {
            String key = dragRowKey(row);
            ExpansionAnimation expansion = submenuExpansions.computeIfAbsent(key, ignored -> new ExpansionAnimation(row.expanded()));
            expansion.setExpanded(row.expanded(), uiAnimationFrame);
            GeometryRenderer.drawDisclosureChevron(graphics, x + 11 + indent, y + 14,
                    isManagerTutorialActive() ? (row.expanded() ? 1F : 0F) : expansion.amount(uiAnimationFrame), ACTION);
            arrowOffset = 10;
        }

        int glyphX = x + 10 + indent + arrowOffset;
        boolean hasVisibleIcon = entry.showIcon() && EntryIconRenderer.isUsableIcon(minecraft, entry.glyph());
        int iconSlotWidth = hasVisibleIcon ? 22 : 0;
        int iconCenterX = glyphX + (iconSlotWidth / 2);
        int iconCenterY = y + (PROFILE_ROW_HEIGHT / 2);
        if (hasVisibleIcon) {
            EntryIconRenderer.drawIcon(graphics, minecraft, entry.glyph(), iconCenterX, iconCenterY, 0.9F);
        }

        String typeLabel = row.depth() == 0 ? entryTypeLabel(entry) : "";
        int typeWidth = minecraft.font.width(typeLabel);
        int textStartX = glyphX + (hasVisibleIcon ? iconSlotWidth + 6 : 0);
        int textWidth = Math.max(16, x + width - textStartX - typeWidth - 26);
        int labelColor = entry.active() ? entry.color() : ACTION_DISABLED;
        int summaryColor = entry.active() ? TEXT_SECONDARY : ACTION_DISABLED;
        graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, displayText(entry.label()), textWidth), textStartX, y + 7, labelColor, false);
        graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, actionSummary(entry.action()), textWidth), textStartX, y + 17, summaryColor, false);
        graphics.drawString(minecraft.font, typeLabel, x + width - typeWidth - 20, y + 11, !entry.active() ? ACTION_DISABLED : selected ? ACTION : TEXT_SECONDARY, false);
        drawDragHandle(graphics, x + width - 10, y + 8, "omniwheel.manager.drag.hint");
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
        int visualCount = draggedWheelActive ? (int) visibleRows.stream().filter(row -> !shouldSkipDraggedWheelRow(row)).count() + 1 : visibleRows.size();
        int contentHeight = listContentHeight(visualCount);
        boolean needsScrollbar = contentHeight > Math.max(0, actionY - y - 8);
        wheelsAreaX = x;
        wheelsAreaY = y;
        wheelsAreaWidth = width;
        wheelsAreaHeight = Math.max(0, actionY - y - 8);
        wheelsActionY = actionY;
        wheelsListWidth = width - (needsScrollbar ? (SCROLLBAR_WIDTH + SCROLLBAR_GAP) : 0);
        wheelsScrollOffset = clamp(wheelsScrollOffset, 0, Math.max(0, contentHeight - wheelsAreaHeight));
        updateWheelsScrollbarBounds(contentHeight);
        if (draggedWheelActive) {
            entryDropTarget = computeEntryDropTarget(currentMouseX(minecraft), currentMouseY(minecraft));
        }
        if (wheel != null) {
            enableUiScissor(graphics, x, y, x + wheelsListWidth, y + wheelsAreaHeight);
            int gap = draggedWheelActive ? entryGapSlot(visibleRows) : -1;
            if (draggedWheelActive) {
                drawDragPlaceholder(graphics, x, wheelRowY(gap), wheelsListWidth);
            }
            int slot = 0;
            var animatedEntries = entryRowsAnimation.update(selectedProfileId, visibleRows, this::dragRowKey,
                    uiAnimationFrame, draggedWheelActive || isManagerTutorialActive());
            if (!draggedWheelActive) drawAnimatedList(graphics, animatedEntries, x, y, wheelsListWidth, wheelsAreaHeight,
                    wheelsScrollOffset, entryRowHits, entryDragRowY,
                    (row, rowY) -> drawEntryListRow(graphics, minecraft, row, x, rowY, wheelsListWidth, false));
            for (EntryListRow row : visibleRows) {
                if (!draggedWheelActive) break;
                if (draggedWheelActive && shouldSkipDraggedWheelRow(row)) {
                    continue;
                }
                int visualSlot = slot + (draggedWheelActive && slot >= gap ? 1 : 0);
                int rowY = animateDragRow(entryDragRowY, dragRowKey(row), wheelRowY(visualSlot), draggedWheelActive);
                if (rowY + PROFILE_ROW_HEIGHT >= y && rowY <= y + wheelsAreaHeight) {
                    drawEntryListRow(graphics, minecraft, row, x, rowY, wheelsListWidth, false);
                }
                slot++;
            }
            drawEntryDropTarget(graphics, minecraft);
            graphics.disableScissor();
        }
        drawWheelsScrollbar(graphics, needsScrollbar);

        int halfWidth = (width - 8) / 2;
        addButton(graphics, minecraft, x, actionY, halfWidth, text("omniwheel.manager.new_command"), canCreateEntry(), this::createCommandEntry);
        addButton(graphics, minecraft, x + halfWidth + 8, actionY, halfWidth, text("omniwheel.manager.new_chat"), canCreateEntry(), this::createChatEntry);
        addButton(graphics, minecraft, x, actionY + 30, halfWidth, text("omniwheel.manager.new_submenu"), canCreateEntry(), this::createSubmenuEntry);
        addButton(graphics, minecraft, x + halfWidth + 8, actionY + 30, halfWidth, text("omniwheel.manager.new_function"), canCreateEntry(), this::createFunctionEntry);
        addButton(graphics, minecraft, x, actionY + 60, halfWidth, text("omniwheel.manager.new_keybind"), canCreateEntry(), () -> createEntry(DraftActionType.KEYBIND));
        addButton(graphics, minecraft, x + halfWidth + 8, actionY + 60, halfWidth, text("omniwheel.common.delete"), canDeleteSelection(), this::deleteSelection);
        addButton(graphics, minecraft, x, actionY + 90, width, selectedThingToggleLabel(), canToggleSelection(), this::toggleSelectionActive);
    }

    private void drawEditorPanel(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        // Inventory selection is content within the browser, not a newly opened panel.
        String view = iconPickerOpen ? "icons" : previewOpen ? "preview" : "editor";
        long now = uiAnimationFrame;
        if (!Objects.equals(view, revealedEditorView)) {
            editorRevealStarted = revealedEditorView == null || isManagerTutorialActive() ? 0 : now;
            revealedEditorView = view;
        }
        if (previewOpen) {
            editorRevealBottom = Integer.MAX_VALUE;
            drawEditorPanelContents(graphics, minecraft, x, y, width, height);
            return;
        }
        double progress = editorRevealStarted == 0 || isManagerTutorialActive() ? 1 : Math.clamp((now - editorRevealStarted) / 180_000_000.0, 0, 1);
        double ease = progress * progress * (3 - 2 * progress);
        if (iconPickerClosingAt != 0) {
            double closing = Math.clamp((now - iconPickerClosingAt) / 180_000_000.0, 0, 1);
            ease = iconPickerClosingFrom * (1 - closing * closing * (3 - 2 * closing));
        }
        editorRevealBottom = y + (int) Math.ceil(height * ease);
        enableUiScissor(graphics, x, y, x + width, editorRevealBottom);
        drawEditorPanelContents(graphics, minecraft, x, y, width, height);
        graphics.disableScissor();
    }

    private void drawEditorPanelContents(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        if (iconPickerOpen) {
            drawIconPicker(graphics, minecraft, x, y, width, height);
            return;
        }
        if (previewOpen) {
            radialPreview.sync(selectedWheelId, selectedEntryId);
            radialPreview.draw(graphics, minecraft, x, y, width, height,
                    isManagerTutorialActive() ? -10000 : currentMouseX(minecraft), isManagerTutorialActive() ? -10000 : currentMouseY(minecraft));
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
        int visibilityButtonWidth = visibilityButtonWidth(minecraft);
        int iconToggleButtonWidth = visibilityButtonWidth;
        int shortcutToggleButtonWidth = visibilityButtonWidth;
        int shortcutButtonWidth = Math.max(64, minecraft.font.width(text("omniwheel.manager.button.controls")) + 20);
        int labelToggleButtonWidth = visibilityButtonWidth;
        int iconToggleButtonGap = 6;
        int shortcutToggleButtonGap = 6;
        int shortcutButtonGap = 6;
        int nameButtonGap = 6;
        int nameFieldWidth = Math.max(48, leftColumnWidth - labelToggleButtonWidth - nameButtonGap);
        int iconFieldWidth = Math.max(48, leftColumnWidth - iconToggleButtonWidth - iconToggleButtonGap);
        int shortcutFieldWidth = Math.max(96, width - shortcutToggleButtonWidth - shortcutToggleButtonGap - shortcutButtonWidth - shortcutButtonGap);
        int lowerSectionWidth = width;
        // Appearance and action controls remain visible at every tutorial step.
        boolean showNameSection = true;
        boolean showIconSection = true;
        boolean showColorSection = true;
        boolean showLowerSection = true;
        boolean showDetailsSection = true;

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
            graphics.drawString(minecraft.font, text("omniwheel.manager.field.icon"), x, editorY + 52, TEXT_SECONDARY, false);
            addButton(graphics, minecraft, x, editorY + 64, iconFieldWidth,
                    ellipsizeToWidth(minecraft.font, iconDisplayName(entryGlyphDraft), iconFieldWidth - 16),
                    22, true, this::toggleIconPicker, "icon-picker");
            drawIconVisibilityButton(graphics, minecraft, x + iconFieldWidth + iconToggleButtonGap, editorY + 64, iconToggleButtonWidth, 22);
            tooltipRegions.add(new TooltipRegion(x, editorY + 64, iconFieldWidth, 22, text("omniwheel.manager.tooltip.icon_picker")));
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
        int sendModeButtonWidth = sendModeButtonWidth(minecraft);
        if (showLowerSection) {
            if (entryActionTypeDraft == DraftActionType.COMMAND) {
                ensureCommandValueFields();
                boolean showSendMode = commandValueFields.size() == 1;
                int commandFieldsWidth = lowerSectionWidth - (showSendMode ? sendModeButtonWidth + 6 : 0);
                drawCommandValueFields(graphics, minecraft, x, lowerSectionY, commandFieldsWidth, Math.max(140, height - 174));
                if (showSendMode) {
                    drawSendModeButton(graphics, minecraft, x + lowerSectionWidth - sendModeButtonWidth, lowerSectionY + 12, true);
                }
                textFields.get(EditorField.ENTRY_VALUE).visible = false;
            } else if (entryActionTypeDraft == DraftActionType.KEYBIND) {
                drawKeybindSelector(graphics, minecraft, x, lowerSectionY, lowerSectionWidth, Math.max(140, height - 174));
            } else if (entryActionTypeDraft == DraftActionType.FUNCTION) {
                drawFunctionSelector(graphics, minecraft, x, lowerSectionY, lowerSectionWidth, Math.max(140, height - 174));
                textFields.get(EditorField.ENTRY_VALUE).visible = false;
            } else if (entryActionTypeDraft == DraftActionType.SUBMENU) {
                textFields.get(EditorField.ENTRY_VALUE).visible = false;
            } else if (readOnlyValue) {
                drawReadOnlyField(graphics, minecraft, x, lowerSectionY, lowerSectionWidth, valueLabel, valueText);
                textFields.get(EditorField.ENTRY_VALUE).visible = false;
            } else {
                int dataWidth = dataButtonWidth(minecraft);
                int modeWidth = entryActionTypeDraft == DraftActionType.CHAT ? sendModeButtonWidth + 6 : 0;
                int valueWidth = lowerSectionWidth - modeWidth - dataWidth - 6;
                drawTextField(graphics, minecraft, EditorField.ENTRY_VALUE, x, lowerSectionY, valueWidth, valueLabel, true);
                addDataButton(graphics, minecraft, x + valueWidth + 6, lowerSectionY + 12, dataWidth, -1, true);
                if (entryValueDraft.contains("{")) {
                    var snapshot = LiveDataSnapshot.capture(minecraft);
                    String missing = snapshot.missingMessage(entryValueDraft);
                    String preview = missing == null ? "Preview: " + snapshot.resolve(entryValueDraft) : missing;
                    graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, preview, lowerSectionWidth), x, lowerSectionY + 40, missing == null ? TEXT_SECONDARY : WARNING, false);
                }
                if (entryActionTypeDraft == DraftActionType.CHAT) {
                    drawSendModeButton(graphics, minecraft, x + lowerSectionWidth - sendModeButtonWidth, lowerSectionY + 12, true);
                }
            }
        }

    }

    private int sendModeButtonWidth(Minecraft minecraft) {
        // Match both 24-pixel shortcut buttons and their 6-pixel gap.
        return 24 + 6 + 24;
    }

    private void drawSendModeButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean enabled) {
        int width = sendModeButtonWidth(minecraft);
        addButton(graphics, minecraft, x, y, width,
                text(entryPresetDraft ? "omniwheel.manager.button.mode_preset" : "omniwheel.manager.button.mode_send"), 22, enabled, () -> {
                    entryPresetDraft = !entryPresetDraft;
                    autosaveCurrentEntryDraft();
                }, "send-mode");
        tooltipRegions.add(new TooltipRegion(x, y, width, 22, text(!enabled
                ? "omniwheel.manager.tooltip.mode_multiple"
                : entryPresetDraft ? "omniwheel.manager.tooltip.mode_preset" : "omniwheel.manager.tooltip.mode_send")));
    }

    private static String iconDisplayName(String icon) {
        if (CustomImageData.isImage(icon)) return CustomImageData.displayName(icon);
        if (CustomItemIcons.isCustom(icon)) {
            ItemStack stack = CustomItemIcons.decode(icon);
            return stack.isEmpty() ? "Custom item" : stack.getHoverName().getString();
        }
        String normalized = normalizedIconId(icon);
        if (normalized.isEmpty()) return "Choose Icon";
        ResourceLocation itemId = ResourceLocation.tryParse(normalized);
        if (itemId != null) {
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            if (item != null && item != Items.AIR) return new ItemStack(item).getHoverName().getString();
        }
        if (normalized.startsWith("effect:")) {
            ResourceLocation effectId = ResourceLocation.tryParse(normalized.substring("effect:".length()));
            MobEffect effect = effectId == null ? null : BuiltInRegistries.MOB_EFFECT.getOptional(effectId).orElse(null);
            if (effect != null) return Component.translatable(effect.getDescriptionId()).getString();
        }
        String path = normalized.substring(normalized.lastIndexOf(':') + 1);
        StringBuilder name = new StringBuilder();
        for (String word : path.split("[_/ .-]+")) {
            if (word.isEmpty()) continue;
            if (!name.isEmpty()) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.isEmpty() ? "Choose Icon" : name.toString();
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
        boolean customSelected = java.util.stream.IntStream.range(0, colorCount - 1).noneMatch(index -> palette[index] == selectedColorValue);
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
            boolean customSwatch = colorIndex == colorCount - 1;
            int color = customSwatch ? selectedColorValue : palette[colorIndex];
            boolean defaultSwatch = colorIndex == 0;
            boolean selectedSwatch = customSwatch ? customSelected : defaultSwatch ? selectedColorValue == defaultColorValue : color == selectedColorValue;
            boolean focusedSwatch = !isReordering() && isNavigationActionFocused("palette:" + label + ":" + colorIndex);
            boolean hoveredSwatch = isButtonHighlighted(minecraft, swatchX, swatchY, swatchOuterSize, swatchOuterSize, false);
            if (selectedSwatch) {
                drawSelectedPaletteOutline(graphics, swatchX, swatchY, swatchOuterSize);
            } else {
                graphics.fill(swatchX + 1, swatchY + 1, swatchX + swatchOuterSize - 1, swatchY + swatchOuterSize - 1, FIELD_EDGE);
            }
            if (customSwatch && !customSelected) {
                for (int column = 0; column < swatchInnerSize; column++) {
                    graphics.fill(swatchX + swatchInnerInset + column, swatchY + swatchInnerInset,
                            swatchX + swatchInnerInset + column + 1, swatchY + swatchInnerInset + swatchInnerSize,
                            java.awt.Color.HSBtoRGB(column / (float) swatchInnerSize, .7F, .8F));
                }
            } else if (defaultSwatch) {
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
            if (customSwatch) {
                graphics.drawCenteredString(minecraft.font, "+", swatchX + swatchOuterSize / 2, swatchY + 7, 0xFFFFFFFF);
                tooltipRegions.add(new TooltipRegion(swatchX, swatchY, swatchOuterSize, swatchOuterSize, text("omniwheel.color.custom")));
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
            actions.add(new ClickAction(swatchX, swatchY, swatchOuterSize, swatchOuterSize, true, () -> {
                if (customSwatch) {
                    clearFocus(); clearProfileDrag(); clearWheelDrag(); clearCommandDrag();
                    customColorPicker.open("", selectedColorValue, onSelect);
                } else onSelect.accept(swatchColor);
            }, "palette:" + label + ":" + colorIndex, ""));
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

    private static int visibilityButtonWidth(Minecraft minecraft) {
        return Math.max(52, Math.max(minecraft.font.width(text("omniwheel.manager.button.shown")),
                minecraft.font.width(text("omniwheel.manager.button.hidden"))) + 20);
    }

    private void drawIconVisibilityButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        iconToggleButtonX = x;
        iconToggleButtonY = y;
        iconToggleButtonWidth = width;
        iconToggleButtonHeight = height;
        addButton(graphics, minecraft, x, y, width,
                text(entryShowIconDraft ? "omniwheel.manager.button.shown" : "omniwheel.manager.button.hidden"),
                height, true, this::toggleEntryIconVisibility, "icon-visibility");
    }

    private void drawLabelVisibilityButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        labelToggleButtonX = x;
        labelToggleButtonY = y;
        labelToggleButtonWidth = width;
        labelToggleButtonHeight = height;
        addButton(graphics, minecraft, x, y, width,
                text(entryShowLabelDraft ? "omniwheel.manager.button.shown" : "omniwheel.manager.button.hidden"),
                height, true, this::toggleEntryLabelVisibility, "label-visibility");
    }

    private void drawShortcutVisibilityButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        shortcutToggleButtonX = x;
        shortcutToggleButtonY = y;
        shortcutToggleButtonWidth = width;
        shortcutToggleButtonHeight = height;
        addButton(graphics, minecraft, x, y, width,
                text(entryShowShortcutDraft ? "omniwheel.manager.button.shown" : "omniwheel.manager.button.hidden"),
                height, true, this::toggleEntryShortcutVisibility, "shortcut-visibility");
    }

    private void drawShortcutControlsButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        shortcutButtonX = x;
        shortcutButtonY = y;
        shortcutButtonWidth = width;
        shortcutButtonHeight = height;
        boolean focused = isNavigationActionFocused("controls");
        boolean highlighted = isButtonHighlighted(minecraft, x, y, width, height, focused);
        drawButtonChrome(graphics, x, y, width, height, ROW, ROW_SELECTED, true, highlighted);
        graphics.drawCenteredString(minecraft.font, text("omniwheel.manager.button.controls"), x + (width / 2), y + 7, highlighted ? TEXT : ACTION);
        actions.add(new ClickAction(x, y, width, height, true, this::openControlsMenu, "controls", ""));
    }

    private void drawEditorButtonTooltip(GuiGraphics graphics, Minecraft minecraft) {
        if (previewOpen || iconPickerOpen || isManagerTutorialActive()) {
            return;
        }
        float mouseX = currentMouseX(minecraft);
        float mouseY = currentMouseY(minecraft);
        String tooltip = null;
        if (contains(mouseX, mouseY, iconToggleButtonX, iconToggleButtonY, iconToggleButtonWidth, iconToggleButtonHeight)) {
            tooltip = text(entryShowIconDraft ? "omniwheel.manager.tooltip.icon_off" : "omniwheel.manager.tooltip.icon_on");

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

        graphics.nextStratum();
        drawPanel(graphics, boxX, boxY, boxWidth, boxHeight, false);
        graphics.drawString(minecraft.font, text, boxX + paddingX, boxY + paddingY, TEXT, false);
    }

    private void startManagerTutorial() {
        customColorPicker.close();
        tutorialKeyboardFocus = false;
        persistPendingEdits();
        restoreTutorialSelection();
        runtime.tutorialProgressStore().setCurrentStep(TutorialType.MANAGER, 0);
        runtime.tutorialProgressStore().setCurrentStep(TutorialType.RADIAL, 0);
        managerTutorialStepIndex = 0;
        preparedTutorialStep = -1;
        tutorialKeyboardButton = 1;
    }

    private boolean selectTutorialEntry(java.util.function.Predicate<WheelAction> predicate) {
        var candidates = new ArrayList<WheelProfile>();
        if (selectedProfile() != null) candidates.add(selectedProfile());
        runtime.profileManager().getProfiles().stream().filter(profile -> profile.id().equals("default") && !candidates.contains(profile)).findFirst().ifPresent(candidates::add);
        for (WheelProfile profile : runtime.profileManager().getProfiles()) if (!candidates.contains(profile)) candidates.add(profile);
        for (WheelProfile profile : candidates) for (WheelDefinition wheel : profile.wheels().values()) for (WheelEntry entry : wheel.entries()) {
            if (!predicate.test(entry.action())) continue;
            selectedProfileId = profile.id(); selectedWheelId = wheel.id(); selectedEntryId = entry.id();
            expandedWheelIds.add(wheel.id());
            syncSelection(true);
            return true;
        }
        return false;
    }

    private void prepareManagerTutorialStep() {
        if (!isManagerTutorialActive() || preparedTutorialStep == managerTutorialStepIndex) return;
        if (tutorialSelection == null) tutorialSelection = new TutorialSelection(selectedProfileId, selectedWheelId, selectedEntryId,
                Set.copyOf(expandedWheelIds), previewOpen, profilesScrollOffset, wheelsScrollOffset);
        preparedTutorialStep = managerTutorialStepIndex;
        customColorPicker.close();
        clearProfileDrag(); clearWheelDrag(); clearCommandDrag(); radialPreview.cancelDrag();
        draggingProfilesScrollbar = draggingWheelsScrollbar = draggingIconPickerScrollbar = draggingFunctionListScrollbar = draggingCommandListScrollbar = false;
        closeIconPicker();
        previewOpen = false;
        clearFocus();
        selectedProfileId = tutorialSelection.profile();
        selectedWheelId = tutorialSelection.wheel(); selectedEntryId = tutorialSelection.entry();
        syncSelection(true);
        TutorialGuide.Step step = TutorialGuide.step(TutorialType.MANAGER, managerTutorialStepIndex);
        switch (step.scene()) {
            case EDITOR -> {
                if (selectedEntry() == null && (step.focus() == TutorialGuide.Focus.APPEARANCE || step.focus() == TutorialGuide.Focus.SHORTCUTS)) selectTutorialEntry(action -> true);
            }
            case ICONS -> { selectTutorialEntry(action -> true); toggleIconPicker(); }
            case COLORS -> {
                if (selectedEntry() == null) selectTutorialEntry(action -> true);
                customColorPicker.open("", entryColorDraft, ignored -> {});
            }
            case CHAT -> selectTutorialEntry(action -> action instanceof ChatAction || action instanceof OpenChatAction);
            case COMMANDS -> selectTutorialEntry(action -> action instanceof CommandAction);
            case DATA -> {
                selectTutorialEntry(action -> action instanceof ChatAction || action instanceof CopyTextAction || action instanceof LocalMessageAction || action instanceof OpenChatAction);
                openDataPicker(-1);
            }
            case KEYBINDS -> {
                if (!selectTutorialEntry(action -> action instanceof KeybindAction)) selectTutorialEntry(action -> action instanceof FunctionAction);
            }
            case PREVIEW, SUBMENU -> {
                if (selectedProfile() != null && selectedProfile().wheel(selectedProfile().rootWheelId()).entries().isEmpty())
                    selectTutorialEntry(action -> action instanceof OpenWheelAction);
                WheelProfile profile = selectedProfile();
                if (profile != null) {
                    selectedWheelId = profile.rootWheelId();
                    if (step.scene() == TutorialGuide.Scene.SUBMENU) {
                        profile.wheel(profile.rootWheelId()).radialEntries().stream().filter(entry -> entry.action() instanceof OpenWheelAction)
                                .findFirst().ifPresent(entry -> selectedWheelId = ((OpenWheelAction) entry.action()).wheelId());
                    }
                    selectedEntryId = null;
                    syncSelection(true);
                    previewOpen = true;
                }
            }
        }
        clearFocus();
    }

    private void restoreTutorialSelection() {
        customColorPicker.close();
        if (tutorialSelection == null) return;
        TutorialSelection saved = tutorialSelection;
        tutorialSelection = null; preparedTutorialStep = -1;
        finishCloseIconPicker(); clearFocus();
        selectedProfileId = saved.profile(); selectedWheelId = saved.wheel(); selectedEntryId = saved.entry();
        expandedWheelIds.clear(); expandedWheelIds.addAll(saved.expanded());
        profilesScrollOffset = saved.profilesScroll(); wheelsScrollOffset = saved.entriesScroll();
        previewOpen = saved.preview();
        syncSelection(true);
    }

    private Bounds managerTutorialFocus(TutorialGuide.Step step) {
        return switch (step.focus()) {
            case ALL -> new Bounds(18, 18, uiWidth(Minecraft.getInstance()) - 36, uiHeight(Minecraft.getInstance()) - 36);
            case PROFILES -> new Bounds(profilesPanelX, profilesPanelY, profilesPanelWidth, profilesPanelHeight);
            case ENTRIES -> new Bounds(wheelsPanelX, wheelsPanelY, wheelsPanelWidth, wheelsPanelHeight);
            case CREATE -> new Bounds(wheelsPanelX + 6, wheelsActionY - 6, wheelsPanelWidth - 12, 132);
            case APPEARANCE -> new Bounds(editorPanelX + 6, editorPanelY + 26, editorPanelWidth - 12, 162);
            case COLORS -> customColorPicker.bounds();
            case BROWSER, DATA -> new Bounds(iconPickerX - 3, iconPickerY - 3, iconPickerWidth + 6, iconPickerHeight + 6);
            case IMPORTS -> new Bounds(iconPickerX - 3, iconPickerY + 26, iconPickerWidth + 6, 32);
            case ACTION -> new Bounds(editorPanelX + 6, editorPanelY + 194, editorPanelWidth - 12, editorPanelHeight - 202);
            case SHORTCUTS -> new Bounds(editorPanelX + 6, editorPanelY + 140, editorPanelWidth - 12, 48);
            case PREVIEW -> new Bounds(editorPanelX + 4, editorPanelY + 4, editorPanelWidth - 8, editorPanelHeight - 8);
        };
    }

    private void drawManagerTutorial(GuiGraphics graphics, Minecraft minecraft) {
        TutorialGuide.Step step = currentManagerTutorialStep();
        if (step == null) return;
        float mouseX = currentMouseX(minecraft), mouseY = currentMouseY(minecraft);
        if (!Float.isNaN(tutorialFocusMouseX) && (Math.abs(mouseX - tutorialFocusMouseX) > 0.5F || Math.abs(mouseY - tutorialFocusMouseY) > 0.5F)) tutorialKeyboardFocus = false;
        tutorialFocusMouseX = mouseX; tutorialFocusMouseY = mouseY;
        Bounds focus = managerTutorialFocus(step);
        if (step.id().equals("preview")) radialPreview.drawTutorialSpotlight(graphics, uiWidth(minecraft), uiHeight(minecraft));
        else TutorialSpotlight.draw(graphics, uiWidth(minecraft), uiHeight(minecraft), focus);
        boolean preview = step.focus() == TutorialGuide.Focus.PREVIEW;
        boolean leftFocus = focus.right() <= editorPanelX;
        int boxWidth = preview ? profilesPanelWidth - 8 : Math.min(330, editorPanelWidth - 24);
        String title = text(step.titleKey(TutorialType.MANAGER));
        var titleLines = minecraft.font.split(Component.literal(title), boxWidth - 20);
        var bodyLines = minecraft.font.split(Component.literal(text(step.bodyKey(TutorialType.MANAGER))), boxWidth - 20);
        int boxHeight = 24 + titleLines.size() * 10 + 8 + bodyLines.size() * 10 + 36;
        int boxX = preview || !leftFocus && step.focus() != TutorialGuide.Focus.ALL ? profilesPanelX + 4 : editorPanelX + 12;
        int boxY = step.focus() == TutorialGuide.Focus.ALL ? uiHeight(minecraft) - boxHeight - 38 : profilesPanelY + 38;
        boxY = clamp(boxY, 8, Math.max(8, uiHeight(minecraft) - boxHeight - 8));
        LiftedRowRenderer.shadow(graphics, boxX, boxY, boxWidth, boxHeight);
        graphics.nextStratum();
        drawPanel(graphics, boxX, boxY, boxWidth, boxHeight, true);
        graphics.drawString(minecraft.font, "Guide " + (managerTutorialStepIndex + 1) + "/" + MANAGER_TUTORIAL_STEP_COUNT, boxX + 10, boxY + 10, ACTION, false);
        int lineY = boxY + 25;
        for (var line : titleLines) { graphics.drawString(minecraft.font, line, boxX + 10, lineY, TEXT, false); lineY += 10; }
        lineY += 8;
        for (var line : bodyLines) { graphics.drawString(minecraft.font, line, boxX + 10, lineY, TEXT_SECONDARY, false); lineY += 10; }
        int buttonWidth = (boxWidth - 28) / 3, buttonY = boxY + boxHeight - 28;
        tutorialBackButtonX = boxX + 8; tutorialBackButtonY = buttonY; tutorialBackButtonWidth = buttonWidth; tutorialBackButtonHeight = 20;
        tutorialNextButtonX = boxX + 14 + buttonWidth; tutorialNextButtonY = buttonY; tutorialNextButtonWidth = buttonWidth; tutorialNextButtonHeight = 20;
        tutorialSkipAllButtonX = boxX + 20 + buttonWidth * 2; tutorialSkipAllButtonY = buttonY; tutorialSkipAllButtonWidth = buttonWidth; tutorialSkipAllButtonHeight = 20;
        drawGuideButton(graphics, minecraft, tutorialBackButtonX, buttonY, buttonWidth, "Back", managerTutorialStepIndex > 0, 0);
        drawGuideButton(graphics, minecraft, tutorialNextButtonX, buttonY, buttonWidth, managerTutorialStepIndex == MANAGER_TUTORIAL_STEP_COUNT - 1 ? "Finish" : "Next", true, 1);
        drawGuideButton(graphics, minecraft, tutorialSkipAllButtonX, buttonY, buttonWidth, "Skip", true, 2);
    }

    private void drawGuideButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String label, boolean enabled, int index) {
        boolean highlighted = enabled && ((tutorialKeyboardFocus && tutorialKeyboardButton == index) || contains(currentMouseX(minecraft), currentMouseY(minecraft), x, y, width, 20));
        drawButtonChrome(graphics, x, y, width, 20, ROW, ROW_SELECTED, enabled, highlighted, true);
        graphics.drawCenteredString(minecraft.font, label, x + width / 2, y + 6, enabled ? TEXT : ACTION_DISABLED);
    }

    private boolean handleManagerTutorialClick(float mouseX, float mouseY) {
        if (!isManagerTutorialActive()) return false;
        tutorialKeyboardFocus = false;
        if (contains(mouseX, mouseY, tutorialBackButtonX, tutorialBackButtonY, tutorialBackButtonWidth, tutorialBackButtonHeight)) previousManagerTutorial();
        else if (contains(mouseX, mouseY, tutorialNextButtonX, tutorialNextButtonY, tutorialNextButtonWidth, tutorialNextButtonHeight)) advanceManagerTutorial();
        else if (contains(mouseX, mouseY, tutorialSkipAllButtonX, tutorialSkipAllButtonY, tutorialSkipAllButtonWidth, tutorialSkipAllButtonHeight)) completeManagerTutorial();
        return true;
    }

    private void previousManagerTutorial() {
        if (managerTutorialStepIndex <= 0) return;
        managerTutorialStepIndex--;
        tutorialKeyboardButton = 1;
        runtime.tutorialProgressStore().setCurrentStep(TutorialType.MANAGER, managerTutorialStepIndex);
    }

    private void advanceManagerTutorial() {
        if (managerTutorialStepIndex < 0) return;
        if (++managerTutorialStepIndex >= MANAGER_TUTORIAL_STEP_COUNT) { completeManagerTutorial(); return; }
        tutorialKeyboardButton = 1;
        runtime.tutorialProgressStore().setCurrentStep(TutorialType.MANAGER, managerTutorialStepIndex);
    }

    private void completeManagerTutorial() {
        runtime.tutorialProgressStore().markCompleted(TutorialType.MANAGER);
        managerTutorialStepIndex = -1;
        restoreTutorialSelection();
    }

    private boolean isManagerTutorialActive() {
        return managerTutorialStepIndex >= 0 && !runtime.tutorialProgressStore().isCompleted(TutorialType.MANAGER);
    }

    private TutorialGuide.Step currentManagerTutorialStep() {
        return isManagerTutorialActive() ? TutorialGuide.step(TutorialType.MANAGER, managerTutorialStepIndex) : null;
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
            InputConstants.Key bound = OmniWheelKeyMappings.currentKey(keyMapping);
            if (bound.getType() == InputConstants.Type.MOUSE && spec.mouseButtons().contains(bound.getValue())
                    || bound.getType() == InputConstants.Type.SCANCODE && spec.scanCodes().contains(bound.getValue())) {
                conflicts.add(Component.translatable(keyMapping.getName()).getString());
                continue;
            }
            for (int key : spec.keys()) {
                if (keyMapping.matches(new KeyEvent(key, -1, 0))) {
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
        int gripWidth = showRemoveButtons ? 18 : 0;
        int fieldX = x + gripWidth;
        int dataButtonWidth = dataButtonWidth(minecraft);
        int fieldWidth = Math.max(64, commandListWidth - gripWidth - removeButtonWidth - removeButtonGap - dataButtonWidth - 6);
        if (draggedCommandActive) updateCommandDrop(currentMouseX(minecraft), currentMouseY(minecraft));

        enableUiScissor(graphics, commandListX, commandListY, commandListX + commandListWidth, commandListY + commandListHeight);
        int gap = draggedCommandDropValid ? draggedCommandTargetIndex : draggedCommandIndex;
        if (draggedCommandActive) drawDragPlaceholder(graphics, x, commandRowY(gap), commandListWidth, 34);
        int slot = 0;
        for (int index = 0; index < commandValueFields.size(); index++) {
            if (draggedCommandActive && index == draggedCommandIndex) {
                commandValueFields.get(index).visible = false;
                continue;
            }
            int visualSlot = slot + (draggedCommandActive && slot >= gap ? 1 : 0);
            int fieldY = animateDragRow(commandDragRowY, Integer.toString(index), commandRowY(visualSlot), draggedCommandActive);
            slot++;
            if (fieldY + 34 < commandListY || fieldY > commandListY + commandListHeight) {
                commandValueFields.get(index).visible = false;
                addDataButton(graphics, minecraft, fieldX + fieldWidth + 6, fieldY + 12, dataButtonWidth, index, false);
                if (showRemoveButtons) {
                    final int commandIndex = index;
                    actions.add(new ClickAction(fieldX + fieldWidth + 6 + dataButtonWidth + removeButtonGap, fieldY + 12, removeButtonWidth, 22,
                            true, () -> removeCommandValueField(commandIndex), "remove-command:" + index, "commands", commandListScrollOffset));
                }
                continue;
            }
            if (isUsingVanillaCommandInput(index)) {
                drawActiveCommandField(graphics, minecraft, fieldX, fieldY, fieldWidth, text("omniwheel.manager.command_number", index + 1), index);
            } else {
                drawTextFieldState(
                        graphics,
                        minecraft,
                        commandValueFields.get(index),
                        fieldX,
                        fieldY,
                        fieldWidth,
                        text("omniwheel.manager.command_number", index + 1),
                        true
                );
            }
            addDataButton(graphics, minecraft, fieldX + fieldWidth + 6, fieldY + 12, dataButtonWidth, index, true);
            if (showRemoveButtons) {
                drawDragHandle(graphics, x + 4, fieldY + 17, "omniwheel.manager.drag.command_hint");
                drawCommandRemoveButton(
                        graphics,
                        minecraft,
                        fieldX + fieldWidth + 6 + dataButtonWidth + removeButtonGap,
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
            addButton(graphics, minecraft, addCommandButtonX, addCommandButtonY, addCommandButtonWidth, text("omniwheel.manager.add_command"), !entryPresetDraft, this::addCommandValueField);
        } else {
            actions.add(new ClickAction(addCommandButtonX, addCommandButtonY, addCommandButtonWidth, addCommandButtonHeight,
                    !entryPresetDraft, this::addCommandValueField, "button:" + text("omniwheel.manager.add_command"), "commands", commandListScrollOffset));
        }
        drawCommandListScrollbar(graphics, needsScrollbar);
    }

    private int commandRowY(int index) {
        return commandListY + index * 38 - commandListScrollOffset;
    }

    private boolean tryStartCommandDrag(float mouseX, float mouseY) {
        if (entryActionTypeDraft != DraftActionType.COMMAND || commandValueFields.size() < 2
                || !contains(mouseX, mouseY, commandListX, commandListY, commandListWidth, commandListHeight)) return false;
        for (int index = 0; index < commandValueFields.size(); index++) {
            int y = commandRowY(index);
            // The grip and label move the command; its input remains available for editing.
            if (mouseY < y || mouseY > y + 34 || (mouseX >= commandListX + 18 && mouseY >= y + 12)) continue;
            applyFocusedFieldDraft();
            clearFocus();
            draggedCommandIndex = index;
            draggedCommandTargetIndex = index;
            commandDragStartX = mouseX;
            commandDragStartY = mouseY;
            commandGrabX = mouseX - commandListX;
            commandGrabY = mouseY - y;
            return true;
        }
        return false;
    }

    private void updateCommandDrag(float mouseX, float mouseY, boolean pressed) {
        if (draggedCommandIndex < 0) return;
        if (!pressed) {
            if (draggedCommandActive) updateCommandDrop(mouseX, mouseY);
            finishCommandDrag();
            return;
        }

        if (!draggedCommandActive) {
            if (Math.hypot(mouseX - commandDragStartX, mouseY - commandDragStartY) < PROFILE_DRAG_THRESHOLD) return;
            draggedCommandActive = true;
        }
        updateCommandDrop(mouseX, mouseY);
    }

    private void updateCommandDrop(float mouseX, float mouseY) {
        draggedCommandDropValid = contains(mouseX, mouseY, commandListX, commandListY, commandListWidth, commandListHeight);
        if (!draggedCommandDropValid) return;
        if (Math.abs(mouseX - lastCommandDropX) < 2 && Math.abs(mouseY - lastCommandDropY) < 2
                && lastCommandDropScroll == commandListScrollOffset && lastCommandDropTop == commandListY) return;
        boolean sameScroll = lastCommandDropScroll == commandListScrollOffset;
        lastCommandDropX = mouseX;
        lastCommandDropY = mouseY;
        lastCommandDropScroll = commandListScrollOffset;
        lastCommandDropTop = commandListY;
        int gap = draggedCommandTargetIndex;
        if (mouseY >= commandRowY(gap) && mouseY < commandRowY(gap) + 38) return;
        int boundary = 0, slot = 0;
        for (int index = 0; index < commandValueFields.size(); index++) {
            if (index == draggedCommandIndex) continue;
            int projectedY = commandRowY(slot + (slot >= gap ? 1 : 0));
            float y = sameScroll ? commandDragRowY.getOrDefault(Integer.toString(index), (float) projectedY) : projectedY;
            if (mouseY > y + 17) boundary++;
            slot++;
        }
        draggedCommandTargetIndex = boundary;
    }

    private void finishCommandDrag() {
        int source = draggedCommandIndex, target = draggedCommandTargetIndex;
        boolean move = draggedCommandActive && draggedCommandDropValid && source >= 0
                && source < commandValueDrafts.size() && target >= 0 && target < commandValueDrafts.size() && source != target;
        clearCommandDrag();
        if (move) {
            String command = commandValueDrafts.remove(source);
            commandValueDrafts.add(target, command);
            syncCommandValueFields();
            autosaveCurrentEntryDraft();
        }
    }

    private void clearCommandDrag() {
        draggedCommandIndex = -1;
        draggedCommandActive = false;
        draggedCommandDropValid = false;
        lastCommandDropX = lastCommandDropY = Float.NaN;
        lastCommandDropScroll = lastCommandDropTop = -1;
        commandDragRowY.clear();
    }

    private void drawCommandDragPreview(GuiGraphics graphics, Minecraft minecraft) {
        if (draggedCommandIndex < 0 || draggedCommandIndex >= commandValueDrafts.size()) return;
        int width = commandListWidth;
        int x = clamp(Math.round(currentMouseX(minecraft) - commandGrabX + 3), 4, uiWidth(minecraft) - width - 10);
        int y = clamp(Math.round(currentMouseY(minecraft) - commandGrabY - 4), 4, uiHeight(minecraft) - 46);
        graphics.nextStratum();
        graphics.fill(x - 3, y + 7, x + width + 9, y + 47, 0x18000000);
        graphics.fill(x - 1, y + 5, x + width + 6, y + 43, 0x30000000);
        graphics.fill(x + 2, y + 4, x + width + 4, y + 40, 0x60000000);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(1.025F, 1.025F);
        graphics.fill(0, 0, width, 34, DRAG_SURFACE);
        graphics.fill(0, 0, width, 1, DRAG_EDGE);
        graphics.fill(0, 33, width, 34, DRAG_EDGE);
        graphics.fill(0, 1, 1, 33, DRAG_EDGE);
        graphics.fill(width - 1, 1, width, 33, DRAG_EDGE);
        drawDragHandle(graphics, 4, 17, "omniwheel.manager.drag.command_hint");
        graphics.drawString(minecraft.font, text("omniwheel.manager.command_number", draggedCommandIndex + 1), 18, 2, TEXT_SECONDARY, false);
        graphics.fill(19, 13, width - 3, 31, FIELD);
        graphics.fill(18, 12, width - 2, 13, DRAG_FIELD_EDGE);
        graphics.fill(18, 31, width - 2, 32, DRAG_FIELD_EDGE);
        graphics.fill(18, 13, 19, 31, DRAG_FIELD_EDGE);
        graphics.fill(width - 3, 13, width - 2, 31, DRAG_FIELD_EDGE);
        graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, commandValueDrafts.get(draggedCommandIndex), width - 34), 26, 19, TEXT, false);
        graphics.pose().popMatrix();
        if (!draggedCommandDropValid) graphics.drawString(minecraft.font, text("omniwheel.manager.drag.cancel"), x + 3, y + 46, WARNING, true);
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
        boolean focused = isNavigationActionFocused("remove-command:" + index);
        boolean highlighted = isButtonHighlighted(minecraft, x, y, width, height, focused);
        drawButtonChrome(graphics, x, y, width, height, ROW, ROW_SELECTED, true, highlighted);
        graphics.drawCenteredString(minecraft.font, "-", x + (width / 2), y + 7, highlighted ? TEXT : ACTION);
        tooltipRegions.add(new TooltipRegion(x, y, width, height, text("omniwheel.manager.tooltip.delete_command")));
        actions.add(new ClickAction(x, y, width, height, true, () -> removeCommandValueField(index), "remove-command:" + index, "commands", commandListScrollOffset));
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
        int rowHeight = SELECTOR_ROW_HEIGHT;
        int rowStep = SELECTOR_ROW_STEP;
        int contentHeight = selectorContentHeight(functions.size());
        boolean needsScrollbar = contentHeight > Math.max(0, Math.max(64, height - 12));
        functionListX = x;
        functionListY = y + 12;
        functionListWidth = width - (needsScrollbar ? (SCROLLBAR_WIDTH + SCROLLBAR_GAP) : 0);
        functionListHeight = Math.max(64, height - 12);
        functionListScrollOffset = clamp(functionListScrollOffset, 0, Math.max(0, contentHeight - functionListHeight));
        updateFunctionListScrollbarBounds(contentHeight);

        graphics.drawString(minecraft.font, text("omniwheel.manager.field.function"), x, y, TEXT_SECONDARY, false);
        graphics.fill(functionListX, functionListY, functionListX + functionListWidth, functionListY + functionListHeight, FIELD);

        int rowX = functionListX + SELECTOR_PADDING;
        int rowWidth = functionListWidth - SELECTOR_PADDING * 2;
        enableUiScissor(graphics, functionListX + 1, functionListY + 1, functionListX + functionListWidth - 1, functionListY + functionListHeight - 1);
        for (int index = 0; index < functions.size(); index++) {
            GameplayFunction function = functions.get(index);
            int rowY = functionListY + SELECTOR_PADDING + (index * rowStep) - functionListScrollOffset;
            ClickAction optionAction = new ClickAction(rowX, rowY, rowWidth, rowHeight, true, () -> {
                entryFunctionDraft = function;
                autosaveCurrentEntryDraft();
            }, "function:" + function.name(), "functions", functionListScrollOffset);
            if (rowY + rowHeight < functionListY || rowY > functionListY + functionListHeight) {
                actions.add(optionAction);
                continue;
            }

            boolean selected = function == entryFunctionDraft;
            boolean focusedRow = isActionFocused(optionAction);
            graphics.fill(rowX, rowY, rowX + rowWidth, rowY + rowHeight, selected ? ROW_SELECTED : ROW);
            if (selected || focusedRow) {
                drawFocusedActionOutline(graphics, rowX, rowY, rowWidth, rowHeight);
            }

            String title = ellipsizeToWidth(minecraft.font, function.displayName(), rowWidth - 16);
            String detail = ellipsizeToWidth(minecraft.font, function.category().label() + " | " + functionModeLabel(function.mode()), rowWidth - 16);
            graphics.drawString(minecraft.font, title, rowX + 8, rowY + 7, TEXT, false);
            graphics.drawString(minecraft.font, detail, rowX + 8, rowY + 19, selected ? ACTION : TEXT_SECONDARY, false);

            actions.add(optionAction);
        }
        graphics.disableScissor();
        graphics.fill(functionListX, functionListY, functionListX + functionListWidth, functionListY + 1, FIELD_EDGE);
        graphics.fill(functionListX, functionListY + functionListHeight - 1, functionListX + functionListWidth, functionListY + functionListHeight, FIELD_EDGE);
        graphics.fill(functionListX, functionListY, functionListX + 1, functionListY + functionListHeight, FIELD_EDGE);
        graphics.fill(functionListX + functionListWidth - 1, functionListY, functionListX + functionListWidth, functionListY + functionListHeight, FIELD_EDGE);
        drawFunctionListScrollbar(graphics, needsScrollbar);
    }

    private void drawKeybindSelector(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        drawReadOnlyField(graphics, minecraft, x, y, width, text("omniwheel.manager.field.keybind"), Component.translatable(entryKeybindDraft).getString());
        drawTextField(graphics, minecraft, EditorField.ENTRY_VALUE, x, y + 38, width, text("omniwheel.manager.field.keybind_search"), true);
        y += 76;
        height -= 76;
        List<KeybindGroupLayout> groups = layoutKeybindGroups(minecraft);
        int count = groups.stream().mapToInt(group -> group.group().options().size()).sum();
        int contentHeight = keybindContentHeight(groups);
        boolean needsScrollbar = contentHeight > Math.max(64, height - 12);
        functionListX = x;
        functionListY = y + 12;
        functionListWidth = width - (needsScrollbar ? (SCROLLBAR_WIDTH + SCROLLBAR_GAP) : 0);
        functionListHeight = Math.max(64, height - 12);
        functionListScrollOffset = clamp(functionListScrollOffset, 0, Math.max(0, contentHeight - functionListHeight));
        updateFunctionListScrollbarBounds(contentHeight);
        graphics.drawString(minecraft.font, text("omniwheel.manager.field.keybind_results", count), x, y, TEXT_SECONDARY, false);
        graphics.fill(functionListX, functionListY, functionListX + functionListWidth, functionListY + functionListHeight, FIELD);
        int rowX = functionListX + SELECTOR_PADDING;
        int rowWidth = functionListWidth - SELECTOR_PADDING * 2;
        enableUiScissor(graphics, functionListX + 1, functionListY + 1, functionListX + functionListWidth - 1, functionListY + functionListHeight - 1);
        for (KeybindGroupLayout layout : groups) {
            KeybindCategories.Group group = layout.group();
            int headerY = functionListY + SELECTOR_PADDING + layout.top() - functionListScrollOffset;
            ClickAction header = new ClickAction(rowX, headerY, rowWidth, KEYBIND_CATEGORY_HEIGHT, true,
                    () -> toggleKeybindCategory(group.id()), "keybind-category:" + group.id(), "functions", functionListScrollOffset);
            actions.add(header);
            boolean selectedCategory = group.options().stream().anyMatch(option -> option.id().equals(entryKeybindDraft));
            if (headerY + KEYBIND_CATEGORY_HEIGHT >= functionListY && headerY < functionListY + functionListHeight) {
                boolean highlighted = isButtonHighlighted(minecraft, rowX, headerY, rowWidth, KEYBIND_CATEGORY_HEIGHT, isActionFocused(header));
                graphics.fill(rowX, headerY, rowX + rowWidth, headerY + KEYBIND_CATEGORY_HEIGHT, highlighted ? ROW_SELECTED : ROW);
                GeometryRenderer.drawDisclosureChevron(graphics, rowX + 11, headerY + KEYBIND_CATEGORY_HEIGHT / 2F, layout.expansion(), ACTION);
                graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, group.name() + " (" + group.options().size() + ")", rowWidth - 32),
                        rowX + 25, headerY + 8, selectedCategory ? ACTION : TEXT, false);
                if (isActionFocused(header)) drawFocusedActionOutline(graphics, rowX, headerY, rowWidth, KEYBIND_CATEGORY_HEIGHT);
            }
            if (layout.bodyHeight() <= 0) continue;
            int bodyTop = headerY + KEYBIND_CATEGORY_HEIGHT + SELECTOR_PADDING;
            int bodyBottom = bodyTop + layout.bodyHeight();
            enableUiScissor(graphics, rowX, bodyTop, rowX + rowWidth, bodyBottom);
            for (int index = 0; index < group.options().size(); index++) {
                var keybind = group.options().get(index);
                int optionX = rowX + 10, optionWidth = rowWidth - 10;
                int rowY = bodyTop + index * SELECTOR_ROW_STEP;
                if (rowY >= bodyBottom) break;
                ClickAction action = new ClickAction(optionX, rowY, optionWidth, SELECTOR_ROW_HEIGHT, true, () -> {
                    entryKeybindDraft = keybind.id();
                    autosaveCurrentEntryDraft();
                }, "keybind:" + keybind.id(), "functions", functionListScrollOffset);
                boolean accessible = group.expanded() && rowY + SELECTOR_ROW_HEIGHT <= bodyBottom;
                if (accessible) actions.add(action);
                if (rowY + SELECTOR_ROW_HEIGHT < functionListY || rowY >= functionListY + functionListHeight) continue;
                boolean selected = keybind.id().equals(entryKeybindDraft);
                boolean focused = accessible && isActionFocused(action);
                boolean hovered = accessible && isButtonHighlighted(minecraft, optionX, rowY, optionWidth, SELECTOR_ROW_HEIGHT, false);
                graphics.fill(optionX, rowY, optionX + optionWidth, rowY + SELECTOR_ROW_HEIGHT, selected || hovered ? ROW_SELECTED : ROW);
                if (selected || focused) drawFocusedActionOutline(graphics, optionX, rowY, optionWidth, SELECTOR_ROW_HEIGHT);
                graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, keybind.name(), optionWidth - 16), optionX + 8, rowY + 7, TEXT, false);
                graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, keybind.id(), optionWidth - 16), optionX + 8, rowY + 19, selected ? ACTION : TEXT_SECONDARY, false);
            }
            graphics.disableScissor();
        }
        if (groups.isEmpty()) graphics.drawString(minecraft.font, text("omniwheel.manager.keybinds.empty"), rowX + 8, functionListY + 12, TEXT_SECONDARY, false);
        graphics.disableScissor();
        graphics.fill(functionListX, functionListY, functionListX + functionListWidth, functionListY + 1, FIELD_EDGE);
        graphics.fill(functionListX, functionListY + functionListHeight - 1, functionListX + functionListWidth, functionListY + functionListHeight, FIELD_EDGE);
        graphics.fill(functionListX, functionListY, functionListX + 1, functionListY + functionListHeight, FIELD_EDGE);
        graphics.fill(functionListX + functionListWidth - 1, functionListY, functionListX + functionListWidth, functionListY + functionListHeight, FIELD_EDGE);
        drawFunctionListScrollbar(graphics, needsScrollbar);
    }

    private void toggleKeybindCategory(String id) {
        Set<String> collapsed = entryValueDraft.isBlank() ? collapsedKeybindCategories : searchCollapsedKeybindCategories;
        if (!collapsed.remove(id)) collapsed.add(id);
    }

    private List<KeybindGroupLayout> layoutKeybindGroups(Minecraft minecraft) {
        String query = entryValueDraft.strip();
        if (!query.equals(lastKeybindCategorySearch)) {
            searchCollapsedKeybindCategories.clear();
            keybindCategoryExpansions.clear();
            lastKeybindCategorySearch = query;
        }
        List<KeybindCategories.Option> options = java.util.Arrays.stream(minecraft.options.keyMappings)
                .map(mapping -> new KeybindCategories.Option(mapping.getName(), Component.translatable(mapping.getName()).getString(),
                        mapping.getCategory().id().toString(), mapping.getCategory().label().getString())).toList();
        for (var option : options) if (knownKeybindCategories.add(option.categoryId())) collapsedKeybindCategories.add(option.categoryId());
        String selection = selectedProfileId + "/" + selectedWheelId + "/" + selectedEntryId;
        if (!Objects.equals(selection, keybindCategorySelection)) {
            keybindCategorySelection = selection;
            options.stream().filter(option -> option.id().equals(entryKeybindDraft)).findFirst()
                    .ifPresent(option -> collapsedKeybindCategories.remove(option.categoryId()));
        }
        var groups = KeybindCategories.group(options, query, query.isEmpty() ? collapsedKeybindCategories : searchCollapsedKeybindCategories);
        List<KeybindGroupLayout> result = new ArrayList<>();
        int top = 0;
        long now = uiAnimationFrame == 0 ? System.nanoTime() : uiAnimationFrame;
        for (var group : groups) {
            ExpansionAnimation animation = keybindCategoryExpansions.computeIfAbsent(group.id(), ignored -> new ExpansionAnimation(group.expanded()));
            animation.setExpanded(group.expanded(), now);
            float expansion = isManagerTutorialActive() ? (group.expanded() ? 1 : 0) : animation.amount(now);
            int bodyHeight = Math.round(group.options().size() * SELECTOR_ROW_STEP * expansion);
            result.add(new KeybindGroupLayout(group, top, bodyHeight, expansion));
            top += KEYBIND_CATEGORY_HEIGHT + SELECTOR_PADDING + bodyHeight;
        }
        return result;
    }

    private static int keybindContentHeight(List<KeybindGroupLayout> groups) {
        if (groups.isEmpty()) return SELECTOR_PADDING * 2;
        var last = groups.getLast();
        return SELECTOR_PADDING * 2 + last.top() + KEYBIND_CATEGORY_HEIGHT + SELECTOR_PADDING + last.bodyHeight();
    }

    private static int dataButtonWidth(Minecraft minecraft) {
        return Math.max(90, minecraft.font.width("Insert Data...") + 16);
    }

    private void addDataButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int commandIndex, boolean visible) {
        ClickAction action = new ClickAction(x, y, width, 22, !draggedCommandActive,
                () -> openDataPicker(commandIndex), "insert-data:" + commandIndex,
                commandIndex >= 0 ? "commands" : "", commandIndex >= 0 ? commandListScrollOffset : -1);
        actions.add(action);
        if (!visible) return;
        boolean highlighted = isButtonHighlighted(minecraft, x, y, width, 22, isActionFocused(action));
        drawButtonChrome(graphics, x, y, width, 22, ROW, ROW_SELECTED, action.enabled(), highlighted);
        graphics.drawCenteredString(minecraft.font, "Insert Data...", x + width / 2, y + 7, highlighted ? TEXT : ACTION);
        tooltipRegions.add(new TooltipRegion(x, y, width, 22, "Choose live information to insert at the text cursor."));
    }

    private void openDataPicker(int commandIndex) {
        TextFieldState target = commandIndex < 0 ? textFields.get(EditorField.ENTRY_VALUE) : commandValueFields.get(commandIndex);
        dataInsertCommandIndex = commandIndex;
        dataInsertUsesCommandInput = commandIndex >= 0 && commandInput != null && commandInputOwner == commandIndex
                && Objects.equals(commandInput.getValue(), target.value);
        clearFocus();
        iconPickerOpen = true;
        dataPickerOpen = true;
        pickingCustomItem = false;
        dataPickerError = "";
        iconPickerSearchDraft = "";
        textFields.get(EditorField.ICON_PICKER_SEARCH).setValue("");
        iconPickerScrollOffset = 0;
        focusField(EditorField.ICON_PICKER_SEARCH);
        activateTextEntry();
    }

    private void insertLiveData(LiveDataValues.Value value) {
        int commandIndex = dataInsertCommandIndex;
        TextFieldState target = commandIndex < 0 ? textFields.get(EditorField.ENTRY_VALUE) : commandValueFields.get(commandIndex);
        int selectedLength = dataInsertUsesCommandInput ? commandInput.getHighlighted().length() : target.selectionEnd() - target.selectionStart();
        if (target.value.length() - selectedLength + value.token().length() > target.maxLength) {
            dataPickerError = "Text is full. Go back and remove some text before inserting data.";
            return;
        }
        if (dataInsertUsesCommandInput) {
            commandInput.insertText(value.token());
            applyCommandFieldDraft(commandIndex, commandInput.getValue());
        } else {
            target.insertText(value.token());
            if (commandIndex >= 0) commandValueDrafts.set(commandIndex, target.value);
            else applyFieldDraft(EditorField.ENTRY_VALUE, target.value);
        }
        closeIconPicker();
        autosaveCurrentEntryDraft();
        if (commandIndex >= 0) focusCommandField(commandIndex); else focusField(EditorField.ENTRY_VALUE);
        activateTextEntry();
    }

    private void drawDataPicker(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        var values = LiveDataValues.search(iconPickerSearchDraft);
        var snapshot = LiveDataSnapshot.capture(minecraft);
        iconPickerX = x; iconPickerY = y; iconPickerWidth = width; iconPickerHeight = height;
        drawCompactTextFieldState(graphics, minecraft, textFields.get(EditorField.ICON_PICKER_SEARCH), x, y, width - 60, 22, "Search data...", true);
        addButton(graphics, minecraft, x + width - 52, y, 52, text("omniwheel.common.back"), 22, true, this::closeIconPicker, "data-back");
        graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font,
                dataPickerError.isEmpty() ? "Choose a value. It updates when you use the entry." : dataPickerError, width), x, y + 30,
                dataPickerError.isEmpty() ? TEXT_SECONDARY : WARNING, false);
        iconPickerGridX = x; iconPickerGridY = y + 48; iconPickerGridHeight = Math.max(0, height - 48);
        int contentHeight = Math.max(0, values.size() * DATA_PICKER_ROW_STEP - 4);
        boolean scrollbar = contentHeight > Math.max(0, iconPickerGridHeight - 16);
        iconPickerListWidth = width - (scrollbar ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
        iconPickerGridWidth = iconPickerListWidth;
        iconPickerScrollOffset = clamp(iconPickerScrollOffset, 0, maxIconPickerScrollOffset());
        updateIconPickerScrollbarBounds(contentHeight);
        graphics.fill(x, iconPickerGridY, x + iconPickerGridWidth, y + height, FIELD);
        enableUiScissor(graphics, x, iconPickerGridY, x + iconPickerGridWidth, y + height);
        float mouseX = currentMouseX(minecraft), mouseY = currentMouseY(minecraft);
        for (int index = 0; index < values.size(); index++) {
            var value = values.get(index);
            int rowX = x + 8, rowY = iconPickerGridY + 8 + index * DATA_PICKER_ROW_STEP - iconPickerScrollOffset;
            int rowWidth = iconPickerGridWidth - 16, rowHeight = DATA_PICKER_ROW_STEP - 4;
            ClickAction action = new ClickAction(rowX, rowY, rowWidth, rowHeight, true, () -> insertLiveData(value),
                    "data:" + value.id(), "icons", iconPickerScrollOffset);
            actions.add(action);
            if (rowY + rowHeight < iconPickerGridY || rowY > iconPickerGridY + iconPickerGridHeight) continue;
            boolean focused = isActionFocused(action);
            boolean hovered = action.contains(mouseX, mouseY);
            graphics.fill(rowX, rowY, rowX + rowWidth, rowY + rowHeight, hovered || focused ? ROW_SELECTED : ROW);
            graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, value.group() + " - " + value.name(), rowWidth - 12), rowX + 6, rowY + 5, TEXT, false);
            graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, value.description(), rowWidth - 12), rowX + 6, rowY + 18, TEXT_SECONDARY, false);
            String current = snapshot.values().get(value.token());
            String example = current == null ? "Example: " + value.example() + "  (" + value.missingMessage() + ")"
                    : current.isEmpty() ? "Not applicable to this item" : "Now: " + current;
            graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, example, rowWidth - 12), rowX + 6, rowY + 31, ACTION, false);
            if (focused) drawFocusedActionOutline(graphics, rowX, rowY, rowWidth, rowHeight);
        }
        if (values.isEmpty()) graphics.drawCenteredString(minecraft.font, "No matching data. Try position, UUID, block, or item.", x + iconPickerGridWidth / 2, iconPickerGridY + 20, TEXT_SECONDARY);
        graphics.disableScissor();
        graphics.fill(x, iconPickerGridY, x + iconPickerGridWidth, iconPickerGridY + 1, FIELD_EDGE);
        graphics.fill(x, y + height - 1, x + iconPickerGridWidth, y + height, FIELD_EDGE);
        graphics.fill(x, iconPickerGridY, x + 1, y + height, FIELD_EDGE);
        graphics.fill(x + iconPickerGridWidth - 1, iconPickerGridY, x + iconPickerGridWidth, y + height, FIELD_EDGE);
        drawIconPickerScrollbar(graphics, scrollbar);
    }

    private void drawIconPicker(GuiGraphics graphics, Minecraft minecraft, int editorX, int editorY, int editorWidth, int editorHeight) {
        if (dataPickerOpen) {
            drawDataPicker(graphics, minecraft, editorX, editorY, editorWidth, editorHeight);
            return;
        }
        ensureIconPickerOptions();

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
        int importMessageHeight = customIconError.isEmpty() ? 0 : 20;
        iconPickerGridY = editorY + searchFieldHeight + padding + 30 + importMessageHeight;
        iconPickerGridHeight = Math.max(0, editorHeight - searchFieldHeight - padding - 30 - importMessageHeight);

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
            contentHeight = iconCategoryContentHeight(columns);
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
                this::backFromIconPicker
        );
        int importWidth = Math.max(110, minecraft.font.width("Import PNG...") + 20);
        addButton(graphics, minecraft, editorX, editorY + 30, editorWidth - importWidth - 8,
                pickingCustomItem ? "Cancel Item Selection" : "Add from Inventory / GUI...", 22,
                minecraft.player != null && !importingCustomImage, pickingCustomItem ? this::cancelCustomItemPicker : this::startCustomItemPicker);
        addButton(graphics, minecraft, editorX + editorWidth - importWidth, editorY + 30, importWidth,
                importingCustomImage ? "Importing..." : "Import PNG...", 22, !importingCustomImage, this::importCustomImage, "import-image");


        graphics.fill(iconPickerGridX, iconPickerGridY, iconPickerGridX + iconPickerGridWidth, iconPickerGridY + iconPickerGridHeight, 0x70101820);

        String selectedIconId = normalizedIconId(entryGlyphDraft);
        float mouseX = currentMouseX(minecraft);
        float mouseY = currentMouseY(minecraft);

        enableUiScissor(graphics, iconPickerGridX, iconPickerGridY, iconPickerGridX + iconPickerGridWidth, iconPickerGridY + iconPickerGridHeight);
        for (IconCategory category : layoutIconCategories(columns)) {
            int headerX = iconPickerGridX + contentPadding;
            int headerY = iconPickerGridY + contentPadding + category.top() - iconPickerScrollOffset;
            int headerWidth = iconPickerGridWidth - contentPadding * 2;
            ClickAction header = new ClickAction(headerX, headerY, headerWidth, ICON_CATEGORY_HEIGHT, true,
                    () -> toggleIconCategory(category.id()), "icon-category:" + category.id(), "icons", iconPickerScrollOffset);
            if (!pickingCustomItem) actions.add(header);
            if (!pickingCustomItem && headerY + ICON_CATEGORY_HEIGHT >= iconPickerGridY && headerY <= iconPickerGridY + iconPickerGridHeight) {
                graphics.fill(headerX, headerY, headerX + headerWidth, headerY + ICON_CATEGORY_HEIGHT, ROW);
                GeometryRenderer.drawDisclosureChevron(graphics, headerX + 10, headerY + ICON_CATEGORY_HEIGHT / 2.0F,
                        categoryExpansions.get(category.id()).amount(uiAnimationFrame), TEXT_SECONDARY);
                graphics.drawString(Minecraft.getInstance().font, ellipsizeToWidth(Minecraft.getInstance().font, category.name() + " (" + category.options().size() + ")", headerWidth - 28), headerX + 24, headerY + 8, TEXT, false);
                if (isActionFocused(header)) drawFocusedActionOutline(graphics, headerX, headerY, headerWidth, ICON_CATEGORY_HEIGHT);
            }
            int bodyHeight = categoryBodyHeights.getOrDefault(category.id(), 0);
            if (bodyHeight <= 0) continue;
            int headerSpace = pickingCustomItem ? 0 : ICON_CATEGORY_HEIGHT + cellGap;
            int bodyTop = headerY + headerSpace;
            int bodyBottom = bodyTop + bodyHeight;
            enableUiScissor(graphics, iconPickerGridX, bodyTop, iconPickerGridX + iconPickerGridWidth, bodyBottom);
            for (int index = 0; index < category.options().size(); index++) {
                IconPickerOption option = category.options().get(index);
                int row = index / columns;
                int column = index % columns;
                int cellX = iconPickerGridX + contentPadding + horizontalInset + (column * (cellSize + cellGap));
                int cellY = iconPickerGridY + contentPadding + category.top() + headerSpace + (row * (cellSize + cellGap)) - iconPickerScrollOffset;
                if (cellY >= bodyBottom) continue;
                // Do not submit item models which are wholly outside the panel reveal.
                if (cellY >= editorRevealBottom) continue;
                ClickAction optionAction = new ClickAction(cellX, cellY, cellSize, cellSize, true,
                        () -> selectIconFromPicker(option.id()), "icon:" + option.id(), "icons", iconPickerScrollOffset);
                if (cellY + cellSize < iconPickerGridY || cellY > iconPickerGridY + iconPickerGridHeight) {
                    if (category.expanded() && cellY + cellSize <= bodyBottom) actions.add(optionAction);
                    continue;
                }

                boolean selected = option.id().equals(selectedIconId);
                boolean hovered = category.expanded() && cellY + cellSize <= bodyBottom && mouseY < editorRevealBottom
                        && mouseX >= cellX && mouseX <= cellX + cellSize && mouseY >= cellY && mouseY <= cellY + cellSize;
                boolean focused = isActionFocused(optionAction);
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
                if (hovered && CustomItemIcons.isCustom(option.id())) {
                    graphics.setTooltipForNextFrame(minecraft.font, CustomItemIcons.decode(option.id()), Math.round(mouseX), Math.round(mouseY));
                } else if (hovered && CustomImageData.isImage(option.id())) {
                    graphics.setTooltipForNextFrame(minecraft.font, Component.literal(CustomImageData.displayName(option.id())), Math.round(mouseX), Math.round(mouseY));
                }
                if (category.expanded() && cellY + cellSize <= bodyBottom) actions.add(optionAction);

            }
            graphics.disableScissor();
        }
        graphics.disableScissor();
        graphics.fill(iconPickerGridX, iconPickerGridY, iconPickerGridX + iconPickerGridWidth, iconPickerGridY + 1, FIELD_EDGE);
        graphics.fill(iconPickerGridX, iconPickerGridY + iconPickerGridHeight - 1, iconPickerGridX + iconPickerGridWidth, iconPickerGridY + iconPickerGridHeight, FIELD_EDGE);
        graphics.fill(iconPickerGridX, iconPickerGridY, iconPickerGridX + 1, iconPickerGridY + iconPickerGridHeight, FIELD_EDGE);
        graphics.fill(iconPickerGridX + iconPickerGridWidth - 1, iconPickerGridY, iconPickerGridX + iconPickerGridWidth, iconPickerGridY + iconPickerGridHeight, FIELD_EDGE);
        drawIconPickerScrollbar(graphics, needsScrollbar);
        if (pickingCustomItem && iconPickerOptions.isEmpty()) graphics.drawCenteredString(minecraft.font, "No items available. Add items to your inventory first.", editorX + editorWidth / 2, iconPickerGridY + 20, TEXT_SECONDARY);
        if (!customIconError.isEmpty()) graphics.drawString(minecraft.font, ellipsizeToWidth(minecraft.font, customIconError, editorWidth - 8), editorX + 4, iconPickerGridY - 16, WARNING, false);
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
            if (wheelEntry.showIcon()) {
                EntryIconRenderer.drawIcon(graphics, minecraft, wheelEntry.glyph(), cardX + cardWidth - 9.0F, cardY + 19.0F, 0.75F);
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
        addButton(graphics, minecraft, x, y, width, label, height, enabled, handler, "button:" + label);
    }

    private void addButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String label, int height, boolean enabled, Runnable handler, String navigationId) {
        String group = entryActionTypeDraft == DraftActionType.COMMAND && navigationId.equals("button:" + text("omniwheel.manager.add_command")) ? "commands" : "";
        ClickAction action = new ClickAction(x, y, width, height, enabled, handler, navigationId, group, group.isEmpty() ? -1 : navigationScroll(group));
        boolean focused = isActionFocused(action);
        boolean highlighted = isButtonHighlighted(minecraft, x, y, width, height, focused);
        drawButtonChrome(graphics, x, y, width, height, ROW, ROW_SELECTED, enabled, highlighted);
        graphics.drawCenteredString(minecraft.font, label, x + (width / 2), y + ((height - 8) / 2), enabled ? (highlighted ? TEXT : ACTION) : ACTION_DISABLED);
        actions.add(action);
    }

    private boolean isNavigationActionFocused(String id) {
        return keyboardButtonFocus && focusedActionKey != null && focusedActionKey.equals(NavigationList.EDITOR + ":" + id);
    }

    private boolean isActionFocused(ClickAction action) {
        return keyboardButtonFocus && focusedActionKey != null && focusedActionKey.equals(actionNavigationId(action));
    }

    private boolean isReordering() {
        return draggedProfileActive || draggedWheelActive || draggedCommandActive || radialPreview.isDragging();
    }

    private boolean isButtonHighlighted(Minecraft minecraft, int x, int y, int width, int height, boolean focused) {
        if (isManagerTutorialActive() || isReordering()) {
            return false;
        }
        return keyboardButtonFocus && focused || contains(currentMouseX(minecraft), currentMouseY(minecraft), x, y, width, height);
    }

    private void drawButtonChrome(GuiGraphics graphics, int x, int y, int width, int height, int normalFill, int highlightedFill, boolean enabled, boolean highlighted) {
        drawButtonChrome(graphics, x, y, width, height, normalFill, highlightedFill, enabled, highlighted, false);
    }

    private void drawButtonChrome(GuiGraphics graphics, int x, int y, int width, int height, int normalFill, int highlightedFill, boolean enabled, boolean highlighted, boolean tutorialControl) {
        long now = uiAnimationFrame;
        Bounds bounds = new Bounds(x, y, width, height);
        ButtonHover previous = buttonHovers.get(bounds);
        float amount = previous == null || now - previous.frame() > 250_000_000L ? 0 : previous.amount();
        double seconds = previous == null ? 0 : Math.clamp((now - previous.frame()) / 1_000_000_000.0, 0, .1);
        amount += ((enabled && highlighted ? 1 : 0) - amount) * (float) -Math.expm1(-24 * seconds);
        if (!enabled || isReordering() || isManagerTutorialActive() && !tutorialControl) amount = 0;
        if (buttonHovers.size() > 512) buttonHovers.clear();
        buttonHovers.put(bounds, new ButtonHover(amount, now));
        int fillColor = enabled ? de.artemis.omniwheel.client.render.RadialAnimation.blendColor(normalFill, highlightedFill, amount) : ROW_DISABLED;
        int edgeColor = de.artemis.omniwheel.client.render.RadialAnimation.blendColor(FIELD_EDGE, FIELD_FOCUSED, amount);
        graphics.fill(x, y, x + width, y + height, fillColor);
        graphics.fill(x, y, x + width, y + 1, edgeColor);
        graphics.fill(x, y + height - 1, x + width, y + height, edgeColor);
        graphics.fill(x, y, x + 1, y + height, edgeColor);
        graphics.fill(x + width - 1, y, x + width, y + height, edgeColor);
        if (amount > .001F) {
            int insetShade = Math.round(48 * amount) << 24;
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, insetShade);
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
            if (action.enabled && action.contains(mouseX, mouseY) && isActionMouseAccessible(action, mouseX, mouseY)) {
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
            entryPresetDraft = false;
            commandValueDrafts.clear();
            clearCommandDrag();
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
        entryPresetDraft = switch (entry.action()) {
            case ChatAction chat -> chat.preset();
            case CommandAction command -> command.preset();
            default -> false;
        };
        entryKeybindDraft = entry.action() instanceof KeybindAction keybind ? keybind.keybindId() : "key.inventory";
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
                currentWheel.active(),
                currentWheel.radialOrder()
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
            case KEYBIND -> new KeybindAction("key.inventory");
            case FUNCTION -> new FunctionAction(GameplayFunction.OPEN_INVENTORY);
            default -> new CommandAction("/command", false);
        };
        WheelEntry entry = new WheelEntry(
                nextEntryId(wheel),
                switch (actionType) {
                    case COMMAND -> text("omniwheel.manager.created.command_entry");
                    case CHAT -> text("omniwheel.manager.created.message_entry");
                    case KEYBIND -> text("omniwheel.manager.created.keybind_entry");
                    case FUNCTION -> text("omniwheel.manager.created.function_entry");
                    default -> text("omniwheel.manager.created.entry");
                },
                "",
                switch (actionType) {
                    case COMMAND -> "C";
                    case CHAT -> "M";
                    case KEYBIND -> "K";
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
                wheel.active(),
                wheel.radialOrder()
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
            case COMMAND -> new CommandAction(normalizedCommandDrafts(), false, entryPresetDraft);
            case CHAT -> new ChatAction(preservedActionValue(currentAction, ChatAction.class, entryValueDraft, text("omniwheel.manager.created.chat_value")), false, entryPresetDraft);
            case KEYBIND -> new KeybindAction(entryKeybindDraft);
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
                wheel.active(),
                wheel.radialOrder()
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
                !wheel.active(),
                wheel.radialOrder()
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
        Set<String> visited = new HashSet<>();
        java.util.ArrayDeque<String> pending = new java.util.ArrayDeque<>();
        pending.add(subtreeRootWheelId);
        while (!pending.isEmpty()) {
            String currentId = pending.removeFirst();
            if (currentId.equals(wheelId)) {
                return true;
            }
            if (visited.add(currentId) && profile.wheels().containsKey(currentId)) {
                for (WheelEntry entry : profile.wheel(currentId).entries()) {
                    if (entry.action() instanceof OpenWheelAction submenu) {
                        pending.addLast(submenu.wheelId());
                    }
                }
            }
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
                        wheel.active(),
                        wheel.radialOrder()
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
        if (CustomItemIcons.isCustom(trimmed) || CustomImageData.isImage(trimmed) || trimmed.length() <= 2 || EntryIconRenderer.isVisualIcon(trimmed) || looksLikeResourceLocationCandidate(trimmed)) {
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

    private void importCustomImage() {
        if (importingCustomImage) return;
        importingCustomImage = true;
        customIconError = "";
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
                var filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.png")).flip();
                String path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Import PNG Icon", "", filters, "PNG images", false);
                return path == null ? null : CustomImageData.read(java.nio.file.Path.of(path)).icon();
            } catch (Exception exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        }).whenComplete((icon, error) -> Minecraft.getInstance().execute(() -> finishImageImport(icon, error)));
    }

    private void finishImageImport(String icon, Throwable error) {
        importingCustomImage = false;
        if (error != null) {
            Throwable cause = error.getCause() == null ? error : error.getCause();
            customIconError = cause.getMessage() == null ? "Could not import PNG image." : cause.getMessage();
            de.artemis.omniwheel.OmniWheel.LOGGER.warn("Could not import PNG icon", cause);
            return;
        }
        if (icon == null) return;
        try {
            CustomItemIcons.save(icon);
            iconPickerResourceGeneration = -1;
            if (iconPickerOpen) {
                cancelCustomItemPicker();
                collapsedIconCategories.remove("custom");
            }
        } catch (RuntimeException exception) {
            customIconError = "Could not save image icon. See log for details.";
            de.artemis.omniwheel.OmniWheel.LOGGER.error("Could not save image icon", exception);
        }
    }

    public void captureIconSources(Screen parent) {
        guiIconSources.clear();
        if (parent instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container) {
            for (var slot : container.getMenu().slots) if (slot.hasItem()) guiIconSources.add(slot.getItem().copy());
            if (!container.getMenu().getCarried().isEmpty()) guiIconSources.add(container.getMenu().getCarried().copy());
        }
    }

    private static String customItemSearchText(ItemStack stack) {
        return (stack.getHoverName().getString() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()) + " custom inventory equipment").toLowerCase(java.util.Locale.ROOT);
    }

    private void startCustomItemPicker() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Map<String, IconPickerOption> options = new LinkedHashMap<>();
        List<ItemStack> stacks = new ArrayList<>(guiIconSources);
        var inventory = minecraft.player.getInventory();
        for (int index = 0; index < inventory.getContainerSize(); index++) stacks.add(inventory.getItem(index));
        for (var slot : minecraft.player.containerMenu.slots) stacks.add(slot.getItem());
        stacks.add(minecraft.player.containerMenu.getCarried());
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) stacks.add(minecraft.player.getItemBySlot(slot));
        stacks.add(minecraft.player.getMainHandItem());
        stacks.add(minecraft.player.getOffhandItem());
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            try {
                String id = CustomItemIcons.encode(stack);
                if (EntryIconRenderer.isUsableIcon(minecraft, id)) options.putIfAbsent(id, new IconPickerOption(id, customItemSearchText(stack)));
            } catch (RuntimeException | LinkageError exception) {
                de.artemis.omniwheel.OmniWheel.LOGGER.debug("Could not capture item icon", exception);
            }
        }
        pickingCustomItem = true;
        customIconError = "";
        collapsedIconCategories.remove("inventory");
        searchCollapsedIconCategories.clear();
        iconPickerSearchDraft = "";
        textFields.get(EditorField.ICON_PICKER_SEARCH).setValue("");
        iconPickerOptions = List.copyOf(options.values());
        iconCategories = List.of(new IconCategory("inventory", options.isEmpty() ? "No available items" : "Choose an item to copy", iconPickerOptions, 0, true));
        iconPickerScrollOffset = 0;
        clearFocus();
    }

    private void cancelCustomItemPicker() {
        pickingCustomItem = false;
        customIconError = "";
        iconPickerResourceGeneration = -1;
        iconPickerSearchDraft = "";
        textFields.get(EditorField.ICON_PICKER_SEARCH).setValue("");
        ensureIconPickerOptions();
        iconPickerScrollOffset = 0;
    }

    private void backFromIconPicker() {
        if (pickingCustomItem) cancelCustomItemPicker(); else closeIconPicker();
    }

    private void toggleIconPicker() {
        if (iconPickerOpen) {
            closeIconPicker();
            return;
        }
        pickingCustomItem = false;
        iconPickerResourceGeneration = -1;
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
        scrollIconPickerToSelection();
    }

    private void closeIconPicker() {
        if (iconPickerOpen && open && !dataPickerOpen && !isManagerTutorialActive()) {
            if (iconPickerClosingAt == 0) {
                long now = System.nanoTime();
                double progress = editorRevealStarted == 0 ? 1 : Math.clamp((now - editorRevealStarted) / 180_000_000.0, 0, 1);
                iconPickerClosingFrom = progress * progress * (3 - 2 * progress);
                iconPickerClosingAt = now;
                clearFocus();
                draggingIconPickerScrollbar = false;
            }
            return;
        }
        finishCloseIconPicker();
    }

    private void finishCloseIconPicker() {
        boolean wasOpen = iconPickerOpen;
        iconPickerClosingAt = 0;
        boolean restoreFocus = iconPickerOpen && open;
        boolean wasDataPicker = dataPickerOpen;
        if (iconPickerOpen) clearFocus();
        iconPickerOpen = false;
        dataPickerOpen = false;
        pickingCustomItem = false;
        if (restoreFocus) {
            activeList = NavigationList.EDITOR;
            focusedActionKey = NavigationList.EDITOR + (wasDataPicker ? ":insert-data:" + dataInsertCommandIndex : ":icon-picker");
        }
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
        textFields.get(EditorField.ICON_PICKER_SEARCH).setValue("");
        if (wasOpen) {
            // The outgoing browser already animated away; do not wipe the editor a second time.
            revealedEditorView = previewOpen ? "preview" : "editor";
            editorRevealStarted = 0;
            editorRevealBottom = Integer.MAX_VALUE;
        }
    }

    private boolean handleIconPickerClick(float mouseX, float mouseY, boolean doubleClick) {
        if (mouseX < iconPickerX || mouseX > iconPickerX + iconPickerWidth || mouseY < iconPickerY || mouseY > iconPickerY + iconPickerHeight) {
            return false;
        }
        TextFieldState searchField = textFields.get(EditorField.ICON_PICKER_SEARCH);
        if (searchField.contains(mouseX, mouseY)) {
            focusField(EditorField.ICON_PICKER_SEARCH);
            activateTextEntry();
            searchField.click(Minecraft.getInstance().font, mouseX);
            applyFieldDraft(EditorField.ICON_PICKER_SEARCH, searchField.value);
            return true;
        }
        if (!dataPickerOpen && !pickingCustomItem && mouseX >= iconPickerGridX && mouseX <= iconPickerGridX + iconPickerGridWidth
                && mouseY >= iconPickerGridY && mouseY <= iconPickerGridY + iconPickerGridHeight) {
            for (IconCategory category : layoutIconCategories(iconPickerColumnCount())) {
                int top = iconPickerGridY + ICON_PICKER_CONTENT_PADDING + category.top() - iconPickerScrollOffset;
                if (mouseY >= top && mouseY < top + ICON_CATEGORY_HEIGHT) {
                    if (doubleClick || mouseX < iconPickerGridX + ICON_PICKER_CONTENT_PADDING + 24) toggleIconCategory(category.id());
                    return true;
                }
            }
        }
        for (int index = actions.size() - 1; index >= 0; index--) {
            ClickAction action = actions.get(index);
            if (action.enabled && action.contains(mouseX, mouseY) && isActionMouseAccessible(action, mouseX, mouseY)) {
                action.handler.run();
                syncSelection(false);
                return true;
            }
        }
        return true;
    }

    private void ensureIconPickerOptions() {
        if (pickingCustomItem) return;
        long generation = EntryIconRenderer.resourceGeneration();
        if (iconPickerResourceGeneration != generation) {
            iconPickerOptions = List.of();
            iconPickerResourceGeneration = generation;
        }
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
            if (!EntryIconRenderer.isUsableIcon(Minecraft.getInstance(), id.toString())) {
                continue;
            }
            try {
                ItemStack stack = new ItemStack(item);
                String searchText = iconPickerItemSearchText(id, item, stack);
                options.add(new IconPickerOption(id.toString(), searchText));
            } catch (RuntimeException | LinkageError ignored) {
                // Malformed mod item components must not prevent other icons from loading.
            }
        }
        for (String custom : CustomItemIcons.saved()) {
            if (CustomImageData.isImage(custom)) {
                options.add(new IconPickerOption(custom, (CustomImageData.displayName(custom) + " custom image png").toLowerCase(java.util.Locale.ROOT)));
                continue;
            }
            ItemStack stack = CustomItemIcons.decode(custom);
            if (!stack.isEmpty()) options.add(new IconPickerOption(custom, customItemSearchText(stack)));
        }
        options.removeIf(option -> !EntryIconRenderer.isUsableIcon(Minecraft.getInstance(), option.id()));
        iconPickerOptions = List.copyOf(options);
        Map<String, List<IconPickerOption>> grouped = new LinkedHashMap<>();
        Map<String, String> names = new HashMap<>();
        names.put("custom", "Custom");
        names.put("blocks", "Blocks"); names.put("items", "Items"); names.put("extras", "Extras");
        for (String id : List.of("custom", "blocks", "items", "extras")) grouped.put(id, new ArrayList<>());
        for (IconPickerOption option : options) {
            String raw = option.id();
            if (CustomItemIcons.isCustom(raw) || CustomImageData.isImage(raw)) { grouped.get("custom").add(option); continue; }
            boolean extra = raw.startsWith("effect:") || raw.startsWith("particle:") || raw.startsWith("gui:")
                    || raw.startsWith("symbol:") || raw.startsWith("texture:");
            ResourceLocation identifier = ResourceLocation.tryParse(extra ? raw.substring(raw.indexOf(':') + 1) : raw);
            String namespace = identifier == null ? "minecraft" : identifier.getNamespace();
            String category;
            if (!namespace.equals("minecraft")) {
                var owner = net.neoforged.fml.ModList.get().getModContainerById(namespace);
                category = "mod:" + owner.map(mod -> mod.getModId()).orElse(namespace);
                names.put(category, owner.map(mod -> mod.getModInfo().getDisplayName()).orElse(namespace.replace('_', ' ')));
            } else if (extra) category = "extras";
            else category = BuiltInRegistries.ITEM.getOptional(identifier).orElse(null) instanceof net.minecraft.world.item.BlockItem ? "blocks" : "items";
            grouped.computeIfAbsent(category, ignored -> new ArrayList<>()).add(option);
        }
        List<IconCategory> categories = new ArrayList<>();
        grouped.forEach((id, icons) -> { if (!icons.isEmpty()) categories.add(new IconCategory(id, names.get(id), List.copyOf(icons), 0, false)); });
        categories.sort(Comparator.comparingInt((IconCategory category) -> switch (category.id()) {
            case "custom" -> 0; case "blocks" -> 1; case "items" -> 2; case "extras" -> 3; default -> 4;
        }).thenComparing(IconCategory::name, String.CASE_INSENSITIVE_ORDER));
        iconCategories = List.copyOf(categories);
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
            if (!EntryIconRenderer.isUsableIcon(minecraft, iconId)) {
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

    private void toggleIconCategory(String id) {
        Set<String> collapsed = iconPickerSearchDraft == null || iconPickerSearchDraft.isBlank()
                ? collapsedIconCategories : searchCollapsedIconCategories;
        if (!collapsed.remove(id)) collapsed.add(id);
        iconPickerScrollOffset = clamp(iconPickerScrollOffset, 0, maxIconPickerScrollOffset());
    }

    private List<IconCategory> layoutIconCategories(int columns) {
        String query = iconPickerSearchDraft == null ? "" : iconPickerSearchDraft.trim();
        if (!query.equals(lastIconCategorySearch)) {
            searchCollapsedIconCategories.clear();
            lastIconCategorySearch = query;
            categoryExpansions.clear();
        }
        var categories = layoutIconCategories(iconCategories, query.isEmpty() ? collapsedIconCategories : searchCollapsedIconCategories,
                query, columns, false);
        List<IconCategory> result = new ArrayList<>();
        int top = 0;
        long now = uiAnimationFrame == 0 ? System.nanoTime() : uiAnimationFrame;
        for (IconCategory category : categories) {
            ExpansionAnimation expansion = categoryExpansions.computeIfAbsent(category.id(), ignored -> new ExpansionAnimation(category.expanded()));
            expansion.setExpanded(category.expanded(), now);
            float amount = pickingCustomItem ? 1 : isManagerTutorialActive() ? (category.expanded() ? 1 : 0) : expansion.amount(now);
            int bodyHeight = Math.round(((category.options().size() + columns - 1) / columns)
                    * (ICON_PICKER_CELL_SIZE + ICON_PICKER_CELL_GAP) * amount);
            categoryBodyHeights.put(category.id(), bodyHeight);
            result.add(new IconCategory(category.id(), category.name(), category.options(), top, category.expanded()));
            top += (pickingCustomItem ? 0 : ICON_CATEGORY_HEIGHT + ICON_PICKER_CELL_GAP) + bodyHeight;
        }
        return result;
    }

    static List<IconCategory> layoutIconCategories(List<IconCategory> categories, Set<String> collapsed, String search, int columns) {
        return layoutIconCategories(categories, collapsed, search, columns, true);
    }

    static List<IconCategory> layoutIconCategories(List<IconCategory> categories, Set<String> collapsed, String search, int columns, boolean expandMatches) {
        String query = search == null ? "" : search.trim().toLowerCase(java.util.Locale.ROOT);
        String[] terms = query.split("\\s+");
        List<IconCategory> result = new ArrayList<>();
        int top = 0;
        for (IconCategory category : categories) {
            List<IconPickerOption> matches = category.options().stream().filter(option -> {
                String searchable = option.searchText() + " " + category.name().toLowerCase(java.util.Locale.ROOT);
                for (String term : terms) if (!searchable.contains(term)) return false;
                return true;
            }).toList();
            if (matches.isEmpty()) continue;
            boolean expanded = (expandMatches && !query.isEmpty()) || !collapsed.contains(category.id());
            result.add(new IconCategory(category.id(), category.name(), matches, top, expanded));
            top += ICON_CATEGORY_HEIGHT + ICON_PICKER_CELL_GAP;
            if (expanded) top += ((matches.size() + columns - 1) / columns) * (ICON_PICKER_CELL_SIZE + ICON_PICKER_CELL_GAP);
        }
        return result;
    }

    private int iconCategoryContentHeight(int columns) {
        List<IconCategory> layout = layoutIconCategories(columns);
        if (layout.isEmpty()) return 0;
        IconCategory last = layout.getLast();
        return last.top() + (pickingCustomItem ? 0 : ICON_CATEGORY_HEIGHT + ICON_PICKER_CELL_GAP) + categoryBodyHeights.getOrDefault(last.id(), 0);
    }

    private void selectIconFromPicker(String iconId) {
        if (pickingCustomItem) {
            try { CustomItemIcons.save(iconId); }
            catch (RuntimeException exception) {
                customIconError = "Could not save item icon. See log for details.";
                de.artemis.omniwheel.OmniWheel.LOGGER.error("Could not save custom icon", exception);
                return;
            }
            iconPickerResourceGeneration = -1;
        }
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
        if (dataPickerOpen) return Math.max(0, LiveDataValues.search(iconPickerSearchDraft).size() * DATA_PICKER_ROW_STEP - 4 - Math.max(0, iconPickerGridHeight - 16));
        return Math.max(0, iconCategoryContentHeight(iconPickerColumnCount())
                - Math.max(0, iconPickerGridHeight - ICON_PICKER_CONTENT_PADDING * 2));
    }

    private void scrollIconPickerToSelection() {
        iconPickerScrollOffset = 0;
        String selected = normalizedIconId(entryGlyphDraft);
        for (IconCategory category : iconCategories) {
            if (category.options().stream().anyMatch(option -> option.id().equals(selected))) collapsedIconCategories.remove(category.id());
        }
    }

    private static String normalizedIconId(String iconText) {
        String trimmed = iconText == null ? "" : iconText.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (CustomItemIcons.isCustom(trimmed) || CustomImageData.isImage(trimmed)) return trimmed;
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

    private static String actionSummary(WheelAction action) {
        return switch (action) {
            case KeybindAction keybind -> Component.translatable(keybind.keybindId()).getString();
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
            case KeybindAction ignored -> text("omniwheel.action.type.keybind");
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
        InputConstants.grabOrReleaseMouse(minecraft.getWindow(), 212993, rawX, rawY);
    }

    private static boolean isEscapeDown(Minecraft minecraft) {
        return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_ESCAPE);
    }

    private static long windowHandle(Minecraft minecraft) {
        return minecraft.getWindow().handle();
    }

    private static KeyEvent keyEvent(int keyCode, int scanCode, int modifiers) {
        return new KeyEvent(keyCode, scanCode, modifiers);
    }

    private static CharacterEvent characterEvent(char codePoint, int modifiers) {
        return new CharacterEvent(codePoint, modifiers);
    }

    private static MouseButtonEvent mouseButtonEvent(double mouseX, double mouseY, int button) {
        Minecraft minecraft = Minecraft.getInstance();
        return new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, currentModifierFlags(minecraft)));
    }

    private static int currentModifierFlags(Minecraft minecraft) {
        int modifiers = 0;
        if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            modifiers |= GLFW.GLFW_MOD_SHIFT;
        }
        if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            modifiers |= GLFW.GLFW_MOD_CONTROL;
        }
        if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT)) {
            modifiers |= GLFW.GLFW_MOD_ALT;
        }
        return modifiers;
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

        if (!draggedProfileActive && !profileRowHits.isEmpty()) {
            for (int index = 0; index < profiles.size(); index++) {
                Bounds hit = profileRowHits.get(profiles.get(index).id());
                if (hit != null && mouseY >= hit.y() && mouseY < hit.bottom()) return index;
            }
            return -1;
        }
        int index = (int) ((mouseY - profilesAreaY + profilesScrollOffset) / PROFILE_ROW_STEP);
        if (index < 0 || index >= profiles.size()) {
            return -1;
        }

        float localY = mouseY - profileRowY(index);
        return localY <= PROFILE_ROW_HEIGHT ? index : -1;
    }

    private int wheelIndexAt(float mouseX, float mouseY) {
        List<EntryListRow> rows = visibleEntryRows();
        if (mouseX < wheelsAreaX || mouseX > wheelsAreaX + wheelsListWidth || mouseY < wheelsAreaY || mouseY >= wheelsAreaY + wheelsAreaHeight) {
            return -1;
        }

        if (!draggedWheelActive && !entryRowHits.isEmpty()) {
            for (int index = 0; index < rows.size(); index++) {
                Bounds hit = entryRowHits.get(dragRowKey(rows.get(index)));
                if (hit != null && mouseY >= hit.y() && mouseY < hit.bottom()) return index;
            }
            return -1;
        }
        int index = (int) ((mouseY - wheelsAreaY + wheelsScrollOffset) / PROFILE_ROW_STEP);
        if (index < 0 || index >= rows.size()) {
            return -1;
        }

        float localY = mouseY - wheelRowY(index);
        return localY <= PROFILE_ROW_HEIGHT ? index : -1;
    }

    private EntryDropTarget computeEntryDropTarget(float mouseX, float mouseY) {
        WheelProfile profile = selectedProfile();
        if (profile == null || mouseX < wheelsAreaX || mouseX > wheelsAreaX + wheelsListWidth) {
            return null;
        }
        if (mouseY < wheelsAreaY || mouseY > wheelsAreaY + wheelsAreaHeight) {
            return null;
        }
        List<EntryListRow> rows = visibleEntryRows();
        if (rows.isEmpty()) {
            return new EntryDropTarget(profile.rootWheelId(), 0, 0, 0, text("omniwheel.manager.drag.to_root"), false);
        }
        // Reflow beneath a stationary pointer must not change its destination every frame.
        if (entryDropTarget != null && Math.abs(mouseX - lastEntryDropX) < 2
                && Math.abs(mouseY - lastEntryDropY) < 2 && lastEntryDropScroll == wheelsScrollOffset
                && lastEntryDropTop == wheelsAreaY) return entryDropTarget;
        boolean sameScroll = lastEntryDropScroll == wheelsScrollOffset;
        lastEntryDropX = mouseX;
        lastEntryDropY = mouseY;
        lastEntryDropScroll = wheelsScrollOffset;
        lastEntryDropTop = wheelsAreaY;
        int gap = entryGapSlot(rows);
        // Submenu bodies take precedence over the placeholder, including when moving
        // horizontally from a before/after position into the same submenu row.
        int submenuSlot = 0;
        for (int index = 0; index < rows.size(); index++) {
            EntryListRow candidate = rows.get(index);
            if (shouldSkipDraggedWheelRow(candidate)) continue;
            int projectedY = wheelRowY(submenuSlot + (submenuSlot >= gap ? 1 : 0));
            float y = sameScroll ? entryDragRowY.getOrDefault(dragRowKey(candidate), (float) projectedY) : projectedY;
            int indent = Math.min(candidate.depth() * 14, Math.max(0, wheelsListWidth - 90));
            if (candidate.expandable() && candidate.entry().action() instanceof OpenWheelAction submenu
                    && mouseX >= wheelsAreaX + indent + 22
                    && mouseY >= y + 6 && mouseY < y + PROFILE_ROW_HEIGHT - 6) {
                return new EntryDropTarget(submenu.wheelId(), profile.wheel(submenu.wheelId()).entries().size(),
                        index, candidate.depth() + 1,
                        text("omniwheel.manager.drag.into", displayText(candidate.entry().label())), true);
            }
            submenuSlot++;
        }
        if (validEntryDrop() && mouseY >= wheelRowY(gap) && mouseY < wheelRowY(gap) + PROFILE_ROW_STEP) return entryDropTarget;
        int rowIndex = -1;
        float nearestDistance = Float.MAX_VALUE;
        float rowTop = wheelsAreaY;
        int slot = 0;
        for (int index = 0; index < rows.size(); index++) {
            EntryListRow candidate = rows.get(index);
            if (shouldSkipDraggedWheelRow(candidate)) continue;
            int projectedY = wheelRowY(slot + (slot >= gap ? 1 : 0));
            float y = sameScroll ? entryDragRowY.getOrDefault(dragRowKey(candidate), (float) projectedY) : projectedY;
            float distance = Math.abs(mouseY - (y + PROFILE_ROW_HEIGHT / 2.0F));
            if (distance < nearestDistance) {
                rowIndex = index;
                rowTop = y;
                nearestDistance = distance;
            }
            slot++;
        }
        if (rowIndex < 0) return entryDropTarget;
        EntryListRow row = rows.get(rowIndex);
        float within = mouseY - rowTop;
        int desiredDepth = Math.max(0, (int) ((mouseX - wheelsAreaX - 6) / 14));
        int displayedIndent = Math.min(row.depth() * 14, Math.max(0, wheelsListWidth - 90));
        if (desiredDepth < row.depth() && mouseX < wheelsAreaX + displayedIndent) {
            for (int index = rowIndex - 1; index >= 0; index--) {
                if (rows.get(index).depth() <= desiredDepth) {
                    rowIndex = index;
                    row = rows.get(index);
                    within = PROFILE_ROW_HEIGHT;
                    break;
                }
            }
        }
        boolean before = within < PROFILE_ROW_HEIGHT / 2.0F;
        int boundary = rowIndex;
        if (!before) {
            boundary++;
            while (boundary < rows.size() && rows.get(boundary).depth() > row.depth()) {
                boundary++;
            }
        }
        return new EntryDropTarget(row.ownerWheelId(), siblingEntryIndex(row.ownerWheelId(), row.entry().id()) + (before ? 0 : 1),
                boundary, row.depth(), text(before ? "omniwheel.manager.drag.before" : "omniwheel.manager.drag.after", displayText(row.entry().label())), false);
    }

    private boolean validEntryDrop() {
        return entryDropTarget != null && WheelEntryMover.canMove(selectedProfile(), draggedWheelOwnerWheelId,
                draggedWheelId, entryDropTarget.wheelId());
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
        List<EntryListRow> rows = visibleEntryRows();
        int count = draggedWheelActive ? (int) rows.stream().filter(row -> !shouldSkipDraggedWheelRow(row)).count() + 1 : rows.size();
        return Math.max(0, listContentHeight(count) - wheelsAreaHeight);
    }

    private int maxCommandListScrollOffset() {
        return Math.max(0, commandListContentHeight() - commandListHeight);
    }

    private static int selectorContentHeight(int count) {
        return SELECTOR_PADDING * 2 + (count == 0 ? 0 : (count - 1) * SELECTOR_ROW_STEP + SELECTOR_ROW_HEIGHT);
    }

    private int maxFunctionListScrollOffset() {
        int contentHeight = entryActionTypeDraft == DraftActionType.KEYBIND
                ? keybindContentHeight(layoutKeybindGroups(Minecraft.getInstance())) : selectorContentHeight(selectableFunctions().size());
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

    private boolean scrollIconPickerBy(int delta) {
        return scrollIconPickerTo(iconPickerScrollOffset + delta);
    }

    private boolean scrollIconPickerTo(int targetOffset) {
        int nextOffset = clamp(targetOffset, 0, maxIconPickerScrollOffset());
        if (nextOffset == iconPickerScrollOffset) {
            return false;
        }
        iconPickerScrollOffset = nextOffset;
        return true;
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
                wheel.active(),
                wheel.radialOrder()
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
        if ((entryActionTypeDraft != DraftActionType.FUNCTION && entryActionTypeDraft != DraftActionType.KEYBIND) || maxFunctionListScrollOffset() <= 0) {
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
        draggedProfileTargetIndex = profileIndex;
        profileGrabX = mouseX - profilesAreaX;
        profileGrabY = mouseY - profileDragRowY.getOrDefault(profile.id(), (float) profileRowY(profileIndex));
        draggedProfileMouseStartX = mouseX;
        draggedProfileMouseStartY = mouseY;
        draggedProfileActive = false;
        return true;
    }

    private boolean tryStartWheelDrag(float mouseX, float mouseY, boolean doubleClick) {
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
        boolean arrowClicked = mouseX >= wheelsAreaX + 4 + Math.min(row.depth() * 14, Math.max(0, wheelsListWidth - 90))
                && mouseX <= wheelsAreaX + 22 + Math.min(row.depth() * 14, Math.max(0, wheelsListWidth - 90));
        if (row.expandable() && (arrowClicked || (doubleClick && isEntryRowSelected(row.ownerWheelId(), row.entry().id())))) {
            OpenWheelAction submenu = (OpenWheelAction) row.entry().action();
            toggleExpandedSubmenu(row.ownerWheelId(), row.entry(), submenu.wheelId());
            return true;
        }
        selectedWheelId = row.ownerWheelId();
        selectedEntryId = row.entry().id();
        loadEntryDraft(selectedEntry());
        draggedWheelId = row.entry().id();
        draggedWheelOwnerWheelId = row.ownerWheelId();
        draggedWheelSubmenuTargetId = row.expandable() && row.entry().action() instanceof OpenWheelAction openWheelAction
                ? openWheelAction.wheelId()
                : null;
        entryGrabX = mouseX - wheelsAreaX;
        entryGrabY = mouseY - entryDragRowY.getOrDefault(dragRowKey(row), (float) wheelRowY(wheelIndex));
        draggedWheelMouseStartX = mouseX;
        draggedWheelMouseStartY = mouseY;
        draggedWheelActive = false;
        return true;
    }

    private void autoScrollDrags(float mouseX, float mouseY, boolean leftPressed) {
        if (!leftPressed) {
            return;
        }
        if (draggedCommandActive && mouseX >= commandListX && mouseX <= commandListX + commandListWidth) {
            commandListScrollOffset = clamp(commandListScrollOffset + dragScrollDelta(mouseY, commandListY, commandListHeight), 0, maxCommandListScrollOffset());
        }
        if (draggedProfileActive && mouseX >= profilesAreaX && mouseX <= profilesAreaX + profilesAreaWidth) {
            profilesScrollOffset = clamp(profilesScrollOffset + dragScrollDelta(mouseY, profilesAreaY, profilesAreaHeight), 0, maxProfilesScrollOffset());
        }
        if (draggedWheelActive && mouseX >= wheelsAreaX && mouseX <= wheelsAreaX + wheelsAreaWidth) {
            wheelsScrollOffset = clamp(wheelsScrollOffset + dragScrollDelta(mouseY, wheelsAreaY, wheelsAreaHeight), 0, maxWheelsScrollOffset());
        }
    }

    private int dragScrollDelta(float mouseY, int top, int height) {
        if (height <= 0 || mouseY < top - 8 || mouseY > top + height + 8) {
            return 0;
        }
        int edge = Math.max(1, Math.min(DRAG_SCROLL_EDGE, height / 3));
        if (mouseY < top + edge) {
            return -Math.max(1, Math.round(12 * (1 - (mouseY - top) / edge)));
        }
        if (mouseY > top + height - edge) {
            return Math.max(1, Math.round(12 * (1 - (top + height - mouseY) / edge)));
        }
        return 0;
    }

    private void updateProfileDrag(float mouseX, float mouseY, boolean leftPressed) {
        if (draggedProfileId == null) {
            return;
        }
        if (!leftPressed) {
            if (draggedProfileActive) {
                updateProfileDrop(mouseX, mouseY);
            }
            finishProfileDrag();
            return;
        }
        if (!draggedProfileActive) {
            float dx = mouseX - draggedProfileMouseStartX, dy = mouseY - draggedProfileMouseStartY;
            if (dx * dx + dy * dy < PROFILE_DRAG_THRESHOLD * PROFILE_DRAG_THRESHOLD) {
                return;
            }
            draggedProfileActive = true;
        }
        updateProfileDrop(mouseX, mouseY);
    }

    private void updateProfileDrop(float mouseX, float mouseY) {
        draggedProfileDropValid = contains(mouseX, mouseY, profilesAreaX, profilesAreaY, profilesListWidth, profilesAreaHeight);
        List<WheelProfile> profiles = runtime.profileManager().getProfiles();
        draggedProfileDropValid &= profiles.stream().anyMatch(profile -> profile.id().equals(draggedProfileId));
        if (!draggedProfileDropValid) return;
        if (Math.abs(mouseX - lastProfileDropX) < 2 && Math.abs(mouseY - lastProfileDropY) < 2
                && lastProfileDropScroll == profilesScrollOffset && lastProfileDropTop == profilesAreaY) return;
        boolean sameScroll = lastProfileDropScroll == profilesScrollOffset;
        lastProfileDropX = mouseX;
        lastProfileDropY = mouseY;
        lastProfileDropScroll = profilesScrollOffset;
        lastProfileDropTop = profilesAreaY;
        int gap = profileGapIndex(profiles);
        if (mouseY >= profileRowY(gap) && mouseY < profileRowY(gap) + PROFILE_ROW_STEP) return;
        int boundary = 0;
        int slot = 0;
        for (WheelProfile profile : profiles) {
            if (profile.id().equals(draggedProfileId)) continue;
            int projectedY = profileRowY(slot + (slot >= gap ? 1 : 0));
            float y = sameScroll ? profileDragRowY.getOrDefault(profile.id(), (float) projectedY) : projectedY;
            if (mouseY > y + PROFILE_ROW_HEIGHT / 2.0F) boundary++;
            slot++;
        }
        draggedProfileTargetIndex = boundary;
    }

    private void updateWheelDrag(float mouseX, float mouseY, boolean leftPressed) {
        if (draggedWheelId == null) {
            return;
        }
        if (!leftPressed) {
            if (draggedWheelActive) {
                entryDropTarget = computeEntryDropTarget(mouseX, mouseY);
            }
            finishWheelDrag();
            return;
        }
        if (!draggedWheelActive) {
            float dx = mouseX - draggedWheelMouseStartX, dy = mouseY - draggedWheelMouseStartY;
            if (dx * dx + dy * dy < PROFILE_DRAG_THRESHOLD * PROFILE_DRAG_THRESHOLD) {
                return;
            }
            draggedWheelActive = true;
        }
        entryDropTarget = computeEntryDropTarget(mouseX, mouseY);
        String hovered = entryDropTarget != null && entryDropTarget.inside() && entryDropTarget.visibleIndex() >= 0
                && validEntryDrop() ? entryDropTarget.wheelId() : null;
        if (!Objects.equals(hovered, dragHoveredSubmenuId)) {
            dragHoveredSubmenuId = hovered;
            dragHoveredSubmenuSince = System.nanoTime();
        } else if (hovered != null && System.nanoTime() - dragHoveredSubmenuSince >= SUBMENU_HOVER_DELAY_NANOS) {
            expandedWheelIds.add(hovered);
        }
    }

    private void finishProfileDrag() {
        if (draggedProfileActive && draggedProfileDropValid && draggedProfileId != null) {
            runtime.profileManager().moveProfile(draggedProfileId, draggedProfileTargetIndex);
            selectedProfileId = draggedProfileId;
            syncSelection(true);
        }
        clearProfileDrag();
    }

    private void finishWheelDrag() {
        if (draggedWheelActive && validEntryDrop()) {
            EntryDropTarget target = entryDropTarget;
            WheelEntryMover.Result moved = WheelEntryMover.move(selectedProfile(), draggedWheelOwnerWheelId,
                    draggedWheelId, target.wheelId(), target.insertionIndex());
            persistProfile(moved.profile());
            selectedWheelId = target.wheelId();
            selectedEntryId = moved.entryId();
            if (!target.wheelId().equals(moved.profile().rootWheelId())) {
                expandedWheelIds.add(target.wheelId());
            }
            clearWheelDrag();
            clearCommandDrag();
            syncSelection(true);
            List<EntryListRow> rows = visibleEntryRows();
            for (int index = 0; index < rows.size(); index++) {
                if (isEntryRowSelected(rows.get(index).ownerWheelId(), rows.get(index).entry().id())) {
                    ensureWheelVisible(index);
                    break;
                }
            }
            return;
        }
        clearWheelDrag();
        clearCommandDrag();
    }

    private void clearProfileDrag() {
        lastProfileDropX = lastProfileDropY = Float.NaN;
        lastProfileDropScroll = lastProfileDropTop = -1;
        draggedProfileDropValid = false;
        draggedProfileId = null;
        draggedProfileTargetIndex = -1;
        draggedProfileMouseStartX = 0.0F;
        draggedProfileMouseStartY = 0.0F;
        draggedProfileActive = false;
    }

    private void clearWheelDrag() {
        radialPreview.cancelDrag();
        lastEntryDropX = lastEntryDropY = Float.NaN;
        lastEntryDropScroll = lastEntryDropTop = -1;
        entryDropTarget = null;
        dragHoveredSubmenuId = null;
        dragHoveredSubmenuSince = 0;
        draggedWheelId = null;
        draggedWheelOwnerWheelId = null;
        draggedWheelSubmenuTargetId = null;
        draggedWheelMouseStartX = 0.0F;
        draggedWheelMouseStartY = 0.0F;
        draggedWheelActive = false;
    }

    private boolean clickFocusedTextField(float mouseX, float mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entryActionTypeDraft == DraftActionType.COMMAND
                && contains(mouseX, mouseY, commandListX, commandListY, commandListWidth, commandListHeight)) {
            for (int index = 0; index < commandValueFields.size(); index++) {
                TextFieldState field = commandValueFields.get(index);
                if (field.contains(mouseX, mouseY)) {
                    focusCommandField(index);
                    activateTextEntry();
                    if (isUsingVanillaCommandInput(index)) {
                        double clampedX = Math.max(commandInput.getX(), Math.min(mouseX, commandInput.getX() + commandInput.getWidth() - 1));
                        double clampedY = Math.max(commandInput.getY(), Math.min(mouseY, commandInput.getY() + commandInput.getHeight() - 1));
                        commandInput.onClick(mouseButtonEvent(clampedX, clampedY, 0), false);
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
        long now = System.currentTimeMillis();
        for (int key : POLLED_KEYS) {
            boolean down = InputConstants.isKeyDown(minecraft.getWindow(), key);
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
        int modifiers = (isShiftModifierDown() ? GLFW.GLFW_MOD_SHIFT : 0)
                | (isControlModifierDown() ? GLFW.GLFW_MOD_CONTROL : 0);
        keyPressed(key, 0, modifiers);
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) == 0 && textEntryActive && focusedField != EditorField.ENTRY_SHORTCUT) {
            Character typed = typedCharacterForKey(key, isShiftModifierDown());
            if (typed != null) charTyped(typed, modifiers);
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
            case ENTRY_VALUE -> {
                entryValueDraft = value;
                if (entryActionTypeDraft == DraftActionType.KEYBIND) {
                    functionListScrollOffset = 0;
                }
            }
        }
    }

    private void focusField(EditorField fieldId) {
        if (focusedField == EditorField.PROFILE_NAME && fieldId != EditorField.PROFILE_NAME) {
            commitProfileRename();
        }
        if (focusedField == EditorField.WHEEL_TITLE && fieldId != EditorField.WHEEL_TITLE) {
            commitWheelRename();
        }
        persistFocusedEntryDraft();
        clearFocus();
        activeList = navigationListForField(fieldId);
        focusedField = fieldId;
        textFields.get(fieldId).focused = true;
    }

    private void focusCommandField(int index) {
        persistFocusedEntryDraft();
        clearFocus();
        activeList = NavigationList.EDITOR;
        focusedCommandFieldIndex = index;
        commandValueFields.get(index).focused = true;
        ensureCommandFieldVisible(index);
        syncActiveCommandInput();
    }

    private void activateTextEntry() {
        textEntryActive = true;
        if (focusedCommandFieldIndex >= 0 && ensureCommandInput()) {
            commandInput.setVisible(true);
            commandInput.setFocused(true);
        }
    }

    private void deactivateTextEntry() {
        applyFocusedFieldDraft();
        if (focusedField == EditorField.PROFILE_NAME) {
            commitProfileRename();
        } else if (focusedField == EditorField.WHEEL_TITLE) {
            commitWheelRename();
        } else {
            persistFocusedEntryDraft();
        }
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
        focusedActionKey = null;
        if (focusedField == EditorField.PROFILE_NAME) {
            commitProfileRename();
        } else if (focusedField == EditorField.WHEEL_TITLE) {
            commitWheelRename();
        } else {
            persistFocusedEntryDraft();
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
        focusedActionIndex = -1;
        focusedActionKey = null;
        textEntryActive = false;
    }

    private TextFieldState currentFocusedField() {
        if (focusedCommandFieldIndex >= 0 && focusedCommandFieldIndex < commandValueFields.size()) {
            return commandValueFields.get(focusedCommandFieldIndex);
        }
        return focusedField == null ? null : textFields.get(focusedField);
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
        return navigationControls().stream().filter(control -> control.target() != null && control.node().panel() == zone.ordinal())
                .sorted(Comparator.comparingDouble(control -> control.node().order())).map(NavigationControl::target).toList();
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
        persistFocusedEntryDraft();
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
        textEntryActive = false;
        focusedActionKey = actionNavigationId(actions.get(index));
        ensureNavigationVisible(actions.get(index));
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

    private int iconPickerColumnCount() {
        int usableGridWidth = Math.max(0, iconPickerGridWidth - (ICON_PICKER_CONTENT_PADDING * 2));
        return Math.max(1, (usableGridWidth + ICON_PICKER_CELL_GAP) / (ICON_PICKER_CELL_SIZE + ICON_PICKER_CELL_GAP));
    }

    private boolean handleCommandFieldCharTyped(char codePoint, int modifiers) {
        return textEntryActive
                && focusedCommandFieldIndex >= 0
                && ensureCommandInput()
                && commandInput.charTyped(characterEvent(codePoint, modifiers));
    }

    private boolean handleCommandSuggestionsClick(double mouseX, double mouseY, int button) {
        return textEntryActive && focusedCommandFieldIndex >= 0
                && ensureCommandInput()
                && commandSuggestions.mouseClicked(mouseButtonEvent(mouseX, mouseY - commandSuggestionsYOffset, button));
    }

    private boolean handleCommandSuggestionsScroll(double scrollY) {
        return textEntryActive && focusedCommandFieldIndex >= 0 && ensureCommandInput() && commandSuggestions.mouseScrolled(scrollY);
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

    private boolean isActionAtBounds(int actionIndex, int x, int y, int width, int height) {
        if (actionIndex < 0 || actionIndex >= actions.size()) {
            return false;
        }
        ClickAction action = actions.get(actionIndex);
        return action.x == x && action.y == y && action.width == width && action.height == height;
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

    private NavigationList navigationListForField(EditorField fieldId) {
        return switch (fieldId) {
            case PROFILE_NAME -> NavigationList.PROFILES;
            case WHEEL_TITLE -> NavigationList.WHEELS;
            case ICON_PICKER_SEARCH, ENTRY_LABEL, ENTRY_DESCRIPTION, ENTRY_SHORTCUT, ENTRY_GLYPH, ENTRY_VALUE -> NavigationList.EDITOR;
        };
    }

    private NavigationList navigationListForAction(ClickAction action) {
        if (action.x >= profilesPanelX && action.x + action.width <= profilesPanelX + profilesPanelWidth) return NavigationList.PROFILES;
        if (action.x >= wheelsPanelX && action.x + action.width <= wheelsPanelX + wheelsPanelWidth) return NavigationList.WHEELS;
        return NavigationList.EDITOR;
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
            commandInput.addFormatter((value, offset) -> LiveDataValues.contains(commandInput.getValue())
                    ? FormattedCharSequence.forward(value, net.minecraft.network.chat.Style.EMPTY) : null);
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
        commandInputOwner = focusedCommandFieldIndex;
        String value = commandValueDrafts.get(focusedCommandFieldIndex);
        syncingCommandInput = true;
        if (!Objects.equals(commandInput.getValue(), value)) {
            commandInput.setValue(value);
        }
        syncingCommandInput = false;
        if (commandSuggestions != null) {
            commandSuggestions.setAllowSuggestions(!LiveDataValues.contains(commandInput.getValue()));
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
        commandInputOwner = -1;
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
        for (KeyMapping keyMapping : gameplayKeyMappings(minecraft)) {
            InputConstants.Key key = OmniWheelKeyMappings.currentKey(keyMapping);
            if (key.getType() == InputConstants.Type.KEYSYM) {
                keyMapping.setDown(InputConstants.isKeyDown(minecraft.getWindow(), key.getValue()));
            }
        }
    }

    private void persistFocusedEntryDraft() {
        if (focusedField == EditorField.ENTRY_LABEL
                || focusedField == EditorField.ENTRY_DESCRIPTION
                || focusedField == EditorField.ENTRY_VALUE
                || focusedCommandFieldIndex >= 0) {
            WheelEntry entry = selectedEntry();
            if (entry != null && isEntryDraftDirty(entry)) {
                saveCurrentEntry();
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
        return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private boolean isControlModifierDown() {
        Minecraft minecraft = Minecraft.getInstance();
        return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
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
        KEYBIND,
        LOCAL_MESSAGE,
        OPEN_CHAT,
        COPY_TEXT,
        OPEN_SCREEN,
        SUBMENU;

        private DraftActionType next() {
            return switch (this) {
                case COMMAND -> CHAT;
                case CHAT -> FUNCTION;
                case FUNCTION -> KEYBIND;
                case KEYBIND -> SUBMENU;
                case LOCAL_MESSAGE, OPEN_CHAT, COPY_TEXT, OPEN_SCREEN, SUBMENU -> COMMAND;
            };
        }

        private static DraftActionType fromAction(WheelAction action) {
            return switch (action) {
                case CommandAction ignored -> COMMAND;
                case ChatAction ignored -> CHAT;
                case KeybindAction ignored -> KEYBIND;
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

    private record ClickAction(int x, int y, int width, int height, boolean enabled, Runnable handler, String navigationId, String navigationGroup, int scrollOffset) {
        private ClickAction(int x, int y, int width, int height, boolean enabled, Runnable handler) {
            this(x, y, width, height, enabled, handler, null, "", -1);
        }
        private ClickAction(int x, int y, int width, int height, boolean enabled, Runnable handler, String navigationId, String navigationGroup) {
            this(x, y, width, height, enabled, handler, navigationId, navigationGroup, -1);
        }
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

    private record EntryDropTarget(String wheelId, int insertionIndex, int visibleIndex, int depth, String label, boolean inside) {
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


    record IconCategory(String id, String name, List<IconPickerOption> options, int top, boolean expanded) {}

    record IconPickerOption(String id, String searchText) {
    }
}
