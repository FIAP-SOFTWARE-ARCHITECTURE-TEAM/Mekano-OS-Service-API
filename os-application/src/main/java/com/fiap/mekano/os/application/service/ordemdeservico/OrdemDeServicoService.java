package com.fiap.mekano.os.application.service.ordemdeservico;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.os.domain.event.DiagnosticoFinalizadoEvent;
import com.fiap.mekano.os.domain.event.OSCanceladaEvent;
import com.fiap.mekano.os.domain.event.OSEntregueEvent;
import com.fiap.mekano.os.domain.event.OSExecucaoIniciadaEvent;
import com.fiap.mekano.os.domain.event.OSFinalizadaEvent;
import com.fiap.mekano.os.domain.event.OrdemDeServicoCriadaEvent;
import com.fiap.mekano.os.domain.event.OsTransitionedEvent;
import com.fiap.mekano.os.domain.exception.AppException;
import com.fiap.mekano.os.domain.exception.Messages;
import com.fiap.mekano.os.domain.model.ItemOs;
import com.fiap.mekano.os.domain.model.OrdemDeServico;
import com.fiap.mekano.os.domain.model.StatusOS;
import com.fiap.mekano.os.domain.port.in.CancelarOSCommand;
import com.fiap.mekano.os.domain.port.in.CreateItemOsCommand;
import com.fiap.mekano.os.domain.port.in.CreateOrdemDeServicoCommand;
import com.fiap.mekano.os.domain.port.in.EntregarOSCommand;
import com.fiap.mekano.os.domain.port.in.FinalizarDiagnosticoCommand;
import com.fiap.mekano.os.domain.port.in.FinalizarExecucaoCommand;
import com.fiap.mekano.os.domain.port.in.IniciarExecucaoCommand;
import com.fiap.mekano.os.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.os.domain.port.out.CatalogoQueryPort;
import com.fiap.mekano.os.domain.port.out.ClienteQueryPort;
import com.fiap.mekano.os.domain.port.out.EventPublisher;
import com.fiap.mekano.os.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.os.domain.port.out.VeiculoQueryPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Caso de uso de {@link OrdemDeServico}.
 * Orquestra validações cross-contexto via query ports, transições no domínio
 * e publicação de eventos via {@link EventPublisher} (Outbox na mesma transação).
 */
@ApplicationScoped
public class OrdemDeServicoService implements OrdemDeServicoServicePort {

    private static final List<StatusOS> STATUS_ATIVOS = List.of(
            StatusOS.RECEBIDA,
            StatusOS.EM_DIAGNOSTICO,
            StatusOS.AGUARDANDO_APROVACAO,
            StatusOS.AGUARDANDO_EXECUCAO,
            StatusOS.EM_EXECUCAO);

    private final OrdemDeServicoRepositoryPort repository;
    private final EventPublisher eventPublisher;
    private final ClienteQueryPort clienteQuery;
    private final VeiculoQueryPort veiculoQuery;
    private final CatalogoQueryPort catalogoQuery;

