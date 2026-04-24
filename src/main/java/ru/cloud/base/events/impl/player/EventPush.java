package ru.cloud.base.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.cloud.base.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public class EventPush extends EventCancellable {

    private final Type type;

    public enum Type {
        WATER, BLOCK, ENTITY_COLLISION
    }
}
