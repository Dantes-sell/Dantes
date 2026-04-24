package ru.cloud.client.modules.impl.misc;

import ru.cloud.Zenith;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;

// ООО<<МИНЦЕТ ПАСТИНГ INC>>ООО
@ModuleAnnotation(name = "NameProtect", category = Category.MISC, description = "Защищает имена игроков")
public final class NameProtect extends Module {
    public static final NameProtect INSTANCE = new NameProtect();
    
    private NameProtect() {
    }

    private final BooleanSetting hideFriends = new BooleanSetting("Скрыть друзей", false);

    public static String getCustomName() {
        Module module = NameProtect.INSTANCE;
        return module != null && module.isEnabled() ? "Dantes Beta" : mc.player.getNameForScoreboard();
    }

    public static String getCustomName(String originalName) {
        Module module = NameProtect.INSTANCE;
        if (module == null || !module.isEnabled() || mc.player == null) {
            return originalName;
        }

        String me = mc.player.getNameForScoreboard();
        if (originalName.contains(me)) {
            return originalName.replace(me, "Dantes Beta");
        }

        if (module instanceof NameProtect nameProtect && nameProtect.hideFriends.isEnabled()) {
            var friends = Zenith.getInstance().getFriendManager().getItems();
            for (String friend : friends) {
                if (originalName.contains(friend)) {
                    return originalName.replace(friend, "Dantes Beta");
                }
            }
        }

        return originalName;
    }
}
