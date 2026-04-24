package ru.cloud.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.cloud.base.events.impl.input.EventKey;
import ru.cloud.client.modules.impl.misc.UnHook;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"))
    public void triggerKeyEvent(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (key == GLFW.GLFW_KEY_UNKNOWN || UnHook.UNHOOKED) return;
        EventManager.call(new EventKey(action, key));
    }



}
