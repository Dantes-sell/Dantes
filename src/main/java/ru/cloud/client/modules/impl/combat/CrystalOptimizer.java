package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;

import java.util.Comparator;

@ModuleAnnotation(name = "CrystalOptimizer", category = Category.COMBAT, description = "Explodes crystals you aim at")
public final class CrystalOptimizer extends Module {

    public static final CrystalOptimizer INSTANCE = new CrystalOptimizer();

    private static final int BASE_ATTACK_TICKS = 1;
    private static final double ATTACK_REACH = 4.5;

    private int attackTicks = 0;

    private CrystalOptimizer() {
    }

    @Override
    public void onDisable() {
        super.onDisable();
        attackTicks = 0;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        boolean holdingCrystal = mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)
                || mc.player.getOffHandStack().isOf(Items.END_CRYSTAL);
        if (!holdingCrystal || mc.player.getAttackCooldownProgress(1.0f) < 1.0f) {
            return;
        }

        float tps = SyncTps.INSTANCE.getCurrentTPS();
        float tickRate = Math.max(tps, 1.0f);
        float ticksPerAttack = BASE_ATTACK_TICKS * (20.0f / tickRate);

        attackTicks++;
        if (attackTicks < (int) Math.ceil(ticksPerAttack)) {
            return;
        }

        double reach = ATTACK_REACH;
        Entity crystal = mc.world.getEntitiesByClass(
                        EndCrystalEntity.class,
                        mc.player.getBoundingBox().expand(reach),
                        entity -> entity.isAlive() && canBeSeen(entity, reach)
                ).stream()
                .min(Comparator.comparingDouble(entity -> mc.player.squaredDistanceTo(entity)))
                .orElse(null);

        if (crystal != null) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            attackTicks = 0;
        }
    }

    private boolean canBeSeen(Entity entity, double reach) {
        Vec3d eyePos = mc.player.getEyePos();
        Box bb = entity.getBoundingBox().expand(0.1);

        Vec3d[] corners = {
                new Vec3d(bb.minX, bb.minY, bb.minZ),
                new Vec3d(bb.minX, bb.minY, bb.maxZ),
                new Vec3d(bb.minX, bb.maxY, bb.minZ),
                new Vec3d(bb.minX, bb.maxY, bb.maxZ),
                new Vec3d(bb.maxX, bb.minY, bb.minZ),
                new Vec3d(bb.maxX, bb.minY, bb.maxZ),
                new Vec3d(bb.maxX, bb.maxY, bb.minZ),
                new Vec3d(bb.maxX, bb.maxY, bb.maxZ)
        };

        for (Vec3d corner : corners) {
            HitResult result = mc.world.raycast(new RaycastContext(
                    eyePos,
                    corner,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (result.getType() == HitResult.Type.MISS && eyePos.squaredDistanceTo(corner) <= reach * reach) {
                return true;
            }
        }
        return false;
    }
}
