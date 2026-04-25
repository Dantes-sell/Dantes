package ru.cloud.client.modules.impl.render;

import net.minecraft.client.option.Perspective;
import net.minecraft.util.hit.HitResult;
import com.darkmagician6.eventapi.EventTarget;
import ru.cloud.base.events.impl.render.EventHudRender;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;


// ������ ��������
@ModuleAnnotation(name = "Crosshair", category = Category.RENDER, description = "Settings for Crosshair")
public final class Crosshair extends Module {
    public static final Crosshair INSTANCE = new Crosshair();
    
    private Crosshair() {
    }

    private final NumberSetting thickness = new NumberSetting("T hi ck ne ss", 1.0f, 0.5f, 3.0f, 0.1f);
    private final NumberSetting length = new NumberSetting("Длина", 3.0f, 1.0f, 8.0f, 0.5f);
    private final NumberSetting gap = new NumberSetting("G ap", 2.0f, 0.0f, 5.0f, 0.5f);
    private final BooleanSetting dynamicGap = new BooleanSetting("D yn am ic Ga p", false);

    private final BooleanSetting useEntityColor = new BooleanSetting("U se En ti ty Co lo r", false);


    private final ColorRGBA entityColor = new ColorRGBA(255, 0, 0, 255);


    @EventTarget
    public void onRender(EventHudRender event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) {
            return;
        }

        CustomDrawContext ctx = event.getContext();
        float x = mc.getWindow().getScaledWidth() / 2f;
        float y = mc.getWindow().getScaledHeight() / 2f;

        float currentGap = gap.getCurrent();
        if (dynamicGap.isEnabled()) {
            float cooldown = 1 - mc.player.getAttackCooldownProgress(0);
            currentGap += 8 * cooldown;
        }

        float currentThickness = thickness.getCurrent();
        float currentLength = length.getCurrent();

        ColorRGBA color = useEntityColor.isEnabled() && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY 
            ? entityColor 
            : new ColorRGBA(255, 255, 255, 255);
        // �������� ������
        drawLine(ctx, x - currentThickness / 2, y - currentGap - currentLength,
                currentThickness, currentLength, color);

        // нижняя залупа
        drawLine(ctx, x - currentThickness / 2, y + currentGap,
                currentThickness, currentLength, color);

        // левая залупа
        drawLine(ctx, x - currentGap - currentLength, y - currentThickness / 2,
                currentLength, currentThickness, color);

        // правая залупа
        drawLine(ctx, x + currentGap, y - currentThickness / 2,
                currentLength, currentThickness, color);
    }

    private void drawLine(CustomDrawContext ctx, float x, float y, float width, float height, ColorRGBA color) {
        ctx.drawRect(x, y, width, height, color);
    }
}

