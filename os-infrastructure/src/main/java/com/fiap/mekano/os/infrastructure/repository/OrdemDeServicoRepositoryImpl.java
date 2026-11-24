package com.fiap.mekano.os.infrastructure.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.microprofile.faulttolerance.Retry;

import com.fiap.mekano.os.domain.exception.AppException;
import com.fiap.mekano.os.domain.model.OrdemDeServico;
import com.fiap.mekano.os.domain.model.StatusOS;
import com.fiap.mekano.os.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.os.infrastructure.cache.CacheNames;
import com.fiap.mekano.os.infrastructure.entity.ItemOsEntity;
import com.fiap.mekano.os.infrastructure.entity.OrdemDeServicoEntity;
import com.fiap.mekano.os.infrastructure.mapper.OrdemDeServicoEntityMapper;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.OptimisticLockException;

@ApplicationScoped
public class OrdemDeServicoRepositoryImpl implements OrdemDeServicoRepositoryPort {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "status", "descricaoProblema");

    private static final String STATUS_PRIORITY_ORDER =
            " ORDER BY CASE status"
            + " WHEN 'EM_EXECUCAO' THEN 0"
            + " WHEN 'AGUARDANDO_APROVACAO' THEN 1"
            + " WHEN 'EM_DIAGNOSTICO' THEN 2"
            + " WHEN 'RECEBIDA' THEN 3"
            + " WHEN 'AGUARDANDO_EXECUCAO' THEN 4"
            + " ELSE 5 END ASC, createdAt ASC";

    private final OrdemDeServicoPanacheRepository panacheRepository;
    private final OrdemDeServicoEntityMapper mapper;
    private final Cache cache;

    public OrdemDeServicoRepositoryImpl(OrdemDeServicoPanacheRepository panacheRepository,
                                        OrdemDeServicoEntityMapper mapper,
                                        @CacheName(CacheNames.ORDENS_SERVICO) Cache cache) {
        this.panacheRepository = panacheRepository;
        this.mapper = mapper;
        this.cache = cache;
    }

    @Override
    public OrdemDeServico save(OrdemDeServico os) {
        cache.invalidate(os.getId()).await().indefinitely();
        Optional<OrdemDeServicoEntity> existing = panacheRepository
                .find("uuid = ?1", os.getId()).firstResultOptional();

        try {
            if (existing.isPresent()) {
                OrdemDeServicoEntity managed = existing.get();
                if (os.getVersion() != null && !os.getVersion().equals(managed.getVersion())) {
                    throw AppException.conflict("Conflito de versão — a OS foi atualizada por outro usuário");
                }
                aplicarCampos(managed, os);
                panacheRepository.flush();
                return mapper.toDomain(managed);
            }

            OrdemDeServicoEntity entity = mapper.toEntity(os);
            panacheRepository.persist(entity);
            panacheRepository.flush();
            return mapper.toDomain(entity);
        } catch (OptimisticLockException e) {
            throw AppException.conflict("Conflito de versão — a OS foi atualizada por outro usuário");
        }
    }

    @Override
    @Retry(maxRetries = 3)
    @CacheResult(cacheName = CacheNames.ORDENS_SERVICO)
    public Optional<OrdemDeServico> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 AND isActive = ?2", id, true)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public List<OrdemDeServico> findAll(int page, int size, String sort) {
        String sortValue = sort == null || sort.isBlank() ? "createdAt,desc" : sort;
        String[] sortParts = sortValue.split(",", 2);
        String sortField = sortParts[0].strip();
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            sortField = "createdAt";
        }
        boolean ascending = sortParts.length < 2 || "asc".equalsIgnoreCase(sortParts[1].strip());
        var direction = ascending ? Sort.Direction.Ascending : Sort.Direction.Descending;

        return panacheRepository.find("isActive = ?1", Sort.by(sortField).direction(direction), true)
                .page(Page.of(page, size)).list()
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count("isActive = ?1", true);
    }

    @Override
    public List<OrdemDeServico> findAllWithFilters(StatusOS status, UUID clienteUuid, UUID veiculoUuid,
                                                   LocalDateTime dataInicio, LocalDateTime dataFim,
                                                   int page, int size) {
        StringBuilder jpql = new StringBuilder("isActive = true");
        Map<String, Object> params = new HashMap<>();

        if (status != null) {
            jpql.append(" AND status = :status");
            params.put("status", status.name());
        }
        if (clienteUuid != null) {
            jpql.append(" AND clienteUuid = :clienteUuid");
            params.put("clienteUuid", clienteUuid);
        }
        if (veiculoUuid != null) {
            jpql.append(" AND veiculoUuid = :veiculoUuid");
            params.put("veiculoUuid", veiculoUuid);
        }
        if (dataInicio != null) {
            jpql.append(" AND createdAt >= :dataInicio");
            params.put("dataInicio", dataInicio);
        }
        if (dataFim != null) {
            jpql.append(" AND createdAt <= :dataFim");
            params.put("dataFim", dataFim);
        }

        jpql.append(STATUS_PRIORITY_ORDER);

        return panacheRepository.find(jpql.toString(), params)
                .page(Page.of(page, size)).list()
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Double> calcularTempoMedioExecucao(LocalDateTime dataInicio, LocalDateTime dataFim) {
        List<OrdemDeServicoEntity> finalizadas = buscarFinalizadasNoPeriodo(dataInicio, dataFim);
        if (finalizadas.isEmpty()) {
            return Optional.empty();
        }
        double media = finalizadas.stream()
                .mapToLong(e -> ChronoUnit.MINUTES.between(e.getExecucaoIniciadaEm(), e.getExecucaoFinalizadaEm()))
                .average()
                .orElse(0.0);
        return Optional.of(media);
    }

    @Override
    public Map<UUID, Double> calcularTempoMedioPorMecanico(LocalDateTime dataInicio, LocalDateTime dataFim) {
        return buscarFinalizadasNoPeriodo(dataInicio, dataFim).stream()
                .filter(e -> e.getMecanicoUuid() != null)
                .collect(Collectors.groupingBy(
                        OrdemDeServicoEntity::getMecanicoUuid,
                        LinkedHashMap::new,
                        Collectors.averagingLong(e -> ChronoUnit.MINUTES.between(
                                e.getExecucaoIniciadaEm(), e.getExecucaoFinalizadaEm()))));
    }

    @Override
    public boolean existsByClienteUuidAndStatusIn(UUID clienteUuid, List<StatusOS> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return false;
        }
        List<String> nomes = statuses.stream().map(StatusOS::name).toList();
        return panacheRepository.count("clienteUuid = ?1 AND status IN ?2 AND isActive = true", clienteUuid, nomes) > 0;
    }

    @Override
    public void markAsDeleted(UUID id) {
        cache.invalidate(id).await().indefinitely();
        panacheRepository.find("uuid = ?1 AND isActive = true", id)
                .firstResultOptional()
                .ifPresent(entity -> {
                    entity.setIsActive(false);
                    entity.setDeletedAt(LocalDateTime.now());
                    panacheRepository.flush();
                });
    }

    private List<OrdemDeServicoEntity> buscarFinalizadasNoPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        StringBuilder jpql = new StringBuilder(
                "isActive = true AND execucaoIniciadaEm IS NOT NULL AND execucaoFinalizadaEm IS NOT NULL");
        Map<String, Object> params = new HashMap<>();
        if (dataInicio != null) {
            jpql.append(" AND execucaoFinalizadaEm >= :dataInicio");
            params.put("dataInicio", dataInicio);
        }
        if (dataFim != null) {
            jpql.append(" AND execucaoFinalizadaEm <= :dataFim");
            params.put("dataFim", dataFim);
        }
        return panacheRepository.find(jpql.toString(), params).list();
    }

    private void aplicarCampos(OrdemDeServicoEntity managed, OrdemDeServico os) {
        managed.setClienteUuid(os.getClienteUuid());
        managed.setVeiculoUuid(os.getVeiculoUuid());
        managed.setDescricaoProblema(os.getDescricaoProblema());
        managed.setStatus(os.getStatus().name());
        managed.setStatusEntrega(os.getStatusEntrega().name());
        managed.setMotivoCancelamento(os.getMotivoCancelamento());
        managed.setOrcamentoUuid(os.getOrcamentoUuid());
        managed.setDataAprovacao(os.getDataAprovacao());
        managed.setMecanicoUuid(os.getMecanicoUuid());
        managed.setExecucaoIniciadaEm(os.getExecucaoIniciadaEm());
        managed.setExecucaoFinalizadaEm(os.getExecucaoFinalizadaEm());
        managed.setObservacaoExecucao(os.getObservacaoExecucao());
        managed.setEntregueEm(os.getEntregueEm());
        managed.setRecebidoPor(os.getRecebidoPor());
        managed.getItens().clear();
        panacheRepository.flush();
        List<ItemOsEntity> itens = os.getItens().stream().map(mapper::toItemEntity).toList();
        managed.substituirItens(itens);
    }
}
