package ru.cloud.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.player.EventMoveInput;
import ru.cloud.base.events.impl.player.EventRotate;
import ru.cloud.base.rotation.RotationTarget;
import ru.cloud.base.rotation.mods.config.InterpolationRotationConfig;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.MultiBooleanSetting;
import ru.cloud.utility.game.player.SimulatedPlayer;
import ru.cloud.utility.game.player.rotation.Rotation;
import ru.cloud.utility.game.player.rotation.RotationUtil;
import ru.cloud.utility.interfaces.IClient;
import ru.cloud.utility.math.IntRange;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleAnnotation(name = "AutoDodge", category = Category.PLAYER, description = "Уклоняется от стрел")
public final class AutoDodge extends Module implements IClient {

    public static final AutoDodge INSTANCE = new AutoDodge();
    private AutoDodge() {}

    private static final double HIT_EXPAND = 0.7;
    private static final int MAX_TICKS = 80;

    // Плавная ротация: скорость 60-80% за тик, достаточно быстро но не мгновенно
    private static final InterpolationRotationConfig SMOOTH_ROT = new InterpolationRotationConfig(
            new IntRange(60, 80),  // horizontal speed %
            new IntRange(60, 80),  // vertical speed %
            new IntRange(0, 5),    // direction change factor
            0.5f                   // midpoint
    );

    final BooleanSetting allowRotation = new BooleanSetting("Разрешить поворот", false);
    final BooleanSetting allowJump = new BooleanSetting("Разрешить прыжок", "Прыжок для уклонения", true,
            allowRotation::isEnabled);

    final MultiBooleanSetting ignore = MultiBooleanSetting.create("Игнорировать",
            List.of("Открытый инвентарь", "Использование предмета"));

    // Целевой yaw для плавного поворота
    Rotation dodgeRotation = null;

    @Override
    public boolean isEnabled() {
        if (!super.isEnabled()) return false;
        if (mc.player == null) return false;
        if (!ignore.isEnable("Открытый инвентарь") && mc.currentScreen instanceof InventoryScreen) return false;
        if (!ignore.isEnable("Использование предмета") && mc.player.isUsingItem()) return false;
        return true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        dodgeRotation = null;
    }

    @EventTarget
    public void onRotate(EventRotate e) {
        if (!isEnabled() || !allowRotation.isEnabled()) return;
        if (dodgeRotation == null) return;

        var rotManager = Zenith.getInstance().getRotationManager();
        var aimManager = rotManager.getAimManager();
        final Rotation target = dodgeRotation;

        rotManager.setRotation(
                new RotationTarget(
                        target,
                        () -> aimManager.rotate(SMOOTH_ROT, target),
                        SMOOTH_ROT
                ),
                1, this
        );
    }

    @EventTarget
    public void onMoveInput(EventMoveInput e) {
        if (!isEnabled()) return;

        List<Entity> arrows = findArrows();
        if (arrows.isEmpty()) {
            dodgeRotation = null;
            return;
        }

        SimulatedPlayer simPlayer = SimulatedPlayer.fromClientPlayer(
                SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(mc.player.input.playerInput)
        );

        HitInfo hit = findFirstHit(simPlayer, arrows);
        if (hit == null) {
            dodgeRotation = null;
            return;
        }

        DodgePlan plan = planDodge(hit);
        if (plan == null) return;

        if (plan.forward != 0 || plan.sideways != 0) {
            e.setForward(plan.forward);
            e.setStrafe(plan.sideways);
        }

        if (plan.shouldJump && allowRotation.isEnabled() && allowJump.isEnabled() && mc.player.isOnGround()) {
            mc.player.jump();
        }
    }

    private List<Entity> findArrows() {
        List<Entity> list = new ArrayList<>();
        if (mc.world == null) return list;
        for (Entity e : mc.world.getEntities()) {
            if ((e instanceof ArrowEntity || e instanceof PersistentProjectileEntity || e instanceof TridentEntity)
                    && !e.isOnGround()) {
                list.add(e);
            }
        }
        return list;
    }

    private HitInfo findFirstHit(SimulatedPlayer simPlayer, List<Entity> arrows) {
        Vec3d[] arrowPos = new Vec3d[arrows.size()];
        Vec3d[] arrowVel = new Vec3d[arrows.size()];
        for (int i = 0; i < arrows.size(); i++) {
            arrowPos[i] = arrows.get(i).getPos();
            arrowVel[i] = arrows.get(i).getVelocity();
        }

        for (int tick = 0; tick < MAX_TICKS; tick++) {
            simPlayer.tick();
            for (int i = 0; i < arrows.size(); i++) {
                Vec3d lastPos = arrowPos[i];
                arrowVel[i] = arrowVel[i].multiply(0.99).add(0, -0.05, 0);
                arrowPos[i] = arrowPos[i].add(arrowVel[i]);

                Box playerBox = new Box(-0.3, 0, -0.3, 0.3, 1.8, 0.3)
                        .expand(HIT_EXPAND)
                        .offset(simPlayer.pos);

                if (playerBox.intersects(lastPos, arrowPos[i])) {
                    return new HitInfo(tick, arrows.get(i), arrowPos[i], lastPos, arrowVel[i]);
                }
            }
        }
        return null;
    }

    private DodgePlan planDodge(HitInfo hit) {
        Vec3d arrowDir = hit.velocity.normalize();

        // перпендикуляр к направлению стрелы в горизонтальной плоскости
        Vec3d perpendicular = arrowDir.crossProduct(new Vec3d(0, 1, 0)).normalize();
        if (perpendicular.lengthSquared() < 0.1) {
            perpendicular = arrowDir.crossProduct(new Vec3d(1, 0, 0)).normalize();
        }

        float yaw = mc.player.getYaw();
        double yawRad = Math.toRadians(yaw);
        Vec3d forward = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3d right = new Vec3d(Math.cos(yawRad), 0, Math.sin(yawRad));

        float fwd = (float) MathHelper.clamp(perpendicular.dotProduct(forward) * 2, -1, 1);
        float side = (float) MathHelper.clamp(perpendicular.dotProduct(right) * 2, -1, 1);

        // Плавный поворот в сторону уклонения через RotationManager
        if (allowRotation.isEnabled()) {
            double targetYaw = Math.toDegrees(Math.atan2(-perpendicular.x, perpendicular.z));
            float currentPitch = RotationUtil.getClientRotation().getPitch();
            dodgeRotation = new Rotation((float) MathHelper.wrapDegrees(targetYaw), currentPitch);
        }

        boolean shouldJump = hit.hitPos.y < mc.player.getY() + 0.5;
        return new DodgePlan(fwd, side, shouldJump);
    }

    private record HitInfo(int tick, Entity entity, Vec3d hitPos, Vec3d prevPos, Vec3d velocity) {}
    private record DodgePlan(float forward, float sideways, boolean shouldJump) {}
}
