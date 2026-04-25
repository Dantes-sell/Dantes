package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.render.display.Render2DUtil;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

@ModuleAnnotation(name = "Trails", category = Category.RENDER, description = "След за игроком")
public final class Trails extends Module {
    public static final Trails INSTANCE = new Trails();

    // visual mode
    private final ModeSetting mode          = new ModeSetting("Режим");
    private final ModeSetting.Value modeLine   = new ModeSetting.Value(mode, "M od eL in e").select();
    private final ModeSetting.Value modeRibbon = new ModeSetting.Value(mode, "M od eR ib bo n");

    // color mode
    private final ModeSetting colorMode         = new ModeSetting("Цвет");
    private final ModeSetting.Value colorGrad    = new ModeSetting.Value(colorMode, "C ol or Gr ad").select();
    private final ModeSetting.Value colorRainbow = new ModeSetting.Value(colorMode, "Радуга");

    private final NumberSetting trailLength = new NumberSetting("Длина следа",   80, 10, 300, 5);
    private final NumberSetting lineWidth   = new NumberSetting("L in eW id th", 3.0f, 0.5f, 10.0f, 0.5f, modeLine::isSelected);
    private final NumberSetting minDist     = new NumberSetting("M in Di st", 0.04f, 0.01f, 0.3f, 0.01f);

    private final Deque<Vec3d> points = new ArrayDeque<>();
    private Vec3d lastPos = null;
    private boolean wasFirstPerson = false;
    private long lastFadeTime = 0L;

    private Trails() {}

    @Override
    public void onDisable() {
        super.onDisable();
        points.clear();
        lastPos = null;
        wasFirstPerson = false;
        lastFadeTime = 0L;
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        boolean firstPerson = mc.options.getPerspective().isFirstPerson();

        
        if (firstPerson && !wasFirstPerson) {
            points.clear();
            lastPos = null;
        }
        wasFirstPerson = firstPerson;

        if (firstPerson) return;

        // �������� ����� � ��� ������
        Vec3d feetPos = MathUtil.interpolate(mc.player);

        
        if (lastPos == null) {
            lastPos = feetPos;
            return;
        }

        boolean moving = feetPos.distanceTo(lastPos) > minDist.getCurrent();

        if (moving) {
            points.addLast(feetPos);
            lastPos = feetPos;
            lastFadeTime = System.currentTimeMillis();
        } else if (!points.isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - lastFadeTime >= 5) {
                points.pollFirst();
                lastFadeTime = now;
            }
        }

        int maxLen = (int) trailLength.getCurrent();
        while (points.size() > maxLen) points.pollFirst();
        if (points.size() < 2) return;

        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
        MatrixStack matrices = event.getMatrix();
        Vec3d[] arr = points.toArray(new Vec3d[0]);
        int total = arr.length;

        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        if (modeLine.isSelected()) {
            renderLine(matrices, arr, total, cam);
        } else {
            renderRibbon(matrices, arr, total, cam);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private void renderLine(MatrixStack matrices, Vec3d[] arr, int total, Vec3d cam) {
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(lineWidth.getCurrent());
        MatrixStack.Entry entry = matrices.peek();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        for (int i = 0; i < total - 1; i++) {
            float t = (float) (i + 1) / total;
            int color = getColor(t, i);

            Vec3d a = arr[i].subtract(cam);
            Vec3d b = arr[i + 1].subtract(cam);
            float dx = (float)(b.x-a.x), dy = (float)(b.y-a.y), dz = (float)(b.z-a.z);
            float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (len == 0) continue;
            float nx = dx/len, ny = dy/len, nz = dz/len;

            buf.vertex(entry, (float)a.x, (float)a.y, (float)a.z).color(color).normal(entry, nx, ny, nz);
            buf.vertex(entry, (float)b.x, (float)b.y, (float)b.z).color(color).normal(entry, nx, ny, nz);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void renderRibbon(MatrixStack matrices, Vec3d[] arr, int total, Vec3d cam) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        MatrixStack.Entry entry = matrices.peek();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        
        float bodyHeight = mc.player.getHeight() * 0.85f;

        for (int i = 0; i < total - 1; i++) {
            float t0 = (float) i / total;
            float t1 = (float) (i + 1) / total;
            int c0 = getColor(t0, i);
            int c1 = getColor(t1, i + 1);

            Vec3d a = arr[i].subtract(cam);
            Vec3d b = arr[i + 1].subtract(cam);

            
            
            float ayBot = (float) a.y + 0.02f;   // ���� ���� �����
            float ayTop = (float) a.y + bodyHeight;
            float byBot = (float) b.y + 0.02f;
            float byTop = (float) b.y + bodyHeight;

            // ����: ������-A, �������-A, �������-B, ������-B
            buf.vertex(entry.getPositionMatrix(), (float)a.x, ayBot, (float)a.z).color(c0);
            buf.vertex(entry.getPositionMatrix(), (float)a.x, ayTop, (float)a.z).color(c0);
            buf.vertex(entry.getPositionMatrix(), (float)b.x, byTop, (float)b.z).color(c1);
            buf.vertex(entry.getPositionMatrix(), (float)b.x, byBot, (float)b.z).color(c1);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private int getColor(float t, int index) {
        if (colorRainbow.isSelected()) {
            Color rc = Render2DUtil.rainbow(20, index * 8, 0.85f, 1f, t);
            return new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), (int)(t * 230)).getRGB();
        } else {
            return Zenith.getInstance().getThemeManager()
                    .getClientColor((int)(t * 270))
                    .withAlpha((int)(t * 230)).getRGB();
        }
    }
}


