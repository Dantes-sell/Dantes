package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.player.EventMoveInput;
import ru.cloud.base.rotation.RotationTarget;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.utility.game.player.MovingUtil;
import ru.cloud.utility.game.player.PlayerIntersectionUtil;
import ru.cloud.utility.game.player.rotation.Rotation;

@ModuleAnnotation(name = "ElytraRecast", description = "Позволяет выше прыгать на элитрах", category = Category.MOVEMENT)
public final class ElytraRecast extends Module {
    public static final ElytraRecast INSTANCE = new ElytraRecast();

    private ElytraRecast() {

    }




    private int groundTick = 0;
    private boolean changed = false;
    @EventTarget
    public void update(EventMoveInput eventUpdate) {

        if(mc.player.isUsingItem()){
            if (Zenith.getInstance().getServerHandler().isServerSprint()) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                mc.player.setSprinting(false);
            }

            groundTick =5;
        }else if(groundTick>0){
            groundTick--;
            return;
        }

        if (!mc.player.isUsingItem()&& !mc.player.isTouchingWater()&&mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA) && MovingUtil.hasPlayerMovement()) {
            if (mc.player.isOnGround() && mc.player.isWalking()) {
               if(mc.player.canSprint()	&& mc.player.isWalking() &&	 !mc.player.isBlind() && !mc.player.isUsingItem()&& (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater())) {
                    if (!mc.player.isSprinting() && Zenith.getInstance().getServerHandler().isServerSprint()) {
                        mc.player.setSprinting(true);
                    }
                    if (!Zenith.getInstance().getServerHandler().isServerSprint()) {
                        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                        mc.player.setSprinting(true);
                        changed = true;
                    }
                }else {
                   if (Zenith.getInstance().getServerHandler().isServerSprint()) {
                       mc.player.lastSprinting =true;
                       mc.player.setSprinting(false);
                   }
                   mc.player.setSprinting(false);
               }


                    mc.player.jump();



            } else if (!mc.player.isGliding()) {
                PlayerIntersectionUtil.startFallFlying();


            }

        } else {

            if (changed&&Zenith.getInstance().getServerHandler().isServerSprint()) {
                mc.player.lastSprinting =true;
                mc.player.setSprinting(false);
                changed = false;
            }

        }
        if (groundTick > 0) {

            if (false) {
                rotationManager.setRotation(new RotationTarget(new Rotation(rotationManager.getCurrentRotation().getYaw(), -50), () -> aimManager.rotate(aimManager.getInstantSetup(), new Rotation(rotationManager.getCurrentRotation().getYaw(), -50)), aimManager.getAiSetup()), 2, this);
            }

            groundTick--;
        }

    }

    @Override
    public void onDisable() {
        if (Zenith.getInstance().getServerHandler().isServerSprint() &&changed) {
           // mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            mc.player.lastSprinting =true;
            mc.player.setSprinting(false);
        }

        super.onDisable();
    }
}
