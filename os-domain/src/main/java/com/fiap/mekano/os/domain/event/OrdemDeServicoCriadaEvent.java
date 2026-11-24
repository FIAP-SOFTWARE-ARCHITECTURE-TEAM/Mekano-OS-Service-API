package com.fiap.mekano.os.domain.event;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Evento disparado quando uma nova OS é criada com status {@code RECEBIDA}. */
public record OrdemDeServicoCriadaEvent(
        UUID eventId,
        UUID osUuid,
        UUID clienteUuid,
        UUID veiculoUuid,
        String descricaoProblema,
        LocalDateTime occurredAt
) implements OsDomainEvent {

    public OrdemDeServicoCriadaEvent {
        Objects.requireNonNull(eventId, "eventId é obrigatório");
        Objects.requireNonNull(osUuid, "osUuid é obrigatório");
        Objects.requireNonNull(clienteUuid, "clienteUuid é obrigatório");
        Objects.requireNonNull(veiculoUuid, "veiculoUuid é obrigatório");
        Objects.requireNonNull(occurredAt, "occurredAt é obrigatório");
    }

    public static OrdemDeServicoCriadaEvent of(UUID osUuid, UUID clienteUuid, UUID veiculoUuid,
                                               String descricaoProblema) {
        return new OrdemDeServicoCriadaEvent(
                UUID.randomUUID(), osUuid, clienteUuid, veiculoUuid, descricaoProblema, LocalDateTime.now());
    }

    @Override
    public String eventType() {
        return "OrdemDeServicoCriada";
    }
}
