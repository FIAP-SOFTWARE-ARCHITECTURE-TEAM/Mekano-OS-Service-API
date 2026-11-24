package com.fiap.mekano.os.domain.port.in;

import java.util.List;
import java.util.UUID;

/** Comando para finalizar o diagnóstico técnico da OS, opcionalmente incluindo novos itens. */
public record FinalizarDiagnosticoCommand(
        UUID osUuid,
        String descricao,
        List<CreateItemOsCommand> itens
) {
    public FinalizarDiagnosticoCommand {
        itens = itens == null ? List.of() : List.copyOf(itens);
    }
}
