package com.fiap.mekano.os.infrastructure.repository;

import java.util.UUID;

import com.fiap.mekano.os.domain.port.out.ProcessedEventsPort;
import com.fiap.mekano.os.infrastructure.entity.ProcessedEventEntity;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessedEventRepositoryImpl implements ProcessedEventsPort {

    private final ProcessedEventPanacheRepository panacheRepository;

    public ProcessedEventRepositoryImpl(ProcessedEventPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public boolean alreadyProcessed(UUID eventId) {
        return panacheRepository.count("eventoId = ?1", eventId) > 0;
    }

    @Override
    public void markProcessed(UUID eventId, String eventType) {
        if (alreadyProcessed(eventId)) {
            return;
        }
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventoId(eventId);
        entity.setTipoEvento(eventType);
        panacheRepository.persist(entity);
    }
}
