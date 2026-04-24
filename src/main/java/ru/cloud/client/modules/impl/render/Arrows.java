package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.render.EventRender2D;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

@ModuleAnnotation(name = "Arrows", category = Category.RENDER, description = "Показывает стрелки к игрокам")
public final class Arrows extends Module {
    public static final Arrows INSTANCE = new Arrows();

    private static final Identifier TRIANGLE_TEXTURE = Zenith.id("icons/triangle.png");

    private float animationStep;
    private float animatedYaw;
    private float animatedPitch;

    private Arrows() {
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        float size = 60f;
        if (mc.currentScreen instanceof InventoryScreen) {
            size += 100f;
        }

        animationStep += (size - animationStep) * 0.1f;

        if (!mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.getName().getString().isEmpty()) {
                continue;
            }

            double x = player.prevX + (player.getX() - player.prevX) * event.getTickDelta()
                    - mc.gameRenderer.getCamera().getPos().getX();
            double z = player.prevZ + (player.getZ() - player.prevZ) * event.getTickDelta()
                    - mc.gameRenderer.getCamera().getPos().getZ();

            double cos = MathHelper.cos((float) (mc.gameRenderer.getCamera().getYaw() * (Math.PI * 2 / 360)));
            double sin = MathHelper.sin((float) (mc.gameRenderer.getCamera().getYaw() * (Math.PI * 2 / 360)));
            double rotY = -(z * cos - x * sin);
            double rotX = -(x * cos + z * sin);

            float angle = (float) (Math.atan2(rotY, rotX) * 180 / Math.PI);

            double x2 = animationStep * MathHelper.cos((float) Math.toRadians(angle)) + mc.getWindow().getScaledWidth() / 2f;
            double y2 = animationStep * MathHelper.sin((float) Math.toRadians(angle)) + mc.getWindow().getScaledHeight() / 2f;

            x2 += animatedYaw;
            y2 += animatedPitch;

            MatrixStack matrices = event.getContext().getMatrices();
            matrices.push();
            matrices.translate(x2, y2, 0);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(angle + 90));

            boolean isFriend = Zenith.getInstance().getFriendManager().isFriend(player.getName().getString());
            drawTriangle(matrices, isFriend);

            matrices.pop();
        }
    }

    private void drawTriangle(MatrixStack matrices, boolean isFriend) {
        int alpha = isFriend ? 255 : 205;
        ColorRGBA color1 = Zenith.getInstance().getThemeManager().getClientColor(0).withAlpha(alpha);
        ColorRGBA color2 = Zenith.getInstance().getThemeManager().getClientColor(90).withAlpha(alpha);
        ColorRGBA color3 = Zenith.getInstance().getThemeManager().getClientColor(180).withAlpha(alpha);
        ColorRGBA color4 = Zenith.getInstance().getThemeManager().getClientColor(270).withAlpha(alpha);
        DrawUtil.drawImageAlpha(matrices, TRIANGLE_TEXTURE, -8, -9, 18, 18, color1, color2, color3, color4);
    }
}

