package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.AirBlock;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.render.level.Render3DUtil;

import java.util.*;

@ModuleAnnotation(name = "JumpCircle", category = Category.RENDER, description = "????????? ?????? JumpCircle")
public final class JumpCircle extends Module {
    public static final JumpCircle INSTANCE = new JumpCircle();

    // mode
    private final ModeSetting mode     = new ModeSetting("?????");
    private final ModeSetting.Value modeCircle = new ModeSetting.Value(mode, "Круг").select();
    private final ModeSetting.Value modeBlocks = new ModeSetting.Value(mode, "Блоки");

    // circle settings
    private final NumberSetting maxRadius = new NumberSetting("Макс. радиус", 2.5f, 0.5f, 6.0f, 0.1f,  modeCircle::isSelected);
    private final NumberSetting speed     = new NumberSetting("????????", 0.05f, 0.01f, 0.2f, 0.005f);
    private final NumberSetting lineWidth = new NumberSetting("??????? ?????", 2.5f, 0.5f, 6.0f, 0.5f, modeCircle::isSelected);
    private final NumberSetting rings     = new NumberSetting("Кол-во колец", 2, 1, 4, 1,                modeCircle::isSelected);
    private final BooleanSetting fill     = new BooleanSetting("Заливка",     true,                      modeCircle::isSelected);

    // blocks settings
    private final NumberSetting blockRadius = new NumberSetting("Радиус блоков", 3, 1, 8, 1, modeBlocks::isSelected);
    private final NumberSetting blockWidth  = new NumberSetting("??????? ?????", 1.5f, 0.0f, 4.0f, 0.5f, modeBlocks::isSelected);

    // common
    private final BooleanSetting selfEffect = new BooleanSetting("?? ????", true);
    private final BooleanSetting others     = new BooleanSetting("?? ??????", true);

    private static final int SEGS = 64;

    private final Map<UUID, Boolean>          wasOnGround   = new HashMap<>();
    private final Map<UUID, CircleData>       circleEffects = new HashMap<>();
    private final Map<UUID, List<BlockData>>  blockEffects  = new HashMap<>();

    private JumpCircle() {}

    // -- Circle mode data ------------------------------------------------------
    private static class CircleData {
        float[] progresses;
        Vec3d pos;
        CircleData(Vec3d pos, int ringCount) {
            this.pos = pos;
            progresses = new float[ringCount];
            for (int i = 0; i < ringCount; i++) progresses[i] = -i * 0.15f;
        }
    }

    // -- Blocks mode data ------------------------------------------------------
    private static class BlockData {
        float progress; 
        List<BlockPos> blocks;
        Vec3d origin;
        BlockData(Vec3d origin, List<BlockPos> blocks) {
            this.origin = origin;
            this.blocks = blocks;
            this.progress = 0f;
        }
        // distance from origin for each block (precomputed)
        float[] distances;
        float maxDist;
        void computeDistances() {
            distances = new float[blocks.size()];
            maxDist = 0f;
            for (int i = 0; i < blocks.size(); i++) {
                BlockPos bp = blocks.get(i);
                float dx = (float)(bp.getX() + 0.5 - origin.x);
                float dz = (float)(bp.getZ() + 0.5 - origin.z);
                distances[i] = (float) Math.sqrt(dx*dx + dz*dz);
                if (distances[i] > maxDist) maxDist = distances[i];
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        circleEffects.clear();
        blockEffects.clear();
        wasOnGround.clear();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        // -- detect jumps ------------------------------------------------------
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!selfEffect.isEnabled() && player == mc.player) continue;
            if (!others.isEnabled() && player != mc.player) continue;

            UUID id = player.getUuid();
            boolean onGround = player.isOnGround();
            boolean prev = wasOnGround.getOrDefault(id, true);

            if (prev && !onGround) {
                Vec3d pos = MathUtil.interpolate(player);
                if (modeCircle.isSelected()) {
                    circleEffects.put(id, new CircleData(pos, (int) rings.getCurrent()));
                } else {
                    BlockData bd = new BlockData(pos, collectBlocks(pos, (int) blockRadius.getCurrent()));
                    bd.computeDistances();
                    blockEffects.computeIfAbsent(id, k -> new ArrayList<>()).add(bd);
                }
            }
            wasOnGround.put(id, onGround);
        }

        if (modeCircle.isSelected()) renderCircles(event.getMatrix());
        else renderBlocks();
    }

