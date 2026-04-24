package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.MultiBooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
        name = "ElytraResolver",
        category = Category.MOVEMENT,
        description = "Меняет ваше движение так, чтобы по вам не могли ударить на элитрах"
)
public final class ElytraResolver extends Module {
    public static final ElytraResolver INSTANCE = new ElytraResolver();

    private ElytraResolver() {}

    private final MultiBooleanSetting vector = new MultiBooleanSetting(
            "Векторы лива",
            new MultiBooleanSetting.Value("Вверх", true),
            new MultiBooleanSetting.Value("Вниз", false),
            new MultiBooleanSetting.Value("Восток", true),
            new MultiBooleanSetting.Value("Запад", true),
            new MultiBooleanSetting.Value("Юг", true),
            new MultiBooleanSetting.Value("Север", true)
    );

    private final NumberSetting elytraDistance = new NumberSetting("Дистанция", 4.5F, 3.0F, 8F, 0.5F);

    private final BooleanSetting skipVector = new BooleanSetting("Исключать столкновение", true);
    private final BooleanSetting autoF = new BooleanSetting("Авто фейерверк", true);
    private final BooleanSetting freezeDummy = new BooleanSetting("Замораживать игрока", true);

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;
        if (!mc.player.isGliding()) return;

        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;

        if (vector.isEnable("Вверх")) y += 0.01D;
        if (vector.isEnable("Вниз")) y -= 0.01D;
        if (vector.isEnable("Восток")) x += 0.01D;
        if (vector.isEnable("Запад")) x -= 0.01D;
        if (vector.isEnable("Юг")) z += 0.01D;
        if (vector.isEnable("Север")) z -= 0.01D;

        if (skipVector.isEnabled()) {
            Vec3d velocity = mc.player.getVelocity().add(x, y, z);
            mc.player.setVelocity(velocity);
        }
    }
}
