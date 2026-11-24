package com.fiap.mekano.os.rest.api.dto;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Dados para iniciar execução da OS")
public class IniciarExecucaoRequest {

    @NotNull
    @Schema(description = "UUID do mecânico responsável")
    private UUID mecanicoUuid;

    @Schema(description = "Observação inicial")
    private String observacao;
}
