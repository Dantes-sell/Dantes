package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.other.EventSpawnEntity;
import ru.cloud.base.events.impl.player.EventAttack;
import ru.cloud.base.events.impl.player.EventJump;
import ru.cloud.base.events.impl.player.EventMove;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.base.events.impl.server.EventEntityStatus;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.render.display.Render2DUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(
        name = "CustomParticle",
        category = Category.RENDER,
        description = "Кастомные частицы"
)
public final class CustomParticle extends Module {

    public static final CustomParticle INSTANCE = new CustomParticle();

    private static final Random RND = new Random();

    
    private static final Identifier TEX_STAR   = Zenith.id("particles/star.png");
    private static final Identifier TEX_SNOW   = Zenith.id("particles/snow.png");
    private static final Identifier TEX_DOLLAR = Zenith.id("particles/dollar.png");
    private static final Identifier TEX_DANTES = Zenith.id("particles/dantes.png");

    
    private final ModeSetting texType  = new ModeSetting("Текстура", "Звезда", "Снег", "Dollar", "Dantes", "Микс");
    private final ModeSetting.Value texStar   = texType.getValues().get(0);
    private final ModeSetting.Value texSnow   = texType.getValues().get(1);
    private final ModeSetting.Value texDollar = texType.getValues().get(2);
    private final ModeSetting.Value texDantes = texType.getValues().get(3);
    private final ModeSetting.Value texMix    = texType.getValues().get(4);

    
    private final BooleanSetting rainbow    = new BooleanSetting("Радуга", false);
    private final BooleanSetting themeColor = new BooleanSetting("Цвет темы", true);

    
    private final BooleanSetting walkEnabled = new BooleanSetting("Ходьба", true);
    private final NumberSetting  walkCount   = new NumberSetting("Кол-во (Ходьба)", 3, 1, 20, 1, walkEnabled::isEnabled);
    private final NumberSetting  walkLife    = new NumberSetting("Время жизни (Ходьба)", 30, 10, 120, 5, walkEnabled::isEnabled);
    private final NumberSetting  walkSize    = new NumberSetting("Размер (Ходьба)", 0.14f, 0.03f, 0.6f, 0.01f, walkEnabled::isEnabled);
    private final NumberSetting  walkSpeed   = new NumberSetting("Скорость (Ходьба)", 0.06f, 0.01f, 0.3f, 0.01f, walkEnabled::isEnabled);
    private final NumberSetting  walkDelaySetting = new NumberSetting("Задержка (Ходьба)", 3, 1, 10, 1, walkEnabled::isEnabled);

    
    private final BooleanSetting hitEnabled = new BooleanSetting("Удар", true);
    private final NumberSetting  hitCount   = new NumberSetting("Кол-во (Удар)", 8, 1, 40, 1, hitEnabled::isEnabled);
    private final NumberSetting  hitLife    = new NumberSetting("Время жизни (Удар)", 25, 10, 100, 5, hitEnabled::isEnabled);
    private final NumberSetting  hitSize    = new NumberSetting("Размер (Удар)", 0.18f, 0.03f, 0.6f, 0.01f, hitEnabled::isEnabled);
    private final NumberSetting  hitSpeed   = new NumberSetting("Скорость (Удар)", 0.18f, 0.02f, 0.5f, 0.01f, hitEnabled::isEnabled);

    
    private final BooleanSetting throwEnabled = new BooleanSetting("Бросок", true);
    private final NumberSetting  throwCount   = new NumberSetting("Кол-во (Бросок)", 6, 1, 30, 1, throwEnabled::isEnabled);
    private final NumberSetting  throwLife    = new NumberSetting("Время жизни (Бросок)", 25, 10, 100, 5, throwEnabled::isEnabled);
    private final NumberSetting  throwSize    = new NumberSetting("Размер (Бросок)", 0.15f, 0.03f, 0.5f, 0.01f, throwEnabled::isEnabled);
    private final NumberSetting  throwSpeed   = new NumberSetting("Скорость (Бросок)", 0.12f, 0.02f, 0.4f, 0.01f, throwEnabled::isEnabled);

    
    private final BooleanSetting jumpEnabled = new BooleanSetting("Прыжок", true);
    private final NumberSetting  jumpCount   = new NumberSetting("Кол-во (Прыжок)", 5, 1, 30, 1, jumpEnabled::isEnabled);
    private final NumberSetting  jumpLife    = new NumberSetting("Время жизни (Прыжок)", 30, 10, 120, 5, jumpEnabled::isEnabled);
    private final NumberSetting  jumpSize    = new NumberSetting("Размер (Прыжок)", 0.15f, 0.03f, 0.6f, 0.01f, jumpEnabled::isEnabled);
    private final NumberSetting  jumpSpeed   = new NumberSetting("Скорость (Прыжок)", 0.10f, 0.02f, 0.4f, 0.01f, jumpEnabled::isEnabled);

    
    private final BooleanSetting totemEnabled = new BooleanSetting("Тотем", true);
    private final NumberSetting  totemCount   = new NumberSetting("Кол-во (Тотем)", 30, 3, 100, 1, totemEnabled::isEnabled);
    private final NumberSetting  totemLife    = new NumberSetting("Время жизни (Тотем)", 60, 20, 200, 5, totemEnabled::isEnabled);
    private final NumberSetting  totemSize    = new NumberSetting("Размер (Тотем)", 0.22f, 0.05f, 0.7f, 0.01f, totemEnabled::isEnabled);
    private final NumberSetting  totemSpeed   = new NumberSetting("Скорость (Тотем)", 0.14f, 0.02f, 0.5f, 0.01f, totemEnabled::isEnabled);

