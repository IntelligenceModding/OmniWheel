package de.artemis.omniwheel.client.profile.store;

import de.artemis.omniwheel.common.action.ChatAction;
import de.artemis.omniwheel.common.action.CommandAction;
import de.artemis.omniwheel.common.action.CopyTextAction;
import de.artemis.omniwheel.common.action.FunctionAction;
import de.artemis.omniwheel.common.action.GameplayFunction;
import de.artemis.omniwheel.common.action.LocalMessageAction;
import de.artemis.omniwheel.common.action.OpenChatAction;
import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.action.WheelAction;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ProfileUpgrader {
    private static final Map<String, String> LEGACY_COMMUNICATION_GLYPHS = Map.of(
            "chat_omw", "O",
            "chat_help", "!",
            "chat_target", "@",
            "chat_reply", "R"
    );

    private static final Map<String, String> CURRENT_COMMUNICATION_GLYPHS = Map.of(
            "chat_omw", "1",
            "chat_help", "2",
            "chat_thanks", "3",
            "chat_share", "4",
            "chat_message", "5",
            "chat_reply", "6",
            "chat_copy", "7"
    );

    private ProfileUpgrader() {
    }

    static ProfileCollection upgrade(ProfileCollection profiles) {
        boolean changed = false;
        List<WheelProfile> upgradedProfiles = new ArrayList<>(profiles.profiles().size());
        for (WheelProfile profile : profiles.profiles()) {
            WheelProfile upgradedProfile = upgradeProfile(profile);
            upgradedProfiles.add(upgradedProfile);
            changed |= upgradedProfile != profile;
        }

        if (!changed) {
            return profiles;
        }
        return new ProfileCollection(profiles.activeProfileId(), upgradedProfiles);
    }

    private static WheelProfile upgradeProfile(WheelProfile profile) {
        WheelProfile strippedProfile = stripLegacyBackEntries(profile);
        if (!"default".equals(profile.id())) {
            return strippedProfile;
        }

        WheelDefinition communication = strippedProfile.wheels().get("communication");
        WheelDefinition gamemode = strippedProfile.wheels().get("gamemode");
        WheelDefinition main = strippedProfile.wheels().get("main");
        WheelDefinition quick = strippedProfile.wheels().get("quick");
        WheelDefinition utility = strippedProfile.wheels().get("utility");
        boolean changed = false;

        WheelDefinition upgradedCommunication = communication;
        if (communication != null) {
            WheelDefinition migratedCommunication = upgradeCommunicationWheel(communication);
            if (migratedCommunication != communication) {
                upgradedCommunication = migratedCommunication;
                changed = true;
            }
        }

        WheelDefinition upgradedMain = main;
        if (main != null) {
            WheelDefinition migratedMain = upgradeMainWheel(main);
            if (migratedMain != main) {
                upgradedMain = migratedMain;
                changed = true;
            }
        }

        WheelDefinition upgradedGamemode = gamemode;

        WheelDefinition upgradedQuick = quick;
        if (quick != null) {
            WheelDefinition migratedQuick = upgradeQuickWheel(quick);
            if (migratedQuick != quick) {
                upgradedQuick = migratedQuick;
                changed = true;
            }
        } else {
            upgradedQuick = defaultQuickWheel();
            changed = true;
        }

        WheelDefinition upgradedUtility = utility;
        if (utility != null) {
            WheelDefinition migratedUtility = upgradeUtilityWheel(utility);
            if (migratedUtility != utility) {
                upgradedUtility = migratedUtility;
                changed = true;
            }
        }

        if (!changed) {
            return strippedProfile;
        }

        Map<String, WheelDefinition> upgradedWheels = new LinkedHashMap<>();
        for (Map.Entry<String, WheelDefinition> wheelEntry : strippedProfile.wheels().entrySet()) {
            if (wheelEntry.getKey().equals("communication")) {
                upgradedWheels.put(wheelEntry.getKey(), upgradedCommunication);
            } else if (wheelEntry.getKey().equals("gamemode")) {
                upgradedWheels.put(wheelEntry.getKey(), upgradedGamemode);
            } else if (wheelEntry.getKey().equals("main")) {
                upgradedWheels.put(wheelEntry.getKey(), upgradedMain);
            } else if (wheelEntry.getKey().equals("quick")) {
                upgradedWheels.put(wheelEntry.getKey(), upgradedQuick);
            } else if (wheelEntry.getKey().equals("utility")) {
                upgradedWheels.put(wheelEntry.getKey(), upgradedUtility);
            } else {
                upgradedWheels.put(wheelEntry.getKey(), wheelEntry.getValue());
            }
        }
        if (!upgradedWheels.containsKey("quick")) {
            upgradedWheels.put("quick", upgradedQuick);
        }

        return new WheelProfile(strippedProfile.id(), strippedProfile.displayName(), strippedProfile.rootWheelId(), upgradedWheels);
    }

    private static WheelProfile stripLegacyBackEntries(WheelProfile profile) {
        boolean changed = false;
        Map<String, WheelDefinition> upgradedWheels = new LinkedHashMap<>();
        for (Map.Entry<String, WheelDefinition> wheelEntry : profile.wheels().entrySet()) {
            WheelDefinition wheel = wheelEntry.getValue();
            List<WheelEntry> filteredEntries = new ArrayList<>(wheel.entries().size());
            for (WheelEntry entry : wheel.entries()) {
                if (entry.action() instanceof FunctionAction functionAction && functionAction.function() == GameplayFunction.OMNI_BACK) {
                    changed = true;
                    continue;
                }
                filteredEntries.add(entry);
            }

            if (filteredEntries.size() == wheel.entries().size()) {
                upgradedWheels.put(wheelEntry.getKey(), wheel);
                continue;
            }

            upgradedWheels.put(wheelEntry.getKey(), new WheelDefinition(
                    wheel.id(),
                    wheel.title(),
                    wheel.description(),
                    wheel.segmentCount(),
                    filteredEntries,
                    wheel.active()
            ));
        }

        if (!changed) {
            return profile;
        }
        return new WheelProfile(profile.id(), profile.displayName(), profile.rootWheelId(), upgradedWheels);
    }

    private static WheelDefinition upgradeMainWheel(WheelDefinition main) {
        Map<String, WheelEntry> entriesById = new LinkedHashMap<>();
        List<WheelEntry> extras = new ArrayList<>();
        for (WheelEntry entry : main.entries()) {
            if ("modes".equals(entry.id()) || "status".equals(entry.id()) || "thanks".equals(entry.id()) || "coords".equals(entry.id())) {
                continue;
            }
            if (isMainStockEntry(entry.id())) {
                entriesById.put(entry.id(), entry);
            } else {
                extras.add(entry);
            }
        }

        List<WheelEntry> upgradedEntries = new ArrayList<>();
        upgradedEntries.add(ensureEntry(entriesById.get("spawn"), "spawn", "Spawn", "Sends your configured /spawn command.", "S", new CommandAction("/spawn", false)));
        upgradedEntries.add(ensureEntry(entriesById.get("home"), "home", "Home", "Sends your configured /home command.", "H", new CommandAction("/home", false)));
        upgradedEntries.add(ensureEntry(entriesById.get("chat"), "chat", "Chat", "Open communication shortcuts.", "C", new OpenWheelAction("communication")));
        upgradedEntries.add(ensureEntry(entriesById.get("quick"), "quick", "Quick", "Open client-side quick actions.", "Q", new OpenWheelAction("quick")));
        upgradedEntries.add(ensureEntry(entriesById.get("util"), "util", "Utility", "Open utility shortcuts.", "U", new OpenWheelAction("utility")));
        upgradedEntries.addAll(extras);

        if (upgradedEntries.equals(main.entries())) {
            return main;
        }

        return new WheelDefinition(
                main.id(),
                main.title(),
                main.description(),
                main.segmentCount(),
                upgradedEntries,
                main.active()
        );
    }

    private static WheelDefinition upgradeUtilityWheel(WheelDefinition utility) {
        Map<String, WheelEntry> entriesById = new LinkedHashMap<>();
        List<WheelEntry> extras = new ArrayList<>();
        for (WheelEntry entry : utility.entries()) {
            switch (entry.id()) {
                case "util_safe" -> {
                    continue;
                }
                case "util_profile", "util_manage", "util_copy", "util_server" -> entriesById.put(entry.id(), entry);
                default -> {
                    extras.add(entry);
                }
            }
        }

        List<WheelEntry> upgradedEntries = new ArrayList<>();
        upgradedEntries.add(ensureEntry(entriesById.get("util_profile"), "util_profile", "Profiles", "Open the available profile list.", "P", new FunctionAction(GameplayFunction.OMNI_OPEN_PROFILES)));
        upgradedEntries.add(ensureEntry(entriesById.get("util_manage"), "util_manage", "Manage", "Open the local profile manager.", "G", new FunctionAction(GameplayFunction.OMNI_OPEN_MANAGER)));
        upgradedEntries.add(ensureEntry(entriesById.get("util_copy"), "util_copy", "Copy Pos", "Copies your raw coordinates to the clipboard.", "C", new CopyTextAction("{x} {y} {z}")));
        upgradedEntries.add(ensureEntry(entriesById.get("util_server"), "util_server", "Server", "Shows the current server or world context.", "S", new LocalMessageAction("Current context: {server} ({dimension})")));
        upgradedEntries.addAll(extras);

        return new WheelDefinition(
                utility.id(),
                utility.title(),
                utility.description(),
                utility.segmentCount(),
                upgradedEntries,
                utility.active()
        );
    }

    private static WheelDefinition upgradeCommunicationWheel(WheelDefinition communication) {
        Map<String, WheelEntry> entriesById = new LinkedHashMap<>();
        List<WheelEntry> extras = new ArrayList<>();
        for (WheelEntry entry : communication.entries()) {
            switch (entry.id()) {
                case "chat_target" -> entriesById.put("chat_message", new WheelEntry(
                        "chat_message",
                        "Message",
                        "Opens chat with a private-message command prefilled.",
                        "6",
                        entry.color(),
                        entry.guiColor(),
                        new OpenChatAction("/msg "),
                        entry.active(),
                        entry.showIcon(),
                        entry.showLabel(),
                        entry.showShortcut(),
                        entry.shortcut()
                ));
                case "chat_copy" -> {
                }
                case "chat_omw", "chat_help", "chat_thanks", "chat_share", "chat_open", "chat_message", "chat_reply" ->
                        entriesById.put(entry.id(), entry);
                default -> extras.add(entry);
            }
        }

        List<WheelEntry> upgradedEntries = new ArrayList<>();
        upgradedEntries.add(ensureEntry(entriesById.get("chat_omw"), "chat_omw", "On My Way", "Posts a quick travel update.", "1", new ChatAction("On my way.", false)));
        upgradedEntries.add(ensureEntry(entriesById.get("chat_help"), "chat_help", "Need Help", "Requests help and includes your coordinates.", "2", new ChatAction("Need help at {x}, {y}, {z}.", false)));
        upgradedEntries.add(ensureEntry(entriesById.get("chat_thanks"), "chat_thanks", "Thanks", "Posts a quick thank-you in chat.", "3", new ChatAction("Thanks!", false)));
        upgradedEntries.add(ensureEntry(entriesById.get("chat_share"), "chat_share", "Share Pos", "Posts your current position in chat.", "4", new ChatAction("Meet me at {x}, {y}, {z} in {dimension}.", false)));
        upgradedEntries.add(ensureEntry(entriesById.get("chat_open"), "chat_open", "Open Chat", "Open normal chat input.", "5", new FunctionAction(GameplayFunction.OPEN_CHAT)));
        upgradedEntries.add(ensureEntry(entriesById.get("chat_message"), "chat_message", "Message", "Opens chat with a private-message command prefilled.", "6", new OpenChatAction("/msg ")));
        upgradedEntries.add(ensureEntry(entriesById.get("chat_reply"), "chat_reply", "Reply", "Opens chat with a reply command prefilled.", "7", new OpenChatAction("/r ")));
        upgradedEntries.addAll(extras);

        return new WheelDefinition(
                communication.id(),
                communication.title(),
                communication.description(),
                communication.segmentCount(),
                upgradedEntries,
                communication.active()
        );
    }

    private static WheelDefinition upgradeQuickWheel(WheelDefinition quick) {
        Map<String, WheelEntry> entriesById = new LinkedHashMap<>();
        List<WheelEntry> extras = new ArrayList<>();
        for (WheelEntry entry : quick.entries()) {
            if (isQuickStockEntry(entry.id())) {
                entriesById.put(entry.id(), entry);
            } else {
                extras.add(entry);
            }
        }

        List<WheelEntry> upgradedEntries = new ArrayList<>();
        upgradedEntries.add(ensureEntry(entriesById.get("quick_inventory"), "quick_inventory", "Inventory", "Open the player inventory.", "1", new FunctionAction(GameplayFunction.OPEN_INVENTORY)));
        upgradedEntries.add(ensureEntry(entriesById.get("quick_command"), "quick_command", "Command Chat", "Open chat prefilled with '/'.", "2", new FunctionAction(GameplayFunction.OPEN_COMMAND_CHAT)));
        upgradedEntries.add(ensureEntry(entriesById.get("quick_view"), "quick_view", "Perspective", "Toggle first- and third-person view.", "3", new FunctionAction(GameplayFunction.TOGGLE_PERSPECTIVE)));
        upgradedEntries.add(ensureEntry(entriesById.get("quick_shot"), "quick_shot", "Screenshot", "Take a screenshot.", "4", new FunctionAction(GameplayFunction.TAKE_SCREENSHOT)));
        upgradedEntries.addAll(extras);

        return new WheelDefinition(
                quick.id(),
                quick.title(),
                quick.description(),
                quick.segmentCount(),
                upgradedEntries,
                quick.active()
        );
    }

    private static WheelDefinition defaultQuickWheel() {
        return new WheelDefinition(
                "quick",
                "Quick",
                "Client-side actions that are useful in normal play.",
                8,
                List.of(
                        new WheelEntry("quick_inventory", "Inventory", "Open the player inventory.", "1", new FunctionAction(GameplayFunction.OPEN_INVENTORY)),
                        new WheelEntry("quick_command", "Command Chat", "Open chat prefilled with '/'.", "2", new FunctionAction(GameplayFunction.OPEN_COMMAND_CHAT)),
                        new WheelEntry("quick_view", "Perspective", "Toggle first- and third-person view.", "3", new FunctionAction(GameplayFunction.TOGGLE_PERSPECTIVE)),
                        new WheelEntry("quick_shot", "Screenshot", "Take a screenshot.", "4", new FunctionAction(GameplayFunction.TAKE_SCREENSHOT))
                )
        );
    }

    private static WheelEntry ensureEntry(WheelEntry entry, String label, String description, String glyph, WheelAction action) {
        return entry;
    }

    private static WheelEntry ensureEntry(WheelEntry entry, String id, String label, String description, String glyph, WheelAction action) {
        if (entry == null) {
            return new WheelEntry(id, label, description, glyph, action);
        }
        return ensureEntry(entry, label, description, glyph, action);
    }

    private static boolean isMainStockEntry(String id) {
        return "spawn".equals(id)
                || "home".equals(id)
                || "chat".equals(id)
                || "quick".equals(id)
                || "util".equals(id);
    }

    private static boolean isQuickStockEntry(String id) {
        return "quick_inventory".equals(id)
                || "quick_command".equals(id)
                || "quick_view".equals(id)
                || "quick_shot".equals(id);
    }

    private static void insertBeforeBack(List<WheelEntry> entries, WheelEntry newEntry) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).action() instanceof OpenWheelAction) {
                entries.add(index, newEntry);
                return;
            }
        }
        entries.add(newEntry);
    }
}
