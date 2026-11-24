package com.fiap.mekano.os.domain.port.in;

import java.util.List;
import java.util.UUID;

/** Comando para criação ou atualização de Ordem de Serviço. */
public record CreateOrdemDeServicoCommand(
        UUID clienteUuid,
        UUID veiculoUuid,
        String descricaoProblema,
        List<CreateItemOsCommand> itens
) {
    public CreateOrdemDeServicoCommand {
        itens = itens == null ? List.of() : List.copyOf(itens);
    }
}
