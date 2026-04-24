package ru.cloud.client.modules.impl.misc;

import net.minecraft.client.gui.screen.DeathScreen;
import com.darkmagician6.eventapi.EventTarget;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "AutoRespawn", category = Category.MISC, description = "Автоматически возрождается после смерти")
public final class AutoRespawn extends Module {
    public static final AutoRespawn INSTANCE = new AutoRespawn();
    
    private AutoRespawn() {
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.currentScreen instanceof DeathScreen && mc.player.deathTime > 5) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}
