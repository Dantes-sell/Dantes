package ru.cloud.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import ru.cloud.base.events.impl.input.EventKey;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.KeySetting;
import ru.cloud.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(name = "ClickPearl", category = Category.PLAYER, description = "????????? ?????? ClickPearl")
public final class ClickPearl extends Module {
    public static final ClickPearl INSTANCE = new ClickPearl();

    private final KeySetting clickKey = new KeySetting("??????", -98);
    private final BooleanSetting legit = new BooleanSetting("????????", false);

    private int previousSlot = -1;
    private int pendingUseTicks = -1;
    private int pendingRestoreTicks = -1;

    private ClickPearl() {
    }

    @EventTarget
    public void onKey(EventKey e) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (e.isKeyDown(clickKey.getKeyCode())) {
            handleKeyPress();
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) {
            resetPending();
            return;
        }

        if (pendingUseTicks >= 0 && --pendingUseTicks == 0) {
            useMainHandPearl();
            pendingUseTicks = -1;
        }

        if (pendingRestoreTicks >= 0 && --pendingRestoreTicks == 0) {
            if (previousSlot >= 0 && previousSlot <= 8) {
                mc.player.getInventory().selectedSlot = previousSlot;
            }
            previousSlot = -1;
            pendingRestoreTicks = -1;
        }
    }

    private void handleKeyPress() {
        if (mc.player.getItemCooldownManager().getCooldownProgress(Items.ENDER_PEARL.getDefaultStack(), 0f) > 0f) {
            return;
        }

        if (legit.isEnabled()) {
            handleLegitUse();
            return;
        }

        Slot slot = PlayerInventoryUtil.getSlot(Items.ENDER_PEARL);
        if (slot == null) {
            return;
        }

        if (mc.player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
            useMainHandPearl();
            return;
        }

        PlayerInventoryUtil.swapAndUse(Items.ENDER_PEARL);
    }

    private void handleLegitUse() {
        int pearlSlot = findHotbarPearlSlot();
        if (pearlSlot == -1) {
            return;
        }

        previousSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = pearlSlot;
        pendingUseTicks = 3;
        pendingRestoreTicks = 5;
    }

    private void useMainHandPearl() {
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private int findHotbarPearlSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) {
                return i;
            }
        }
        return -1;
    }

    private void resetPending() {
        previousSlot = -1;
        pendingUseTicks = -1;
        pendingRestoreTicks = -1;
    }
}




