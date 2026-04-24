package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.joml.Vector4d;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.render.EventRender2D;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.base.theme.Theme;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.MultiBooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.client.modules.impl.misc.NameProtect;
import ru.cloud.utility.game.player.PlayerIntersectionUtil;
import ru.cloud.utility.math.ProjectionUtil;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(name = "NameTags", category = Category.RENDER, description = "Показывает информацию игрока")
public final class NameTags extends Module {
    public static final NameTags INSTANCE = new NameTags();

    private final MultiBooleanSetting elements = MultiBooleanSetting.create("Настройки",
            List.of("Броня", "Зачарование", "Показывать шары", "Отображать VoiceChat"));
    private final NumberSetting size = new NumberSetting("Размер шрифта", 0.6f, 0.5f, 0.9f, 0.02f);

    private NameTags() {
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        CustomDrawContext context = event.getContext();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) {
                continue;
            }

            Vector4d vec = ProjectionUtil.getVector4D(player);
            if (ProjectionUtil.canSee(vec)) {
                continue;
            }

            renderTag(context, player, vec);
        }
    }

    private void renderTag(CustomDrawContext context, PlayerEntity player, Vector4d vec) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(8 * size.getCurrent());

        String displayName = getDisplayName(player);
        String health = PlayerIntersectionUtil.getHealthString(player);
        String finalText = displayName + " " + health + "HP";

        float textWidth = font.width(finalText);
        float centerX = (float) ProjectionUtil.centerX(vec);
        float x = centerX - textWidth / 2f - 6f;
        float y = (float) vec.y - 16f;
        float width = textWidth + 12f;
        float height = 12f;

        ColorRGBA bg = Zenith.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())
                ? theme.getColor().withAlpha(theme.getForegroundLight().getAlpha())
                : theme.getForegroundLight();

        context.drawRoundedRect(x, y, width, height, BorderRadius.all(2), bg);
        context.drawText(font, finalText, x + 6f, y + 3f, getHealthColor(PlayerIntersectionUtil.getHealth(player)));

        if (player.getDisplayName().getString().contains("●")) {
            int dotColor = resolvePrefixDotColor(player.getDisplayName().getString());
            context.drawRoundedRect(x - 5f, y + 4f, 3f, 3f, BorderRadius.all(1.5f), new ColorRGBA(dotColor));
        }

        if (elements.isEnable("Показывать шары")) {
            drawSpecialOffhand(context, player, x + width + 3f, y);
        }

        if (elements.isEnable("Броня")) {
            drawArmor(context, player, centerX, y - 19f);
        }
    }

    private String getDisplayName(PlayerEntity player) {
        boolean friend = Zenith.getInstance().getFriendManager().isFriend(player.getNameForScoreboard());
        if (NameProtect.getCustomName() != null && friend) {
            return NameProtect.getCustomName(player.getNameForScoreboard());
        }
        Text text = player.getDisplayName();
        return text == null ? player.getGameProfile().getName() : text.getString();
    }

    private void drawSpecialOffhand(CustomDrawContext context, PlayerEntity player, float x, float y) {
        ItemStack offhand = player.getOffHandStack();
        if (offhand.isEmpty()) {
            return;
        }

        boolean special = offhand.isOf(Items.PLAYER_HEAD) || offhand.isOf(Items.TOTEM_OF_UNDYING);
        if (!special) {
            return;
        }

        context.pushMatrix();
        context.getMatrices().translate(x, y - 1f, 0);
        context.getMatrices().scale(0.8f, 0.8f, 1f);
        context.drawItem(offhand, 0, 0);
        context.popMatrix();
    }

    private void drawArmor(CustomDrawContext context, PlayerEntity player, float centerX, float y) {
        List<ItemStack> stacks = new ArrayList<>();
        if (!player.getMainHandStack().isEmpty()) stacks.add(player.getMainHandStack());
        if (!player.getOffHandStack().isEmpty()) stacks.add(player.getOffHandStack());
        for (ItemStack armor : player.getInventory().armor) {
            if (!armor.isEmpty()) stacks.add(armor);
        }

        if (stacks.isEmpty()) {
            return;
        }

        float startX = centerX - (stacks.size() * 16f) / 2f;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            float itemX = startX + i * 16f;

            context.pushMatrix();
            context.getMatrices().translate(itemX, y, 0);
            context.getMatrices().scale(0.85f, 0.85f, 1f);
            context.drawItem(stack, 0, 0);
            context.popMatrix();

            if (elements.isEnable("Зачарование")) {
                drawEnchantments(context, stack, itemX + 8f, y - 7f);
            }
        }
    }

    private void drawEnchantments(CustomDrawContext context, ItemStack stack, float x, float y) {
        ItemEnchantmentsComponent component = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        if (component == ItemEnchantmentsComponent.DEFAULT) {
            return;
        }

        Font font = Fonts.MEDIUM.getFont(6f);
        int index = 0;
        for (RegistryEntry<Enchantment> enchantment : component.getEnchantments()) {
            int level = component.getLevel(enchantment);
            if (level <= 0) {
                continue;
            }

            String id = enchantment.getKey().map(key -> key.getValue().getPath()).orElse("ench");
            String shortName = id.isEmpty() ? "E" : id.substring(0, 1).toUpperCase();
            context.drawText(font, shortName + level, x - 4f, y - index * 6f, ColorRGBA.WHITE);
            index++;
            if (index >= 3) {
                break;
            }
        }
    }

    private ColorRGBA getHealthColor(float health) {
        if (health <= 7) return new ColorRGBA(255, 0, 0, 255);
        if (health <= 15) return new ColorRGBA(255, 255, 0, 255);
        return new ColorRGBA(0, 255, 0, 255);
    }

    private int resolvePrefixDotColor(String name) {
        if (name.contains("§a●")) return 0xFF54FC54;
        if (name.contains("§c●")) return 0xFFFC5454;
        if (name.contains("§6●")) return 0xFFFCA800;
        return 0xFFFC5454;
    }
}
