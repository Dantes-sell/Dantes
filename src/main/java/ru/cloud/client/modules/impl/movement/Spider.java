package ru.cloud.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import ru.cloud.base.events.impl.player.EventRotate;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.rotation.RotationTarget;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.player.PlayerIntersectionUtil;
import ru.cloud.utility.game.player.SimulatedPlayer;
import ru.cloud.utility.game.player.rotation.Rotation;
import ru.cloud.utility.game.player.rotation.RotationUtil;
import ru.cloud.utility.math.StopWatch;
import ru.cloud.Zenith;

import java.util.Random;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleAnnotation(name = "Spider", category = Category.MOVEMENT, description = "????????? ?????? Spider")
public final class Spider extends Module {

    public static final Spider INSTANCE = new Spider();
    private Spider() {}

    final ModeSetting mode = new ModeSetting("?????", "?????", "?????????", "???????");

    final NumberSetting delay = new NumberSetting("Задержка", 0.4f, 0.1f, 1.0f, 0.001f,
            () -> mode.is("���������"));

    final BooleanSetting holdShift = new BooleanSetting("???????? Shift", "???????? Shift", true, () -> mode.is("?????????"));

    final BooleanSetting silentUse = new BooleanSetting("????? ?????????????", "????? ?????????????", true, () -> mode.is("?????????"));

    final BooleanSetting holdSpace = new BooleanSetting("???????? ??????", "???????? ??????", false, () -> mode.is("?????????"));

    final StopWatch stopWatch = new StopWatch();
    final Random random = new Random();

    // Spooky lava bucket state
    java.util.Timer spookyTimer = new java.util.Timer();
    boolean canUseSpooky = true;
    long lastWallJumpMs = 0L;
    static final long WALL_JUMP_COOLDOWN_MS = 250L;

    // ��������� (water bucket) state
    boolean canUseWater = true;
    final StopWatch waterCooldown = new StopWatch();

    // ФанТайм state
    boolean canUseWater2 = true;
    final StopWatch waterCooldown2 = new StopWatch();

    @Override
    public void onDisable() {
        super.onDisable();
        spookyTimer.cancel();
        spookyTimer = new java.util.Timer();
        canUseSpooky = true;
        canUseWater = true;
        canUseWater2 = true;
        if (mc.options != null) {
            mc.options.sneakKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("ФанТайм")) {
            if (mc.options.jumpKey.isPressed()) return;
            Box playerBox = mc.player.getBoundingBox().expand(-1e-3);
            Box lowerBox = new Box(playerBox.minX, playerBox.minY, playerBox.minZ,
                    playerBox.maxX, playerBox.minY + 0.5, playerBox.maxZ);
            if (!stopWatch.every(400)) return;
            if (PlayerIntersectionUtil.isBox(lowerBox, this::hasCollision)) {
                Box upperBox = new Box(playerBox.minX - 0.3, playerBox.minY + 1, playerBox.minZ - 0.3,
                        playerBox.maxX, playerBox.maxY, playerBox.maxZ);
                if (PlayerIntersectionUtil.isBox(upperBox, this::hasCollision)) {
                    mc.player.setOnGround(true);
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.6, mc.player.getVelocity().z);
                } else {
                    mc.player.setOnGround(true);
                    mc.player.jump();
                }
            }
        }

