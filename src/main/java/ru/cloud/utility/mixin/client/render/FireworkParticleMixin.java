package ru.cloud.utility.mixin.client.render;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.client.modules.impl.render.NoRender;

@Mixin(ParticleManager.class)
public class FireworkParticleMixin {

    // Убирает эмиттеры фейерверков (трейл + взрыв)
    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V", at = @At("HEAD"), cancellable = true)
    private void noFireworkEmitter(Entity entity, ParticleEffect effect, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveFireworks() &&
                entity instanceof net.minecraft.entity.projectile.FireworkRocketEntity) {
            ci.cancel();
        }
    }
}
