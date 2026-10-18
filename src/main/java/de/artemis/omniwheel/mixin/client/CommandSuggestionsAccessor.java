package de.artemis.omniwheel.mixin.client;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(CommandSuggestions.class)
public interface CommandSuggestionsAccessor {
    @Accessor("suggestions")
    CommandSuggestions.SuggestionsList omniwheel$getSuggestions();

    @Accessor("commandUsagePosition")
    void omniwheel$setCommandUsagePosition(int position);

    @Accessor("commandUsage")
    List<FormattedCharSequence> omniwheel$getCommandUsage();

    @Accessor("commandUsageWidth")
    int omniwheel$getCommandUsageWidth();
}