    public OrdemDeServicoService(OrdemDeServicoRepositoryPort repository,
                                 EventPublisher eventPublisher,
                                 ClienteQueryPort clienteQuery,
                                 VeiculoQueryPort veiculoQuery,
                                 CatalogoQueryPort catalogoQuery) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clienteQuery = clienteQuery;
        this.veiculoQuery = veiculoQuery;
        this.catalogoQuery = catalogoQuery;
    }

    @Override
    @Transactional
    public OrdemDeServico create(CreateOrdemDeServicoCommand command) {
        validarClienteEVeiculo(command.clienteUuid(), command.veiculoUuid());

        OrdemDeServico os = OrdemDeServico.create(
                command.clienteUuid(), command.veiculoUuid(), command.descricaoProblema());
        resolverItens(command.itens()).forEach(os::adicionarItem);

        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(OrdemDeServicoCriadaEvent.of(
                saved.getId(), saved.getClienteUuid(), saved.getVeiculoUuid(), saved.getDescricaoProblema()));
        eventPublisher.publish(OsTransitionedEvent.of(
                saved.getId(), null, StatusOS.RECEBIDA, "OS criada"));
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico update(UUID id, CreateOrdemDeServicoCommand command) {
        OrdemDeServico os = findById(id);
        validarClienteEVeiculo(command.clienteUuid(), command.veiculoUuid());

        os.atualizar(command.clienteUuid(), command.veiculoUuid(), command.descricaoProblema());
        os.substituirItens(resolverItens(command.itens()));
        return repository.save(os);
    }

    @Override
    public OrdemDeServico findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> AppException.notFound(Messages.get("os.not.found", id)));
    }

    @Override
    public List<OrdemDeServico> findAll(int page, int size, String sort) {
        return repository.findAll(page, size, sort);
    }

    @Override
    public long countAll() {
        return repository.countAll();
    }

    @Override
    public List<OrdemDeServico> findAllWithFilters(StatusOS status, UUID clienteUuid, UUID veiculoUuid,
                                                   LocalDateTime dataInicio, LocalDateTime dataFim,
                                                   int page, int size) {
        return repository.findAllWithFilters(status, clienteUuid, veiculoUuid, dataInicio, dataFim, page, size);
    }

    @Override
    @Transactional
    public OrdemDeServico iniciarDiagnostico(UUID id) {
        OrdemDeServico os = findById(id);
        StatusOS anterior = os.getStatus();
        os.iniciarDiagnostico();
        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(OsTransitionedEvent.of(
                saved.getId(), anterior, saved.getStatus(), "Diagnóstico iniciado"));
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico finalizarDiagnostico(FinalizarDiagnosticoCommand command) {
        OrdemDeServico os = findById(command.osUuid());
        StatusOS anterior = os.getStatus();

        resolverItens(command.itens()).forEach(os::adicionarItem);
        os.finalizarDiagnostico();

        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(DiagnosticoFinalizadoEvent.of(
                saved.getId(), saved.getClienteUuid(), command.descricao(),
                saved.getItens(), saved.getValorTotalItens()));
        eventPublisher.publish(OsTransitionedEvent.of(
                saved.getId(), anterior, saved.getStatus(), command.descricao()));
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico iniciarExecucao(IniciarExecucaoCommand command) {
        OrdemDeServico os = findById(command.osUuid());
        StatusOS anterior = os.getStatus();
        os.iniciarExecucao(command.mecanicoUuid(), command.observacao());

        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(OSExecucaoIniciadaEvent.of(
                saved.getId(), command.mecanicoUuid(), command.observacao()));
        eventPublisher.publish(OsTransitionedEvent.of(
                saved.getId(), anterior, saved.getStatus(), command.observacao()));
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico finalizarExecucao(FinalizarExecucaoCommand command) {
        OrdemDeServico os = findById(command.osUuid());
        StatusOS anterior = os.getStatus();
        os.finalizarExecucao(command.observacao());

        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(OSFinalizadaEvent.of(
                saved.getId(), saved.getClienteUuid(), saved.getOrcamentoUuid(), saved.getValorTotalItens()));
        eventPublisher.publish(OsTransitionedEvent.of(
                saved.getId(), anterior, saved.getStatus(), command.observacao()));
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico cancelar(CancelarOSCommand command) {
        return cancelarInterno(findById(command.osUuid()), command.motivo());
    }

    @Override
    @Transactional
    public OrdemDeServico entregar(EntregarOSCommand command) {
        OrdemDeServico os = findById(command.osUuid());
        StatusOS anterior = os.getStatus();
        os.entregar(command.recebidoPor());

        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(OSEntregueEvent.of(saved.getId(), command.recebidoPor()));
        eventPublisher.publish(OsTransitionedEvent.of(
                saved.getId(), anterior, saved.getStatus(), "Entregue a " + saved.getRecebidoPor()));
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico registrarAprovacaoOrcamento(UUID osUuid, UUID orcamentoUuid) {
        OrdemDeServico os = findById(osUuid);
        if (os.getStatus() != StatusOS.AGUARDANDO_APROVACAO
                && orcamentoUuid != null && orcamentoUuid.equals(os.getOrcamentoUuid())) {
            return os;
        }
        StatusOS anterior = os.getStatus();
        os.registrarAprovacaoOrcamento(orcamentoUuid);

        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(OsTransitionedEvent.of(
                saved.getId(), anterior, saved.getStatus(), "Orçamento aprovado: " + orcamentoUuid));
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico cancelarPorOrcamento(UUID osUuid, String motivo) {
        OrdemDeServico os = findById(osUuid);
        if (os.getStatus() == StatusOS.CANCELADA) {
            return os;
        }
        return cancelarInterno(os, motivo);
    }

    @Override
    @Transactional
    public OrdemDeServico liberarEntregaPorPagamento(UUID osUuid) {
        OrdemDeServico os = findById(osUuid);
        os.liberarEntrega();
        return repository.save(os);
    }

    @Override
    public Optional<Double> calcularTempoMedioExecucao(LocalDateTime dataInicio, LocalDateTime dataFim) {
        return repository.calcularTempoMedioExecucao(dataInicio, dataFim);
    }

    @Override
    public Map<UUID, Double> calcularTempoMedioPorMecanico(LocalDateTime dataInicio, LocalDateTime dataFim) {
        return repository.calcularTempoMedioPorMecanico(dataInicio, dataFim);
    }

    @Override
    public boolean clientePossuiOsAtiva(UUID clienteUuid) {
        return repository.existsByClienteUuidAndStatusIn(clienteUuid, STATUS_ATIVOS);
    }

    // ───────────── Helpers ─────────────

    private OrdemDeServico cancelarInterno(OrdemDeServico os, String motivo) {
        StatusOS anterior = os.getStatus();
        os.cancelar(motivo);
        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(OSCanceladaEvent.of(
                saved.getId(), saved.getOrcamentoUuid(), saved.getMotivoCancelamento()));
        eventPublisher.publish(OsTransitionedEvent.of(
                saved.getId(), anterior, saved.getStatus(), saved.getMotivoCancelamento()));
        return saved;
    }

    private void validarClienteEVeiculo(UUID clienteUuid, UUID veiculoUuid) {
        if (!clienteQuery.existsActiveById(clienteUuid)) {
            throw AppException.notFound(Messages.get("cliente.not.found", clienteUuid));
        }
        if (!veiculoQuery.existsActiveByIdAndCliente(veiculoUuid, clienteUuid)) {
            throw AppException.notFound(Messages.get("veiculo.not.found", veiculoUuid));
        }
    }

    private List<ItemOs> resolverItens(List<CreateItemOsCommand> comandos) {
        if (comandos == null || comandos.isEmpty()) {
            return List.of();
        }
        return comandos.stream().map(cmd -> {
            var snapshot = catalogoQuery.buscarItem(cmd.referenciaUuid(), cmd.tipo())
                    .filter(CatalogoQueryPort.CatalogoItemSnapshot::ativo)
                    .orElseThrow(() -> AppException.notFound(
                            Messages.get("catalogo.item.not.found", cmd.tipo(), cmd.referenciaUuid())));
            return ItemOs.create(
                    snapshot.referenciaUuid(),
                    snapshot.tipo(),
                    snapshot.descricao(),
                    snapshot.valorUnitario(),
                    cmd.quantidade());
        }).toList();
    }
}
