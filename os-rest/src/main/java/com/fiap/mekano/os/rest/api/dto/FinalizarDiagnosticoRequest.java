package com.fiap.mekano.os.rest.api.dto;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Dados para finalizar diagnóstico técnico")
public class FinalizarDiagnosticoRequest {

    @NotBlank
    @Schema(description = "Resumo do diagnóstico", example = "Necessária troca de pastilhas e disco")
    private String descricao;

    @Valid
    @Schema(description = "Novos itens identificados no diagnóstico (opcional se já houver itens na OS)")
    private List<CreateItemOsRequest> itens;
}
