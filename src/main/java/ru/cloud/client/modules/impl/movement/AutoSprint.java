package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;


import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "AutoSprint", category = Category.MOVEMENT, description = "Автоматически включает спринт")
public final class AutoSprint extends Module {
    public static final AutoSprint INSTANCE = new AutoSprint();
    private AutoSprint() {
    }
    @EventTarget
    public void onUpdate(EventUpdate event) {
        mc.options.sprintKey.setPressed(true);
    }
}
