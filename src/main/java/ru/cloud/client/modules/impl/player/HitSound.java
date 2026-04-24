package ru.cloud.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import ru.cloud.base.events.impl.player.EventAttack;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.other.SoundManager;

import java.util.concurrent.ThreadLocalRandom;

@ModuleAnnotation(name = "HitSound", category = Category.PLAYER, description = "Воспроизводит звук при ударе по сущности")
public final class HitSound extends Module {

    public static final HitSound INSTANCE = new HitSound();

    private HitSound() {}

    private final ModeSetting soundType = new ModeSetting("Тип звука", "Hentai", "Rust", "Soft");

    private final NumberSetting volume = new NumberSetting("Громкость", 1.0f, 0.1f, 2.0f, 0.1f);

    @EventTarget
    public void onAttack(EventAttack event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getAction() != EventAttack.Action.PRE) return;
        if (!(event.getTarget() instanceof LivingEntity)) return;

        playSelectedSound();
    }

    private void playSelectedSound() {
        float vol = volume.getCurrent();

        if (soundType.is("Hentai")) {
            playRandomMoan(vol, 1f);
        } else if (soundType.is("Rust")) {
            SoundManager.playSound(SoundManager.METALLIC, vol, 1f);
        } else if (soundType.is("soft")) {
            SoundManager.playSound(SoundManager.CRIME, vol, 1f);
        }
    }

    private void playRandomMoan(float vol, float pitch) {
        switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> SoundManager.playSound(SoundManager.MOAN1, vol, pitch);
            case 1 -> SoundManager.playSound(SoundManager.MOAN2, vol, pitch);
            case 2 -> SoundManager.playSound(SoundManager.MOAN3, vol, pitch);
            case 3 -> SoundManager.playSound(SoundManager.MOAN4, vol, pitch);
        }
    }
}
