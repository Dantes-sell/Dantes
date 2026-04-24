package ru.cloud.utility.mixin.client.render.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.client.modules.impl.render.InventoryAnimation;

@Mixin(HandledScreen.class)
public abstract class InventoryAnimationMixin {

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        InventoryAnimation anim = InventoryAnimation.INSTANCE;
        if (!anim.isEnabled()) return;
        anim.pushTransform(context.getMatrices(), x, y, backgroundWidth, backgroundHeight);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!InventoryAnimation.INSTANCE.isEnabled()) return;
        InventoryAnimation.INSTANCE.popTransform(context.getMatrices());
    }
}
