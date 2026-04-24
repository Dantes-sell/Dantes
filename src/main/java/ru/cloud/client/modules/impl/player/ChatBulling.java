package ru.cloud.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.client.modules.impl.combat.Aura;
import ru.cloud.client.modules.impl.combat.AuraV2;
import ru.cloud.utility.math.Timer;

import java.util.List;
import java.util.Random;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleAnnotation(name = "ChatBulling", category = Category.PLAYER, description = "Текст")
public final class ChatBulling extends Module {

    public static final ChatBulling INSTANCE = new ChatBulling();
    private ChatBulling() {}

    final NumberSetting cooldown = new NumberSetting("Кулдаун (мс)", 3000f, 500f, 10000f, 100f,
            "Текст");

    final BooleanSetting onlyOnHit = new BooleanSetting("Текст", false);

    final Timer timer = new Timer();
    final Random random = new Random();

    int lastHurtTime = 0;

    private static final List<String> MESSAGES = List.of(
            "Текст",
            "Текст",
            "Текст",
            "Текст",
            "Текст",
            "Текст",
            "Текст",
            "Текст",
            "Текст",
            "%s skill issue detected. CloudClient powered by gods",
            "%s L + ratio + CloudClient boost",
            "%s bro got cooked by CloudClient LMAOOO",
            "Текст",
            "Текст",
            "%s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s"
    );

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        LivingEntity target = resolveAuraTarget();
        if (!(target instanceof PlayerEntity player)) {
            lastHurtTime = 0;
            return;
        }

        int currentHurtTime = player.hurtTime;

        
        boolean justHit = currentHurtTime > lastHurtTime && currentHurtTime >= 9;
        lastHurtTime = currentHurtTime;

        if (!justHit) return;
        if (!timer.finished((long) cooldown.getCurrent())) return;

        String name = player.getGameProfile().getName();
        String msg = MESSAGES.get(random.nextInt(MESSAGES.size() - 1)); 
        String formatted = msg.replace("%s", name);

        mc.player.networkHandler.sendChatMessage(formatted);
        timer.reset();
    }

    private LivingEntity resolveAuraTarget() {
        LivingEntity auraTarget = Aura.INSTANCE.isEnabled() ? Aura.INSTANCE.getTarget() : null;
        if (auraTarget != null) {
            return auraTarget;
        }
        return AuraV2.INSTANCE.isEnabled() ? AuraV2.INSTANCE.getTarget() : null;
    }
}

