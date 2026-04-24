package ru.cloud.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.math.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleAnnotation(name = "ChestStealer", category = Category.PLAYER, description = "Забирает все предметы из сундука")
public final class ChestStealer extends Module {

    public static final ChestStealer INSTANCE = new ChestStealer();
    private ChestStealer() {}

    final ModeSetting mode = new ModeSetting("Режим", "FunTime", "Обычный");

    final NumberSetting delayNormal = new NumberSetting("Задержка", 100f, 0f, 1000f, 1f,
            () -> mode.is("Обычный"));

    final NumberSetting delayFunTime = new NumberSetting("Задержка", 100f, 50f, 1000f, 1f,
            () -> mode.is("FunTime"));

    final BooleanSetting autoClose = new BooleanSetting("Закрыть при пустом", true);

    final Timer timer = new Timer();
    final Random random = new Random();

    long lastClickTime = 0;
    long nextDelay     = 0;

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.currentScreen instanceof GenericContainerScreen)) return;
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) return;

        int chestSize = handler.getInventory().size();

        // Проверяем пуст ли сундук
        boolean empty = true;
        for (int i = 0; i < chestSize; i++) {
            if (!handler.getInventory().getStack(i).isEmpty()) {
                empty = false;
                break;
            }
        }

        if (empty) {
            if (autoClose.isEnabled()) mc.player.closeHandledScreen();
            return;
        }

        if (mode.is("Обычный")) {
            handleNormal(handler, chestSize);
        } else {
            handleFunTime(handler, chestSize);
        }
    }

    private void handleNormal(GenericContainerScreenHandler handler, int chestSize) {
        long delay = (long) delayNormal.getCurrent();

        if (delay == 0) {
            // Забираем всё сразу
            for (int i = 0; i < chestSize; i++) {
                if (!handler.getInventory().getStack(i).isEmpty()) {
                    mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                }
            }
        } else {
            if (!timer.finished(delay)) return;
            for (int i = 0; i < chestSize; i++) {
                if (!handler.getInventory().getStack(i).isEmpty()) {
                    mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                    timer.reset();
                    break;
                }
            }
        }
    }

    private void handleFunTime(GenericContainerScreenHandler handler, int chestSize) {
        long baseDelay = (long) delayFunTime.getCurrent();
        if (System.currentTimeMillis() - lastClickTime < nextDelay) return;

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < chestSize; i++) {
            if (!handler.getInventory().getStack(i).isEmpty()) slots.add(i);
        }

        if (slots.isEmpty()) return;

        int slot = slots.get(random.nextInt(slots.size()));
        mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
        lastClickTime = System.currentTimeMillis();
        nextDelay = Math.max(0, baseDelay + (long)(random.nextDouble() * 40 - 20));
    }
}
