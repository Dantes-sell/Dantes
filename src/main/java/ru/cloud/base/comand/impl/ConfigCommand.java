package ru.cloud.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import ru.cloud.Zenith;
import ru.cloud.base.comand.api.CommandAbstract;
import ru.cloud.base.comand.impl.args.ConfigArgumentType;
import ru.cloud.base.comand.impl.args.PlayerArgumentType;
import ru.cloud.base.config.ConfigManager;
import ru.cloud.utility.game.other.MessageUtil;

import java.io.File;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ConfigCommand extends CommandAbstract {
    public ConfigCommand() {
        super("config");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {

        // .config save [name]
        builder.then(literal("save")
                .executes(context -> {
                    doSave("current_config");
                    return SINGLE_SUCCESS;
                })
                .then(arg("name", PlayerArgumentType.create()).executes(context -> {
                    String name = context.getArgument("name", String.class);
                    doSave(name);
                    return SINGLE_SUCCESS;
                }))
        );

        // .config load [name]
        builder.then(literal("load")
                .executes(context -> {
                    doLoad("current_config");
                    return SINGLE_SUCCESS;
                })
                .then(arg("name", ConfigArgumentType.create()).executes(context -> {
                    String name = context.getArgument("name", String.class);
                    doLoad(name);
                    return SINGLE_SUCCESS;
                }))
        );

        // .config delete <name>
        builder.then(literal("delete")
                .then(arg("name", ConfigArgumentType.create()).executes(context -> {
                    String name = context.getArgument("name", String.class);
                    if (name.equals("current_config")) {
                        MessageUtil.displayMessage(MessageUtil.LogLevel.WARN, "Нельзя удалить current_config");
                        return SINGLE_SUCCESS;
                    }
                    boolean ok = Zenith.getInstance().getConfigManager().deleteConfig(name);
                    if (ok) {
                        MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§aКонфиг §f" + name + " §aудалён");
                    } else {
                        MessageUtil.displayMessage(MessageUtil.LogLevel.ERROR, "Не удалось удалить конфиг: " + name);
                    }
                    return SINGLE_SUCCESS;
                }))
        );

        // .config list
        builder.then(literal("list").executes(context -> {
            List<String> names = Zenith.getInstance().getConfigManager().configNames();
            if (names.isEmpty()) {
                MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§7Нет сохранённых конфигов");
            } else {
                MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§6Конфиги (" + names.size() + "):");
                for (String n : names) {
                    MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "  §f" + n);
                }
            }
            return SINGLE_SUCCESS;
        }));

        // .config dir
        builder.then(literal("dir").executes(context -> {
            File dir = ConfigManager.configDirectory;
            try {
                dir.mkdirs();
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"explorer.exe", dir.getAbsolutePath()});
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"open", dir.getAbsolutePath()});
                } else {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", dir.getAbsolutePath()});
                }
                MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§aОткрыта папка конфигов");
            } catch (Exception e) {
                MessageUtil.displayMessage(MessageUtil.LogLevel.ERROR, "Не удалось открыть папку: " + e.getMessage());
            }
            return SINGLE_SUCCESS;
        }));

        // .config help
        builder.then(literal("help").executes(context -> {
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§6.config save §7[название] §8- сохранить конфиг");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§6.config load §7<название> §8- загрузить конфиг");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§6.config delete §7<название> §8- удалить конфиг");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§6.config list §8- список конфигов");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§6.config dir §8- открыть папку с конфигами");
            return SINGLE_SUCCESS;
        }));
    }

    private void doSave(String name) {
        boolean ok = Zenith.getInstance().getConfigManager().saveConfig(name);
        if (ok) {
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§aКонфиг §f" + name + " §aсохранён");
        } else {
            MessageUtil.displayMessage(MessageUtil.LogLevel.ERROR, "Ошибка при сохранении конфига: " + name);
        }
    }

    private void doLoad(String name) {
        boolean ok = Zenith.getInstance().getConfigManager().loadConfig(name);
        if (ok) {
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§aКонфиг §f" + name + " §aзагружен");
        } else {
            MessageUtil.displayMessage(MessageUtil.LogLevel.ERROR, "Конфиг не найден: " + name);
        }
    }
}
