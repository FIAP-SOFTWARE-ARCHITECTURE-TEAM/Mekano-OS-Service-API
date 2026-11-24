package com.fiap.mekano.os.domain.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.os.domain.model.TipoItemOs;

/**
 * Consulta somente-leitura ao catálogo de Serviços e Peças/Estoque (monolito via REST)
 * para captura do snapshot de descrição e valor unitário na criação dos itens da OS.
 */
public interface CatalogoQueryPort {

    record CatalogoItemSnapshot(
            UUID referenciaUuid,
            TipoItemOs tipo,
            String descricao,
            BigDecimal valorUnitario,
            boolean ativo
    ) {
    }

    Optional<CatalogoItemSnapshot> buscarItem(UUID referenciaUuid, TipoItemOs tipo);
}