    private static final byte TOTEM_STATUS = 35;

    
    private final List<Particle> particles = new ArrayList<>();
    private int walkDelayTick = 0;

    private CustomParticle() {}

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
        walkDelayTick = 0;
    }

    
    @EventTarget
    public void onMove(EventMove event) {
        if (!walkEnabled.isEnabled() || mc.player == null) return;
        if (!mc.player.isOnGround()) return;

        Vec3d mv = event.getMovePos();
        if (Math.sqrt(mv.x * mv.x + mv.z * mv.z) < 0.04) return;

        if (walkDelayTick-- > 0) return;
        walkDelayTick = (int) walkDelaySetting.getCurrent();

        float spd = walkSpeed.getCurrent();
        int count = (int) walkCount.getCurrent();
        int life  = (int) walkLife.getCurrent();
        Vec3d foot = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        for (int i = 0; i < count; i++) {
            Vec3d vel = new Vec3d(
                    rnd(-spd, spd),
                    rnd(spd * 0.3f, spd),
                    rnd(-spd, spd)
            );
            particles.add(new Particle(foot, vel, walkSize.getCurrent(), life + RND.nextInt(10), i, pickTexIndex()));
        }
    }

    
    @EventTarget
    public void onAttack(EventAttack event) {
        if (!hitEnabled.isEnabled() || event.getAction() != EventAttack.Action.PRE) return;
        if (mc.player == null) return;

        Entity t = event.getTarget();
        float spd = hitSpeed.getCurrent();
        int count = (int) hitCount.getCurrent();
        int life  = (int) hitLife.getCurrent();
        Vec3d pos = new Vec3d(t.getX(), t.getY() + t.getHeight() * 0.5, t.getZ());

        for (int i = 0; i < count; i++) {
            Vec3d vel = new Vec3d(
                    rnd(-spd, spd),
                    rnd(spd * 0.2f, spd * 1.2f),
                    rnd(-spd, spd)
            );
            particles.add(new Particle(pos, vel, hitSize.getCurrent(), life + RND.nextInt(15), i, pickTexIndex()));
        }
    }

    
    @EventTarget
    public void onJump(EventJump event) {
        if (!jumpEnabled.isEnabled() || mc.player == null) return;

        float spd = jumpSpeed.getCurrent();
        int count = (int) jumpCount.getCurrent();
        int life  = (int) jumpLife.getCurrent();
        Vec3d foot = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        for (int i = 0; i < count; i++) {
            Vec3d vel = new Vec3d(
                    rnd(-spd, spd),
                    rnd(-spd * 0.5f, spd * 0.3f), 
                    rnd(-spd, spd)
            );
            particles.add(new Particle(foot, vel, jumpSize.getCurrent(), life + RND.nextInt(10), i, pickTexIndex()));
        }
    }

    
    @EventTarget
    public void onSpawnEntity(EventSpawnEntity event) {
        if (!throwEnabled.isEnabled() || mc.player == null) return;
        Entity e = event.getEntity();
        if (!(e instanceof ThrownItemEntity) && !(e instanceof ItemEntity)) return;
        if (e.distanceTo(mc.player) > 3.0) return;

        float spd = throwSpeed.getCurrent();
        int count = (int) throwCount.getCurrent();
        int life  = (int) throwLife.getCurrent();
        Vec3d pos = new Vec3d(e.getX(), e.getY(), e.getZ());

        for (int i = 0; i < count; i++) {
            Vec3d vel = new Vec3d(
                    rnd(-spd, spd),
                    rnd(spd * 0.3f, spd),
                    rnd(-spd, spd)
            );
            particles.add(new Particle(pos, vel, throwSize.getCurrent(), life + RND.nextInt(10), i, pickTexIndex()));
        }
    }

    
    @EventTarget
    public void onEntityStatus(EventEntityStatus event) {
        if (!totemEnabled.isEnabled() || event.getStatus() != TOTEM_STATUS) return;

        Entity entity = event.getEntity();
        float spd = totemSpeed.getCurrent();
        int count = (int) totemCount.getCurrent();
        int life  = (int) totemLife.getCurrent();

        for (int i = 0; i < count; i++) {
            Vec3d pos = new Vec3d(
                    entity.getX(),
                    entity.getY() + MathUtil.random(0, entity.getHeight()),
                    entity.getZ()
            );
            Vec3d vel = new Vec3d(
                    rnd(-spd, spd),
                    rnd(-spd * 0.5f, spd * 0.8f),
                    rnd(-spd, spd)
            );
            particles.add(new Particle(pos, vel, totemSize.getCurrent(), life + RND.nextInt(20), i, pickTexIndex()));
        }
    }

    
    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (particles.isEmpty() || mc.player == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos  = camera.getPos();
        float pitch   = camera.getPitch();
        float yaw     = camera.getYaw();
        Identifier globalTex = getTexture(); 

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.isDead()) { it.remove(); continue; }

            float alpha = p.fade();
            if (alpha <= 0.005f) continue;

            int color = getColor(p, alpha);
            float s = p.size * p.scaleFactor();

            double dx = p.pos.x - camPos.x;
            double dy = p.pos.y - camPos.y;
            double dz = p.pos.z - camPos.z;

            Identifier tex = globalTex != null ? globalTex : getTexForParticle(p.texIndex);

            // billboard: align to camera (same order as WorldTweaks)
            MatrixStack matrices = new MatrixStack();
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180f));
            matrices.translate(dx, dy, dz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rotation));

            drawTexturedQuad(matrices, tex, -s * 0.5f, -s * 0.5f, s, s, color);
            matrices.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    
    private static double rnd(double min, double max) {
        return min + (max - min) * RND.nextDouble();
    }

    
    private int pickTexIndex() {
        if (texMix.isSelected())    return RND.nextInt(4);
        if (texSnow.isSelected())   return 1;
        if (texDollar.isSelected()) return 2;
        if (texDantes.isSelected()) return 3;
        return 0;
    }

    private Identifier getTexture() {
        if (texSnow.isSelected())   return TEX_SNOW;
        if (texDollar.isSelected()) return TEX_DOLLAR;
        if (texDantes.isSelected()) return TEX_DANTES;
        if (texMix.isSelected())    return null; // null = per-particle
        return TEX_STAR;
    }

    private Identifier getTexForParticle(int texIndex) {
        return switch (texIndex) {
            case 1  -> TEX_SNOW;
            case 2  -> TEX_DOLLAR;
            case 3  -> TEX_DANTES;
            default -> TEX_STAR;
        };
    }


    private int getColor(Particle p, float alpha) {
        int a = Math.min(255, (int)(alpha * 255));
        if (rainbow.isEnabled()) {
            Color rc = Render2DUtil.rainbow(25, p.seed * 15, 0.85f, 1f, 1f);
            return new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), a).getRGB();
        }
        if (themeColor.isEnabled()) {
            return Zenith.getInstance().getThemeManager()
                    .getClientColor((p.seed * 37) % 270)
                    .withAlpha(a).getRGB();
        }
        return new Color(255, 255, 255, a).getRGB();
    }

    private void drawTexturedQuad(MatrixStack matrices, Identifier tex,
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

    
    private static class Particle {
        Vec3d pos, vel;
        int life, maxLife, seed, texIndex;
        float size, rotation, rotSpeed;

        
        private static final int FADE_IN  = 8;
        private static final int FADE_OUT = 12;

        Particle(Vec3d pos, Vec3d vel, float size, int life, int seed, int texIndex) {
            this.pos      = pos;
            this.vel      = vel;
            this.size     = size;
            this.life     = life;
            this.maxLife  = life;
            this.seed     = seed;
            this.texIndex = texIndex;
            this.rotation = RND.nextFloat() * 360f;
            this.rotSpeed = (RND.nextFloat() - 0.5f) * 6f;
        }

        boolean isDead() { return life <= 0; }

        float fade() {
            int elapsed = maxLife - life;
            // fade-in
            if (elapsed < FADE_IN) return elapsed / (float) FADE_IN;
            // fade-out
            if (life < FADE_OUT)   return life / (float) FADE_OUT;
            return 1f;
        }

        float scaleFactor() {
            int elapsed = maxLife - life;
            if (elapsed < FADE_IN) return 0.5f + 0.5f * (elapsed / (float) FADE_IN);
            if (life < FADE_OUT)   return 0.5f + 0.5f * (life / (float) FADE_OUT);
            return 1f;
        }

        void update() {
            rotation += rotSpeed;
            
            vel = new Vec3d(
                    vel.x * 0.90,
                    vel.y * 0.90 - 0.004,
                    vel.z * 0.90
            );
            pos = pos.add(vel);
            life--;
        }
    }
}



