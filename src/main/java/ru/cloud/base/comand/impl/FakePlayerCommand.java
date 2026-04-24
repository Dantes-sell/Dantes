package ru.cloud.base.comand.impl;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.comand.api.CommandAbstract;
import ru.cloud.base.events.impl.input.EventKey;
import ru.cloud.base.events.impl.player.EventAttack;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.utility.game.other.MessageUtil;

import java.util.UUID;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class FakePlayerCommand extends CommandAbstract {

    private OtherClientPlayerEntity fakePlayer;
    private float moveForward = 0.0F;
    private float moveStrafe = 0.0F;

    public FakePlayerCommand() {
        super("fakeplayer");
        EventManager.register(this);
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add").executes(ctx -> {
            add();
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("del").executes(ctx -> {
            del();
            return SINGLE_SUCCESS;
        }));
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (fakePlayer == null || event.getAction() != EventAttack.Action.PRE) return;
        if (event.getTarget() != fakePlayer) return;
        if (fakePlayer.hurtTime != 0) return;

        mc.world.playSound(mc.player,
                fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0F, 1.0F);

        net.minecraft.item.ItemStack held = mc.player.getMainHandStack();
        boolean isSword = held.getItem() instanceof net.minecraft.item.SwordItem;
        boolean isAxe   = held.getItem() instanceof net.minecraft.item.AxeItem;

        if (mc.player.fallDistance > 0.0F) {
            mc.world.playSound(mc.player,
                    fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        } else if (isSword || isAxe) {
            mc.world.playSound(mc.player,
                    fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        } else {
            mc.world.playSound(mc.player,
                    fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }

        fakePlayer.onDamaged(mc.world.getDamageSources().generic());
        fakePlayer.setHealth(fakePlayer.getHealth() + fakePlayer.getAbsorptionAmount() - 1.0F);

        if (fakePlayer.isDead()) {
            fakePlayer.setHealth(10.0F);
            new EntityStatusS2CPacket(fakePlayer, (byte) 35).apply(mc.player.networkHandler);
        }
    }

    @EventTarget
    public void onKey(EventKey event) {
        if (fakePlayer == null || mc.currentScreen != null) return;

        int key = event.getKeyCode();
        boolean down = event.getAction() == 1 || event.getAction() == 2;

        if (key == 265)      moveForward = down ? 1.0F : 0.0F;   // W
        else if (key == 264) moveForward = down ? -1.0F : 0.0F;  // S
        else if (key == 263) moveStrafe  = down ? 1.0F : 0.0F;   // A
        else if (key == 262) moveStrafe  = down ? -1.0F : 0.0F;  // D
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (fakePlayer == null || mc.player == null) return;

        if (moveForward == 0.0F && moveStrafe == 0.0F) {
            fakePlayer.setSprinting(false);
            fakePlayer.setVelocity(0.0, fakePlayer.getVelocity().y, 0.0);
            fakePlayer.limbAnimator.setSpeed(0.0F);
        } else {
            float yaw = mc.player.getYaw();
            double speed = 0.2;
            double motionX = moveStrafe * Math.cos(Math.toRadians(yaw)) - moveForward * Math.sin(Math.toRadians(yaw));
            double motionZ = moveForward * Math.cos(Math.toRadians(yaw)) + moveStrafe * Math.sin(Math.toRadians(yaw));
            Vec3d velocity = new Vec3d(motionX * speed, fakePlayer.getVelocity().y, motionZ * speed);
            fakePlayer.setVelocity(velocity);
            fakePlayer.move(MovementType.SELF, velocity);
            fakePlayer.setSprinting(true);
        }
    }

    public void add() {
        if (fakePlayer != null) {
            fakePlayer.discard();
            fakePlayer = null;
        }

        fakePlayer = new OtherClientPlayerEntity(mc.world,
                new GameProfile(UUID.fromString("66123666-6666-6666-6666-666666666600"), "FakePlayer"));
        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setYaw(mc.player.getYaw());
        fakePlayer.setPitch(mc.player.getPitch());
        fakePlayer.setHeadYaw(mc.player.getHeadYaw());
        fakePlayer.setBodyYaw(mc.player.getBodyYaw());
        fakePlayer.setStackInHand(Hand.MAIN_HAND, mc.player.getMainHandStack().copy());
        fakePlayer.setStackInHand(Hand.OFF_HAND, mc.player.getOffHandStack().copy());
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 9999, 2));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 9999, 4));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 9999, 1));
        mc.world.addEntity(fakePlayer);

        MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "FakePlayer добавлен");
    }

    public void del() {
        if (fakePlayer == null) {
            MessageUtil.displayMessage(MessageUtil.LogLevel.WARN, "FakePlayer не существует");
            return;
        }

        fakePlayer.discard();
        fakePlayer = null;
        MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "FakePlayer удалён");
    }
}
