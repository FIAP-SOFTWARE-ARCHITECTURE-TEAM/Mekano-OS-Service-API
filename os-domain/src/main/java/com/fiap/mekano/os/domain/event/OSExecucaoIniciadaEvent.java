package com.fiap.mekano.os.domain.event;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Evento disparado quando o mecânico inicia a execução da OS. */
public record OSExecucaoIniciadaEvent(
        UUID eventId,
        UUID osUuid,
        UUID mecanicoUuid,
        String observacao,
        LocalDateTime occurredAt
) implements OsDomainEvent {

    public OSExecucaoIniciadaEvent {
        Objects.requireNonNull(eventId, "eventId é obrigatório");
        Objects.requireNonNull(osUuid, "osUuid é obrigatório");
        Objects.requireNonNull(mecanicoUuid, "mecanicoUuid é obrigatório");
        Objects.requireNonNull(occurredAt, "occurredAt é obrigatório");
    }

    public static OSExecucaoIniciadaEvent of(UUID osUuid, UUID mecanicoUuid, String observacao) {
        return new OSExecucaoIniciadaEvent(
                UUID.randomUUID(), osUuid, mecanicoUuid, observacao, LocalDateTime.now());
    }

    @Override
    public String eventType() {
        return "OSExecucaoIniciada";
    }
}
