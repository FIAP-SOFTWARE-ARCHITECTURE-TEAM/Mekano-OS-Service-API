package com.fiap.mekano.os.domain.port.out;

import java.util.UUID;

/** Consulta somente-leitura ao contexto de Clientes (monolito via REST). */
public interface ClienteQueryPort {

    boolean existsActiveById(UUID clienteUuid);
}
