package com.fiap.mekano.os.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServicoClientResponse(UUID id, String nome, String descricao, BigDecimal valor, Boolean isActive) {
}
