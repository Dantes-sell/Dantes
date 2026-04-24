package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import ru.cloud.base.events.impl.other.EventSpawnEntity;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.server.EventPacket;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(name = "AutoCrystal", category = Category.COMBAT, description = "Автоматически взрывает кристаллы энда")
public final class AutoCrystal extends Module {

    public static final AutoCrystal INSTANCE = new AutoCrystal();
    private AutoCrystal() {}

    private final NumberSetting breakDelay      = new NumberSetting("Задержка взрыва", 1, 0, 20, 1, "Взрыв кристала");
    private final BooleanSetting crystalOptimizer = new BooleanSetting("Оптимизация", "Быстрый взрыв при спавне кристалла", false);

    private BlockPos crystalPlacePos = null;
    private int waitTicks = 0;
    private int breakTickCounter = -1; // -1 = не активен
    private EndCrystalEntity pendingCrystal = null;

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
    }

    private void reset() {
        crystalPlacePos = null;
        waitTicks = 0;
        breakTickCounter = -1;
        pendingCrystal = null;
    }

    // Перехватываем пакет размещения кристалла
    @EventTarget
    public void onPacket(EventPacket e) {
        if (!e.isSent()) return;
        if (mc.player == null || mc.world == null) return;

        if (e.getPacket() instanceof PlayerInteractBlockC2SPacket packet) {
            if (mc.player.getStackInHand(packet.getHand()).getItem() == Items.END_CRYSTAL) {
                BlockHitResult hit = packet.getBlockHitResult();
                BlockPos target = hit.getBlockPos();
                Block block = mc.world.getBlockState(target).getBlock();
                if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
                    crystalPlacePos = target;
                    waitTicks = 20;
                    breakTickCounter = -1;
                    pendingCrystal = null;
                }
            }
        }
    }

    // Быстрый взрыв при спавне кристалла (Crystal Optimizer)
    @EventTarget
    public void onEntitySpawn(EventSpawnEntity e) {
        if (!crystalOptimizer.isEnabled()) return;
        if (crystalPlacePos == null) return;

        if (e.getEntity() instanceof EndCrystalEntity crystal) {
            if (crystal.getBlockPos().down().equals(crystalPlacePos)) {
                attackCrystal(crystal);
                crystalPlacePos = null;
                breakTickCounter = -1;
                pendingCrystal = null;
            }
        }
    }

    @EventTarget
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        // Тикаем счётчик задержки взрыва
        if (breakTickCounter > 0) {
            breakTickCounter--;
            return;
        }
        if (breakTickCounter == 0) {
            if (pendingCrystal != null && pendingCrystal.isAlive()) {
                attackCrystal(pendingCrystal);
            }
            crystalPlacePos = null;
            pendingCrystal = null;
            breakTickCounter = -1;
            return;
        }

        if (crystalPlacePos == null) return;

        if (waitTicks > 0) {
            waitTicks--;
            EndCrystalEntity crystal = findCrystal(crystalPlacePos);
            if (crystal != null) {
                waitTicks = 0;
                int delay = (int) breakDelay.getCurrent();
                if (delay == 0) {
                    attackCrystal(crystal);
                    crystalPlacePos = null;
                } else {
                    pendingCrystal = crystal;
                    breakTickCounter = delay;
                }
            }
        } else {
            // Время вышло, кристалл не появился
            crystalPlacePos = null;
        }
    }

    private EndCrystalEntity findCrystal(BlockPos basePos) {
        BlockPos expectedPos = basePos.up();
        Box searchBox = new Box(expectedPos);
        for (Entity entity : mc.world.getOtherEntities(null, searchBox)) {
            if (entity instanceof EndCrystalEntity crystal && entity.getBlockPos().down().equals(basePos)) {
                return crystal;
            }
        }
        return null;
    }

    private void attackCrystal(Entity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
