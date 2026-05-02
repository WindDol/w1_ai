package cn.winddol.ai.shared.model.event;

import java.time.Instant;
import java.util.UUID;

public abstract class DomainEvent<T> {

    private final String eventId;
    private final Instant occurredAt;
    private final T payload;

    protected DomainEvent(T payload) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public T getPayload() {
        return payload;
    }
}
