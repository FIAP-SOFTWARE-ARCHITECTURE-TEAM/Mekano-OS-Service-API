package com.fiap.mekano.os.domain.port.out;

import com.fiap.mekano.os.domain.event.OsDomainEvent;

/**
 * Driven port para publicação de eventos de domínio.
 * Implementado na infraestrutura gravando na tabela outbox na mesma transação.
 */
public interface EventPublisher {

    void publish(OsDomainEvent event);
}
