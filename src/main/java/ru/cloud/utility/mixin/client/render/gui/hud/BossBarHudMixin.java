package ru.cloud.utility.mixin.client.render.gui.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.client.modules.impl.render.HudBeta;
import ru.cloud.client.modules.impl.render.Interface;
import ru.cloud.client.modules.impl.render.NoRender;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    private boolean didPush = false;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderBossBarHead(DrawContext context, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveBossBar() || HudBeta.shouldHideBossBar()) {
            ci.cancel();
            didPush = false;
            return;
        }

        float shift = Interface.getBossBarShift();
        if (shift > 0f) {
            context.getMatrices().push();
            context.getMatrices().translate(0f, shift, 0f);
            didPush = true;
        } else {
            didPush = false;
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderBossBarTail(DrawContext context, CallbackInfo ci) {
        if (didPush) {
            context.getMatrices().pop();
            didPush = false;
        }
    }
}
