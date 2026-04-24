package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import ru.cloud.base.events.impl.input.EventKey;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(name = "GrimGlide", category = Category.MOVEMENT, description = "Ускорение на элитре без фейеров")
public final class GrimGlide extends Module {

    public static final GrimGlide INSTANCE = new GrimGlide();

    public final BooleanSetting fireworkMode = new BooleanSetting("FireworkMode", false);
    public int ticksSinceLastBoost = 0;

    private GrimGlide() {
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ticksSinceLastBoost = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ticksSinceLastBoost = 0;
    }

    @EventTarget
    public void onKey(EventKey event) {
        ticksSinceLastBoost++;
    }
}
