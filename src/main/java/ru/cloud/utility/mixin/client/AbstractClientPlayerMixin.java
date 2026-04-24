package ru.cloud.utility.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ru.cloud.utility.waveycapes.WaveyCapesBase;
import ru.cloud.utility.waveycapes.interfaces.CapeHolder;
import ru.cloud.utility.waveycapes.sim.StickSimulation;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerMixin implements CapeHolder {

    @Unique
    private StickSimulation zenith$simulation;

    @Override
    public StickSimulation getSimulation() {
        if (zenith$simulation == null && WaveyCapesBase.config != null) {
            zenith$simulation = new StickSimulation();
        }
        return zenith$simulation;
    }
}
