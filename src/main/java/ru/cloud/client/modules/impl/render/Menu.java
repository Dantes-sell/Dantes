package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import org.lwjgl.glfw.GLFW;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.render.EventRenderScreen;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.screens.dropdown.DropDownScreen;
import ru.cloud.utility.render.display.base.UIContext;

@ModuleAnnotation(name = "Menu", category = Category.RENDER, description = "Меню чита")
public final class Menu extends Module {
    public static final Menu INSTANCE = new Menu();

    private final ModeSetting theme = new ModeSetting("Тип GUI", "CS GUI", "DropDown");
    private DropDownScreen dropDownScreen;

    private Menu() {
        this.setKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        if (mc.world == null) {
            this.setEnabled(false);
            return;
        }

        try {
            if (theme.is("DropDown")) {
                openDropDown();
            } else {
                if (mc.currentScreen == Zenith.getInstance().getMenuScreen()) {
                    return;
                }
                mc.setScreen(Zenith.getInstance().getMenuScreen());
            }
        } catch (Throwable t) {
            // Safety fallback: if CS GUI fails (e.g., class loading issues), open DropDown instead of crashing.
            openDropDown();
        }

        super.onEnable();
    }

    private void openDropDown() {
        if (dropDownScreen == null) {
            dropDownScreen = new DropDownScreen();
        }
        if (mc.currentScreen == dropDownScreen) {
            return;
        }
        mc.setScreen(dropDownScreen);
    }

    @Override
    public void setKeyCode(int keyCode) {
        if (keyCode == -1) {
            return;
        }
        super.setKeyCode(keyCode);
    }

    @EventTarget
    public void render2d(EventRenderScreen eventRender2D) {
        if (theme.is("CS GUI")) {
            UIContext uiContext = eventRender2D.getContext();
            Zenith.getInstance().getMenuScreen().renderTop(uiContext, uiContext.getMouseX(), uiContext.getMouseY());
            if (Zenith.getInstance().getMenuScreen().isFinish()) {
                this.toggle();
            }
        } else if (dropDownScreen != null && dropDownScreen.needToClose && mc.currentScreen != dropDownScreen) {
            this.setToggled(false);
        }
    }
}
