package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.player.EventRotate;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.rotation.RotationTarget;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.other.InventoryUtil;
import ru.cloud.utility.game.player.rotation.Rotation;
import ru.cloud.utility.math.Timer;

import java.util.Comparator;

@ModuleAnnotation(name = "WebTrap", category = Category.COMBAT, description = "Places cobwebs into targets")
public final class WebTrap extends Module {

    public static final WebTrap INSTANCE = new WebTrap();

    private final NumberSetting range = new NumberSetting("Range", 4.0f, 2.0f, 6.0f, 0.1f);
    private final NumberSetting placeDelay = new NumberSetting("Delay", 250.0f, 0.0f, 1000.0f, 10.0f);
    private final BooleanSetting head = new BooleanSetting("Head", true);
    private final BooleanSetting feet = new BooleanSetting("Feet", true);
    private final BooleanSetting fromInventory = new BooleanSetting("FromInventory", true);
    private final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    private final BooleanSetting onlyStanding = new BooleanSetting("OnlyStanding", true);

    private final Timer timer = new Timer();

    private LivingEntity target;
    private BlockPos placePos;
    private Rotation targetRotation;

    private WebTrap() {
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            resetState();
            return;
        }

        if (findCobwebHotbarSlot() == -1 && (!fromInventory.isEnabled() || InventoryUtil.findInventory(Items.COBWEB) == -1)) {
            resetState();
            return;
        }

        target = resolveTarget();
        if (target == null) {
            resetState();
            return;
        }

        placePos = resolvePlacementPos(target);
        if (placePos == null) {
            targetRotation = null;
            return;
        }

        targetRotation = calculateRotation(placePos);

        if (!timer.finished((long) placeDelay.getCurrent())) {
            return;
        }

        placeCobweb(placePos);
    }

    @EventTarget
    public void onRotate(EventRotate event) {
        if (!rotate.isEnabled() || targetRotation == null) {
            return;
        }

        var rotationManager = Zenith.getInstance().getRotationManager();
        var aimManager = rotationManager.getAimManager();
        rotationManager.setRotation(
                new RotationTarget(
                        targetRotation,
                        () -> aimManager.rotate(aimManager.getInstantSetup(), targetRotation),
                        aimManager.getInstantSetup()
                ),
                4,
                this
        );
    }

    private void placeCobweb(BlockPos pos) {
        int previousSlot = mc.player.getInventory().selectedSlot;
        int hotbarSlot = findCobwebHotbarSlot();
        int inventorySlot = -1;
        boolean swapped = false;

        if (hotbarSlot == -1) {
            if (!fromInventory.isEnabled()) {
                return;
            }

            inventorySlot = InventoryUtil.findInventory(Items.COBWEB);
            if (inventorySlot == -1) {
                return;
            }

            hotbarSlot = previousSlot;
            InventoryUtil.swap(inventorySlot, hotbarSlot);
            swapped = true;
        }

        mc.player.getInventory().selectedSlot = hotbarSlot;

        BlockPos supportPos = pos.down();
        Vec3d hitVec = Vec3d.ofCenter(supportPos).add(0.0, 0.5, 0.0);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, supportPos, false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);

        mc.player.getInventory().selectedSlot = previousSlot;
        if (swapped) {
            InventoryUtil.swap(inventorySlot, hotbarSlot);
        }

        timer.reset();
    }

    private LivingEntity resolveTarget() {
        LivingEntity auraTarget = Aura.INSTANCE.isEnabled() ? Aura.INSTANCE.getTarget() : null;
        if (isValidTarget(auraTarget)) {
            return auraTarget;
        }

        LivingEntity auraV2Target = AuraV2.INSTANCE.isEnabled() ? AuraV2.INSTANCE.getTarget() : null;
        if (isValidTarget(auraV2Target)) {
            return auraV2Target;
        }

        return mc.world.getPlayers().stream()
                .filter(this::isValidTarget)
                .min(Comparator.comparingDouble(player -> player.squaredDistanceTo(mc.player)))
                .orElse(null);
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player) {
            return false;
        }
        if (!living.isAlive()) {
            return false;
        }
        if (!(living instanceof PlayerEntity)) {
            return false;
        }
        if (Zenith.getInstance().getFriendManager().isFriend(living.getName().getString())) {
            return false;
        }
        if (mc.player.squaredDistanceTo(living) > range.getCurrent() * range.getCurrent()) {
            return false;
        }
        if (onlyStanding.isEnabled()) {
            if (!living.isOnGround()) {
                return false;
            }
            if (Math.abs(living.getX() - living.prevX) > 0.05 || Math.abs(living.getZ() - living.prevZ) > 0.05) {
                return false;
            }
        }
        return true;
    }

    private BlockPos resolvePlacementPos(LivingEntity entity) {
        boolean feetWeb = isCobwebAt(entity, 0.0);
        boolean headWeb = isCobwebAt(entity, 1.0);

        BlockPos feetPos = BlockPos.ofFloored(entity.getPos());
        BlockPos headPos = feetPos.up();

        if (head.isEnabled() && feetWeb && !headWeb && canPlaceAt(headPos)) {
            return headPos;
        }

        if (feet.isEnabled() && !feetWeb && canPlaceAt(feetPos)) {
            return feetPos;
        }

        if (head.isEnabled() && !headWeb && canPlaceAt(headPos)) {
            return headPos;
        }

        return null;
    }

    private boolean isCobwebAt(LivingEntity entity, double yOffset) {
        Vec3d pos = entity.getPos().add(0.0, yOffset, 0.0);
        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z += 0.3) {
                BlockPos check = BlockPos.ofFloored(pos.x + x, pos.y, pos.z + z);
                if (mc.world.getBlockState(check).isOf(Blocks.COBWEB)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canPlaceAt(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isReplaceable() && !state.isAir()) {
            return false;
        }

        BlockState support = mc.world.getBlockState(pos.down());
        if (support.isAir() || !support.isSolidBlock(mc.world, pos.down())) {
            return false;
        }

        return mc.world.getOtherEntities(null, new Box(pos)).stream().allMatch(entity -> entity == target);
    }

    private Rotation calculateRotation(BlockPos pos) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return Rotation.lookingAt(hitVec, eyes).normalize(Zenith.getInstance().getRotationManager().getCurrentRotation());
    }

    private int findCobwebHotbarSlot() {
        return InventoryUtil.findHotbar(Items.COBWEB);
    }

    private void resetState() {
        target = null;
        placePos = null;
        targetRotation = null;
    }
}
