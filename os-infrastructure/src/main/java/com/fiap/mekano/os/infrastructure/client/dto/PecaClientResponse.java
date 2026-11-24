package com.fiap.mekano.os.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PecaClientResponse(UUID id, String descricao, BigDecimal valorUnitario, Boolean isActive) {
}
