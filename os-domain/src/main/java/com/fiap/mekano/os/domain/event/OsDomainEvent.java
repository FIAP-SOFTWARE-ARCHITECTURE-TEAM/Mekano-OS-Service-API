package com.fiap.mekano.os.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/** Contrato base para eventos de domínio publicados pela OS (Outbox + SNS). */
public interface OsDomainEvent {

    UUID eventId();

    UUID osUuid();

    String eventType();

    LocalDateTime occurredAt();
}
