package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import ru.cloud.base.events.impl.input.EventHotBarScroll;
import ru.cloud.base.events.impl.input.EventKey;
import ru.cloud.base.events.impl.input.EventMouseRotation;
import ru.cloud.base.events.impl.render.EventAspectRatio;
import ru.cloud.base.events.impl.render.EventCamera;
import ru.cloud.base.events.impl.render.EventFov;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.KeySetting;
import ru.cloud.client.modules.api.setting.impl.MultiBooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.player.PlayerIntersectionUtil;
import ru.cloud.utility.game.player.rotation.Rotation;
import ru.cloud.utility.math.MathUtil;

@ModuleAnnotation(
        name = "CameraTweaks",
        description = "Настройки камеры",
        category = Category.RENDER
)
public class CameraTweaks extends Module {
    public static final CameraTweaks INSTANCE = new CameraTweaks();

    private float fov = 110f;
    private float smoothFov = 110f;
    private boolean zooming = false;
    private Perspective perspective;
    private Rotation angle;

    // IMPORTANT: all options are OFF by default to avoid forced 4:3 aspect on first launch.
    private final MultiBooleanSetting multiSetting = new MultiBooleanSetting(
            "Настройки",
            new MultiBooleanSetting.Value("Соотношение сторон", false),
            new MultiBooleanSetting.Value("Клип камеры", false),
            new MultiBooleanSetting.Value("Дистанция камеры", false)
    );

    private final NumberSetting ratioSetting =
            new NumberSetting("Соотношение сторон", 1f, 0.1f, 2.0f, 0.1f,
                    () -> multiSetting.isEnable(0));

    private final NumberSetting distanceSetting =
            new NumberSetting("Дистанция камеры", 3.0F, 2.0F, 5.0F, 0.5f,
                    () -> multiSetting.isEnable(2));

    private final KeySetting zoomSetting = new KeySetting("Зум");
    private final KeySetting freeLookSetting = new KeySetting("Свободный взгляд");

    private CameraTweaks() {
    }

    private Rotation getAngle() {
        if (angle != null) return angle;
        if (mc.player != null) return new Rotation(mc.player.getYaw(), mc.player.getPitch());
        return new Rotation(0, 0);
    }

    @EventTarget
    public void onKey(EventKey e) {
        if (e.is(zoomSetting.getKeyCode()) && !zooming) {
            fov = mc.options.getFov().getValue() - 50;
            zooming = true;
        }

        if (e.isKeyReleased(zoomSetting.getKeyCode(), true)) {
            fov = mc.options.getFov().getValue();
            smoothFov = fov;
            zooming = false;
        }

        if (e.isKeyDown(freeLookSetting.getKeyCode())) {
            perspective = mc.options.getPerspective();
        }
    }

    @EventTarget
    public void onHotBarScroll(EventHotBarScroll e) {
        if (PlayerIntersectionUtil.isKey(zoomSetting)) {
            fov = (int) MathHelper.clamp(fov - e.getVertical() * 10, 1, mc.options.getFov().getValue());
            e.setCancelled(true);
        }
    }

    @EventTarget
    public void onFov(EventFov e) {
        if (PlayerIntersectionUtil.isKey(freeLookSetting)) {
            if (mc.options.getPerspective().isFirstPerson()) {
                mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
        } else if (perspective != null) {
            mc.options.setPerspective(perspective);
            perspective = null;
        }

        if (zooming) {
            e.setFov((int) MathHelper.clamp(
                    (smoothFov = MathUtil.interpolateSmooth(1.6, smoothFov, fov)) + 1,
                    1,
                    mc.options.getFov().getValue()
            ));
            e.cancel();
        }
    }

    @EventTarget
    public void onMouseRotation(EventMouseRotation e) {
        if (PlayerIntersectionUtil.isKey(freeLookSetting)) {
            Rotation current = getAngle();
            angle = new Rotation(
                    current.getYaw() + e.getCursorDeltaX() * 0.15F,
                    MathHelper.clamp(current.getPitch() + e.getCursorDeltaY() * 0.15F, -90F, 90F)
            );
            e.setCancelled(true);
        } else if (mc.player != null) {
            angle = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }
    }

    @EventTarget
    public void onCamera(EventCamera e) {
        e.setCameraClip(multiSetting.isEnable(1));

        if (multiSetting.isEnable(2)) {
            e.setDistance(distanceSetting.getCurrent());
        }

        e.setAngle(getAngle());
        e.cancel();
    }

    @EventTarget
    public void onAspectRatio(EventAspectRatio e) {
        if (multiSetting.isEnable(0)) {
            e.setRatio(ratioSetting.getCurrent());
            e.setCancelled(true);
        }
    }
}