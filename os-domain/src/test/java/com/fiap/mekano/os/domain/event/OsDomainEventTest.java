package com.fiap.mekano.os.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fiap.mekano.os.domain.model.ItemOs;
import com.fiap.mekano.os.domain.model.StatusOS;
import com.fiap.mekano.os.domain.model.TipoItemOs;

class OsDomainEventTest {

    private static final UUID OS_ID = UUID.randomUUID();
    private static final UUID CLIENTE_ID = UUID.randomUUID();
    private static final UUID VEICULO_ID = UUID.randomUUID();
    private static final UUID ORCAMENTO_ID = UUID.randomUUID();
    private static final UUID MECANICO_ID = UUID.randomUUID();

    @Test
    void factoriesPreenchemEventIdTimestampEType() {
        ItemOs item = ItemOs.create(UUID.randomUUID(), TipoItemOs.PECA, "Filtro", BigDecimal.TEN, 2L);

        OrdemDeServicoCriadaEvent criada = OrdemDeServicoCriadaEvent.of(OS_ID, CLIENTE_ID, VEICULO_ID, "Freio");
        DiagnosticoFinalizadoEvent diag = DiagnosticoFinalizadoEvent.of(
                OS_ID, CLIENTE_ID, "Revisão", List.of(item), new BigDecimal("20"));
        OSExecucaoIniciadaEvent exec = OSExecucaoIniciadaEvent.of(OS_ID, MECANICO_ID, "Início");
        OSFinalizadaEvent fin = OSFinalizadaEvent.of(OS_ID, CLIENTE_ID, ORCAMENTO_ID, new BigDecimal("20"));
        OSCanceladaEvent canc = OSCanceladaEvent.of(OS_ID, ORCAMENTO_ID, "Desistência");
        OSEntregueEvent ent = OSEntregueEvent.of(OS_ID, "Maria");
        OsTransitionedEvent trans = OsTransitionedEvent.of(
                OS_ID, StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO, "Diagnóstico iniciado");

        assertThat(List.of(criada, diag, exec, fin, canc, ent, trans))
                .allSatisfy(ev -> {
                    assertThat(ev.eventId()).isNotNull();
                    assertThat(ev.osUuid()).isEqualTo(OS_ID);
                    assertThat(ev.occurredAt()).isNotNull();
                    assertThat(ev.eventType()).isNotBlank();
                });

        assertThat(criada.eventType()).isEqualTo("OrdemDeServicoCriada");
        assertThat(diag.eventType()).isEqualTo("DiagnosticoFinalizado");
        assertThat(diag.itens()).hasSize(1);
        assertThat(diag.itens().get(0).descricao()).isEqualTo("Filtro");
        assertThat(exec.eventType()).isEqualTo("OSExecucaoIniciada");
        assertThat(fin.eventType()).isEqualTo("OSFinalizada");
        assertThat(canc.eventType()).isEqualTo("OSCancelada");
        assertThat(ent.eventType()).isEqualTo("OSEntregue");
        assertThat(trans.eventType()).isEqualTo("OsTransitioned");
    }

    @Test
    void diagnosticoFinalizadoListaEhImutavelENullSafe() {
        DiagnosticoFinalizadoEvent semItens = DiagnosticoFinalizadoEvent.of(
                OS_ID, CLIENTE_ID, "Sem itens", null, BigDecimal.ZERO);

        assertThat(semItens.itens()).isEmpty();
        assertThatThrownBy(() -> semItens.itens().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void validaCamposObrigatorios() {
        assertThatThrownBy(() -> OrdemDeServicoCriadaEvent.of(null, CLIENTE_ID, VEICULO_ID, "x"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> DiagnosticoFinalizadoEvent.of(OS_ID, null, "x", List.of(), BigDecimal.ZERO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OSExecucaoIniciadaEvent.of(OS_ID, null, "x"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OSFinalizadaEvent.of(OS_ID, CLIENTE_ID, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OSCanceladaEvent.of(null, null, "x"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OSEntregueEvent.of(null, "x"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OsTransitionedEvent.of(OS_ID, StatusOS.RECEBIDA, null, "x"))
                .isInstanceOf(NullPointerException.class);
    }
}
