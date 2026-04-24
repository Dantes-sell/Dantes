package ru.cloud.base.comand.impl.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import ru.cloud.Zenith;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConfigArgumentType implements ArgumentType<String> {

    public static ConfigArgumentType create() {
        return new ConfigArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readUnquotedString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(Zenith.getInstance().getConfigManager().configNames(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("pvp", "default", "current_config");
    }
}
