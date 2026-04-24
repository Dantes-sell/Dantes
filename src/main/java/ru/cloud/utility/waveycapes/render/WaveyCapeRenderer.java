package ru.cloud.utility.waveycapes.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import ru.cloud.utility.waveycapes.WaveyCapesBase;
import ru.cloud.utility.waveycapes.enums.CapeMovement;
import ru.cloud.utility.waveycapes.enums.CapeStyle;
import ru.cloud.utility.waveycapes.enums.WindMode;
import ru.cloud.utility.waveycapes.interfaces.CapeHolder;
import ru.cloud.utility.waveycapes.math.Vector3;
import ru.cloud.utility.waveycapes.sim.StickSimulation;

public class WaveyCapeRenderer {

    public static final int PART_COUNT = 16;

    /**
     * Вызывается из mixin вместо стандартного рендера плаща.
     * Возвращает true если плащ был отрендерен нами.
     */
    public static boolean render(PlayerEntityRenderState renderState, MatrixStack matrices,
                                  VertexConsumerProvider vertexConsumers, int light, float tickDelta) {
        if (WaveyCapesBase.config == null) return false;

        
        if (!renderState.capeVisible) return false;
        if (renderState.skinTextures == null) return false;
        net.minecraft.util.Identifier capeTexture = renderState.skinTextures.capeTexture();
        if (capeTexture == null) return false;

        // Получаем живого игрока по entity ID из render state
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return false;

        net.minecraft.entity.Entity entity = mc.world.getEntityById(renderState.id);
        if (!(entity instanceof AbstractClientPlayerEntity player)) return false;

        // Не рендерим если надет элитра
        if (player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) return false;

        // Обновляем симуляцию
        ((CapeHolder) player).updateSimulation(player, PART_COUNT);

        if (WaveyCapesBase.config.capeStyle == CapeStyle.SMOOTH) {
            renderSmooth(matrices, vertexConsumers, capeTexture, player, tickDelta, light);
        } else {
            renderBlocky(matrices, vertexConsumers, capeTexture, player, tickDelta, light);
        }
        return true;
    }

