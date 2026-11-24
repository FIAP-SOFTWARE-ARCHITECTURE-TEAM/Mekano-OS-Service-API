package com.fiap.mekano.os.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fiap.mekano.os.domain.exception.AppException;

class OrdemDeServicoTest {

    private static final UUID CLIENTE = UUID.randomUUID();
    private static final UUID VEICULO = UUID.randomUUID();
    private static final UUID MECANICO = UUID.randomUUID();
    private static final UUID ORCAMENTO = UUID.randomUUID();

    private static OrdemDeServico nova() {
        return OrdemDeServico.create(CLIENTE, VEICULO, "  Barulho no motor ");
    }

    private static ItemOs item(String desc, String valor, long qtd) {
        return ItemOs.create(UUID.randomUUID(), TipoItemOs.PECA, desc, new BigDecimal(valor), qtd);
    }

    private static OrdemDeServico emDiagnosticoComItem() {
        OrdemDeServico os = nova();
        os.iniciarDiagnostico();
        os.adicionarItem(item("Pastilha", "100.00", 1));
        return os;
    }

    private static OrdemDeServico aguardandoExecucao() {
        OrdemDeServico os = emDiagnosticoComItem();
        os.finalizarDiagnostico();
        os.registrarAprovacaoOrcamento(ORCAMENTO);
        return os;
    }

    private static OrdemDeServico finalizada() {
        OrdemDeServico os = aguardandoExecucao();
        os.iniciarExecucao(MECANICO, "ok");
        os.finalizarExecucao("pronto");
        return os;
    }

    private static void assertStatus(Runnable r, int status) {
        assertThatThrownBy(r::run).isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(status);
    }

    // ── criação ──

    @Test
    void createIniciaRecebidaEntregaNaoLiberada() {
        OrdemDeServico os = nova();

        assertThat(os.getId()).isNotNull();
        assertThat(os.getStatus()).isEqualTo(StatusOS.RECEBIDA);
        assertThat(os.getStatusEntrega()).isEqualTo(StatusEntrega.NAO_LIBERADA);
        assertThat(os.getDescricaoProblema()).isEqualTo("Barulho no motor");
        assertThat(os.getVersion()).isZero();
        assertThat(os.getItens()).isEmpty();
        assertThat(os.getCreatedAt()).isNotNull();
    }

    @Test
    void createValidaObrigatorios() {
        assertStatus(() -> OrdemDeServico.create(null, VEICULO, "x"), 400);
        assertStatus(() -> OrdemDeServico.create(CLIENTE, null, "x"), 400);
        assertStatus(() -> OrdemDeServico.create(CLIENTE, VEICULO, " "), 400);
        assertStatus(() -> OrdemDeServico.create(CLIENTE, VEICULO, null), 400);
    }

    @Test
    void reconstitutePreservaDadosEAplicaDefaults() {
        UUID id = UUID.randomUUID();
        LocalDateTime criado = LocalDateTime.of(2026, 5, 1, 8, 0);
        ItemOs it = item("x", "1", 1);

        OrdemDeServico os = OrdemDeServico.reconstitute(id, CLIENTE, VEICULO, "desc", StatusOS.EM_EXECUCAO,
                null, null, ORCAMENTO, criado, MECANICO, criado, null, "obs", null, null, criado, 3L, List.of(it));

        assertThat(os.getId()).isEqualTo(id);
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(os.getStatusEntrega()).isEqualTo(StatusEntrega.NAO_LIBERADA);
        assertThat(os.getVersion()).isEqualTo(3L);
        assertThat(os.getCreatedAt()).isEqualTo(criado);
        assertThat(os.getItens()).containsExactly(it);
        assertThat(OrdemDeServico.reconstitute(id, CLIENTE, VEICULO, "d", StatusOS.RECEBIDA, null, null, null,
                null, null, null, null, null, null, null, criado, 0L, null).getItens()).isEmpty();
    }

    // ── atualização e itens ──

    @Test
    void atualizarSoEmRecebida() {
        OrdemDeServico os = nova();
        UUID outroCliente = UUID.randomUUID();
        os.atualizar(outroCliente, VEICULO, " nova descrição ");

        assertThat(os.getClienteUuid()).isEqualTo(outroCliente);
        assertThat(os.getDescricaoProblema()).isEqualTo("nova descrição");
        assertStatus(() -> os.atualizar(null, VEICULO, "x"), 400);

        os.iniciarDiagnostico();
        assertStatus(() -> os.atualizar(CLIENTE, VEICULO, "x"), 422);
    }

    @Test
    void itensEditaveisEmRecebidaEDiagnosticoCalculamTotal() {
        OrdemDeServico os = nova();
        os.adicionarItem(item("A", "10.00", 2));
        os.iniciarDiagnostico();
        os.adicionarItem(item("B", "5.50", 1));

        assertThat(os.getItens()).hasSize(2);
        assertThat(os.getValorTotalItens()).isEqualByComparingTo("25.50");

        os.substituirItens(List.of(item("C", "1.00", 1)));
        assertThat(os.getItens()).hasSize(1);
        os.substituirItens(null);
        assertThat(os.getItens()).isEmpty();
    }

