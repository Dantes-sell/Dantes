package ru.cloud.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.input.EventKey;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.KeySetting;
import ru.cloud.utility.game.player.PlayerIntersectionUtil;
import ru.cloud.utility.game.player.PlayerInventoryUtil;
import ru.cloud.utility.math.Timer;

@ModuleAnnotation(
        name = "ElytraHelper",
        category = Category.MISC,
        description = "Помогает свапать элитры и автоматизирует взлет"
)
public final class ElytraHelper extends Module {

    public static final ElytraHelper INSTANCE = new ElytraHelper();
    private ElytraHelper() {}

    private final KeySetting elytraSwapKey = new KeySetting("Элитры", -1);
    private final KeySetting fireworkKey = new KeySetting("Фейерверк", -1);

    private final BooleanSetting autoTakeoff = new BooleanSetting("Авто взлёт", true);
    private final BooleanSetting autoJump = new BooleanSetting("Авто прыжок", true);
    private final BooleanSetting autoFirework = new BooleanSetting("Авто фейерверк", false);
    private final BooleanSetting fireworkOnlyTakeoff = new BooleanSetting("Только при взлёте", false, autoFirework::isEnabled);
    private final BooleanSetting bypassCakeWorld = new BooleanSetting("Обход CakeWorld", false);

    private final Timer swapDelayTimer = new Timer();
    private final Timer autoFireworkTimer = new Timer();
    private final Timer swapCooldownTimer = new Timer();

    private boolean takeoffFireworkUsed = false;
    private boolean pendingManualFirework = false;
    private boolean recentlySwapped = false;

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        handleAutoJump();
        handleAutoTakeoff();
        handleAutoFireworkLoop();

        if (mc.player.isOnGround() || mc.player.isTouchingWater() || mc.player.isInLava()) {
            takeoffFireworkUsed = false;
        }

        if (recentlySwapped && swapCooldownTimer.finished(2000L)) {
            recentlySwapped = false;
        }

        if (pendingManualFirework && mc.player.isGliding()) {
            useFirework();
            pendingManualFirework = false;
        }
    }

    @EventTarget
    public void onKey(EventKey event) {
        if (mc.player == null || mc.world == null) return;

        if (event.isKeyDown(elytraSwapKey.getKeyCode()) && swapDelayTimer.finished(150L)) {
            boolean wasSprinting = mc.player.isSprinting();

            if (bypassCakeWorld.isEnabled() && wasSprinting) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                mc.player.setSprinting(false);
            }

            swapChestPiece();
            PlayerInventoryUtil.updateSlots();
            swapDelayTimer.reset();
            recentlySwapped = true;
            swapCooldownTimer.reset();

            if (bypassCakeWorld.isEnabled() && wasSprinting) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                mc.player.setSprinting(true);
            }
        }

        if (event.isKeyDown(fireworkKey.getKeyCode())) {
            pendingManualFirework = true;
        }
    }

    private void handleAutoJump() {
        if (!autoJump.isEnabled()) return;
        if (mc.player.getAbilities().flying) return;
        if (!mc.player.isOnGround()) return;
        if (mc.options.jumpKey.isPressed()) return;
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return;

        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isOf(Items.ELYTRA)) {
            mc.player.jump();
        }
    }

    private void handleAutoTakeoff() {
        if (!autoTakeoff.isEnabled()) return;
        if (mc.player.getAbilities().flying) return;
        if (mc.player.isOnGround()) return;
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return;
        if (mc.player.isGliding()) return;

        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (!chest.isOf(Items.ELYTRA)) return;

        PlayerIntersectionUtil.startFallFlying();

        if (autoFirework.isEnabled() && fireworkOnlyTakeoff.isEnabled() && !takeoffFireworkUsed) {
            if (PlayerInventoryUtil.getSlot(Items.FIREWORK_ROCKET) != null) {
                PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET);
                takeoffFireworkUsed = true;
            } else {
                notifyText("M", "Фейерверки не найдены");
            }
        }
    }

    private void handleAutoFireworkLoop() {
        if (!autoFirework.isEnabled()) return;
        if (fireworkOnlyTakeoff.isEnabled()) return;
        if (!mc.player.isGliding()) return;
        if (!autoFireworkTimer.finished(570L)) return;

        if (PlayerInventoryUtil.getSlot(Items.FIREWORK_ROCKET) != null) {
            PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET);
        } else {
            notifyText("M", "Фейерверки не найдены");
        }
        autoFireworkTimer.reset();
    }

    private void swapChestPiece() {
        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);

        if (chest.isOf(Items.ELYTRA)) {
            int armorSlot = findChestArmorSlot();
            if (armorSlot != -1) {
                PlayerInventoryUtil.moveItem(armorSlot, 6, false, true);
                return;
            }

            int freeSlot = findFreeInventorySlot();
            if (freeSlot != -1) {
                PlayerInventoryUtil.moveItem(6, freeSlot, false, true);
            }
            return;
        }

        int elytraSlot = findItemSlot(Items.ELYTRA);
        if (elytraSlot != -1) {
            PlayerInventoryUtil.moveItem(elytraSlot, 6, false, true);
        }
    }

    private int findFreeInventorySlot() {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return toScreenSlot(i);
            }
        }
        return -1;
    }

    private int findChestArmorSlot() {
        Item[] armors = {
                Items.NETHERITE_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE,
                Items.GOLDEN_CHESTPLATE,
                Items.IRON_CHESTPLATE,
                Items.LEATHER_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE
        };

        for (Item armor : armors) {
            int slot = findItemSlot(armor);
            if (slot != -1) return slot;
        }
        return -1;
    }

    private int findItemSlot(Item item) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                return toScreenSlot(i);
            }
        }
        return -1;
    }

    private int toScreenSlot(int invIndex) {
        return invIndex < 9 ? invIndex + 36 : invIndex;
    }

    private void useFirework() {
        if (!mc.player.isGliding()) return;

        if (mc.player.getItemCooldownManager().getCooldownProgress(Items.FIREWORK_ROCKET.getDefaultStack(), 0f) > 0f) {
            notifyText("N", "Фейерверк на перезарядке");
            return;
        }

        Slot fireworkSlot = PlayerInventoryUtil.getSlot(Items.FIREWORK_ROCKET);
        if (fireworkSlot == null) {
            notifyText("M", "Фейерверки не найдены");
            return;
        }

        if (fireworkSlot.id >= 36 && fireworkSlot.id <= 44) {
            PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET);
            return;
        }

        int freeHotbar = findFreeHotbar();
        if (freeHotbar == -1) freeHotbar = mc.player.getInventory().selectedSlot;

        PlayerInventoryUtil.moveItem(fireworkSlot.id, freeHotbar, false, true);
        PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET);
    }

    private int findFreeHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void notifyText(String icon, String message) {
        Zenith.getInstance().getNotifyManager().addNotification(icon, Text.of(message));
    }

    @Override
    public void onDisable() {
        swapDelayTimer.reset();
        autoFireworkTimer.reset();
        swapCooldownTimer.reset();

        pendingManualFirework = false;
        takeoffFireworkUsed = false;
        recentlySwapped = false;

        super.onDisable();
    }
}