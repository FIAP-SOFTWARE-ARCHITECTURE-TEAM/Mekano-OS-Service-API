package com.fiap.mekano.os.domain.event;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fiap.mekano.os.domain.model.StatusOS;

/** Evento genérico de transição de status da OS, consumido pelo serviço de Auditoria. */
public record OsTransitionedEvent(
        UUID eventId,
        UUID osUuid,
        StatusOS statusAnterior,
        StatusOS statusNovo,
        String observacao,
        LocalDateTime occurredAt
) implements OsDomainEvent {

    public OsTransitionedEvent {
        Objects.requireNonNull(eventId, "eventId é obrigatório");
        Objects.requireNonNull(osUuid, "osUuid é obrigatório");
        Objects.requireNonNull(statusNovo, "statusNovo é obrigatório");
        Objects.requireNonNull(occurredAt, "occurredAt é obrigatório");
    }

    public static OsTransitionedEvent of(UUID osUuid, StatusOS statusAnterior, StatusOS statusNovo,
                                         String observacao) {
        return new OsTransitionedEvent(
                UUID.randomUUID(), osUuid, statusAnterior, statusNovo, observacao, LocalDateTime.now());
    }

    @Override
    public String eventType() {
        return "OsTransitioned";
    }
}
