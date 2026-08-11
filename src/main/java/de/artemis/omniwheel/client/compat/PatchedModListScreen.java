package de.artemis.omniwheel.client.compat;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.StringUtil;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.gui.widget.ModListWidget;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class PatchedModListScreen extends ModListScreen {
    private static final Field MOD_LIST_FIELD = field("modList");
    private static final Field SELECTED_FIELD = field("selected");
    private static final Field MODS_FIELD = field("mods");
    private static final Field UNSORTED_MODS_FIELD = field("unsortedMods");
    private static final Field SEARCH_FIELD = field("search");
    private static final Field LAST_FILTER_TEXT_FIELD = field("lastFilterText");
    private static final Field SORTED_FIELD = field("sorted");
    private static final Field SORT_TYPE_FIELD = field("sortType");
    private static final Method UPDATE_CACHE_METHOD = method("updateCache");

    public PatchedModListScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void tick() {
        ModListWidget modList = getField(MOD_LIST_FIELD, this, ModListWidget.class);
        ModListWidget.ModEntry selected = getField(SELECTED_FIELD, this, ModListWidget.ModEntry.class);
        EditBox search = getField(SEARCH_FIELD, this, EditBox.class);
        String filterText = getField(LAST_FILTER_TEXT_FIELD, this, String.class);

        modList.setSelected(selected);

        if (!search.getValue().equals(filterText)) {
            reloadMods(search);
            setField(SORTED_FIELD, this, false);
        }

        if (!getBooleanField(SORTED_FIELD, this)) {
            reloadMods(search);
            List<ModContainer> mods = getField(MODS_FIELD, this, List.class);
            Comparator<ModContainer> sortType = (Comparator<ModContainer>) getField(SORT_TYPE_FIELD, this, Comparator.class);
            mods.sort(sortType);
            modList.refreshList();

            selected = getField(SELECTED_FIELD, this, ModListWidget.ModEntry.class);
            if (selected != null) {
                ModListWidget.ModEntry previous = selected;
                ModListWidget.ModEntry refreshed = modList.children().stream()
                        .filter(entry -> entry.getInfo() == previous.getInfo())
                        .findFirst()
                        .orElse(null);
                setField(SELECTED_FIELD, this, refreshed);
                invoke(UPDATE_CACHE_METHOD, this);
            }

            setField(SORTED_FIELD, this, true);
        }
    }

    @SuppressWarnings("unchecked")
    private void reloadMods(EditBox search) {
        List<ModContainer> unsortedMods = getField(UNSORTED_MODS_FIELD, this, List.class);
        String searchText = search.getValue().toLowerCase(Locale.ROOT);
        List<ModContainer> filteredMods = new ArrayList<>(unsortedMods.stream()
                .filter(mod -> StringUtil.stripColor(mod.getModInfo().getDisplayName()).toLowerCase(Locale.ROOT).contains(searchText))
                .toList());
        setField(MODS_FIELD, this, filteredMods);
        setField(LAST_FILTER_TEXT_FIELD, this, search.getValue());
    }

    private static Field field(String name) {
        try {
            Field field = ModListScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to access ModListScreen field: " + name, exception);
        }
    }

    private static Method method(String name, Class<?>... parameterTypes) {
        try {
            Method method = ModListScreen.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to access ModListScreen method: " + name, exception);
        }
    }

    private static boolean getBooleanField(Field field, Object target) {
        try {
            return field.getBoolean(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to read ModListScreen boolean field: " + field.getName(), exception);
        }
    }

    private static <T> T getField(Field field, Object target, Class<T> type) {
        try {
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to read ModListScreen field: " + field.getName(), exception);
        }
    }

    private static void setField(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to write ModListScreen field: " + field.getName(), exception);
        }
    }

    private static void invoke(Method method, Object target, Object... arguments) {
        try {
            method.invoke(target, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke ModListScreen method: " + method.getName(), exception);
        }
    }
}
