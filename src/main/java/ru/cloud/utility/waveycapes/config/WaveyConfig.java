package ru.cloud.utility.waveycapes.config;

import ru.cloud.utility.waveycapes.enums.CapeMovement;
import ru.cloud.utility.waveycapes.enums.CapeStyle;
import ru.cloud.utility.waveycapes.enums.WindMode;

public class WaveyConfig {
    public WindMode windMode = WindMode.WAVES;
    public CapeStyle capeStyle = CapeStyle.SMOOTH;
    public CapeMovement capeMovement = CapeMovement.BASIC_SIMULATION;
    public int gravity = 25;
    public int heightMul = 5;
    public int straveMul = 5;
}
