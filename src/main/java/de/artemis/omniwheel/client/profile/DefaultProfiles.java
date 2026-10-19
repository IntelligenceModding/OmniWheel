package de.artemis.omniwheel.client.profile;

import de.artemis.omniwheel.common.action.*;
import de.artemis.omniwheel.common.profile.WheelProfile;
import de.artemis.omniwheel.common.wheel.WheelDefinition;
import de.artemis.omniwheel.common.wheel.WheelEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A vanilla survival starter: ordinary controls and client-visible information, with no operator commands. */
public final class DefaultProfiles {
    // Also identifies the replacement generation, so upgrades never rebuild an already customized new default.
    public static final String ROOT_WHEEL_ID = "vanilla_main";
    private DefaultProfiles() {}
    private static String key(String suffix) { return "omniwheel.default." + suffix; }
    private static WheelEntry entry(String id, String icon, WheelAction action) {
        return new WheelEntry(id, key("entry." + id), key("entry." + id + ".description"), "minecraft:" + icon, action);
    }
    private static WheelEntry function(String id, String icon, GameplayFunction function) {
        return entry(id, icon, new FunctionAction(function));
    }
    private static WheelEntry binding(String id, String icon, String keybind) {
        return entry(id, icon, new KeybindAction("key." + keybind));
    }
    private static WheelEntry submenu(String id, String icon, String wheel) {
        return entry(id, icon, new OpenWheelAction(wheel));
    }
    private static WheelEntry chat(String id, String icon) {
        return entry(id, icon, new ChatAction(key("text." + id), false, true));
    }
    private static void wheel(Map<String, WheelDefinition> wheels, String id, String title, WheelEntry... entries) {
        wheels.put(id, new WheelDefinition(id, key("wheel." + title), key("wheel." + title + ".description"), 8, List.of(entries)));
    }

    public static WheelProfile createDefaultProfile() {
        Map<String, WheelDefinition> wheels = new LinkedHashMap<>();
        wheel(wheels, ROOT_WHEEL_ID, "main",
                function("inventory", "chest", GameplayFunction.OPEN_INVENTORY),
                function("advancements", "book", GameplayFunction.OPEN_ADVANCEMENTS),
                function("screenshot", "painting", GameplayFunction.TAKE_SCREENSHOT),
                submenu("communication", "writable_book", "communication"),
                submenu("location", "compass", "location"),
                submenu("inspect", "spyglass", "inspect"),
                submenu("controls", "lever", "controls"),
                submenu("view", "ender_eye", "view"));
        wheel(wheels, "communication", "communication",
                function("open_chat", "paper", GameplayFunction.OPEN_CHAT),
                chat("share_position", "compass"),
                chat("share_block", "stone"),
                chat("share_item", "item_frame"),
                chat("need_help", "bell"),
                chat("thanks", "sunflower"),
                entry("whisper", "name_tag", new OpenChatAction("/tell ")),
                entry("command_help", "book", new CommandAction(List.of("/help"), false, true)));
        wheel(wheels, "location", "location",
                entry("copy_position", "map", new CopyTextAction("{position}")),
                entry("copy_exact_position", "filled_map", new CopyTextAction("{exact_position}")),
                entry("location_info", "compass", new LocalMessageAction(key("text.location_info"))),
                entry("copy_dimension", "obsidian", new CopyTextAction("{dimension}")),
                entry("copy_biome", "grass_block", new CopyTextAction("{biome}")),
                entry("copy_facing", "recovery_compass", new CopyTextAction("{facing}")));
        wheel(wheels, "inspect", "inspect",
                entry("block_info", "spyglass", new LocalMessageAction(key("text.block_info"))),
                entry("copy_block_position", "map", new CopyTextAction("{target_block_position}")),
                entry("copy_block_id", "stone", new CopyTextAction("{target_block_id}")),
                entry("entity_info", "name_tag", new LocalMessageAction(key("text.entity_info"))),
                entry("copy_entity_uuid", "lead", new CopyTextAction("{target_entity_uuid}")),
                entry("copy_entity_position", "compass", new CopyTextAction("{target_entity_position}")),
                entry("item_info", "diamond_pickaxe", new LocalMessageAction(key("text.item_info"))),
                entry("copy_item_id", "item_frame", new CopyTextAction("{held_item_id}")));
        wheel(wheels, "controls", "controls",
                function("swap_offhand", "shield", GameplayFunction.SWAP_OFFHAND),
                function("pick_block", "grass_block", GameplayFunction.PICK_BLOCK),
                function("previous_slot", "wooden_sword", GameplayFunction.HOTBAR_PREVIOUS),
                function("next_slot", "wooden_pickaxe", GameplayFunction.HOTBAR_NEXT));
        wheel(wheels, "view", "view",
                binding("perspective", "ender_eye", "togglePerspective"),
                binding("hud", "glass_pane", "toggleGui"),
                binding("smooth_camera", "feather", "smoothCamera"),
                binding("fullscreen", "painting", "fullscreen"),
                binding("debug_overlay", "redstone", "debug.overlay"),
                binding("hitboxes", "target", "debug.showHitboxes"),
                binding("chunk_borders", "slime_block", "debug.showChunkBorders"),
                binding("advanced_tooltips", "book", "debug.showAdvancedTooltips"));
        return new WheelProfile("default", key("profile.default"), ROOT_WHEEL_ID, wheels);
    }
}
