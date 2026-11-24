package com.fiap.mekano.os.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Observação de finalização da execução")
public class FinalizarExecucaoRequest {

    @Schema(description = "Observação técnica final")
    private String observacao;
}
