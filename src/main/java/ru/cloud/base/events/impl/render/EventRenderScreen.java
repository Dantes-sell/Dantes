package ru.cloud.base.events.impl.render;

import com.darkmagician6.eventapi.events.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.cloud.utility.render.display.base.UIContext;

@Getter
@RequiredArgsConstructor
public class EventRenderScreen implements Event {

    private final UIContext context;


}