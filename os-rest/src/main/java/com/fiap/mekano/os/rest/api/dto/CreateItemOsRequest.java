package com.fiap.mekano.os.rest.api.dto;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fiap.mekano.os.domain.model.TipoItemOs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Item (peça ou serviço) da OS")
public class CreateItemOsRequest {

    @NotNull
    @Schema(description = "UUID da peça ou serviço no catálogo")
    private UUID referenciaUuid;

    @NotNull
    @Schema(description = "Tipo: PECA ou SERVICO")
    private TipoItemOs tipo;

    @Positive
    @Schema(description = "Quantidade (default 1)", example = "1")
    private Long quantidade;
}
