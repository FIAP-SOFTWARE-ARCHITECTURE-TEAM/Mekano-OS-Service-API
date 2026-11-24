package com.fiap.mekano.os.rest.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Consulta pública resumida de status da OS")
public record OrdemDeServicoStatusResponse(
        UUID id,
        String status,
        String statusEntrega,
        LocalDateTime dataEntrada
) {
}
