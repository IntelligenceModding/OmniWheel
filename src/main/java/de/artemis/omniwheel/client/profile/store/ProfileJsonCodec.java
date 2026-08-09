package de.artemis.omniwheel.client.profile.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.artemis.omniwheel.common.action.ChatAction;
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
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProfileJsonCodec {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public String encode(ProfileCollection collection) {
        return GSON.toJson(toDto(collection));
    }

    public ProfileCollection decode(String json) {
        StoredProfiles dto = GSON.fromJson(json, StoredProfiles.class);
        if (dto == null) {
            throw new IllegalStateException("profiles json was empty");
        }
        return fromDto(dto);
    }

    private StoredProfiles toDto(ProfileCollection collection) {
        StoredProfiles dto = new StoredProfiles();
        dto.activeProfileId = collection.activeProfileId();
        dto.profiles = new ArrayList<>();
        for (WheelProfile profile : collection.profiles()) {
            StoredProfile storedProfile = new StoredProfile();
            storedProfile.id = profile.id();
            storedProfile.displayName = profile.displayName();
            storedProfile.rootWheelId = profile.rootWheelId();
            storedProfile.wheels = new ArrayList<>();
            for (WheelDefinition wheel : profile.wheels().values()) {
                StoredWheel storedWheel = new StoredWheel();
                storedWheel.id = wheel.id();
                storedWheel.title = wheel.title();
                storedWheel.description = wheel.description();
                storedWheel.segmentCount = wheel.segmentCount();
                storedWheel.active = wheel.active();
                storedWheel.entries = new ArrayList<>();
                for (WheelEntry entry : wheel.entries()) {
                    StoredEntry storedEntry = new StoredEntry();
                    storedEntry.id = entry.id();
                    storedEntry.label = entry.label();
                    storedEntry.description = entry.description();
                    storedEntry.icon = entry.glyph();
                    storedEntry.color = entry.color();
                    storedEntry.guiColor = entry.guiColor();
                    storedEntry.active = entry.active();
                    storedEntry.showIcon = entry.showIcon();
                    storedEntry.showLabel = entry.showLabel();
                    storedEntry.showShortcut = entry.showShortcut();
                    storedEntry.shortcut = entry.shortcut();
                    storedEntry.action = toDto(entry.action());
                    storedWheel.entries.add(storedEntry);
                }
                storedProfile.wheels.add(storedWheel);
            }
            dto.profiles.add(storedProfile);
        }
        return dto;
    }

    private ProfileCollection fromDto(StoredProfiles dto) {
        List<WheelProfile> profiles = new ArrayList<>();
        if (dto.profiles == null || dto.profiles.isEmpty()) {
            throw new IllegalStateException("profiles json did not contain profiles");
        }

        for (StoredProfile storedProfile : dto.profiles) {
            Map<String, WheelDefinition> wheels = new LinkedHashMap<>();
            if (storedProfile.wheels == null || storedProfile.wheels.isEmpty()) {
                throw new IllegalStateException("profile " + storedProfile.id + " did not contain wheels");
            }

            for (StoredWheel storedWheel : storedProfile.wheels) {
                List<WheelEntry> entries = new ArrayList<>();
                if (storedWheel.entries != null) {
                    for (StoredEntry storedEntry : storedWheel.entries) {
                        entries.add(new WheelEntry(
                                storedEntry.id,
                                storedEntry.label,
                                defaultString(storedEntry.description),
                                defaultString(storedEntry.icon != null ? storedEntry.icon : storedEntry.glyph),
                                storedEntry.color != null ? storedEntry.color : WheelEntry.DEFAULT_COLOR,
                                storedEntry.guiColor != null ? storedEntry.guiColor : WheelEntry.DEFAULT_GUI_COLOR,
                                fromDto(storedEntry.action),
                                storedEntry.active == null || storedEntry.active,
                                storedEntry.showIcon == null || storedEntry.showIcon,
                                storedEntry.showLabel == null || storedEntry.showLabel,
                                storedEntry.showShortcut == null || storedEntry.showShortcut,
                                defaultString(storedEntry.shortcut)
                        ));
                    }
                }
                WheelDefinition wheel = new WheelDefinition(
                        storedWheel.id,
                        storedWheel.title,
                        defaultString(storedWheel.description),
                        storedWheel.segmentCount,
                        entries,
                        storedWheel.active == null || storedWheel.active
                );
                wheels.put(wheel.id(), wheel);
            }

            profiles.add(new WheelProfile(
                    storedProfile.id,
                    storedProfile.displayName,
                    storedProfile.rootWheelId,
                    wheels
            ));
        }

        String activeId = dto.activeProfileId != null && !dto.activeProfileId.isBlank()
                ? dto.activeProfileId
                : profiles.getFirst().id();
        return new ProfileCollection(activeId, profiles);
    }

    private StoredAction toDto(WheelAction action) {
        StoredAction stored = new StoredAction();
        switch (action) {
            case CommandAction commandAction -> {
                stored.type = "command";
                if (commandAction.commands().size() == 1) {
                    stored.value = commandAction.commands().getFirst();
                } else {
                    stored.values = new ArrayList<>(commandAction.commands());
                }
                stored.requireConfirmation = commandAction.requireConfirmation();
            }
            case ChatAction chatAction -> {
                stored.type = "chat";
                stored.value = chatAction.message();
                stored.requireConfirmation = chatAction.requireConfirmation();
            }
            case FunctionAction functionAction -> {
                stored.type = "function";
                stored.value = functionAction.function().id();
            }
            case LocalMessageAction localMessageAction -> {
                stored.type = "local_message";
                stored.value = localMessageAction.message();
            }
            case OpenChatAction openChatAction -> {
                stored.type = "open_chat";
                stored.value = openChatAction.initialText();
            }
            case CopyTextAction copyTextAction -> {
                stored.type = "copy_text";
                stored.value = copyTextAction.text();
            }
            case OpenWheelAction openWheelAction -> {
                stored.type = "open_wheel";
                stored.value = openWheelAction.wheelId();
            }
            case OpenScreenAction openScreenAction -> {
                stored.type = "open_screen";
                stored.value = openScreenAction.target().name().toLowerCase(Locale.ROOT);
            }
            default -> throw new IllegalArgumentException("Unsupported action type: " + action.getClass().getName());
        }
        return stored;
    }

    private WheelAction fromDto(StoredAction stored) {
        if (stored == null || stored.type == null) {
            throw new IllegalStateException("entry action was missing");
        }

        return switch (stored.type) {
            case "command" -> new CommandAction(
                    stored.values != null && !stored.values.isEmpty() ? stored.values : List.of(stored.value),
                    stored.requireConfirmation
            );
            case "chat" -> new ChatAction(stored.value, stored.requireConfirmation);
            case "function" -> new FunctionAction(GameplayFunction.byId(stored.value));
            case "local_message" -> new LocalMessageAction(stored.value);
            case "open_chat" -> new OpenChatAction(defaultString(stored.value));
            case "copy_text" -> new CopyTextAction(stored.value);
            case "open_wheel" -> new OpenWheelAction(stored.value);
            case "open_screen" -> new OpenScreenAction(ScreenTarget.valueOf(stored.value.toUpperCase(Locale.ROOT)));
            default -> throw new IllegalStateException("Unknown action type: " + stored.type);
        };
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static final class StoredProfiles {
        String activeProfileId;
        List<StoredProfile> profiles;
    }

    private static final class StoredProfile {
        String id;
        String displayName;
        String rootWheelId;
        List<StoredWheel> wheels;
    }

    private static final class StoredWheel {
        String id;
        String title;
        String description;
        int segmentCount;
        Boolean active;
        List<StoredEntry> entries;
    }

    private static final class StoredEntry {
        String id;
        String label;
        String description;
        String icon;
        String glyph;
        Integer color;
        Integer guiColor;
        Boolean active;
        Boolean showIcon;
        Boolean showLabel;
        Boolean showShortcut;
        String shortcut;
        StoredAction action;
    }

    private static final class StoredAction {
        String type;
        String value;
        List<String> values;
        boolean requireConfirmation;
    }
}
