package ru.cloud.client.modules.impl.render;

import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.MultiBooleanSetting;

import java.util.List;

@ModuleAnnotation(name = "NoRender", category = Category.RENDER, description = "Убирает лишние элементы с экрана")
public final class NoRender extends Module {
    public static final NoRender INSTANCE = new NoRender();

    private final MultiBooleanSetting settings = MultiBooleanSetting.create("Убрать", List.of(
            "Огонь",           // 0
            "Плохие эффекты",  // 1
            "Партиклы",        // 2
            "Голограммы",      // 3
            "Броня",           // 4
            "Тошнота",         // 5
            "Дождь",           // 6
            "Туман",           // 7
            "Небо",            // 8
            "Звёзды",          // 9
            "Облака",          // 10
            "Удар по экрану",  // 11
            "Тыква",           // 12
            "Лёд",             // 13
            "Паутина",         // 14
            "Скорборд",        // 15
            "Боссбар",         // 16
            "Тайтл",           // 17
            "Экшн-бар",        // 18
            "Тотем",           // 19
            "Фейерверки",      // 20
            "Фон контейнера",  // 21
            "Тени энтити"      // 22
    ));

    private NoRender() {}

    public boolean isRemoveFire()         { return isEnabled() && settings.isEnable(0); }
    public boolean isRemoveBadEffect()    { return isEnabled() && settings.isEnable(1); }
    public boolean isRemoveParticles()    { return isEnabled() && settings.isEnable(2); }
    public boolean isRemoveHolograms()    { return isEnabled() && settings.isEnable(3); }
    public boolean isRemoveArmorHud()     { return isEnabled() && settings.isEnable(4); }
    public boolean isRemoveNausea()       { return isEnabled() && settings.isEnable(5); }
    public boolean isRemoveRain()         { return isEnabled() && settings.isEnable(6); }
    public boolean isRemoveFog()          { return isEnabled() && settings.isEnable(7); }
    public boolean isRemoveSky()          { return isEnabled() && settings.isEnable(8); }
    public boolean isRemoveStars()        { return isEnabled() && settings.isEnable(9); }
    public boolean isRemoveClouds()       { return isEnabled() && settings.isEnable(10); }
    public boolean isRemoveHurtOverlay()  { return isEnabled() && settings.isEnable(11); }
    public boolean isRemovePumpkin()      { return isEnabled() && settings.isEnable(12); }
    public boolean isRemoveIce()          { return isEnabled() && settings.isEnable(13); }
    public boolean isRemoveUnderwaterFog(){ return isEnabled() && settings.isEnable(14); }
    public boolean isRemoveScoreboard()   { return isEnabled() && settings.isEnable(15); }
    public boolean isRemoveBossBar()      { return isEnabled() && settings.isEnable(16); }
    public boolean isRemoveTitle()        { return isEnabled() && settings.isEnable(17); }
    public boolean isRemoveActionBar()    { return isEnabled() && settings.isEnable(18); }
    public boolean isRemoveTotem()        { return isEnabled() && settings.isEnable(19); }
    public boolean isRemoveFireworks()    { return isEnabled() && settings.isEnable(20); }
    public boolean isRemoveContainerBackground() { return isEnabled() && settings.isEnable(21); }
    public boolean isRemoveEntityShadows()        { return isEnabled() && settings.isEnable(22); }
}
