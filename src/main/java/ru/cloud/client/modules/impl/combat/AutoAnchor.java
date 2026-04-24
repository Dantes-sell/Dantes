package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.server.EventPacket;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(name = "AutoAnchor", category = Category.COMBAT, description = "Автоматически заряжает и взрывает якорь возрождения")
public final class AutoAnchor extends Module {

    public static final AutoAnchor INSTANCE = new AutoAnchor();
    private AutoAnchor() {}

    private final NumberSetting chargeDelay  = new NumberSetting("Задержка зарядки",  0, 0, 10, 1);
    private final NumberSetting protectDelay = new NumberSetting("Задержка защиты",   0, 0, 10, 1);
    private final NumberSetting breakDelay   = new NumberSetting("Задержка взрыва",   1, 0, 10, 1);

    // State machine
    private enum Stage { IDLE, CHARGE, PROTECT, EXPLODE }
    private Stage stage = Stage.IDLE;
    private BlockPos anchorPos = null;
    private boolean pendingAnchorPlace = false;
    private int tickCounter = 0;

    // ������ �� ��������� ��������� ����� �������
    private BlockPos lastExplodedPos = null;
    private long lastExplodedTime = 0;

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
        stage = Stage.IDLE;
        anchorPos = null;
        pendingAnchorPlace = false;
        tickCounter = 0;
    }

    // Перехватываем пакет размещения якоря игроком
    @EventTarget
    public void onPacket(EventPacket e) {
        if (!e.isSent()) return;
        if (mc.player == null || mc.world == null) return;

        if (e.getPacket() instanceof PlayerInteractBlockC2SPacket packet) {
            if (mc.player.getStackInHand(packet.getHand()).getItem() == Items.RESPAWN_ANCHOR) {
                BlockHitResult hit = packet.getBlockHitResult();
                anchorPos = hit.getBlockPos().offset(hit.getSide());
                pendingAnchorPlace = true;
            }
        }
    }

    @EventTarget
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        // Ждём пока якорь появится в мире после размещения
        if (pendingAnchorPlace && anchorPos != null) {
            if (mc.world.getBlockState(anchorPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                pendingAnchorPlace = false;
                startSequence(anchorPos);
                return;
            }
        }

        
        if (stage == Stage.IDLE) {
            if (mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hit.getBlockPos();
                if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    // ������ �� ���������� ������������ �� ��� �� �������
                    if (pos.equals(lastExplodedPos) && System.currentTimeMillis() - lastExplodedTime < 500) return;
                    startSequence(pos);
                }
            }
            return;
        }

        // Тиковый счётчик задержки
        if (tickCounter > 0) {
            tickCounter--;
            return;
        }

        switch (stage) {
            case CHARGE -> {
                chargeAnchor(anchorPos);
                int pd = (int) protectDelay.getCurrent();
                int bd = (int) breakDelay.getCurrent();
                if (pd == 0) {
                    attemptPlaceProtection(anchorPos);
                    if (bd == 0) {
                        explodeAnchor(anchorPos);
                        stage = Stage.IDLE;
                    } else {
                        tickCounter = bd;
                        stage = Stage.EXPLODE;
                    }
                } else {
                    tickCounter = pd;
                    stage = Stage.PROTECT;
                }
            }
            case PROTECT -> {
                attemptPlaceProtection(anchorPos);
                int bd = (int) breakDelay.getCurrent();
                if (bd == 0) {
                    explodeAnchor(anchorPos);
                    stage = Stage.IDLE;
                } else {
                    tickCounter = bd;
                    stage = Stage.EXPLODE;
                }
            }
            case EXPLODE -> {
                explodeAnchor(anchorPos);
                stage = Stage.IDLE;
            }
            default -> {}
        }
    }

    private void startSequence(BlockPos pos) {
        anchorPos = pos;
        int cd = (int) chargeDelay.getCurrent();
        if (cd == 0) {
            stage = Stage.CHARGE;
            tickCounter = 0;
        } else {
            tickCounter = cd;
            stage = Stage.CHARGE;
        }
    }

    // Заряжает якорь светокамнем
    private void chargeAnchor(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() != Blocks.RESPAWN_ANCHOR) return;
        if (mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) > 0) return;

        int slot = findSlot(Items.GLOWSTONE);
        if (slot == -1) return;

        int prev = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, createHitResult(pos));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = prev;
    }

    // Ставит защитный блок со стороны игрока
    private void attemptPlaceProtection(BlockPos anchorPos) {
        double dX = mc.player.getX() - (anchorPos.getX() + 0.5);
        double dZ = mc.player.getZ() - (anchorPos.getZ() + 0.5);
        Direction facing = Direction.getFacing(dX, 0, dZ);
        BlockPos protectPos = anchorPos.offset(facing);
        BlockPos playerPos = mc.player.getBlockPos();

        if (protectPos.equals(playerPos) || protectPos.equals(playerPos.up())) return;
        if (!mc.world.getBlockState(protectPos).isReplaceable()) return;

        int slot = findBlockSlot();
        if (slot == -1) return;

        int prev = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        Vec3d hitVec = Vec3d.ofCenter(anchorPos).add(
                facing.getOffsetX() * 0.5,
                facing.getOffsetY() * 0.5,
                facing.getOffsetZ() * 0.5
        );
        BlockHitResult hitResult = new BlockHitResult(hitVec, facing, anchorPos, false);

        boolean wasSneaking = mc.player.isSneaking();
        if (!wasSneaking) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        }
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (!wasSneaking) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }

        mc.player.getInventory().selectedSlot = prev;
    }

    // Взрывает якорь
    private void explodeAnchor(BlockPos pos) {
        if (mc.interactionManager == null) return;
        if (mc.world.getBlockState(pos).getBlock() != Blocks.RESPAWN_ANCHOR) return;
        if (mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) == 0) return;

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, createHitResult(pos));
        mc.player.swingHand(Hand.MAIN_HAND);

        lastExplodedPos = pos;
        lastExplodedTime = System.currentTimeMillis();
    }

    private int findBlockSlot() {
        int slot = findSlot(Items.GLOWSTONE);
        if (slot != -1) return slot;
        slot = findSlot(Items.OBSIDIAN);
        if (slot != -1) return slot;
        return findSlot(Items.CRYING_OBSIDIAN);
    }

    private int findSlot(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    private BlockHitResult createHitResult(BlockPos pos) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        Vec3d dir = eye.subtract(center);

        double ax = Math.abs(dir.x), ay = Math.abs(dir.y), az = Math.abs(dir.z);
        Direction side;
        if (ay > ax && ay > az) side = dir.y > 0 ? Direction.UP : Direction.DOWN;
        else if (ax > az)       side = dir.x > 0 ? Direction.EAST : Direction.WEST;
        else                    side = dir.z > 0 ? Direction.SOUTH : Direction.NORTH;

        Vec3d hitVec = center.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        return new BlockHitResult(hitVec, side, pos, false);
    }
}

