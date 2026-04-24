package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.server.EventPacket;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(name = "Velocity", category = Category.COMBAT, description = "Уменьшает отдачу от ударов")
public final class Velocity extends Module {

    public static final Velocity INSTANCE = new Velocity();

    private Velocity() {}

    private final ModeSetting mode = new ModeSetting("Режим");
    private final ModeSetting.Value newGrim  = new ModeSetting.Value(mode, "NewGrim").select();
    private final ModeSetting.Value oldGrim  = new ModeSetting.Value(mode, "OldGrim");
    private final ModeSetting.Value matrix   = new ModeSetting.Value(mode, "Matrix");
    private final ModeSetting.Value normal   = new ModeSetting.Value(mode, "Normal");

    private boolean flag;
    private int grimTicks;
    private int ccCooldown;
    private Vec3d pendingVelocity;

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null) return;
        if (!e.isReceive()) return;
        if (mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.player.isInLava()) return;

        if (ccCooldown > 0) {
            ccCooldown--;
            return;
        }

        if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket pac
                && pac.getEntityId() == mc.player.getId()) {
            handleVelocityPacket(e, pac);
        }

        handleAdditionalPackets(e);
    }

    private void handleVelocityPacket(EventPacket e, EntityVelocityUpdateS2CPacket pac) {
        Vec3d velocity = new Vec3d(pac.getVelocityX(), pac.getVelocityY(), pac.getVelocityZ());

        if (matrix.isSelected()) {
            if (!flag) {
                e.cancel();
                flag = true;
            } else {
                flag = false;
                e.cancel();
                pendingVelocity = new Vec3d(
                        velocity.x * -0.1,
                        velocity.y,
                        velocity.z * -0.1
                );
            }
        } else if (normal.isSelected()) {
            e.cancel();
        } else if (oldGrim.isSelected()) {
            e.cancel();
            grimTicks = 6;
        } else if (newGrim.isSelected()) {
            e.cancel();
            flag = true;
        }
    }

    private void handleAdditionalPackets(EventPacket e) {
        if (oldGrim.isSelected() && e.getPacket() instanceof CommonPingS2CPacket && grimTicks > 0) {
            e.cancel();
            grimTicks--;
        }

        if (e.getPacket() instanceof PlayerPositionLookS2CPacket && newGrim.isSelected()) {
            ccCooldown = 5;
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;
        if (mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) return;

        if (matrix.isSelected()) {
            handleMatrixTick();
        }

        if (newGrim.isSelected() && flag) {
            handleNewGrimTick();
        }

        if (grimTicks > 0) {
            grimTicks--;
        }
    }

    private void handleMatrixTick() {
        if (pendingVelocity != null) {
            mc.player.setVelocity(pendingVelocity);
            pendingVelocity = null;
        }

        if (mc.player.hurtTime > 0 && !mc.player.isOnGround()) {
            double yaw = mc.player.getYaw() * 0.017453292F;
            double speed = Math.sqrt(
                    mc.player.getVelocity().x * mc.player.getVelocity().x
                    + mc.player.getVelocity().z * mc.player.getVelocity().z
            );
            mc.player.setVelocity(
                    -Math.sin(yaw) * speed,
                    mc.player.getVelocity().y,
                    Math.cos(yaw) * speed
            );
            mc.player.setSprinting(mc.player.age % 2 != 0);
        }
    }

    private void handleNewGrimTick() {
        if (ccCooldown <= 0) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.isOnGround(),
                    false
            ));
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                    BlockPos.ofFloored(mc.player.getPos()),
                    Direction.DOWN
            ));
        }
        flag = false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        grimTicks = 0;
        flag = false;
        ccCooldown = 0;
        pendingVelocity = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        pendingVelocity = null;
    }
}
