package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import ru.cloud.base.events.impl.input.EventKey;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.KeySetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.utility.game.player.MovingUtil;

@ModuleAnnotation(name = "WaterSpeed", category = Category.MOVEMENT, description = "Ускоряет ваше движение в воде")
public final class WaterSpeed extends Module {

    public static final WaterSpeed INSTANCE = new WaterSpeed();

    private final ModeSetting mode = new ModeSetting("Мод", "Matrix", "Grim", "MetaHvH");
    private final KeySetting boostKey = new KeySetting("Кнопка буста", -1, () -> mode.is("MetaHvH"));

    private long boostEndTime = 0L;
    private boolean isBoosting = false;

    private final float s20 = 0.7015F;
    private final float s0 = 0.595F;
    private final float s15 = 0.6499F;
    private final float s25 = 0.749F;

    private WaterSpeed() {
        mode.set("Matrix");
    }

    @EventTarget
    public void onKey(EventKey e) {
        if (!mode.is("MetaHvH")) return;
        if (boostKey.getKeyCode() == -1) return;

        if (e.isKeyDown(boostKey.getKeyCode())) {
            isBoosting = true;
            boostEndTime = System.currentTimeMillis() + 900L;
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (isBoosting && System.currentTimeMillis() > boostEndTime) {
            isBoosting = false;
        }

        if (mode.is("Grim")) {
            handleGrim();
            return;
        }

        if (mode.is("Matrix")) {
            handleMatrix();
            return;
        }

        if (mode.is("MetaHvH")) {
            handleMetaHvH();
        }
    }

    private void handleGrim() {
        if (!mc.player.isTouchingWater()) return;

        if (mc.options.jumpKey.isPressed()) {
            if (!mc.player.isSubmergedInWater()) {
                mc.player.setVelocity(mc.player.getVelocity().x, 0.2, mc.player.getVelocity().z);
                double current = Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z);
                MovingUtil.setVelocity(Math.max(current * 2.0, 0.08));
            } else {
                mc.player.setVelocity(mc.player.getVelocity().x, Math.max(mc.player.getVelocity().y, 0.03), mc.player.getVelocity().z);
            }
        }
    }

    private void handleMatrix() {
        if (mc.player.horizontalCollision || mc.player.verticalCollision) return;
        if (!(mc.player.isTouchingWater() && mc.player.isSwimming())) return;

        double y = mc.player.getVelocity().y;
        if (mc.options.jumpKey.isPressed()) {
            y += 0.05f;
        }
        if (mc.options.sneakKey.isPressed()) {
            y -= 0.05f;
        }

        double current = Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z);
        double applied = Math.max(current + 0.05, 0.05);
        MovingUtil.setVelocity(applied, y);
    }

    private void handleMetaHvH() {
        if (mc.player.horizontalCollision || mc.player.verticalCollision) return;
        if (!((mc.player.isTouchingWater() || mc.player.isInLava()) && mc.player.isSwimming())) return;

        ItemStack offHandItem = mc.player.getOffHandStack();
        String itemName = offHandItem.getName().getString();

        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        StatusEffectInstance slownessEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);

        float appliedSpeed;
        if (speedEffect != null && speedEffect.getAmplifier() >= 2) {
            appliedSpeed = getBaseSpeedByItem(itemName) * 1.14F;
        } else if (speedEffect != null && speedEffect.getAmplifier() >= 1) {
            appliedSpeed = getBaseSpeedByItem(itemName);
        } else {
            appliedSpeed = getBaseSpeedByItem(itemName) * 0.68F;
        }

        if (slownessEffect != null) {
            appliedSpeed *= 0.85F;
        }

        if (isBoosting) {
            appliedSpeed *= 1.75F;
        }

        MovingUtil.setVelocity(appliedSpeed);
    }

    private float getBaseSpeedByItem(String itemName) {
        if (containsAny(itemName,
                "Шар Геракла 2", "Шар CHAMPION", "Шар Аида 2",
                "Шар GOD", "КУБИК-РУБИК", "Шар BUNNY")) {
            return s20;
        }
        if (itemName.contains("Талисман Венома")) {
            return s25;
        }
        if (itemName.contains("Талисман Картеля")) {
            return s15;
        }
        return s0;
    }

    private boolean containsAny(String value, String... patterns) {
        for (String pattern : patterns) {
            if (value.contains(pattern)) return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        isBoosting = false;
        boostEndTime = 0L;
        super.onDisable();
    }
}
