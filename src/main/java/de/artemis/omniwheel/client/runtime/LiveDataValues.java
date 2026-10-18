package de.artemis.omniwheel.client.runtime;

import java.util.List;
import java.util.Locale;

/**
 * One catalog drives the picker, examples, search, and missing-data checks.
 */
public final class LiveDataValues {

    public final static class Value {

        private final String id;

        private final String group;

        private final String name;

        private final String description;

        private final String example;

        public String token() {
            return "{" + id + "}";
        }

        public boolean matches(String query) {
            String haystack = (group + " " + name + " " + description + " " + id).toLowerCase(Locale.ROOT);
            for (String word : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) if (!haystack.contains(word))
                return false;
            return true;
        }

        public String missingMessage() {
            if (java.util.Objects.equals(group, "Target block")) {
                return "Look at a block first.";
            } else {
                if (java.util.Objects.equals(group, "Target entity")) {
                    return "Look at an entity first.";
                } else {
                    if (java.util.Objects.equals(group, "Held item")) {
                        return "Hold an item in your main hand first.";
                    } else {
                        return "Join a world first.";
                    }
                }
            }
        }

        public Value(String id, String group, String name, String description, String example) {
            this.id = id;
            this.group = group;
            this.name = name;
            this.description = description;
            this.example = example;
        }

        public String id() {
            return id;
        }

        public String group() {
            return group;
        }

        public String name() {
            return name;
        }

        public String description() {
            return description;
        }

        public String example() {
            return example;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value)
                return true;
            if (value == null || value.getClass() != getClass())
                return false;
            Value other = (Value) value;
            return true && java.util.Objects.equals(id, other.id) && java.util.Objects.equals(group, other.group) && java.util.Objects.equals(name, other.name) && java.util.Objects.equals(description, other.description) && java.util.Objects.equals(example, other.example);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, group, name, description, example);
        }

        @Override
        public String toString() {
            return "Value[" + "id=" + id + ", group=" + group + ", name=" + name + ", description=" + description + ", example=" + example + "]";
        }
    }

    private static Value value(String id, String group, String name, String description, String example) {
        return new Value(id, group, name, description, example);
    }

    public static final List<Value> ALL = de.artemis.omniwheel.common.Java8.list(value("position", "Player", "Block coordinates", "Your X Y Z block position, ready for commands.", "120 64 -350"), value("exact_position", "Player", "Exact coordinates", "Your X Y Z position with two decimal places.", "120.25 64.00 -349.75"), value("x", "Player", "X coordinate", "Your block position on the X axis.", "120"), value("y", "Player", "Y coordinate", "Your block position on the Y axis.", "64"), value("z", "Player", "Z coordinate", "Your block position on the Z axis.", "-350"), value("player", "Player", "Name", "Your player name.", "Alex"), value("player_uuid", "Player", "UUID", "Your unique player identifier.", "123e4567-e89b-12d3-a456-426614174000"), value("facing", "Player", "Facing direction", "The compass direction you are facing.", "north"), value("yaw", "Player", "Yaw", "Your horizontal viewing angle in degrees.", "90.00"), value("pitch", "Player", "Pitch", "Your vertical viewing angle in degrees.", "12.50"), value("health", "Player", "Health", "Your current health points; two points equal one heart.", "20.00"), value("gamemode", "Player", "Game mode", "Your current game mode.", "Survival"), value("dimension", "World", "Dimension ID", "The dimension identifier used by commands.", "minecraft:overworld"), value("biome", "World", "Biome ID", "The biome at your position.", "minecraft:plains"), value("server", "World", "Server name", "The current server name, or Singleplayer.", "My server"), value("profile", "World", "Wheel profile", "The name of your active Omni Wheel profile.", "Default"), value("target_block_position", "Target block", "Coordinates", "X Y Z of the block under your crosshair.", "125 65 -348"), value("target_block_x", "Target block", "X coordinate", "The targeted block's X coordinate.", "125"), value("target_block_y", "Target block", "Y coordinate", "The targeted block's Y coordinate.", "65"), value("target_block_z", "Target block", "Z coordinate", "The targeted block's Z coordinate.", "-348"), value("target_block_id", "Target block", "Block ID", "The block's registry identifier, including its mod namespace.", "minecraft:stone"), value("target_block_name", "Target block", "Block name", "The targeted block's readable name.", "Stone"), value("target_entity_uuid", "Target entity", "UUID", "The unique identifier of the entity under your crosshair.", "123e4567-e89b-12d3-a456-426614174000"), value("target_entity_id", "Target entity", "Entity type ID", "The entity type's registry identifier, including its mod namespace.", "minecraft:zombie"), value("target_entity_name", "Target entity", "Name", "The targeted entity's name, including custom names.", "Zombie"), value("target_entity_position", "Target entity", "Coordinates", "The targeted entity's X Y Z block position.", "124 64 -347"), value("target_entity_x", "Target entity", "X coordinate", "The targeted entity's block X coordinate.", "124"), value("target_entity_y", "Target entity", "Y coordinate", "The targeted entity's block Y coordinate.", "64"), value("target_entity_z", "Target entity", "Z coordinate", "The targeted entity's block Z coordinate.", "-347"), value("held_item_id", "Held item", "Item ID", "The registry identifier of the item in your main hand.", "minecraft:diamond_pickaxe"), value("held_item_name", "Held item", "Item name", "The held item's name, including custom names.", "Diamond Pickaxe"), value("held_item_count", "Held item", "Count", "The number of items in your main-hand stack.", "32"), value("held_item_durability", "Held item", "Remaining durability", "Remaining uses. Omitted for items without durability.", "1534"));

    private LiveDataValues() {
    }

    public static boolean contains(String template) {
        return ALL.stream().anyMatch(value -> template.contains(value.token()));
    }

    public static List<Value> search(String query) {
        return ALL.stream().filter(value -> value.matches(query == null ? "" : query)).collect(de.artemis.omniwheel.common.Java8.toList());
    }
}
