package de.artemis.omniwheel.client.runtime;

import de.artemis.omniwheel.client.overlay.OmniWheelOverlay;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

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

    public GameplayFunctionController functionController() {
        return functionController;
    }

    public WheelAvailabilityResolver availabilityResolver() {
        return availabilityResolver;
    }

    public TutorialProgressStore tutorialProgressStore() {
        return tutorialProgressStore;
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
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
        switch (target) {
            case PROFILE_MANAGER -> minecraft.setScreen(new OmniWheelProfilesScreen(this, parent));
        }
    }

    private void processEntryShortcuts(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null || isOverlayUiOpen()) {
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
            if (!entry.shortcut().isBlank() && availabilityResolver.isVisible(profile, entry, new ArrayDeque<>(path))) {
                List<String> openPath = null;
                if (entry.action() instanceof OpenWheelAction openWheelAction && profile.wheels().containsKey(openWheelAction.wheelId())) {
                    openPath = new ArrayList<>(path);
                    openPath.add(openWheelAction.wheelId());
                }
                candidates.add(new ShortcutCandidate(wheelId + ":" + entry.id(), entry, openPath));
            }

            if (entry.action() instanceof OpenWheelAction openWheelAction
                    && profile.wheels().containsKey(openWheelAction.wheelId())
                    && !path.contains(openWheelAction.wheelId())) {
                path.addLast(openWheelAction.wheelId());
                collectShortcutCandidates(profile, openWheelAction.wheelId(), path, new HashSet<>(activePath), candidates);
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
        if (entry.action() instanceof OpenScreenAction openScreenAction) {
            openTargetScreen(openScreenAction.target(), null);
            return;
        }
        if (entry.action() instanceof FunctionAction functionAction && handleOmniWheelShortcutFunction(functionAction.function())) {
            return;
        }
        actionExecutor.execute(entry.action());
    }

    private boolean handleOmniWheelShortcutFunction(GameplayFunction function) {
        switch (function) {
            case OMNI_OPEN_PROFILES -> {
                overlay.openWheelPath(List.of(WheelAvailabilityResolver.PROFILES_WHEEL_ID));
                return true;
            }
            case OMNI_OPEN_MANAGER -> {
                openTargetScreen(ScreenTarget.PROFILE_MANAGER, null);
                return true;
            }
            case OMNI_NEXT_PROFILE -> {
                cycleActiveProfile(1);
                return true;
            }
            case OMNI_PREVIOUS_PROFILE -> {
                cycleActiveProfile(-1);
                return true;
            }
            case OMNI_BACK -> {
                return true;
            }
            default -> {
                return false;
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

    private record ShortcutCandidate(String id, WheelEntry entry, List<String> openPath) {
    }
}
