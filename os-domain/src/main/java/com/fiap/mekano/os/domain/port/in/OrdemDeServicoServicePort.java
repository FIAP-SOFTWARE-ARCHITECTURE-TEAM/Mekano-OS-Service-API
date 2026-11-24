package com.fiap.mekano.os.domain.port.in;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.os.domain.model.OrdemDeServico;
import com.fiap.mekano.os.domain.model.StatusOS;

/**
 * Driving port (entrada) para os casos de uso de Ordem de Serviço.
 * Inclui as operações da API REST e os handlers disparados por eventos de Orçamento/Pagamento.
 */
public interface OrdemDeServicoServicePort {

    OrdemDeServico create(CreateOrdemDeServicoCommand command);

    OrdemDeServico update(UUID id, CreateOrdemDeServicoCommand command);

    OrdemDeServico findById(UUID id);

    List<OrdemDeServico> findAll(int page, int size, String sort);

    long countAll();

    List<OrdemDeServico> findAllWithFilters(StatusOS status, UUID clienteUuid, UUID veiculoUuid,
                                            LocalDateTime dataInicio, LocalDateTime dataFim,
                                            int page, int size);

    OrdemDeServico iniciarDiagnostico(UUID id);

    OrdemDeServico finalizarDiagnostico(FinalizarDiagnosticoCommand command);

    OrdemDeServico iniciarExecucao(IniciarExecucaoCommand command);

    OrdemDeServico finalizarExecucao(FinalizarExecucaoCommand command);

    OrdemDeServico cancelar(CancelarOSCommand command);

    OrdemDeServico entregar(EntregarOSCommand command);

    /** Handler para evento {@code OrcamentoAprovado} vindo do serviço de Orçamento. */
    OrdemDeServico registrarAprovacaoOrcamento(UUID osUuid, UUID orcamentoUuid);

    /** Handler para eventos {@code OrcamentoReprovado} / {@code OrcamentoExpirado}. */
    OrdemDeServico cancelarPorOrcamento(UUID osUuid, String motivo);

    /** Handler para evento {@code PagamentoConfirmado} vindo do serviço de Pagamento. */
    OrdemDeServico liberarEntregaPorPagamento(UUID osUuid);

    Optional<Double> calcularTempoMedioExecucao(LocalDateTime dataInicio, LocalDateTime dataFim);

    Map<UUID, Double> calcularTempoMedioPorMecanico(LocalDateTime dataInicio, LocalDateTime dataFim);

    boolean clientePossuiOsAtiva(UUID clienteUuid);
}
