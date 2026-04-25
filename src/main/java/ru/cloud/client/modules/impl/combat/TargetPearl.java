package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.player.EventRotate;
import ru.cloud.base.rotation.RotationTarget;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.player.PlayerInventoryUtil;
import ru.cloud.utility.game.player.rotation.Rotation;
import ru.cloud.utility.game.player.rotation.RotationUtil;
import ru.cloud.utility.math.Timer;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleAnnotation(name = "TargetPearl", category = Category.COMBAT, description = "Settings for TargetPearl")
public final class TargetPearl extends Module {

    public static final TargetPearl INSTANCE = new TargetPearl();
    private TargetPearl() {}

    final Timer timer = new Timer();

    final ModeSetting targetSetting = new ModeSetting("Цели", "Aura Target", "All");

    final NumberSetting distanceSetting = new NumberSetting("Дистанция", 10f, 5f, 15f, 1f,
            "Максимальная дистанция до точки падения");

    @EventTarget
    public void onRotate(EventRotate e) {
        if (mc.player == null || mc.world == null) return;

        // кулдаун между бросками
        if (!timer.finished(1000)) return;

        Slot slot = PlayerInventoryUtil.getSlot(Items.ENDER_PEARL);
        if (slot == null) return;

        
        boolean ourPearlFlying = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(EnderPearlEntity.class::isInstance)
                .map(EnderPearlEntity.class::cast)
                .anyMatch(pearl -> Objects.equals(pearl.getOwner(), mc.player));
        if (ourPearlFlying) return;

        LivingEntity auraTarget = resolveAuraTarget();

        StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(EnderPearlEntity.class::isInstance)
                .map(EnderPearlEntity.class::cast)
                .filter(pearl -> {
                    if (pearl.getOwner() == null) return false;
                    if (pearl.getOwner() == mc.player) return false;
                    if (Zenith.getInstance().getFriendManager().isFriend(pearl.getOwner().getName().getString())) return false;
                    if (targetSetting.is("Aura Target") && auraTarget != null && !auraTarget.equals(pearl.getOwner())) return false;
                    return true;
                })
                .min(Comparator.comparingDouble(pearl -> {
                    Vec3d landPos = predictLandingPos(pearl);
                    if (landPos == null) return Double.MAX_VALUE;
                    // �������� ���� ��������� � ��� �� ����� �����������
                    return mc.player.getPos().distanceTo(landPos);
                }))
                .ifPresent(pearl -> {
                    Vec3d landPos = predictLandingPos(pearl);
                    if (landPos == null) return;

                    
                    if (mc.player.getPos().distanceTo(landPos) <= distanceSetting.getCurrent()) return;

                    Vec3d eyePos = mc.player.getEyePos();
                    float yaw = RotationUtil.fromVec3d(landPos.subtract(eyePos)).getYaw();
                    final Vec3d finalLandPos = landPos;

                    // ���������� ����� �� �������� � �������, ���� ������������ ���� (�������� ������)
                    IntStream.range(-89, 90)
                            .map(i -> 89 - i) // �� 89 �� -89 (������ ����)
                            .mapToObj(pitch -> new Rotation(yaw, pitch))
                            .filter(angle -> {
                                Vec3d hit = simulatePearlTrajectory(angle.toVector(), eyePos);
                                return hit != null && hit.distanceTo(finalLandPos) <= 3.0;
                            })
                            .findFirst()
                            .ifPresent(angle -> {
                                var rotManager = Zenith.getInstance().getRotationManager();
                                var aimManager = rotManager.getAimManager();
                                rotManager.setRotation(
                                        new RotationTarget(
                                                angle,
                                                () -> aimManager.rotate(aimManager.getInstantSetup(), angle),
                                                aimManager.getInstantSetup()
                                        ),
                                        5, this
                                );
                                PlayerInventoryUtil.swapAndUse(Items.ENDER_PEARL, angle);
                                timer.reset();
                            });
                });
    }

    private LivingEntity resolveAuraTarget() {
        LivingEntity auraTarget = Aura.INSTANCE.isEnabled() ? Aura.INSTANCE.getTarget() : null;
        if (auraTarget != null) {
            return auraTarget;
        }
        return AuraV2.INSTANCE.isEnabled() ? AuraV2.INSTANCE.getTarget() : null;
    }

    /**
     * ���������� ���������� ���������� ����� � ���������� ����� �����������.
     * ������ �����: drag=0.99, gravity=-0.03 �� ���.
     */
    private Vec3d predictLandingPos(EnderPearlEntity pearl) {
        if (mc.world == null) return null;
        Vec3d pos = pearl.getPos();
        Vec3d vel = pearl.getVelocity();
        for (int i = 0; i < 120; i++) {
            Vec3d nextPos = pos.add(vel);
            if (mc.world.getBlockState(BlockPos.ofFloored(nextPos)).isSolid()) return nextPos;
            // ����� �� ������� ����
            if (nextPos.y < mc.world.getBottomY()) return pos;
            vel = vel.multiply(0.99).add(0, -0.03, 0);
            pos = nextPos;
        }
        return pos;
    }

    /**
     * ���������� ���������� ������ ����� �� eyePos � �������� ������������.
     * ��������� �������� ����� = direction * 1.5.
     */
    private Vec3d simulatePearlTrajectory(Vec3d direction, Vec3d startPos) {
        if (mc.world == null) return null;
        Vec3d pos = startPos;
        Vec3d vel = direction.multiply(1.5);
        for (int i = 0; i < 120; i++) {
            Vec3d nextPos = pos.add(vel);
            if (mc.world.getBlockState(BlockPos.ofFloored(nextPos)).isSolid()) return nextPos;
            if (nextPos.y < mc.world.getBottomY()) return pos;
            vel = vel.multiply(0.99).add(0, -0.03, 0);
            pos = nextPos;
        }
        return pos;
    }
}


