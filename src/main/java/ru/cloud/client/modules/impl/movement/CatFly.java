package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.player.PlayerIntersectionUtil;
import ru.cloud.utility.game.player.PlayerInventoryUtil;
import ru.cloud.utility.math.Timer;

@ModuleAnnotation(
        name = "CatFly",
        category = Category.MOVEMENT,
        description = "Полет, использующий элитры при этом летя на нагруднике"
)
public final class CatFly extends Module {
    public static final CatFly INSTANCE = new CatFly();

    private final Timer swapTimer = new Timer();
    private final Timer fireworkTimer = new Timer();
    private final NumberSetting timerStartFireWork = new NumberSetting("Задержка фейер", 4F, 1F, 15F, 1F);

    private int oldItem = -1;

    private CatFly() {}

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (PlayerInventoryUtil.getSlot(Items.FIREWORK_ROCKET) == null) return;

        int timeSwapMs = 550;
        int elytraHotbar = findElytraInHotbar();

        if (elytraHotbar != -1
                && !mc.player.isOnGround()
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && !mc.player.isGliding()) {

            if (swapTimer.finished(timeSwapMs)) {
                int syncId = mc.player.playerScreenHandler.syncId;
                mc.interactionManager.clickSlot(syncId, 6, elytraHotbar, SlotActionType.SWAP, mc.player);
                PlayerIntersectionUtil.startFallFlying();
                mc.interactionManager.clickSlot(syncId, 6, elytraHotbar, SlotActionType.SWAP, mc.player);
                oldItem = elytraHotbar;
                swapTimer.reset();
                fireworkTimer.reset();
            }
        }

        if (mc.player.isGliding() && fireworkTimer.finished((long) (timerStartFireWork.getCurrent() * 40F))) {
            if (mc.player.isUsingItem()) return;
            useFirework();
            fireworkTimer.reset();
        }
    }

    private void useFirework() {
        if (PlayerInventoryUtil.getSlot(Items.FIREWORK_ROCKET) == null) return;
        PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET);
    }

    private int findElytraInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.ELYTRA)) return i;
        }
        return -1;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player == null) return;

        if (oldItem != -1) {
            ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack oldHotbarStack = mc.player.getInventory().getStack(oldItem);
            if (chestStack.isOf(Items.ELYTRA) && oldHotbarStack.getItem() instanceof ArmorItem) {
                int syncId = mc.player.playerScreenHandler.syncId;
                mc.interactionManager.clickSlot(syncId, 6, oldItem, SlotActionType.SWAP, mc.player);
            }
            oldItem = -1;
        }

        mc.options.sneakKey.setPressed(false);
    }
}
