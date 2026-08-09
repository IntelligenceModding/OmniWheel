package de.artemis.omniwheel.client.runtime;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.omniwheel.common.action.GameplayFunction;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

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

        switch (function) {
            case OMNI_BACK, OMNI_OPEN_PROFILES -> {
            }
            case OMNI_OPEN_MANAGER -> OmniWheelClientRuntime.getInstance().openTargetScreen(de.artemis.omniwheel.common.action.ScreenTarget.PROFILE_MANAGER, null);
            case OMNI_NEXT_PROFILE -> cycleProfile(1);
            case OMNI_PREVIOUS_PROFILE -> cycleProfile(-1);
            case OPEN_INVENTORY -> openInventory(minecraft);
            case OPEN_CHAT -> minecraft.setScreen(new ChatScreen(""));
            case OPEN_COMMAND_CHAT -> minecraft.setScreen(new ChatScreen("/"));
            case OPEN_ADVANCEMENTS -> openAdvancements(minecraft);
            case OPEN_SOCIAL_INTERACTIONS -> openSocialInteractions(minecraft);
            case ATTACK -> invokeMinecraftPrivate(minecraft, "startAttack");
            case USE_ITEM -> invokeMinecraftPrivate(minecraft, "startUseItem");
            case PICK_BLOCK -> invokeMinecraftPrivate(minecraft, "pickBlock");
            case DROP_ITEM -> dropHeldItem(minecraft);
            case SWAP_OFFHAND -> swapOffhand(minecraft);
            case JUMP -> jump(minecraft);
            case HOTBAR_SLOT_1 -> setSelectedHotbarSlot(minecraft.player.getInventory(), 0);
            case HOTBAR_SLOT_2 -> setSelectedHotbarSlot(minecraft.player.getInventory(), 1);
            case HOTBAR_SLOT_3 -> setSelectedHotbarSlot(minecraft.player.getInventory(), 2);
            case HOTBAR_SLOT_4 -> setSelectedHotbarSlot(minecraft.player.getInventory(), 3);
            case HOTBAR_SLOT_5 -> setSelectedHotbarSlot(minecraft.player.getInventory(), 4);
            case HOTBAR_SLOT_6 -> setSelectedHotbarSlot(minecraft.player.getInventory(), 5);
            case HOTBAR_SLOT_7 -> setSelectedHotbarSlot(minecraft.player.getInventory(), 6);
            case HOTBAR_SLOT_8 -> setSelectedHotbarSlot(minecraft.player.getInventory(), 7);
            case HOTBAR_SLOT_9 -> setSelectedHotbarSlot(minecraft.player.getInventory(), 8);
            case HOTBAR_NEXT -> cycleHotbarSlot(minecraft.player.getInventory(), 1);
            case HOTBAR_PREVIOUS -> cycleHotbarSlot(minecraft.player.getInventory(), -1);
            case TOGGLE_PERSPECTIVE -> togglePerspective(minecraft);
            case TAKE_SCREENSHOT -> takeScreenshot(minecraft);
            case TOGGLE_FULLSCREEN -> toggleFullscreen(minecraft);
            case TOGGLE_SMOOTH_CAMERA -> toggleSmoothCamera(minecraft);
            case TOGGLE_SNEAK_LOCK -> toggleSneakLock(minecraft);
            case TOGGLE_SPRINT_LOCK -> toggleSprintLock(minecraft);
            case TOGGLE_AUTO_WALK -> toggleAutoWalk(minecraft);
        }
    }

    public boolean isAvailable(GameplayFunction function) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }

        return switch (function) {
            case OMNI_BACK, OMNI_OPEN_PROFILES, OMNI_OPEN_MANAGER -> true;
            case OMNI_NEXT_PROFILE, OMNI_PREVIOUS_PROFILE -> OmniWheelClientRuntime.getInstance().profileManager().getProfiles().size() > 1;
            case OPEN_ADVANCEMENTS -> minecraft.player.connection != null;
            case OPEN_SOCIAL_INTERACTIONS -> isSocialInteractionsAvailable(minecraft);
            case DROP_ITEM, SWAP_OFFHAND -> !minecraft.player.isSpectator();
            default -> true;
        };
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
            minecraft.player.displayClientMessage(Component.literal(function.displayName() + ": " + (enabled ? "On" : "Off")), true);
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

    private void applyForcedKey(Minecraft minecraft, KeyMapping keyMapping, boolean enabled, ForcedKey forcedKey) {
        if (enabled) {
            keyMapping.setDown(true);
            markForced(forcedKey, true);
        } else {
            releaseForcedKey(minecraft, keyMapping, forcedKey);
        }
    }

    private void releaseForcedKey(Minecraft minecraft, KeyMapping keyMapping, ForcedKey forcedKey) {
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
            minecraft.player.displayClientMessage(Component.translatable("multiplayer.socialInteractions.not_available"), true);
            return;
        }
        minecraft.setScreen(new SocialInteractionsScreen());
    }

    private static boolean isSocialInteractionsAvailable(Minecraft minecraft) {
        return !minecraft.isLocalServer()
                || minecraft.hasSingleplayerServer()
                && minecraft.getSingleplayerServer() != null
                && minecraft.getSingleplayerServer().isPublished();
    }

    private static void dropHeldItem(Minecraft minecraft) {
        if (!minecraft.player.isSpectator() && minecraft.player.drop(false)) {
            minecraft.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static void swapOffhand(Minecraft minecraft) {
        if (minecraft.player.isSpectator() || minecraft.getConnection() == null) {
            return;
        }
        minecraft.getConnection()
                .send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
    }

    private static void jump(Minecraft minecraft) {
        if (!minecraft.player.onGround()) {
            return;
        }
        minecraft.player.jumpFromGround();
    }

    private static void togglePerspective(Minecraft minecraft) {
        CameraType previous = minecraft.options.getCameraType();
        CameraType next = previous.cycle();
        minecraft.options.setCameraType(next);
        if (previous.isFirstPerson() != next.isFirstPerson()) {
            minecraft.gameRenderer.checkEntityPostEffect(next.isFirstPerson() ? minecraft.getCameraEntity() : null);
        }
        minecraft.levelRenderer.needsUpdate();
    }

    private static void takeScreenshot(Minecraft minecraft) {
        Screenshot.grab(
                minecraft.gameDirectory,
                minecraft.getMainRenderTarget(),
                message -> minecraft.execute(() -> minecraft.gui.getChat().addMessage(message))
        );
    }

    private static void toggleFullscreen(Minecraft minecraft) {
        minecraft.getWindow().toggleFullScreen();
        minecraft.options.fullscreen().set(minecraft.getWindow().isFullscreen());
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

    private static void invokeMinecraftPrivate(Minecraft minecraft, String methodName) {
        try {
            Method method = Minecraft.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(minecraft);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void setSelectedHotbarSlot(Inventory inventory, int slot) {
        int clamped = Math.max(0, Math.min(8, slot));
        for (String methodName : new String[]{"setSelectedSlot", "setSelected"}) {
            try {
                Method method = inventory.getClass().getMethod(methodName, int.class);
                method.invoke(inventory, clamped);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            var field = inventory.getClass().getField("selected");
            field.setInt(inventory, clamped);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void cycleHotbarSlot(Inventory inventory, int delta) {
        int current = selectedHotbarSlot(inventory);
        setSelectedHotbarSlot(inventory, Math.floorMod(current + delta, 9));
    }

    private static int selectedHotbarSlot(Inventory inventory) {
        for (String methodName : new String[]{"getSelectedSlot", "getSelected"}) {
            try {
                Method method = inventory.getClass().getMethod(methodName);
                Object value = method.invoke(inventory);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            var field = inventory.getClass().getField("selected");
            return field.getInt(inventory);
        } catch (ReflectiveOperationException ignored) {
        }
        return 0;
    }

    private static boolean isPhysicalBindingDown(Minecraft minecraft, KeyMapping keyMapping) {
        long windowHandle = minecraft.getWindow().getWindow();
        InputConstants.Key key = keyMapping.getKey();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(windowHandle, key.getValue());
            case SCANCODE -> GLFW.glfwGetKey(windowHandle, key.getValue()) == GLFW.GLFW_PRESS;
            case MOUSE -> GLFW.glfwGetMouseButton(windowHandle, key.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }

    private boolean isForced(ForcedKey forcedKey) {
        return switch (forcedKey) {
            case SNEAK -> forcedSneakApplied;
            case SPRINT -> forcedSprintApplied;
            case WALK -> forcedWalkApplied;
        };
    }

    private void markForced(ForcedKey forcedKey, boolean forced) {
        switch (forcedKey) {
            case SNEAK -> forcedSneakApplied = forced;
            case SPRINT -> forcedSprintApplied = forced;
            case WALK -> forcedWalkApplied = forced;
        }
    }

    private enum ForcedKey {
        SNEAK,
        SPRINT,
        WALK
    }
}
