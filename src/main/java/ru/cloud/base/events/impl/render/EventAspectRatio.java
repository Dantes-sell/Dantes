package ru.cloud.base.events.impl.render;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class EventAspectRatio extends EventCancellable {
    private float ratio;
}
