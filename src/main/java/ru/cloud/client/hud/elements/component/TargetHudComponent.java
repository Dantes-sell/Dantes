package ru.cloud.client.hud.elements.component;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import ru.cloud.Zenith;
import ru.cloud.base.animations.base.Animation;
import ru.cloud.base.animations.base.Easing;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.base.theme.Theme;
import ru.cloud.client.hud.elements.draggable.DraggableHudElement;
import ru.cloud.client.modules.impl.combat.Aura;
import ru.cloud.client.modules.impl.combat.AuraV2;
import ru.cloud.client.modules.impl.render.Interface;
import ru.cloud.utility.game.player.PlayerIntersectionUtil;
import ru.cloud.utility.mixin.accessors.DrawContextAccessor;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.base.Gradient;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import java.util.List;

import static java.lang.Math.round;
public class TargetHudComponent extends DraggableHudElement {

    private final Animation healthAnimation = new Animation(200, Easing.LINEAR);
    private final Animation gappleAnimation = new Animation(200, Easing.LINEAR);
    private final Animation toggleAnimation = new Animation(200, Easing.QUAD_IN_OUT);
    private final Animation targetSwitchAnimation = new Animation(150, Easing.SINE_IN_OUT);
    private LivingEntity target;
    private float nursultanHealth = 0f;

    private String lastTargetName = "";

