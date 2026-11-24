package com.fiap.mekano.os.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.mekano.os.domain.event.OsDomainEvent;
import com.fiap.mekano.os.domain.port.out.EventPublisher;
import com.fiap.mekano.os.infrastructure.entity.OutboxEventEntity;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Implementação de {@link EventPublisher} usando Transactional Outbox.
 * Grava o evento na tabela {@code outbox_events} na mesma transação do caso de uso;
 * o envio ao SNS fica a cargo do {@link OutboxRelayJob}.
 */
@ApplicationScoped
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxEventPanacheRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxEventPanacheRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(OsDomainEvent event) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setEventId(event.eventId());
        entity.setAggregateId(event.osUuid());
        entity.setEventType(event.eventType());
        entity.setPayload(serialize(event));
        entity.setStatus("PENDING");
        repository.persist(entity);
    }

    private String serialize(OsDomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar evento de domínio: " + event.eventType(), e);
        }
    }
}
