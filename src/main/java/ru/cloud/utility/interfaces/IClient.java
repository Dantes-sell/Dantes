package ru.cloud.utility.interfaces;

import ru.cloud.Zenith;
import ru.cloud.base.rotation.AimManager;
import ru.cloud.base.rotation.RotationManager;

public interface IClient extends IWindow {
    Zenith zenith = Zenith.getInstance();
    RotationManager rotationManager = Zenith.getInstance().getRotationManager();
    AimManager aimManager = rotationManager.getAimManager();
}
