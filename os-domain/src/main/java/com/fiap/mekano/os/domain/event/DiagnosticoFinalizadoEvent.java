package com.fiap.mekano.os.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fiap.mekano.os.domain.model.ItemOs;
import com.fiap.mekano.os.domain.model.TipoItemOs;

/**
 * Evento disparado ao finalizar o diagnóstico. Carrega o snapshot dos itens e o valor total
 * para que o serviço de Orçamento gere o orçamento sem precisar consultar a OS de volta.
 */
public record DiagnosticoFinalizadoEvent(
        UUID eventId,
        UUID osUuid,
        UUID clienteUuid,
        String descricao,
        List<ItemSnapshot> itens,
        BigDecimal valorTotal,
        LocalDateTime occurredAt
) implements OsDomainEvent {

    public record ItemSnapshot(
            UUID referenciaUuid,
            TipoItemOs tipo,
            String descricao,
            BigDecimal valorUnitario,
            Long quantidade
    ) {
        public static ItemSnapshot from(ItemOs item) {
            return new ItemSnapshot(
                    item.getReferenciaUuid(),
                    item.getTipo(),
                    item.getDescricao(),
                    item.getValorUnitario(),
                    item.getQuantidade());
        }
    }

    public DiagnosticoFinalizadoEvent {
        Objects.requireNonNull(eventId, "eventId é obrigatório");
        Objects.requireNonNull(osUuid, "osUuid é obrigatório");
        Objects.requireNonNull(clienteUuid, "clienteUuid é obrigatório");
        Objects.requireNonNull(valorTotal, "valorTotal é obrigatório");
        Objects.requireNonNull(occurredAt, "occurredAt é obrigatório");
        itens = itens == null ? List.of() : List.copyOf(itens);
    }

    public static DiagnosticoFinalizadoEvent of(UUID osUuid, UUID clienteUuid, String descricao,
                                                List<ItemOs> itens, BigDecimal valorTotal) {
        List<ItemSnapshot> snapshots = itens == null
                ? List.of()
                : itens.stream().map(ItemSnapshot::from).toList();
        return new DiagnosticoFinalizadoEvent(
                UUID.randomUUID(), osUuid, clienteUuid, descricao, snapshots, valorTotal, LocalDateTime.now());
    }

    @Override
    public String eventType() {
        return "DiagnosticoFinalizado";
    }
}
