package com.fiap.mekano.os.rest.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Dados completos da Ordem de Serviço")
public record OrdemDeServicoResponse(
        UUID id,
        UUID clienteUuid,
        UUID veiculoUuid,
        String descricaoProblema,
        String status,
        String statusEntrega,
        String motivoCancelamento,
        UUID orcamentoUuid,
        LocalDateTime dataAprovacao,
        UUID mecanicoUuid,
        LocalDateTime execucaoIniciadaEm,
        LocalDateTime execucaoFinalizadaEm,
        String observacaoExecucao,
        LocalDateTime entregueEm,
        String recebidoPor,
        LocalDateTime createdAt,
        BigDecimal valorTotalItens,
        List<ItemOsResponse> itens
) {
}
