package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.render.EventRender2D;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.utility.math.ProjectionUtil;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ModuleAnnotation(name = "FireworkEsp", category = Category.RENDER, description = "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u043c\u0435\u0442\u043a\u0443 \u0442\u0430\u043c, \u0433\u0434\u0435 \u0431\u044b\u043b \u0437\u0430\u043f\u0443\u0449\u0435\u043d \u0444\u0435\u0439\u0435\u0440\u0432\u0435\u0440\u043a")
public final class FireworkEsp extends Module {
    public static final FireworkEsp INSTANCE = new FireworkEsp();

    private static final int MAX_FIREWORKS = 15;
    private static final float ITEM_RENDER_OFFSET = 8.0f;
    private static final long FIREWORK_FADE_TIME = 1400L;
    private static final ItemStack FIREWORK_STACK = Items.FIREWORK_ROCKET.getDefaultStack();

    private final Map<Integer, FireworkData> fireworks = new ConcurrentHashMap<>();

    private FireworkEsp() {
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        updateFireworks();
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        renderFireworks(event.getContext());
    }

    private void updateFireworks() {
        long currentTime = System.currentTimeMillis();
        fireworks.entrySet().removeIf(entry -> currentTime - entry.getValue().spawnTime > FIREWORK_FADE_TIME);

        if (fireworks.size() >= MAX_FIREWORKS) {
            return;
        }

        for (FireworkRocketEntity firework : mc.world.getEntitiesByClass(FireworkRocketEntity.class, mc.player.getBoundingBox().expand(128.0), entity -> true)) {
            fireworks.computeIfAbsent(firework.getId(), id -> new FireworkData(
                    firework.getX(),
                    firework.getY(),
                    firework.getZ(),
                    currentTime
            ));

            if (fireworks.size() >= MAX_FIREWORKS) {
                break;
            }
        }
    }

    private void renderFireworks(CustomDrawContext context) {
        long currentTime = System.currentTimeMillis();
        int rendered = 0;

        for (FireworkData data : fireworks.values()) {
            if (rendered >= MAX_FIREWORKS) {
                break;
            }

            long timeAlive = currentTime - data.spawnTime;
            if (timeAlive > FIREWORK_FADE_TIME) {
                continue;
            }

            Vec3d screenPos = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(data.x, data.y, data.z));
            if (screenPos.z <= 0.0 || screenPos.z >= 1.0) {
                continue;
            }

            float fade = 1.0f - (timeAlive / (float) FIREWORK_FADE_TIME);
            float x = (float) screenPos.x - ITEM_RENDER_OFFSET + 2.0f;
            float y = (float) screenPos.y - ITEM_RENDER_OFFSET + 2.0f;
            ColorRGBA accent = Zenith.getInstance().getThemeManager().getClientColor(0).withAlpha(255.0f * fade);

            context.drawRoundedRect(x, y, 13.0f, 13.0f, BorderRadius.all(5.0f), new ColorRGBA(25, 25, 28, Math.round(172.0f * fade)));
            context.drawRoundedBorder(x, y, 13.0f, 13.0f, 1.1f, BorderRadius.all(5.0f), accent);
            context.drawRoundedRect(x + 2.0f, y + 11.0f, 9.0f * fade, 1.0f, BorderRadius.all(1.0f), accent);

            context.pushMatrix();
            context.getMatrices().translate(screenPos.x, screenPos.y, 0.0f);
            context.getMatrices().scale(0.6f, 0.6f, 1.0f);
            context.getMatrices().translate(-screenPos.x, -screenPos.y, 0.0f);
            context.drawItem(FIREWORK_STACK, (int) screenPos.x - 8, (int) screenPos.y - 8);
            context.popMatrix();
            rendered++;
        }
    }

    private record FireworkData(double x, double y, double z, long spawnTime) {
    }
}


