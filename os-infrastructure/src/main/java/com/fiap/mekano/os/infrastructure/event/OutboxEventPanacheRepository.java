package com.fiap.mekano.os.infrastructure.event;

import com.fiap.mekano.os.infrastructure.entity.OutboxEventEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OutboxEventPanacheRepository implements PanacheRepository<OutboxEventEntity> {
}
