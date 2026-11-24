package com.fiap.mekano.os.rest.api.dto;

import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Tempo médio de execução de OS (em minutos)")
public record TempoMedioResponse(
        Double tempoMedioMinutos,
        Map<UUID, Double> porMecanico
) {
}
