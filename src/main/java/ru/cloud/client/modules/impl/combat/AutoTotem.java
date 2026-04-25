package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.server.EventEntityStatus;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.MultiBooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.player.PlayerInventoryComponent;
import ru.cloud.utility.game.player.PlayerInventoryUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@ModuleAnnotation(name = "AutoTotem", category = Category.COMBAT, description = "Settings for AutoTotem")
public final class AutoTotem extends Module {

    public static final AutoTotem INSTANCE = new AutoTotem();

    private final NumberSetting health = new NumberSetting("Здоровье", 5.0f, 1.0f, 20.0f, 0.5f);
    private final NumberSetting healthElytra = new NumberSetting("Здоровье с элитрой", 9.0f, 1.0f, 20.0f, 0.5f);
    private final NumberSetting healthNoArmor = new NumberSetting("Без полной брони", 8.0f, 1.0f, 20.0f, 0.5f);
    private final MultiBooleanSetting checks = new MultiBooleanSetting(
            "Проверки на",
            new MultiBooleanSetting.Value("Поглощение", true),
            new MultiBooleanSetting.Value("Кристаллы рядом", true),
            new MultiBooleanSetting.Value("Падение", true),
            new MultiBooleanSetting.Value("Игрок с кристаллом", true)
    );
    private final BooleanSetting swapBack = new BooleanSetting("Вернуть предмет", true);
    private final BooleanSetting noBallSwitch = new BooleanSetting("Не менять голову", false);
    private final BooleanSetting saveEnchanted = new BooleanSetting("Беречь зачарованные", true);

    private int nonEnchantedTotems;
    private int totemCount;
    private int previousOffhandSlot = -1;
    private boolean totemUsed;

    private AutoTotem() {
    }

    @Override
    public void onDisable() {
        previousOffhandSlot = -1;
        totemUsed = false;
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        totemCount = countTotems(true);
        nonEnchantedTotems = countTotems(false);

        int totemSlot = getTotemSlot();
        boolean offhandHasItem = !mc.player.getOffHandStack().isEmpty();

        if (shouldSwapToTotem()) {
            if (totemSlot != -1 && !isTotemInOffhand()) {
                if (offhandHasItem && previousOffhandSlot == -1) {
                    previousOffhandSlot = totemSlot;
                }
                swapSlotToOffhand(totemSlot);
            }
        } else if (swapBack.isEnabled() && previousOffhandSlot != -1 && isTotemInOffhand()) {
            swapSlotToOffhand(previousOffhandSlot);
            previousOffhandSlot = -1;
        }

        if (totemUsed && !shouldSwapToTotem() && !isTotemInOffhand()) {
            previousOffhandSlot = -1;
            totemUsed = false;
        }
    }

    @EventTarget
    public void onEntityStatus(EventEntityStatus event) {
        if (mc.player == null) {
            return;
        }

        if (event.getStatus() == 35 && event.getEntity() == mc.player) {
            totemUsed = true;
        }
    }

    private void swapSlotToOffhand(int slotId) {
        PlayerInventoryComponent.addTask(() -> {
            PlayerInventoryUtil.clickSlot(slotId, 40, SlotActionType.SWAP, false);
            PlayerInventoryUtil.closeScreen(true);
        });
    }

    private boolean shouldSwapToTotem() {
        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        float currentHealth = mc.player.getHealth();

        if (checks.isEnable("Поглощение")) {
            currentHealth += mc.player.getAbsorptionAmount();
        }

        boolean fullArmor = hasFullArmor();
        float healthThreshold = fullArmor ? health.getCurrent() : healthNoArmor.getCurrent();
        if (chest.isOf(Items.ELYTRA)) {
            healthThreshold = healthElytra.getCurrent();
        }

        return (!isOffhandBall() && isInDangerousSituation())
                || currentHealth <= healthThreshold
                || checkFall();
    }

    private boolean isInDangerousSituation() {
        return checkCrystalNear() || checkPlayerWithCrystalNearObsidian();
    }

    private boolean checkFall() {
        if (!checks.isEnable("Падение")) {
            return false;
        }
        if (mc.player.isTouchingWater() || mc.player.isGliding()) {
            return false;
        }

        float fallDistance = mc.player.fallDistance;
        float fallDamage = calculateFallDamage(fallDistance);
        return fallDamage >= mc.player.getHealth() / 1.92f;
    }

    private float calculateFallDamage(float fallDistance) {
        if (fallDistance <= 3.0f) {
            return 0.0f;
        }

        float fallDamage = (fallDistance - 3.0f) / 2.0f;

        if (hasProtectionAura()) {
            fallDamage *= 0.2f;
        }

        float absorption = checks.isEnable("Поглощение") ? mc.player.getAbsorptionAmount() : 0.0f;
        fallDamage = Math.max(0.0f, fallDamage - absorption);
        return Math.min(fallDamage, mc.player.getMaxHealth());
    }

