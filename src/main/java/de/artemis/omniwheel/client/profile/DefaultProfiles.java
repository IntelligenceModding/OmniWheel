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

    public static WheelProfile createDefaultProfile() {
        Map<String, WheelDefinition> wheels = new LinkedHashMap<>();

        wheels.put("main", new WheelDefinition(
                "main",
                "Main",
                "General shortcuts for typical survival play sessions.",
                8,
                List.of(
                        new WheelEntry("spawn", "Spawn", "Sends your configured /spawn command.", "S", new CommandAction("/spawn", false)),
                        new WheelEntry("home", "Home", "Sends your configured /home command.", "H", new CommandAction("/home", false)),
                        new WheelEntry("chat", "Chat", "Open communication shortcuts.", "C", new OpenWheelAction("communication")),
                        new WheelEntry("quick", "Quick", "Open client-side quick actions.", "Q", new OpenWheelAction("quick")),
                        new WheelEntry("util", "Utility", "Open utility shortcuts.", "U", new OpenWheelAction("utility")))
        ));

        wheels.put("communication", new WheelDefinition(
                "communication",
                "Communication",
                "Reusable multiplayer chat actions.",
                8,
                List.of(
                        new WheelEntry("chat_omw", "On My Way", "Posts a quick travel update.", "1", new ChatAction("On my way.", false)),
                        new WheelEntry("chat_help", "Need Help", "Requests help and includes your coordinates.", "2", new ChatAction("Need help at {x}, {y}, {z}.", false)),
                        new WheelEntry("chat_thanks", "Thanks", "Posts a quick thank-you in chat.", "3", new ChatAction("Thanks!", false)),
                        new WheelEntry("chat_share", "Share Pos", "Posts your current position in chat.", "4", new ChatAction("Meet me at {x}, {y}, {z} in {dimension}.", false)),
                        new WheelEntry("chat_open", "Open Chat", "Open normal chat input.", "5", new FunctionAction(GameplayFunction.OPEN_CHAT)),
                        new WheelEntry("chat_message", "Message", "Opens chat with a private-message command prefilled.", "6", new OpenChatAction("/msg ")),
                        new WheelEntry("chat_reply", "Reply", "Opens chat with a reply command prefilled.", "7", new OpenChatAction("/r ")))
        ));

        wheels.put("quick", new WheelDefinition(
                "quick",
                "Quick",
                "Client-side actions that are useful in normal play.",
                8,
                List.of(
                        new WheelEntry("quick_inventory", "Inventory", "Open the player inventory.", "1", new FunctionAction(GameplayFunction.OPEN_INVENTORY)),
                        new WheelEntry("quick_command", "Command Chat", "Open chat prefilled with '/'.", "2", new FunctionAction(GameplayFunction.OPEN_COMMAND_CHAT)),
                        new WheelEntry("quick_view", "Perspective", "Toggle first- and third-person view.", "3", new FunctionAction(GameplayFunction.TOGGLE_PERSPECTIVE)),
                        new WheelEntry("quick_shot", "Screenshot", "Take a screenshot.", "4", new FunctionAction(GameplayFunction.TAKE_SCREENSHOT)))
        ));

        wheels.put("utility", new WheelDefinition(
                "utility",
                "Utility",
                "Profiles, management, and quiet local tools.",
                8,
                List.of(
                        new WheelEntry("util_profile", "Profiles", "Open the available profile list.", "P", new FunctionAction(GameplayFunction.OMNI_OPEN_PROFILES)),
                        new WheelEntry("util_manage", "Manage", "Open the local profile manager.", "G", new FunctionAction(GameplayFunction.OMNI_OPEN_MANAGER)),
                        new WheelEntry("util_copy", "Copy Pos", "Copies your raw coordinates to the clipboard.", "C", new CopyTextAction("{x} {y} {z}")),
                        new WheelEntry("util_server", "Server", "Shows the current server or world context.", "S", new LocalMessageAction("Current context: {server} ({dimension})")))
        ));

        return new WheelProfile("default", "Default", "main", wheels);
    }
}
