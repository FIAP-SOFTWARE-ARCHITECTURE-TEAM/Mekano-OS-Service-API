package com.fiap.mekano.os.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * Situação da entrega do veículo. A liberação ocorre quando o serviço de Pagamento
 * confirma o pagamento (evento {@code PagamentoConfirmado}).
 */
public enum StatusEntrega {

    NAO_LIBERADA,
    LIBERADA_PARA_ENTREGA,
    ENTREGUE;

    private static final Map<StatusEntrega, Set<StatusEntrega>> TRANSICOES = Map.of(
            NAO_LIBERADA, Set.of(LIBERADA_PARA_ENTREGA),
            LIBERADA_PARA_ENTREGA, Set.of(ENTREGUE),
            ENTREGUE, Set.of()
    );

    public boolean podeTransicionarPara(StatusEntrega destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }
}
