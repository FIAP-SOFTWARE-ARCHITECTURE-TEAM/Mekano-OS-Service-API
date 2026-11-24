package com.fiap.mekano.os.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StatusOSTest {

    @Test
    void fluxoFelizEPermitido() {
        assertThat(StatusOS.RECEBIDA.podeTransicionarPara(StatusOS.EM_DIAGNOSTICO)).isTrue();
        assertThat(StatusOS.EM_DIAGNOSTICO.podeTransicionarPara(StatusOS.AGUARDANDO_APROVACAO)).isTrue();
        assertThat(StatusOS.AGUARDANDO_APROVACAO.podeTransicionarPara(StatusOS.AGUARDANDO_EXECUCAO)).isTrue();
        assertThat(StatusOS.AGUARDANDO_EXECUCAO.podeTransicionarPara(StatusOS.EM_EXECUCAO)).isTrue();
        assertThat(StatusOS.EM_EXECUCAO.podeTransicionarPara(StatusOS.FINALIZADA)).isTrue();
        assertThat(StatusOS.FINALIZADA.podeTransicionarPara(StatusOS.ENTREGUE)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"ENTREGUE", "CANCELADA"})
    void terminaisNaoTemSaida(StatusOS terminal) {
        assertThat(terminal.isTerminal()).isTrue();
        for (StatusOS destino : StatusOS.values()) {
            assertThat(terminal.podeTransicionarPara(destino)).isFalse();
        }
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO",
            "AGUARDANDO_EXECUCAO", "EM_EXECUCAO"})
    void podeCancelarAteEmExecucao(StatusOS origem) {
        assertThat(origem.podeTransicionarPara(StatusOS.CANCELADA)).isTrue();
    }

    @Test
    void finalizadaNaoPodeSerCancelada() {
        assertThat(StatusOS.FINALIZADA.podeTransicionarPara(StatusOS.CANCELADA)).isFalse();
    }

    @Test
    void naoPermitePularEtapas() {
        assertThat(StatusOS.RECEBIDA.podeTransicionarPara(StatusOS.EM_EXECUCAO)).isFalse();
        assertThat(StatusOS.EM_DIAGNOSTICO.podeTransicionarPara(StatusOS.FINALIZADA)).isFalse();
        assertThat(StatusEntrega.NAO_LIBERADA.podeTransicionarPara(StatusEntrega.ENTREGUE)).isFalse();
        assertThat(StatusEntrega.NAO_LIBERADA.podeTransicionarPara(StatusEntrega.LIBERADA_PARA_ENTREGA)).isTrue();
    }
}
