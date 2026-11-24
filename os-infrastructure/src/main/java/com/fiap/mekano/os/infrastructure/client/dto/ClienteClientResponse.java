package com.fiap.mekano.os.infrastructure.client.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClienteClientResponse(UUID id, Boolean isActive) {
}
