package de.artemis.omniwheel.client.overlay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Uses registered category IDs, so translated names and mod categories need no special cases. */
final class KeybindCategories {
    record Option(String id, String name, String categoryId, String categoryName) {}
    record Group(String id, String name, List<Option> options, boolean expanded) {}

    static List<Group> group(List<Option> options, String search, Set<String> collapsed) {
        String[] terms = (search == null ? "" : search).strip().toLowerCase(Locale.ROOT).split("\\s+");
        var grouped = new LinkedHashMap<String, List<Option>>();
        var sorted = options.stream().sorted(Comparator.comparing(Option::categoryName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Option::categoryId).thenComparing(Option::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Option::id)).toList();
        for (Option option : sorted) {
            String text = (option.name() + " " + option.id() + " " + option.categoryName() + " " + option.categoryId()).toLowerCase(Locale.ROOT);
            boolean matches = true;
            for (String term : terms) if (!text.contains(term)) { matches = false; break; }
            if (matches) grouped.computeIfAbsent(option.categoryId(), ignored -> new ArrayList<>()).add(option);
        }
        var result = new ArrayList<Group>();
        grouped.forEach((id, values) -> result.add(new Group(id, values.getFirst().categoryName(), List.copyOf(values), !collapsed.contains(id))));
        return List.copyOf(result);
    }
    private KeybindCategories() {}
}
