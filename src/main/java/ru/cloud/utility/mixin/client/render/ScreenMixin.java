package ru.cloud.utility.mixin.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.client.modules.impl.render.InventoryAnimation;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "renderInGameBackground", at = @At("HEAD"), cancellable = true)
    private void cancelInGameBackground(DrawContext context, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (InventoryAnimation.INSTANCE.isEnabled() && mc.currentScreen instanceof HandledScreen) {
            ci.cancel();
        }
    }
}