    private static void renderBlocky(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                      net.minecraft.util.Identifier capeTexture, AbstractClientPlayerEntity player,
                                      float tickDelta, int light) {
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(capeTexture));
        for (int part = 0; part < PART_COUNT; part++) {
            matrices.push();
            applyPartTransform(matrices, player, tickDelta, part);
            // Простой квад для каждой части
            float u0 = 0.015625f;
            float u1 = 0.34375f;
            float v0 = 0.03125f + (0.5f / PART_COUNT) * part;
            float v1 = 0.03125f + (0.5f / PART_COUNT) * (part + 1);
            Matrix4f m = matrices.peek().getPositionMatrix();
            float y = (float) part / PART_COUNT * (-0.96f);
            float y2 = (float) (part + 1) / PART_COUNT * (-0.96f);
            consumer.vertex(m, -0.3f, y, -0.06f).color(1f, 1f, 1f, 1f).texture(u1, v0).overlay(0).light(light).normal(0, 0, 1);
            consumer.vertex(m, 0.3f, y, -0.06f).color(1f, 1f, 1f, 1f).texture(u0, v0).overlay(0).light(light).normal(0, 0, 1);
            consumer.vertex(m, 0.3f, y2, -0.06f).color(1f, 1f, 1f, 1f).texture(u0, v1).overlay(0).light(light).normal(0, 0, 1);
            consumer.vertex(m, -0.3f, y2, -0.06f).color(1f, 1f, 1f, 1f).texture(u1, v1).overlay(0).light(light).normal(0, 0, 1);
            matrices.pop();
        }
    }

    private static void renderSmooth(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                      net.minecraft.util.Identifier capeTexture, AbstractClientPlayerEntity player,
                                      float tickDelta, int light) {
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(capeTexture));

        Matrix4f prevMatrix = null;
        for (int part = 0; part < PART_COUNT; part++) {
            matrices.push();
            applyPartTransform(matrices, player, tickDelta, part);
            Matrix4f cur = new Matrix4f(matrices.peek().getPositionMatrix());

            if (prevMatrix == null) {
                prevMatrix = new Matrix4f(cur);
            }

            float vScale = 0.96f / PART_COUNT;
            float vTop = 0.03125f + vScale * part;
            float vBot = 0.03125f + vScale * (part + 1);

            if (part == 0) {
                addTopFace(consumer, cur, prevMatrix, light, vTop);
            }
            if (part == PART_COUNT - 1) {
                addBottomFace(consumer, cur, cur, light, vBot);
            }

            addBackFace(consumer, cur, prevMatrix, light, vTop, vBot);
            addFrontFace(consumer, prevMatrix, cur, light, vTop, vBot);
            addLeftFace(consumer, cur, prevMatrix, light, vTop, vBot);
            addRightFace(consumer, cur, prevMatrix, light, vTop, vBot);

            prevMatrix = new Matrix4f(cur);
            matrices.pop();
        }
    }

    private static void applyPartTransform(MatrixStack matrices, AbstractClientPlayerEntity player,
                                            float delta, int part) {
        if (WaveyCapesBase.config.capeMovement == CapeMovement.BASIC_SIMULATION) {
            applySimulation(matrices, player, delta, part);
        } else {
            applyVanilla(matrices, player, delta, part);
        }
    }

    private static void applySimulation(MatrixStack matrices, AbstractClientPlayerEntity player,
                                         float delta, int part) {
        StickSimulation sim = ((CapeHolder) player).getSimulation();
        boolean hasChestplate = !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty();
        double z1 = hasChestplate ? 0.15 : 0.125;
        matrices.translate(0.0, 0.0, z1);

        StickSimulation.Point base = sim.getPoints().get(0);
        float x = sim.getPoints().get(part).getLerpX(delta) - base.getLerpX(delta);
        if (x > 0.0f) x = 0.0f;
        float y = base.getLerpY(delta) - part - sim.getPoints().get(part).getLerpY(delta);
        float z = base.getLerpZ(delta) - sim.getPoints().get(part).getLerpZ(delta);

        float height = 0.0f;
        if (player.isSneaking()) {
            height += 25.0f;
            matrices.translate(0.0, 0.15, 0.0);
        }

        float naturalWind = getNaturalWindSwing(part, player.isSubmergedInWater());
        float partRotation = getPartRotation(delta, part, sim);

        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(6.0f + height + naturalWind));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(0.0f));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
        matrices.translate(-z / PART_COUNT, y / PART_COUNT, x / PART_COUNT);
        matrices.translate(0.0, 0.03, -0.03);
        matrices.translate(0.0, part * 1.0f / PART_COUNT, 0.0);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-partRotation));
        matrices.translate(0.0, -part * 1.0f / PART_COUNT, 0.0);
        matrices.translate(0.0, -0.03, 0.03);
    }

    private static void applyVanilla(MatrixStack matrices, AbstractClientPlayerEntity player,
                                      float h, int part) {
        matrices.translate(0.0, 0.0, 0.125);

        
        double d = MathHelper.lerp(h, player.prevCapeX, player.capeX) - MathHelper.lerp(h, player.prevX, player.getX());
        double e = MathHelper.lerp(h, player.prevCapeY, player.capeY) - MathHelper.lerp(h, player.prevY, player.getY());
        double m = MathHelper.lerp(h, player.prevCapeZ, player.capeZ) - MathHelper.lerp(h, player.prevZ, player.getZ());

        float n = player.prevBodyYaw + (player.bodyYaw - player.prevBodyYaw);
        double o = MathHelper.sin(n * 0.017453292f);
        double p = -MathHelper.cos(n * 0.017453292f);

        float height = (float) e * 10.0f;
        height = MathHelper.clamp(height, -6.0f, 32.0f);

        float swing = (float) (d * o + m * p) * easeOutSine(1.0f / PART_COUNT * part) * 100.0f;
        swing = MathHelper.clamp(swing, 0.0f, 150.0f * easeOutSine(1.0f / PART_COUNT * part));

        float sideways = (float) (d * p - m * o) * 100.0f;
        sideways = MathHelper.clamp(sideways, -20.0f, 20.0f);

        
        float strideProgress = MathHelper.lerp(h, player.prevStrideDistance, player.strideDistance);
        height += MathHelper.sin(strideProgress * 6.0f) * 32.0f * 0.5f;

        if (player.isSneaking()) {
            height += 25.0f;
            matrices.translate(0.0, 0.15, 0.0);
        }

        float naturalWind = getNaturalWindSwing(part, player.isSubmergedInWater());
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(6.0f + swing / 2.0f + height + naturalWind));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(sideways / 2.0f));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - sideways / 2.0f));
    }

    private static float getPartRotation(float delta, int part, StickSimulation sim) {
        if (part == PART_COUNT - 1) return getPartRotation(delta, part - 1, sim);
        Vector3 a = sim.points.get(part).getLerpedPos(delta);
        Vector3 b = sim.points.get(part + 1).getLerpedPos(delta);
        Vector3 angle = b.subtract(a);
        return (float) (Math.toDegrees(Math.atan2(angle.x, angle.y)) + 180.0);
    }

    private static float getNaturalWindSwing(int part, boolean underwater) {
        long tick = System.currentTimeMillis() / (underwater ? 9 : 3) % 360L;
        float rel = (part + 1) / (float) PART_COUNT;
        if (WaveyCapesBase.config.windMode == WindMode.WAVES) {
            return (float) (Math.sin(Math.toRadians(rel * 360.0f - tick)) * 3.0);
        }
        return 0.0f;
    }

    private static float easeOutSine(float x) {
        return (float) Math.sin((x * Math.PI) / 2f);
    }

    // UV константы
    private static final float BACK_U0 = .015625f, BACK_U1 = .171875f;
    private static final float FRONT_U0 = .1875f, FRONT_U1 = .34375f;
    private static final float LEFT_U0 = 0f, LEFT_U1 = .015625f;
    private static final float RIGHT_U0 = .171875f, RIGHT_U1 = .1875f;
    private static final float TOP_U0 = .015625f, TOP_U1 = .171875f;
    private static final float BOT_U0 = .171875f, BOT_U1 = .328125f;
    private static final float SIDE_V0 = .03125f, SIDE_V1 = .53125f;
    private static final float CAP_V0 = 0f, CAP_V1 = .03125f;

    private static void addBackFace(VertexConsumer buf, Matrix4f cur, Matrix4f prev, int light, float vTop, float vBot) {
        buf.vertex(prev, -0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(BACK_U1, vTop).overlay(0).light(light).normal(0,0,1);
        buf.vertex(prev,  0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(BACK_U0, vTop).overlay(0).light(light).normal(0,0,1);
        buf.vertex(cur,   0.3f, 0,  0f   ).color(1f,1f,1f,1f).texture(BACK_U0, vBot).overlay(0).light(light).normal(0,0,1);
        buf.vertex(cur,  -0.3f, 0,  0f   ).color(1f,1f,1f,1f).texture(BACK_U1, vBot).overlay(0).light(light).normal(0,0,1);
    }

    private static void addFrontFace(VertexConsumer buf, Matrix4f prev, Matrix4f cur, int light, float vTop, float vBot) {
        buf.vertex(prev, -0.3f, 0, 0f   ).color(1f,1f,1f,1f).texture(FRONT_U1, vTop).overlay(0).light(light).normal(0,0,-1);
        buf.vertex(prev,  0.3f, 0, 0f   ).color(1f,1f,1f,1f).texture(FRONT_U0, vTop).overlay(0).light(light).normal(0,0,-1);
        buf.vertex(cur,   0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(FRONT_U0, vBot).overlay(0).light(light).normal(0,0,-1);
        buf.vertex(cur,  -0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(FRONT_U1, vBot).overlay(0).light(light).normal(0,0,-1);
    }

    private static void addLeftFace(VertexConsumer buf, Matrix4f cur, Matrix4f prev, int light, float vTop, float vBot) {
        buf.vertex(cur,  -0.3f, 0, 0f   ).color(1f,1f,1f,1f).texture(LEFT_U1, vBot).overlay(0).light(light).normal(-1,0,0);
        buf.vertex(cur,  -0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(LEFT_U0, vBot).overlay(0).light(light).normal(-1,0,0);
        buf.vertex(prev, -0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(LEFT_U0, vTop).overlay(0).light(light).normal(-1,0,0);
        buf.vertex(prev, -0.3f, 0, 0f   ).color(1f,1f,1f,1f).texture(LEFT_U1, vTop).overlay(0).light(light).normal(-1,0,0);
    }

    private static void addRightFace(VertexConsumer buf, Matrix4f cur, Matrix4f prev, int light, float vTop, float vBot) {
        buf.vertex(cur,  0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(RIGHT_U0, vBot).overlay(0).light(light).normal(1,0,0);
        buf.vertex(cur,  0.3f, 0, 0f   ).color(1f,1f,1f,1f).texture(RIGHT_U1, vBot).overlay(0).light(light).normal(1,0,0);
        buf.vertex(prev, 0.3f, 0, 0f   ).color(1f,1f,1f,1f).texture(RIGHT_U1, vTop).overlay(0).light(light).normal(1,0,0);
        buf.vertex(prev, 0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(RIGHT_U0, vTop).overlay(0).light(light).normal(1,0,0);
    }

    private static void addTopFace(VertexConsumer buf, Matrix4f cur, Matrix4f prev, int light, float v) {
        buf.vertex(prev, -0.3f, 0, 0f   ).color(1f,1f,1f,1f).texture(TOP_U1, CAP_V1).overlay(0).light(light).normal(0,1,0);
        buf.vertex(prev,  0.3f, 0, 0f   ).color(1f,1f,1f,1f).texture(TOP_U0, CAP_V1).overlay(0).light(light).normal(0,1,0);
        buf.vertex(cur,   0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(TOP_U0, CAP_V0).overlay(0).light(light).normal(0,1,0);
        buf.vertex(cur,  -0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(TOP_U1, CAP_V0).overlay(0).light(light).normal(0,1,0);
    }

    private static void addBottomFace(VertexConsumer buf, Matrix4f cur, Matrix4f prev, int light, float v) {
        buf.vertex(prev, -0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(BOT_U1, CAP_V0).overlay(0).light(light).normal(0,-1,0);
        buf.vertex(prev,  0.3f, 0, -0.06f).color(1f,1f,1f,1f).texture(BOT_U0, CAP_V0).overlay(0).light(light).normal(0,-1,0);
        buf.vertex(cur,   0.3f, 0, 0f   ).color(1f,1f,1f,1f).texture(BOT_U0, CAP_V1).overlay(0).light(light).normal(0,-1,0);
        buf.vertex(cur,  -0.3f, 0, 0f   ).color(1f,1f,1f,1f).texture(BOT_U1, CAP_V1).overlay(0).light(light).normal(0,-1,0);
    }
}

