package de.artemis.omniwheel.client.runtime;

import de.artemis.omniwheel.client.overlay.OmniWheelOverlay;
import de.artemis.omniwheel.client.input.ShortcutInputState;
import de.artemis.omniwheel.client.overlay.OmniWheelProfilesOverlay;
import de.artemis.omniwheel.client.profile.OmniWheelProfileManager;
import de.artemis.omniwheel.client.screen.OmniWheelProfilesScreen;
import de.artemis.omniwheel.client.tutorial.TutorialProgressStore;
import de.artemis.omniwheel.common.action.FunctionAction;
import de.artemis.omniwheel.common.action.GameplayFunction;
import de.artemis.omniwheel.common.action.OpenScreenAction;
import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.action.ScreenTarget;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import net.minecraft.client.Minecraft;
import de.artemis.omniwheel.client.render.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OmniWheelClientRuntime {

    private static final OmniWheelClientRuntime INSTANCE = new OmniWheelClientRuntime();

    private final OmniWheelProfileManager profileManager = new OmniWheelProfileManager();

    private final GameplayFunctionController functionController = new GameplayFunctionController();

    private final KeybindActionController keybindController = new KeybindActionController();

    private final ClientActionExecutor actionExecutor = new ClientActionExecutor();

    private final WheelAvailabilityResolver availabilityResolver = new WheelAvailabilityResolver();

    private final OmniWheelOverlay overlay = new OmniWheelOverlay(this);

    private final OmniWheelProfilesOverlay profilesOverlay = new OmniWheelProfilesOverlay(this);

    private final TutorialProgressStore tutorialProgressStore = new TutorialProgressStore();

    private final Set<String> activeShortcutEntries = new HashSet<>();

    private OmniWheelClientRuntime() {
    }

    public static OmniWheelClientRuntime getInstance() {
        return INSTANCE;
    }

    public WheelProfile activeProfile() {
        return profileManager.getActiveProfile();
    }

    public OmniWheelProfileManager profileManager() {
        return profileManager;
    }

    public ClientActionExecutor actionExecutor() {
        return actionExecutor;
    }

    public KeybindActionController keybindController() {
        return keybindController;
    }

    public GameplayFunctionController functionController() {
        return functionController;
    }

    public WheelAvailabilityResolver availabilityResolver() {
        return availabilityResolver;
    }

    public TutorialProgressStore tutorialProgressStore() {
        return tutorialProgressStore;
    }

    public boolean handleTutorialKey(int key, int modifiers, int action) {
        return overlay.handleTutorialKey(key, modifiers, action);
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isWindowActive()) {
            ShortcutInputState.clear();
            activeShortcutEntries.clear();
        }
        boolean profilesReloaded = profileManager.pollExternalChanges();
        if (profilesReloaded && profilesOverlay.isOpen()) {
            profilesOverlay.onProfilesReloaded();
        }
        functionController.tick();
        processEntryShortcuts(minecraft);
        if (profilesOverlay.isOpen()) {
            profilesOverlay.tick();
            return;
        }
        overlay.tick();
    }

    public void renderOverlay(GuiGraphics graphics) {
        overlay.render(graphics);
        profilesOverlay.render(graphics);
    }

    public boolean isWheelOpen() {
        return overlay.isOpen();
    }

    public boolean isOverlayUiOpen() {
        return overlay.isOpen() || profilesOverlay.isOpen();
    }

    public boolean handleMouseScroll(double scrollDeltaX, double scrollDeltaY) {
        return overlay.handleMouseScroll(scrollDeltaX, scrollDeltaY);
    }

    public boolean handleMouseButton(int button, int action) {
        return overlay.handleMouseButton(button, action);
    }

    public void openWheelShortcutPath(List<String> wheelPath) {
        overlay.openWheelPath(wheelPath);
    }

    public void openProfileManagerShortcut() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof OmniWheelProfilesScreen) {
            return;
        }
        if (minecraft.screen != null) {
            return;
        }
        openTargetScreen(ScreenTarget.PROFILE_MANAGER, null);
    }

    public void openTargetScreen(ScreenTarget target, Screen parent) {
        profileManager.ensureLoaded();
        Minecraft minecraft = Minecraft.getInstance();
        overlay.close();
        switch(target) {
            case PROFILE_MANAGER:
                {
                    minecraft.setScreen(new OmniWheelProfilesScreen(this, parent));
                    break;
                }
        }
    }

    private void processEntryShortcuts(Minecraft minecraft) {
        if (!minecraft.isWindowActive() || minecraft.player == null || minecraft.level == null || minecraft.screen != null || isOverlayUiOpen()) {
            activeShortcutEntries.clear();
            return;
        }
        profileManager.ensureLoaded();
        WheelProfile profile = activeProfile();
        if (profile == null) {
            activeShortcutEntries.clear();
            return;
        }
        List<ShortcutCandidate> candidates = collectShortcutCandidates(profile);
        Set<String> downShortcutIds = new HashSet<>();
        ShortcutCandidate nextTrigger = null;
        int nextTriggerWeight = -1;
        for (ShortcutCandidate candidate : candidates) {
            EntryShortcutMatcher.ShortcutSpec shortcut = EntryShortcutMatcher.parse(candidate.entry().shortcut());
            if (shortcut == null || !shortcut.matches(minecraft.getWindow())) {
                continue;
            }
            downShortcutIds.add(candidate.id());
            if (activeShortcutEntries.contains(candidate.id())) {
                continue;
            }
            int weight = shortcut.weight();
            if (nextTrigger == null || weight > nextTriggerWeight) {
                nextTrigger = candidate;
                nextTriggerWeight = weight;
            }
        }
        activeShortcutEntries.clear();
        activeShortcutEntries.addAll(downShortcutIds);
        if (nextTrigger != null) {
            executeShortcutCandidate(nextTrigger);
        }
    }

    /**
     * Process edges as well as ticks, so a quick macro tap cannot disappear between ticks.
     */
    public void shortcutInputChanged(int action) {
        if (action != GLFW.GLFW_REPEAT)
            processEntryShortcuts(Minecraft.getInstance());
    }

    private List<ShortcutCandidate> collectShortcutCandidates(WheelProfile profile) {
        List<ShortcutCandidate> candidates = new ArrayList<>();
        String rootWheelId = profile.rootWheelId();
        if (!profile.wheels().containsKey(rootWheelId)) {
            return candidates;
        }
        Deque<String> rootPath = new ArrayDeque<>();
        rootPath.addLast(rootWheelId);
        collectShortcutCandidates(profile, rootWheelId, rootPath, new HashSet<>(), candidates);
        return candidates;
    }

    private void collectShortcutCandidates(WheelProfile profile, String wheelId, Deque<String> path, Set<String> activePath, List<ShortcutCandidate> candidates) {
        if (!activePath.add(wheelId) || !profile.wheels().containsKey(wheelId)) {
            return;
        }
        WheelDefinition wheel = profile.wheel(wheelId);
        if (!wheel.active()) {
            return;
        }
        for (WheelEntry entry : wheel.entries()) {
            if (!de.artemis.omniwheel.common.Java8.isBlank(entry.shortcut()) && availabilityResolver.canActivate(profile, wheelId, entry, new ArrayDeque<>(path))) {
                List<String> openPath = null;
                if (entry.action() instanceof OpenWheelAction && profile.wheels().containsKey(((OpenWheelAction) (entry.action())).wheelId())) {
                    openPath = new ArrayList<>(path);
                    openPath.add(((OpenWheelAction) (entry.action())).wheelId());
                }
                candidates.add(new ShortcutCandidate(wheelId + ":" + entry.id(), entry, openPath));
            }
            if (entry.action() instanceof OpenWheelAction && profile.wheels().containsKey(((OpenWheelAction) (entry.action())).wheelId()) && !path.contains(((OpenWheelAction) (entry.action())).wheelId())) {
                path.addLast(((OpenWheelAction) (entry.action())).wheelId());
                collectShortcutCandidates(profile, ((OpenWheelAction) (entry.action())).wheelId(), path, new HashSet<>(activePath), candidates);
                path.removeLast();
            }
        }
    }

    private void executeShortcutCandidate(ShortcutCandidate candidate) {
        WheelEntry entry = candidate.entry();
        if (entry.action() instanceof OpenWheelAction) {
            if (candidate.openPath() != null && !candidate.openPath().isEmpty()) {
                overlay.openWheelPath(candidate.openPath());
            }
            return;
        }
        if (entry.action() instanceof OpenScreenAction) {
            openTargetScreen(((OpenScreenAction) (entry.action())).target(), null);
            return;
        }
        if (entry.action() instanceof FunctionAction && handleOmniWheelShortcutFunction(((FunctionAction) (entry.action())).function())) {
            return;
        }
        actionExecutor.execute(entry.action());
    }

    private boolean handleOmniWheelShortcutFunction(GameplayFunction function) {
        switch(function) {
            case OMNI_OPEN_PROFILES:
                {
                    {
                        overlay.openWheelPath(de.artemis.omniwheel.common.Java8.list(WheelAvailabilityResolver.PROFILES_WHEEL_ID));
                        return true;
                    }
                }
            case OMNI_OPEN_MANAGER:
                {
                    {
                        openTargetScreen(ScreenTarget.PROFILE_MANAGER, null);
                        return true;
                    }
                }
            case OMNI_NEXT_PROFILE:
                {
                    {
                        cycleActiveProfile(1);
                        return true;
                    }
                }
            case OMNI_PREVIOUS_PROFILE:
                {
                    {
                        cycleActiveProfile(-1);
                        return true;
                    }
                }
            case OMNI_BACK:
                {
                    {
                        return true;
                    }
                }
            default:
                {
                    {
                        return false;
                    }
                }
        }
    }

    private void cycleActiveProfile(int delta) {
        profileManager.ensureLoaded();
        List<WheelProfile> profiles = profileManager.getProfiles();
        if (profiles.size() <= 1) {
            return;
        }
        String activeProfileId = activeProfile().id();
        int currentIndex = 0;
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).id().equals(activeProfileId)) {
                currentIndex = index;
                break;
            }
        }
        profileManager.setActiveProfile(profiles.get(Math.floorMod(currentIndex + delta, profiles.size())).id());
    }

    private final static class ShortcutCandidate {

        private final String id;

        private final WheelEntry entry;

        private final List<String> openPath;

        private ShortcutCandidate(String id, WheelEntry entry, List<String> openPath) {
            this.id = id;
            this.entry = entry;
            this.openPath = openPath;
        }

        public String id() {
            return id;
        }

        public WheelEntry entry() {
            return entry;
        }

        public List<String> openPath() {
            return openPath;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value)
                return true;
            if (value == null || value.getClass() != getClass())
                return false;
            ShortcutCandidate other = (ShortcutCandidate) value;
            return true && java.util.Objects.equals(id, other.id) && java.util.Objects.equals(entry, other.entry) && java.util.Objects.equals(openPath, other.openPath);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, entry, openPath);
        }

        @Override
        public String toString() {
            return "ShortcutCandidate[" + "id=" + id + ", entry=" + entry + ", openPath=" + openPath + "]";
        }
    }
}
