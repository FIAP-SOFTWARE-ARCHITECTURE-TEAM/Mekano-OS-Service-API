package com.fiap.mekano.os.domain.port.out;

import java.util.UUID;

/** Consulta somente-leitura ao contexto de Veículos (monolito via REST). */
public interface VeiculoQueryPort {

    boolean existsActiveByIdAndCliente(UUID veiculoUuid, UUID clienteUuid);
}
