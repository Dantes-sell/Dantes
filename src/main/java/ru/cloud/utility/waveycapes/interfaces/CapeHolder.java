package ru.cloud.utility.waveycapes.interfaces;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import ru.cloud.utility.waveycapes.WaveyCapesBase;
import ru.cloud.utility.waveycapes.math.Vector2;
import ru.cloud.utility.waveycapes.math.Vector3;
import ru.cloud.utility.waveycapes.sim.StickSimulation;

public interface CapeHolder {
    StickSimulation getSimulation();

    default void updateSimulation(AbstractClientPlayerEntity player, int partCount) {
        StickSimulation simulation = this.getSimulation();
        if (simulation == null) return;

        boolean dirty = simulation.init(partCount);
        if (dirty) {
            simulation.applyMovement(new Vector3(1.0f, 1.0f, 0.0f));
            for (int i = 0; i < 5; ++i) {
                this.simulate(player);
            }
        }
    }

    default void simulate(AbstractClientPlayerEntity player) {
        StickSimulation simulation = this.getSimulation();
        if (simulation == null || simulation.empty()) return;

        // В Fabric 1.21.4 используем lastRenderX/Y/Z вместо chasingPos
        double d = player.lastRenderX - player.getX();
        double m = player.lastRenderZ - player.getZ();
        float n = player.prevBodyYaw + (player.bodyYaw - player.prevBodyYaw);
        double o = MathHelper.sin(n * 0.017453292f);
        double p = -MathHelper.cos(n * 0.017453292f);

        float heightMul = (float) WaveyCapesBase.config.heightMul;
        float straveMul = (float) WaveyCapesBase.config.straveMul;

        if (player.isSubmergedInWater()) {
            heightMul *= 2.0f;
        }

        double fallHack = MathHelper.clamp((player.prevY - player.getY()) * 10.0, 0.0, 1.0);

        if (player.isSubmergedInWater()) {
            simulation.setGravity(WaveyCapesBase.config.gravity / 10.0f);
        } else {
            simulation.setGravity((float) WaveyCapesBase.config.gravity);
        }

        Vector3 gravity = new Vector3(0.0f, -1.0f, 0.0f);
        Vector2 strafe = new Vector2(
                (float) (player.getX() - player.prevX),
                (float) (player.getZ() - player.prevZ));
        strafe.rotateDegrees(-player.getYaw());

        double changeX = d * o + m * p + fallHack
                + ((player.isSneaking() && !simulation.isSneaking()) ? 3 : 0);
        double changeY = (player.getY() - player.prevY) * heightMul
                + ((player.isSneaking() && !simulation.isSneaking()) ? 1 : 0);
        double changeZ = -strafe.x * straveMul;

        simulation.setSneaking(player.isSneaking());
        Vector3 change = new Vector3((float) changeX, (float) changeY, (float) changeZ);

        if (player.isSwimming()) {
            float rotation = player.getPitch();
            rotation += 90.0f;
            gravity.rotateDegrees(rotation);
            change.rotateDegrees(rotation);
        }

        simulation.setGravityDirection(gravity);
        simulation.applyMovement(change);
        simulation.simulate();
    }
}
