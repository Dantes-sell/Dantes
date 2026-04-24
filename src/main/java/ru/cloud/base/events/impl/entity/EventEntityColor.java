package ru.cloud.base.events.impl.entity;

import lombok.*;
import ru.cloud.base.events.callables.EventCancellable;
@Getter
@Setter
@AllArgsConstructor
public class EventEntityColor extends EventCancellable {
    private int color;
}
