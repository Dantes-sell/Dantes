package ru.cloud.base.mainmenu;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.TitleScreen;
import ru.cloud.client.screens.mainmenu.MainMenuScreen;
import ru.cloud.base.events.impl.input.EventSetScreen;
import ru.cloud.client.screens.loading.LoadingScreen;

public class MainMenuHandler {

    private boolean shown = false;

    public MainMenuHandler() {
        EventManager.register(this);
    }

    @EventTarget
    public void onSetScreen(EventSetScreen event) {
        if (event.getScreen() instanceof TitleScreen) {
            if (!shown) {
                shown = true;
                event.setScreen(new LoadingScreen());
            } else {
                
                event.setScreen(new MainMenuScreen());
            }
        }
    }
}

