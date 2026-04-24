package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.util.math.Vec3d;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.player.EventMove;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.utility.game.player.MovingUtil;

@ModuleAnnotation(
        name = "DragonFly",
        category = Category.MOVEMENT,
        description = "\u0423\u0441\u043a\u043e\u0440\u044f\u0435\u0442 \u0432\u0430\u0448 \u043f\u043e\u043b\u0451\u0442"
)
public final class DragonFly extends Module {
    public static final DragonFly INSTANCE = new DragonFly();

    private DragonFly() {
    }

    @EventTarget
    public void onMove(EventMove event) {
        if (mc.player == null || !mc.player.getAbilities().flying) {
            return;
        }

        boolean cakeWorld = Zenith.getInstance().getServerHandler().getServer().equalsIgnoreCase("CakeWorld");
        Vec3d move = event.getMovePos();
        double y = move.y;

        if (!mc.player.isSneaking() && mc.options.jumpKey.isPressed()) {
            y = MovingUtil.hasPlayerMovement() ? (cakeWorld ? 1.2f : 0.49f) : (cakeWorld ? 1.2f : 1.191f);
        }

        if (mc.options.sneakKey.isPressed()) {
            y = MovingUtil.hasPlayerMovement() ? (cakeWorld ? -1.2f : -0.49f) : (cakeWorld ? -1.2f : -1.191f);
        }

        double speed = cakeWorld
                ? (mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed() ? 1.109399f : 1.111f)
                : (mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed() ? 1.095399f : 1.1725f);

        double[] direction = MovingUtil.calculateDirection(speed);
        event.setMovePos(new Vec3d(direction[0], y, direction[1]));
    }
}