    public TargetHudComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name,initialX, initialY,windowWidth,windowHeight,offsetX,offsetY,align);
    }

    @Override
    public void render(CustomDrawContext ctx) {
        this.width=145f;
        this.height=40f;

        LivingEntity target = (mc.currentScreen instanceof ChatScreen) ? mc.player : resolveAuraTarget();
        setTarget(target);

        if (toggleAnimation.getValue() == 0 ||this.target ==null) return;

        if (Interface.INSTANCE.isNursultanTargetHud()) {
            renderNursultan(ctx, this.target, toggleAnimation.getValue());
            return;
        }

        renderTargetHud(ctx, this.target, toggleAnimation.getValue());
    }

    private LivingEntity resolveAuraTarget() {
        LivingEntity auraTarget = Aura.INSTANCE.isEnabled() ? Aura.INSTANCE.getTarget() : null;
        if (auraTarget != null) {
            return auraTarget;
        }

        return AuraV2.INSTANCE.isEnabled() ? AuraV2.INSTANCE.getTarget() : null;
    }

    private void renderTargetHud(CustomDrawContext ctx, LivingEntity target, float animation) {
        float posX = x;
        float posY = y;
        float width = 103f;
        float height = 31f;
        float headSize = 19f;
        float padding = 6f;
        float borderRadius = 6f;

        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA textColor = theme.getWhite();
        ColorRGBA accent = theme.getColor();
        ColorRGBA panelColor = new ColorRGBA(18, 16, 14, 205);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(posX + width / 2f, posY + height / 2f, 0f);
        ctx.getMatrices().scale(animation, animation, 1f);
        ctx.getMatrices().translate(-(posX + width / 2f), -(posY + height / 2f), 0f);

        ctx.drawClientRect(posX, posY, width, height, 1.0f, 0.0f, 7.0f);
        ctx.drawRoundedRect(posX, posY, width, height, BorderRadius.all(borderRadius), panelColor);
        ctx.drawRoundedBorder(posX, posY, width, height, 0.1f, BorderRadius.all(borderRadius), theme.getForegroundStroke());

        float hp = PlayerIntersectionUtil.getHealth(target);
        float maxHp = PlayerIntersectionUtil.getMaxHealth(target);
        float baseMaxHp = target.getMaxHealth();
        float absorption = Math.max(0, maxHp - baseMaxHp);
        float gapple = Math.max(0, hp - baseMaxHp);

        float healthPercent = maxHp > 0 ? hp / maxHp : 0;
        float gapplePercent = maxHp > 0 ? gapple / maxHp : 0;
        float barFullWidth = 65f;

        float animatedHealth = healthAnimation.update(barFullWidth * healthPercent);
        float animatedGapple = gappleAnimation.update(barFullWidth * gapplePercent);

        float headX = posX + padding;
        float headY = posY + 6f;

        if (target instanceof PlayerEntity player) {
            DrawUtil.drawPlayerHeadWithRoundedShader(
                    ctx.getMatrices(),
                    ((AbstractClientPlayerEntity) player).getSkinTextures().texture(),
                    headX, headY, headSize,
                    BorderRadius.all(3f), ColorRGBA.WHITE
            );
        } else {
            Font qFont = Fonts.MEDIUM.getFont(12);
            ctx.drawText(qFont, "?", headX + (headSize-qFont.width("?")) / 2f,
                    headY + headSize / 2f - qFont.height() / 2f, ColorRGBA.WHITE);
        }

        Font nameFont = Fonts.REGULAR.getFont(7f);
        String name = target.getName().getString();
        float maxNameWidth = 60f;
        String displayName = name;
        if (nameFont.width(name) > maxNameWidth) {
            while (nameFont.width(displayName + "...") > maxNameWidth && displayName.length() > 0) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            displayName += "...";
        }

        String hpText = (int) Math.round(hp) + " HP";
        Font hpFont = Fonts.SEMIBOLD.getFont(6f);
        ctx.drawText(nameFont, displayName, posX + 30f, posY + 4f, textColor);
        ctx.drawText(hpFont, hpText, posX + 30f, posY + 13f, textColor.withAlpha(220));

        float barX = posX + 30f;
        float barY = posY + height - 6f;
        ctx.drawRoundedRect(barX, barY, barFullWidth, 3f, BorderRadius.all(0.7f), theme.getForegroundLight().withAlpha(130));
        ctx.drawRoundedRect(barX, barY, animatedHealth, 3f, BorderRadius.all(0.7f), accent);
        if (animatedGapple > 0.2f) {
            ctx.drawRoundedRect(barX + Math.max(0f, animatedHealth - animatedGapple), barY, animatedGapple, 3f, BorderRadius.all(0.7f), new ColorRGBA(255, 220, 81, 255));
        }

        if (target instanceof PlayerEntity player) {
            drawArmor(ctx, player, posX + 30f, posY + 19f, 0f, 0f, 0f);
        }

        ctx.getMatrices().pop();

        this.width = width;
        this.height = height;
    }

    private void drawArmor(CustomDrawContext ctx, PlayerEntity player, float posX, float posY, float headSize, float padding, float fontSize) {
        float boxSizeItem = 10;
        float paddingItem = 4;
        float iconX = posX;
        float iconY = posY + 1;

        Font xFont = Fonts.ICONS.getFont(5f);
        List<ItemStack> armor = player.getInventory().armor;
        ItemStack[] items = {
                player.getMainHandStack(),
                player.getOffHandStack(),
                armor.get(3), armor.get(2), armor.get(1), armor.get(0)
        };
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                ctx.getMatrices().push();
                ctx.getMatrices().translate(iconX+(boxSizeItem-9.6)/2,iconY+(boxSizeItem-9.6)/2,0);
                ctx.getMatrices().scale(0.6f, 0.6f, 0.6f);
                ctx.drawItem(stack, 0,0);
                ((DrawContextAccessor) ctx).callDrawItemBar(stack,0,0);
                ((DrawContextAccessor) ctx).callDrawCooldownProgress(stack,0,0);
                ctx.getMatrices().pop();
            } else {
                ctx.drawText(xFont, "M", iconX + (boxSizeItem-xFont.width("X"))/2, iconY + (boxSizeItem-xFont.height())/2, Zenith.getInstance().getThemeManager().getCurrentTheme().getGrayLight());
            }
            iconX += boxSizeItem + paddingItem;
        }
    }

    private void renderNursultan(CustomDrawContext ctx, LivingEntity target, float animation) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(8f);
        Font fontSmall = Fonts.MEDIUM.getFont(7f);
        ColorRGBA bg = theme.getForegroundColor();
        ColorRGBA accent = theme.getColor();

        float w = 100f, h = 36f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + w / 2f, y + h / 2f, 0f);
        ctx.getMatrices().scale(animation, animation, 1f);
        ctx.getMatrices().translate(-(x + w / 2f), -(y + h / 2f), 0f);

        // Основной блок
        DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, w, h, 21, BorderRadius.all(8), ColorRGBA.WHITE);
        ctx.drawRoundedRect(x, y, w, h, BorderRadius.all(8), bg);

        // HP
        float hp = PlayerIntersectionUtil.getHealth(target);
        float maxHp = target.getMaxHealth();
        float widthHp = 61f;
        nursultanHealth = net.minecraft.util.math.MathHelper.clamp(
                nursultanHealth + (Math.round(hp / maxHp * widthHp) - nursultanHealth) * 0.1f, 2f, widthHp);

        // HP ��� ���
        ctx.drawRoundedRect(x + 34, y + 22f, widthHp, 8f, BorderRadius.all(4), theme.getForegroundLight());
        
        ColorRGBA barDark = accent.darker(0.5f);
        ctx.drawRoundedRect(x + 34, y + 22f, nursultanHealth, 8f, BorderRadius.all(4),
                Gradient.of(barDark, barDark, accent, accent));

        // РРјСЏ
        String name = target.getName().getString();
        if (font.width(name) > 60f) {
            while (font.width(name + "...") > 60f && name.length() > 0)
                name = name.substring(0, name.length() - 1);
            name += "...";
        }
        ctx.drawText(font, name, x + 35, y + 6, theme.getWhite());

        // HP �����
        ctx.drawText(fontSmall, "HP: " + (int) Math.round(hp), x + 35.5f, y + 15.5f, theme.getWhite());

        // ������
        if (target instanceof AbstractClientPlayerEntity player) {
            DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(),
                    player.getSkinTextures().texture(),
                    x + 3, y + 4f, 28, BorderRadius.all(0.5f), ColorRGBA.WHITE);
        }

        ctx.getMatrices().pop();

        this.width = w;
        this.height = h;
    }

    public void setTarget(LivingEntity target) {
        if (target == null) {
            toggleAnimation.update(0);
            if (toggleAnimation.getValue() == 0) {
                this.target = null;
            }
        } else {
            if(target!=this.target) {
                toggleAnimation.update(0);
                if(toggleAnimation.getValue()==0){
                    this.target = target;
                }
            }else {
                toggleAnimation.update(1);
            }
        }
    }
}

