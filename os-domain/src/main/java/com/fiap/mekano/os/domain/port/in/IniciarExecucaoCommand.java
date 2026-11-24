package com.fiap.mekano.os.domain.port.in;

import java.util.UUID;

/** Comando para iniciar a execução dos serviços por um mecânico. */
public record IniciarExecucaoCommand(UUID osUuid, UUID mecanicoUuid, String observacao) {
}
