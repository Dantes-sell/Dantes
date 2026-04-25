package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.render.EventFog;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ColorSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.MultiBooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.render.display.Render2DUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(name = "WorldTweaks", description = "Settings for WorldTweaks", category = Category.RENDER)
public final class WorldTweaks extends Module {

    // -- ???????????? ????????? ------------------------------------------------
    public final MultiBooleanSetting modeSetting =
            MultiBooleanSetting.create("Эффекты", List.of("Яркость", "Туман", "Время", "Частицы"));

    public final NumberSetting brightSetting =
            new NumberSetting("Яркость", 1.0F, 0.0F, 1.0F, 0.1f, () -> modeSetting.isEnable(0));

    private final ColorSetting colorFog =
            new ColorSetting("Цвет тумана", Zenith.getInstance().getThemeManager().getCurrentTheme().getColor());
    public final NumberSetting distanceSetting =
            new NumberSetting("Дальность тумана", 80, 10, 255, 5, () -> modeSetting.isEnable(1));

    public final NumberSetting timeSetting =
            new NumberSetting("Время мира", 12, 0, 24, 1, () -> modeSetting.isEnable(2));

    // -- ????????? ????????? ---------------------------------------------------
    private final ModeSetting particleType = new ModeSetting("Тип частиц", () -> modeSetting.isEnable(3));
    private final ModeSetting.Value typeStars   = new ModeSetting.Value(particleType, "Звёзды").select();
    private final ModeSetting.Value typeSnow    = new ModeSetting.Value(particleType, "Снег");
    private final ModeSetting.Value typeDollar  = new ModeSetting.Value(particleType, "Dollar");
    private final ModeSetting.Value typeMixed   = new ModeSetting.Value(particleType, "Микс");

    private final NumberSetting maxParticles =
            new NumberSetting("Количество", 60, 10, 200, 5, () -> modeSetting.isEnable(3));
    private final NumberSetting particleSize =
            new NumberSetting("Размер", 0.8f, 0.2f, 2.5f, 0.1f, () -> modeSetting.isEnable(3));
    private final NumberSetting lifeTime =
            new NumberSetting("Время жизни (мс)", 800f, 200f, 2000f, 50f, () -> modeSetting.isEnable(3));
    private final NumberSetting spawnRange =
            new NumberSetting("Радиус спавна", 25f, 5f, 80f, 5f, () -> modeSetting.isEnable(3));
    private final BooleanSetting particleRainbow =
            new BooleanSetting("Радуга", false, () -> modeSetting.isEnable(3));
    private final BooleanSetting particleTheme =
            new BooleanSetting("Цвет темы", true, () -> modeSetting.isEnable(3));

    public static final WorldTweaks INSTANCE = new WorldTweaks();

    // -- ???????? (??????????? ?? ????????) ------------------------------------
    private static final Identifier TEX_STAR   = Zenith.id("particles/star.png");
    private static final Identifier TEX_SNOW   = Zenith.id("particles/snow.png");
    private static final Identifier TEX_DOLLAR = Zenith.id("particles/dollar.png");

    // -- ??????? ---------------------------------------------------------------
    private final List<Particle> particles = new ArrayList<>();
    private final Random rnd = new Random();
    private int spawnDelay = 0;

