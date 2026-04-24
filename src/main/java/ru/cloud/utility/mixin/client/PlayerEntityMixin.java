package ru.cloud.utility.mixin.client;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.cloud.client.modules.impl.player.NoPush;
import ru.cloud.client.modules.impl.render.SwingAnimation;

@Mixin(LivingEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void getArmSwingAnimationEnd(CallbackInfoReturnable<Integer> info) {
        SwingAnimation swingAnimation = SwingAnimation.INSTANCE;
        if (swingAnimation.isEnabled()) {
            info.setReturnValue((int) swingAnimation.swingPower.getCurrent());
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void removePushFromEntities(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof ClientPlayerEntity
                && NoPush.INSTANCE.isEnabled()
                && NoPush.INSTANCE.getRemovePushFrom().isEnable("Энтити")) {
            cir.setReturnValue(false);
        }
    }
}


