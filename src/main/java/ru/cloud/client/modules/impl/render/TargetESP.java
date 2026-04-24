package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.joml.Quaternionf;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.impl.combat.Aura;
import ru.cloud.client.modules.impl.combat.AuraV2;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.render.display.base.color.ColorUtil;
import ru.cloud.utility.render.level.Render3DUtil;

@ModuleAnnotation(name = "TargetESP", description = "Highlights current target", category = Category.RENDER)
public class TargetESP extends Module {

    public static final TargetESP INSTANCE = new TargetESP();

    private final ModeSetting espMode = new ModeSetting("Mode", "Ring", "Ghosts", "Ghosts V2", "Marker", "Orbit", "Circle", "Crystals");
    private final BooleanSetting aimEsp = new BooleanSetting("Aim Assist ESP", false);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", false);
    private final BooleanSetting animateColors = new BooleanSetting("Animate Colors", true);

    private LivingEntity target;
    public LivingEntity lastTarget;
    private float animationProgress = 0f;

    // Ring mode
    private double kolcoStep = 0.0;
    private static final double KOLCO_SPEED = 0.25;
    private static final double ring2Speed = 0.25;
    private final Identifier glowTexture = Zenith.id("icons/glow.png");

    // Marker mode
    private float markerAngle = 0f;
    private float markerSpeed = 1f;
    private boolean markerFlip = false;
    private float prevMarkerAngle = 0f;

    // Ghosts mode
    private double ghostAge = 0.0;

    // Ghosts V2 mode
    private final java.util.Map<LivingEntity, java.util.List<Vec3d>> phantomTrail1 = new java.util.WeakHashMap<>();
    private final java.util.Map<LivingEntity, java.util.List<Vec3d>> phantomTrail2 = new java.util.WeakHashMap<>();
    private final java.util.Map<LivingEntity, java.util.List<Vec3d>> phantomTrail3 = new java.util.WeakHashMap<>();
    private int lastHurtTime = 0;
    private long lastHurtTimeMs = 0L;

    // Orbit mode
    private float orbitAngle = 0f;

    // Crystals mode
    private float crystalMoving = 0f;

    private final Identifier markerTex = Zenith.id("icons/marker.png");
    private final Identifier glowTex = Zenith.id("icons/glow.png");

