package ru.cloud.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.client.modules.api.setting.impl.StringSetting;
import ru.cloud.utility.math.Timer;

@ModuleAnnotation(name = "ChatSpammer", category = Category.MISC, description = "Автоматически отправляет фразу в чат")
public final class ChatSpammer extends Module {

    public static final ChatSpammer INSTANCE = new ChatSpammer();

    private final StringSetting message = new StringSetting("Message", "Фраза для спама", "Hello from Dantes!", 256);
    private final NumberSetting delayMs = new NumberSetting("Delay (ms)", 1500f, 250f, 10000f, 50f);
    private final Timer timer = new Timer();

    private ChatSpammer() {
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return;
        }

        String text = message.getValue();
        if (text == null || text.isBlank()) {
            return;
        }

        if (!timer.finished((long) delayMs.getCurrent())) {
            return;
        }

        mc.player.networkHandler.sendChatMessage(text);
        timer.reset();
    }

    @Override
    public void onEnable() {
        timer.reset();
        super.onEnable();
    }
}

