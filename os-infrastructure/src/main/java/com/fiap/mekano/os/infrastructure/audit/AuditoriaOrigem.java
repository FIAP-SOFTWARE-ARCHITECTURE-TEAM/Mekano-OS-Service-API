package com.fiap.mekano.os.infrastructure.audit;

import java.util.UUID;

/** Códigos reservados de origem para os campos {@code createdBy}/{@code updatedBy}. */
public enum AuditoriaOrigem {

    PUBLICO(UUID.fromString("00000000-0000-0000-0000-000000000001")),
    SISTEMA(UUID.fromString("00000000-0000-0000-0000-000000000002"));

    private final UUID codigo;

    AuditoriaOrigem(UUID codigo) {
        this.codigo = codigo;
    }

    public UUID getCodigo() {
        return codigo;
    }

    public static UUID resolver(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            return PUBLICO.codigo;
        }
        try {
            return UUID.fromString(principalName);
        } catch (IllegalArgumentException e) {
            return PUBLICO.codigo;
        }
    }
}
