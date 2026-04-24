package ru.cloud.client.hud.elements.component;

import by.saskkeee.user.UserInfo;
import net.minecraft.util.Identifier;
import ru.cloud.Zenith;
import ru.cloud.base.animations.base.Animation;
import ru.cloud.base.animations.base.Easing;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.base.theme.Theme;
import ru.cloud.client.hud.elements.HudElement;
import ru.cloud.client.hud.elements.draggable.DraggableHudElement;
import ru.cloud.client.modules.impl.render.Interface;
import ru.cloud.utility.game.other.TextUtil;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WatermarkComponent extends DraggableHudElement {
    private static final int GIF_FRAMES = 121;
    private static final int GIF_FRAME_DELAY_MS = 32;
    private final List<HudElement> elements = new ArrayList<>();
    private final Animation visibleAnim = new Animation(250, 0f, Easing.QUAD_IN_OUT);

    public WatermarkComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
    }

    @Override
    public void render(CustomDrawContext ctx) {
        if (Interface.INSTANCE.isNursultanWatermark()) {
            renderNew(ctx);
        } else {
            renderDefault(ctx);
        }
    }

    private void renderDefault(CustomDrawContext ctx) {
        float iconSize = 7f;
        float iconTextSpacing = 4f;
        float cellPadding = 5f;
        float borderRadius = 4f;
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();

        ColorRGBA mainBgColor = theme.getForegroundColor();
        ColorRGBA highlightBgColor = theme.getForegroundLight();
        ColorRGBA iconColor = theme.getColor();
        ColorRGBA textColor = theme.getWhite();
        Font font = Fonts.MEDIUM.getFont(6);

        elements.clear();
        elements.add(new HudElement("H", () -> "", ""));
        elements.add(new HudElement("2", () -> String.valueOf(UserInfo.getUsername())));
        elements.add(new HudElement("G", () -> String.valueOf(mc.getCurrentFps()), "fps"));
        elements.add(new HudElement("H", () -> mc.player.networkHandler.getServerInfo() == null || mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()) == null ? "0" : String.valueOf(mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()).getLatency()), "ms"));
        elements.add(new HudElement("I", () -> mc.player.networkHandler.getServerInfo() == null ? "20" : TextUtil.formatNumber(Zenith.getInstance().getServerHandler().getTPS()).replace(",", ".").replace(".0", ""), "tps"));

        float totalWidth = 0;
        for (HudElement el : elements) {
            el.calculateWidth(font, iconSize, cellPadding, iconTextSpacing);
            totalWidth += el.getWidth();
        }
        totalWidth += 4;
        float totalHeight = 17;
        this.width = totalWidth;
        this.height = totalHeight;

        DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, height, 21, BorderRadius.all(4), ColorRGBA.WHITE);

        float currentX = this.x;
        for (int i = 0; i < elements.size(); i++) {
            HudElement el = elements.get(i);
            BorderRadius r = (i == 0) ? BorderRadius.left(borderRadius, borderRadius) : (i == elements.size() - 1) ? BorderRadius.right(borderRadius, borderRadius) : BorderRadius.ZERO;
            ctx.drawRoundedRect(currentX, y, el.getWidth() + (i == elements.size() - 1 ? 4 : 0), totalHeight, r, i % 2 == 0 ? highlightBgColor : mainBgColor);
            el.drawContent(ctx, currentX + (i == 0 ? 4 : 0), y, totalHeight, iconSize, iconTextSpacing, iconColor, textColor, font);
            currentX += el.getWidth();
        }
        ctx.drawRoundedBorder(x, y, totalWidth, totalHeight, 0.01f, BorderRadius.all(4), theme.getForegroundStroke());
        DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y, totalWidth, totalHeight, 0.01f, 12f, theme.getColor(), BorderRadius.all(4));
    }

    private void renderNew(CustomDrawContext ctx) {
        if (mc.player == null) return;
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();

        Font fontMed   = Fonts.MEDIUM.getFont(6);
        Font fontSmall = Fonts.MEDIUM.getFont(5.5f);
        Font fontIcons = Fonts.ICONS.getFont(6);

        visibleAnim.update(1f);
        float anim = visibleAnim.getValue();

        String fpsVal  = String.valueOf(mc.getCurrentFps());
        String pingVal = mc.player.networkHandler.getServerInfo() == null
                || mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()) == null
                ? "0" : String.valueOf(mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()).getLatency());
        String username = UserInfo.getUsername();
        String time     = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String icoFps      = "G";
        String icoPing     = "H";
        String icoUsername = "2";
        String icoTime     = "A";

        float h      = 16f;
        float pad    = 5f;
        float otstup = 3f;
        float icoGap = 2f; // отступ между иконкой и текстом

        // суффиксы fps/ms
        String sufFps  = " fps";
        String sufPing = " ms";

        float logoSize = 16f;
        float logoW    = 20f;
        float nameW    = fontMed.width("Dantes") + 2.5f + fontMed.width("Client") + pad * 2;
        float usernameW = fontIcons.width(icoUsername) + icoGap + fontMed.width(username) + pad * 2;
        float fpsW     = fontIcons.width(icoFps) + icoGap + fontMed.width(fpsVal) + 1f + fontSmall.width(sufFps) + pad * 2;
        float pingW    = fontIcons.width(icoPing) + icoGap + fontMed.width(pingVal) + 1f + fontSmall.width(sufPing) + pad * 2;
        float timeW    = fontIcons.width(icoTime) + icoGap + fontMed.width(time) + pad * 2;

        float totalW = logoW + otstup + nameW + otstup + usernameW + otstup + fpsW + otstup + pingW + otstup + timeW;

        this.width  = totalW;
        this.height = h;

        ctx.pushMatrix();
        ctx.getMatrices().translate(x + totalW / 2f, y + h / 2f, 0);
        ctx.getMatrices().scale(anim, anim, 1f);
        ctx.getMatrices().translate(-(x + totalW / 2f), -(y + h / 2f), 0);

        DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, totalW, h, 21, BorderRadius.all(4), ColorRGBA.WHITE);

        float cx  = x;
        float icoY = y + (h - fontIcons.getSize() * 0.7f) / 2f;
        float txtY = y + (h - fontMed.getSize() * 0.7f) / 2f;
        float sufY = y + (h - fontSmall.getSize() * 0.7f) / 2f + 0.5f;

        // лого
        ctx.drawTexture(
                getAnimatedLogoFrame(),
                cx + (logoW - logoSize) / 2f,
                y + (h - logoSize) / 2f,
                logoSize,
                logoSize,
                ColorRGBA.WHITE
        );
        cx += logoW + otstup;

        // Dantes Client
        ctx.drawRoundedRect(cx, y, nameW, h, BorderRadius.all(4), theme.getForegroundLight());
        ctx.drawText(fontMed, "Dantes", cx + pad, txtY, theme.getWhite());
        ctx.drawText(fontMed, "Client", cx + pad + fontMed.width("Dantes") + 2.5f, txtY, theme.getWhite());
        cx += nameW + otstup;

        // username
        ctx.drawRoundedRect(cx, y, usernameW, h, BorderRadius.all(4), theme.getForegroundColor());
        ctx.drawText(fontIcons, icoUsername, cx + pad, icoY, theme.getColor());
        ctx.drawText(fontMed, username, cx + pad + fontIcons.width(icoUsername) + icoGap, txtY, theme.getWhite());
        cx += usernameW + otstup;

        // fps + суффикс
        ctx.drawRoundedRect(cx, y, fpsW, h, BorderRadius.all(4), theme.getForegroundColor());
        ctx.drawText(fontIcons, icoFps, cx + pad, icoY, theme.getColor());
        ctx.drawText(fontMed, fpsVal, cx + pad + fontIcons.width(icoFps) + icoGap, txtY, theme.getWhite());
        ctx.drawText(fontSmall, sufFps, cx + pad + fontIcons.width(icoFps) + icoGap + fontMed.width(fpsVal) + 1f, sufY, theme.getGrayLight());
        cx += fpsW + otstup;

        // ping + суффикс
        ctx.drawRoundedRect(cx, y, pingW, h, BorderRadius.all(4), theme.getForegroundColor());
        ctx.drawText(fontIcons, icoPing, cx + pad, icoY, theme.getColor());
        ctx.drawText(fontMed, pingVal, cx + pad + fontIcons.width(icoPing) + icoGap, txtY, theme.getWhite());
        ctx.drawText(fontSmall, sufPing, cx + pad + fontIcons.width(icoPing) + icoGap + fontMed.width(pingVal) + 1f, sufY, theme.getGrayLight());
        cx += pingW + otstup;

        // time
        ctx.drawRoundedRect(cx, y, timeW, h, BorderRadius.all(4), theme.getForegroundColor());
        ctx.drawText(fontIcons, icoTime, cx + pad, icoY, theme.getColor());
        ctx.drawText(fontMed, time, cx + pad + fontIcons.width(icoTime) + icoGap, txtY, theme.getWhite());

        ctx.popMatrix();
    }

    private Identifier getAnimatedLogoFrame() {
        int frame = (int) ((System.currentTimeMillis() / GIF_FRAME_DELAY_MS) % GIF_FRAMES) + 1;
        return Zenith.id("textures/watermark/gif/" + frame + ".png");
    }
}