    private WorldTweaks() {}

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
        spawnDelay = 0;
    }

    // -- Fog -------------------------------------------------------------------
    @EventTarget
    public void onFog(EventFog e) {
        if (modeSetting.isEnable(1)) {
            e.setDistance(distanceSetting.getCurrent());
            e.setColor(colorFog.getIntColor());
            e.setCancelled(true);
        }
    }

    // -- 3D render -------------------------------------------------------------
    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (!modeSetting.isEnable(3)) return;
        if (mc.player == null || mc.world == null) return;

        // spawn
        if (spawnDelay <= 0 && particles.size() < (int) maxParticles.getCurrent()) {
            spawnParticle();
            spawnDelay = 5 + rnd.nextInt(10);
        } else {
            spawnDelay--;
        }

        // update + remove dead
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.isDead()) it.remove();
        }

        // render
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        for (Particle p : particles) {
            if (!isVisible(camPos, p.pos)) continue;

            float alpha = p.fade();
            if (alpha <= 0.01f) continue;

            int color = getParticleColor(p, alpha);
            float size = particleSize.getCurrent() * p.scaleFactor() * 0.5f;

            Identifier tex = getTexture(p.type);

            double dx = p.pos.x - camPos.x;
            double dy = p.pos.y - camPos.y;
            double dz = p.pos.z - camPos.z;

            MatrixStack matrices = new MatrixStack();
            matrices.push();
            // billboard: align to camera
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));
            matrices.translate(dx, dy, dz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            // spin
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rotation));

            drawBillboardTexture(matrices, tex, -size / 2f, -size / 2f, size, size, color);
            matrices.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // -- Billboard texture draw ------------------------------------------------
    private void drawBillboardTexture(MatrixStack matrices, Identifier tex,
                                      float x, float y, float w, float h, int color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, tex);

        Matrix4f mat = matrices.peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(mat, x,     y,     0).texture(0f, 0f).color(color);
        buf.vertex(mat, x,     y + h, 0).texture(0f, 1f).color(color);
        buf.vertex(mat, x + w, y + h, 0).texture(1f, 1f).color(color);
        buf.vertex(mat, x + w, y,     0).texture(1f, 0f).color(color);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.setShaderTexture(0, 0);
    }

    // -- Spawn -----------------------------------------------------------------
    private void spawnParticle() {
        double ang  = rnd.nextDouble() * Math.PI * 2.0;
        double dist = rnd.nextDouble() * spawnRange.getCurrent();
        double x = mc.player.getX() + Math.cos(ang) * dist;
        double y = mc.player.getY() + 18.0 + rnd.nextDouble() * 6.0;
        double z = mc.player.getZ() + Math.sin(ang) * dist;

        double s = 0.02;
        Vec3d vel = new Vec3d(
                (rnd.nextDouble() - 0.5) * s * 0.5,
                0,
                (rnd.nextDouble() - 0.5) * s * 0.5);

        int life = Math.max(20, (int) lifeTime.getCurrent() + rnd.nextInt(50) - 25);
        particles.add(new Particle(new Vec3d(x, y, z), vel, life, pickType()));
    }

    // -- Helpers ---------------------------------------------------------------
    private static final int TYPE_STAR = 0, TYPE_SNOW = 1, TYPE_DOLLAR = 2;

    private int pickType() {
        if (typeStars.isSelected())  return TYPE_STAR;
        if (typeSnow.isSelected())   return TYPE_SNOW;
        if (typeDollar.isSelected()) return TYPE_DOLLAR;
        return rnd.nextInt(3); // mixed
    }

    private Identifier getTexture(int type) {
        return switch (type) {
            case TYPE_SNOW   -> TEX_SNOW;
            case TYPE_DOLLAR -> TEX_DOLLAR;
            default          -> TEX_STAR;
        };
    }

    private int getParticleColor(Particle p, float alpha) {
        int a = (int) (alpha * 255 * 0.85f);
        if (particleRainbow.isEnabled()) {
            Color rc = Render2DUtil.rainbow(25, p.colorSeed * 15, 0.85f, 1f, 1f);
            return new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), a).getRGB();
        }
        if (particleTheme.isEnabled()) {
            return Zenith.getInstance().getThemeManager()
                    .getClientColor((p.colorSeed * 37) % 270)
                    .withAlpha(a).getRGB();
        }
        return new Color(255, 255, 255, a).getRGB();
    }

    private boolean isVisible(Vec3d from, Vec3d to) {
        if (mc.world == null) return true;
        
        return from.distanceTo(to) < spawnRange.getCurrent() + 10;
    }

    // -- Particle --------------------------------------------------------------
    private class Particle {
        Vec3d pos, vel;
        int life, maxLife, type, colorSeed;
        boolean hitSurface = false;
        final int fadeOutTime = 30;
        float rotation, rotSpeed;

        Particle(Vec3d pos, Vec3d vel, int life, int type) {
            this.pos = pos;
            this.vel = vel;
            this.life = life;
            this.maxLife = life;
            this.type = type;
            this.colorSeed = rnd.nextInt(100);
            this.rotation = rnd.nextFloat() * 360f;
            this.rotSpeed = (rnd.nextFloat() - 0.5f) * 3f;
        }

        boolean isDead() { return life <= 0; }

        float fade() {
            if (hitSurface) return (float) life / fadeOutTime;
            return (float) life / maxLife;
        }

        float scaleFactor() {
            if (hitSurface) return (float) life / fadeOutTime;
            return 1f;
        }

        void update() {
            rotation += rotSpeed;
            if (hitSurface) { life--; vel = Vec3d.ZERO; return; }

            double drag = 0.98, gravity = -0.02 * (1 - drag);
            double jitter = 0.002;
            vel = new Vec3d(
                    vel.x * drag + (rnd.nextDouble() - 0.5) * jitter,
                    vel.y * drag + gravity + (rnd.nextDouble() - 0.5) * jitter * 0.3,
                    vel.z * drag + (rnd.nextDouble() - 0.5) * jitter);
            pos = pos.add(vel);

            BlockPos bp = BlockPos.ofFloored(pos.x, pos.y, pos.z);
            if (mc.world != null && !mc.world.isAir(bp)) {
                hitSurface = true;
                life = fadeOutTime;
                vel = new Vec3d(vel.x * 0.3, Math.abs(vel.y) * 0.5, vel.z * 0.3);
            } else {
                life--;
            }
        }
    }
}


