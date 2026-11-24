package com.fiap.mekano.os.application.service.ordemdeservico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fiap.mekano.os.domain.event.DiagnosticoFinalizadoEvent;
import com.fiap.mekano.os.domain.event.OSCanceladaEvent;
import com.fiap.mekano.os.domain.event.OSEntregueEvent;
import com.fiap.mekano.os.domain.event.OSExecucaoIniciadaEvent;
import com.fiap.mekano.os.domain.event.OSFinalizadaEvent;
import com.fiap.mekano.os.domain.event.OrdemDeServicoCriadaEvent;
import com.fiap.mekano.os.domain.event.OsDomainEvent;
import com.fiap.mekano.os.domain.event.OsTransitionedEvent;
import com.fiap.mekano.os.domain.exception.AppException;
import com.fiap.mekano.os.domain.model.ItemOs;
import com.fiap.mekano.os.domain.model.OrdemDeServico;
import com.fiap.mekano.os.domain.model.StatusEntrega;
import com.fiap.mekano.os.domain.model.StatusOS;
import com.fiap.mekano.os.domain.model.TipoItemOs;
import com.fiap.mekano.os.domain.port.in.CancelarOSCommand;
import com.fiap.mekano.os.domain.port.in.CreateItemOsCommand;
import com.fiap.mekano.os.domain.port.in.CreateOrdemDeServicoCommand;
import com.fiap.mekano.os.domain.port.in.EntregarOSCommand;
import com.fiap.mekano.os.domain.port.in.FinalizarDiagnosticoCommand;
import com.fiap.mekano.os.domain.port.in.FinalizarExecucaoCommand;
import com.fiap.mekano.os.domain.port.in.IniciarExecucaoCommand;
import com.fiap.mekano.os.domain.port.out.CatalogoQueryPort;
import com.fiap.mekano.os.domain.port.out.CatalogoQueryPort.CatalogoItemSnapshot;
import com.fiap.mekano.os.domain.port.out.ClienteQueryPort;
import com.fiap.mekano.os.domain.port.out.EventPublisher;
import com.fiap.mekano.os.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.os.domain.port.out.VeiculoQueryPort;

@ExtendWith(MockitoExtension.class)
class OrdemDeServicoServiceTest {

    private static final UUID CLIENTE = UUID.randomUUID();
    private static final UUID VEICULO = UUID.randomUUID();
    private static final UUID PECA = UUID.randomUUID();
    private static final UUID SERVICO = UUID.randomUUID();
    private static final UUID MECANICO = UUID.randomUUID();
    private static final UUID ORCAMENTO = UUID.randomUUID();

    @Mock
    OrdemDeServicoRepositoryPort repository;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    ClienteQueryPort clienteQuery;
    @Mock
    VeiculoQueryPort veiculoQuery;
    @Mock
    CatalogoQueryPort catalogoQuery;

    @InjectMocks
    OrdemDeServicoService service;

    @BeforeEach
    void stubSave() {
        // lenient not needed if we stub only in tests that call save, or use lenient()
    }

    private void stubClienteEVeiculoValidos() {
        when(clienteQuery.existsActiveById(CLIENTE)).thenReturn(true);
        when(veiculoQuery.existsActiveByIdAndCliente(VEICULO, CLIENTE)).thenReturn(true);
    }

    private void stubSaveRetornaProprio() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── create & update ──

    @Test
    void createValidaClienteVeiculoResolveSnapshotEPublicaEventos() {
        stubClienteEVeiculoValidos();
        stubSaveRetornaProprio();
        when(catalogoQuery.buscarItem(PECA, TipoItemOs.PECA))
                .thenReturn(Optional.of(new CatalogoItemSnapshot(
                        PECA, TipoItemOs.PECA, "Filtro de óleo", new BigDecimal("45.00"), true)));

        var cmd = new CreateOrdemDeServicoCommand(
                CLIENTE, VEICULO, "Troca de óleo",
                List.of(new CreateItemOsCommand(PECA, TipoItemOs.PECA, 2L)));

        OrdemDeServico criada = service.create(cmd);

        assertThat(criada.getStatus()).isEqualTo(StatusOS.RECEBIDA);
        assertThat(criada.getItens()).hasSize(1);
        assertThat(criada.getValorTotalItens()).isEqualByComparingTo("90.00");

        ArgumentCaptor<OsDomainEvent> captor = ArgumentCaptor.forClass(OsDomainEvent.class);
        verify(eventPublisher, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues())
                .anySatisfy(e -> assertThat(e).isInstanceOf(OrdemDeServicoCriadaEvent.class))
                .anySatisfy(e -> assertThat(e).isInstanceOf(OsTransitionedEvent.class));
    }

