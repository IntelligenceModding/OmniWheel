package de.artemis.omniwheel.common;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class OmniWheelText {
    private OmniWheelText() {
    }

    public static String translate(String key, Object... args) {
        return I18n.get(key, args);
    }

    public static ITextComponent component(String key, Object... args) {
        return new TranslationTextComponent(key, args);
    }

    public static String resolve(String value) {
        return isTranslationKey(value) ? I18n.get(value) : value;
    }

    public static boolean isTranslationKey(String value) {
        return value != null && I18n.exists(value);
    }
}
