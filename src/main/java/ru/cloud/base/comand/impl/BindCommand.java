package ru.cloud.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import ru.cloud.Zenith;
import ru.cloud.base.comand.api.CommandAbstract;
import ru.cloud.base.comand.impl.args.KeyArgumentType;
import ru.cloud.base.comand.impl.args.ModuleArgumentType;
import ru.cloud.client.modules.api.Module;
import ru.cloud.utility.game.other.MessageUtil;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class BindCommand extends CommandAbstract {

    public BindCommand() {
        super("bind");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        // .bind <module> <key>
        builder.then(arg("module", ModuleArgumentType.create())
                .then(arg("key", KeyArgumentType.create()).executes(context -> {
                    String moduleName = context.getArgument("module", String.class);
                    int keyCode = context.getArgument("key", Integer.class);

                    Module module = Zenith.getInstance().getModuleManager().getModule(moduleName);
                    if (module == null) {
                        MessageUtil.displayMessage(MessageUtil.LogLevel.ERROR, "Модуль не найден: " + moduleName);
                        return SINGLE_SUCCESS;
                    }

                    module.setKeyCode(keyCode);

                    if (keyCode == -1) {
                        MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                                "§7Бинд с §f" + module.getName() + " §7снят");
                    } else {
                        MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                                "§f" + module.getName() + " §7привязан к §f" + KeyArgumentType.keyName(keyCode));
                    }
                    return SINGLE_SUCCESS;
                }))

                .then(literal("none").executes(context -> {
                    String moduleName = context.getArgument("module", String.class);
                    Module module = Zenith.getInstance().getModuleManager().getModule(moduleName);
                    if (module == null) {
                        MessageUtil.displayMessage(MessageUtil.LogLevel.ERROR, "Модуль не найден: " + moduleName);
                        return SINGLE_SUCCESS;
                    }
                    module.setKeyCode(-1);
                    MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                            "§7Бинд с §f" + module.getName() + " §7снят");
                    return SINGLE_SUCCESS;
                }))
        );

        // .bind list
        builder.then(literal("list").executes(context -> {
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§6Активные бинды:");
            boolean any = false;
            for (Module module : Zenith.getInstance().getModuleManager().getModules()) {
                if (module.getKeyCode() != -1) {
                    MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                            "  §f" + module.getName() + " §7-> §f" + KeyArgumentType.keyName(module.getKeyCode()));
                    any = true;
                }
            }
            if (!any) {
                MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "  §7Нет активных биндов");
            }
            return SINGLE_SUCCESS;
        }));
    }
}
