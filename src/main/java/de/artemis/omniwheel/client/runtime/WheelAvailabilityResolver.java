package de.artemis.omniwheel.client.runtime;

import com.mojang.brigadier.ParseResults;
import de.artemis.omniwheel.common.action.ChatAction;
import de.artemis.omniwheel.common.action.CommandAction;
import de.artemis.omniwheel.common.action.CopyTextAction;
import de.artemis.omniwheel.common.action.FunctionAction;
import de.artemis.omniwheel.common.action.GameplayFunction;
import de.artemis.omniwheel.common.action.LocalMessageAction;
import de.artemis.omniwheel.common.action.OpenChatAction;
import de.artemis.omniwheel.common.action.OpenScreenAction;
import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.action.SwitchProfileAction;
import de.artemis.omniwheel.common.action.WheelAction;
import de.artemis.omniwheel.common.OmniWheelText;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class WheelAvailabilityResolver {
    public static final String PROFILES_WHEEL_ID = "__profiles__";

    public List<WheelEntry> visibleEntries(WheelProfile profile, String wheelId, Deque<String> wheelPath) {
        if (PROFILES_WHEEL_ID.equals(wheelId)) {
            return profileEntries();
        }
        WheelDefinition wheel = profile.wheel(wheelId);
        if (!wheel.active()) {
            return com.google.common.collect.ImmutableList.of();
        }
        List<WheelEntry> visibleEntries = new ArrayList<>();
        for (WheelEntry entry : wheel.entries()) {
            if (isVisible(profile, entry, wheelPath)) {
                visibleEntries.add(entry);
            }
        }
        return visibleEntries;
    }

    public boolean isVisible(WheelProfile profile, WheelEntry entry, Deque<String> wheelPath) {
        if (!entry.active()) {
            return false;
        }
        WheelAction action = entry.action();
        if (action instanceof CommandAction) {
            return isCommandAvailable(((CommandAction) action));
        }
        if (action instanceof ChatAction) {
            return isChatAvailable();
        }
        if (action instanceof FunctionAction) {
            return ((FunctionAction) action).function() != GameplayFunction.OMNI_BACK
                    && OmniWheelClientRuntime.getInstance().functionController().isAvailable(((FunctionAction) action).function());
        }
        if (action instanceof OpenChatAction) {
            return isOpenChatAvailable(((OpenChatAction) action));
        }
        if (action instanceof CopyTextAction
                || action instanceof LocalMessageAction
                || action instanceof OpenScreenAction
                || action instanceof SwitchProfileAction) {
            return true;
        }
        if (action instanceof OpenWheelAction) {
            return isWheelEntryVisible(profile, ((OpenWheelAction) action), wheelPath);
        }
        return false;
    }

    private boolean isWheelEntryVisible(WheelProfile profile, OpenWheelAction action, Deque<String> wheelPath) {
        if (PROFILES_WHEEL_ID.equals(action.wheelId())) {
            return !profileEntries().isEmpty();
        }
        if (!profile.wheels().containsKey(action.wheelId())) {
            return false;
        }
        if (!profile.wheel(action.wheelId()).active()) {
            return false;
        }

        if (wheelPath.contains(action.wheelId())) {
            return true;
        }

        return hasUsefulVisibleEntry(profile, action.wheelId(), new ArrayList<>(wheelPath));
    }

    private boolean hasUsefulVisibleEntry(WheelProfile profile, String wheelId, List<String> path) {
        WheelDefinition wheel = profile.wheel(wheelId);
        if (!wheel.active()) {
            return false;
        }
        path.add(wheelId);

        for (WheelEntry entry : wheel.entries()) {
            if (!entry.active()) {
                continue;
            }
            WheelAction action = entry.action();
            if (action instanceof OpenWheelAction) {
                if (path.contains(((OpenWheelAction) action).wheelId())) {
                    continue;
                }
                if (profile.wheels().containsKey(((OpenWheelAction) action).wheelId()) && hasUsefulVisibleEntry(profile, ((OpenWheelAction) action).wheelId(), new ArrayList<>(path))) {
                    return true;
                }
                continue;
            }

            if (isVisible(profile, entry, copyAsDeque(path))) {
                return true;
            }
        }

        return false;
    }

    private boolean isCommandAvailable(CommandAction action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return false;
        }
        if (!canPlayerChat(minecraft, true)) {
            return false;
        }

        for (String command : action.commands()) {
            if (!isSingleCommandAvailable(command, minecraft)) {
                return false;
            }
        }
        return !action.commands().isEmpty();
    }

    private boolean isSingleCommandAvailable(String commandText, Minecraft minecraft) {
        ClientPlayNetHandler connection = minecraft.player.connection;
        String command = normalizeCommand(PlaceholderResolver.resolve(commandText, minecraft));
        ParseResults<?> parse = connection.getCommands().parse(command, connection.getSuggestionsProvider());
        return !parse.getReader().canRead() && parse.getExceptions().isEmpty() && parse.getContext().getLastChild().getCommand() != null;
    }

    private boolean isChatAvailable() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.connection != null && canPlayerChat(minecraft, false);
    }

    private boolean isOpenChatAvailable(OpenChatAction action) {
        String initialText = action.initialText().trim();
        if (!initialText.startsWith("/")) {
            return true;
        }
        return isCommandPrefixAvailable(initialText);
    }

    private boolean isCommandPrefixAvailable(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return false;
        }
        if (!canPlayerChat(minecraft, true)) {
            return false;
        }

        String normalized = normalizeCommand(command);
        if (normalized.trim().isEmpty()) {
            return false;
        }

        String commandName = normalized.split("\\s+", 2)[0];
        ClientPlayNetHandler connection = minecraft.player.connection;
        return connection.getCommands().getRoot().getChild(commandName) != null;
    }

    private static String normalizeCommand(String command) {
        String normalized = command.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static Deque<String> copyAsDeque(List<String> values) {
        return new java.util.ArrayDeque<>(values);
    }

    private static boolean canPlayerChat(Minecraft minecraft, boolean command) {
        return minecraft.player != null && canPlayerChat(minecraft.isLocalServer(), minecraft.allowsChat(),
                minecraft.options.chatVisibility, command);
    }

    static boolean canPlayerChat(boolean localServer, boolean allowsChat,
                                 net.minecraft.entity.player.ChatVisibility visibility, boolean command) {
        // Minecraft 1.16.5's openChatScreen ignores account chat restrictions in singleplayer.
        return (localServer || allowsChat)
                && visibility != net.minecraft.entity.player.ChatVisibility.HIDDEN
                && (command || visibility == net.minecraft.entity.player.ChatVisibility.FULL);
    }

    private List<WheelEntry> profileEntries() {
        List<WheelProfile> profiles = OmniWheelClientRuntime.getInstance().profileManager().getProfiles();
        WheelProfile activeProfile = OmniWheelClientRuntime.getInstance().activeProfile();
        List<WheelEntry> entries = new ArrayList<>(profiles.size());
        for (WheelProfile profile : profiles) {
            String displayName = OmniWheelText.resolve(profile.displayName());
            String glyph = displayName.trim().isEmpty()
                    ? "P"
                    : Character.toString(Character.toUpperCase(displayName.charAt(0)));
            int color = profile.id().equals(activeProfile.id()) ? 0xFF8BDAF8 : WheelEntry.DEFAULT_COLOR;
            entries.add(new WheelEntry(
                    "profile_" + profile.id(),
                    profile.displayName(),
                    profile.id().equals(activeProfile.id()) ? "omniwheel.profiles_wheel.current" : "omniwheel.profiles_wheel.activate",
                    glyph,
                    color,
                    new SwitchProfileAction(profile.id())
            ));
        }
        return entries;
    }
}
