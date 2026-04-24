package ru.cloud.client.screens.menu.settings.api;

import lombok.Getter;
import ru.cloud.base.animations.base.Animation;
import ru.cloud.base.animations.base.Easing;
import ru.cloud.base.theme.Theme;
import ru.cloud.utility.render.display.base.ChangeRect;
import ru.cloud.utility.render.display.base.UIContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;

public abstract class MenuPopupSetting extends MenuSetting {
    @Getter
    protected final ChangeRect bounds;
    @Getter
    protected Animation animationScale = new Animation(200,0.01f, Easing.QUAD_IN_OUT);

    protected MenuPopupSetting(ChangeRect bounds) {
        this.bounds = bounds;
    }

    public abstract void render(UIContext ctx, float mouseX, float mouseY, float alpha, Theme theme);

    @Override
    public final void render(UIContext ctx, float mouseX, float mouseY, float x, float settingY, float moduleWidth, float alpha, float animEnable, ColorRGBA themeColor, ColorRGBA textColor, ColorRGBA descriptionColor, Theme theme) {
    }


    public abstract boolean charTyped(char chr, int modifiers);
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }
}
