package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.render.level.Render3DUtil;

import java.awt.*;

@ModuleAnnotation(name = "CrystalDmgEsp", category = Category.RENDER, description = "Показывает дамаг от кристалла")
public final class CrystalDamageEsp extends Module {
    public static final CrystalDamageEsp INSTANCE = new CrystalDamageEsp();

    private final NumberSetting crystalRange = new NumberSetting("Crystal range", 12.0f, 4.0f, 24.0f, 1.0f);
    private final BooleanSetting includeFriends = new BooleanSetting("Include friends", false);
    private final BooleanSetting includeSelf = new BooleanSetting("Include self", false);

    private CrystalDamageEsp() {
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!shouldRender(player)) {
                continue;
            }

            double damage = estimateDamage(player);
            renderEntityBox(player, getDamageColor(damage));
        }
    }

    private boolean shouldRender(PlayerEntity player) {
        if (player == null || !player.isAlive()) {
            return false;
        }
        if (player == mc.player) {
            return includeSelf.isEnabled();
        }
        return includeFriends.isEnabled() || !Zenith.getInstance().getFriendManager().isFriend(player.getGameProfile().getName());
    }

    private double estimateDamage(PlayerEntity player) {
        double best = 0.0;
        double maxRange = crystalRange.getCurrent();

        Box searchBox = player.getBoundingBox().expand(maxRange);
        for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, searchBox, EndCrystalEntity::isAlive)) {
            double distance = player.distanceTo(crystal);
            if (distance > maxRange) {
                continue;
            }

            double rawDamage = Math.max(0.0, 12.0 - distance * 2.2);
            rawDamage *= Math.max(0.25, 1.0 - player.getArmor() / 40.0);
            if (rawDamage > best) {
                best = rawDamage;
            }
        }

        return best;
    }

    private int getDamageColor(double damage) {
        if (damage >= 8.0) {
            return rgba(255, 70, 70, 190);
        }
        if (damage >= 5.0) {
            return rgba(255, 145, 60, 190);
        }
        if (damage >= 2.0) {
            return rgba(255, 220, 70, 190);
        }
        return rgba(80, 220, 120, 190);
    }

    private void renderEntityBox(PlayerEntity player, int color) {
        Render3DUtil.drawBox(
                player.getBoundingBox().offset(MathUtil.interpolate(player).subtract(player.getPos())),
                color,
                1.0f
        );
    }

    private int rgba(int r, int g, int b, int a) {
        return new Color(r, g, b, a).getRGB();
    }
}
