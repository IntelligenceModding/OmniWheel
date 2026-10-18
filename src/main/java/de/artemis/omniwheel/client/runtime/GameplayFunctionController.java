package de.artemis.omniwheel.client.runtime;

import net.minecraft.client.util.InputMappings;
import de.artemis.omniwheel.client.input.OmniWheelKeyMappings;
import de.artemis.omniwheel.common.OmniWheelText;
import de.artemis.omniwheel.common.action.GameplayFunction;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.social.SocialInteractionsScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerInventory;
import org.lwjgl.glfw.GLFW;
import de.artemis.omniwheel.mixin.client.MinecraftAccessor;

public final class GameplayFunctionController {

    private boolean sneakLock;

    private boolean sprintLock;

    private boolean autoWalk;

    private boolean forcedSneakApplied;

    private boolean forcedSprintApplied;

    private boolean forcedWalkApplied;

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            clearLatchedStates(minecraft);
            return;
        }
        applyForcedKey(minecraft, minecraft.options.keyShift, sneakLock, ForcedKey.SNEAK);
        applyForcedKey(minecraft, minecraft.options.keySprint, sprintLock, ForcedKey.SPRINT);
        applyForcedKey(minecraft, minecraft.options.keyUp, autoWalk, ForcedKey.WALK);
    }

    public void execute(GameplayFunction function) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        switch(function) {
            case OMNI_BACK:
            case OMNI_OPEN_PROFILES:
                {
                    {
                    }
                    break;
                }
            case OMNI_OPEN_MANAGER:
                {
                    OmniWheelClientRuntime.getInstance().openTargetScreen(de.artemis.omniwheel.common.action.ScreenTarget.PROFILE_MANAGER, null);
                    break;
                }
            case OMNI_NEXT_PROFILE:
                {
                    cycleProfile(1);
                    break;
                }
            case OMNI_PREVIOUS_PROFILE:
                {
                    cycleProfile(-1);
                    break;
                }
            case OPEN_INVENTORY:
                {
                    openInventory(minecraft);
                    break;
                }
            case OPEN_CHAT:
                {
                    minecraft.setScreen(new ChatScreen(""));
                    break;
                }
            case OPEN_COMMAND_CHAT:
                {
                    minecraft.setScreen(new ChatScreen("/"));
                    break;
                }
            case OPEN_ADVANCEMENTS:
                {
                    openAdvancements(minecraft);
                    break;
                }
            case OPEN_SOCIAL_INTERACTIONS:
                {
                    openSocialInteractions(minecraft);
                    break;
                }
            case ATTACK:
                {
                    ((MinecraftAccessor) minecraft).omniwheel$startAttack();
                    break;
                }
            case USE_ITEM:
                {
                    ((MinecraftAccessor) minecraft).omniwheel$startUseItem();
                    break;
                }
            case PICK_BLOCK:
                {
                    ((MinecraftAccessor) minecraft).omniwheel$pickBlock();
                    break;
                }
            case DROP_ITEM:
                {
                    dropHeldItem(minecraft);
                    break;
                }
            case SWAP_OFFHAND:
                {
                    swapOffhand(minecraft);
                    break;
                }
            case JUMP:
                {
                    jump(minecraft);
                    break;
                }
            case HOTBAR_SLOT_1:
                {
                    setSelectedHotbarSlot(minecraft.player.inventory, 0);
                    break;
                }
            case HOTBAR_SLOT_2:
                {
                    setSelectedHotbarSlot(minecraft.player.inventory, 1);
                    break;
                }
            case HOTBAR_SLOT_3:
                {
                    setSelectedHotbarSlot(minecraft.player.inventory, 2);
                    break;
                }
            case HOTBAR_SLOT_4:
                {
                    setSelectedHotbarSlot(minecraft.player.inventory, 3);
                    break;
                }
            case HOTBAR_SLOT_5:
                {
                    setSelectedHotbarSlot(minecraft.player.inventory, 4);
                    break;
                }
            case HOTBAR_SLOT_6:
                {
                    setSelectedHotbarSlot(minecraft.player.inventory, 5);
                    break;
                }
            case HOTBAR_SLOT_7:
                {
                    setSelectedHotbarSlot(minecraft.player.inventory, 6);
                    break;
                }
            case HOTBAR_SLOT_8:
                {
                    setSelectedHotbarSlot(minecraft.player.inventory, 7);
                    break;
                }
            case HOTBAR_SLOT_9:
                {
                    setSelectedHotbarSlot(minecraft.player.inventory, 8);
                    break;
                }
            case HOTBAR_NEXT:
                {
                    cycleHotbarSlot(minecraft.player.inventory, 1);
                    break;
                }
            case HOTBAR_PREVIOUS:
                {
                    cycleHotbarSlot(minecraft.player.inventory, -1);
                    break;
                }
            case TOGGLE_PERSPECTIVE:
                {
                    togglePerspective(minecraft);
                    break;
                }
            case TAKE_SCREENSHOT:
                {
                    takeScreenshot(minecraft);
                    break;
                }
            case TOGGLE_FULLSCREEN:
                {
                    toggleFullscreen(minecraft);
                    break;
                }
            case TOGGLE_SMOOTH_CAMERA:
                {
                    toggleSmoothCamera(minecraft);
                    break;
                }
            case TOGGLE_SNEAK_LOCK:
                {
                    toggleSneakLock(minecraft);
                    break;
                }
            case TOGGLE_SPRINT_LOCK:
                {
                    toggleSprintLock(minecraft);
                    break;
                }
            case TOGGLE_AUTO_WALK:
                {
                    toggleAutoWalk(minecraft);
                    break;
                }
        }
    }

    public boolean isAvailable(GameplayFunction function) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }
        if (java.util.Objects.equals(function, de.artemis.omniwheel.common.action.GameplayFunction.OMNI_BACK) || java.util.Objects.equals(function, de.artemis.omniwheel.common.action.GameplayFunction.OMNI_OPEN_PROFILES) || java.util.Objects.equals(function, de.artemis.omniwheel.common.action.GameplayFunction.OMNI_OPEN_MANAGER)) {
            return true;
        } else {
            if (java.util.Objects.equals(function, de.artemis.omniwheel.common.action.GameplayFunction.OMNI_NEXT_PROFILE) || java.util.Objects.equals(function, de.artemis.omniwheel.common.action.GameplayFunction.OMNI_PREVIOUS_PROFILE)) {
                return OmniWheelClientRuntime.getInstance().profileManager().getProfiles().size() > 1;
            } else {
                if (java.util.Objects.equals(function, de.artemis.omniwheel.common.action.GameplayFunction.OPEN_ADVANCEMENTS)) {
                    return minecraft.player.connection != null;
                } else {
                    if (java.util.Objects.equals(function, de.artemis.omniwheel.common.action.GameplayFunction.OPEN_SOCIAL_INTERACTIONS)) {
                        return isSocialInteractionsAvailable(minecraft);
                    } else {
                        if (java.util.Objects.equals(function, de.artemis.omniwheel.common.action.GameplayFunction.DROP_ITEM) || java.util.Objects.equals(function, de.artemis.omniwheel.common.action.GameplayFunction.SWAP_OFFHAND)) {
                            return !minecraft.player.isSpectator();
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
    }

    private void toggleSneakLock(Minecraft minecraft) {
        sneakLock = !sneakLock;
        if (!sneakLock) {
            releaseForcedKey(minecraft, minecraft.options.keyShift, ForcedKey.SNEAK);
        }
        notifyToggle(minecraft, GameplayFunction.TOGGLE_SNEAK_LOCK, sneakLock);
    }

    private void toggleSprintLock(Minecraft minecraft) {
        sprintLock = !sprintLock;
        if (!sprintLock) {
            releaseForcedKey(minecraft, minecraft.options.keySprint, ForcedKey.SPRINT);
        }
        notifyToggle(minecraft, GameplayFunction.TOGGLE_SPRINT_LOCK, sprintLock);
    }

    private void toggleAutoWalk(Minecraft minecraft) {
        autoWalk = !autoWalk;
        if (!autoWalk) {
            releaseForcedKey(minecraft, minecraft.options.keyUp, ForcedKey.WALK);
        }
        notifyToggle(minecraft, GameplayFunction.TOGGLE_AUTO_WALK, autoWalk);
    }

    private void notifyToggle(Minecraft minecraft, GameplayFunction function, boolean enabled) {
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(OmniWheelText.component("omniwheel.message.toggle_state", function.displayName(), OmniWheelText.translate(enabled ? "omniwheel.common.on" : "omniwheel.common.off")), true);
        }
    }

    private void clearLatchedStates(Minecraft minecraft) {
        sneakLock = false;
        sprintLock = false;
        autoWalk = false;
        releaseForcedKey(minecraft, minecraft.options.keyShift, ForcedKey.SNEAK);
        releaseForcedKey(minecraft, minecraft.options.keySprint, ForcedKey.SPRINT);
        releaseForcedKey(minecraft, minecraft.options.keyUp, ForcedKey.WALK);
    }

    private void applyForcedKey(Minecraft minecraft, KeyBinding keyMapping, boolean enabled, ForcedKey forcedKey) {
        if (enabled) {
            keyMapping.setDown(true);
            markForced(forcedKey, true);
        } else {
            releaseForcedKey(minecraft, keyMapping, forcedKey);
        }
    }

    private void releaseForcedKey(Minecraft minecraft, KeyBinding keyMapping, ForcedKey forcedKey) {
        if (!isForced(forcedKey)) {
            return;
        }
        keyMapping.setDown(isPhysicalBindingDown(minecraft, keyMapping));
        markForced(forcedKey, false);
    }

    private static void openInventory(Minecraft minecraft) {
        if (minecraft.screen != null) {
            return;
        }
        if (minecraft.gameMode != null && minecraft.gameMode.isServerControlledInventory()) {
            minecraft.player.sendOpenInventory();
            return;
        }
        minecraft.getTutorial().onOpenInventory();
        minecraft.setScreen(new InventoryScreen(minecraft.player));
    }

    private static void openAdvancements(Minecraft minecraft) {
        if (minecraft.player.connection == null) {
            return;
        }
        minecraft.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements()));
    }

    private static void openSocialInteractions(Minecraft minecraft) {
        if (!isSocialInteractionsAvailable(minecraft)) {
            minecraft.player.displayClientMessage(new net.minecraft.util.text.TranslationTextComponent("multiplayer.socialInteractions.not_available"), true);
            return;
        }
        minecraft.setScreen(new SocialInteractionsScreen());
    }

    private static boolean isSocialInteractionsAvailable(Minecraft minecraft) {
        return !minecraft.isLocalServer() || minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null && minecraft.getSingleplayerServer().isPublished();
    }

    private static void dropHeldItem(Minecraft minecraft) {
        if (!minecraft.player.isSpectator() && minecraft.player.drop(false)) {
            minecraft.player.swing(Hand.MAIN_HAND);
        }
    }

    private static void swapOffhand(Minecraft minecraft) {
        if (minecraft.player.isSpectator() || minecraft.getConnection() == null) {
            return;
        }
        minecraft.getConnection().send(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
    }

    private static void jump(Minecraft minecraft) {
        if (!minecraft.player.isOnGround()) {
            return;
        }
        minecraft.player.jumpFromGround();
    }

    private static void togglePerspective(Minecraft minecraft) {
        PointOfView previous = minecraft.options.getCameraType();
        PointOfView next = previous.cycle();
        minecraft.options.setCameraType(next);
        if (previous.isFirstPerson() != next.isFirstPerson()) {
            minecraft.gameRenderer.checkEntityPostEffect(next.isFirstPerson() ? minecraft.getCameraEntity() : null);
        }
        minecraft.levelRenderer.needsUpdate();
    }

    private static void takeScreenshot(Minecraft minecraft) {
        ScreenShotHelper.grab(minecraft.gameDirectory, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), minecraft.getMainRenderTarget(), message -> minecraft.execute(() -> minecraft.gui.getChat().addMessage(message)));
    }

    private static void toggleFullscreen(Minecraft minecraft) {
        minecraft.getWindow().toggleFullScreen();
        minecraft.options.fullscreen = minecraft.getWindow().isFullscreen();
    }

    private static void toggleSmoothCamera(Minecraft minecraft) {
        minecraft.options.smoothCamera = !minecraft.options.smoothCamera;
    }

    private static void cycleProfile(int delta) {
        OmniWheelClientRuntime runtime = OmniWheelClientRuntime.getInstance();
        runtime.profileManager().ensureLoaded();
        java.util.List<de.artemis.omniwheel.common.profile.WheelProfile> profiles = runtime.profileManager().getProfiles();
        if (profiles.size() <= 1) {
            return;
        }
        String activeProfileId = runtime.activeProfile().id();
        int currentIndex = 0;
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).id().equals(activeProfileId)) {
                currentIndex = index;
                break;
            }
        }
        int nextIndex = Math.floorMod(currentIndex + delta, profiles.size());
        runtime.profileManager().setActiveProfile(profiles.get(nextIndex).id());
    }

    private static void setSelectedHotbarSlot(PlayerInventory inventory, int slot) {
        inventory.selected = Math.max(0, Math.min(8, slot));
    }

    private static void cycleHotbarSlot(PlayerInventory inventory, int delta) {
        setSelectedHotbarSlot(inventory, Math.floorMod(inventory.selected + delta, 9));
    }

    private static boolean isPhysicalBindingDown(Minecraft minecraft, KeyBinding keyMapping) {
        net.minecraft.client.MainWindow window = minecraft.getWindow();
        long windowHandle = window.getWindow();
        InputMappings.Input key = OmniWheelKeyMappings.currentKey(keyMapping);
        if (java.util.Objects.equals(key.getType(), net.minecraft.client.util.InputMappings.Type.KEYSYM)) {
            return InputMappings.isKeyDown(window.getWindow(), key.getValue());
        } else {
            if (java.util.Objects.equals(key.getType(), net.minecraft.client.util.InputMappings.Type.SCANCODE)) {
                return GLFW.glfwGetKey(windowHandle, key.getValue()) == GLFW.GLFW_PRESS;
            } else {
                if (java.util.Objects.equals(key.getType(), net.minecraft.client.util.InputMappings.Type.MOUSE)) {
                    return GLFW.glfwGetMouseButton(windowHandle, key.getValue()) == GLFW.GLFW_PRESS;
                } else {
                    return false;
                }
            }
        }
    }

    private boolean isForced(ForcedKey forcedKey) {
        if (java.util.Objects.equals(forcedKey, de.artemis.omniwheel.client.runtime.GameplayFunctionController.ForcedKey.SNEAK)) {
            return forcedSneakApplied;
        } else {
            if (java.util.Objects.equals(forcedKey, de.artemis.omniwheel.client.runtime.GameplayFunctionController.ForcedKey.SPRINT)) {
                return forcedSprintApplied;
            } else {
                if (java.util.Objects.equals(forcedKey, de.artemis.omniwheel.client.runtime.GameplayFunctionController.ForcedKey.WALK)) {
                    return forcedWalkApplied;
                } else {
                    return de.artemis.omniwheel.common.Java8.fail(new IllegalStateException("Unexpected switch value"));
                }
            }
        }
    }

    private void markForced(ForcedKey forcedKey, boolean forced) {
        switch(forcedKey) {
            case SNEAK:
                {
                    forcedSneakApplied = forced;
                    break;
                }
            case SPRINT:
                {
                    forcedSprintApplied = forced;
                    break;
                }
            case WALK:
                {
                    forcedWalkApplied = forced;
                    break;
                }
        }
    }

    private enum ForcedKey {

        SNEAK, SPRINT, WALK
    }
}
