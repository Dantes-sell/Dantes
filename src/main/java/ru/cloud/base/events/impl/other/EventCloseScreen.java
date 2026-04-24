package ru.cloud.base.events.impl.other;

import lombok.AllArgsConstructor;
import net.minecraft.client.gui.screen.Screen;
import ru.cloud.base.events.callables.EventCancellable;
@AllArgsConstructor
public class EventCloseScreen extends EventCancellable {
   private final Screen screen;
}
