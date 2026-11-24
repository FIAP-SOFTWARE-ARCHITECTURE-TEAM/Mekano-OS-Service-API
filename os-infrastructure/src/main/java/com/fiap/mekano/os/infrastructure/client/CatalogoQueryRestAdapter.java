package com.fiap.mekano.os.infrastructure.client;

import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fiap.mekano.os.domain.model.TipoItemOs;
import com.fiap.mekano.os.domain.port.out.CatalogoQueryPort;
import com.fiap.mekano.os.infrastructure.cache.CacheNames;

import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class CatalogoQueryRestAdapter implements CatalogoQueryPort {

    private final MekanoApiRestClient client;

    public CatalogoQueryRestAdapter(@RestClient MekanoApiRestClient client) {
        this.client = client;
    }

    @Override
    public Optional<CatalogoItemSnapshot> buscarItem(UUID referenciaUuid, TipoItemOs tipo) {
        if (referenciaUuid == null || tipo == null) {
            return Optional.empty();
        }
        return switch (tipo) {
            case PECA -> buscarPeca(referenciaUuid);
            case SERVICO -> buscarServico(referenciaUuid);
        };
    }

    @Retry(maxRetries = 2, delay = 200)
    @Timeout(2000)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000)
    @CacheResult(cacheName = CacheNames.PECAS)
    Optional<CatalogoItemSnapshot> buscarPeca(UUID pecaUuid) {
        try {
            var p = client.getPeca(pecaUuid);
            if (p == null) {
                return Optional.empty();
            }
            return Optional.of(new CatalogoItemSnapshot(
                    p.id(), TipoItemOs.PECA, p.descricao(), p.valorUnitario(), Boolean.TRUE.equals(p.isActive())));
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Retry(maxRetries = 2, delay = 200)
    @Timeout(2000)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000)
    @CacheResult(cacheName = CacheNames.SERVICOS)
    Optional<CatalogoItemSnapshot> buscarServico(UUID servicoUuid) {
        try {
            var s = client.getServico(servicoUuid);
            if (s == null) {
                return Optional.empty();
            }
            String descricao = s.nome() != null && !s.nome().isBlank() ? s.nome() : s.descricao();
            return Optional.of(new CatalogoItemSnapshot(
                    s.id(), TipoItemOs.SERVICO, descricao, s.valor(), Boolean.TRUE.equals(s.isActive())));
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
