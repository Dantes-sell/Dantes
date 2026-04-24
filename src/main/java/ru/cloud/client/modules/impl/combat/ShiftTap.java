package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import ru.cloud.base.events.impl.player.EventAttack;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "ShiftTap", category = Category.COMBAT, description = "Нажимает шифт после удара")
public final class ShiftTap extends Module {

    public static final ShiftTap INSTANCE = new ShiftTap();
    private ShiftTap() {}

    private boolean shouldSneak = false;

    @EventTarget
    public void onAttack(EventAttack event) {
        if (mc.player == null) return;
        shouldSneak = true;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (shouldSneak) {
            mc.options.sneakKey.setPressed(true);
            shouldSneak = false;
        } else {
            mc.options.sneakKey.setPressed(false);
        }
    }

    @Override
    public void onDisable() {
        mc.options.sneakKey.setPressed(false);
        shouldSneak = false;
        super.onDisable();
    }
}
