package ru.cloud.base.rotation.mods.config;


import ru.cloud.base.rotation.mods.config.api.RotationConfig;
import ru.cloud.base.rotation.mods.config.api.RotationModeType;

public class InstantRotationConfig extends RotationConfig {
    @Override
    public RotationModeType getType() {
        return RotationModeType.INSTANT;
    }
}
