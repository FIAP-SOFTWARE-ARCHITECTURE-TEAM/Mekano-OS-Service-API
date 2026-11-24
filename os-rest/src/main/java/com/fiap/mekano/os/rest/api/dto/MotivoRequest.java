package com.fiap.mekano.os.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Motivo do cancelamento da OS")
public class MotivoRequest {

    @NotBlank
    @Schema(description = "Motivo do cancelamento", example = "Cliente desistiu do serviço")
    private String motivo;
}
