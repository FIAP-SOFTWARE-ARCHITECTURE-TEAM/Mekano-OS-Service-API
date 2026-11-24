package com.fiap.mekano.os.domain.port.out;

import java.util.UUID;

/** Controle de idempotência para consumo de eventos externos (tabela {@code processed_events}). */
public interface ProcessedEventsPort {

    boolean alreadyProcessed(UUID eventId);

    void markProcessed(UUID eventId, String eventType);
}
