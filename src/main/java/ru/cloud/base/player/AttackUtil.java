package ru.cloud.base.player;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Hand;
import ru.cloud.utility.game.player.PlayerIntersectionUtil;
import ru.cloud.utility.game.player.SimulatedPlayer;
import ru.cloud.utility.interfaces.IClient;
import ru.cloud.utility.math.Timer;


@Setter
@Getter
@UtilityClass
public class AttackUtil implements IClient {
    private final Timer attackTimer = new Timer();
    private int count = 0;




    public void attackEntity(Entity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
        attackTimer.reset();
        count++;

    }




    public boolean hasMovementRestrictions() {
        return mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                || PlayerIntersectionUtil.isPlayerInBlock(Blocks.COBWEB)
                || mc.player.isSubmergedInWater()
                || mc.player.isInLava()
                || mc.player.isClimbing()
                || !PlayerIntersectionUtil.canChangeIntoPose(EntityPose.STANDING) && mc.player.isInSneakingPose()
                || mc.player.getAbilities().flying;
    }

    public boolean hasPreMovementRestrictions(SimulatedPlayer simulatedPlayer) {
        return simulatedPlayer.hasStatusEffect(StatusEffects.BLINDNESS)
                || simulatedPlayer.hasStatusEffect(StatusEffects.LEVITATION)
                || PlayerIntersectionUtil.isBoxInBlock(simulatedPlayer.boundingBox, Blocks.COBWEB)
                || simulatedPlayer.isSubmergedInWater()
                || simulatedPlayer.isInLava()
                || simulatedPlayer.isClimbing()
                || !PlayerIntersectionUtil.canChangeIntoPose(EntityPose.STANDING) && mc.player.isInSneakingPose()
                || mc.player.getAbilities().flying;
    }

    public boolean isPlayerInCriticalState() {
        if (mc.player.isOnGround()) return false;
        if (mc.player.isClimbing()) return false;
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return false;
        if (mc.player.hasStatusEffect(StatusEffects.LEVITATION)) return false;
        
        boolean isFalling = mc.player.getVelocity().y < 0;
        boolean hasFallDistance = mc.player.fallDistance > 0.2f;
        return isFalling && hasFallDistance;
    }

    public boolean isPrePlayerInCriticalState(SimulatedPlayer simulatedPlayer) {
        if (simulatedPlayer.onGround) return false;
        if (simulatedPlayer.isClimbing()) return false;
        if (simulatedPlayer.isSubmergedInWater() || simulatedPlayer.isInLava()) return false;
        if (simulatedPlayer.hasStatusEffect(StatusEffects.LEVITATION)) return false;
        boolean isFalling = simulatedPlayer.velocity.y < 0;
        boolean hasFallDistance = simulatedPlayer.fallDistance > 0.2f;
        return isFalling && hasFallDistance;
    }

}

