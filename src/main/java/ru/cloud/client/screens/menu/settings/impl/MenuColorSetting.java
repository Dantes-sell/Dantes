package ru.cloud.client.screens.menu.settings.impl;

import lombok.Getter;
import ru.cloud.Zenith;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.base.theme.Theme;
import ru.cloud.client.modules.api.setting.impl.ColorSetting;
import ru.cloud.client.screens.menu.settings.api.MenuSetting;
import ru.cloud.client.screens.menu.settings.impl.popup.MenuColorPopupSetting;
import ru.cloud.utility.game.other.MouseButton;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.ChangeRect;
import ru.cloud.utility.render.display.base.Rect;
import ru.cloud.utility.render.display.base.UIContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;

public class MenuColorSetting  extends MenuSetting {
    @Getter
    private final ColorSetting setting;

    
    public static java.util.function.Consumer<MenuColorPopupSetting> popupHandler = null;


    private Rect bounds;
    private ChangeRect boundsColor;


    public MenuColorSetting(ColorSetting setting) {

        this.setting = setting;
        boundsColor = new ChangeRect(0,0,156/2f,96/2f);
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float x, float settingY, float moduleWidth, float alpha, float animEnable, ColorRGBA themeColor, ColorRGBA textColor, ColorRGBA descriptionColor, Theme theme) {
        float settingHeight = 24;
        float settingX = x + 8;
        Font settingFont = Fonts.MEDIUM.getFont(7);
        Font descFont = Fonts.MEDIUM.getFont(6);
        float textY = settingY + (8 - settingFont.height()) / 2 - 0.5f;

        ctx.drawText(settingFont, setting.getName(), x + 8 + 10, textY, textColor);
        // ctx.drawText(descFont, setting.getDescription(), settingX + 15, textY + 10, theme.getWhite().mulAlpha(alpha));






        float iconSize = 6;
        float iconY = textY - 1;
        Font iconFont = Fonts.ICONS.getFont(6);
       // ctx.drawRoundedRect(settingX, iconY, iconSize, iconSize, BorderRadius.all(1), themeColor);

        ctx.drawText(Fonts.ICONS.getFont(6), "V", settingX+1.5f, iconY + 1,themeColor);
        // ctx.drawTexture(Zenith.id("icons/check.png"), settingX + 1.5f, iconY + 2f, 7, 7, Zenith.getInstance().getThemeManager().getCurrentTheme().getForegroundColor().mulAlpha(alpha));

        float toggleSize = 8;
        float toggleX = x + moduleWidth - toggleSize - 8;
        float toggleY = settingY;

        ColorRGBA colorEnable = theme.getWhiteGray().mix(setting.getColor(),animEnable).mulAlpha(alpha);
        ctx.drawRoundedBorder(toggleX-0.8f, toggleY-0.8f, toggleSize+0.8f*2, toggleSize+0.8f*2,0.1f, BorderRadius.all(3), themeColor);

        ctx.drawRoundedRect(toggleX, toggleY, toggleSize, toggleSize, BorderRadius.all(3), colorEnable);

        //  ctx.drawText(iconFont,"S",toggleX+2,toggleY+1,golochakaFinalColor);
        bounds = new Rect(toggleX, toggleY, toggleSize, toggleSize);
        boundsColor.setX(toggleX+20);
        boundsColor.setY(toggleY+toggleSize-boundsColor.getHeight()/2);


    }


    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (bounds != null && bounds.contains(mouseX, mouseY)) {
            MenuColorPopupSetting popup = new MenuColorPopupSetting(boundsColor, this.setting);
            if (popupHandler != null) {
                popupHandler.accept(popup);
            } else {
                Zenith.getInstance().getMenuScreen().addPopupMenuSetting(popup);
            }
        }
    }

    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public float getHeight() {
        return 8;
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}

