package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.BlockState;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ColorSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.level.Render3DUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleAnnotation(
        name = "ChorusTpEsp",
        category = Category.RENDER,
        description = "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u043a\u0443\u0434\u0430 \u043c\u043e\u0436\u043d\u043e \u0442\u0435\u043f\u043d\u0443\u0442\u044c\u0441\u044f"
)
public final class ChorusTeleportEsp extends Module {
    public static final ChorusTeleportEsp INSTANCE = new ChorusTeleportEsp();

    private static final int MAX_POSITIONS = 24;

    private final NumberSetting range = new NumberSetting("\u0414\u0438\u0441\u0442\u0430\u043d\u0446\u0438\u044f", 8.0f, 4.0f, 16.0f, 1.0f);
    private final BooleanSetting onlyWithChorus = new BooleanSetting("\u0422\u043e\u043b\u044c\u043a\u043e \u0441 \u0445\u043e\u0440\u0443\u0441\u043e\u043c", true);
    private final ColorSetting color = new ColorSetting("\u0426\u0432\u0435\u0442", new ColorRGBA(170, 120, 255, 180));

    private final List<BlockPos> teleportPositions = new ArrayList<>();

    private ChorusTeleportEsp() {
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            teleportPositions.clear();
            return;
        }

        updatePositions();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) {
            teleportPositions.clear();
            return;
        }

        updatePositions();
        renderPositions();
    }

    private void updatePositions() {
        if (onlyWithChorus.isEnabled()
                && !mc.player.getMainHandStack().isOf(Items.CHORUS_FRUIT)
                && !mc.player.getOffHandStack().isOf(Items.CHORUS_FRUIT)) {
            teleportPositions.clear();
            return;
        }

        teleportPositions.clear();
        BlockPos base = mc.player.getBlockPos();
        int scanRange = Math.round(range.getCurrent());

        for (int x = -scanRange; x <= scanRange; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -scanRange; z <= scanRange; z++) {
                    BlockPos pos = base.add(x, y, z);
                    if (canTeleportTo(pos)) {
                        teleportPositions.add(pos.toImmutable());
                    }
                }
            }
        }

        teleportPositions.sort(Comparator.comparingDouble(base::getSquaredDistance));
        if (teleportPositions.size() > MAX_POSITIONS) {
            teleportPositions.subList(MAX_POSITIONS, teleportPositions.size()).clear();
        }
    }

    private boolean canTeleportTo(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        BlockState above = mc.world.getBlockState(pos.up());
        BlockState below = mc.world.getBlockState(pos.down());

        return state.isAir() && above.isAir() && below.isSolidBlock(mc.world, pos.down());
    }

    private void renderPositions() {
        int boxColor = color.getColor().getRGB();
        for (BlockPos pos : teleportPositions) {
            Render3DUtil.drawBox(new Box(pos), boxColor, 1.0f);
        }
    }
}
