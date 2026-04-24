package ru.cloud.client.modules.impl.player;

import lombok.Getter;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.MultiBooleanSetting;

@ModuleAnnotation(name = "NoPush", category = Category.PLAYER, description = "Удаляет коллизию от внешних факторов")
public final class NoPush extends Module {
    public static final NoPush INSTANCE = new NoPush();

    private NoPush() {}

    @Getter
    private final MultiBooleanSetting removePushFrom = new MultiBooleanSetting("Отключать для",
            new MultiBooleanSetting.Value("Энтити", true),
            new MultiBooleanSetting.Value("Воды и лавы", false),
            new MultiBooleanSetting.Value("Блоков", true)
    );
}
