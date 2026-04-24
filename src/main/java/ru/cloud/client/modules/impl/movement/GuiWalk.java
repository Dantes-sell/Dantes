package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import ru.cloud.base.events.impl.other.EventClickSlot;
import ru.cloud.base.events.impl.other.EventCloseScreen;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.server.EventPacket;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.utility.game.player.MovingUtil;
import ru.cloud.utility.game.player.PlayerIntersectionUtil;
import ru.cloud.utility.game.player.PlayerInventoryComponent;
import ru.cloud.utility.game.player.PlayerInventoryUtil;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(
        name = "GuiMove",
        category = Category.MOVEMENT,
        description = "\u0425\u043e\u0434\u044c\u0431\u0430 \u0432 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u0435"
)
public final class GuiWalk extends Module {
    public static final GuiWalk INSTANCE = new GuiWalk();

    public static final ModeSetting mode = new ModeSetting("\u041e\u0431\u0445\u043e\u0434", "Vanila", "Spooky", "Grim");
    public static final BooleanSetting syncSwap = new BooleanSetting("\u0421\u0438\u043d\u0445 \u0441\u0432\u0430\u043f\u044b", false, () -> !mode.is("Vanila"));

    private final List<ClickSlotC2SPacket> pendingPackets = new ArrayList<>();
    private int tick;

    private GuiWalk() {
    }

    @EventTarget
    public void onTick(EventUpdate event) {
        if (mc.player == null) {
            return;
        }

        KeyBinding[] pressedKeys = {
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey,
                mc.options.sprintKey
        };

        if (tick > 0) {
            for (KeyBinding keyBinding : pressedKeys) {
                keyBinding.setPressed(false);
            }
            tick--;
            return;
        }

        if (mc.currentScreen == null
                || mc.currentScreen instanceof ChatScreen
                || mc.currentScreen instanceof SignEditScreen) {
            return;
        }

        if (mode.is("Grim") && mc.currentScreen instanceof HandledScreen<?> handled && !(handled instanceof InventoryScreen)) {
            return;
        }

        for (KeyBinding keyBinding : pressedKeys) {
            keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyBinding.getDefaultKey().getCode()));
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null) {
            return;
        }

        if (event.getPacket() instanceof CloseScreenS2CPacket closeScreenPacket
                && closeScreenPacket.getSyncId() == 0
                && !pendingPackets.isEmpty()) {
            event.cancel();
            return;
        }

        if (!(event.getPacket() instanceof ClickSlotC2SPacket clickPacket)) {
            return;
        }

        if (!MovingUtil.hasPlayerMovement() || !(mc.currentScreen instanceof InventoryScreen)) {
            return;
        }

        if (mode.is("Grim") || mode.is("Spooky")) {
            pendingPackets.add(clickPacket);
            event.cancel();
        }
    }

    @EventTarget
    public void onClickSlot(EventClickSlot event) {
        SlotActionType actionType = event.getActionType();
        if ((!pendingPackets.isEmpty() || MovingUtil.hasPlayerMovement())
                && event.getButton() == 1
                && actionType != SlotActionType.SWAP
                && actionType != SlotActionType.THROW) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onCloseScreen(EventCloseScreen event) {
        if (!(mc.currentScreen instanceof InventoryScreen) || pendingPackets.isEmpty()) {
            pendingPackets.clear();
            return;
        }

        if (!MovingUtil.hasPlayerMovement() || mode.is("Vanila")) {
            flushPendingPackets();
            return;
        }

        tick = 5;
        int delay = mode.is("Spooky") ? 90 : 40;
        new Thread(() -> {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {
            }
            mc.execute(this::flushPendingPackets);
        }, "GuiMove-Flush").start();
    }

    private void flushPendingPackets() {
        if (pendingPackets.isEmpty()) {
            return;
        }

        pendingPackets.forEach(PlayerIntersectionUtil::sendPacketWithOutEvent);
        pendingPackets.clear();
        PlayerInventoryUtil.updateSlots();
    }

    public static void stopMovementTemporarily(int ticks) {
        if (INSTANCE.isEnabled()) {
            INSTANCE.tick = Math.max(INSTANCE.tick, ticks);
        }
    }

    @Override
    public void onDisable() {
        pendingPackets.clear();
        PlayerInventoryComponent.unPressMoveKeys();
        super.onDisable();
    }
}
