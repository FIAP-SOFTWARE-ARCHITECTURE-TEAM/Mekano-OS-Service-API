package com.fiap.mekano.os.infrastructure.event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Envelope dos eventos recebidos via SQS (Orçamento e Pagamento). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OsInboundEventPayload(
        UUID eventId,
        String eventType,
        UUID osUuid,
        UUID orcamentoUuid,
        String motivo
) {
}
