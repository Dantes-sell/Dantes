package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import lombok.Getter;


import ru.cloud.base.events.impl.entity.EventEntityColor;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.ColorSetting;
import ru.cloud.utility.render.display.base.color.ColorRGBA;

@Getter
@ModuleAnnotation(name = "Anti Invisible", category = Category.RENDER,description = "Видно инвизок")
public final class AntiInvisible extends Module {
    public static final AntiInvisible INSTANCE = new AntiInvisible();
    private AntiInvisible() {
    }
    private final ColorSetting colorSetting = new ColorSetting("Цвет", ColorRGBA.WHITE.mulAlpha(0.5f));

    @EventTarget
    public void onEntityColor(EventEntityColor e) {
        e.setColor(colorSetting.getColor().getRGB());
        e.cancel();
    }

}
