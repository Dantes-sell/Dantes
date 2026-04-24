package ru.cloud.utility.mixin.client.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.cloud.client.modules.impl.render.NoRender;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void noRenderLabel(EntityRenderState state, Text text, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveHolograms()) ci.cancel();
    }

    @Inject(method = "getShadowRadius", at = @At("HEAD"), cancellable = true)
    private void noEntityShadow(EntityRenderState state, CallbackInfoReturnable<Float> cir) {
        if (NoRender.INSTANCE.isRemoveEntityShadows()) cir.setReturnValue(0f);
    }
}
