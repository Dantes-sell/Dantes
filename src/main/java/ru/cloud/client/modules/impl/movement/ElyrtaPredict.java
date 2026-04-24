package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.math.StopWatch;

@ModuleAnnotation(
        name = "ElytraSample",
        category = Category.MOVEMENT,
        description = "Смещает хитбокс противника во время полёта на элитрах для перегона"
)
public final class ElyrtaPredict extends Module {
    public static final ElyrtaPredict INSTANCE = new ElyrtaPredict();

    private ElyrtaPredict() {}

    public final NumberSetting elytraDistance = new NumberSetting("Дистанция обгона", 3.0F, 0.0F, 4.25F, 0.05F);
    public final StopWatch timer = new StopWatch();
    public boolean disabled = false;

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;
        if (!mc.player.isGliding()) return;
    }

    public double getElytraDistance(LivingEntity target) {
        return elytraDistance.getCurrent();
    }

    public boolean canPredict(LivingEntity target) {
        if (mc.player == null || target == null) return false;

        if (mc.player.hurtTime > 0 && target.handSwinging) {
            disabled = true;
            timer.reset();
        }

        if (timer.getElapsedTime() >= 500L) {
            disabled = false;
        }

        return !disabled;
    }
}
