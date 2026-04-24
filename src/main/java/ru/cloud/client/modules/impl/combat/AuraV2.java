package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.events.impl.player.EventMoveInput;
import ru.cloud.base.events.impl.player.EventRotate;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.player.AttackUtil;
import ru.cloud.base.rotation.RotationTarget;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.player.MovingUtil;
import ru.cloud.utility.game.player.rotation.Rotation;
import ru.cloud.utility.game.player.rotation.RotationUtil;
import ru.cloud.utility.math.Timer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(name = "AuraV2", category = Category.COMBAT, description = "Улучшенная аура")
public final class AuraV2 extends Module {

    public static final AuraV2 INSTANCE = new AuraV2();

    private final ModeSetting targets = new ModeSetting("Targets", "Players", "Team", "Animals", "Mobs");
    private final BooleanSetting invisible = new BooleanSetting("Invisible", true);
    private final BooleanSetting nakedOnly = new BooleanSetting("NakedOnly", false);

    private final ModeSetting sortMode = new ModeSetting("Sort", "Adaptive", "Distance", "Health", "Aim");
    private final ModeSetting rotationMode = new ModeSetting("Rotation", "Normal", "FunTime", "HollyWorld", "SpookyTime", "Matrix", "Snaps", "LegitStand", "Grim");
    private final ModeSetting correction = new ModeSetting("Correction", "Targeted", "Free", "None");

    private final NumberSetting distance = new NumberSetting("Distance", 3.0f, 1.0f, 6.0f, 0.1f);
    private final NumberSetting attackDelay = new NumberSetting("AttackDelay", 458.0f, 0.0f, 1000.0f, 1.0f);

    private final BooleanSetting shieldBreak = new BooleanSetting("ShieldBreak", true);
    private final BooleanSetting onlyCrit = new BooleanSetting("OnlyCrit", false);

    @Getter
    private LivingEntity target;

    private final Timer attackTimer = new Timer();
    private final Random random = new Random();
    private float lastYaw;
    private float lastPitch;

    private AuraV2() {
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        target = updateTarget();

        if (target == null || !canAttack() || !attackTimer.finished((long) attackDelay.getCurrent())) {
            return;
        }

        if (shieldBreak.isEnabled() && target instanceof PlayerEntity playerTarget && playerTarget.isBlocking()) {
            int axeSlot = findAxeInHotbar();
            if (axeSlot != -1) {
                int previousSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = axeSlot;
                AttackUtil.attackEntity(target);
                mc.player.getInventory().selectedSlot = previousSlot;
            } else {
                AttackUtil.attackEntity(target);
            }
        } else {
            AttackUtil.attackEntity(target);
        }

        attackTimer.reset();
    }

    @EventTarget
    public void onRotate(EventRotate event) {
        if (mc.player == null || mc.world == null || target == null || !isValid(target)) {
            return;
        }

        Vec3d eyes = mc.player.getEyePos();
        Box targetBox = target.getBoundingBox();
        Rotation angle = RotationUtil.fromVec3d(targetBox.getCenter().subtract(eyes));

        Rotation computed;
        if (rotationMode.is("Normal")) {
            computed = computeNormalRotation(angle);
        } else if (rotationMode.is("FunTime") || rotationMode.is("HollyWorld")) {
            computed = computeFunTimeAndHollyRotation(angle);
        } else if (rotationMode.is("SpookyTime") || rotationMode.is("Matrix") || rotationMode.is("Snaps")) {
            computed = computeSmoothHalfRotation(angle);
        } else {
            computed = computeLegitStandAndGrimRotation(eyes, targetBox);
        }

        lastYaw = computed.getYaw();
        lastPitch = computed.getPitch();

        rotationManager.setRotation(
                new RotationTarget(computed, () -> aimManager.rotate(aimManager.getInstantSetup(), computed), aimManager.getInstantSetup()),
                3,
                this
        );
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (target == null || correction.is("None")) {
            return;
        }

        if (correction.is("Targeted")) {
            MovingUtil.fixMovement(event, mc.player.getYaw(), lastYaw);
            return;
        }

        MovingUtil.fixMovement(event, lastYaw, lastYaw);
    }

