package ru.cloud.utility.mixin.minecraft.entity;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.Zenith;
import ru.cloud.client.modules.impl.misc.UnHook;
import ru.cloud.utility.game.player.rotation.Rotation;

import static ru.cloud.utility.interfaces.IMinecraft.mc;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {



    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void tickMovement(CallbackInfo ci) {

    }

    @Mutable
    float savedYaw;
    @Mutable
    float savedPitch;
    @Mutable
    boolean rotationApplied;

    @Inject(method="travel",at = @At(value = "HEAD"))
    public void fixElytra(CallbackInfo ci){
        if (UnHook.UNHOOKED) { rotationApplied = false; return; }
        if(((Object)this) instanceof ClientPlayerEntity player){
            Rotation currentRotation = Zenith.getInstance().getRotationManager().getCurrentRotation();
            savedYaw = mc.player.getYaw();
            savedPitch = mc.player.getPitch();
            player.setYaw(currentRotation.getYaw());
            player.setPitch(currentRotation.getPitch());
            rotationApplied = true;
        }
    }

    @Inject(method="travel",at = @At(value = "RETURN"))
    public void fixElytraEnd(CallbackInfo ci){
        if (!rotationApplied) return;
        if(((Object)this) instanceof ClientPlayerEntity player){
            player.setYaw(savedYaw);
            player.setPitch(savedPitch);
            rotationApplied = false;
        }
    }
}
