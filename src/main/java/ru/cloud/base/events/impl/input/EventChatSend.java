package ru.cloud.base.events.impl.input;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.cloud.base.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public final class EventChatSend extends EventCancellable {
    @Setter
    private String message;
}