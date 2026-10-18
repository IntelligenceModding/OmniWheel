package de.artemis.omniwheel.client.runtime;

import org.junit.Test;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import static org.junit.Assert.*;

public class LiveDataSnapshotTest {
    @Test public void itemsWithoutDurabilityOmitTheWholeInformationField() {
        var snapshot = new LiveDataSnapshot(Map.of("{held_item_name}", "Stone", "{held_item_id}", "minecraft:stone",
                "{held_item_count}", "64", "{held_item_durability}", ""));
        assertEquals("Stone (minecraft:stone) | Count: 64", snapshot.resolve(
                "{held_item_name} ({held_item_id}) | Count: {held_item_count} | Remaining durability: {held_item_durability}"));
        assertEquals("Stone | Count: 64", snapshot.resolve(
                "{held_item_name} | Durability: {held_item_durability} | Count: {held_item_count}"));
        assertEquals("Stone\nCount: 64", snapshot.resolve(
                "{held_item_name}\nDurability: {held_item_durability}\nCount: {held_item_count}"));
        assertEquals("", snapshot.resolve("{held_item_durability}"));
        assertNull(snapshot.missingMessage("{held_item_name} | Durability: {held_item_durability}"));
    }

    @Test public void realZeroDurabilityIsNotConfusedWithAnInapplicableValue() {
        var snapshot = new LiveDataSnapshot(Map.of("{held_item_durability}", "0"));
        assertEquals("Remaining durability: 0", snapshot.resolve("Remaining durability: {held_item_durability}"));
        var tool = new LiveDataSnapshot(Map.of("{held_item_durability}", "1534"));
        assertEquals("Remaining durability: 1534", tool.resolve("Remaining durability: {held_item_durability}"));
    }

    @Test public void omittingDurabilityPreservesOtherValuesAndLiteralItemNames() {
        var snapshot = new LiveDataSnapshot(Map.of("{held_item_name}", "Stone | {held_item_durability}", "{held_item_durability}", ""));
        assertEquals("Stone | {held_item_durability}", snapshot.resolve("{held_item_name} | Durability: {held_item_durability}"));
        assertEquals("Stone | {held_item_durability} ", snapshot.resolve("{held_item_name} {held_item_durability}"));
        assertEquals("Hold an item in your main hand first.", new LiveDataSnapshot(Map.of()).missingMessage("{held_item_durability}"));
    }
    @Test public void catalogTokensAreUniqueAndSearchableWithoutKnowingSyntax() {
        Set<String> tokens = new HashSet<>();
        for (var value : LiveDataValues.ALL) {
            assertTrue(tokens.add(value.token()));
            assertFalse(value.name().isBlank()); assertFalse(value.description().isBlank()); assertFalse(value.example().isBlank());
        }
        assertTrue(LiveDataValues.search("entity UUID").stream().anyMatch(value -> value.id().equals("target_entity_uuid")));
        assertTrue(LiveDataValues.search("block coordinates").stream().anyMatch(value -> value.id().equals("target_block_position")));
        assertTrue(LiveDataValues.search("held durability").stream().anyMatch(value -> value.id().equals("held_item_durability")));
        assertEquals(LiveDataValues.ALL.size(), LiveDataValues.search("").size());
    }
    @Test public void substitutesRepeatedValuesAndPreservesUnknownPlaceholders() {
        var snapshot = new LiveDataSnapshot(Map.of("{x}", "-12", "{position}", "-12 64 3"));
        assertEquals("At -12 64 3 (-12, -12) {unrelated}", snapshot.resolve("At {position} ({x}, {x}) {unrelated}"));
        assertEquals("unfinished {x", snapshot.resolve("unfinished {x"));
        assertEquals("", snapshot.resolve(""));
    }
    @Test public void namesCannotInjectOtherPlaceholders() {
        var snapshot = new LiveDataSnapshot(Map.of("{target_entity_name}", "Pet {x}", "{x}", "12"));
        assertEquals("Pet {x} at 12", snapshot.resolve("{target_entity_name} at {x}"));
    }
    @Test public void missingTargetsExplainWhatToDo() {
        var snapshot = new LiveDataSnapshot(Map.of("{position}", "1 2 3"));
        assertNull(snapshot.missingMessage("Here: {position}"));
        assertNull(snapshot.missingMessage("Unknown {not_a_live_value}"));
        assertEquals("Look at a block first.", snapshot.missingMessage("/setblock {target_block_position} stone"));
        assertEquals("Look at an entity first.", snapshot.missingMessage("/data get entity {target_entity_uuid}"));
        assertEquals("Hold an item in your main hand first.", snapshot.missingMessage("{held_item_id}"));
    }
    @Test public void addingDataMakesSameTemplateUsable() {
        var snapshot = new LiveDataSnapshot(Map.of("{target_entity_uuid}", "test-uuid", "{target_entity_name}", "Pet"));
        assertNull(snapshot.missingMessage("{target_entity_name}: {target_entity_uuid}"));
        assertEquals("Pet: test-uuid", snapshot.resolve("{target_entity_name}: {target_entity_uuid}"));
    }
    @Test public void snapshotDoesNotChangeWhenOriginalMapChanges() {
        var values = new java.util.HashMap<String, String>(); values.put("{x}", "1");
        var snapshot = new LiveDataSnapshot(values); values.put("{x}", "2");
        assertEquals("1", snapshot.resolve("{x}"));
    }
}
