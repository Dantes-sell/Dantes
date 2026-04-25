package ru.cloud.client.modules.impl.player;

import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import com.darkmagician6.eventapi.EventTarget;
import ru.cloud.base.events.impl.player.EventJump;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(name = "NoDelay", category = Category.PLAYER, description = "Settings for NoDelay")
public final class NoDelay extends Module {
    public static final NoDelay INSTANCE = new NoDelay();

    public final BooleanSetting useItem = new BooleanSetting("Use Item", true);
    public final BooleanSetting jump    = new BooleanSetting("Jump",     false);

    public final NumberSetting jumpDelay = new NumberSetting("Jump Delay", 0, 0, 10, 1,
            () -> jump.isEnabled());

    private int jumpTimer = 0;

    private NoDelay() {}

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (useItem.isEnabled() && mc.options.useKey.isPressed()) {
            Hand activeHand = mc.player.getActiveHand();
            if (activeHand != null) {
                mc.interactionManager.interactItem(mc.player, activeHand);
            }
        }

        if (jump.isEnabled()) {
            if (jumpTimer <= 0) {
                if (mc.player.isOnGround() && mc.options.jumpKey.isPressed() && hasBlockAbove()) {
                    mc.player.jump();
                    jumpTimer = (int) jumpDelay.getCurrent();
                }
            } else {
                jumpTimer--;
            }
        }
    }

    private boolean hasBlockAbove() {
        // Проверяем блоки над головой игрока на высоту 2 блока (голова + 1 блок выше)
        BlockPos feet = mc.player.getBlockPos();
        for (int y = 1; y <= 2; y++) {
            if (!mc.world.getBlockState(feet.up(y)).isAir()) {
                return true;
            }
        }
        return false;
    }

    @EventTarget
    public void onJump(EventJump event) {
        if (jump.isEnabled()) {
            jumpTimer = 0;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        jumpTimer = 0;
    }
}