    // -------------------------------------------------------------------------
    // Easing
    // -------------------------------------------------------------------------

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1f - x, 3.0);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        LivingEntity newTarget = Aura.INSTANCE.getTarget();
        if (newTarget == null && AuraV2.INSTANCE.isEnabled()) {
            newTarget = AuraV2.INSTANCE.getTarget();
        }
        if (aimEsp.isEnabled() && mc.targetedEntity instanceof LivingEntity living && living.isAlive()) {
            newTarget = living;
        }

        target = newTarget;
        if (target != null) lastTarget = target;

        float speed = 0.1f;
        if (target != null && target.isAlive())
            animationProgress = Math.min(animationProgress + speed, 1f);
        else
            animationProgress = Math.max(animationProgress - speed, 0f);

        // Advance per-mode timers
        kolcoStep += KOLCO_SPEED;

        prevMarkerAngle = markerAngle;
        markerAngle += markerSpeed;
        if (markerSpeed > 25f) markerFlip = true;
        if (markerSpeed < -25f) markerFlip = false;
        markerSpeed += markerFlip ? -0.5f : 0.5f;

        ghostAge++;
        orbitAngle += 2.5f;
        crystalMoving += 60f;
    }

    // -------------------------------------------------------------------------
    // Render dispatch
    // -------------------------------------------------------------------------

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (animationProgress <= 0f) return;
        LivingEntity entity = target != null ? target : lastTarget;
        if (entity == null) return;

        int modeIndex = espMode.getValues().indexOf(espMode.getValue());
        switch (modeIndex) {
            case 0 -> renderKolco(event, entity);
            case 1 -> renderGhosts(event, entity);
            case 2 -> renderGhostsV2(event, entity);
            case 3 -> renderMarker(event, entity);
            case 4 -> renderOrbit(event, entity);
            case 5 -> renderCircle(event, entity);
            case 6 -> renderCrystals(event, entity);
            default -> { }
        }
    }

    // -------------------------------------------------------------------------
    // Ring (пїЅпїЅпїЅпїЅпїЅпїЅ)
    // -------------------------------------------------------------------------

    private void renderKolco(EventRender3D event, LivingEntity entity) {
        float alpha = Math.max(this.animationProgress, easeOutCubic(animationProgress));
        if (alpha <= 0.0F) return;

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d renderPos = MathUtil.interpolate(entity).subtract(camera.getPos());
        float entityWidth = entity.getWidth() * 0.9f;
        float entityHeight = entity.getHeight();
        float animationAlpha = easeOutCubic(animationProgress);

        boolean canSee = mc.player != null && mc.player.canSee(entity);
        boolean useDepth = !throughWalls.isEnabled() && canSee;
        if (!throughWalls.isEnabled() && !canSee) return;

        MatrixStack matrices = event.getMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        if (useDepth) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        double currentStep = MathUtil.interpolate(kolcoStep - ring2Speed, kolcoStep);
        double golovkaY = MathUtil.absSinAnimation(currentStep) * entityHeight;
        double tailBaseY = MathUtil.absSinAnimation(currentStep - 0.4) * entityHeight;
        float golovkaSize = 0.12f;
        float tailSize = 0.08f;
        int totalPoints = 138;
        int tailSegments = 16;
        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < totalPoints; i++) {
            double angleRadians = 2 * Math.PI * i / totalPoints;
            float xOffset = (float) (Math.cos(angleRadians) * entityWidth);
            float zOffset = (float) (Math.sin(angleRadians) * entityWidth);

            int baseColor = getThemeColorAngle(i * (360 / totalPoints), currentTime);

            matrices.push();
            matrices.translate(renderPos.x + xOffset, renderPos.y + golovkaY, renderPos.z + zOffset);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            MatrixStack.Entry golovkaEntry = matrices.peek().copy();
            int coreColor = ColorUtil.multAlpha(baseColor, animationAlpha * 0.9f);
            Vector4i coreVec = new Vector4i(coreColor, coreColor, coreColor, coreColor);
            Render3DUtil.drawGlowTexture(golovkaEntry, glowTexture, -golovkaSize / 2, -golovkaSize / 2, golovkaSize, golovkaSize, coreVec, useDepth);
            matrices.pop();

            for (int t = 1; t <= tailSegments; t++) {
                float tailProgress = (float) t / (tailSegments + 1);
                double currentTailY = golovkaY + (tailBaseY - golovkaY) * tailProgress;
                float currentTailAlpha = animationAlpha * (1f - tailProgress) * 0.6f;

                matrices.push();
                matrices.translate(renderPos.x + xOffset, renderPos.y + currentTailY, renderPos.z + zOffset);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                MatrixStack.Entry tailEntry = matrices.peek().copy();
                int tailCoreColor = ColorUtil.multAlpha(baseColor, currentTailAlpha);
                Vector4i tailCoreVec = new Vector4i(tailCoreColor, tailCoreColor, tailCoreColor, tailCoreColor);
                Render3DUtil.drawGlowTexture(tailEntry, glowTexture, -tailSize / 2, -tailSize / 2, tailSize, tailSize, tailCoreVec, useDepth);
                matrices.pop();
            }
        }

        if (useDepth) RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private int getThemeColorAngle(int offsetAngle, long currentTime) {
        try {
            float timeFactor = (currentTime % 3000) / 3000.0f;
            int colorAngle = (int) (timeFactor * 360) + offsetAngle;
            return Zenith.getInstance().getThemeManager().getClientColor(colorAngle % 360).getRGB();
        } catch (Exception e) {
            return 0xFF42A5F5;
        }
    }

    // -------------------------------------------------------------------------
    // Ghosts (РџСЂРёР·СЂР°РєРё)
    // -------------------------------------------------------------------------

    private void renderGhosts(EventRender3D event, LivingEntity entity) {
        float easedAnim = easeOutCubic(animationProgress);
        if (easedAnim == 0f) return;

        boolean canSee = mc.player != null && mc.player.canSee(entity);
        if (!throughWalls.isEnabled() && !canSee) return;
        boolean useDepth = !throughWalls.isEnabled();

        int color = getThemeColor();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = MathUtil.interpolate(entity).subtract(camera.getPos());

        double iAge = MathUtil.interpolate(ghostAge - 1, ghostAge);
        float movingAngle  = (float) iAge * 3.2f;
        float globalAngle  = (float) Math.toRadians((float) iAge * -4.4f);

        float radius   = entity.getWidth() * 1f;
        float centerY  = entity.getHeight() / 2f;
        float amplitude = entity.getHeight() / 2.5f;

        int trailLength = 33;
        float baseSize  = 0.2f;
        float sizeStep  = 0.00515f;

        for (int j = 0; j < 4; j++) {
            for (int seg = 0; seg < trailLength; seg++) {
                float segAngle = movingAngle + (j * 90f) - seg;
                float rad = (float) Math.toRadians(segAngle);

                float px = (float) Math.sin(rad) * radius;
                float pz = (float) Math.cos(rad) * radius;
                float y  = centerY + (float) (Math.cos(Math.toRadians(segAngle * 2f)) * amplitude);

                float x = px * (float) Math.cos(globalAngle) - pz * (float) Math.sin(globalAngle);
                float z = px * (float) Math.sin(globalAngle) + pz * (float) Math.cos(globalAngle);

                float offset = (float) (trailLength - seg) / trailLength;
                float scale  = baseSize + sizeStep * (trailLength - seg);

                int fadeColor   = ColorUtil.fade((int) (offset * 255));
                int blended     = ColorUtil.blend(color, fadeColor, 0.7f);
                int ghostColor  = ColorUtil.multAlpha(blended, offset * easedAnim);
                Vector4i vColor = new Vector4i(ghostColor, ghostColor, ghostColor, ghostColor);

                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));
                matrices.translate(vec.x + x, vec.y + y, vec.z + z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                MatrixStack.Entry entry = matrices.peek().copy();
                Render3DUtil.drawGlowTexture(entry, glowTex, -scale / 2, -scale / 2, scale, scale, vColor, useDepth);
                Render3DUtil.drawGlowTexture(entry, glowTex, -scale / 2, -scale / 2, scale, scale, vColor, useDepth);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Ghosts V2 (РџСЂРёР·СЂР°РєРё V2)
    // -------------------------------------------------------------------------

    private void renderGhostsV2(EventRender3D event, LivingEntity entity) {
        float easedAnim = easeOutCubic(animationProgress);
        if (easedAnim == 0f) return;

        boolean canSee = mc.player != null && mc.player.canSee(entity);
        if (!throughWalls.isEnabled() && !canSee) return;
        boolean useDepth = !throughWalls.isEnabled();

        final float baseSizePx = 0.3f;
        final float radius = 0.7f;
        final int trailLength = 55;

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d camPos = camera.getPos();
        Vec3d entityPos = MathUtil.interpolate(entity);

        final double bx = entityPos.x;
        final double by = entityPos.y;
        final double bz = entityPos.z;

        // Hurt state
        boolean isHurt = entity.hurtTime > 0;
        float hurtSpeedBonus = 0f;
        if (isHurt) {
            hurtSpeedBonus = 0.2f;
            lastHurtTime = entity.hurtTime;
            lastHurtTimeMs = System.currentTimeMillis();
        } else if (lastHurtTime > 0) {
            long timeSinceHurt = System.currentTimeMillis() - lastHurtTimeMs;
            if (timeSinceHurt < 300) {
                hurtSpeedBonus = 0.2f * (1f - timeSinceHurt / 300f);
            } else {
                lastHurtTime = 0;
                hurtSpeedBonus = 0f;
            }
        }

        float currentSpeedMultiplier = 1.0f + hurtSpeedBonus;
        final double t = System.currentTimeMillis() / (350.0 / currentSpeedMultiplier);

        float hurtFactor = net.minecraft.util.math.MathHelper.clamp(entity.hurtTime / 10f, 0f, 1f);
        long timeSinceHurtMs = System.currentTimeMillis() - lastHurtTimeMs;
        if (timeSinceHurtMs < 200 && lastHurtTime > 0) {
            float afterHurtRed = 1f - (timeSinceHurtMs / 200f);
            hurtFactor = Math.max(hurtFactor, afterHurtRed);
        }

        final float hurtPC = (float) Math.sin(entity.hurtTime * (18f * Math.PI / 180f));

        final float[] baseHeights = {
            entity.getHeight() * 0.85f,
            entity.getHeight() * 0.55f,
            entity.getHeight() * 0.25f
        };
        final double[] angles = {
            Math.toRadians(0),
            Math.toRadians(120),
            Math.toRadians(240)
        };

        @SuppressWarnings("unchecked")
        java.util.List<Vec3d>[] trails = new java.util.List[]{
            phantomTrail1.computeIfAbsent(entity, k -> new java.util.ArrayList<>()),
            phantomTrail2.computeIfAbsent(entity, k -> new java.util.ArrayList<>()),
            phantomTrail3.computeIfAbsent(entity, k -> new java.util.ArrayList<>())
        };

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        if (useDepth) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        for (int k = 0; k < 3; k++) {
            float verticalAmp = 0.16f + (hurtFactor * 0.08f);
            double verticalMove = Math.sin(t * 1.5 + k * 2.0) * verticalAmp;
            double height = baseHeights[k] + verticalMove;

            float rotBonus = hurtSpeedBonus * 0.5f;
            double rotSpeed = t * (1.3f + rotBonus);
            double angle = angles[k] + rotSpeed;
            double baseX = Math.cos(angle) * radius;
            double baseZ = Math.sin(angle) * radius;

            Vec3d currentWorldPos = new Vec3d(bx + baseX, by + height, bz + baseZ);

            java.util.List<Vec3d> trail = trails[k];
            trail.add(0, currentWorldPos);
            while (trail.size() > trailLength) trail.remove(trail.size() - 1);

            for (int j = 0; j < trail.size(); j++) {
                Vec3d pos = trail.get(j);
                float progress = j / (float) trailLength;
                float alphaMul = (float) Math.pow(1.0f - progress, 1.5);
                float sizeFactor = 1.0f - progress * 0.5f;
                float dynSize = baseSizePx * sizeFactor;
                int dynAlpha = net.minecraft.util.math.MathHelper.clamp((int) (255 * easedAnim * alphaMul), 0, 255);

                int baseColor;
                if (hurtFactor > 0.05f) {
                    int r = 255;
                    int g = 80 + (int)(80 * (1 - hurtFactor));
                    int b = 80 + (int)(80 * (1 - hurtFactor));
                    baseColor = ColorUtil.makeColor(r, g, b, dynAlpha);
                } else {
                    baseColor = ColorUtil.fade(j * 3 + k * 40);
                    baseColor = ColorUtil.makeColor(ColorUtil.red(baseColor), ColorUtil.green(baseColor), ColorUtil.blue(baseColor), dynAlpha);
                }

                // overCol: lerp baseColor towards redOverlay by hurtPC*hurtFactor, then replace alpha
                int redOverlay = ColorUtil.makeColor(255, 80, 80, dynAlpha);
                float overFactor = net.minecraft.util.math.MathHelper.clamp(hurtPC * hurtFactor, 0f, 1f);
                int mixedColor = ColorUtil.blend(baseColor, redOverlay, overFactor);
                int color = ColorUtil.makeColor(ColorUtil.red(mixedColor), ColorUtil.green(mixedColor), ColorUtil.blue(mixedColor), dynAlpha);

                // Billboard at trail position
                double rx = pos.x - camPos.x;
                double ry = pos.y - camPos.y;
                double rz = pos.z - camPos.z;

                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));
                matrices.translate(rx, ry, rz);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                MatrixStack.Entry entry = matrices.peek().copy();
                Vector4i vColor = new Vector4i(color, color, color, color);

                // Halo glow
                float glowSize = dynSize * (1.3f + hurtFactor * 0.3f);
                int haloAlpha = net.minecraft.util.math.MathHelper.clamp((int)(dynAlpha * (0.4f + hurtFactor * 0.2f)), 0, 255);
                int haloColor = ColorUtil.makeColor(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), haloAlpha);
                Vector4i haloVec = new Vector4i(haloColor, haloColor, haloColor, haloColor);
                Render3DUtil.drawGlowTexture(entry, glowTex, -glowSize / 2, -glowSize / 2, glowSize, glowSize, haloVec, useDepth);

                // Core dot
                Render3DUtil.drawGlowTexture(entry, glowTex, -dynSize / 2, -dynSize / 2, dynSize, dynSize, vColor, useDepth);
            }
        }

        if (useDepth) RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // -------------------------------------------------------------------------
    // Marker (РњР°СЂРєРµСЂ)
    // -------------------------------------------------------------------------

    private void renderMarker(EventRender3D event, LivingEntity entity) {
        float easedAnim = easeOutCubic(animationProgress);
        if (easedAnim == 0f) return;

        int color = getThemeColor();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = MathUtil.interpolate(entity).subtract(camera.getPos());

        float size = 1.2f;
        float rotation = MathUtil.interpolate(prevMarkerAngle, markerAngle);

        MatrixStack matrix = new MatrixStack();
        matrix.push();
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));
        matrix.translate(vec.x, vec.y + entity.getBoundingBox().getLengthY() / 2.0, vec.z);
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));

        MatrixStack.Entry entry = matrix.peek().copy();

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8)  & 0xFF;
        int b =  color        & 0xFF;
        int a = (int) (255 * 0.7f * easedAnim);

        Vector4i vColor = new Vector4i(
                ColorUtil.makeColor(r, g, b, a),
                ColorUtil.makeColor(Math.min(255, r + 30), g, b, a),
                ColorUtil.makeColor(r, Math.min(255, g + 30), b, a),
                ColorUtil.makeColor(r, g, Math.min(255, b + 30), a)
        );

        Render3DUtil.drawGlowTexture(entry, markerTex, -size / 2, -size / 2, size, size, vColor, false);
        Render3DUtil.drawGlowTexture(entry, markerTex, -size / 2, -size / 2, size, size, vColor, false);
        matrix.pop();
    }

    // -------------------------------------------------------------------------
    
    // -------------------------------------------------------------------------

    private void renderOrbit(EventRender3D event, LivingEntity entity) {
        float easedAnim = easeOutCubic(animationProgress);
        if (easedAnim == 0f) return;

        boolean canSee = mc.player != null && mc.player.canSee(entity);
        if (!throughWalls.isEnabled() && !canSee) return;
        boolean useDepth = !throughWalls.isEnabled();

        int color = getThemeColor();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = MathUtil.interpolate(entity).subtract(camera.getPos());

        float radius = entity.getWidth() * 0.85f;
        float centerY = entity.getHeight() / 2f;
        int dotCount = 6;
        float dotSize = 0.80f;

        for (int i = 0; i < dotCount; i++) {
            float angle = (float) Math.toRadians(orbitAngle + (360f / dotCount) * i);
            float px = (float) Math.cos(angle) * radius;
            float pz = (float) Math.sin(angle) * radius;

            // secondary vertical oscillation per dot
            float py = centerY + (float) Math.sin(Math.toRadians(orbitAngle * 2f + (360f / dotCount) * i)) * (entity.getHeight() * 0.2f);

            int dotAlpha = (int) (220 * easedAnim);
            int dotColor = ColorUtil.makeColor(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), dotAlpha);
            Vector4i vColor = new Vector4i(dotColor, dotColor, dotColor, dotColor);

            MatrixStack matrices = new MatrixStack();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));
            matrices.translate(vec.x + px, vec.y + py, vec.z + pz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            MatrixStack.Entry entry = matrices.peek().copy();
            Render3DUtil.drawGlowTexture(entry, glowTex, -dotSize / 2, -dotSize / 2, dotSize, dotSize, vColor, useDepth);
        }
    }

    private void renderCircle(EventRender3D event, LivingEntity entity) {
        float easedAnim = easeOutCubic(animationProgress);
        if (easedAnim == 0f) return;

        boolean canSee = mc.player != null && mc.player.canSee(entity);
        if (!throughWalls.isEnabled() && !canSee) return;
        boolean useDepth = !throughWalls.isEnabled();

        int color = getThemeColor();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = MathUtil.interpolate(entity).subtract(camera.getPos());

        float radius = entity.getWidth() * 0.85f;
        float centerY = entity.getHeight() / 2f;
        int dotCount = 100;
        float dotSize = 0.30f;

        for (int i = 0; i < dotCount; i++) {
            float angle = (float) Math.toRadians(orbitAngle + (360f / dotCount) * i);
            float px = (float) Math.cos(angle) * radius;
            float pz = (float) Math.sin(angle) * radius;

            // secondary vertical oscillation per dot
            float py = centerY + (float) Math.sin(Math.toRadians(orbitAngle * 2f + (360f / dotCount) * i)) * (entity.getHeight() * 0.2f);

            int dotAlpha = (int) (220 * easedAnim);
            int dotColor = ColorUtil.makeColor(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), dotAlpha);
            Vector4i vColor = new Vector4i(dotColor, dotColor, dotColor, dotColor);

            MatrixStack matrices = new MatrixStack();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));
            matrices.translate(vec.x + px, vec.y + py, vec.z + pz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            MatrixStack.Entry entry = matrices.peek().copy();
            Render3DUtil.drawGlowTexture(entry, glowTex, -dotSize / 2, -dotSize / 2, dotSize, dotSize, vColor, useDepth);
        }
    }

    // -------------------------------------------------------------------------
    // Crystals (пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ)
    // -------------------------------------------------------------------------

    private void renderCrystals(EventRender3D event, LivingEntity entity) {
        float easedAnim = easeOutCubic(animationProgress);
        if (easedAnim == 0f) return;

        boolean canSee = mc.player != null && mc.player.canSee(entity);
        if (!throughWalls.isEnabled() && !canSee) return;
        boolean useDepth = !throughWalls.isEnabled();

        int color = getThemeColor();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d entityPos = MathUtil.interpolate(entity);
        Vec3d cameraPos = camera.getPos();
        Vec3d base = entityPos.subtract(cameraPos); // entity feet, camera-relative

        float width = entity.getWidth() * 1.5f;
        float moving = MathUtil.interpolate(crystalMoving - 60f, crystalMoving);
        float val = 1.2f - 0.5f * easedAnim;

        int alpha = (int)(255 * easedAnim);
        int r = ColorUtil.red(color), g = ColorUtil.green(color), b = ColorUtil.blue(color);
        int faceColor = ColorUtil.makeColor(r, g, b, alpha);
        int darkColor = ColorUtil.makeColor((int)(r * 0.5f), (int)(g * 0.5f), (int)(b * 0.5f), alpha);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        if (useDepth) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        // --- Octahedron crystals ---
        // Use event matrix (world-space) as base, then push/translate/rotate per crystal
        // exactly like the original: prepareMatrices > translate(sin,yOff,cos) > multiply(rotation)
        MatrixStack ms = event.getMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        // push to entity-feet origin (same as prepareMatrices in original)
        ms.push();
        ms.translate(base.x, base.y, base.z);

        for (int i = 0; i < 360; i += 20) {
            double angleRad = Math.toRadians(i + moving * 0.3f);
            float sin = (float)(MathUtil.sin(angleRad) * width * val);
            float cos = (float)(MathUtil.cos(angleRad) * width * val);
            float yOff = 0.1f + entity.getHeight() * (float) Math.abs(MathUtil.sin(Math.toRadians(i)));

            // direction from crystal to entity center (local coords, base is entity feet)
            // crystal local pos: (sin, yOff, cos)
            // entity center local pos: (0, height/2, 0)
            Vector3f dir = new Vector3f(
                    -sin,
                    entity.getHeight() / 2f - yOff,
                    -cos
            ).normalize();
            Quaternionf rot = new Quaternionf().rotationTo(new Vector3f(0f, 1f, 0f), dir);

            ms.push();
            ms.translate(sin, yOff, cos);
            ms.multiply(rot);

            float s = 0.12f;
            MatrixStack.Entry entry = ms.peek();
            // top cap faces
            octaFace(builder, entry,  0,  s,  0,   s, 0,  0,   0, 0,  s,  faceColor, darkColor);
            octaFace(builder, entry,  0,  s,  0,   0, 0,  s,  -s, 0,  0,  faceColor, darkColor);
            octaFace(builder, entry,  0,  s,  0,  -s, 0,  0,   0, 0, -s,  faceColor, darkColor);
            octaFace(builder, entry,  0,  s,  0,   0, 0, -s,   s, 0,  0,  faceColor, darkColor);
            // bottom cap faces
            octaFace(builder, entry,  0, -s,  0,   0, 0,  s,   s, 0,  0,  darkColor, faceColor);
            octaFace(builder, entry,  0, -s,  0,  -s, 0,  0,   0, 0,  s,  darkColor, faceColor);
            octaFace(builder, entry,  0, -s,  0,   0, 0, -s,  -s, 0,  0,  darkColor, faceColor);
            octaFace(builder, entry,  0, -s,  0,   s, 0,  0,   0, 0, -s,  darkColor, faceColor);

            ms.pop();
        }

        ms.pop();
        BufferRenderer.drawWithGlobalProgram(builder.end());

        // --- Bloom glow sprites ---
        float bigSize = 0.9f;
        int glowAlpha = (int)(255 * easedAnim * 0.25f);
        int glowColor = ColorUtil.makeColor(r, g, b, glowAlpha);
        Vector4i vColor = new Vector4i(glowColor, glowColor, glowColor, glowColor);

        for (int i = 0; i < 360; i += 20) {
            double angleRad = Math.toRadians(i + moving * 0.3f);
            float sin = (float)(MathUtil.sin(angleRad) * width * val);
            float cos = (float)(MathUtil.cos(angleRad) * width * val);
            float yOff = 0.1f + entity.getHeight() * (float) Math.abs(MathUtil.sin(Math.toRadians(i)));

            // same billboard pattern as renderGhosts
            MatrixStack bms = new MatrixStack();
            bms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            bms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));
            bms.translate(base.x + sin, base.y + yOff, base.z + cos);
            bms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            bms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            Render3DUtil.drawGlowTexture(bms.peek().copy(), glowTex, -bigSize / 2f, -bigSize / 2f, bigSize, bigSize, vColor, useDepth);
        }

        if (useDepth) RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
    }

    private void octaFace(BufferBuilder buf, MatrixStack.Entry entry,
                           float x1, float y1, float z1,
                           float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           int c1, int c2) {
        buf.vertex(entry, x1, y1, z1).color(c1);
        buf.vertex(entry, x2, y2, z2).color(c2);
        buf.vertex(entry, x3, y3, z3).color(c2);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int getThemeColor() {
        try {
            int index = animateColors.isEnabled()
                    ? (int) ((System.currentTimeMillis() % 3000) / 3000.0f * 360)
                    : 0;
            return Zenith.getInstance().getThemeManager().getClientColor(index).getRGB();
        } catch (Exception e) {
            return 0xFF42A5F5;
        }
    }
}

