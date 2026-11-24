package com.fiap.mekano.os.domain.event;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Evento disparado quando o veículo é entregue ao cliente. */
public record OSEntregueEvent(
        UUID eventId,
        UUID osUuid,
        String recebidoPor,
        LocalDateTime occurredAt
) implements OsDomainEvent {

    public OSEntregueEvent {
        Objects.requireNonNull(eventId, "eventId é obrigatório");
        Objects.requireNonNull(osUuid, "osUuid é obrigatório");
        Objects.requireNonNull(occurredAt, "occurredAt é obrigatório");
    }

    public static OSEntregueEvent of(UUID osUuid, String recebidoPor) {
        return new OSEntregueEvent(UUID.randomUUID(), osUuid, recebidoPor, LocalDateTime.now());
    }

    @Override
    public String eventType() {
        return "OSEntregue";
    }
}
