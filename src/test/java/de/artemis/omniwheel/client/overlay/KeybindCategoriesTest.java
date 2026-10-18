package de.artemis.omniwheel.client.overlay;

import org.junit.Test;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

public class KeybindCategoriesTest {
    private static final List<KeybindCategories.Option> OPTIONS = List.of(
            new KeybindCategories.Option("key.jump", "Jump", "minecraft:movement", "Movement"),
            new KeybindCategories.Option("key.forward", "Walk Forwards", "minecraft:movement", "Movement"),
            new KeybindCategories.Option("key.mod.backpack", "Open Backpack", "example:equipment", "Example Equipment"),
            new KeybindCategories.Option("key.mod.tool", "Change Tool Mode", "example:equipment", "Example Equipment"));

    @Test public void registeredCategoriesGroupAndSortAutomatically() {
        var groups = KeybindCategories.group(OPTIONS, "", Set.of());
        assertEquals(List.of("example:equipment", "minecraft:movement"), groups.stream().map(KeybindCategories.Group::id).toList());
        assertEquals(List.of("Change Tool Mode", "Open Backpack"), groups.get(0).options().stream().map(KeybindCategories.Option::name).toList());
        assertEquals(4, groups.stream().mapToInt(group -> group.options().size()).sum());
    }
    @Test public void collapsedCategoriesStillParticipateInSearch() {
        var groups = KeybindCategories.group(OPTIONS, "backpack", Set.of("example:equipment"));
        assertEquals(1, groups.size()); assertEquals("key.mod.backpack", groups.get(0).options().get(0).id());
        assertFalse(groups.get(0).expanded());
        assertTrue(KeybindCategories.group(OPTIONS, "backpack", Set.of()).get(0).expanded());
    }
    @Test public void categoryNamesIdsAndMultipleTermsAreSearchable() {
        assertEquals(2, KeybindCategories.group(OPTIONS, "example equipment", Set.of()).get(0).options().size());
        assertEquals(1, KeybindCategories.group(OPTIONS, "EXAMPLE backpack", Set.of()).get(0).options().size());
        assertEquals("key.jump", KeybindCategories.group(OPTIONS, "key.jump", Set.of()).get(0).options().get(0).id());
        assertTrue(KeybindCategories.group(OPTIONS, "missing", Set.of()).isEmpty());
    }
    @Test public void identicalTranslatedCategoryNamesDoNotMergeDifferentMods() {
        var groups = KeybindCategories.group(List.of(
                new KeybindCategories.Option("a.open", "Open", "a:tools", "Tools"),
                new KeybindCategories.Option("b.open", "Open", "b:tools", "Tools")), "", Set.of("a:tools"));
        assertEquals(2, groups.size()); assertFalse(groups.get(0).expanded()); assertTrue(groups.get(groups.size() - 1).expanded());
    }
}
