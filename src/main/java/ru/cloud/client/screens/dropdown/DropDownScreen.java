package ru.cloud.client.screens.dropdown;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.cloud.Zenith;
import ru.cloud.base.animations.base.Animation;
import ru.cloud.base.animations.base.Easing;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.base.theme.GuiStyle;
import ru.cloud.base.theme.Theme;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.impl.render.Menu;
import ru.cloud.client.screens.menu.settings.api.MenuPopupSetting;
import ru.cloud.utility.game.other.MouseButton;
import ru.cloud.utility.interfaces.IMinecraft;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.base.UIContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DropDownScreen extends Screen implements IMinecraft {

    private static final File ORDER_FILE = new File(new File(Zenith.DIRECTORY, "configs"), "dropdown_order.txt");

    private final List<CategoryPanel> panels = new ArrayList<>();
    private final Animation openAnimation = new Animation(250, 0f, Easing.CUBIC_OUT);
    private final Animation searchExpandAnim = new Animation(180, 0f, Easing.CUBIC_OUT);
    private final Set<MenuPopupSetting> popupSettings = new HashSet<>();

    public boolean needToClose = false;
    public static boolean searching = false;
    public static String searchText = "";

    private CategoryPanel draggedPanel;
    private float dragOffsetX;
    private float dragOffsetY;
    private float dragCurrentX;
    private float dragCurrentY;
    private float[] slotX;
    private float slotY;
    private float panelWidth = 132f;
    private float panelHeight = 286f;
    private float panelSpacing = 8f;

    private float guiScale = 1.0f;
    private static final float SCALE_MIN = 0.7f;
    private static final float SCALE_MAX = 1.6f;
    private static final float SCALE_STEP = 0.1f;

    public DropDownScreen() {
        super(Text.of("Dropdown"));
    }

    public void addPopupSetting(MenuPopupSetting popup) {
        popupSettings.add(popup);
    }

    @Override
    protected void init() {
        super.init();
        needToClose = false;
        popupSettings.clear();
        guiScale = 1.0f;
        openAnimation.reset();
        initPanels();
    }

    public void reinitPanels() {
        initPanels();
    }

    private void initPanels() {
        panels.clear();

        List<Category> savedOrder = loadOrder();
        List<Category> allCategories = new ArrayList<>();
        for (Category category : Category.values()) {
            allCategories.add(category);
        }

        List<Category> ordered = new ArrayList<>();
        for (Category category : savedOrder) {
            if (allCategories.contains(category)) {
                ordered.add(category);
            }
        }
        for (Category category : allCategories) {
            if (!ordered.contains(category)) {
                ordered.add(category);
            }
        }

        int count = ordered.size();
        float totalWidth = count * panelWidth + (count - 1) * panelSpacing;
        float startX = (width - totalWidth) / 2f;
        slotY = (height - panelHeight) / 2f;
        slotX = new float[count];

        for (int i = 0; i < count; i++) {
            slotX[i] = startX + i * (panelWidth + panelSpacing);
            CategoryPanel panel = new CategoryPanel(ordered.get(i), slotX[i], slotY, panelWidth);
            panel.setPosition(slotX[i], slotY);
            panel.setScreen(this);
            panels.add(panel);
        }
    }

    private void saveOrder() {
        try {
            ORDER_FILE.getParentFile().mkdirs();
            try (PrintWriter writer = new PrintWriter(new FileWriter(ORDER_FILE))) {
                for (CategoryPanel panel : panels) {
                    writer.println(panel.getCategory().name());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private List<Category> loadOrder() {
        List<Category> order = new ArrayList<>();
        if (!ORDER_FILE.exists()) {
            return order;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(ORDER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    order.add(Category.valueOf(line.trim()));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return order;
    }

    private double toScaledX(double screenX) {
        return (screenX - width / 2.0) / guiScale + width / 2.0;
    }

    private double toScaledY(double screenY) {
        return (screenY - height / 2.0) / guiScale + height / 2.0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnimation.update(!needToClose ? 1f : 0f);
        if (openAnimation.getValue() <= 0.01f && needToClose) {
            mc.setScreen(null);
            return;
        }

        CustomDrawContext ctx = CustomDrawContext.of(context);
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        GuiStyle guiStyle = Zenith.getInstance().getThemeManager().getGuiStyle();
        float alpha = openAnimation.getValue();

        ColorRGBA overlay = guiStyle == GuiStyle.LIQUID_GLASS
                ? new ColorRGBA(214, 224, 238, (int) (36 * alpha))
                : new ColorRGBA(7, 7, 7, (int) (95 * alpha));
        DrawUtil.drawRect(ctx.getMatrices(), 0, 0, width, height, overlay);

        int scaledMouseX = (int) toScaledX(mouseX);
        int scaledMouseY = (int) toScaledY(mouseY);

        float centerX = width / 2f;
        float centerY = height / 2f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX, centerY, 0);
        ctx.getMatrices().scale(guiScale, guiScale, 1);
        ctx.getMatrices().translate(-centerX, -centerY, 0);

        if (draggedPanel != null) {
            dragCurrentX = scaledMouseX - dragOffsetX;
            dragCurrentY = scaledMouseY - dragOffsetY;
            draggedPanel.setPosition(dragCurrentX, dragCurrentY);

            int targetSlot = findTargetSlot(dragCurrentX);
            int draggedSlot = panels.indexOf(draggedPanel);
            if (targetSlot != -1 && targetSlot != draggedSlot) {
                DrawUtil.drawRoundedBorder(ctx.getMatrices(),
                        slotX[targetSlot], slotY, panelWidth, draggedPanel.getPanelHeight(),
                        0.8f, BorderRadius.all(14),
                        (guiStyle == GuiStyle.LIQUID_GLASS ? theme.getWhite() : theme.getColor()).withAlpha((int) (130 * alpha)));
            }
        }

        for (CategoryPanel panel : panels) {
            if (panel != draggedPanel) {
                panel.render(ctx, scaledMouseX, scaledMouseY, alpha, theme);
            }
        }
        if (draggedPanel != null) {
            draggedPanel.render(ctx, scaledMouseX, scaledMouseY, alpha * 0.92f, theme);
        }

        ctx.getMatrices().pop();

        for (CategoryPanel panel : panels) {
            String desc = panel.hoveredModuleDescription;
            if (desc != null && !desc.isEmpty()) {
                renderTooltip(ctx, desc, mouseX, mouseY, alpha, theme);
                break;
            }
        }

        searchExpandAnim.update(searching || !searchText.isEmpty() ? 1f : 0f);
        renderSearch(ctx, mouseX, mouseY, alpha, theme);

        UIContext uiContext = UIContext.of(context, mouseX, mouseY, delta);
        List<MenuPopupSetting> toRemove = new ArrayList<>();
        for (MenuPopupSetting popup : popupSettings) {
            popup.render(uiContext, mouseX, mouseY, alpha, theme);
            if (popup.getAnimationScale().getValue() <= 0.01f) {
                toRemove.add(popup);
            }
        }
        popupSettings.removeAll(toRemove);
    }

    private void renderSearch(CustomDrawContext ctx, int mouseX, int mouseY, float alpha, Theme theme) {
        float progress = searchExpandAnim.getValue();
        if (progress <= 0.01f) {
            return;
        }

        Font font = Fonts.MEDIUM.getFont(7f);
        Font iconFont = Fonts.ICONS.getFont(7f);
        float width = 138f;
        float height = 18f;
        float x = this.width / 2f - width / 2f;
        float y = this.height - 30f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + width / 2f, y + height / 2f, 0);
        ctx.getMatrices().scale(progress, progress, 1);
        ctx.getMatrices().translate(-(x + width / 2f), -(y + height / 2f), 0);

        GuiStyle guiStyle = Zenith.getInstance().getThemeManager().getGuiStyle();
        int blurRadius = guiStyle == GuiStyle.LIQUID_GLASS ? 28 : 16;
        ColorRGBA searchBackground = guiStyle == GuiStyle.LIQUID_GLASS
                ? new ColorRGBA(225, 232, 242, (int) (118 * alpha))
                : new ColorRGBA(18, 18, 18, (int) (220 * alpha));
        ColorRGBA searchBorder = guiStyle == GuiStyle.LIQUID_GLASS
                ? new ColorRGBA(255, 255, 255, (int) (72 * alpha))
                : new ColorRGBA(255, 255, 255, (int) (28 * alpha));

        DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, height, blurRadius, BorderRadius.all(8), ColorRGBA.WHITE);
        ctx.drawRoundedRect(x, y, width, height, BorderRadius.all(8), searchBackground);
        ctx.drawRoundedBorder(x, y, width, height, 0.1f, BorderRadius.all(8), searchBorder);

        String displayText = searchText + (searching && System.currentTimeMillis() % 1000 > 500 ? "|" : "");
        if (displayText.isEmpty()) {
            displayText = "Search modules";
        }
        ColorRGBA textColor = searchText.isEmpty() && !searching
                ? theme.getGrayLight().withAlpha((int) (190 * alpha))
                : theme.getWhite().withAlpha((int) (255 * alpha));

        ctx.drawText(iconFont, "V", x + 7f, y + (height - iconFont.height()) / 2f, theme.getWhite());
        ctx.enableScissor((int) (x + 18), (int) y, (int) (x + width - 6), (int) (y + height));
        ctx.drawText(font, displayText, x + 18f, y + (height - font.height()) / 2f, textColor);
        ctx.disableScissor();
        ctx.getMatrices().pop();
    }

    private int findTargetSlot(float panelLeft) {
        if (slotX == null) {
            return -1;
        }

        float centerX = panelLeft + panelWidth / 2f;
        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < slotX.length; i++) {
            float slotCenter = slotX[i] + panelWidth / 2f;
            float distance = Math.abs(centerX - slotCenter);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private void renderTooltip(CustomDrawContext ctx, String text, int mouseX, int mouseY, float alpha, Theme theme) {
        float padding = 6f;
        Font font = Fonts.MEDIUM.getFont(6.5f);
        float maxWidth = Math.min(220f, width * 0.35f);
        List<String> lines = wrapText(text, font, maxWidth - padding * 2f);

        float contentWidth = 0f;
        for (String line : lines) {
            contentWidth = Math.max(contentWidth, font.width(line));
        }

        float tooltipWidth = contentWidth + padding * 2f + 2f;
        float tooltipHeight = lines.size() * (font.height() + 2f) + padding * 2f;
        float x = mouseX + 10f;
        float y = mouseY - tooltipHeight - 4f;

        if (x + tooltipWidth > width - 4f) x = mouseX - tooltipWidth - 4f;
        if (x < 4f) x = 4f;
        if (y < 4f) y = mouseY + 12f;

        ctx.drawRoundedRect(x, y, tooltipWidth, tooltipHeight, BorderRadius.all(6),
                new ColorRGBA(18, 18, 18, (int) (225 * alpha)));
        ctx.drawRoundedBorder(x, y, tooltipWidth, tooltipHeight, 0.1f, BorderRadius.all(6),
                theme.getColor().withAlpha((int) (90 * alpha)));

        float lineY = y + padding;
        for (String line : lines) {
            ctx.drawText(font, line, x + padding, lineY, theme.getWhite());
            lineY += font.height() + 2f;
        }
    }

    private List<String> wrapText(String text, Font font, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.isEmpty() ? word : current + " " + word;
            if (font.width(test) <= maxWidth) {
                current = new StringBuilder(test);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            lines.add(text);
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!popupSettings.isEmpty()) {
            MouseButton mouseButton = MouseButton.fromButtonIndex(button);
            for (MenuPopupSetting popup : popupSettings) {
                if (popup.getBounds().contains(mouseX, mouseY)) {
                    popup.onMouseClicked(mouseX, mouseY, mouseButton);
                    return true;
                }
                popup.getAnimationScale().update(0);
            }
        }

        float searchWidth = 138f;
        float searchHeight = 18f;
        float searchX = width / 2f - searchWidth / 2f;
        float searchY = height - 30f;
        if (searchExpandAnim.getValue() > 0.01f
                && MathUtil.isHovered(mouseX, mouseY, searchX, searchY, searchWidth, searchHeight)) {
            searching = true;
            return true;
        }

        double scaledX = toScaledX(mouseX);
        double scaledY = toScaledY(mouseY);

        if (button == 0) {
            for (CategoryPanel panel : panels) {
                if (panel.isHeaderHovered(scaledX, scaledY)) {
                    draggedPanel = panel;
                    dragOffsetX = (float) scaledX - panel.getX();
                    dragOffsetY = (float) scaledY - panel.getY();
                    return true;
                }
            }
        }

        for (CategoryPanel panel : panels) {
            if (panel.mouseClicked(scaledX, scaledY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double scaledX = toScaledX(mouseX);
        double scaledY = toScaledY(mouseY);

        if (draggedPanel != null) {
            int draggedIndex = panels.indexOf(draggedPanel);
            int targetIndex = findTargetSlot(draggedPanel.getX());

            if (targetIndex != -1 && targetIndex != draggedIndex) {
                CategoryPanel other = panels.get(targetIndex);
                panels.set(draggedIndex, other);
                panels.set(targetIndex, draggedPanel);
                other.animateToSlot(slotX[draggedIndex], slotY);
            }

            int newIndex = panels.indexOf(draggedPanel);
            draggedPanel.animateToSlot(slotX[newIndex], slotY);
            draggedPanel = null;
            saveOrder();
        }

        for (CategoryPanel panel : panels) {
            panel.mouseReleased(scaledX, scaledY, button);
        }

        MouseButton mouseButton = MouseButton.fromButtonIndex(button);
        for (MenuPopupSetting popup : popupSettings) {
            popup.onMouseReleased(mouseX, mouseY, mouseButton);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        long window = mc.getWindow().getHandle();
        boolean ctrlDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (ctrlDown) {
            guiScale = net.minecraft.util.math.MathHelper.clamp(guiScale + (float) verticalAmount * SCALE_STEP, SCALE_MIN, SCALE_MAX);
            return true;
        }

        if (!popupSettings.isEmpty()) {
            for (MenuPopupSetting popup : popupSettings) {
                if (popup.getBounds().contains(mouseX, mouseY)) {
                    popup.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
                    return true;
                }
            }
        }

        double scaledX = toScaledX(mouseX);
        double scaledY = toScaledY(mouseY);
        for (CategoryPanel panel : panels) {
            if (panel.mouseScrolled(scaledX, scaledY, verticalAmount)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (MenuPopupSetting popup : popupSettings) {
            if (popup.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        for (CategoryPanel panel : panels) {
            if (panel.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && keyCode == GLFW.GLFW_KEY_F) {
            searching = true;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!popupSettings.isEmpty()) {
                popupSettings.forEach(popup -> popup.getAnimationScale().update(0));
                return true;
            }
            if (searching || !searchText.isEmpty()) {
                searching = false;
                searchText = "";
                return true;
            }
            needToClose = true;
            Menu.INSTANCE.setToggled(false);
            return true;
        }

        if (searching) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searching = false;
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (MenuPopupSetting popup : popupSettings) {
            if (popup.charTyped(chr, modifiers)) {
                return true;
            }
        }

        for (CategoryPanel panel : panels) {
            if (panel.charTyped(chr)) {
                return true;
            }
        }

        if (searching && !Character.isISOControl(chr) && searchText.length() < 20) {
            searchText += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
