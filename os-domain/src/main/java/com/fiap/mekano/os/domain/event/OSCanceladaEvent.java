package com.fiap.mekano.os.domain.event;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Evento disparado quando a OS é cancelada. Consumido por Estoque (liberar reserva) e WhatsApp. */
public record OSCanceladaEvent(
        UUID eventId,
        UUID osUuid,
        UUID orcamentoUuid,
        String motivo,
        LocalDateTime occurredAt
) implements OsDomainEvent {

    public OSCanceladaEvent {
        Objects.requireNonNull(eventId, "eventId é obrigatório");
        Objects.requireNonNull(osUuid, "osUuid é obrigatório");
        Objects.requireNonNull(occurredAt, "occurredAt é obrigatório");
    }

    public static OSCanceladaEvent of(UUID osUuid, UUID orcamentoUuid, String motivo) {
        return new OSCanceladaEvent(UUID.randomUUID(), osUuid, orcamentoUuid, motivo, LocalDateTime.now());
    }

    @Override
    public String eventType() {
        return "OSCancelada";
    }
}
