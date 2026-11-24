package com.fiap.mekano.os.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Dados de retirada do veículo")
public class RecebidoPorRequest {

    @NotBlank
    @Schema(description = "Nome de quem recebeu o veículo", example = "João da Silva")
    private String recebidoPor;
}
