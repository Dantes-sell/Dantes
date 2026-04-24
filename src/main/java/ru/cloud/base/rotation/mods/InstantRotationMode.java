package ru.cloud.base.rotation.mods;


import ru.cloud.base.rotation.mods.api.RotationMode;
import ru.cloud.utility.game.player.rotation.Rotation;

public class InstantRotationMode extends RotationMode {

    public Rotation process(Rotation target) {

        return rotationManager.getCurrentRotation().add(rotationManager.getCurrentRotation().rotationDeltaTo(target));
    }
}
