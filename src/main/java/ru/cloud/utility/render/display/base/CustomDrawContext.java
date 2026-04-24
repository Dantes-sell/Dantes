package ru.cloud.utility.render.display.base;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.MsdfRenderer;
import ru.cloud.base.theme.GuiStyle;
import ru.cloud.base.theme.Theme;
import ru.cloud.Zenith;
import ru.cloud.utility.interfaces.IMinecraft;
import ru.cloud.utility.mixin.accessors.DrawContextAccessor;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import java.util.Objects;

public class CustomDrawContext extends DrawContext implements IMinecraft {


    public CustomDrawContext(VertexConsumerProvider.Immediate vertexConsumerProvider) {
        super(mc, vertexConsumerProvider);

    }

    public CustomDrawContext(DrawContext originalContext) {
        super(mc, ((DrawContextAccessor) originalContext).getVertexConsumers());

    }

    public static CustomDrawContext of(DrawContext originalContext) {
        return new CustomDrawContext(originalContext);
    }

    public void drawText(Font font, String text, float x, float y, ColorRGBA color) {
        MsdfRenderer.renderText(font.getFont(), text, font.getSize(), color.getRGB(), getMatrices().peek().getPositionMatrix(), x, y, 0);
    }

    public void drawText(Font font, String text, float x, float y, Gradient color) {
        MsdfRenderer.renderText(font.getFont(), text, font.getSize(), color, getMatrices().peek().getPositionMatrix(), x, y, 0);
    }

    public void drawText(Font font, Text text, float x, float y) {
        MsdfRenderer.renderText(font.getFont(), text, font.getSize(), getMatrices().peek().getPositionMatrix(), x, y, 0);
    }

    public void drawSquircle(float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
        DrawUtil.drawSquircle(this.getMatrices(), x, y, width, height, squirt, borderRadius, color);
    }

    public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
        DrawUtil.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, color);
    }

    public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
        DrawUtil.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, gradient);
    }

    public void drawRect(float x, float y, float width, float height, ColorRGBA color) {
        DrawUtil.drawRect(this.getMatrices(), x, y, width, height, color);
    }

    public void drawBlurredRect(float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
        DrawUtil.drawBlur(this.getMatrices(), x, y, width, height, blurRadius, borderRadius, color);
    }

    public void drawBlurredRect(float x, float y, float width, float height, float blurRadius, float squirt, BorderRadius borderRadius, ColorRGBA color) {
        DrawUtil.drawBlur(this.getMatrices(), x, y, width, height, blurRadius, squirt, borderRadius, color);
    }

    public void drawLiquidGlass(float x, float y, float width, float height, float squirt, float power, BorderRadius borderRadius, ColorRGBA color) {
        BorderRadius scaledRadius = new BorderRadius(
                borderRadius.topLeftRadius() * squirt / 2.0f,
                borderRadius.topRightRadius() * squirt / 2.0f,
                borderRadius.bottomRightRadius() * squirt / 2.0f,
                borderRadius.bottomLeftRadius() * squirt / 2.0f
        );
        DrawUtil.drawLiquidGlass(
                this.getMatrices(),
                x,
                y,
                width,
                height,
                scaledRadius,
                color,
                color.getAlpha() / 255.0f,
                50.0f,
                color.withAlpha(255.0f),
                1.0f,
                true,
                0.0f,
                power,
                squirt,
                false
        );
    }

    public void drawClientRect(float x, float y, float width, float height, float alpha, float dragAnim, float squircle) {
        GuiStyle guiStyle = Zenith.getInstance().getThemeManager().getGuiStyle();
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        boolean dark = theme == Theme.DARK;

        if (guiStyle == GuiStyle.MINIMALISM) {
            this.drawBlurredRect(x, y, width, height, 45.0f, squircle, BorderRadius.all(6.0f), ColorRGBA.WHITE.withAlpha(255.0f * alpha));
        } else {
            this.drawLiquidGlass(x, y, width, height, squircle, 0.08f - 0.07f * dragAnim, BorderRadius.all(6.0f), ColorRGBA.WHITE.withAlpha(255.0f * alpha));
        }

        float baseAlpha = dark ? (guiStyle == GuiStyle.LIQUID_GLASS ? 52.0f : 204.0f) : 180.0f;
        this.drawSquircle(
                x,
                y,
                width,
                height,
                squircle,
                BorderRadius.all(6.0f),
                new ColorRGBA(18, 16, 14, (int) (baseAlpha * alpha))
        );
    }

    public int drawTextWithBackground(TextRenderer textRenderer, Text text, int x, int y, int width,BorderRadius borderRadius, ColorRGBA textColor, ColorRGBA backgroundColor) {


        int var10001 = x - 3;
        int var10002 = y - 2;
        int var10003 = width + 6;
        Objects.requireNonNull(textRenderer);
        this.drawRoundedRect(var10001, var10002, var10003,  9 + 4,borderRadius,backgroundColor);


        return this.drawText(textRenderer, text, x, y, textColor.getRGB(), true);
    }

    public void drawSprite(CustomSprite sprite, float x, float y, float width, float height, ColorRGBA textureColor) {
        DrawUtil.drawSprite(this.getMatrices(), sprite, x, y, width, height, textureColor);
    }

    public void drawRoundedCorner(float x, float y, float width, float height, float borderThikenes, float widthCorner, ColorRGBA color, BorderRadius radius) {

        width = Math.round(width);
        height = Math.round(height);
        this.enableScissor((int) Math.ceil(x - 10), (int) (y - 10), (int) (x + widthCorner), (int) (y + widthCorner));
        drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
        this.disableScissor();

        this.enableScissor((int) (x + width - widthCorner), (int) (y - 10), (int) (x + width + 10), (int) (y + widthCorner));
        drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
        this.disableScissor();

        this.enableScissor((int) (x - 10), (int) (y + height - widthCorner), (int) (x + widthCorner), (int) (y + height + 10));
        drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
        this.disableScissor();

        this.enableScissor((int) (x + width - widthCorner), (int) (y + height - widthCorner), (int) (x + width + 10), (int) (y + height + 10));
        drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
        this.disableScissor();
    }

    public void drawRoundedBorder(float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor) {
        DrawUtil.drawRoundedBorder(this.getMatrices(), x, y, width, height, borderThickness, borderRadius, borderColor);
    }

    public void drawTexture(Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
        DrawUtil.drawTexture(this.getMatrices(), identifier, x, y, width, height, textureColor);
    }

    public void pushMatrix() {
        getMatrices().push();
    }

    public void popMatrix() {
        getMatrices().pop();
    }
}
