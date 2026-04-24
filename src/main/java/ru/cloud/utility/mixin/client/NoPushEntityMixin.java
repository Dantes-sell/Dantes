package ru.cloud.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.cloud.base.events.impl.player.EventPlayerCollision;
import ru.cloud.base.events.impl.player.EventPush;
import ru.cloud.utility.interfaces.IMinecraft;

@Mixin(Entity.class)
public class NoPushEntityMixin implements IMinecraft {

    @Inject(method = "updateVelocity", at = @At("HEAD"), cancellable = true)
    private void onWaterPush(float speed, Vec3d movementInput, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity)) return;
        EventPush event = new EventPush(EventPush.Type.WATER);
        EventManager.call(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "onBlockCollision", at = @At("HEAD"), cancellable = true)
    private void onBlockCollision(net.minecraft.block.BlockState state, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity)) return;
        Block block = state.getBlock();
        if (block == Blocks.POWDER_SNOW || block == Blocks.SWEET_BERRY_BUSH) {
            EventPlayerCollision event = new EventPlayerCollision(block);
            EventManager.call(event);
            if (event.isCancelled()) ci.cancel();
        }
    }
}
