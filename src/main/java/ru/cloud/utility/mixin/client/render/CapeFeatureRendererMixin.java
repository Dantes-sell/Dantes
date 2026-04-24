package ru.cloud.utility.mixin.client.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.utility.waveycapes.WaveyCapesBase;
import ru.cloud.utility.waveycapes.render.WaveyCapeRenderer;

@Mixin(CapeFeatureRenderer.class)
public class CapeFeatureRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                          PlayerEntityRenderState renderState, float limbAngle, float limbDistance,
                          CallbackInfo ci) {
        if (WaveyCapesBase.config == null) return;

        // tickDelta не передаётся напрямую, используем limbDistance как приближение
        // или получаем из MinecraftClient
        float tickDelta = net.minecraft.client.MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);

        boolean rendered = WaveyCapeRenderer.render(renderState, matrices, vertexConsumers, light, tickDelta);
        if (rendered) {
            ci.cancel();
        }
    }
}
