package ru.cloud.utility.mixin.minecraft.network;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.base.events.impl.server.EventEntityStatus;
import ru.cloud.client.modules.impl.misc.UnHook;

@Mixin(ClientPlayNetworkHandler.class)
public class EntityStatusPacketMixin {

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (UnHook.UNHOOKED) return;
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.world == null) return;
        net.minecraft.entity.Entity entity = packet.getEntity(mc.world);
        if (entity == null) return;
        EventManager.call(new EventEntityStatus(entity, packet.getStatus()));
    }
}
