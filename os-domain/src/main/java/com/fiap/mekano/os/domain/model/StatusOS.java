package com.fiap.mekano.os.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados da Ordem de Serviço.
 *
 * <p>{@link #TRANSICOES} é a fonte única da verdade para transições válidas.
 * {@code ENTREGUE} e {@code CANCELADA} são terminais: uma OS cancelada não pode ser reaberta.
 */
public enum StatusOS {

    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    AGUARDANDO_EXECUCAO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA;

    private static final Map<StatusOS, Set<StatusOS>> TRANSICOES = Map.of(
            RECEBIDA, Set.of(EM_DIAGNOSTICO, CANCELADA),
            EM_DIAGNOSTICO, Set.of(AGUARDANDO_APROVACAO, CANCELADA),
            AGUARDANDO_APROVACAO, Set.of(AGUARDANDO_EXECUCAO, CANCELADA),
            AGUARDANDO_EXECUCAO, Set.of(EM_EXECUCAO, CANCELADA),
            EM_EXECUCAO, Set.of(FINALIZADA, CANCELADA),
            FINALIZADA, Set.of(ENTREGUE),
            ENTREGUE, Set.of(),
            CANCELADA, Set.of()
    );

    public boolean podeTransicionarPara(StatusOS destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }

    public boolean isTerminal() {
        return this == ENTREGUE || this == CANCELADA;
    }
}