        if (mode.is("���������")) {
            handleWaterBucket();
        }
    }

    @EventTarget
    public void onRotate(EventRotate e) {
        if (mc.player == null || mc.world == null) return;
        if (!mode.is("Блоки")) return;

        boolean offHand = mc.player.getOffHandStack().getItem() instanceof BlockItem;
        int slotId = findBlockSlot();
        BlockPos blockPos = findPlacePos();

        if ((offHand || slotId != -1) && !blockPos.equals(BlockPos.ORIGIN)) {
            ItemStack stack = offHand ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(slotId);
            Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
            Vec3d vec = blockPos.toCenterPos();
            Direction direction = Direction.getFacing(
                    vec.x - mc.player.getX(),
                    vec.y - mc.player.getY(),
                    vec.z - mc.player.getZ()
            );
            Rotation angle = RotationUtil.fromVec3d(
                    vec.subtract(new Vec3d(direction.getVector().getX(), direction.getVector().getY(), direction.getVector().getZ()).multiply(0.1))
                            .subtract(mc.player.getEyePos())
            );

            var rotManager = Zenith.getInstance().getRotationManager();
            var aimManager = rotManager.getAimManager();
            rotManager.setRotation(
                    new RotationTarget(angle, () -> aimManager.rotate(aimManager.getInstantSetup(), angle), aimManager.getInstantSetup()),
                    1, this
            );

            if (canPlace(stack)) {
                int prev = mc.player.getInventory().selectedSlot;
                if (!offHand) mc.player.getInventory().selectedSlot = slotId;
                mc.interactionManager.interactBlock(mc.player, hand,
                        new BlockHitResult(vec, direction.getOpposite(), blockPos, false));
                mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
                if (!offHand) mc.player.getInventory().selectedSlot = prev;
            }
        }
    }

    // ---- Water bucket (?????????) ----

    private void handleWaterBucket() {
        if (mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.45, mc.player.getVelocity().z);
            return;
        }

        if (!canUseWater && waterCooldown.getElapsedTime() >= getDelayMs()) {
            canUseWater = true;
        }

        int waterSlot = findHotbarItem(Items.WATER_BUCKET);

        if (waterSlot != -1 && mc.player.horizontalCollision) {
            climbWall(waterSlot);
        }

        if (!mc.player.horizontalCollision) {
            if (holdShift.isEnabled()) mc.options.sneakKey.setPressed(false);
            if (holdSpace.isEnabled()) mc.options.jumpKey.setPressed(false);
        }
    }

    private void climbWall(int waterSlot) {
        if (holdSpace.isEnabled()) {
            if (mc.player.isOnGround()) {
                long now = System.currentTimeMillis();
                if (now - lastWallJumpMs > WALL_JUMP_COOLDOWN_MS) {
                    mc.player.jump();
                    lastWallJumpMs = now;
                }
            }
            mc.options.jumpKey.setPressed(true);
        }

        if (!canUseWater) return;

        int clientSlot = mc.player.getInventory().selectedSlot;
        mc.player.setPitch(75.0f);

        if (silentUse.isEnabled() && waterSlot != clientSlot) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(waterSlot));
            mc.player.getInventory().selectedSlot = waterSlot;
        }

        PlayerIntersectionUtil.useItem(Hand.MAIN_HAND);
        mc.player.setVelocity(mc.player.getVelocity().x, 0.43, mc.player.getVelocity().z);

        if (silentUse.isEnabled() && waterSlot != clientSlot) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(clientSlot));
            mc.player.getInventory().selectedSlot = clientSlot;
        }

        canUseWater = false;
        waterCooldown.reset();

        if (holdShift.isEnabled()) mc.options.sneakKey.setPressed(true);
    }

    private long getDelayMs() {
        return (long) (delay.getCurrent() * 1000f);
    }

    // ---- Blocks mode helpers ----

    private boolean canPlace(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        BlockPos blockPos = getStandingBlockPos();
        if (blockPos.getY() >= mc.player.getBlockY()) return false;
        VoxelShape shape = blockItem.getBlock().getDefaultState().getCollisionShape(mc.world, blockPos);
        if (shape.isEmpty()) return false;
        Box box = shape.getBoundingBox().offset(blockPos);
        return !box.intersects(mc.player.getBoundingBox())
                && box.intersects(SimulatedPlayer.simulateLocalPlayer(4).boundingBox);
    }

    private BlockPos findPlacePos() {
        BlockPos blockPos = getStandingBlockPos();
        if (mc.world.getBlockState(blockPos).isSolid()) return BlockPos.ORIGIN;
        return Stream.of(blockPos.west(), blockPos.east(), blockPos.south(), blockPos.north())
                .filter(pos -> mc.world.getBlockState(pos).isSolid())
                .findFirst()
                .orElse(BlockPos.ORIGIN);
    }

    private BlockPos getStandingBlockPos() {
        return BlockPos.ofFloored(SimulatedPlayer.simulateLocalPlayer(1).pos.add(0, -1e-3, 0));
    }

    private boolean hasCollision(BlockPos pos) {
        return !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    private int findHotbarItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }
}

