    @Test
    void createFalhaQuandoClienteOuVeiculoOuCatalogoNaoExiste() {
        when(clienteQuery.existsActiveById(CLIENTE)).thenReturn(false);
        var cmd = new CreateOrdemDeServicoCommand(CLIENTE, VEICULO, "desc", List.of());

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(404);

        when(clienteQuery.existsActiveById(CLIENTE)).thenReturn(true);
        when(veiculoQuery.existsActiveByIdAndCliente(VEICULO, CLIENTE)).thenReturn(false);

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(404);

        when(veiculoQuery.existsActiveByIdAndCliente(VEICULO, CLIENTE)).thenReturn(true);
        when(catalogoQuery.buscarItem(PECA, TipoItemOs.PECA))
                .thenReturn(Optional.of(new CatalogoItemSnapshot(PECA, TipoItemOs.PECA, "Inativo", BigDecimal.ONE, false)));

        var cmdComItem = new CreateOrdemDeServicoCommand(
                CLIENTE, VEICULO, "desc", List.of(new CreateItemOsCommand(PECA, TipoItemOs.PECA, 1L)));

        assertThatThrownBy(() -> service.create(cmdComItem))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(404);
    }

    @Test
    void updateSubstituiItensEDadosEmRecebida() {
        OrdemDeServico os = OrdemDeServico.create(CLIENTE, VEICULO, "Antigo");
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));
        stubClienteEVeiculoValidos();
        stubSaveRetornaProprio();
        when(catalogoQuery.buscarItem(SERVICO, TipoItemOs.SERVICO))
                .thenReturn(Optional.of(new CatalogoItemSnapshot(
                        SERVICO, TipoItemOs.SERVICO, "Alinhamento", new BigDecimal("120.00"), true)));

        OrdemDeServico atualizada = service.update(os.getId(), new CreateOrdemDeServicoCommand(
                CLIENTE, VEICULO, "Novo", List.of(new CreateItemOsCommand(SERVICO, TipoItemOs.SERVICO, 1L))));

        assertThat(atualizada.getDescricaoProblema()).isEqualTo("Novo");
        assertThat(atualizada.getItens()).hasSize(1);
    }

    // ── queries ──

    @Test
    void findByIdLanca404QuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(404);
    }

    @Test
    void consultasDelegamAoRepositorio() {
        LocalDateTime ini = LocalDateTime.now().minusDays(7);
        LocalDateTime fim = LocalDateTime.now();

        when(repository.findAll(0, 10, "createdAt")).thenReturn(List.of());
        when(repository.countAll()).thenReturn(5L);
        when(repository.findAllWithFilters(StatusOS.RECEBIDA, CLIENTE, VEICULO, ini, fim, 0, 10)).thenReturn(List.of());
        when(repository.calcularTempoMedioExecucao(ini, fim)).thenReturn(Optional.of(42.5));
        when(repository.calcularTempoMedioPorMecanico(ini, fim)).thenReturn(Map.of(MECANICO, 30.0));
        when(repository.existsByClienteUuidAndStatusIn(any(), any())).thenReturn(true);

        assertThat(service.findAll(0, 10, "createdAt")).isEmpty();
        assertThat(service.countAll()).isEqualTo(5L);
        assertThat(service.findAllWithFilters(StatusOS.RECEBIDA, CLIENTE, VEICULO, ini, fim, 0, 10)).isEmpty();
        assertThat(service.calcularTempoMedioExecucao(ini, fim)).contains(42.5);
        assertThat(service.calcularTempoMedioPorMecanico(ini, fim)).containsEntry(MECANICO, 30.0);
        assertThat(service.clientePossuiOsAtiva(CLIENTE)).isTrue();
    }

    // ── ciclo de vida & eventos ──

    @Test
    void iniciarEFinalizarDiagnosticoPublicamEventos() {
        OrdemDeServico os = OrdemDeServico.create(CLIENTE, VEICULO, "Freio");
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));
        stubSaveRetornaProprio();
        when(catalogoQuery.buscarItem(PECA, TipoItemOs.PECA))
                .thenReturn(Optional.of(new CatalogoItemSnapshot(
                        PECA, TipoItemOs.PECA, "Pastilha", new BigDecimal("150.00"), true)));

        service.iniciarDiagnostico(os.getId());
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);

        OrdemDeServico aposDiag = service.finalizarDiagnostico(new FinalizarDiagnosticoCommand(
                os.getId(), "Trocar pastilhas",
                List.of(new CreateItemOsCommand(PECA, TipoItemOs.PECA, 1L))));

        assertThat(aposDiag.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        ArgumentCaptor<OsDomainEvent> captor = ArgumentCaptor.forClass(OsDomainEvent.class);
        verify(eventPublisher, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues())
                .anySatisfy(e -> {
                    assertThat(e).isInstanceOf(DiagnosticoFinalizadoEvent.class);
                    var df = (DiagnosticoFinalizadoEvent) e;
                    assertThat(df.valorTotal()).isEqualByComparingTo("150.00");
                    assertThat(df.itens()).hasSize(1);
                });
    }

    @Test
    void eventosDeOrcamentoEPagamentoEExecucaoEEntrega() {
        OrdemDeServico os = OrdemDeServico.create(CLIENTE, VEICULO, "Motor");
        os.iniciarDiagnostico();
        os.adicionarItem(ItemOs.create(SERVICO, TipoItemOs.SERVICO, "Revisão", new BigDecimal("300.00"), 1L));
        os.finalizarDiagnostico();

        when(repository.findById(os.getId())).thenReturn(Optional.of(os));
        stubSaveRetornaProprio();

        // Aprovação de orçamento (e chamada duplicada idempotente)
        service.registrarAprovacaoOrcamento(os.getId(), ORCAMENTO);
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_EXECUCAO);
        service.registrarAprovacaoOrcamento(os.getId(), ORCAMENTO);

        // Execução
        service.iniciarExecucao(new IniciarExecucaoCommand(os.getId(), MECANICO, "Iniciando"));
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);

        service.finalizarExecucao(new FinalizarExecucaoCommand(os.getId(), "Concluído"));
        assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);

        // Pagamento confirmado libera entrega
        service.liberarEntregaPorPagamento(os.getId());
        assertThat(os.getStatusEntrega()).isEqualTo(StatusEntrega.LIBERADA_PARA_ENTREGA);

        // Entrega
        service.entregar(new EntregarOSCommand(os.getId(), "Carlos"));
        assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);

        ArgumentCaptor<OsDomainEvent> captor = ArgumentCaptor.forClass(OsDomainEvent.class);
        verify(eventPublisher, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues())
                .anySatisfy(e -> assertThat(e).isInstanceOf(OSExecucaoIniciadaEvent.class))
                .anySatisfy(e -> assertThat(e).isInstanceOf(OSFinalizadaEvent.class))
                .anySatisfy(e -> assertThat(e).isInstanceOf(OSEntregueEvent.class));
    }

    @Test
    void cancelarECancelarPorOrcamentoIdempotente() {
        OrdemDeServico os = OrdemDeServico.create(CLIENTE, VEICULO, "Suspensão");
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));
        stubSaveRetornaProprio();

        service.cancelar(new CancelarOSCommand(os.getId(), "Cliente desistiu"));
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);

        // Reentrega de evento de reprovação após já cancelada deve ser no-op
        OrdemDeServico idempotente = service.cancelarPorOrcamento(os.getId(), "Expirado");
        assertThat(idempotente.getStatus()).isEqualTo(StatusOS.CANCELADA);
        verify(repository, org.mockito.Mockito.times(1)).save(os);

        ArgumentCaptor<OsDomainEvent> captor = ArgumentCaptor.forClass(OsDomainEvent.class);
        verify(eventPublisher, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues())
                .anySatisfy(e -> assertThat(e).isInstanceOf(OSCanceladaEvent.class));
    }

    @Test
    void cancelarPorOrcamentoQuandoAindaNaoCanceladaCancelaEPublica() {
        OrdemDeServico os = OrdemDeServico.create(CLIENTE, VEICULO, "Ar condicionado");
        os.iniciarDiagnostico();
        os.adicionarItem(ItemOs.create(SERVICO, TipoItemOs.SERVICO, "Carga gás", new BigDecimal("200.00"), 1L));
        os.finalizarDiagnostico();

        when(repository.findById(os.getId())).thenReturn(Optional.of(os));
        stubSaveRetornaProprio();

        OrdemDeServico cancelada = service.cancelarPorOrcamento(os.getId(), "Orçamento reprovado");
        assertThat(cancelada.getStatus()).isEqualTo(StatusOS.CANCELADA);
        verify(eventPublisher, atLeastOnce()).publish(any(OSCanceladaEvent.class));
    }

    @Test
    void naoPublicaEventosQuandoTransicaoFalha() {
        OrdemDeServico os = OrdemDeServico.create(CLIENTE, VEICULO, "Freio");
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> service.finalizarDiagnostico(
                new FinalizarDiagnosticoCommand(os.getId(), "x", List.of())))
                .isInstanceOf(AppException.class);

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}
