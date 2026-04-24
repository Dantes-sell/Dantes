package ru.cloud.utility.game.other;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class SoundManager {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static final SoundEvent MOAN1    = SoundEvent.of(Identifier.of("dantes", "hitsound.moan1"));
    public static final SoundEvent MOAN2    = SoundEvent.of(Identifier.of("dantes", "hitsound.moan2"));
    public static final SoundEvent MOAN3    = SoundEvent.of(Identifier.of("dantes", "hitsound.moan3"));
    public static final SoundEvent MOAN4    = SoundEvent.of(Identifier.of("dantes", "hitsound.moan4"));
    public static final SoundEvent METALLIC = SoundEvent.of(Identifier.of("dantes", "hitsound.metallic"));
    public static final SoundEvent CRIME    = SoundEvent.of(Identifier.of("dantes", "hitsound.crime"));

    private SoundManager() {}

    public static void playSound(SoundEvent sound, float volume, float pitch) {
        if (mc.player == null || mc.world == null) return;
        mc.world.playSound(mc.player, mc.player.getBlockPos(), sound, SoundCategory.BLOCKS, volume, pitch);
    }

    public static void playSoundDirect(SoundEvent sound, float volume, float pitch) {
        mc.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
    }
}
