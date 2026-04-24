package ru.cloud.utility.waveycapes;

import ru.cloud.utility.waveycapes.config.WaveyConfig;

public class WaveyCapesBase {
    public static WaveyCapesBase INSTANCE;
    public static WaveyConfig config;

    public static void init() {
        INSTANCE = new WaveyCapesBase();
        config = new WaveyConfig();
    }
}
