package ru.cloud.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventManager;
import ru.cloud.Zenith;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.utility.game.other.MessageUtil;
import ru.cloud.utility.interfaces.IMinecraft;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleAnnotation(name = "UnHook", category = Category.MISC, description = "Полностью отключает чит")
public final class UnHook extends Module implements IMinecraft {
    public static final UnHook INSTANCE = new UnHook();
    public static volatile boolean UNHOOKED = false;
    private UnHook() {}
    private volatile boolean countingDown = false;

    @Override
    public void onEnable() {
        if (countingDown || UNHOOKED) return;
        countingDown = true;
        startCountdown();
    }

    private void startCountdown() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        final int[] remaining = {10};
        scheduler.scheduleAtFixedRate(() -> {
            if (remaining[0] > 0) {
                MessageUtil.displayMessage(MessageUtil.LogLevel.WARN,
                        "Клиент отключится через " + remaining[0] + " сек...");
                remaining[0]--;
            } else {
                scheduler.shutdown();
                killClient();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void killClient() {
        if (UNHOOKED) return;
        if (mc.player != null) {
            Zenith.INSTANCE.getRotationManager().reset();
        }

        UNHOOKED = true;
        for (Module module : Zenith.INSTANCE.getModuleManager().getModules()) {
            if (module == this) continue;
            try {
                if (module.isEnabled()) {
                    module.onDisable();
                    module.setEnabled(false);
                }
            } catch (Exception ignored) {}
        }

        EventManager.unregister(Zenith.INSTANCE.getModuleManager());
        EventManager.unregister(Zenith.INSTANCE.getRotationManager());
        EventManager.unregister(Zenith.INSTANCE.getNotifyManager());
        for (Module module : Zenith.INSTANCE.getModuleManager().getModules()) {
            EventManager.unregister(module);
        }

        Zenith.INSTANCE.getConfigManager().getScheduler().shutdownNow();
        mc.inGameHud.getChatHud().clear(false);
    }

    @Override
    public void onDisable() {
        if (UNHOOKED || countingDown) setEnabled(true);
    }
}
