package com.fiap.mekano.os.rest.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Item (snapshot) da Ordem de Serviço")
public record ItemOsResponse(
        UUID id,
        UUID referenciaUuid,
        String tipo,
        String descricao,
        BigDecimal valorUnitario,
        Long quantidade,
        BigDecimal valorTotal
) {
}
