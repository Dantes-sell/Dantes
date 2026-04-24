package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.events.impl.input.EventKey;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.server.EventPacket;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.KeySetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.player.MovingUtil;

@ModuleAnnotation(name = "Timer", category = Category.MOVEMENT, description = "Регулирует темп движения")
public final class Timer extends Module {

    public static final Timer INSTANCE = new Timer();

    private final ModeSetting mode = new ModeSetting("Mode", "Обычный", "Grim");
    private final KeySetting grimBind = new KeySetting("Кнопка буста", -1, () -> mode.is("Grim"));
    private final NumberSetting timerAmount = new NumberSetting("Скорость", 2.0f, 1.0f, 5.0f, 0.025f);

    private final BooleanSetting smart = new BooleanSetting("Умный", true, () -> !mode.is("Grim"));
    private final BooleanSetting movingUp = new BooleanSetting("Добавлять в движении", false, () -> !mode.is("Grim"));
    private final NumberSetting upValue = new NumberSetting("Значение", 0.02f, 0.01f, 0.5f, 0.01f,
            () -> !mode.is("Grim") && movingUp.isEnabled());
    private final NumberSetting ticks = new NumberSetting("Скорость убывания", 1.0f, 0.15f, 3.0f, 0.1f,
            () -> !mode.is("Grim"));

    private final ru.cloud.utility.math.Timer timerUtil = new ru.cloud.utility.math.Timer();

    public float maxViolation = 100.0f;
    private float violation = 0.0f;
    private double prevPosX, prevPosY, prevPosZ;
    private float prevYaw;
    private float prevPitch;
    private boolean isBoost;

    private Timer() {
    }

    @EventTarget
    public void onKey(EventKey e) {
        if (!mode.is("Grim")) return;
        if (grimBind.getKeyCode() == -1) return;
        if (e.isKeyDown(grimBind.getKeyCode())) {
            isBoost = true;
        }
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (!mode.is("Grim") || mc.player == null) return;

        if (e.isReceive()) {
            if (e.getPacket() instanceof PlayerPositionLookS2CPacket && isBoost) {
                resetSpeed();
                reset();
            }

            if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket velocityPacket
                    && velocityPacket.getEntityId() == mc.player.getId()) {
                reset();
                resetSpeed();
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        handleEventUpdate();
        updateTimer(mc.player.getYaw(), mc.player.getPitch(), mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private void handleEventUpdate() {
        if (timerUtil.finished(25000L)) {
            reset();
            timerUtil.reset();
        }

        if (!mc.player.isOnGround() && !isBoost) {
            violation += 0.1f;
            float limit = maxViolation / (mode.is("Grim") ? 1.0f : timerAmount.getCurrent());
            violation = MathHelper.clamp(violation, 0.0f, limit);
        }

        if (mode.is("Grim") && !isBoost) {
            return;
        }

        applyMovementBoost();

        if (!smart.isEnabled() || timerAmount.getCurrent() <= 1.0f) {
            return;
        }

        float limit = maxViolation / (mode.is("Grim") ? 1.0f : timerAmount.getCurrent());
        if (violation < limit) {
            violation += mode.is("Grim") ? 0.05f : ticks.getCurrent();
            violation = MathHelper.clamp(violation, 0.0f, limit);
        } else {
            resetSpeed();
        }
    }

    private void applyMovementBoost() {
        if (!MovingUtil.hasPlayerMovement()) return;

        float speedMul = Math.max(1.0f, timerAmount.getCurrent());
        double bonus = (speedMul - 1.0f) * 0.045;
        double[] direction = MovingUtil.calculateDirection(bonus);
        Vec3d velocity = mc.player.getVelocity();

        double y = velocity.y;
        if (movingUp.isEnabled() && !mode.is("Grim")) {
            y += upValue.getCurrent();
        }

        mc.player.setVelocity(velocity.x + direction[0], y, velocity.z + direction[1]);
    }

    public void updateTimer(float yaw, float pitch, double posX, double posY, double posZ) {
        if (notMoving()) {
            if (mode.is("Grim")) {
                violation -= 0.05f;
            } else {
                violation -= (ticks.getCurrent() + 0.4f);
            }
        } else if (movingUp.isEnabled() && !mode.is("Grim")) {
            violation -= upValue.getCurrent();
        }

        violation = MathHelper.clamp(violation, 0.0f, (float) Math.floor(maxViolation));

        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        prevYaw = yaw;
        prevPitch = pitch;
    }

    private boolean notMoving() {
        return prevPosX == mc.player.getX()
                && prevPosY == mc.player.getY()
                && prevPosZ == mc.player.getZ()
                && prevYaw == mc.player.getYaw()
                && prevPitch == mc.player.getPitch();
    }

    public float getViolation() {
        return violation;
    }

    public void resetSpeed() {
        isBoost = false;
        violation = 0.0f;
    }

    public void reset() {
        if (mode.is("Grim")) {
            violation = maxViolation / timerAmount.getCurrent();
            isBoost = false;
        } else {
            violation = 0.0f;
        }
    }

    @Override
    public void onDisable() {
        reset();
        timerUtil.reset();
        super.onDisable();
    }

    @Override
    public void onEnable() {
        reset();
        timerUtil.reset();
        super.onEnable();
    }
}
