package ru.cloud.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;

import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

@ModuleAnnotation(name = "ObsidianFarm", category = Category.PLAYER, description = "Автоматически копает и сейвит обсидиан")
public final class ObsidianFarm extends Module {
    public static final ObsidianFarm INSTANCE = new ObsidianFarm();

    private static final double MAX_RANGE = 5.0;
    private static final double MAX_RANGE_SQ = MAX_RANGE * MAX_RANGE;

    private BlockPos targetPos;

    private ObsidianFarm() {
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        updateFarm();
    }

    private void updateFarm() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            targetPos = null;
            return;
        }

        boolean veinMiner = isVeinMinerActive();

        if (targetPos != null && (!isObsidian(targetPos) || !isInRange(targetPos) || !hasBlockBelow(targetPos, veinMiner))) {
            targetPos = null;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos from = playerPos.add(-5, -5, -5);
        BlockPos to = playerPos.add(5, 5, 5);
        List<BlockPos> blocks = StreamSupport.stream(BlockPos.iterate(from, to).spliterator(), false).toList();

        BlockPos newTargetPos;
        if (veinMiner) {
            int playerY = playerPos.getY();

            newTargetPos = blocks.stream()
                    .filter(this::isObsidian)
                    .filter(this::isInRange)
                    .filter(pos -> hasBlockBelow(pos, true))
                    .filter(pos -> pos.getY() > playerY)
                    .filter(pos -> countObsidianIn3x3(pos) >= 2)
                    .max(Comparator.comparingInt(BlockPos::getY)
                            .thenComparing((BlockPos pos) -> -getDistanceToCenterOf3x3Area(pos)))
                    .orElse(null);

            if (newTargetPos == null) {
                newTargetPos = blocks.stream()
                        .filter(this::isObsidian)
                        .filter(this::isInRange)
                        .filter(pos -> hasBlockBelow(pos, true))
                        .filter(pos -> pos.getY() > playerY)
                        .max(Comparator.comparingInt(BlockPos::getY)
                                .thenComparing(pos -> -mc.player.squaredDistanceTo(Vec3d.ofCenter(pos))))
                        .orElse(null);
            }

            if (newTargetPos == null) {
                newTargetPos = blocks.stream()
                        .filter(this::isObsidian)
                        .filter(this::isInRange)
                        .filter(pos -> hasBlockBelow(pos, true))
                        .filter(pos -> pos.getY() == playerY)
                        .filter(pos -> countObsidianIn3x3(pos) >= 2)
                        .max(Comparator.comparingDouble(pos -> -getDistanceToCenterOf3x3Area(pos)))
                        .orElse(null);
            }

            if (newTargetPos == null) {
                newTargetPos = blocks.stream()
                        .filter(this::isObsidian)
                        .filter(this::isInRange)
                        .filter(pos -> hasBlockBelow(pos, true))
                        .filter(pos -> pos.getY() == playerY)
                        .min(Comparator.comparingDouble(pos -> mc.player.squaredDistanceTo(Vec3d.ofCenter(pos))))
                        .orElse(null);
            }

            if (newTargetPos == null) {
                newTargetPos = blocks.stream()
                        .filter(this::isObsidian)
                        .filter(this::isInRange)
                        .filter(pos -> hasBlockBelow(pos, true))
                        .filter(pos -> pos.getY() < playerY)
                        .filter(pos -> countObsidianIn3x3(pos) >= 2)
                        .max(Comparator.comparingInt(BlockPos::getY)
                                .thenComparing((BlockPos pos) -> -getDistanceToCenterOf3x3Area(pos)))
                        .orElse(null);
            }

            if (newTargetPos == null) {
                newTargetPos = blocks.stream()
                        .filter(this::isObsidian)
                        .filter(this::isInRange)
                        .filter(pos -> hasBlockBelow(pos, true))
                        .filter(pos -> pos.getY() < playerY)
                        .max(Comparator.comparingInt(BlockPos::getY)
                                .thenComparing(pos -> -mc.player.squaredDistanceTo(Vec3d.ofCenter(pos))))
                        .orElse(null);
            }
        } else {
            newTargetPos = blocks.stream()
                    .filter(this::isObsidian)
                    .filter(this::isInRange)
                    .filter(pos -> hasBlockBelow(pos, false))
                    .max(Comparator.comparingInt(BlockPos::getY)
                            .thenComparing(pos -> -mc.player.squaredDistanceTo(Vec3d.ofCenter(pos))))
                    .orElse(null);
        }

        if (newTargetPos != null && !newTargetPos.equals(targetPos)) {
            targetPos = newTargetPos;
        } else if (newTargetPos == null) {
            targetPos = null;
        } else {
            targetPos = newTargetPos;
        }

        if (targetPos != null && isObsidian(targetPos) && isInRange(targetPos) && hasBlockBelow(targetPos, veinMiner)) {
            if (mc.interactionManager.attackBlock(targetPos, Direction.UP)) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private boolean isInRange(BlockPos pos) {
        return mc.player != null && mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= MAX_RANGE_SQ;
    }

    private boolean isObsidian(BlockPos pos) {
        if (mc.world == null) {
            return false;
        }
        BlockState state = mc.world.getBlockState(pos);
        return state.isOf(Blocks.OBSIDIAN);
    }

    private boolean hasBlockBelow(BlockPos pos, boolean veinMiner) {
        return true;
    }

    private boolean isVeinMinerActive() {
        if (mc.player == null) {
            return false;
        }

        ItemStack mainHand = mc.player.getMainHandStack();
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof PickaxeItem)) {
            return false;
        }

        String itemName = mainHand.getName().getString().toLowerCase();
        return itemName.contains("бур");
    }

    private int countObsidianIn3x3(BlockPos center) {
        int count = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = center.add(x, 0, z);
                if (isObsidian(pos) && isInRange(pos) && hasBlockBelow(pos, true)) {
                    count++;
                }
            }
        }
        return count;
    }

    private double getDistanceToCenterOf3x3Area(BlockPos pos) {
        double sumX = 0;
        double sumZ = 0;
        int count = 0;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                if (isObsidian(checkPos) && isInRange(checkPos) && hasBlockBelow(checkPos, true)) {
                    sumX += checkPos.getX();
                    sumZ += checkPos.getZ();
                    count++;
                }
            }
        }

        if (count == 0) {
            return Double.MAX_VALUE;
        }

        double avgX = sumX / count;
        double avgZ = sumZ / count;
        double dx = pos.getX() - avgX;
        double dz = pos.getZ() - avgZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
