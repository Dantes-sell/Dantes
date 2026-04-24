package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.base.events.impl.server.EventPacket;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.ItemSelectSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.math.Timer;
import ru.cloud.utility.render.level.Render3DUtil;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ModuleAnnotation(name = "BlockESP", category = Category.RENDER, description = "Подсвечивает блоки")
public final class BlockESP extends Module {


    private volatile List<BlockPos> blockPos = new ArrayList<>();
    private volatile List<BlockPos> defaultModeCache = new ArrayList<>();

    private ModeSetting espMode = new ModeSetting("Режим");
    private ModeSetting.Value defaultMode = new ModeSetting.Value(espMode, "Дефолт").select();
    private ModeSetting.Value xrayMode = new ModeSetting.Value(espMode, "Xray");

    private final ItemSelectSetting itemSelectSetting = new ItemSelectSetting("Блоки", new ArrayList<>());

    private ModeSetting scanMode = new ModeSetting("Мод сканирования");
    private ModeSetting.Value onlyUpdate = new ModeSetting.Value(scanMode,"Обновление").select();
    private ModeSetting.Value all = new ModeSetting.Value(scanMode,"Сканировать");

    private final NumberSetting range = new NumberSetting("Радиус", 80, 1, 128, 2, () -> all.isSelected() && xrayMode.isSelected());
    private final NumberSetting time = new NumberSetting("Таймер", 4, 0, 100, 5, () -> all.isSelected() && xrayMode.isSelected());
    private final NumberSetting defaultUpdateTime = new NumberSetting("Обновление (сек)", 2, 0.5f, 10, 0.5f, defaultMode::isSelected);

    private final Timer timerUtil = new Timer();
    private final Timer defaultModeTimer = new Timer();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isStarted = true;

    public static BlockESP INSTANCE = new BlockESP();
    private BlockESP() {
        itemSelectSetting.add(Blocks.DIAMOND_ORE);
        itemSelectSetting.setVisible(xrayMode::isSelected);
        scanMode.setVisible(xrayMode::isSelected);
    }
    @Override
    public void onEnable() {
        timerUtil.setMillis(0);
        defaultModeTimer.setMillis(0);
        isStarted = true;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        isStarted = false;
        defaultModeCache.clear();
        super.onDisable();
    }

    @EventTarget
    public void render(EventRender3D eventRender3D) {
        if (xrayMode.isSelected()) {
            renderXrayMode();
        } else if (defaultMode.isSelected()) {
            renderDefaultMode();
        }
    }

    private void renderXrayMode() {
        if (timerUtil.finished(time.getCurrent() * 1000) && !onlyUpdate.isSelected() && isStarted) {
            executor.submit(this::scan);
            timerUtil.reset();
        }

        for (BlockPos pos : blockPos) {
            Block block = mc.world.getBlockState(pos).getBlock();

            if (itemSelectSetting.contains(block)) {
                if ((block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)) {
                    drawBox(pos, Color.cyan.getRGB());
                } else if ((block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE)) {
                    drawBox(pos, 0xFFFFD700);
                } else if (block == Blocks.NETHER_GOLD_ORE) {
                    drawBox(pos, 0xFFFFD700);
                } else if ((block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)) {
                    drawBox(pos, 0xFF00FF4D);
                } else if ((block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE)) {
                    drawBox(pos, 0xFFD5D5D5);
                } else if ((block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE)) {
                    drawBox(pos, 0xFFFF0000);
                } else if (block == Blocks.ANCIENT_DEBRIS) {
                    drawBox(pos, 0xFFFFFFFF);
                } else {
                    MapColor color = block.getDefaultMapColor();
                    int rgb = new Color(color.color).getRGB();
                    drawBox(pos, rgb);
                }
            }
        }
    }

    private void renderDefaultMode() {
        if (mc.world == null || mc.player == null) return;

        if (defaultModeTimer.finished((long)(defaultUpdateTime.getCurrent() * 1000))) {
            executor.submit(this::scanDefaultMode);
            defaultModeTimer.reset();
        }

        for (BlockPos pos : defaultModeCache) {
            BlockEntity blockEntity = mc.world.getBlockEntity(pos);

            if (blockEntity instanceof ChestBlockEntity) {
                drawBox(pos, Color.YELLOW.getRGB());
            } else if (blockEntity instanceof EnderChestBlockEntity) {
                drawBox(pos, Color.MAGENTA.getRGB());
            } else if (blockEntity instanceof MobSpawnerBlockEntity) {
                drawBox(pos, Color.RED.getRGB());
            } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
                drawBox(pos, Color.PINK.getRGB());
            } else if (blockEntity instanceof BedBlockEntity) {
                drawBox(pos, Color.WHITE.getRGB());
            } else if (blockEntity instanceof BarrelBlockEntity) {
                drawBox(pos, Color.ORANGE.getRGB());
            }
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ChestMinecartEntity) {
                drawBox(entity.getBlockPos(), Color.YELLOW.getRGB());
            }
        }
    }

    private void scanDefaultMode() {
        if (mc.world == null || mc.player == null) return;

        ArrayList<BlockPos> found = new ArrayList<>();
        int range = 50;
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    if (!isStarted) return;

                    BlockPos pos = playerPos.add(x, y, z);
                    BlockEntity blockEntity = mc.world.getBlockEntity(pos);

                    if (blockEntity instanceof ChestBlockEntity ||
                            blockEntity instanceof EnderChestBlockEntity ||
                            blockEntity instanceof MobSpawnerBlockEntity ||
                            blockEntity instanceof ShulkerBoxBlockEntity ||
                            blockEntity instanceof BedBlockEntity ||
                            blockEntity instanceof BarrelBlockEntity) {
                        found.add(pos);
                    }
                }
            }
        }

        defaultModeCache = found;
    }

    @EventTarget
    public void packer(EventPacket eventPacket) {
        if (eventPacket.isReceive()) {
            if (eventPacket.getPacket() instanceof ChunkDeltaUpdateS2CPacket chunkDelta)
                chunkDelta.visitUpdates((pos, state) -> {
                    if(itemSelectSetting.contains(state.getBlock())) {
                        blockPos.add(pos);
                    }
                });
            if (eventPacket.getPacket() instanceof BlockUpdateS2CPacket chunkDelta) {

                if (itemSelectSetting.contains(chunkDelta.getState ().getBlock())){
                    blockPos.add(chunkDelta.getPos());
                }
            }

        }
    }

    public void drawBox(BlockPos blockPos, int start) {
        Render3DUtil.drawBox(new Box(blockPos),start, 1);
    }

    private void scan() {

        ArrayList<BlockPos> blocks = new ArrayList<>();
        int startX = (int) Math.floor(mc.player.getX() - range.getCurrent());
        int endX = (int) Math.ceil(mc.player.getX() + range.getCurrent());
        int startY = mc.world.getBottomY() + 1;
        int endY = mc.world.getTopYInclusive();
        int startZ = (int) Math.floor(mc.player.getZ() - range.getCurrent());
        int endZ = (int) Math.ceil(mc.player.getZ() + range.getCurrent());

        for (int x = startX; x <= endX; x++) {

            for (int y = startY; y <= endY; y++) {

                for (int z = startZ; z <= endZ; z++) {
                    if (!isStarted) return;
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    if (!(block instanceof AirBlock) && itemSelectSetting.contains(block)) {
                        blocks.add(pos);
                    }


                }
            }
        }
        this.blockPos = blocks;
        isStarted = true;
    }


}
