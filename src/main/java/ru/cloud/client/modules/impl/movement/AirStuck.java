package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.events.impl.player.EventMove;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.server.EventPacket;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "AirStuck", category = Category.MOVEMENT, description = "Замораживает игрока в воздухе")
public final class AirStuck extends Module {

    public static final AirStuck INSTANCE = new AirStuck();

    private boolean oldFlying;
    private float yaw;
    private float pitch;
    private float bodyYaw;

    private AirStuck() {
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null) {
            return;
        }

        oldFlying = mc.player.getAbilities().flying;
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
        bodyYaw = mc.player.getBodyYaw();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player == null) {
            return;
        }

        mc.player.noClip = false;
        mc.player.getAbilities().flying = oldFlying;
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().allowFlying = oldFlying;
        }
        mc.player.sendAbilitiesUpdate();
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) {
            return;
        }

        mc.player.noClip = true;
        mc.player.setOnGround(false);
        mc.player.setVelocity(Vec3d.ZERO);
        mc.player.setSprinting(false);

        mc.player.getAbilities().allowFlying = true;
        mc.player.getAbilities().flying = true;
        mc.player.sendAbilitiesUpdate();

        mc.player.setHeadYaw(yaw);
        mc.player.setBodyYaw(bodyYaw);
        mc.player.setPitch(pitch);
    }

    @EventTarget
    public void onMove(EventMove e) {
        if (mc.player == null) {
            return;
        }

        e.setMovePos(Vec3d.ZERO);
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null) {
            return;
        }

        if (e.isSent() && e.getPacket() instanceof PlayerMoveC2SPacket) {
            e.cancel();
            return;
        }

        if (e.isReceive() && e.getPacket() instanceof GameJoinS2CPacket) {
            this.toggle();
        }
    }
}