    // -- Circle rendering ------------------------------------------------------
    private void renderCircles(MatrixStack matrices) {
        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();

        Iterator<Map.Entry<UUID, CircleData>> it = circleEffects.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CircleData> entry = it.next();
            CircleData data = entry.getValue();

            boolean allDone = true;
            for (int ri = 0; ri < data.progresses.length; ri++) {
                data.progresses[ri] += speed.getCurrent();
                if (data.progresses[ri] < 1.0f) allDone = false;
            }
            if (allDone) { it.remove(); continue; }

            double cx = data.pos.x - cam.x;
            double cy = data.pos.y - cam.y + 0.03;
            double cz = data.pos.z - cam.z;

            for (float progress : data.progresses) {
                if (progress <= 0 || progress >= 1.0f) continue;

                float r        = progress * maxRadius.getCurrent();
                int   alpha    = (int) ((1f - progress) * 210);
                int   colorIdx = (int) (progress * 180);
                int   color     = Zenith.getInstance().getThemeManager().getClientColor(colorIdx).withAlpha(alpha).getRGB();
                int   colorFill = Zenith.getInstance().getThemeManager().getClientColor(colorIdx).withAlpha(alpha / 5).getRGB();

                matrices.push();
                RenderSystem.enableBlend();
                RenderSystem.disableCull();
                RenderSystem.enableDepthTest();
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
                MatrixStack.Entry mEntry = matrices.peek();

                if (fill.isEnabled()) {
                    RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                    BufferBuilder bufFill = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                    bufFill.vertex(mEntry.getPositionMatrix(), (float) cx, (float) cy, (float) cz).color(colorFill);
                    for (int i = 0; i <= SEGS; i++) {
                        double angle = (2 * Math.PI * i) / SEGS;
                        bufFill.vertex(mEntry.getPositionMatrix(),
                                (float) (cx + Math.cos(angle) * r), (float) cy,
                                (float) (cz + Math.sin(angle) * r)).color(colorFill);
                    }
                    BufferRenderer.drawWithGlobalProgram(bufFill.end());
                }

                RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
                RenderSystem.lineWidth(lineWidth.getCurrent());
                BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.LINES);
                for (int i = 0; i <= SEGS; i++) {
                    double angle = (2 * Math.PI * i) / SEGS;
                    float nx = (float) Math.cos(angle), nz = (float) Math.sin(angle);
                    buf.vertex(mEntry, (float) (cx + nx * r), (float) cy, (float) (cz + nz * r))
                            .color(color).normal(mEntry, nx, 0, nz);
                }
                BufferRenderer.drawWithGlobalProgram(buf.end());

                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                matrices.pop();
            }
        }
    }

    
    private void renderBlocks() {
        Iterator<Map.Entry<UUID, List<BlockData>>> it = blockEffects.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<BlockData>> entry = it.next();
            List<BlockData> list = entry.getValue();

            list.removeIf(data -> {
                data.progress += speed.getCurrent() * 0.55f;
                if (data.progress >= 1.0f) return true;

                if (data.distances == null) data.computeDistances();

                float waveFront = data.progress * (data.maxDist + 1.5f);
                float waveWidth = data.maxDist * 0.45f;

                for (int i = 0; i < data.blocks.size(); i++) {
                    BlockPos bp = data.blocks.get(i);
                    float dist = data.distances[i];

                    float delta = waveFront - dist;
                    if (delta < 0 || delta > waveWidth) continue;

                    float localT = 1f - (delta / waveWidth);
                    float fadeIn = Math.min(1f, delta / 0.3f);
                    float alpha  = fadeIn * localT;

                    int colorIdx  = (int) ((dist / Math.max(data.maxDist, 0.1f)) * 270);
                    int colorLine = Zenith.getInstance().getThemeManager().getClientColor(colorIdx)
                            .withAlpha((int) (alpha * 235)).getRGB();

                    Box box = new Box(bp).expand(0.002);
                    Render3DUtil.drawBox(box, colorLine, blockWidth.getCurrent(), true, true, false);
                }
                return false;
            });

            if (list.isEmpty()) it.remove();
        }
    }

    // -- Collect solid blocks in radius around jump point ----------------------
    private List<BlockPos> collectBlocks(Vec3d origin, int r) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = BlockPos.ofFloored(origin);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                // only surface layer: y = 0 and -1 relative to jump pos
                for (int dy = -1; dy <= 0; dy++) {
                    if (dx * dx + dz * dz > r * r) continue;
                    BlockPos bp = center.add(dx, dy, dz);
                    if (mc.world == null) continue;
                    var state = mc.world.getBlockState(bp);
                    if (!(state.getBlock() instanceof AirBlock) && !state.isAir()) {
                        result.add(bp);
                    }
                }
            }
        }
        return result;
    }
}









