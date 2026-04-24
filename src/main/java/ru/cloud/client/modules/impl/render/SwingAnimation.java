package ru.cloud.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.client.modules.impl.combat.Aura;

@ModuleAnnotation(name = "SwingAnimation", category = Category.RENDER, description = "Custom hand swing animation")
public final class SwingAnimation extends Module {
    public static final SwingAnimation INSTANCE = new SwingAnimation();

    private SwingAnimation() {}

    public final ModeSetting animationMode = new ModeSetting(
            "Animation Mode",
            "Classic", "Aggressive", "Sharp", "Heavy", "Flow", "Long",
            "Swipe", "Down", "Smooth", "Power", "Feast", "Twist", "Spin"
    );

    // пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ (пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅ PlayerEntityMixin пїЅпїЅпїЅ getHandSwingDuration)
    public final NumberSetting swingPower = new NumberSetting("Swing Duration", 5.0f, 1.0f, 10.0f, 0.05f);

    // ???? ???????? (????????? ?????)
    public final NumberSetting hitStrength = new NumberSetting("Hit Strength", 1.0f, 0.5f, 3.0f, 0.05f);

    public final BooleanSetting onlySwing = new BooleanSetting("Only On Swing", false);
    public final BooleanSetting onlyAura = new BooleanSetting("Only With Aura", false);

    public void renderSwordAnimation(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float strength = hitStrength.getCurrent();

        
        if (onlySwing.isEnabled() && swingProgress == 0f) {
            matrices.translate(i * 0.56F, -0.52F, -0.72F);
            return;
        }

        
        if (onlyAura.isEnabled() && !(Aura.INSTANCE.isEnabled() && Aura.INSTANCE.getTarget() != null)) {
            applyDefaultSwing(matrices, arm, swingProgress, equipProgress);
            return;
        }

        float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float sinSmooth = (float) (Math.sin(swingProgress * Math.PI) * 0.5F);

        int modeIndex = animationMode.getValues().indexOf(animationMode.getValue());

        switch (modeIndex) {
            case 0 -> {
                matrices.translate(0.56F, -0.52F, -0.72F);
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -60.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(g * -30.0F));
            }
            case 1 -> {
                if (swingProgress > 0) {
                    float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    matrices.translate(0.56F, equipProgress * -0.2f - 0.5F, -0.7F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -85.0F));
                    matrices.translate(-0.1F, 0.28F, 0.2F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85.0F));
                } else {
                    float n = -0.4f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    float m = 0.2f * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float) Math.PI * 2));
                    float f1 = -0.2f * MathHelper.sin(swingProgress * (float) Math.PI);
                    matrices.translate(n, m, f1);
                    applyEquipOffset(matrices, arm, equipProgress);
                    applySwingOffset(matrices, arm, swingProgress);
                }
            }
            case 2 -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f + 20f * g));
            }
            case 3 -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-30f * (1f - g) - 30f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f));
            }
            case 4 -> {
                float g = MathHelper.sin(swingProgress * (float) Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.translate(0.1F, -0.2F, -0.3F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30f * g - 36f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25f * g));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(12f));
            }
            case 5 -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.translate(0.0F, -0.2F, -0.4F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-120f * g - 3f));
            }
            case 12 -> {
                matrices.translate(i * 0.56F, -0.36F, -0.72F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80 * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90 * strength));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((sin1 - sin2) * 60 * i * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30));
                matrices.translate(0, -0.1F, 0.05F);
            }
            case 7 -> {
                matrices.translate(0.56F * i, -0.32F, -0.72F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(60 * i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60 * i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5 * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -120 * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60));
            }
            case 8 -> {
                matrices.translate(i * 0.56F, -0.32F, -0.72F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5 * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100 * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155 * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100));
            }
            case 9 -> {
                matrices.translate(i * 0.56F, -0.42F, -0.72F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + sin1 * -20.0F * strength)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * sin2 * -20.0F * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
                matrices.translate(0, -0.1, 0);
            }
            case 10 -> {
                matrices.translate(i * 0.56F, -0.32F, -0.72F);
                matrices.translate((-sinSmooth * sinSmooth * sin1) * i * strength, 0, 0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61 * i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * strength));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5 * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -30 * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60 * strength));
            }
            case 11 -> {
                matrices.translate(i * 0.56F, -0.32F, -0.72F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75 * i * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45 * strength));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
            }
            case 13 -> {
                // пїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅ
                matrices.translate(i * 0.56F, -0.52F, -0.72F);
                // пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅ
                float angle = (float) (System.currentTimeMillis() / 4L % 360L);

                float anim = (float) Math.sin(swingProgress * (Math.PI / 2) * 2);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle + (40.0F + strength * 5) * anim));
            }
            default -> applyDefaultSwing(matrices, arm, swingProgress, equipProgress);
        }
    }

    private void applyDefaultSwing(MatrixStack matrices, Arm arm, float swingProgress, float equipProgress) {
        applyEquipOffset(matrices, arm, equipProgress);
        applySwingOffset(matrices, arm, swingProgress);
    }

    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }
}

