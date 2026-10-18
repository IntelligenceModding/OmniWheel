package de.artemis.omniwheel.common;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

public final class OmniWheelText {
    private OmniWheelText() {
    }

    public static String translate(String key, Object... args) {
        return I18n.get(key, args);
    }

    public static Component component(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static String resolve(String value) {
        return isTranslationKey(value) ? I18n.get(value) : value;
    }

    public static boolean isTranslationKey(String value) {
        return value != null && Language.getInstance().has(value);
    }
}
