package de.artemis.omniwheel.client.overlay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Uses registered category IDs, so translated names and mod categories need no special cases.
 */
final class KeybindCategories {

    final static class Option {

        private final String id;

        private final String name;

        private final String categoryId;

        private final String categoryName;

        Option(String id, String name, String categoryId, String categoryName) {
            this.id = id;
            this.name = name;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        public String categoryId() {
            return categoryId;
        }

        public String categoryName() {
            return categoryName;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value)
                return true;
            if (value == null || value.getClass() != getClass())
                return false;
            Option other = (Option) value;
            return true && java.util.Objects.equals(id, other.id) && java.util.Objects.equals(name, other.name) && java.util.Objects.equals(categoryId, other.categoryId) && java.util.Objects.equals(categoryName, other.categoryName);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name, categoryId, categoryName);
        }

        @Override
        public String toString() {
            return "Option[" + "id=" + id + ", name=" + name + ", categoryId=" + categoryId + ", categoryName=" + categoryName + "]";
        }
    }

    final static class Group {

        private final String id;

        private final String name;

        private final List<Option> options;

        private final boolean expanded;

        Group(String id, String name, List<Option> options, boolean expanded) {
            this.id = id;
            this.name = name;
            this.options = options;
            this.expanded = expanded;
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        public List<Option> options() {
            return options;
        }

        public boolean expanded() {
            return expanded;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value)
                return true;
            if (value == null || value.getClass() != getClass())
                return false;
            Group other = (Group) value;
            return true && java.util.Objects.equals(id, other.id) && java.util.Objects.equals(name, other.name) && java.util.Objects.equals(options, other.options) && java.util.Objects.equals(expanded, other.expanded);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name, options, expanded);
        }

        @Override
        public String toString() {
            return "Group[" + "id=" + id + ", name=" + name + ", options=" + options + ", expanded=" + expanded + "]";
        }
    }

    static List<Group> group(List<Option> options, String search, Set<String> collapsed) {
        String[] terms = de.artemis.omniwheel.common.Java8.strip((search == null ? "" : search)).toLowerCase(Locale.ROOT).split("\\s+");
        java.util.LinkedHashMap<java.lang.String, java.util.List<de.artemis.omniwheel.client.overlay.KeybindCategories.Option>> grouped = new LinkedHashMap<String, List<Option>>();
        java.util.List<de.artemis.omniwheel.client.overlay.KeybindCategories.Option> sorted = options.stream().sorted(Comparator.comparing(Option::categoryName, String.CASE_INSENSITIVE_ORDER).thenComparing(Option::categoryId).thenComparing(Option::name, String.CASE_INSENSITIVE_ORDER).thenComparing(Option::id)).collect(de.artemis.omniwheel.common.Java8.toList());
        for (Option option : sorted) {
            String text = (option.name() + " " + option.id() + " " + option.categoryName() + " " + option.categoryId()).toLowerCase(Locale.ROOT);
            boolean matches = true;
            for (String term : terms) if (!text.contains(term)) {
                matches = false;
                break;
            }
            if (matches)
                grouped.computeIfAbsent(option.categoryId(), ignored -> new ArrayList<>()).add(option);
        }
        java.util.ArrayList<de.artemis.omniwheel.client.overlay.KeybindCategories.Group> result = new ArrayList<Group>();
        grouped.forEach((id, values) -> result.add(new Group(id, values.get(0).categoryName(), de.artemis.omniwheel.common.Java8.copyList(values), !collapsed.contains(id))));
        return de.artemis.omniwheel.common.Java8.copyList(result);
    }

    private KeybindCategories() {
    }
}
