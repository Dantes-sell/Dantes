package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import ru.cloud.base.animations.base.Animation;
import ru.cloud.base.animations.base.Easing;
import ru.cloud.base.events.impl.input.EventSetScreen;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(name = "InventoryAnimation", category = Category.RENDER, description = "Анимация при открытии инвентаря")
public final class InventoryAnimation extends Module {
    public static final InventoryAnimation INSTANCE = new InventoryAnimation();

    private final ModeSetting animMode = new ModeSetting("Mode");
    private final ModeSetting.Value slideUp    = new ModeSetting.Value(animMode, "Slide Up").select();
    private final ModeSetting.Value slideDown  = new ModeSetting.Value(animMode, "Slide Down");
    private final ModeSetting.Value slideLeft  = new ModeSetting.Value(animMode, "Slide Left");
    private final ModeSetting.Value slideRight = new ModeSetting.Value(animMode, "Slide Right");
    private final ModeSetting.Value modeScale  = new ModeSetting.Value(animMode, "Scale");
    private final ModeSetting.Value modeRotate = new ModeSetting.Value(animMode, "Rotate");
    private final ModeSetting.Value modeBounce = new ModeSetting.Value(animMode, "Bounce");

    private final NumberSetting speed     = new NumberSetting("Speed (ms)", 300, 100, 800, 25);
    private final NumberSetting intensity = new NumberSetting("Intensity", 30, 5, 80, 5,
            () -> !animMode.is(modeScale) && !animMode.is(modeRotate));
    private final BooleanSetting fadeIn      = new BooleanSetting("Fade In", true);

    private final ModeSetting easingMode = new ModeSetting("Easing");
    private final ModeSetting.Value easingSpring  = new ModeSetting.Value(easingMode, "Spring").select();
    private final ModeSetting.Value easingCubic   = new ModeSetting.Value(easingMode, "Cubic Out");
    private final ModeSetting.Value easingBack    = new ModeSetting.Value(easingMode, "Back Out");
    private final ModeSetting.Value easingElastic = new ModeSetting.Value(easingMode, "Elastic");
    private final ModeSetting.Value easingQuint   = new ModeSetting.Value(easingMode, "Quint Out");

    private final Animation openAnim = new Animation(300, 0f, Easing.BAKEK);
    private boolean pushed = false;

    private InventoryAnimation() {}

    @Override
    public void onEnable() {
        openAnim.setValue(0f);
        pushed = false;
        super.onEnable();
    }

    @EventTarget
    public void onSetScreen(EventSetScreen event) {
        if (!isEnabled()) return;
        if (event.getScreen() instanceof HandledScreen) {
            openAnim.setValue(0f);
            openAnim.setDuration((long) speed.getCurrent());
            openAnim.setEasing(resolveEasing());
        }
    }

    private Easing resolveEasing() {
        if (easingMode.is(easingCubic))   return Easing.CUBIC_OUT;
        if (easingMode.is(easingBack))    return Easing.BACK_OUT;
        if (easingMode.is(easingElastic)) return Easing.ELASTIC_OUT;
        if (easingMode.is(easingQuint))   return Easing.QUINTIC_OUT;
        return Easing.BAKEK;
    }

    /** Returns current animation progress without advancing it. */
    public float peekProgress() {
        return openAnim.getValue();
    }

    public boolean pushTransform(MatrixStack matrices, int screenX, int screenY, int bgWidth, int bgHeight) {
        float progress = openAnim.update(1f);
        if (progress >= 0.999f) { pushed = false; return false; }

        matrices.push();
        pushed = true;

        float inv = 1f - progress;
        float offset = inv * intensity.getCurrent();
        float cx = screenX + bgWidth / 2f;
        float cy = screenY + bgHeight / 2f;

        if (animMode.is(slideUp))         matrices.translate(0, offset, 0);
        else if (animMode.is(slideDown))  matrices.translate(0, -offset, 0);
        else if (animMode.is(slideLeft))  matrices.translate(-offset, 0, 0);
        else if (animMode.is(slideRight)) matrices.translate(offset, 0, 0);
        else if (animMode.is(modeScale)) {
            float s = 0.6f + 0.4f * progress;
            matrices.translate(cx, cy, 0);
            matrices.scale(s, s, 1f);
            matrices.translate(-cx, -cy, 0);
        } else if (animMode.is(modeRotate)) {
            float s = 0.7f + 0.3f * progress;
            matrices.translate(cx, cy, 0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(inv * 12f));
            matrices.scale(s, s, 1f);
            matrices.translate(-cx, -cy, 0);
        } else if (animMode.is(modeBounce)) {
            float s = 0.75f + 0.25f * progress;
            matrices.translate(cx, cy, 0);
            matrices.scale(s, s, 1f);
            matrices.translate(-cx, -cy, 0);
            matrices.translate(0, offset * 1.5f, 0);
        }

        if (fadeIn.isEnabled()) {
            RenderSystem.setShaderColor(1f, 1f, 1f, Math.min(1f, progress * 1.5f));
        }
        return true;
    }

    public void popTransform(MatrixStack matrices) {
        if (pushed) {
            matrices.pop();
            pushed = false;
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }
}
