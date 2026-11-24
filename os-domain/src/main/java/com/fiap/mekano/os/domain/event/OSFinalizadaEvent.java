package com.fiap.mekano.os.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento disparado quando a execução da OS é finalizada.
 * Consumido por Pagamento (gerar cobrança), WhatsApp (avisar cliente) e Estoque (baixa definitiva).
 */
public record OSFinalizadaEvent(
        UUID eventId,
        UUID osUuid,
        UUID clienteUuid,
        UUID orcamentoUuid,
        BigDecimal valorTotal,
        LocalDateTime occurredAt
) implements OsDomainEvent {

    public OSFinalizadaEvent {
        Objects.requireNonNull(eventId, "eventId é obrigatório");
        Objects.requireNonNull(osUuid, "osUuid é obrigatório");
        Objects.requireNonNull(clienteUuid, "clienteUuid é obrigatório");
        Objects.requireNonNull(valorTotal, "valorTotal é obrigatório");
        Objects.requireNonNull(occurredAt, "occurredAt é obrigatório");
    }

    public static OSFinalizadaEvent of(UUID osUuid, UUID clienteUuid, UUID orcamentoUuid, BigDecimal valorTotal) {
        return new OSFinalizadaEvent(
                UUID.randomUUID(), osUuid, clienteUuid, orcamentoUuid, valorTotal, LocalDateTime.now());
    }

    @Override
    public String eventType() {
        return "OSFinalizada";
    }
}
