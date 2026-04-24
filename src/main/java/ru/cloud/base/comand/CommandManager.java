package ru.cloud.base.comand;

import com.mojang.brigadier.CommandDispatcher;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import ru.cloud.base.comand.api.CommandAbstract;
import ru.cloud.base.comand.impl.BindCommand;
import ru.cloud.base.comand.impl.FakePlayerCommand;
import ru.cloud.base.comand.impl.FriendCommand;
import ru.cloud.base.comand.impl.MacroCommand;
import ru.cloud.base.comand.impl.ClipCommand;
import ru.cloud.base.comand.impl.ConfigCommand;
import ru.cloud.base.comand.impl.RCTCommand;
import ru.cloud.base.comand.impl.BindCommand;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandManager {
    private String prefix = ".";


    private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();

    private final CommandSource source = new ClientCommandSource(null, MinecraftClient.getInstance());

    private final List<CommandAbstract> commands = new ArrayList<>();

    public CommandManager() {


        registerCommand(new FriendCommand());
        registerCommand(new MacroCommand());
        registerCommand(new ClipCommand());
        registerCommand(new ConfigCommand());
        registerCommand(new RCTCommand());
        registerCommand(new BindCommand());
        registerCommand(new FakePlayerCommand());

    }


    public void registerCommand(CommandAbstract command) {
        if (command == null) return;

        command.register(dispatcher);
        this.commands.add(command);
    }
}