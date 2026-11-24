package com.fiap.mekano.os.infrastructure.audit;

import java.util.UUID;

import com.fiap.mekano.os.infrastructure.entity.BaseEntity;

import io.quarkus.arc.Arc;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/** Preenche {@code createdBy}/{@code updatedBy} automaticamente em entidades {@link BaseEntity}. */
public class AuditoriaListener {

    @PrePersist
    void onCreate(BaseEntity entity) {
        entity.setCreatedBy(atorAtual());
    }

    @PreUpdate
    void onUpdate(BaseEntity entity) {
        entity.setUpdatedBy(atorAtual());
    }

    private UUID atorAtual() {
        try {
            AuditoriaContext context = Arc.container().instance(AuditoriaContext.class).get();
            return AuditoriaOrigem.resolver(context.principalName());
        } catch (Exception e) {
            return AuditoriaOrigem.SISTEMA.getCodigo();
        }
    }
}
