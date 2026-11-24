package com.fiap.mekano.os.domain.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fiap.mekano.os.domain.model.TipoItemOs;

class CommandRecordTest {

    @Test
    void commandsGarantemListasImutaveisENullSafe() {
        CreateItemOsCommand item = new CreateItemOsCommand(UUID.randomUUID(), TipoItemOs.SERVICO, 1L);

        CreateOrdemDeServicoCommand createSemItens =
                new CreateOrdemDeServicoCommand(UUID.randomUUID(), UUID.randomUUID(), "desc", null);
        assertThat(createSemItens.itens()).isEmpty();
        assertThatThrownBy(() -> createSemItens.itens().add(item))
                .isInstanceOf(UnsupportedOperationException.class);

        FinalizarDiagnosticoCommand diagSemItens =
                new FinalizarDiagnosticoCommand(UUID.randomUUID(), "diag", null);
        assertThat(diagSemItens.itens()).isEmpty();

        CreateOrdemDeServicoCommand createComItem =
                new CreateOrdemDeServicoCommand(UUID.randomUUID(), UUID.randomUUID(), "desc", List.of(item));
        assertThat(createComItem.itens()).containsExactly(item);
    }
}
