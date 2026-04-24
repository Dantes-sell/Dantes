package ru.cloud.base.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.Block;
import ru.cloud.base.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public class EventPlayerCollision extends EventCancellable {
    private final Block block;
}
