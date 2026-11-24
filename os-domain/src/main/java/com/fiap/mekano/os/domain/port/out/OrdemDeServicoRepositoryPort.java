package com.fiap.mekano.os.domain.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.os.domain.model.OrdemDeServico;
import com.fiap.mekano.os.domain.model.StatusOS;

/** Driven port de persistência para o agregado {@link OrdemDeServico} (inclui seus itens). */
public interface OrdemDeServicoRepositoryPort {

    OrdemDeServico save(OrdemDeServico ordemDeServico);

    Optional<OrdemDeServico> findById(UUID id);

    List<OrdemDeServico> findAll(int page, int size, String sort);

    long countAll();

    List<OrdemDeServico> findAllWithFilters(StatusOS status, UUID clienteUuid, UUID veiculoUuid,
                                            LocalDateTime dataInicio, LocalDateTime dataFim,
                                            int page, int size);

    Optional<Double> calcularTempoMedioExecucao(LocalDateTime dataInicio, LocalDateTime dataFim);

    Map<UUID, Double> calcularTempoMedioPorMecanico(LocalDateTime dataInicio, LocalDateTime dataFim);

    boolean existsByClienteUuidAndStatusIn(UUID clienteUuid, List<StatusOS> statuses);

    void markAsDeleted(UUID id);
}
