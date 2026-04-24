package ru.cloud.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.other.EventSpawnEntity;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "addEntity",at = @At(value = "RETURN"))
    public void injectAddEntity(Entity entity, CallbackInfo ci) {
        EventSpawnEntity eventSpawnEntity = new EventSpawnEntity(entity);
        EventManager.call(eventSpawnEntity);
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    public void injectRemoveEntity(int entityId, Entity.RemovalReason reason, CallbackInfo ci) {
        Zenith.getInstance().getHealthTracker().clearEntity(entityId);
    }
}
