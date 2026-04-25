package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.events.impl.player.EventSlowWalking;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.player.MovingUtil;

@ModuleAnnotation(name = "NoSlow", category = Category.MOVEMENT, description = "????????? ?????? NoSlow")
public final class NoSlow extends Module {
    public static final NoSlow INSTANCE = new NoSlow();

    private final ModeSetting mode = new ModeSetting("?????", "Vanilla", "Matrix", "Grim", "FunTime", "ReallyWorld", "SpookyTime");
    private final NumberSetting vanillaSpeed = new NumberSetting("????????", 0.6F, 0.1F, 1F, 0.05F, () -> mode.is("Vanilla"));
    private final BooleanSetting onlyGround = new BooleanSetting("Только на земле", false, () -> !mode.is("FunTime"));

    public static int ticks = 0;
    private static int cycleCounter = 0;

    private NoSlow() {
        mode.set("Grim");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ticks = 0;
        cycleCounter = 0;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) {
            return;
        }

        if ((mode.is("ReallyWorld") || mode.is("SpookyTime")) && !mc.player.isGliding()) {
            if (mc.player.isUsingItem()) {
                ticks++;
            } else {
                ticks = 0;
                cycleCounter = 0;
            }
        }
    }

    @EventTarget
    public void onSlowWalking(EventSlowWalking event) {
        if (mc.player == null || mc.player.isGliding() || !mc.player.isUsingItem()) {
            return;
        }

        if (onlyGround.isEnabled() && !mode.is("FunTime") && !mc.player.isOnGround()) {
            return;
        }

        switch (mode.get()) {
            case "Vanilla" -> handleVanillaMode(event);
            case "Grim" -> handleGrimMode(event);
            case "Matrix" -> handleMatrixMode(event);
            case "FunTime" -> handleFunTimeMode(event);
            case "ReallyWorld" -> handleReallyWorldMode(event);
            case "SpookyTime" -> handleSpookyTimeMode(event);
            default -> {
            }
        }
    }

    private void handleVanillaMode(EventSlowWalking event) {
        event.setCancelled(true);
        float speedMultiplier = vanillaSpeed.getCurrent();
        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x * speedMultiplier, velocity.y, velocity.z * speedMultiplier);
    }

    private void handleMatrixMode(EventSlowWalking event) {
        event.setCancelled(true);

        Vec3d velocity = mc.player.getVelocity();
        double motionX = velocity.x;
        double motionZ = velocity.z;

        boolean isFalling = mc.player.fallDistance > 0.725F;
        if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
            if (mc.player.age % 2 == 0) {
                boolean isNotStrafing = mc.player.sidewaysSpeed == 0.0F;
                float speedMultiplier = (isNotStrafing ? 0.5F : 0.4F) * 2.0F;
                motionX *= speedMultiplier;
                motionZ *= speedMultiplier;
            }
        } else if (isFalling) {
            float speedMultiplier = mc.player.fallDistance > 1.4F ? 0.95F : 0.97F;
            if (velocity.y < -0.5) {
                speedMultiplier *= 0.9F;
            } else if (velocity.y < -0.2) {
                speedMultiplier *= 0.95F;
            }
            motionX *= speedMultiplier;
            motionZ *= speedMultiplier;
        }

        if (mc.player.isTouchingWater()) {
            motionX *= 0.8F;
            motionZ *= 0.8F;
        }

        if (mc.player.isSneaking()) {
            motionX *= 0.5F;
            motionZ *= 0.5F;
        }

        mc.player.setVelocity(motionX, velocity.y, motionZ);
    }

    private void handleGrimMode(EventSlowWalking event) {
        String offhandUseAction = mc.player.getOffHandStack().getUseAction().name();
        if (("BLOCK".equals(offhandUseAction) || "EAT".equals(offhandUseAction))
                && mc.player.getActiveHand() == Hand.MAIN_HAND) {
            return;
        }

        if (mc.player.getActiveHand() == Hand.MAIN_HAND) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        sendItemChangePacket();
    }

    private void handleFunTimeMode(EventSlowWalking event) {
        if (mc.player.isOnGround() && mc.player.getMainHandStack().getItem() instanceof CrossbowItem) {
            event.setCancelled(true);
        }
    }

    private void handleReallyWorldMode(EventSlowWalking event) {
        int[] thresholds = {2, 3, 3};
        int threshold = thresholds[cycleCounter % thresholds.length];
        if (ticks >= threshold) {
            event.setCancelled(true);
            ticks = 0;
            cycleCounter++;
        }
    }

    private void handleSpookyTimeMode(EventSlowWalking event) {
        int[] thresholds = {2, 2, 2};
        int threshold = thresholds[cycleCounter % 2];
        if (ticks >= threshold) {
            event.setCancelled(true);
            ticks = 0;
            cycleCounter++;
        }
    }

    private void sendItemChangePacket() {
        if (!MovingUtil.hasPlayerMovement()) {
            return;
        }

        int currentSlot = mc.player.getInventory().selectedSlot;
        int nextSlot = (currentSlot % 8) + 1;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(nextSlot));
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
    }
}





