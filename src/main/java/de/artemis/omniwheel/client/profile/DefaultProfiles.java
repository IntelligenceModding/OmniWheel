package de.artemis.omniwheel.client.profile;

import de.artemis.omniwheel.common.action.ChatAction;
import de.artemis.omniwheel.common.action.CommandAction;
import de.artemis.omniwheel.common.action.CopyTextAction;
import de.artemis.omniwheel.common.action.FunctionAction;
import de.artemis.omniwheel.common.action.GameplayFunction;
import de.artemis.omniwheel.common.action.LocalMessageAction;
import de.artemis.omniwheel.common.action.OpenChatAction;
import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DefaultProfiles {
    private DefaultProfiles() {
    }

    private static String key(String suffix) {
        return "omniwheel.default." + suffix;
    }

    public static WheelProfile createDefaultProfile() {
        Map<String, WheelDefinition> wheels = new LinkedHashMap<>();

        wheels.put("main", new WheelDefinition(
                "main",
                key("wheel.main"),
                key("wheel.main.description"),
                8,
                List.of(
                        new WheelEntry("spawn", key("entry.spawn"), key("entry.spawn.description"), "S", new CommandAction("/spawn", false)),
                        new WheelEntry("home", key("entry.home"), key("entry.home.description"), "H", new CommandAction("/home", false)),
                        new WheelEntry("chat", key("entry.chat"), key("entry.chat.description"), "C", new OpenWheelAction("communication")),
                        new WheelEntry("quick", key("entry.quick"), key("entry.quick.description"), "Q", new OpenWheelAction("quick")),
                        new WheelEntry("util", key("entry.utility"), key("entry.utility.description"), "U", new OpenWheelAction("utility")))
        ));

        wheels.put("communication", new WheelDefinition(
                "communication",
                key("wheel.communication"),
                key("wheel.communication.description"),
                8,
                List.of(
                        new WheelEntry("chat_omw", key("entry.chat_omw"), key("entry.chat_omw.description"), "1", new ChatAction(key("chat.on_my_way"), false)),
                        new WheelEntry("chat_help", key("entry.chat_help"), key("entry.chat_help.description"), "2", new ChatAction(key("chat.need_help"), false)),
                        new WheelEntry("chat_thanks", key("entry.chat_thanks"), key("entry.chat_thanks.description"), "3", new ChatAction(key("chat.thanks"), false)),
                        new WheelEntry("chat_share", key("entry.chat_share"), key("entry.chat_share.description"), "4", new ChatAction(key("chat.share_position"), false)),
                        new WheelEntry("chat_open", key("entry.chat_open"), key("entry.chat_open.description"), "5", new FunctionAction(GameplayFunction.OPEN_CHAT)),
                        new WheelEntry("chat_message", key("entry.chat_message"), key("entry.chat_message.description"), "6", new OpenChatAction("/msg ")),
                        new WheelEntry("chat_reply", key("entry.chat_reply"), key("entry.chat_reply.description"), "7", new OpenChatAction("/r ")))
        ));

        wheels.put("quick", new WheelDefinition(
                "quick",
                key("wheel.quick"),
                key("wheel.quick.description"),
                8,
                List.of(
                        new WheelEntry("quick_inventory", key("entry.quick_inventory"), key("entry.quick_inventory.description"), "1", new FunctionAction(GameplayFunction.OPEN_INVENTORY)),
                        new WheelEntry("quick_command", key("entry.quick_command"), key("entry.quick_command.description"), "2", new FunctionAction(GameplayFunction.OPEN_COMMAND_CHAT)),
                        new WheelEntry("quick_view", key("entry.quick_view"), key("entry.quick_view.description"), "3", new FunctionAction(GameplayFunction.TOGGLE_PERSPECTIVE)),
                        new WheelEntry("quick_shot", key("entry.quick_shot"), key("entry.quick_shot.description"), "4", new FunctionAction(GameplayFunction.TAKE_SCREENSHOT)))
        ));

        wheels.put("utility", new WheelDefinition(
                "utility",
                key("wheel.utility"),
                key("wheel.utility.description"),
                8,
                List.of(
                        new WheelEntry("util_profile", key("entry.util_profile"), key("entry.util_profile.description"), "P", new FunctionAction(GameplayFunction.OMNI_OPEN_PROFILES)),
                        new WheelEntry("util_manage", key("entry.util_manage"), key("entry.util_manage.description"), "G", new FunctionAction(GameplayFunction.OMNI_OPEN_MANAGER)),
                        new WheelEntry("util_copy", key("entry.util_copy"), key("entry.util_copy.description"), "C", new CopyTextAction("{x} {y} {z}")),
                        new WheelEntry("util_server", key("entry.util_server"), key("entry.util_server.description"), "S", new LocalMessageAction(key("local.current_context"))))
        ));

        return new WheelProfile("default", key("profile.default"), "main", wheels);
    }
}
