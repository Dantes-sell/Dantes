package ru.cloud.base.comand.impl.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import ru.cloud.Zenith;
import ru.cloud.client.modules.api.Module;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModuleArgumentType implements ArgumentType<String> {

    public static ModuleArgumentType create() {
        return new ModuleArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString();
        Module module = Zenith.getInstance().getModuleManager().getModule(name);
        if (module == null) {
            throw new DynamicCommandExceptionType(
                    n -> Text.literal("Модуль не найден: " + n)
            ).create(name);
        }
        return name;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        List<String> names = Zenith.getInstance().getModuleManager().getModules()
                .stream().map(Module::getName).toList();
        return CommandSource.suggestMatching(names, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("Aura", "Fly", "AutoSprint");
    }
}
