package ru.cloud.utility.mixin.client.render;

import net.minecraft.client.render.Fog;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.client.modules.impl.render.NoRender;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {

    @Inject(method = "renderSky(FFF)V", at = @At("HEAD"), cancellable = true)
    private void noRenderSky(float r, float g, float b, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveSky()) ci.cancel();
    }

    @Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true)
    private void noRenderStars(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
                               float tickDelta, int moonPhase, float skyAngle, float starBrightness, Fog fog, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveStars()) ci.cancel();
    }
}
