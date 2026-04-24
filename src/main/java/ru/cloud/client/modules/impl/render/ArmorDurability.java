package ru.cloud.client.modules.impl.render;

import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
        name = "ArmorDurability",
        category = Category.RENDER,
        description = "\u041c\u0435\u043d\u044f\u0435\u0442 \u0446\u0432\u0435\u0442 \u0431\u0440\u043e\u043d\u0438 \u0432 \u0437\u0430\u0432\u0438\u0441\u0438\u043c\u043e\u0441\u0442\u0438 \u043e\u0442 \u0435\u0451 \u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u0438"
)
public final class ArmorDurability extends Module {
    public static final ArmorDurability INSTANCE = new ArmorDurability();

    private ArmorDurability() {
    }
}