    private boolean hasProtectionAura() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            if ("Аура Защиты".equals(stack.getName().getString())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkCrystalNear() {
        if (!checks.isEnable("Кристаллы рядом")) {
            return false;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) <= 6.0f) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPlayerWithCrystalNearObsidian() {
        if (!checks.isEnable("Игрок с кристаллом")) {
            return false;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity otherPlayer) || entity == mc.player) {
                continue;
            }
            if (mc.player.distanceTo(otherPlayer) > 5.0f) {
                continue;
            }

            ItemStack mainHand = otherPlayer.getMainHandStack();
            ItemStack offHand = otherPlayer.getOffHandStack();
            if (!mainHand.isOf(Items.END_CRYSTAL) && !offHand.isOf(Items.END_CRYSTAL)) {
                continue;
            }

            BlockPos obsidianPos = getNearestBlock(5.0f, Blocks.OBSIDIAN);
            if (obsidianPos != null && getDistanceToBlock(otherPlayer, obsidianPos) <= 5.0f) {
                return true;
            }
        }

        return false;
    }

    private boolean isOffhandBall() {
        return noBallSwitch.isEnabled() && mc.player.getOffHandStack().isOf(Items.PLAYER_HEAD);
    }

    private boolean hasFullArmor() {
        for (int i = 0; i < mc.player.getInventory().armor.size(); i++) {
            ItemStack armor = mc.player.getInventory().armor.get(i);
            if (!armor.isEmpty()) {
                continue;
            }
            if (i == 3 && hasJackHeadInInventory()) {
                continue;
            }
            return false;
        }
        return true;
    }

    private boolean hasJackHeadInInventory() {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.PLAYER_HEAD) && "Голова Джека".equals(stack.getName().getString())) {
                return true;
            }
        }
        return false;
    }

    private int getTotemSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isOf(Items.TOTEM_OF_UNDYING)) {
                continue;
            }
            if (shouldSaveEnchanted(stack)) {
                continue;
            }
            return toScreenSlot(i);
        }
        return -1;
    }

    private int countTotems(boolean includeEnchanted) {
        return (int) IntStream.range(0, mc.player.getInventory().size())
                .mapToObj(i -> mc.player.getInventory().getStack(i))
                .filter(stack -> stack.isOf(Items.TOTEM_OF_UNDYING) && (includeEnchanted || !stack.hasEnchantments()))
                .count();
    }

    private boolean isTotemInOffhand() {
        ItemStack offhand = mc.player.getOffHandStack();
        return offhand.isOf(Items.TOTEM_OF_UNDYING) && !shouldSaveEnchanted(offhand);
    }

    private boolean shouldSaveEnchanted(ItemStack stack) {
        return saveEnchanted.isEnabled() && stack.hasEnchantments() && nonEnchantedTotems > 0;
    }

    private BlockPos getNearestBlock(float distance, Block block) {
        return getSphere(getPlayerPos(), distance, 6, false, true, 0).stream()
                .filter(position -> mc.world.getBlockState(position).isOf(block))
                .min(Comparator.comparingDouble(pos -> getDistanceToBlock(mc.player, pos)))
                .orElse(null);
    }

    private List<BlockPos> getSphere(BlockPos center, float radius, int height, boolean hollow, boolean fromBottom, int yOffset) {
        List<BlockPos> positions = new ArrayList<>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        for (int x = centerX - (int) radius; x <= centerX + radius; ++x) {
            for (int z = centerZ - (int) radius; z <= centerZ + radius; ++z) {
                int yStart = fromBottom ? (centerY - (int) radius) : centerY;
                int yEnd = fromBottom ? (centerY + (int) radius) : (centerY + height);
                for (int y = yStart; y < yEnd; ++y) {
                    if (isPositionWithinSphere(centerX, centerY, centerZ, x, y, z, radius, hollow)) {
                        positions.add(new BlockPos(x, y + yOffset, z));
                    }
                }
            }
        }
        return positions;
    }

    private BlockPos getPlayerPos() {
        return new BlockPos(
                MathHelper.floor(mc.player.getX()),
                MathHelper.floor(mc.player.getY()),
                MathHelper.floor(mc.player.getZ())
        );
    }

    private double getDistanceToBlock(Entity entity, BlockPos blockPos) {
        return getDistance(entity.getX(), entity.getY(), entity.getZ(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    private double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean isPositionWithinSphere(int centerX, int centerY, int centerZ, int x, int y, int z, float radius, boolean hollow) {
        double distanceSq = Math.pow(centerX - x, 2.0) + Math.pow(centerZ - z, 2.0) + Math.pow(centerY - y, 2.0);
        return distanceSq < Math.pow(radius, 2.0) && (!hollow || distanceSq >= Math.pow(radius - 1.0f, 2.0));
    }

    private int toScreenSlot(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }
}

