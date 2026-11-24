package com.fiap.mekano.os.domain.port.in;

import java.util.UUID;

/** Comando para registrar a entrega do veículo ao cliente. */
public record EntregarOSCommand(UUID osUuid, String recebidoPor) {
}
