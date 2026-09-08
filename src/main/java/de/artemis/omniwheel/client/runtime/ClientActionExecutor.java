package de.artemis.omniwheel.client.runtime;

import de.artemis.omniwheel.common.action.ChatAction;
import de.artemis.omniwheel.common.action.CommandAction;
import de.artemis.omniwheel.common.action.CopyTextAction;
import de.artemis.omniwheel.common.action.FunctionAction;
import de.artemis.omniwheel.common.action.LocalMessageAction;
import de.artemis.omniwheel.common.action.OpenChatAction;
import de.artemis.omniwheel.common.action.SwitchProfileAction;
import de.artemis.omniwheel.common.action.WheelAction;
import de.artemis.omniwheel.common.OmniWheelText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientActionExecutor {
    private final Map<Class<?>, ClientActionHandler<?>> handlers = new LinkedHashMap<>();

    public ClientActionExecutor() {
        register(CommandAction.class, this::executeCommand);
        register(ChatAction.class, this::executeChat);
        register(FunctionAction.class, this::executeFunction);
        register(OpenChatAction.class, this::executeOpenChat);
        register(CopyTextAction.class, this::executeCopyText);
        register(LocalMessageAction.class, this::executeLocalMessage);
        register(SwitchProfileAction.class, this::executeSwitchProfile);
    }

    public <T extends WheelAction> void register(Class<T> actionType, ClientActionHandler<T> handler) {
        handlers.put(actionType, handler);
    }

    public boolean execute(WheelAction action) {
        ClientActionHandler<WheelAction> handler = findHandler(action);
        if (handler == null) {
            return false;
        }
        handler.execute(action);
        return true;
    }

    @SuppressWarnings("unchecked")
    private ClientActionHandler<WheelAction> findHandler(WheelAction action) {
        for (Map.Entry<Class<?>, ClientActionHandler<?>> entry : handlers.entrySet()) {
            if (entry.getKey().isInstance(action)) {
                return (ClientActionHandler<WheelAction>) entry.getValue();
            }
        }
        return null;
    }

    private void executeCommand(CommandAction action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return;
        }

        for (String command : action.commands()) {
            String resolved = PlaceholderResolver.resolve(command, minecraft);
            if (resolved.startsWith("/")) {
                resolved = resolved.substring(1);
            }
            minecraft.player.connection.sendCommand(resolved);
        }
    }

    private void executeChat(ChatAction action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return;
        }

        minecraft.player.connection.sendChat(ContextValueFormatter.resolveForDisplay(action.message(), minecraft));
    }

    private void executeFunction(FunctionAction action) {
        runtime().functionController().execute(action.function());
    }

    private void executeLocalMessage(LocalMessageAction action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (ContextValueFormatter.extractCopyValues(action.message(), minecraft).isEmpty()) {
            minecraft.player.displayClientMessage(
                    Component.literal(PlaceholderResolver.resolve(action.message(), minecraft)),
                    true
            );
            return;
        }

        minecraft.player.displayClientMessage(
                ContextValueFormatter.buildInteractiveComponent(action.message(), minecraft),
                false
        );
    }

    private void executeOpenChat(OpenChatAction action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.setScreen(new ChatScreen(ContextValueFormatter.resolveForDisplay(action.initialText(), minecraft), false));
    }

    private void executeCopyText(CopyTextAction action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        String resolved = PlaceholderResolver.resolve(action.text(), minecraft);
        minecraft.keyboardHandler.setClipboard(resolved);
        minecraft.player.displayClientMessage(OmniWheelText.component("omniwheel.message.copied_to_clipboard"), true);
    }

    private void executeSwitchProfile(SwitchProfileAction action) {
        Minecraft minecraft = Minecraft.getInstance();
        runtime().profileManager().setActiveProfile(action.profileId());
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    OmniWheelText.component("omniwheel.message.active_profile", OmniWheelText.resolve(runtime().activeProfile().displayName())),
                    true
            );
        }
    }

    private static OmniWheelClientRuntime runtime() {
        return OmniWheelClientRuntime.getInstance();
    }

    @FunctionalInterface
    public interface ClientActionHandler<T extends WheelAction> {
        void execute(T action);
    }
}
