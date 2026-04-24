package ru.cloud.utility.mixin.client.render;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.client.modules.impl.render.NoRender;

@Mixin(CloudRenderer.class)
public class CloudRendererMixin {

    @Inject(method = "renderClouds(ILnet/minecraft/client/option/CloudRenderMode;FLorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/util/math/Vec3d;F)V", at = @At("HEAD"), cancellable = true)
    private void noRenderClouds(int ticks, CloudRenderMode mode, float tickDelta,
                                Matrix4f modelView, Matrix4f projection, Vec3d cameraPos, float cloudHeight, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveClouds()) ci.cancel();
    }
}