    private Rotation computeNormalRotation(Rotation angle) {
        float deltaYaw = MathHelper.wrapDegrees(angle.getYaw() - lastYaw);
        float deltaPitch = angle.getPitch() - lastPitch;

        float newYaw = lastYaw + deltaYaw;
        float newPitch = lastPitch + deltaPitch;

        float gcd = getGcd();
        newYaw = applyGcd(newYaw, lastYaw, gcd);
        newPitch = applyGcd(newPitch, lastPitch, gcd);

        return new Rotation(newYaw, MathHelper.clamp(newPitch, -90.0f, 90.0f));
    }

    private Rotation computeFunTimeAndHollyRotation(Rotation angle) {
        float currentYaw = lastYaw;
        float currentPitch = lastPitch;

        float deltaYaw = MathHelper.wrapDegrees(angle.getYaw() - currentYaw);
        float deltaPitch = angle.getPitch() - currentPitch;

        float rotDiff = (float) Math.hypot(Math.abs(deltaYaw), Math.abs(deltaPitch));
        float straightYaw = rotDiff > 0.0001f ? Math.abs(deltaYaw / rotDiff) * 45.0f : 0.0f;
        float straightPitch = rotDiff > 0.0001f ? Math.abs(deltaPitch / rotDiff) * 12.0f : 0.0f;

        float randomYaw = randomLerp(-4.0f, 4.0f);
        float randomPitch = randomLerp(-4.0f, 4.0f);

        float newYaw = currentYaw + MathHelper.clamp(deltaYaw, -straightYaw, straightYaw) + randomYaw;
        float newPitch = currentPitch + MathHelper.clamp(deltaPitch, -straightPitch, straightPitch) + randomPitch;

        return new Rotation(newYaw, MathHelper.clamp(newPitch, -90.0f, 90.0f));
    }

    private Rotation computeSmoothHalfRotation(Rotation angle) {
        float deltaYaw = MathHelper.wrapDegrees(angle.getYaw() - lastYaw);
        float deltaPitch = angle.getPitch() - lastPitch;

        float smoothFactor = 0.5f;
        float newYaw = lastYaw + deltaYaw * smoothFactor;
        float newPitch = lastPitch + deltaPitch * smoothFactor;

        float gcd = getGcd();
        newYaw = applyGcd(newYaw, lastYaw, gcd);
        newPitch = applyGcd(newPitch, lastPitch, gcd);

        return new Rotation(newYaw, MathHelper.clamp(newPitch, -90.0f, 90.0f));
    }

    private Rotation computeLegitStandAndGrimRotation(Vec3d eyes, Box targetBox) {
        Box innerBox = targetBox.shrink(0.15, 0.15, 0.15);
        Vec3d lookVec = rotationToVector(lastYaw, lastPitch);

        double dist = eyes.distanceTo(targetBox.getCenter());
        Vec3d projected = eyes.add(lookVec.multiply(dist));

        double x = MathHelper.clamp(projected.x, innerBox.minX, innerBox.maxX);
        double y = MathHelper.clamp(projected.y, innerBox.minY, innerBox.maxY);
        double z = MathHelper.clamp(projected.z, innerBox.minZ, innerBox.maxZ);

        Rotation needed = RotationUtil.fromVec3d(new Vec3d(x, y, z).subtract(eyes));

        float yawDiff = MathHelper.wrapDegrees(needed.getYaw() - lastYaw);
        float pitchDiff = needed.getPitch() - lastPitch;
        float speed = 0.4f + random.nextFloat() * 0.3f;

        float newYaw = lastYaw + yawDiff * speed;
        float newPitch = lastPitch + pitchDiff * speed;
        newPitch = MathHelper.clamp(newPitch, -80.0f, 89.9f);

        float gcd = getGcd();
        newYaw = applyGcd(newYaw, lastYaw, gcd);
        newPitch = applyGcd(newPitch, lastPitch, gcd);

        return new Rotation(newYaw, newPitch);
    }

