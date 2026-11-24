package com.fiap.mekano.os.rest.api.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.fiap.mekano.os.domain.model.ItemOs;
import com.fiap.mekano.os.domain.model.OrdemDeServico;
import com.fiap.mekano.os.domain.port.in.CreateItemOsCommand;
import com.fiap.mekano.os.domain.port.in.CreateOrdemDeServicoCommand;
import com.fiap.mekano.os.rest.api.dto.CreateItemOsRequest;
import com.fiap.mekano.os.rest.api.dto.CreateOrdemDeServicoRequest;
import com.fiap.mekano.os.rest.api.dto.ItemOsResponse;
import com.fiap.mekano.os.rest.api.dto.OrdemDeServicoResponse;
import com.fiap.mekano.os.rest.api.dto.OrdemDeServicoStatusResponse;

/** Mapper CDI entre DTOs REST e modelos/comandos do domínio. */
@Mapper(componentModel = "cdi")
public interface OrdemDeServicoDtoMapper {

    default CreateOrdemDeServicoCommand toCommand(CreateOrdemDeServicoRequest request) {
        return new CreateOrdemDeServicoCommand(
                request.getClienteUuid(),
                request.getVeiculoUuid(),
                request.getDescricaoProblema(),
                toItemCommands(request.getItens()));
    }

    default List<CreateItemOsCommand> toItemCommands(List<CreateItemOsRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            return List.of();
        }
        return itens.stream()
                .map(i -> new CreateItemOsCommand(
                        i.getReferenciaUuid(),
                        i.getTipo(),
                        i.getQuantidade() != null ? i.getQuantidade() : 1L))
                .toList();
    }

    default OrdemDeServicoResponse toResponse(OrdemDeServico os) {
        if (os == null) {
            return null;
        }
        List<ItemOsResponse> itens = os.getItens().stream().map(this::toItemResponse).toList();
        return new OrdemDeServicoResponse(
                os.getId(),
                os.getClienteUuid(),
                os.getVeiculoUuid(),
                os.getDescricaoProblema(),
                os.getStatus().name(),
                os.getStatusEntrega().name(),
                os.getMotivoCancelamento(),
                os.getOrcamentoUuid(),
                os.getDataAprovacao(),
                os.getMecanicoUuid(),
                os.getExecucaoIniciadaEm(),
                os.getExecucaoFinalizadaEm(),
                os.getObservacaoExecucao(),
                os.getEntregueEm(),
                os.getRecebidoPor(),
                os.getCreatedAt(),
                os.getValorTotalItens(),
                itens);
    }

    default ItemOsResponse toItemResponse(ItemOs item) {
        return new ItemOsResponse(
                item.getId(),
                item.getReferenciaUuid(),
                item.getTipo().name(),
                item.getDescricao(),
                item.getValorUnitario(),
                item.getQuantidade(),
                item.getValorTotal());
    }

    default OrdemDeServicoStatusResponse toStatusResponse(OrdemDeServico os) {
        return new OrdemDeServicoStatusResponse(
                os.getId(),
                os.getStatus().name(),
                os.getStatusEntrega().name(),
                os.getCreatedAt());
    }
}
