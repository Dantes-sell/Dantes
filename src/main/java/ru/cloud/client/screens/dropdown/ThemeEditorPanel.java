package ru.cloud.client.screens.dropdown;

import ru.cloud.Zenith;
import ru.cloud.base.font.Fonts;
import ru.cloud.base.theme.Theme;
import ru.cloud.base.theme.ThemeManager;
import ru.cloud.client.modules.api.setting.impl.ColorSetting;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * ������ ������ � �������������� ���� ��� DropDown GUI.
 * ���������� ������ ��� � (��� Custom) �������� ������.
 */
public class ThemeEditorPanel {

    private float x, y, width;
    private float height = 0;

    // ColorSetting-� ��� Custom ���� (�� �� ��� � MenuThemeElement)
    private final List<ThemeColorRow> colorRows = new ArrayList<>();

    // скролл
    private float scrollOffset = 0;
    private float maxScroll = 0;
    private static final float MAX_VISIBLE_HEIGHT = 320f;

    public ThemeEditorPanel(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
        buildColorRows();
    }

    private void buildColorRows() {
        colorRows.clear();
        Theme t = Theme.CUSTOM_THEME;
        colorRows.add(new ThemeColorRow("Текст",      new ColorSetting("color",      t.getColor(),      Theme.DARK::getColor)));
        colorRows.add(new ThemeColorRow("Текст",     new ColorSetting("second",     t.getSecondColor(),Theme.DARK::getSecondColor)));
        colorRows.add(new ThemeColorRow("Фон гуи",            new ColorSetting("bg",         t.getBackgroundColor(), Theme.DARK::getBackgroundColor)));
        colorRows.add(new ThemeColorRow("Текст",       new ColorSetting("fg",         t.getForegroundColor(), Theme.DARK::getForegroundColor)));
        colorRows.add(new ThemeColorRow("Текст",        new ColorSetting("fgLight",    t.getForegroundLight(), Theme.DARK::getForegroundLight)));
        colorRows.add(new ThemeColorRow("Текст",         new ColorSetting("fgDark",     t.getForegroundDark(),  Theme.DARK::getForegroundDark)));
        colorRows.add(new ThemeColorRow("Текст",     new ColorSetting("white",      t.getWhite(),      Theme.DARK::getWhite)));
        colorRows.add(new ThemeColorRow("Текст",         new ColorSetting("gray",       t.getGray(),       Theme.DARK::getGray)));
    }

    public void render(CustomDrawContext ctx, int mouseX, int mouseY, float alpha, Theme theme) {
        ThemeManager tm = Zenith.getInstance().getThemeManager();
        List<Theme> themes = tm.getThemes();

        float padding = 8f;
        float rowH = 22f;
        float themeRowH = rowH * themes.size() + padding * 2;
        boolean isCustom = tm.is(Theme.CUSTOM_THEME);
        float colorRowH = isCustom ? colorRows.size() * 20f + padding : 0;
        float totalContentH = themeRowH + colorRowH + (isCustom ? 4 : 0);

        maxScroll = Math.max(0, totalContentH - MAX_VISIBLE_HEIGHT);
        height = Math.min(totalContentH, MAX_VISIBLE_HEIGHT);

        // фон панели
        DrawUtil.drawBlur(ctx.getMatrices(), x, y, width, height, 20, BorderRadius.all(6), ColorRGBA.WHITE.mulAlpha(alpha));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height,
                BorderRadius.all(6), theme.getForegroundColor().withAlpha((int) (220 * alpha)));

        ctx.enableScissor((int) x, (int) y, (int) (x + width), (int) (y + height));

        float curY = y + padding - scrollOffset;

        // кнопки тем
        for (Theme t : themes) {
            boolean selected = tm.is(t);
            ColorRGBA btnBg = selected
                    ? t.getColor().withAlpha((int) (200 * alpha))
                    : theme.getForegroundLight().withAlpha((int) (180 * alpha));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + padding, curY, width - padding * 2, rowH - 2,
                    BorderRadius.all(4), btnBg);