    @Test
    void itensRejeitamNuloEListaNaoModificavel() {
        OrdemDeServico os = nova();
        assertStatus(() -> os.adicionarItem(null), 400);
        assertStatus(() -> os.substituirItens(java.util.Arrays.asList(item("A", "1", 1), null)), 400);
        ItemOs it = item("A", "1", 1);
        assertThatThrownBy(() -> os.getItens().add(it)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void itensBloqueadosDepoisDoDiagnostico() {
        OrdemDeServico os = emDiagnosticoComItem();
        os.finalizarDiagnostico();

        assertStatus(() -> os.adicionarItem(item("X", "1", 1)), 422);
        assertStatus(() -> os.substituirItens(List.of()), 422);
    }

    // ── ciclo de vida ──

    @Test
    void fluxoCompletoAteEntrega() {
        OrdemDeServico os = finalizada();
        assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);
        assertThat(os.getOrcamentoUuid()).isEqualTo(ORCAMENTO);
        assertThat(os.getDataAprovacao()).isNotNull();
        assertThat(os.getMecanicoUuid()).isEqualTo(MECANICO);
        assertThat(os.getExecucaoIniciadaEm()).isNotNull();
        assertThat(os.getExecucaoFinalizadaEm()).isNotNull();
        assertThat(os.getObservacaoExecucao()).isEqualTo("pronto");

        os.liberarEntrega();
        assertThat(os.getStatusEntrega()).isEqualTo(StatusEntrega.LIBERADA_PARA_ENTREGA);

        os.entregar(" João ");
        assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);
        assertThat(os.getStatusEntrega()).isEqualTo(StatusEntrega.ENTREGUE);
        assertThat(os.getRecebidoPor()).isEqualTo("João");
        assertThat(os.getEntregueEm()).isNotNull();
    }

    @Test
    void finalizarDiagnosticoExigeItens() {
        OrdemDeServico os = nova();
        os.iniciarDiagnostico();
        assertStatus(os::finalizarDiagnostico, 422);
    }

    @Test
    void transicoesForaDeOrdemSaoRejeitadas() {
        assertStatus(() -> nova().finalizarDiagnostico(), 422);
        assertStatus(() -> nova().registrarAprovacaoOrcamento(ORCAMENTO), 422);
        assertStatus(() -> nova().iniciarExecucao(MECANICO, null), 422);
        assertStatus(() -> nova().finalizarExecucao(null), 422);
        OrdemDeServico os = nova();
        os.iniciarDiagnostico();
        assertStatus(os::iniciarDiagnostico, 422);
    }

    @Test
    void aprovacaoEExecucaoValidamArgumentos() {
        OrdemDeServico os = emDiagnosticoComItem();
        os.finalizarDiagnostico();
        assertStatus(() -> os.registrarAprovacaoOrcamento(null), 400);
        os.registrarAprovacaoOrcamento(ORCAMENTO);
        assertStatus(() -> os.iniciarExecucao(null, "x"), 400);
    }

    @Test
    void finalizarExecucaoSemObservacaoMantemAnterior() {
        OrdemDeServico os = aguardandoExecucao();
        os.iniciarExecucao(MECANICO, " inicio ");
        os.finalizarExecucao("  ");

        assertThat(os.getObservacaoExecucao()).isEqualTo("inicio");
    }

    // ── cancelamento ──

    @Test
    void cancelarGuardaMotivoEEhTerminal() {
        OrdemDeServico os = aguardandoExecucao();
        os.cancelar(" cliente desistiu ");

        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        assertThat(os.getMotivoCancelamento()).isEqualTo("cliente desistiu");
        assertStatus(os::iniciarDiagnostico, 422);
        assertStatus(() -> os.cancelar("outra"), 422);
        assertStatus(() -> os.entregar("João"), 422);
    }

    @Test
    void cancelarExigeMotivoENaoPermiteFinalizada() {
        assertStatus(() -> nova().cancelar(" "), 400);
        assertStatus(() -> nova().cancelar(null), 400);
        assertStatus(() -> finalizada().cancelar("tarde demais"), 422);
    }

    // ── entrega ──

    @Test
    void entregarExigeRecebedorLiberacaoEStatusFinalizada() {
        OrdemDeServico os = finalizada();
        assertStatus(() -> os.entregar(" "), 400);
        assertStatus(() -> os.entregar("João"), 422); // pagamento ainda não confirmado
        assertStatus(() -> nova().entregar("João"), 422);
    }

    @Test
    void liberarEntregaEhIdempotenteEExigeFinalizada() {
        assertStatus(() -> nova().liberarEntrega(), 422);

        OrdemDeServico os = finalizada();
        os.liberarEntrega();
        os.liberarEntrega();
        assertThat(os.getStatusEntrega()).isEqualTo(StatusEntrega.LIBERADA_PARA_ENTREGA);

        os.entregar("João");
        os.liberarEntrega();
        assertThat(os.getStatusEntrega()).isEqualTo(StatusEntrega.ENTREGUE);
    }
}
