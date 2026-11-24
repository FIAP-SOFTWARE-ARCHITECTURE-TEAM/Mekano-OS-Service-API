package com.fiap.mekano.os.domain.port.in;

import java.util.UUID;

/** Comando para finalizar a execução técnica da OS. */
public record FinalizarExecucaoCommand(UUID osUuid, String observacao) {
}
