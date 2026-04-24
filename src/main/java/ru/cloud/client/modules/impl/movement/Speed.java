package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.player.MovingUtil;

@ModuleAnnotation(name = "Speed", category = Category.MOVEMENT, description = "Ускоряет ваше движение")
public final class Speed extends Module {
    public static final Speed INSTANCE = new Speed();

    private final ModeSetting mode = new ModeSetting("Режим", "Collision", "Matrix", "MetaHvH", "HolyWorld");
    private final NumberSetting matrixSpeed = new NumberSetting("Скорость", 0.36f, 0.10f, 0.7f, 0.01f, () -> mode.is("Matrix"));
    private final NumberSetting collisionSpeed = new NumberSetting("Скорость", 1.1f, 0.5f, 2.0f, 0.05f, () -> mode.is("Collision"));
    private final NumberSetting holyWorldSpeed = new NumberSetting("Скорость", 1.35f, 1.1f, 4.0f, 0.05f, () -> mode.is("HolyWorld"));
    private final NumberSetting collisionDistance = new NumberSetting("Дистанция", 0.244f, 0.2f, 0.95f, 0.01f, () -> mode.is("HolyWorld"));
    private final BooleanSetting autoJump = new BooleanSetting("Авто прыжок", true, () -> mode.is("Matrix") || mode.is("MetaHvH"));

    private Speed() {
        mode.set("Collision");
    }

    public double getSpeedFactor() {
        return switch (mode.get()) {
            case "Matrix" -> matrixSpeed.getCurrent();
            case "Collision" -> collisionSpeed.getCurrent() * 0.1;
            case "HolyWorld" -> holyWorldSpeed.getCurrent() * 0.01;
            case "MetaHvH" -> 0.358f;
            default -> 0.0;
        };
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.player.getAbilities().flying) {
            return;
        }

        switch (mode.get()) {
            case "Matrix" -> handleMatrix();
            case "MetaHvH" -> handleMetaHvH();
            case "Collision" -> handleCollision();
            case "HolyWorld" -> handleHolyWorld();
            default -> {
            }
        }
    }

    private void handleMatrix() {
        if (canUseAirSpeed()) {
            setHorizontalSpeed(matrixSpeed.getCurrent());
        }

        handleAutoJump();
    }

    private void handleMetaHvH() {
        float currentSpeed = 0.358f;
        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        if (speedEffect != null) {
            int amplifier = speedEffect.getAmplifier();
            if (amplifier == 0) {
                currentSpeed *= 1.2630f;
            } else if (amplifier == 1) {
                currentSpeed *= 1.4530f;
            } else if (amplifier >= 2) {
                currentSpeed *= 1.6520f;
            }
        }

        if (canUseAirSpeed()) {
            setHorizontalSpeed(currentSpeed);
        }

        handleAutoJump();
    }

    private void handleCollision() {
        Box aabb = mc.player.getBoundingBox().expand(0.1);
        boolean canBoost = mc.world.getEntitiesByClass(LivingEntity.class, aabb, entity -> entity != mc.player).size() > 1;
        if (canBoost && !mc.player.isOnGround() && MovingUtil.hasPlayerMovement()) {
            float factor = collisionSpeed.getCurrent() / 10.0f;
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x * factor, velocity.y, velocity.z * factor);
        }
    }

    private void handleHolyWorld() {
        int collisions = 0;
        Box expandedBox = mc.player.getBoundingBox().expand(collisionDistance.getCurrent());
        boolean canBoost = mc.world.getEntitiesByClass(LivingEntity.class, expandedBox, entity -> entity != mc.player).size() > 1;
        if (canBoost) {
            collisions++;
        }

        if (collisions <= 0 || !MovingUtil.hasPlayerMovement()) {
            return;
        }

        double[] motion = MovingUtil.calculateDirection(holyWorldSpeed.getCurrent() * 2.05f * 0.01 * collisions);
        mc.player.addVelocity(motion[0], 0.0, motion[1]);
    }

    private void handleAutoJump() {
        if (!autoJump.isEnabled()) {
            return;
        }

        if (MovingUtil.hasPlayerMovement() && mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
            mc.player.jump();
        }
    }

    private boolean canUseAirSpeed() {
        return !mc.player.isGliding()
                && !mc.player.isTouchingWater()
                && !mc.player.getWorld().getFluidState(mc.player.getBlockPos()).isIn(FluidTags.WATER)
                && !mc.player.isOnGround()
                && MovingUtil.hasPlayerMovement();
    }

    private void setHorizontalSpeed(float speed) {
        double[] direction = MovingUtil.calculateDirection(speed);
        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(direction[0], velocity.y, direction[1]);
    }
}
