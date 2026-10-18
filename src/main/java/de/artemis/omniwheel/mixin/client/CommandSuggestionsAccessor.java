package de.artemis.omniwheel.mixin.client;

import net.minecraft.client.gui.CommandSuggestionHelper;
import net.minecraft.util.IReorderingProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(CommandSuggestionHelper.class)
public interface CommandSuggestionsAccessor {

    @Accessor("suggestions")
    CommandSuggestionHelper.Suggestions omniwheel$getSuggestions();

    @Accessor("commandUsagePosition")
    void omniwheel$setCommandUsagePosition(int position);

    @Accessor("commandUsage")
    List<IReorderingProcessor> omniwheel$getCommandUsage();

    @Accessor("commandUsageWidth")
    int omniwheel$getCommandUsageWidth();
}
