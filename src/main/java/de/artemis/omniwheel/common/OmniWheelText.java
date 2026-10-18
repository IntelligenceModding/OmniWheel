package de.artemis.omniwheel.common;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.ITextComponent;

public final class OmniWheelText {

    private OmniWheelText() {
    }

    public static String translate(String key, Object... args) {
        return I18n.get(key, args);
    }

    public static ITextComponent component(String key, Object... args) {
        return new net.minecraft.util.text.TranslationTextComponent(key, args);
    }

    public static String resolve(String value) {
        if (isTranslationKey(value)) {
            return I18n.get(value);
        } else {
            return value;
        }
    }

    public static boolean isTranslationKey(String value) {
        return value != null && LanguageMap.getInstance().has(value);
    }
}
