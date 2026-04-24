package ru.cloud.client.screens.menu.elements.api;

import ru.cloud.base.font.Font;
import ru.cloud.client.modules.api.Category;
import ru.cloud.utility.game.other.MouseButton;
import ru.cloud.utility.render.display.base.UIContext;

public abstract class AbstractMenuElement {
    public abstract void render(UIContext ctx, float mouseX, float mouseY, Font font,
                                float x, float y, float moduleWidth, float alpha, int colum);

    public abstract float getHeight();

    public abstract void onMouseClicked(double mouseX, double mouseY, MouseButton button);

    public abstract void onMouseReleased(double mouseX, double mouseY, MouseButton button);

    public abstract void onMouseDragged(double mouseX, double mouseY, MouseButton button,
                                        double deltaX, double deltaY);

    public abstract boolean keyPressed(int keyCode, int scanCode, int modifiers);

    public abstract boolean mouseScrolled(double mouseX, double mouseY,
                                          double horizontalAmount, double verticalAmount);
    public abstract Category getCategory();
    public abstract String getName();
}
