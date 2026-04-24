package ru.cloud.utility.mixin.client.render.gui.hud;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.base.events.impl.render.EventRender2D;
import ru.cloud.client.modules.impl.misc.UnHook;
import ru.cloud.client.modules.impl.render.Crosshair;
import ru.cloud.client.modules.impl.render.Interface;
import ru.cloud.client.modules.impl.render.NoRender;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.shader.DrawUtil;
import ru.cloud.client.modules.api.Module;

import static ru.cloud.utility.interfaces.IMinecraft.mc;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (UnHook.UNHOOKED) return;
        DrawUtil.blurProgram.draw();
        CustomDrawContext customDrawContext = new CustomDrawContext(mc.getBufferBuilders().getEntityVertexConsumers());
        EventManager.call(new EventRender2D(customDrawContext, tickCounter.getTickDelta(false)));

    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void removeVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            Module crosshairModule = Crosshair.INSTANCE;
            if ( crosshairModule.isEnabled()) {
                ci.cancel();
            }
        } catch (Exception e) {
            // PIZDEC
        }
    }


    @Inject(method = "renderMainHud", at = @At(value = "HEAD"), cancellable = true)
    private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
                ci.cancel();
            }
        }
    }
    @Inject(method = "renderExperienceLevel", at = @At(value = "HEAD"), cancellable = true)
    private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
                ci.cancel();
            }
        }
    }
    @Inject(method = "renderPlayerList", at = @At(value = "HEAD"), cancellable = true)
    private void inject(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
               Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableTab()) {
                ci.cancel();
            }

    }
    @Inject(method = "renderOverlayMessage", at = @At(value = "HEAD"), cancellable = true)
    private void injectRenderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveActionBar()) { ci.cancel(); return; }
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
                ci.cancel();
            }
        }
    }
    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At(value = "HEAD"), cancellable = true)
    private void injectRenderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveScoreboard()) { ci.cancel(); return; }
        Interface interfaceModule = Interface.INSTANCE;
        if (interfaceModule.isEnabled() && interfaceModule.isEnableScoreBar()) {
            ci.cancel();
        }
    }


    @ModifyVariable(
            method = "renderStatusBars",
            at = @At(value = "STORE"),
            ordinal = 3
    )
    private int modifyM(int original, DrawContext context) {
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
                return context.getScaledWindowWidth() / 2 + 90 + 36;
            }
        }
        return original;
    }

    // NoRender: ����� + ��������
    @Inject(method = "renderTitleAndSubtitle", at = @At("HEAD"), cancellable = true)
    private void noRenderTitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveTitle()) ci.cancel();
    }

    // NoRender: �������
    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    private void noRenderNausea(DrawContext context, float nauseaStrength, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveNausea()) ci.cancel();
    }

    // NoRender: ������ �������
    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void noRenderPortal(DrawContext context, float portalTime, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveNausea()) ci.cancel();
    }

    // NoRender: ???? ?? ?????? (vignette ??? ????????? ?????)
    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void noRenderHurtOverlay(DrawContext context, net.minecraft.entity.Entity entity, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveHurtOverlay()) ci.cancel();
    }

    // NoRender: броня (armor bar)
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static void noRenderArmorBar(DrawContext context, net.minecraft.entity.player.PlayerEntity player,
                                         int top, int left, int lines, int filled, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveArmorHud()) ci.cancel();
    }

}
