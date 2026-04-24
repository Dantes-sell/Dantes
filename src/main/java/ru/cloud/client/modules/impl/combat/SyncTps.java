package ru.cloud.client.modules.impl.combat;

import net.minecraft.util.math.MathHelper;
import ru.cloud.Zenith;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "SyncTps", category = Category.COMBAT, description = "Синхронизирует атаки с TPS сервера")
public final class SyncTps extends Module {

    public static final SyncTps INSTANCE = new SyncTps();
    private SyncTps() {}

    
    public float getCurrentTPS() {
        float tps = Zenith.getInstance().getServerHandler().getTPS();
        return MathHelper.clamp(tps, 0.1f, 20.0f);
    }

    /**
     * Скорректированный кулдаун с учётом TPS.
     * При низком TPS увеличивает задержку, чтобы удары успевали регистрироваться.
     */
    public long getAdjustedCooldown(long baseCooldown) {
        if (!isEnabled()) return baseCooldown;

        float tps = getCurrentTPS();
        if (tps >= 20.0f) return baseCooldown;

        float multiplier = 20.0f / tps;
        float additionalFactor = 1.0f + (20.0f - tps) * 0.05f;
        long adjusted = (long) (baseCooldown * multiplier * additionalFactor);

        return Math.min(adjusted, 3000L);
    }

    /**
     * Можно ли атаковать с учётом TPS.
     */
    public boolean canAttack(long lastAttackTime, long baseCooldown, long currentTime) {
        long cooldown = getAdjustedCooldown(baseCooldown);
        return currentTime >= lastAttackTime + cooldown;
    }
}

