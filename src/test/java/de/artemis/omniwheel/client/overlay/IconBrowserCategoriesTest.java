package de.artemis.omniwheel.client.overlay;

import org.junit.Test;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;
import de.artemis.omniwheel.client.overlay.OmniWheelProfilesOverlay.IconCategory;
import de.artemis.omniwheel.client.overlay.OmniWheelProfilesOverlay.IconPickerOption;

public class IconBrowserCategoriesTest {
    private static IconCategory category(String id, String name, String... icons) {
        return new IconCategory(id, name, java.util.Arrays.stream(icons)
                .map(icon -> new IconPickerOption(icon, icon)).toList(), 0, false);
    }

    @Test public void collapsedCategoriesKeepHeadersAndNoCellsInLayout() {
        var categories = List.of(category("blocks", "Blocks", "stone", "dirt", "sand"), category("items", "Items", "apple"));
        var open = OmniWheelProfilesOverlay.layoutIconCategories(categories, Set.of(), "", 2);
        var closed = OmniWheelProfilesOverlay.layoutIconCategories(categories, Set.of("blocks"), "", 2);
        assertEquals(83, open.get(1).top());
        assertEquals(29, closed.get(1).top());
        assertFalse(closed.getFirst().expanded());
    }

    @Test public void searchIncludesCollapsedCategoriesAndMatchesAllTerms() {
        var categories = List.of(category("mod:mekanism", "Mekanism", "mekanism:steel_block", "mekanism:steel_ingot"), category("blocks", "Blocks", "minecraft:stone"));
        var results = OmniWheelProfilesOverlay.layoutIconCategories(categories, Set.of("mod:mekanism"), "MEKANISM steel", 4);
        assertEquals(1, results.size());
        assertEquals(2, results.getFirst().options().size());
        assertTrue(results.getFirst().expanded());
        assertFalse(OmniWheelProfilesOverlay.layoutIconCategories(categories, Set.of("mod:mekanism"), "", 4).getFirst().expanded());
    }

    @Test public void categoryNamesAreSearchableAndEmptyMatchesDisappear() {
        var categories = List.of(category("mod:example", "Example Machines", "example:widget", "example:frame"));
        assertEquals(2, OmniWheelProfilesOverlay.layoutIconCategories(categories, Set.of(), "machines", 1).getFirst().options().size());
        assertTrue(OmniWheelProfilesOverlay.layoutIconCategories(categories, Set.of(), "absent", 1).isEmpty());
    }
}
