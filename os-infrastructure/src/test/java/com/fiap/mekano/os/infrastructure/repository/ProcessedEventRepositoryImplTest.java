package com.fiap.mekano.os.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ProcessedEventRepositoryImplTest {

    @Inject
    ProcessedEventRepositoryImpl repository;

    @Test
    @TestTransaction
    void marcaEventoDeFormaIdempotente() {
        UUID eventId = UUID.randomUUID();
        assertThat(repository.alreadyProcessed(eventId)).isFalse();

        repository.markProcessed(eventId, "OrcamentoAprovado");
        assertThat(repository.alreadyProcessed(eventId)).isTrue();

        // Chamada duplicada não deve falhar
        repository.markProcessed(eventId, "OrcamentoAprovado");
        assertThat(repository.alreadyProcessed(eventId)).isTrue();
    }
}
