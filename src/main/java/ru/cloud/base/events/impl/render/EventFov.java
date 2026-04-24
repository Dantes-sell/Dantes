package ru.cloud.base.events.impl.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.cloud.base.events.callables.EventCancellable;


@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFov extends EventCancellable {
    int fov;
    boolean hand; // true когда вызывается для рендера рук (changingFov = false)
}
