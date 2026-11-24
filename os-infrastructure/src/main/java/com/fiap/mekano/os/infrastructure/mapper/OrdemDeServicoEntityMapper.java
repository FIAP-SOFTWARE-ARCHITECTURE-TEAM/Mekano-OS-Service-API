package com.fiap.mekano.os.infrastructure.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.fiap.mekano.os.domain.model.ItemOs;
import com.fiap.mekano.os.domain.model.OrdemDeServico;
import com.fiap.mekano.os.domain.model.StatusEntrega;
import com.fiap.mekano.os.domain.model.StatusOS;
import com.fiap.mekano.os.domain.model.TipoItemOs;
import com.fiap.mekano.os.infrastructure.entity.ItemOsEntity;
import com.fiap.mekano.os.infrastructure.entity.OrdemDeServicoEntity;

/** Mapper CDI entre agregado {@link OrdemDeServico} e entidades JPA. */
@Mapper(componentModel = "cdi")
public interface OrdemDeServicoEntityMapper {

    default OrdemDeServico toDomain(OrdemDeServicoEntity entity) {
        if (entity == null) {
            return null;
        }
        List<ItemOs> itens = entity.getItens() == null
                ? List.of()
                : entity.getItens().stream()
                        .filter(i -> Boolean.TRUE.equals(i.getIsActive()))
                        .map(this::toItemDomain)
                        .toList();

        return OrdemDeServico.reconstitute(
                entity.getUuid(),
                entity.getClienteUuid(),
                entity.getVeiculoUuid(),
                entity.getDescricaoProblema(),
                StatusOS.valueOf(entity.getStatus()),
                entity.getStatusEntrega() != null ? StatusEntrega.valueOf(entity.getStatusEntrega()) : null,
                entity.getMotivoCancelamento(),
                entity.getOrcamentoUuid(),
                entity.getDataAprovacao(),
                entity.getMecanicoUuid(),
                entity.getExecucaoIniciadaEm(),
                entity.getExecucaoFinalizadaEm(),
                entity.getObservacaoExecucao(),
                entity.getEntregueEm(),
                entity.getRecebidoPor(),
                entity.getCreatedAt(),
                entity.getVersion(),
                itens);
    }

    default ItemOs toItemDomain(ItemOsEntity entity) {
        if (entity == null) {
            return null;
        }
        return ItemOs.reconstitute(
                entity.getUuid(),
                entity.getReferenciaUuid(),
                TipoItemOs.valueOf(entity.getTipo()),
                entity.getDescricao(),
                entity.getValorUnitario(),
                entity.getQuantidade(),
                entity.getCreatedAt());
    }

    default OrdemDeServicoEntity toEntity(OrdemDeServico os) {
        if (os == null) {
            return null;
        }
        OrdemDeServicoEntity entity = new OrdemDeServicoEntity();
        entity.setUuid(os.getId());
        entity.setClienteUuid(os.getClienteUuid());
        entity.setVeiculoUuid(os.getVeiculoUuid());
        entity.setDescricaoProblema(os.getDescricaoProblema());
        entity.setStatus(os.getStatus().name());
        entity.setStatusEntrega(os.getStatusEntrega().name());
        entity.setMotivoCancelamento(os.getMotivoCancelamento());
        entity.setOrcamentoUuid(os.getOrcamentoUuid());
        entity.setDataAprovacao(os.getDataAprovacao());
        entity.setMecanicoUuid(os.getMecanicoUuid());
        entity.setExecucaoIniciadaEm(os.getExecucaoIniciadaEm());
        entity.setExecucaoFinalizadaEm(os.getExecucaoFinalizadaEm());
        entity.setObservacaoExecucao(os.getObservacaoExecucao());
        entity.setEntregueEm(os.getEntregueEm());
        entity.setRecebidoPor(os.getRecebidoPor());
        entity.setCreatedAt(os.getCreatedAt());
        entity.setVersion(os.getVersion());

        List<ItemOsEntity> itens = os.getItens().stream().map(this::toItemEntity).toList();
        entity.substituirItens(itens);
        return entity;
    }

    default ItemOsEntity toItemEntity(ItemOs item) {
        if (item == null) {
            return null;
        }
        ItemOsEntity entity = new ItemOsEntity();
        entity.setUuid(item.getId());
        entity.setReferenciaUuid(item.getReferenciaUuid());
        entity.setTipo(item.getTipo().name());
        entity.setDescricao(item.getDescricao());
        entity.setValorUnitario(item.getValorUnitario());
        entity.setQuantidade(item.getQuantidade());
        entity.setCreatedAt(item.getCreatedAt());
        return entity;
    }
}
