package ru.cloud.utility.mixin.minecraft.entity;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.player.EventDamage;
import ru.cloud.base.events.impl.player.EventJump;
import ru.cloud.client.modules.impl.misc.UnHook;
import ru.cloud.utility.interfaces.IMinecraft;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements IMinecraft {

    //ЛИКВИДБАБУНС ЧТО ДЕЛАЕТ????
    @Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    public float replaceMovePacketPitch(LivingEntity instance) {
        if ((Object) this != mc.player || UnHook.UNHOOKED) {
            return instance.getYaw();
        } else {
            return Zenith.getInstance().getRotationManager().getCurrentRotation().getYaw();
        }
    }

    @Inject(method = "jump", at = @At("HEAD"))
    private void onJump(CallbackInfo ci) {
        if (UnHook.UNHOOKED) return;
        if ((Object) this != mc.player) return;
        EventManager.call(new EventJump());
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (UnHook.UNHOOKED) return;
        if ((Object) this != mc.player) return;
        if (amount <= 0) return;
        EventManager.call(new EventDamage());
    }
}
