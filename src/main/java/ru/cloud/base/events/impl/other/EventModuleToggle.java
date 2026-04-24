package ru.cloud.base.events.impl.other;

import com.darkmagician6.eventapi.events.Event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.cloud.client.modules.api.Module;
@AllArgsConstructor
@Getter
public class EventModuleToggle implements Event {
    private final Module module;
    private final boolean enabled;



}