    private LivingEntity updateTarget() {
        List<LivingEntity> candidates = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && isValid(living)) {
                candidates.add(living);
            }
        }

        if (candidates.isEmpty() || !isEnabled()) {
            return null;
        }

        if (sortMode.is("Distance") || sortMode.is("Adaptive")) {
            candidates.sort(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));
        } else if (sortMode.is("Health")) {
            candidates.sort(Comparator.comparingDouble(LivingEntity::getHealth));
        } else if (sortMode.is("Aim")) {
            candidates.sort(Comparator.comparingDouble(e -> {
                Rotation rot = RotationUtil.fromVec3d(e.getBoundingBox().getCenter().subtract(mc.player.getEyePos()));
                double yawDiff = Math.abs(MathHelper.wrapDegrees(rot.getYaw() - lastYaw));
                double pitchDiff = Math.abs(rot.getPitch() - lastPitch);
                return yawDiff + pitchDiff;
            }));
        }

        return candidates.getFirst();
    }

    private boolean isValid(LivingEntity entity) {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        if (entity == mc.player) {
            return false;
        }
        if (!entity.isAlive() || entity.getHealth() <= 0.0f) {
            return false;
        }

        if (entity instanceof PlayerEntity player) {
            if (!targets.is("Players") && !targets.is("Team")) {
                return false;
            }
            if (targets.is("Team") && !isTeammate(player)) {
                return false;
            }
            if (!invisible.isEnabled() && player.isInvisible()) {
                return false;
            }
            if (nakedOnly.isEnabled()) {
                boolean naked = player.getInventory().getArmorStack(0).isEmpty()
                        && player.getInventory().getArmorStack(1).isEmpty()
                        && player.getInventory().getArmorStack(2).isEmpty()
                        && player.getInventory().getArmorStack(3).isEmpty();
                if (!naked) {
                    return false;
                }
            }
        }

        if (entity instanceof AnimalEntity && !targets.is("Animals")) {
            return false;
        }
        if (entity instanceof MobEntity && !targets.is("Mobs")) {
            return false;
        }

        return mc.player.distanceTo(entity) <= distance.getCurrent();
    }

    private boolean canAttack() {
        if (mc.player == null) {
            return false;
        }
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.9f) {
            return false;
        }
        return !onlyCrit.isEnabled() || AttackUtil.isPlayerInCriticalState();
    }

    private int findAxeInHotbar() {
        if (mc.player == null) {
            return -1;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private boolean isTeammate(PlayerEntity player) {
        return false;
    }

    private float getGcd() {
        if (mc.options == null) {
            return 0.0f;
        }
        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue();
        float f = sensitivity * 0.6f + 0.2f;
        return f * f * f * 8.0f * 0.15f;
    }

    private float applyGcd(float value, float previous, float gcd) {
        if (gcd <= 0.0f) {
            return value;
        }
        return value - (value - previous) % gcd;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }

    private Vec3d rotationToVector(float yaw, float pitch) {
        float yawRad = yaw * 0.017453292f;
        float pitchRad = pitch * 0.017453292f;
        float cosYaw = MathHelper.cos(-yawRad - (float) Math.PI);
        float sinYaw = MathHelper.sin(-yawRad - (float) Math.PI);
        float cosPitch = -MathHelper.cos(-pitchRad);
        float sinPitch = MathHelper.sin(-pitchRad);
        return new Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

    @Override
    public void onEnable() {
        // AuraV2 and legacy Aura should never run together.
        if (Aura.INSTANCE.isEnabled()) {
            Aura.INSTANCE.setToggled(false);
        }
        target = null;
        if (mc.player != null) {
            lastYaw = rotationManager.getCurrentRotation().getYaw();
            lastPitch = rotationManager.getCurrentRotation().getPitch();
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        target = null;
        super.onDisable();
    }
}
