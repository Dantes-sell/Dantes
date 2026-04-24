package ru.cloud.base.rotation;



import ru.cloud.base.rotation.mods.config.api.RotationConfig;
import ru.cloud.utility.game.player.rotation.Rotation;

import java.util.function.Supplier;


public record RotationTarget(Rotation targetRotation, Supplier<Rotation> rotation, RotationConfig rotationConfigBack) {
}
