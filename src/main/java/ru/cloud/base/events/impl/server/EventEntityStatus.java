package ru.cloud.base.events.impl.server;

import com.darkmagician6.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
@AllArgsConstructor
public class EventEntityStatus implements Event {
    private final Entity entity;
    private final byte status;
}
