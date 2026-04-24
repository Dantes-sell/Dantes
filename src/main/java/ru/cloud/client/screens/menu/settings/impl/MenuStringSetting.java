package ru.cloud.client.screens.menu.settings.impl;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.base.theme.Theme;
import ru.cloud.client.modules.api.setting.impl.StringSetting;
import ru.cloud.client.screens.menu.settings.api.MenuSetting;
import ru.cloud.utility.game.other.MouseButton;
import ru.cloud.utility.render.display.UrlTextureLoader;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.Rect;
import ru.cloud.utility.render.display.base.UIContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

public class MenuStringSetting extends MenuSetting {

    private static final float PREVIEW_SIZE = 32f;
    private static final float PREVIEW_GAP  = 4f;

    @Getter
    private final StringSetting setting;
    private boolean focused      = false;
    private boolean allSelected  = false;
    private Rect    fieldBounds;
    private long    lastBlink    = 0;
    private boolean cursorVisible = true;

    // last URL for which we requested a texture (to detect changes)
    private String lastPreviewUrl = null;

    public MenuStringSetting(StringSetting setting) {
        this.setting = setting;
    }

    private boolean isUrlField() {
        String v = setting.getValue();
        return v.startsWith("http://") || v.startsWith("https://");
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float x, float settingY,
                       float moduleWidth, float alpha, float animEnable,
                       ColorRGBA themeColor, ColorRGBA textColor, ColorRGBA descriptionColor, Theme theme) {

        Font settingFont = Fonts.MEDIUM.getFont(7);
        Font optionFont  = Fonts.MEDIUM.getFont(6);
        Font iconFont    = Fonts.ICONS.getFont(6);

        boolean showPreview = isUrlField();
        float totalRowHeight = showPreview ? PREVIEW_SIZE + PREVIEW_GAP + 13f : 13f;
        float rowHeight = 13f;
        float centerY   = settingY + (rowHeight - settingFont.height()) / 2f - 0.5f;

        // icon + label
        ctx.drawText(iconFont, "E", x + 9, settingY + (rowHeight - iconFont.height()) / 2f - 1f, themeColor);
        ctx.drawText(settingFont, setting.getName(), x + 18, centerY, textColor);

        // input field
        float fieldWidth = moduleWidth / 2.2f;
        float fieldX     = x + moduleWidth - fieldWidth - 8;

        ColorRGBA fieldBg     = focused ? theme.getForegroundColor().mulAlpha(alpha) : theme.getForegroundLight().mulAlpha(alpha);
        ColorRGBA borderColor = focused ? themeColor.mulAlpha(alpha) : theme.getForegroundLightStroke().mulAlpha(alpha);

        ctx.drawRoundedRect(fieldX, settingY, fieldWidth, rowHeight, BorderRadius.all(3), fieldBg);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), fieldX, settingY, fieldWidth, rowHeight, 0.2f, BorderRadius.all(3), borderColor);

        float textPad  = 6f;
        float inputY   = settingY + (rowHeight - optionFont.height()) / 2f;
        float maxWidth = fieldWidth - textPad * 2f;

        ctx.enableScissor((int)(fieldX + 1), (int) settingY, (int)(fieldX + fieldWidth - 1), (int)(settingY + rowHeight));

        String text = setting.getValue();
        if (focused && allSelected && !text.isEmpty()) {
            float selW = Math.min(optionFont.width(text), maxWidth);
            ctx.drawRoundedRect(fieldX + textPad, inputY - 0.5f, selW, optionFont.height() + 1f,
                    BorderRadius.all(1), themeColor.mulAlpha(alpha * 0.35f));
            ctx.drawText(optionFont, text, fieldX + textPad, inputY, textColor);
        } else {
            long now = System.currentTimeMillis();
            if (now - lastBlink > 500) { cursorVisible = !cursorVisible; lastBlink = now; }
            String displayText = focused && cursorVisible ? text + "|" : text;
            float textW = optionFont.width(displayText);
            float drawX = fieldX + textPad;
            if (textW > maxWidth) drawX -= (textW - maxWidth);
            ctx.drawText(optionFont, displayText, drawX, inputY,
                    focused ? textColor : theme.getGray().mix(theme.getGrayLight(), animEnable).mulAlpha(alpha));
        }

        ctx.disableScissor();

        // --- URL preview ---
        if (showPreview) {
            String url = setting.getValue();

            // invalidate cache if URL changed
            if (!url.equals(lastPreviewUrl)) {
                if (lastPreviewUrl != null) UrlTextureLoader.invalidate(lastPreviewUrl);
                lastPreviewUrl = url;
            }

            UrlTextureLoader.Entry entry = UrlTextureLoader.get(url);
            float previewY = settingY + rowHeight + PREVIEW_GAP;
            float previewX = x + moduleWidth - PREVIEW_SIZE - 8;

            // placeholder background
            ctx.drawRoundedRect(previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE,
                    BorderRadius.all(4), theme.getForegroundLight().mulAlpha(alpha));

            if (entry != null && entry.state() == UrlTextureLoader.State.READY) {
                DrawUtil.drawRoundedTexture(ctx.getMatrices(), entry.id(),
                        previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE,
                        BorderRadius.all(4), ColorRGBA.WHITE.mulAlpha(alpha));
            } else if (entry == null || entry.state() == UrlTextureLoader.State.LOADING) {
                // spinning dots indicator
                long t = System.currentTimeMillis();
                int dot = (int)((t / 400) % 3);
                String dots = dot == 0 ? "." : dot == 1 ? ".." : "...";
                Font tiny = Fonts.MEDIUM.getFont(6);
                ctx.drawText(tiny, dots,
                        previewX + (PREVIEW_SIZE - tiny.width(dots)) / 2f,
                        previewY + (PREVIEW_SIZE - tiny.height()) / 2f,
                        theme.getGray().mulAlpha(alpha));
            } else {
                // failed
                Font tiny = Fonts.MEDIUM.getFont(6);
                String err = "err";
                ctx.drawText(tiny, err,
                        previewX + (PREVIEW_SIZE - tiny.width(err)) / 2f,
                        previewY + (PREVIEW_SIZE - tiny.height()) / 2f,
                        new ColorRGBA(255, 80, 80, (int)(200 * alpha)));
            }

            DrawUtil.drawRoundedBorder(ctx.getMatrices(), previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE,
                    0.2f, BorderRadius.all(4), borderColor);
        }

        fieldBounds = new Rect(fieldX, settingY, fieldWidth, rowHeight);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT && fieldBounds != null) {
            focused = fieldBounds.contains(mouseX, mouseY);
            if (!focused) allSelected = false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            focused = false;
            allSelected = false;
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            allSelected = true;
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            MinecraftClient.getInstance().keyboard.setClipboard(setting.getValue());
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                String base = allSelected ? "" : setting.getValue();
                String combined = base + clip;
                if (combined.length() > setting.getMaxLength())
                    combined = combined.substring(0, setting.getMaxLength());
                setting.setValue(combined);
                allSelected = false;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (allSelected || ctrl) {
                setting.setValue("");
            } else {
                String val = setting.getValue();
                if (!val.isEmpty()) setting.setValue(val.substring(0, val.length() - 1));
            }
            allSelected = false;
            return true;
        }
        allSelected = false;
        return true;
    }

    public boolean charTyped(char chr) {
        if (!focused) return false;
        if (allSelected) {
            setting.setValue(String.valueOf(chr));
            allSelected = false;
            return true;
        }
        String val = setting.getValue();
        if (val.length() < setting.getMaxLength()) setting.setValue(val + chr);
        return true;
    }

    @Override
    public float getWidth()  { return 0; }

    @Override
    public float getHeight() {
        return isUrlField() ? 13f + PREVIEW_GAP + PREVIEW_SIZE : 13f;
    }

    @Override
    public boolean isVisible() { return setting.getVisible().get(); }
}
