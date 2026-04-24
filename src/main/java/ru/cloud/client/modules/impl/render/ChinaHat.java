package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.render.level.Render3DUtil;

@ModuleAnnotation(name = "ChinaHat", category = Category.RENDER, description = "Конус над игроками")
public final class ChinaHat extends Module {
    public static final ChinaHat INSTANCE = new ChinaHat();

    private final NumberSetting radius    = new NumberSetting("Радиус",             0.6f, 0.2f, 2.0f, 0.05f);
    private final NumberSetting height    = new NumberSetting("Высота",             1.2f, 0.3f, 3.0f, 0.1f);
    private final NumberSetting segments  = new NumberSetting("Сегменты",           48,   12,   64,   4);
    private final NumberSetting outlineW  = new NumberSetting("Толщина контура",    1.5f, 0.0f, 4.0f, 0.5f);
    private final NumberSetting spinSpeed = new NumberSetting("Скорость вращения",  1.5f, 0.0f, 5.0f, 0.5f);

    private final BooleanSetting selfHat   = new BooleanSetting("Себе тоже",     false);
    private final BooleanSetting othersHat = new BooleanSetting("Другие игроки", true);
    private final BooleanSetting fill      = new BooleanSetting("Заливка",       true);
    private final BooleanSetting outline   = new BooleanSetting("Контур",        true);
    private final BooleanSetting pulse     = new BooleanSetting("Пульсация",     true);

    private float rotAngle  = 0f;
    private float pulseTime = 0f;

    private ChinaHat() {}

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;
        if (mc.options.getPerspective().isFirstPerson()) return;

        rotAngle  = (rotAngle + spinSpeed.getCurrent()) % 360f;
        pulseTime += 0.06f;

        float pulseFactor = pulse.isEnabled() ? 1f + 0.08f * (float) Math.sin(pulseTime) : 1f;

        MatrixStack matrices = event.getMatrix();
        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!selfHat.isEnabled() && player == mc.player) continue;
            if (!othersHat.isEnabled() && player != mc.player) continue;

            Vec3d pos  = MathUtil.interpolate(player);
            double headY = pos.y + player.getStandingEyeHeight() + 0.2;
            double cx = pos.x - cam.x, cy = headY - cam.y, cz = pos.z - cam.z;

            double r = radius.getCurrent() * pulseFactor;
            double h = height.getCurrent() * pulseFactor;
            int segs = (int) segments.getCurrent();
            double spin = Math.toRadians(rotAngle);

            int cBase  = Zenith.getInstance().getThemeManager().getClientColor(0).withAlpha(55).getRGB();
            int cApex  = Zenith.getInstance().getThemeManager().getClientColor(180).withAlpha(28).getRGB();
            int cLBase = Zenith.getInstance().getThemeManager().getClientColor(0).getRGB();
            int cLApex = Zenith.getInstance().getThemeManager().getClientColor(180).getRGB();

            matrices.push();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            MatrixStack.Entry entry = matrices.peek();

            renderCone(entry, cx, cy, cz, r, h, segs, spin, cBase, cApex, cLBase, cLApex, cam);

            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            matrices.pop();
        }
    }

    private void renderCone(MatrixStack.Entry entry, double cx, double cy, double cz,
                            double r, double h, int segs, double spin,
                            int cBase, int cApex, int cLBase, int cLApex, Vec3d cam) {
        if (fill.isEnabled()) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= segs; i++) {
                double a = spin + (2 * Math.PI * i) / segs;
                buf.vertex(entry.getPositionMatrix(), (float)(cx + Math.cos(a)*r), (float)cy, (float)(cz + Math.sin(a)*r)).color(cBase);
                buf.vertex(entry.getPositionMatrix(), (float)cx, (float)(cy+h), (float)cz).color(cApex);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());

            BufferBuilder bufBase = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            bufBase.vertex(entry.getPositionMatrix(), (float)cx, (float)cy, (float)cz).color(cBase);
            for (int i = 0; i <= segs; i++) {
                double a = spin + (2 * Math.PI * i) / segs;
                bufBase.vertex(entry.getPositionMatrix(), (float)(cx+Math.cos(a)*r), (float)cy, (float)(cz+Math.sin(a)*r)).color(cBase);
            }
            BufferRenderer.drawWithGlobalProgram(bufBase.end());
        }
        if (outline.isEnabled() && outlineW.getCurrent() > 0) {
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            RenderSystem.lineWidth(outlineW.getCurrent());
            BufferBuilder buf2 = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.LINES);
            for (int i = 0; i <= segs; i++) {
                double a = spin + (2 * Math.PI * i) / segs;
                float nx = (float)Math.cos(a), nz = (float)Math.sin(a);
                buf2.vertex(entry, (float)(cx+nx*r), (float)cy, (float)(cz+nz*r)).color(cLBase).normal(entry, nx, 0, nz);
            }
            BufferRenderer.drawWithGlobalProgram(buf2.end());
            for (int i = 0; i < 4; i++) {
                double a = spin + (2 * Math.PI * i) / 4;
                Render3DUtil.drawLine(
                        new Vec3d(cx+Math.cos(a)*r+cam.x, cy+cam.y, cz+Math.sin(a)*r+cam.z),
                        new Vec3d(cx+cam.x, cy+h+cam.y, cz+cam.z),
                        cLBase, cLApex, outlineW.getCurrent(), true);
            }
        }
    }
}
