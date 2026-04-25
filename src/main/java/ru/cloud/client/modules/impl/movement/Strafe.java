package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.events.impl.player.EventDamage;
import ru.cloud.base.events.impl.player.EventMove;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.math.Timer;

@ModuleAnnotation(name = "Strafe", category = Category.MOVEMENT, description = "Settings for Strafe")
public final class Strafe extends Module {

    public static final Strafe INSTANCE = new Strafe();
    private Strafe() {}

    private final ModeSetting mode = new ModeSetting("Режим");
    private final ModeSetting.Value modeDefault = new ModeSetting.Value(mode, "Default").select();
    private final ModeSetting.Value modeMatrix  = new ModeSetting.Value(mode, "Matrix");
    private final ModeSetting.Value modeMetaHvh = new ModeSetting.Value(mode, "MetaHVH");

    private final BooleanSetting dmgBoost   = new BooleanSetting("D mg Bo os t", "D mg Bo os t", false);
    private final NumberSetting  boostSpeed = new NumberSetting("B oo st Sp ee d", 0.7f, 0.1f, 5.0f, 0.1f, dmgBoost::isEnabled);
    private final NumberSetting  boostDuration = new NumberSetting("B oo st Du ra ti on", 500f, 100f, 2000f, 50f, dmgBoost::isEnabled);

    private final Timer boostTimer = new Timer();
    private boolean boosted = false;

    @Override
    public void onEnable() {
        super.onEnable();
        boosted = false;
    }

    @EventTarget
    public void onDamage(EventDamage e) {
        if (!dmgBoost.isEnabled()) return;
        boosted = true;
        boostTimer.reset();
    }

    @EventTarget
    public void onMove(EventMove e) {
        if (mc.player == null) return;
        if (!isMoving()) return;

        double spd = calcSpeed();

        // ���� ������� ���� �� ���� ������
        if (dmgBoost.isEnabled() && boosted) {
            if (boostTimer.finished(boostDuration.getCurrent())) {
                boosted = false;
            } else {
                spd += boostSpeed.getCurrent();
            }
        }

        if (modeMatrix.isSelected()) {
            double matrixSpd = 0.25 - Math.random() * 0.001;
            applyMove(e, matrixSpd);
            return;
        }

        if (modeMetaHvh.isSelected()) {
            spd *= 1.2;
        }

        applyMove(e, spd);
    }

    private double calcSpeed() {
        double spd = 0.2873;
        StatusEffectInstance eff = mc.player.getStatusEffect(StatusEffects.SPEED);
        if (eff != null) spd *= 1.0 + 0.2 * (eff.getAmplifier() + 1);
        if (Speed.INSTANCE.isEnabled()) spd *= 1.0 + Speed.INSTANCE.getSpeedFactor();
        return spd;
    }

    private void applyMove(EventMove e, double spd) {
        float yaw = mc.player.getYaw(1.0f);
        float fwd = getFwd();
        float str = getStr();
        if (fwd == 0 && str == 0) return;

        double rad = Math.toRadians(yaw);
        double x = (str * Math.cos(rad) - fwd * Math.sin(rad)) * spd;
        double z = (str * Math.sin(rad) + fwd * Math.cos(rad)) * spd;

        e.setMovePos(new Vec3d(x, e.getMovePos().y, z));
    }

    private boolean isMoving() {
        return mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed();
    }

    private float getFwd() {
        return (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
    }

    private float getStr() {
        return (mc.options.leftKey.isPressed() ? 1 : 0) - (mc.options.rightKey.isPressed() ? 1 : 0);
    }
}

