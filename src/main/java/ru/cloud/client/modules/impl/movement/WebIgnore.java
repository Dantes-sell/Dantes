package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(
        name = "WebIgnore",
        category = Category.MOVEMENT,
        description = "Позволяет ломать и ставить блоки за паутиной"
)
public final class WebIgnore extends Module {

    public static final WebIgnore INSTANCE = new WebIgnore();

    public final BooleanSetting placeBlocks = new BooleanSetting("Ставить блоки", true);

    private static final double RANGE = 4.0;

    private BlockPos targetBlockPos = null;
    private Direction targetFace = null;
    private Vec3d targetHitVec = null;

    private WebIgnore() {
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.options.attackKey.isPressed() || mc.options.useKey.isPressed()) {
            updateTargetBlock();
        }
    }

    private void updateTargetBlock() {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        Vec3d endPos = eyePos.add(lookVec.x * RANGE, lookVec.y * RANGE, lookVec.z * RANGE);

        BlockHitResult result = rayTraceSkipWebs(eyePos, endPos);
        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = result.getBlockPos();
            BlockState state = mc.world.getBlockState(hitPos);

            if (!state.isOf(Blocks.COBWEB) && !state.isAir()) {
                targetBlockPos = hitPos;
                targetFace = result.getSide();
                targetHitVec = result.getPos();
            }
        }
    }

    private BlockHitResult rayTraceSkipWebs(Vec3d start, Vec3d end) {
        Vec3d current = start;
        double step = 0.1;
        Vec3d direction = end.subtract(start).normalize();
        double totalDistance = start.distanceTo(end);
        double traveled = 0.0;

        while (traveled < totalDistance) {
            BlockPos blockPos = BlockPos.ofFloored(current);
            BlockState state = mc.world.getBlockState(blockPos);

            if (state.isOf(Blocks.COBWEB)) {
                current = current.add(direction.x * step, direction.y * step, direction.z * step);
                traveled += step;
                continue;
            }

            if (!state.isAir()) {
                Direction face = getBlockFace(current, blockPos);
                return new BlockHitResult(current, face, blockPos, false);
            }

            current = current.add(direction.x * step, direction.y * step, direction.z * step);
            traveled += step;
        }

        return null;
    }

    private Direction getBlockFace(Vec3d hitVec, BlockPos blockPos) {
        double dx = hitVec.x - (blockPos.getX() + 0.5);
        double dy = hitVec.y - (blockPos.getY() + 0.5);
        double dz = hitVec.z - (blockPos.getZ() + 0.5);

        double absDx = Math.abs(dx);
        double absDy = Math.abs(dy);
        double absDz = Math.abs(dz);

        if (absDx > absDy && absDx > absDz) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else if (absDy > absDz) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    public BlockPos getTargetBlockPos() {
        return targetBlockPos;
    }

    public Direction getTargetFace() {
        return targetFace;
    }

    public Vec3d getTargetHitVec() {
        return targetHitVec;
    }

    public BlockHitResult getPlaceResult() {
        if (targetBlockPos == null || targetFace == null || targetHitVec == null) return null;
        return new BlockHitResult(targetHitVec, targetFace, targetBlockPos, false);
    }

    public boolean shouldOverride() {
        if (!isEnabled() || mc.player == null || mc.world == null) return false;

        HitResult result = mc.crosshairTarget;
        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockResult = (BlockHitResult) result;
            BlockState state = mc.world.getBlockState(blockResult.getBlockPos());
            return state.isOf(Blocks.COBWEB);
        }
        return false;
    }

    public boolean shouldOverridePlace() {
        return isEnabled() && placeBlocks.isEnabled() && shouldOverride();
    }
}
