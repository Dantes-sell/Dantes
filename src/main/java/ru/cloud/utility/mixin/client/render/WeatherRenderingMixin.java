package ru.cloud.utility.mixin.client.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.client.modules.impl.render.NoRender;

@Mixin(WeatherRendering.class)
public class WeatherRenderingMixin {

    @Inject(method = "renderPrecipitation(Lnet/minecraft/world/World;Lnet/minecraft/client/render/VertexConsumerProvider;IFLnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
    private void noRenderRain(World world, VertexConsumerProvider vertexConsumers,
                              int ticks, float tickDelta, Vec3d cameraPos, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveRain()) ci.cancel();
    }
}
