package ru.cloud.client.screens.dropdown;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.cloud.client.screens.menu.settings.impl.MenuColorSetting;
import ru.cloud.Zenith;
import ru.cloud.base.animations.base.Animation;
import ru.cloud.base.animations.base.Easing;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.base.theme.Theme;
import ru.cloud.client.screens.menu.elements.impl.MenuThemeElement;
import ru.cloud.client.screens.menu.settings.api.MenuPopupSetting;
import ru.cloud.utility.game.other.MouseButton;
import ru.cloud.utility.interfaces.IMinecraft;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.render.display.ScrollHandler;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.UIContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ThemeScreen extends Screen implements IMinecraft {

    private final Animation openAnim = new Animation(250, 0f, Easing.CUBIC_OUT);
    private boolean closing = false;
    private Screen parent;

    // те же элементы что в Default GUI
    private final List<MenuThemeElement> themeElements = new ArrayList<>();
    private final Set<MenuPopupSetting> popupSettings = new HashSet<>();
    private final ScrollHandler scrollHandler = new ScrollHandler();

    private float boxX, boxY, boxW, boxH;
    private boolean dragging;
    private float dragOffsetX, dragOffsetY;

    public ThemeScreen() {
        super(Text.of("Themes"));
        themeElements.add(new MenuThemeElement(Theme.DARK));
        themeElements.add(new MenuThemeElement(Theme.LIGHT));
        themeElements.add(new MenuThemeElement(Theme.CUSTOM_THEME));
    }

    public ThemeScreen(Screen parent) {
        this();
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        closing = false;
        openAnim.reset();
        boxW = 260f;
        boxH = 320f;
        boxX = (width - boxW) / 2f;
        boxY = (height - boxH) / 2f;
        // перехватываем popup color picker на себя
        MenuColorSetting.popupHandler = this::addPopupMenuSetting;
    }

    @Override
    public void removed() {
        MenuColorSetting.popupHandler = null;
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnim.update(closing ? 0f : 1f);
        if (openAnim.getValue() <= 0.01f && closing) {
            mc.setScreen(parent);
            return;
        }

        UIContext ctx = UIContext.of(context, mouseX, mouseY, delta);
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        float alpha = openAnim.getValue();

        float scale = 0.88f + 0.12f * alpha;
        ctx.pushMatrix();
        ctx.getMatrices().translate(boxX + boxW / 2f, boxY + boxH / 2f, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.getMatrices().translate(-(boxX + boxW / 2f), -(boxY + boxH / 2f), 0);

        // фон
        DrawUtil.drawBlur(ctx.getMatrices(), boxX, boxY, boxW, boxH, 20, BorderRadius.all(9), ColorRGBA.WHITE.mulAlpha(alpha));
        ctx.drawRoundedRect(boxX, boxY, boxW, boxH, BorderRadius.all(9), theme.getBackgroundColor().mulAlpha(alpha * 4));

        // заголовок
        ctx.drawRoundedRect(boxX, boxY, boxW, 28, BorderRadius.top(9, 9), theme.getForegroundColor().mulAlpha(alpha));
        Font titleFont = Fonts.SEMIBOLD.getFont(9);
        ctx.drawText(titleFont, "Темы", boxX + 12, boxY + (28 - titleFont.height()) / 2f, theme.getWhite().mulAlpha(alpha));

        // кнопка закрыть
        Font iconFont = Fonts.ICONS.getFont(7);
        float closeX = boxX + boxW - 20;
        float closeY = boxY + (28 - iconFont.height()) / 2f;
        ctx.drawText(iconFont, "M", closeX, closeY, theme.getGray().mulAlpha(alpha));

        // контент со скроллом
        float contentX = boxX + 8;
        float contentW = boxW - 16;
        float contentStartY = boxY + 36;
        float visibleH = boxH - 44;

        ctx.enableScissor((int) boxX, (int) contentStartY, (int) (boxX + boxW), (int) (boxY + boxH));

        Font font = Fonts.MEDIUM.getFont(7);
        float curY = contentStartY - (float) scrollHandler.getValue();
        double totalH = 0;

        for (MenuThemeElement el : themeElements) {
            el.render(ctx, mouseX, mouseY, font, contentX, curY, contentW, alpha, 0);
            float elH = el.getHeight() + 6;
            curY += elH;
            totalH += elH;
        }

        ctx.disableScissor();

        scrollHandler.update();
        scrollHandler.setMax(Math.max(0, totalH - visibleH));

        // popups поверх всего
        List<MenuPopupSetting> toRemove = new ArrayList<>();
        for (MenuPopupSetting popup : popupSettings) {
            popup.render(ctx, mouseX, mouseY, alpha, theme);
            if (popup.getAnimationScale().getValue() == 0) toRemove.add(popup);
        }
        popupSettings.removeAll(toRemove);

        ctx.popMatrix();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // popups первыми
        if (!popupSettings.isEmpty()) {
            for (MenuPopupSetting popup : popupSettings) {
                if (popup.getBounds().contains(mouseX, mouseY)) {
                    popup.onMouseClicked(mouseX, mouseY, MouseButton.fromButtonIndex(button));
                    return true;
                } else {
                    popup.getAnimationScale().update(0);
                }
            }
        }

        // кнопка закрыть
        Font iconFont = Fonts.ICONS.getFont(7);
        float closeX = boxX + boxW - 20;
        float closeY = boxY + (28 - iconFont.height()) / 2f;
        if (MathUtil.isHovered(mouseX, mouseY, closeX, closeY, 12, 12)) {
            closing = true;
            return true;
        }

        // drag по заголовку
        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, boxX, boxY, boxW, 28)) {
            dragging = true;
            dragOffsetX = (float) mouseX - boxX;
            dragOffsetY = (float) mouseY - boxY;
            return true;
        }

        // клики по элементам тем
        MouseButton mb = MouseButton.fromButtonIndex(button);
        for (MenuThemeElement el : themeElements) {
            el.onMouseClicked(mouseX, mouseY, mb);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        MouseButton mb = MouseButton.fromButtonIndex(button);
        for (MenuPopupSetting popup : popupSettings) {
            popup.onMouseReleased(mouseX, mouseY, mb);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            boxX = (float) mouseX - dragOffsetX;
            boxY = (float) mouseY - dragOffsetY;
            return true;
        }
        MouseButton mb = MouseButton.fromButtonIndex(button);
        for (MenuThemeElement el : themeElements) {
            el.onMouseDragged(mouseX, mouseY, mb, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!popupSettings.isEmpty()) return true;
        scrollHandler.scroll(verticalAmount * 20);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (MenuPopupSetting popup : popupSettings) {
            if (popup.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closing = true;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (MenuPopupSetting popup : popupSettings) {
            popup.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /** Вызывается из MenuThemeElement через addPopupMenuSetting */
    public void addPopupMenuSetting(MenuPopupSetting setting) {
        popupSettings.add(setting);
    }

    public void removePopupMenuSetting(MenuPopupSetting setting) {
        popupSettings.remove(setting);
    }
}
