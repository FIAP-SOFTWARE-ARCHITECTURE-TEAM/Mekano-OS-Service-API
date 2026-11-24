package com.fiap.mekano.os.domain.port.in;

import java.util.UUID;

/** Comando para cancelar uma Ordem de Serviço. */
public record CancelarOSCommand(UUID osUuid, String motivo) {
}