            // ������� ���������
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + padding + 4, curY + (rowH - 2 - 10) / 2f, 10, 10,
                    BorderRadius.all(2), t.getColor(), t.getSecondColor(), t.getSecondColor(), t.getColor());

            ctx.drawText(Fonts.MEDIUM.getFont(7), t.getName(),
                    x + padding + 18, curY + (rowH - 2 - 7) / 2f,
                    theme.getWhite().withAlpha((int) (255 * alpha)));

            curY += rowH;
        }

        // �������� ������ ��� Custom
        if (isCustom) {
            curY += 4;
            ctx.drawText(Fonts.SEMIBOLD.getFont(7), "Custom",
                    x + padding, curY,
                    theme.getColor().withAlpha((int) (200 * alpha)));
            curY += 12;

            for (ThemeColorRow row : colorRows) {
                float rowY = curY;
                // название
                ctx.drawText(Fonts.MEDIUM.getFont(6), row.label,
                        x + padding, rowY + 3,
                        theme.getWhite().withAlpha((int) (220 * alpha)));

                
                float sqX = x + width - padding - 14;
                float sqY = rowY + 1;
                DrawUtil.drawRoundedRect(ctx.getMatrices(), sqX, sqY, 14, 14,
                        BorderRadius.all(3), row.setting.getColor().withAlpha((int) (255 * alpha)));
                DrawUtil.drawRoundedBorder(ctx.getMatrices(), sqX, sqY, 14, 14,
                        0.8f, BorderRadius.all(3), theme.getWhite().withAlpha((int) (80 * alpha)));

                row.sqX = sqX; row.sqY = sqY;
                curY += 20;
            }

            // ��������� ����� � Custom ���� ������ ����
            applyCustomColors();
        }

        ctx.disableScissor();
    }

    private void applyCustomColors() {
        if (colorRows.size() < 8) return;
        Theme t = Theme.CUSTOM_THEME;
        t.setColor(colorRows.get(0).setting.getColor());
        t.setSecondColor(colorRows.get(1).setting.getColor());
        t.setBackgroundColor(colorRows.get(2).setting.getColor());
        t.setForegroundColor(colorRows.get(3).setting.getColor());
        t.setForegroundLight(colorRows.get(4).setting.getColor());
        t.setForegroundDark(colorRows.get(5).setting.getColor());
        t.setWhite(colorRows.get(6).setting.getColor());
        t.setGray(colorRows.get(7).setting.getColor());
    }

    /** ���������� ColorSetting �� ����� �� �������, ��� null. */
    public ColorSetting getClickedColorSetting(double mouseX, double mouseY) {
        for (ThemeColorRow row : colorRows) {
            if (MathUtil.isHovered(mouseX, mouseY, row.sqX, row.sqY, 14, 14)) {
                return row.setting;
            }
        }
        return null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!MathUtil.isHovered(mouseX, mouseY, x, y, width, height)) return false;

        ThemeManager tm = Zenith.getInstance().getThemeManager();
        List<Theme> themes = tm.getThemes();
        float padding = 8f;
        float rowH = 22f;
        float curY = y + padding - scrollOffset;

        for (Theme t : themes) {
            if (MathUtil.isHovered(mouseX, mouseY, x + padding, curY, width - padding * 2, rowH - 2)) {
                tm.switchTheme(t);
                if (tm.is(Theme.CUSTOM_THEME)) buildColorRows();
                return true;
            }
            curY += rowH;
        }
        return true; // ��������� ���� ������ ������
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!MathUtil.isHovered(mouseX, mouseY, x, y, width, height)) return false;
        scrollOffset -= (float) (delta * 15);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getHeight() { return height; }
    public float getWidth()  { return width; }

    public boolean isHovered(double mouseX, double mouseY) {
        return MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
    }

    static class ThemeColorRow {
        final String label;
        final ColorSetting setting;
        float sqX, sqY;

        ThemeColorRow(String label, ColorSetting setting) {
            this.label = label;
            this.setting = setting;
        }
    }
}

