package com.fiap.mekano.os.domain.port.in;

import java.util.UUID;

import com.fiap.mekano.os.domain.model.TipoItemOs;

/** Item solicitado na criação/atualização da OS ou incluído no diagnóstico. */
public record CreateItemOsCommand(UUID referenciaUuid, TipoItemOs tipo, Long quantidade) {
}
