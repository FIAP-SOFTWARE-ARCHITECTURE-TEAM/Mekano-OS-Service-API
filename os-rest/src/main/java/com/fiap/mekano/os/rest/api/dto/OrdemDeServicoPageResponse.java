package com.fiap.mekano.os.rest.api.dto;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Resposta paginada de Ordens de Serviço")
public record OrdemDeServicoPageResponse(
        List<OrdemDeServicoResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
