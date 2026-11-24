package com.fiap.mekano.os.infrastructure.client;

import java.util.UUID;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fiap.mekano.os.domain.port.out.VeiculoQueryPort;
import com.fiap.mekano.os.infrastructure.cache.CacheNames;

import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class VeiculoQueryRestAdapter implements VeiculoQueryPort {

    private final MekanoApiRestClient client;

    public VeiculoQueryRestAdapter(@RestClient MekanoApiRestClient client) {
        this.client = client;
    }

    @Override
    @Retry(maxRetries = 2, delay = 200)
    @Timeout(2000)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000)
    @CacheResult(cacheName = CacheNames.VEICULOS)
    public boolean existsActiveByIdAndCliente(UUID veiculoUuid, UUID clienteUuid) {
        try {
            var response = client.getVeiculo(veiculoUuid);
            return response != null
                    && Boolean.TRUE.equals(response.isActive())
                    && clienteUuid != null
                    && clienteUuid.equals(response.clienteUuid());
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                return false;
            }
            throw e;
        }
    }
}
