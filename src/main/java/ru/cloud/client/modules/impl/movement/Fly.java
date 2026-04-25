package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(name = "Fly", category = Category.MOVEMENT, description = "Settings for Fly")
public final class Fly extends Module {

    public static final Fly INSTANCE = new Fly();
    private Fly() {}

    private final ModeSetting mode = new ModeSetting("Режим");
    private final ModeSetting.Value vanilla  = new ModeSetting.Value(mode, "Vanilla").select();
    private final ModeSetting.Value velocity = new ModeSetting.Value(mode, "Velocity");
    private final ModeSetting.Value glide    = new ModeSetting.Value(mode, "Glide");

    private final NumberSetting speed = new NumberSetting("S pe ed", 1.0f, 0.1f, 50f, 0.1f);

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null) return;
        mc.player.getAbilities().allowFlying = true;
        mc.player.getAbilities().flying = true;
        mc.player.sendAbilitiesUpdate();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player == null) return;
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().allowFlying = false;
            mc.player.getAbilities().flying = false;
            mc.player.sendAbilitiesUpdate();
        }
        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(vel.x, 0, vel.z);
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;

        float spd = speed.getCurrent();

        // ������ allowFlying ����� �� ���� ����� �� �������
        if (!mc.player.getAbilities().allowFlying) {
            mc.player.getAbilities().allowFlying = true;
            mc.player.getAbilities().flying = true;
            mc.player.sendAbilitiesUpdate();
        }

        float yaw = (float) Math.toRadians(mc.player.getYaw());
        double forward = mc.options.forwardKey.isPressed() ? 1 : mc.options.backKey.isPressed() ? -1 : 0;
        double strafe  = mc.options.leftKey.isPressed()   ? 1 : mc.options.rightKey.isPressed() ? -1 : 0;

        double motX = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * spd * 0.1;
        double motZ = ( Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * spd * 0.1;

        double motY;
        if (mc.options.jumpKey.isPressed())       motY =  spd * 0.1;
        else if (mc.options.sneakKey.isPressed()) motY = -spd * 0.1;
        else                                      motY =  0;

        if (glide.isSelected()) {
            Vec3d cur = mc.player.getVelocity();
            mc.player.setVelocity(
                    lerp(cur.x, motX, 0.3),
                    motY == 0 ? cur.y * 0.6 : motY,
                    lerp(cur.z, motZ, 0.3)
            );
        } else {
            
            mc.player.setVelocity(motX, motY, motZ);
        }

        mc.player.fallDistance = 0;
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}


