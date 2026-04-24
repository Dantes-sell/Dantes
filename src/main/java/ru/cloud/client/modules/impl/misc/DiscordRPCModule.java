package ru.cloud.client.modules.impl.misc;

import ru.cloud.Zenith;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "DiscordRPC", category = Category.MISC, description = "Показывает статус в Discord")
public final class DiscordRPCModule extends Module {
    public static final DiscordRPCModule INSTANCE = new DiscordRPCModule();

    private DiscordRPCModule() {}

    @Override
    public void onEnable() {
        super.onEnable();
        Zenith.getInstance().getDiscordRPC().init();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Zenith.getInstance().getDiscordRPC().stopRPC();
    }
}
