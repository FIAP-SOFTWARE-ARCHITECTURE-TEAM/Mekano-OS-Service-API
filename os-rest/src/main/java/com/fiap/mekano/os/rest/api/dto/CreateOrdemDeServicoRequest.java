package com.fiap.mekano.os.rest.api.dto;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Dados para criação ou atualização de OS")
public class CreateOrdemDeServicoRequest {

    @NotNull
    @Schema(description = "UUID do cliente")
    private UUID clienteUuid;

    @NotNull
    @Schema(description = "UUID do veículo")
    private UUID veiculoUuid;

    @NotBlank
    @Schema(description = "Descrição do problema relatado", example = "Barulho no freio dianteiro")
    private String descricaoProblema;

    @Valid
    @Schema(description = "Itens iniciais da OS (opcional)")
    private List<CreateItemOsRequest> itens;
}
