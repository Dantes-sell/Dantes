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
@ModuleAnnotation(name = "ChatBulling", category = Category.PLAYER, description = "Настройки модуля ChatBulling")
public final class ChatBulling extends Module {

    public static final ChatBulling INSTANCE = new ChatBulling();
    private ChatBulling() {}

    final NumberSetting cooldown = new NumberSetting("Кулдаун (мс)", 3000f, 500f, 10000f, 100f,
            "Задержка между сообщениями");

    final BooleanSetting onlyOnHit = new BooleanSetting("Только при ударе", false);

    final Timer timer = new Timer();
    final Random random = new Random();

    int lastHurtTime = 0;

    private static final List<String> MESSAGES = List.of(
            "%s, учись играть.",
            "%s, слишком легко.",
            "%s, это было быстро.",
            "%s, не твой день.",
            "%s, попробуй еще раз.",
            "%s, без шансов.",
            "%s, GG.",
            "%s, неплохая попытка.",
            "%s, до следующего раунда.",
            "%s, навык не найден. Dantes уже впереди.",
            "%s, без шансов. Dantes сильнее.",
            "%s, тебя переиграл Dantes.",
            "%s, тренируйся дальше.",
            "%s, реванш?",
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


