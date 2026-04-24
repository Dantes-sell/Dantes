package ru.cloud.client.screens.dropdown;

import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import ru.cloud.Zenith;
import ru.cloud.base.animations.base.Animation;
import ru.cloud.base.animations.base.Easing;
import ru.cloud.base.font.Fonts;
import ru.cloud.base.font.MsdfRenderer;
import ru.cloud.base.theme.GuiStyle;
import ru.cloud.base.theme.Theme;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.setting.Setting;
import ru.cloud.client.modules.api.setting.impl.*;
import ru.cloud.client.modules.api.setting.impl.*;
import ru.cloud.client.screens.menu.settings.impl.popup.MenuColorPopupSetting;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.render.display.UrlTextureLoader;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.ChangeRect;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;
import ru.cloud.utility.render.display.Keyboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryPanel {

    private final Category category;
    private float x, y;
    // ������������� X ��� �������� ����������� ��� swap
    private final Animation animX = new Animation(300, 0f, Easing.CUBIC_OUT);
    private boolean animXInitialized = false;
    private final float width;
    private final float panelHeight = 280;
    private float needToScroll = 0;
    private final Animation scroll = new Animation(200, 0f, Easing.CUBIC_OUT);
    private float maxScroll = 0;

    private final List<ModuleEntry> modules = new ArrayList<>();

    private Module bindingModule = null;
    private NumberSetting draggingSlider = null;
    private KeySetting bindingKeySetting = null;
    private StringSetting focusedStringSetting = null;
    private boolean stringAllSelected = false;

    
    public boolean isDragging = false;
    public float dragX, dragY; // ������� ������� ��� drag (����� ������)

    
    public String hoveredModuleDescription = null;

    // ������ �� ����� ��� �������� popup'��
    private DropDownScreen screen;

    private final Map<ModeSetting.Value, Animation> modeAnimations = new HashMap<>();
    private final Map<MultiBooleanSetting.Value, Animation> multiBoolAnimations = new HashMap<>();
    private final Map<Setting, Animation> visibilityAnimations = new HashMap<>();
    private final Map<BooleanSetting, Animation> boolAnimations = new HashMap<>();

    public CategoryPanel(Category category, float x, float y, float width) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.width = width;

        for (Module module : Zenith.getInstance().getModuleManager().getModules()) {
            if (module.getCategory() == category) {
                modules.add(new ModuleEntry(module));
            }
        }
    }

    public void setScreen(DropDownScreen screen) {
        this.screen = screen;
    }

    public void render(CustomDrawContext ctx, int mouseX, int mouseY, float alpha, Theme theme) {
        scroll.update(needToScroll);

        hoveredModuleDescription = null;

        if (category == Category.THEMES) {
            renderAppearancePanel(ctx, mouseX, mouseY, alpha, theme);
            return;
        }

        // используем анимированную X для рендера
        float renderX = animXInitialized ? animX.update(x) : x;

        
        GuiStyle guiStyle = Zenith.getInstance().getThemeManager().getGuiStyle();
        int blurRadius = guiStyle == GuiStyle.LIQUID_GLASS ? 32 : 24;
        ColorRGBA panelBackground = guiStyle == GuiStyle.LIQUID_GLASS
                ? new ColorRGBA(224, 231, 240, (int) (105 * alpha))
                : new ColorRGBA(18, 15, 12, (int) (205 * alpha));
        ColorRGBA panelBorder = guiStyle == GuiStyle.LIQUID_GLASS
                ? new ColorRGBA(255, 255, 255, (int) (78 * alpha))
                : new ColorRGBA(255, 255, 255, (int) (26 * alpha));

        DrawUtil.drawBlurHud(ctx.getMatrices(), renderX, y, width, panelHeight,
                blurRadius, BorderRadius.all(14), ColorRGBA.WHITE);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), renderX, y, width, panelHeight,
                BorderRadius.all(14), panelBackground);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), renderX, y, width, panelHeight,
                0.1f, BorderRadius.all(14), panelBorder);

        String categoryName = getDisplayCategoryName();
        float headerHeight = 28f;
        float textCenterY = y + (headerHeight - Fonts.SEMIBOLD.getFont(9).height()) / 2f + 1f;

        ctx.drawText(Fonts.SEMIBOLD.getFont(9), categoryName,
                renderX + 10, textCenterY,
                theme.getWhite().withAlpha((int) (255 * alpha)));

        ctx.drawText(Fonts.ICONS.getFont(8), category.getIcon(),
                renderX + width - 18, y + (headerHeight - Fonts.ICONS.getFont(8).height()) / 2f + 0.5f,
                theme.getWhite().withAlpha((int) (220 * alpha)));

        DrawUtil.drawRect(ctx.getMatrices(), renderX + 10, y + headerHeight, width - 20, 0.6f,
                new ColorRGBA(255, 255, 255, (int) (14 * alpha)));

        ctx.enableScissor((int) renderX, (int) (y + 36), (int) (renderX + width), (int) (y + panelHeight));

        float innerHeight = panelHeight - 40;
        float moduleY = y + 36 + scroll.getValue();
        float totalHeight = 0;

        for (ModuleEntry entry : modules) {
            if (!DropDownScreen.searchText.isEmpty()) {
                if (!entry.module.getName().toLowerCase().contains(DropDownScreen.searchText.toLowerCase())) {
                    continue;
                }
            }

            float moduleHeight = calculateModuleHeight(entry, entry.expandAnimation.getValue());
            renderModule(ctx, entry, mouseX, mouseY, moduleY, alpha, theme, renderX);

            entry.y = moduleY;
            entry.height = moduleHeight;
            entry.moduleX = renderX + 8;
            entry.moduleWidth = width - 16;

            float spacing = 4.5f;
            moduleY += moduleHeight + spacing;
            totalHeight += moduleHeight + spacing;
        }

        ctx.disableScissor();

        maxScroll = Math.max(0, totalHeight - innerHeight + 6);

        if (draggingSlider != null) {
            updateSlider(mouseX, draggingSlider);
        }
    }

    private float calculateModuleHeight(ModuleEntry entry, float expandAnim) {
        float settingsHeight = 0;
        if (expandAnim > 0.01f) {
            settingsHeight = calculateSettingsHeight(entry.module) * expandAnim;
        }
        return 18 + settingsHeight;
    }

    private void renderModule(CustomDrawContext ctx, ModuleEntry entry, int mouseX, int mouseY,
                              float moduleY, float alpha, Theme theme, float renderX) {
        Module module = entry.module;
        entry.expandAnimation.update(entry.expanded ? 1f : 0f);
        entry.enableAnimation.update(module.isEnabled() ? 1f : 0f);

        float expandAnim = entry.expandAnimation.getValue();
        float enableAnim = entry.enableAnimation.getValue();
        float totalHeight = calculateModuleHeight(entry, expandAnim);
        float moduleX = renderX + 8;
        float moduleWidth = width - 16;

        boolean hovered = MathUtil.isHovered(mouseX, mouseY, moduleX, moduleY, moduleWidth, 18);
        if (hovered || module.isEnabled() || entry.expanded) {
            int hoverAlpha = Zenith.getInstance().getThemeManager().getGuiStyle() == GuiStyle.LIQUID_GLASS ? 34 : 18;
            int baseAlpha = Zenith.getInstance().getThemeManager().getGuiStyle() == GuiStyle.LIQUID_GLASS ? 20 : 10;
            DrawUtil.drawRoundedRect(ctx.getMatrices(), moduleX, moduleY, moduleWidth, 18,
                    BorderRadius.all(6),
                    new ColorRGBA(255, 255, 255, (int) ((hovered ? hoverAlpha : baseAlpha) * alpha + enableAnim * 16)));
        }

        String name = module.getName();
        if (bindingModule == module) name = name + "...";
        float nameY = moduleY + (18f - 8f) / 2f + 0.5f;
        float maxNameWidth = moduleWidth - 28;
        MsdfRenderer.renderText(Fonts.MEDIUM.getFont(8).getFont(), name, 8f,
                theme.getGrayLight().mix(theme.getWhite(), Math.max(enableAnim, hovered ? 0.35f : 0f))
                        .withAlpha((int) (255 * alpha)).getRGB(),
                ctx.getMatrices().peek().getPositionMatrix(),
                moduleX + 6, nameY, 0, true, 0.75f, 1.0f, maxNameWidth);

        
        String desc = module.getInfo().description();
        float panelVisibleTop = y + 36;
        float panelVisibleBottom = y + panelHeight;
        boolean moduleVisible = moduleY >= panelVisibleTop && moduleY + 18 <= panelVisibleBottom;
        if (!desc.isEmpty() && moduleVisible && MathUtil.isHovered(mouseX, mouseY, moduleX, moduleY, moduleWidth, 18)) {
            hoveredModuleDescription = desc;
        }

        if (!module.getSettings().isEmpty()) {
            entry.arrowAnimation.update(entry.expanded ? 1f : 0f);
            float arrowAnim = entry.arrowAnimation.getValue();
            float arrowBoxSize = 12;
            float arrowBoxX = moduleX + moduleWidth - arrowBoxSize - (module.isEnabled() ? 14 : 4);
            float arrowBoxY = moduleY + 3;

            float centerX = arrowBoxX + arrowBoxSize / 2f;
            float centerY = arrowBoxY + arrowBoxSize / 2f;

            ctx.getMatrices().push();
            ctx.getMatrices().translate(centerX, centerY, 0);
            ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(45));

            float closedAlpha = 1f - arrowAnim;
            if (closedAlpha > 0.01f) {
                ctx.drawText(Fonts.MEDIUM.getFont(6), "<", -3.5f, -3f,
                        theme.getGrayLight().withAlpha((int) (220 * alpha * closedAlpha)));
                ctx.drawText(Fonts.MEDIUM.getFont(6), ">", 1f, -3f,
                        theme.getGrayLight().withAlpha((int) (220 * alpha * closedAlpha)));
            }
            if (arrowAnim > 0.01f) {
                ctx.drawText(Fonts.MEDIUM.getFont(6), ">", -3.5f, -3f,
                        theme.getWhite().withAlpha((int) (255 * alpha * arrowAnim)));
                ctx.drawText(Fonts.MEDIUM.getFont(6), "<", 1f, -3f,
                        theme.getWhite().withAlpha((int) (255 * alpha * arrowAnim)));
            }
            ctx.getMatrices().pop();
        }

        if (module.isEnabled()) {
            ctx.drawText(Fonts.MEDIUM.getFont(8), "?",
                    moduleX + moduleWidth - 10f,
                    moduleY + (18f - Fonts.MEDIUM.getFont(8).height()) / 2f,
                    theme.getWhite().withAlpha((int) (240 * alpha)));
        }

        if (expandAnim > 0.01f && !module.getSettings().isEmpty()) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), moduleX, moduleY + 20f, moduleWidth, totalHeight - 20f,
                    BorderRadius.all(8), new ColorRGBA(8, 8, 8, (int) (74 * alpha * expandAnim)));
            float settingY = moduleY + 18 - 2;
            boolean isFirst = true;
            for (Setting setting : module.getSettings()) {
                Animation visAnim = visibilityAnimations.computeIfAbsent(setting, s -> new Animation(200, 0f, Easing.CUBIC_OUT));
                visAnim.update(setting.isVisible() ? 1f : 0f);
                float visValue = visAnim.getValue();
                if (visValue < 0.01f) continue;
                settingY = renderSetting(ctx, setting, mouseX, mouseY, settingY, alpha, theme, expandAnim, moduleX, moduleWidth, module.isEnabled(), visValue, isFirst);
                isFirst = false;
            }
        }
    }

    private float renderSetting(CustomDrawContext ctx, Setting setting, int mouseX, int mouseY,
                                float settingY, float alpha, Theme theme, float expandAlpha,
                                float moduleX, float moduleWidth, boolean moduleEnabled, float visibilityAnim, boolean isFirst) {
        float settingAlpha = alpha * expandAlpha * visibilityAnim;
        float leftPad = moduleX + 5;
        float rightPad = moduleX + moduleWidth - 5;
        float settingWidth = moduleWidth - 10;

        if (setting instanceof BooleanSetting bool) {
            float topPadding = isFirst ? 3 : 0;
            float height = (12 + topPadding) * visibilityAnim;
            ctx.drawText(Fonts.MEDIUM.getFont(7), bool.getName(), leftPad, settingY + 1.75f + topPadding,
                    theme.getWhite().withAlpha((int) (255 * settingAlpha)));

            Animation boolAnim = boolAnimations.computeIfAbsent(bool, b -> new Animation(200, 0f, Easing.CUBIC_OUT));
            boolAnim.update(bool.isEnabled() ? 1f : 0f);
            float anim = boolAnim.getValue();

            float boxSize = 10;
            float boxX = rightPad - boxSize + 3;
            float boxY = settingY + topPadding - 1;

            if (anim > 0.01f) {
                float iconSize = 6.5f * anim;
                ctx.drawText(Fonts.ICONS.getFont(iconSize), "S",
                        boxX + (boxSize - iconSize) / 2f + 0.9f,
                        boxY + (boxSize - iconSize) / 2f + 0.6f,
                        new ColorRGBA(80, 255, 80, (int) (255 * settingAlpha * anim)));
            }
            if (anim < 0.99f) {
                float crossAnim = 1f - anim;
                float iconSize = 5.5f * crossAnim;
                ctx.drawText(Fonts.ICONS.getFont(iconSize), "M",
                        boxX + (boxSize - iconSize) / 2f + 0.9f,
                        boxY + (boxSize - iconSize) / 2f + 0.6f,
                        new ColorRGBA(255, 80, 80, (int) (255 * settingAlpha * crossAnim)));
            }
            return settingY + height;
        }

        if (setting instanceof NumberSetting number) {
            float height = 19 * visibilityAnim;
            int scale = Math.max(0, new BigDecimal(String.valueOf(number.getIncrement())).scale());
            BigDecimal bdDisplay = new BigDecimal(Float.toString(number.getCurrent()))
                    .setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros();
            String valueStr = bdDisplay.toPlainString();

            ctx.drawText(Fonts.MEDIUM.getFont(6.5f), number.getName(), leftPad, settingY + 1.5f,
                    theme.getWhite().withAlpha((int) (255 * settingAlpha)));
            ctx.drawText(Fonts.MEDIUM.getFont(6.5f), valueStr,
                    rightPad - Fonts.MEDIUM.getFont(7).width(valueStr), settingY + 1.5f,
                    theme.getWhite().withAlpha((int) (255 * settingAlpha)));

            float sliderY = settingY + 11 * visibilityAnim;
            float sliderHeight = 4;
            float progress = (number.getCurrent() - number.getMin()) / (number.getMax() - number.getMin());

            ColorRGBA sliderBg = moduleEnabled
                    ? theme.getColor().withAlpha((int) (25 * settingAlpha))
                    : new ColorRGBA(0, 0, 0, (int) (25 * settingAlpha));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), leftPad, sliderY, settingWidth, sliderHeight,
                    BorderRadius.all(1), sliderBg);

            if (progress > 0) {
                ColorRGBA sliderColor = theme.getColor().withAlpha((int) (255 * settingAlpha));
                DrawUtil.drawRoundedRect(ctx.getMatrices(), leftPad, sliderY, settingWidth * progress, sliderHeight,
                        BorderRadius.all(1), sliderColor, sliderColor, sliderColor, sliderColor);
            }

            float circleSize = 7;
            float circleX = leftPad + MathHelper.clamp(settingWidth * progress, 0, settingWidth) - circleSize / 2f;
            float circleY = sliderY - (circleSize - sliderHeight) / 2f;
            DrawUtil.drawRoundedRect(ctx.getMatrices(), circleX, circleY, circleSize, circleSize,
                    BorderRadius.all(circleSize / 2f), new ColorRGBA(255, 255, 255, (int) (255 * settingAlpha)));

            return settingY + height;
        }

        if (setting instanceof ModeSetting mode) {
            float rowHeight = 10;
            float boxHeight = Math.max(22, mode.getValues().size() * rowHeight + 8);
            float totalHeight = (12 + boxHeight + 5) * visibilityAnim;

            ctx.drawText(Fonts.MEDIUM.getFont(7), mode.getName(), leftPad, settingY + 3,
                    theme.getWhite().withAlpha((int) (255 * settingAlpha)));

            float boxY = settingY + 12 * visibilityAnim;
            DrawUtil.drawRoundedRect(ctx.getMatrices(), leftPad - 2, boxY, settingWidth + 4, boxHeight * visibilityAnim,
                    BorderRadius.all(4), theme.getForegroundDark().withAlpha((int) (200 * settingAlpha)));

            float optionY = boxY + 4 * visibilityAnim;
            for (ModeSetting.Value val : mode.getValues()) {
                Animation anim = modeAnimations.computeIfAbsent(val, v -> new Animation(350, 0f, Easing.CUBIC_OUT));
                anim.update(val.isSelected() ? 1f : 0f);
                float animValue = anim.getValue();

                int textBrightness = (int) (150 + 105 * animValue);
                ctx.drawText(Fonts.MEDIUM.getFont(6), val.getName(), leftPad + 2,
                        optionY + (rowHeight - 6) / 2f,
                        theme.getGray().mix(theme.getWhite(), animValue).withAlpha((int) (255 * settingAlpha)));

                if (animValue > 0.01f) {
                    float circleSize = 4.5f;
                    DrawUtil.drawRoundedRect(ctx.getMatrices(),
                            rightPad - circleSize - 1.5f, optionY + (rowHeight - 6) / 2f + 0.3f,
                            circleSize, circleSize, BorderRadius.all(circleSize / 2f),
                            theme.getColor().withAlpha((int) (255 * settingAlpha * animValue)));
                }
                optionY += rowHeight * visibilityAnim;
            }
            return settingY + totalHeight;
        }

        if (setting instanceof MultiBooleanSetting multi) {
            float rowHeight = 10;
            float boxHeight = multi.getBooleanSettings().size() * rowHeight + 8;
            float totalHeight = (14 + boxHeight + 5) * visibilityAnim;

            int enabled = (int) multi.getBooleanSettings().stream().filter(MultiBooleanSetting.Value::isEnabled).count();
            String count = enabled + "/" + multi.getBooleanSettings().size();
            float nameWidth = Fonts.MEDIUM.getFont(7).width(multi.getName());

            ctx.drawText(Fonts.MEDIUM.getFont(7), multi.getName(), leftPad, settingY + 3,
                    theme.getWhite().withAlpha((int) (255 * settingAlpha)));
            ctx.drawText(Fonts.MEDIUM.getFont(6), count, leftPad + nameWidth + 4, settingY + 4,
                    theme.getGray().withAlpha((int) (255 * settingAlpha)));

            float boxY = settingY + 14 * visibilityAnim;
            DrawUtil.drawRoundedRect(ctx.getMatrices(), leftPad - 2, boxY, settingWidth + 4, boxHeight * visibilityAnim,
                    BorderRadius.all(4), theme.getForegroundDark().withAlpha((int) (200 * settingAlpha)));

            float optionY = boxY + 4 * visibilityAnim;
            for (MultiBooleanSetting.Value val : multi.getBooleanSettings()) {
                Animation anim = multiBoolAnimations.computeIfAbsent(val, v -> new Animation(350, 0f, Easing.CUBIC_OUT));
                anim.update(val.isEnabled() ? 1f : 0f);
                float animValue = anim.getValue();

                ctx.drawText(Fonts.MEDIUM.getFont(6), val.getName(), leftPad + 2,
                        optionY + (rowHeight - 6) / 2f,
                        theme.getGray().mix(theme.getWhite(), animValue).withAlpha((int) (255 * settingAlpha)));

                if (animValue > 0.01f) {
                    float circleSize = 4.5f;
                    DrawUtil.drawRoundedRect(ctx.getMatrices(),
                            rightPad - circleSize - 1.5f, optionY + (rowHeight - 6) / 2f + 0.3f,
                            circleSize, circleSize, BorderRadius.all(circleSize / 2f),
                            theme.getColor().withAlpha((int) (255 * settingAlpha * animValue)));
                }
                optionY += rowHeight * visibilityAnim;
            }
            return settingY + totalHeight;
        }

        if (setting instanceof KeySetting key) {
            float topPadding = isFirst ? 3 : 0;
            float height = (11 + topPadding) * visibilityAnim;
            ctx.drawText(Fonts.MEDIUM.getFont(6), key.getName(), leftPad, settingY + 1 + topPadding,
                    theme.getWhite().withAlpha((int) (255 * settingAlpha)));
            String keyName = bindingKeySetting == key ? "..." : Keyboard.getKeyName(key.getKeyCode());
            ctx.drawText(Fonts.MEDIUM.getFont(6), keyName,
                    rightPad - Fonts.MEDIUM.getFont(6).width(keyName), settingY + 1 + topPadding,
                    theme.getWhite().withAlpha((int) (255 * settingAlpha)));
            return settingY + height;
        }

        if (setting instanceof ColorSetting color) {
            float height = 11 * visibilityAnim;
            ctx.drawText(Fonts.MEDIUM.getFont(6), color.getName(), leftPad, settingY + 1,
                    theme.getWhite().withAlpha((int) (255 * settingAlpha)));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), rightPad - 14, settingY, 12, 8,
                    BorderRadius.all(2), color.getColor().withAlpha((int) (255 * settingAlpha)));
            return settingY + height;
        }

        if (setting instanceof StringSetting str) {
            float topPadding = isFirst ? 3 : 0;
            String urlVal = str.getValue();
            boolean isUrl = urlVal.startsWith("http://") || urlVal.startsWith("https://");
            float previewSize = isUrl ? 32f : 0f;
            float previewGap  = isUrl ? 4f  : 0f;
            float totalHeight = (22 + topPadding + previewSize + previewGap) * visibilityAnim;
            float labelY = settingY + topPadding;

            ctx.drawText(Fonts.MEDIUM.getFont(6.5f), str.getName(), leftPad, labelY + 1,
                    theme.getWhite().withAlpha((int) (255 * settingAlpha)));

            float fieldY = labelY + 11;
            float fieldHeight = 11;
            boolean isFocused = focusedStringSetting == str;
            boolean isAllSelected = isFocused && stringAllSelected;

            ColorRGBA fieldBg = theme.getForegroundDark().withAlpha((int) (200 * settingAlpha));
            ColorRGBA borderCol = isFocused
                    ? theme.getColor().withAlpha((int) (255 * settingAlpha))
                    : theme.getForegroundLight().withAlpha((int) (120 * settingAlpha));

            DrawUtil.drawRoundedRect(ctx.getMatrices(), leftPad - 2, fieldY, settingWidth + 4, fieldHeight,
                    BorderRadius.all(3), fieldBg);
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), leftPad - 2, fieldY, settingWidth + 4, fieldHeight,
                    0.5f, BorderRadius.all(3), borderCol);

            float textPad = 4f;
            float maxTW   = settingWidth - textPad * 2;
            float inputY  = fieldY + (fieldHeight - Fonts.MEDIUM.getFont(6).height()) / 2f;

            ctx.enableScissor((int)(leftPad - 1), (int) fieldY, (int)(leftPad + settingWidth + 3), (int)(fieldY + fieldHeight));

            if (isAllSelected && !str.getValue().isEmpty()) {
                float selW = Math.min(Fonts.MEDIUM.getFont(6).width(str.getValue()), maxTW);
                DrawUtil.drawRoundedRect(ctx.getMatrices(), leftPad - 2 + textPad, inputY - 0.5f, selW,
                        Fonts.MEDIUM.getFont(6).height() + 1f, BorderRadius.all(1),
                        theme.getColor().withAlpha((int) (80 * settingAlpha)));
                ctx.drawText(Fonts.MEDIUM.getFont(6), str.getValue(), leftPad - 2 + textPad, inputY,
                        theme.getWhite().withAlpha((int) (255 * settingAlpha)));
            } else {
                long now = System.currentTimeMillis();
                boolean cursorVisible = (now / 500) % 2 == 0;
                String displayText = isFocused && cursorVisible ? str.getValue() + "|" : str.getValue();
                float tw   = Fonts.MEDIUM.getFont(6).width(displayText);
                float drawX = leftPad - 2 + textPad;
                if (tw > maxTW) drawX -= (tw - maxTW);
                ctx.drawText(Fonts.MEDIUM.getFont(6), displayText, drawX, inputY,
                        isFocused ? theme.getWhite().withAlpha((int) (255 * settingAlpha))
                                  : theme.getGray().withAlpha((int) (200 * settingAlpha)));
            }

            ctx.disableScissor();

            // --- URL preview ---
            if (isUrl) {
                UrlTextureLoader.Entry entry = UrlTextureLoader.get(urlVal);
                float previewY = fieldY + fieldHeight + previewGap;
                // center preview in the setting width
                float previewX = leftPad + (settingWidth - previewSize) / 2f;

                DrawUtil.drawRoundedRect(ctx.getMatrices(), previewX, previewY, previewSize, previewSize,
                        BorderRadius.all(4), theme.getForegroundDark().withAlpha((int)(200 * settingAlpha)));

                if (entry != null && entry.state() == UrlTextureLoader.State.READY) {
                    DrawUtil.drawRoundedTexture(ctx.getMatrices(), entry.id(),
                            previewX, previewY, previewSize, previewSize,
                            BorderRadius.all(4), ColorRGBA.WHITE.withAlpha((int)(255 * settingAlpha)));
                } else {
                    String indicator = (entry == null || entry.state() == UrlTextureLoader.State.LOADING)
                            ? "..." : "err";
                    ColorRGBA indColor = entry != null && entry.state() == UrlTextureLoader.State.FAILED
                            ? new ColorRGBA(255, 80, 80, (int)(200 * settingAlpha))
                            : theme.getGray().withAlpha((int)(200 * settingAlpha));
                    float iw = Fonts.MEDIUM.getFont(6).width(indicator);
                    float ih = Fonts.MEDIUM.getFont(6).height();
                    ctx.drawText(Fonts.MEDIUM.getFont(6), indicator,
                            previewX + (previewSize - iw) / 2f,
                            previewY + (previewSize - ih) / 2f, indColor);
                }

                DrawUtil.drawRoundedBorder(ctx.getMatrices(), previewX, previewY, previewSize, previewSize,
                        0.5f, BorderRadius.all(4), borderCol);
            }

            return settingY + totalHeight;
        }

        return settingY + 11 * visibilityAnim;
    }

    private float calculateSettingsHeight(Module module) {
        float h = 0;
        boolean isFirst = true;
        for (Setting setting : module.getSettings()) {
            Animation visAnim = visibilityAnimations.get(setting);
            float visValue = visAnim != null ? visAnim.getValue() : (setting.isVisible() ? 1f : 0f);
            if (visValue < 0.01f) continue;

            if (setting instanceof BooleanSetting) {
                float topPadding = isFirst ? 3 : 0;
                h += (12 + topPadding) * visValue;
            } else if (setting instanceof NumberSetting) {
                h += 19 * visValue;
            } else if (setting instanceof ModeSetting mode) {
                float boxHeight = Math.max(22, mode.getValues().size() * 10 + 8);
                h += (12 + boxHeight + 5) * visValue;
            } else if (setting instanceof MultiBooleanSetting multi) {
                float boxHeight = multi.getBooleanSettings().size() * 10 + 8;
                h += (14 + boxHeight + 5) * visValue;
            } else if (setting instanceof KeySetting) {
                float topPadding = isFirst ? 3 : 0;
                h += (11 + topPadding) * visValue;
            } else if (setting instanceof StringSetting str) {
                float topPadding = isFirst ? 3 : 0;
                boolean isUrl = str.getValue().startsWith("http://") || str.getValue().startsWith("https://");
                float preview = isUrl ? 4f + 32f : 0f;
                h += (22 + topPadding + preview) * visValue;
            } else {
                h += 11 * visValue;
            }
            isFirst = false;
        }
        return h + 2;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // drag �� ����� �������������� � DropDownScreen
        if (!MathUtil.isHovered(mouseX, mouseY, x, y, width, panelHeight)) return false;
        if (category == Category.THEMES) {
            return handleAppearanceClick(mouseX, mouseY, button);
        }

        for (ModuleEntry entry : modules) {
            if (!DropDownScreen.searchText.isEmpty()) {
                if (!entry.module.getName().toLowerCase().contains(DropDownScreen.searchText.toLowerCase())) continue;
            }

            if (MathUtil.isHovered(mouseX, mouseY, entry.moduleX, entry.y, entry.moduleWidth, 18)) {
                if (button == 0) { entry.module.toggle(); return true; }
                if (button == 1 && !entry.module.getSettings().isEmpty()) {
                    entry.expanded = !entry.expanded; return true;
                }
                if (button == 2) { bindingModule = entry.module; return true; }
            }

            if (entry.expanded) {
                // settingY ���������� ��� ��, ��� renderModule: moduleY + 18 - 2
                float settingY = entry.y + 16;
                float leftPad = entry.moduleX + 5;
                float rightPad = entry.moduleX + entry.moduleWidth - 5;
                float settingWidth = entry.moduleWidth - 10;

                boolean isFirst = true;
                for (Setting setting : entry.module.getSettings()) {
                    if (!setting.isVisible()) continue;

                    if (setting instanceof BooleanSetting bool) {
                        
                        float topPadding = isFirst ? 3 : 0;
                        float boxY = settingY + topPadding - 1;
                        if (MathUtil.isHovered(mouseX, mouseY, leftPad, boxY, settingWidth, 13)) {
                            bool.setEnabled(!bool.isEnabled()); return true;
                        }
                        settingY += 12 + topPadding;
                    } else if (setting instanceof NumberSetting number) {
                        float sliderY = settingY + 11;
                        if (MathUtil.isHovered(mouseX, mouseY, leftPad - 3, settingY - 1, settingWidth + 6, 21)) {
                            draggingSlider = number;
                            updateSliderWithBounds(mouseX, number, leftPad, settingWidth);
                            return true;
                        }
                        settingY += 19;
                    } else if (setting instanceof ModeSetting mode) {
                        float boxY = settingY + 12;
                        float rowHeight = 10;
                        float boxHeight = Math.max(22, mode.getValues().size() * rowHeight + 8);
                        float optionY = boxY + 4;
                        for (ModeSetting.Value val : mode.getValues()) {
                            if (MathUtil.isHovered(mouseX, mouseY, leftPad - 2, optionY, settingWidth + 4, rowHeight)) {
                                val.select(); return true;
                            }
                            optionY += rowHeight;
                        }
                        settingY += 12 + boxHeight + 5;
                    } else if (setting instanceof MultiBooleanSetting multi) {
                        float boxY = settingY + 14;
                        float rowHeight = 10;
                        float boxHeight = multi.getBooleanSettings().size() * rowHeight + 8;
                        float optionY = boxY + 4;
                        for (MultiBooleanSetting.Value val : multi.getBooleanSettings()) {
                            if (MathUtil.isHovered(mouseX, mouseY, leftPad - 2, optionY, settingWidth + 4, rowHeight)) {
                                if (button == 0) val.toggle();
                                return true;
                            }
                            optionY += rowHeight;
                        }
                        settingY += 14 + boxHeight + 5;
                    } else if (setting instanceof KeySetting key) {
                        float topPadding = isFirst ? 3 : 0;
                        if (MathUtil.isHovered(mouseX, mouseY, leftPad, settingY + topPadding, settingWidth, 11)) {
                            bindingKeySetting = key; return true;
                        }
                        settingY += 11 + topPadding;
                    } else if (setting instanceof ColorSetting color) {
                        // ���� �� �������� �������� ������
                        float sqX = rightPad - 14;
                        if (MathUtil.isHovered(mouseX, mouseY, sqX - 2, settingY - 1, 16, 11) && screen != null) {
                            ChangeRect popupBounds = new ChangeRect(sqX + 14, settingY, 96, 160);
                            // �� ������� �� ������ ���� ������
                            if (popupBounds.getX() + popupBounds.getWidth() > screen.width - 4) {
                                popupBounds.setX(sqX - popupBounds.getWidth() - 2);
                            }
                            screen.addPopupSetting(new MenuColorPopupSetting(popupBounds, color));
                            return true;
                        }
                        settingY += 11;
                    } else if (setting instanceof StringSetting str) {
                        float topPadding = isFirst ? 3 : 0;
                        float fieldY = settingY + topPadding + 11;
                        if (MathUtil.isHovered(mouseX, mouseY, leftPad - 2, fieldY, settingWidth + 4, 11)) {
                            focusedStringSetting = (focusedStringSetting == str) ? null : str;
                            stringAllSelected = false;
                            return true;
                        }
                        
                        if (focusedStringSetting == str) { focusedStringSetting = null; stringAllSelected = false; }
                        boolean isUrl = str.getValue().startsWith("http://") || str.getValue().startsWith("https://");
                        float preview = isUrl ? 4f + 32f : 0f;
                        settingY += 22 + topPadding + preview;
                    } else {
                        settingY += 11;
                    }
                    isFirst = false;
                }
            }
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingSlider = null;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        
        animX.setValue(x);
        animXInitialized = true;
    }

    /** ������ ��������� ������ �� ����� ���� (���������� ��� swap, �� ��� drag). */
    public void animateToSlot(float targetX, float y) {
        this.x = targetX;
        this.y = y;
        if (!animXInitialized) {
            animX.setValue(targetX);
            animXInitialized = true;
        }
        animX.animateTo(targetX);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getPanelHeight() { return panelHeight; }
    public Category getCategory() { return category; }

    private String getDisplayCategoryName() {
        return switch (category) {
            case RENDER -> "Visuals";
            case MISC -> "Other";
            case THEMES -> "Appearance";
            default -> category.getName();
        };
    }

    private void renderAppearancePanel(CustomDrawContext ctx, int mouseX, int mouseY, float alpha, Theme theme) {
        float renderX = animXInitialized ? animX.update(x) : x;
        GuiStyle guiStyle = Zenith.getInstance().getThemeManager().getGuiStyle();

        ColorRGBA panelBackground = guiStyle == GuiStyle.LIQUID_GLASS
                ? new ColorRGBA(224, 231, 240, (int) (110 * alpha))
                : new ColorRGBA(18, 15, 12, (int) (205 * alpha));
        ColorRGBA panelBorder = guiStyle == GuiStyle.LIQUID_GLASS
                ? new ColorRGBA(255, 255, 255, (int) (80 * alpha))
                : new ColorRGBA(255, 255, 255, (int) (26 * alpha));

        DrawUtil.drawBlurHud(ctx.getMatrices(), renderX, y, width, panelHeight,
                guiStyle == GuiStyle.LIQUID_GLASS ? 32 : 24, BorderRadius.all(14), ColorRGBA.WHITE);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), renderX, y, width, panelHeight, BorderRadius.all(14), panelBackground);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), renderX, y, width, panelHeight, 0.1f, BorderRadius.all(14), panelBorder);

        float headerHeight = 28f;
        ctx.drawText(Fonts.SEMIBOLD.getFont(9), getDisplayCategoryName(),
                renderX + 10, y + (headerHeight - Fonts.SEMIBOLD.getFont(9).height()) / 2f + 1f,
                theme.getWhite().withAlpha((int) (255 * alpha)));
        ctx.drawText(Fonts.ICONS.getFont(8), category.getIcon(),
                renderX + width - 18, y + (headerHeight - Fonts.ICONS.getFont(8).height()) / 2f + 0.5f,
                theme.getWhite().withAlpha((int) (220 * alpha)));
        DrawUtil.drawRect(ctx.getMatrices(), renderX + 10, y + headerHeight, width - 20, 0.6f,
                new ColorRGBA(255, 255, 255, (int) (14 * alpha)));

        float contentX = renderX + 8f;
        float cardWidth = width - 16f;
        float sectionY = y + 40f;

        ctx.drawText(Fonts.SEMIBOLD.getFont(7), "Theme", contentX, sectionY,
                theme.getGrayLight().mix(theme.getWhite(), 0.4f).withAlpha((int) (230 * alpha)));
        sectionY += 12f;

        Theme[] themes = {Theme.DARK, Theme.LIGHT, Theme.CUSTOM_THEME};
        for (Theme option : themes) {
            boolean selected = Zenith.getInstance().getThemeManager().is(option);
            boolean hovered = MathUtil.isHovered(mouseX, mouseY, contentX, sectionY, cardWidth, 24);
            ColorRGBA cardBg = selected
                    ? option.getColor().withAlpha((int) (58 * alpha))
                    : new ColorRGBA(255, 255, 255, (int) ((hovered ? 24 : 12) * alpha));

            DrawUtil.drawRoundedRect(ctx.getMatrices(), contentX, sectionY, cardWidth, 24, BorderRadius.all(7), cardBg);
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), contentX, sectionY, cardWidth, 24, 0.1f, BorderRadius.all(7),
                    (selected ? option.getColor() : theme.getWhite()).withAlpha((int) ((selected ? 110 : 20) * alpha)));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), contentX + 6, sectionY + 7, 10, 10, BorderRadius.all(3),
                    option.getColor(), option.getSecondColor(), option.getSecondColor(), option.getColor());
            ctx.drawText(Fonts.MEDIUM.getFont(7), option.getName(), contentX + 22, sectionY + 8,
                    theme.getWhite().withAlpha((int) (245 * alpha)));
            if (selected) {
                ctx.drawText(Fonts.MEDIUM.getFont(7), "ON", contentX + cardWidth - 16, sectionY + 8,
                        theme.getWhite().withAlpha((int) (245 * alpha)));
            }
            sectionY += 28f;
        }

        sectionY += 4f;
        ctx.drawText(Fonts.SEMIBOLD.getFont(7), "Interface Style", contentX, sectionY,
                theme.getGrayLight().mix(theme.getWhite(), 0.4f).withAlpha((int) (230 * alpha)));
        sectionY += 12f;

        renderStyleCard(ctx, mouseX, mouseY, theme, alpha, contentX, sectionY, cardWidth, 62f,
                GuiStyle.MINIMALISM, "Minimalism", "Dense black surface");
        sectionY += 68f;
        renderStyleCard(ctx, mouseX, mouseY, theme, alpha, contentX, sectionY, cardWidth, 62f,
                GuiStyle.LIQUID_GLASS, "Liquid Glass", "Soft iPhone-like glass");
    }

    private void renderStyleCard(CustomDrawContext ctx, int mouseX, int mouseY, Theme theme, float alpha,
                                 float x, float y, float width, float height,
                                 GuiStyle style, String title, String description) {
        boolean selected = Zenith.getInstance().getThemeManager().getGuiStyle() == style;
        boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, height);

        ColorRGBA background = style == GuiStyle.LIQUID_GLASS
                ? new ColorRGBA(228, 236, 245, (int) ((selected ? 132 : hovered ? 108 : 92) * alpha))
                : new ColorRGBA(10, 10, 12, (int) ((selected ? 228 : hovered ? 210 : 196) * alpha));
        ColorRGBA border = style == GuiStyle.LIQUID_GLASS
                ? new ColorRGBA(255, 255, 255, (int) ((selected ? 120 : 70) * alpha))
                : new ColorRGBA(255, 255, 255, (int) ((selected ? 54 : 20) * alpha));

        DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, height, style == GuiStyle.LIQUID_GLASS ? 24 : 12, BorderRadius.all(9), ColorRGBA.WHITE);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(9), background);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, 0.1f, BorderRadius.all(9), border);

        if (style == GuiStyle.LIQUID_GLASS) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 8, y + 10, width - 16, 18, BorderRadius.all(8),
                    new ColorRGBA(255, 255, 255, (int) (78 * alpha)));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 12, y + 34, width - 24, 12, BorderRadius.all(6),
                    new ColorRGBA(255, 255, 255, (int) (52 * alpha)));
        } else {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 8, y + 10, width - 16, 18, BorderRadius.all(8),
                    new ColorRGBA(21, 21, 24, (int) (255 * alpha)));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 12, y + 34, width - 24, 12, BorderRadius.all(6),
                    new ColorRGBA(32, 32, 36, (int) (230 * alpha)));
        }

        ctx.drawText(Fonts.SEMIBOLD.getFont(7), title, x + 10, y + height - 18,
                style == GuiStyle.LIQUID_GLASS
                        ? new ColorRGBA(42, 51, 64, (int) (255 * alpha))
                        : theme.getWhite().withAlpha((int) (255 * alpha)));
        ctx.drawText(Fonts.MEDIUM.getFont(6), description, x + 10, y + height - 9,
                style == GuiStyle.LIQUID_GLASS
                        ? new ColorRGBA(78, 88, 100, (int) (220 * alpha))
                        : theme.getGrayLight().withAlpha((int) (240 * alpha)));
        if (selected) {
            ctx.drawText(Fonts.MEDIUM.getFont(7), "Selected", x + width - 29, y + 8,
                    style == GuiStyle.LIQUID_GLASS
                            ? new ColorRGBA(34, 44, 58, (int) (230 * alpha))
                            : theme.getWhite().withAlpha((int) (235 * alpha)));
        }
    }

    private boolean handleAppearanceClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }

        float contentX = x + 8f;
        float cardWidth = width - 16f;
        float sectionY = y + 52f;

        Theme[] themes = {Theme.DARK, Theme.LIGHT, Theme.CUSTOM_THEME};
        for (Theme option : themes) {
            if (MathUtil.isHovered(mouseX, mouseY, contentX, sectionY, cardWidth, 24)) {
                Zenith.getInstance().getThemeManager().switchTheme(option);
                return true;
            }
            sectionY += 28f;
        }

        sectionY += 16f;
        if (MathUtil.isHovered(mouseX, mouseY, contentX, sectionY, cardWidth, 62f)) {
            Zenith.getInstance().getThemeManager().setGuiStyle(GuiStyle.MINIMALISM);
            return true;
        }
        sectionY += 68f;
        if (MathUtil.isHovered(mouseX, mouseY, contentX, sectionY, cardWidth, 62f)) {
            Zenith.getInstance().getThemeManager().setGuiStyle(GuiStyle.LIQUID_GLASS);
            return true;
        }
        return true;
    }

    public void resetScroll() {
        needToScroll = 0;
        scroll.setValue(0);
    }

    public boolean isHeaderHovered(double mouseX, double mouseY) {
        return MathUtil.isHovered(mouseX, mouseY, x, y, width, 28);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, panelHeight)) {
            needToScroll += (float) (delta * 15);
            needToScroll = MathHelper.clamp(needToScroll, Math.min(-maxScroll, 0), 0);
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingModule != null) {
            bindingModule.setKeyCode(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode);
            bindingModule = null;
            return true;
        }
        if (bindingKeySetting != null) {
            bindingKeySetting.setKeyCode(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode);
            bindingKeySetting = null;
            return true;
        }
        if (focusedStringSetting != null) {
            boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                focusedStringSetting = null;
                stringAllSelected = false;
                return true;
            }
            if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
                stringAllSelected = true;
                return true;
            }
            if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
                net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(focusedStringSetting.getValue());
                return true;
            }
            if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
                String clip = net.minecraft.client.MinecraftClient.getInstance().keyboard.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    String base = stringAllSelected ? "" : focusedStringSetting.getValue();
                    String combined = base + clip;
                    if (combined.length() > focusedStringSetting.getMaxLength())
                        combined = combined.substring(0, focusedStringSetting.getMaxLength());
                    focusedStringSetting.setValue(combined);
                    stringAllSelected = false;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (stringAllSelected || ctrl) {
                    focusedStringSetting.setValue("");
                } else {
                    String val = focusedStringSetting.getValue();
                    if (!val.isEmpty()) focusedStringSetting.setValue(val.substring(0, val.length() - 1));
                }
                stringAllSelected = false;
                return true;
            }
            stringAllSelected = false;
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (focusedStringSetting != null) {
            if (stringAllSelected) {
                focusedStringSetting.setValue(String.valueOf(chr));
                stringAllSelected = false;
                return true;
            }
            String val = focusedStringSetting.getValue();
            if (val.length() < focusedStringSetting.getMaxLength()) {
                focusedStringSetting.setValue(val + chr);
            }
            return true;
        }
        return false;
    }

    private void updateSlider(double mouseX, NumberSetting number) {
        for (ModuleEntry entry : modules) {
            if (entry.expanded && entry.module.getSettings().contains(number)) {
                float leftPad = entry.moduleX + 5;
                float sliderWidth = entry.moduleWidth - 10;
                updateSliderWithBounds(mouseX, number, leftPad, sliderWidth);
                return;
            }
        }
    }

    private void updateSliderWithBounds(double mouseX, NumberSetting number, float leftPad, float sliderWidth) {
        float progress = (float) ((mouseX - leftPad) / sliderWidth);
        progress = MathHelper.clamp(progress, 0, 1);
        float value = number.getMin() + progress * (number.getMax() - number.getMin());
        BigDecimal bd = BigDecimal.valueOf(value);
        BigDecimal inc = BigDecimal.valueOf(number.getIncrement());
        BigDecimal divided = bd.divide(inc, 0, RoundingMode.HALF_UP);
        number.setCurrent(divided.multiply(inc).floatValue());
    }

    private static class ModuleEntry {
        final Module module;
        boolean expanded = false;
        final Animation expandAnimation = new Animation(350, 0f, Easing.CUBIC_OUT);
        final Animation arrowAnimation = new Animation(350, 0f, Easing.CUBIC_OUT);
        final Animation enableAnimation = new Animation(350, 0f, Easing.CUBIC_OUT);
        float y;
        float height;
        float moduleX;
        float moduleWidth;

        ModuleEntry(Module module) {
            this.module = module;
        }
    }
}

