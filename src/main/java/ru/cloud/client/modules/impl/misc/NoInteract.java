package ru.cloud.client.modules.impl.misc;

import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.Category;

@ModuleAnnotation(name = "NoInteract", category = Category.MISC, description = "Не дает открыть контейнера")
public final class NoInteract extends Module {
    public static final NoInteract INSTANCE = new NoInteract();
    
    private NoInteract() {
    }
}
