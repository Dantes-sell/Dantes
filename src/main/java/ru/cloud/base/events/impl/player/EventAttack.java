package ru.cloud.base.events.impl.player;


import net.minecraft.entity.Entity;
import ru.cloud.base.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class EventAttack extends EventCancellable {
    private final Entity target;
    private final Action action;

    public enum Action {
        POST,
        PRE
    }
}