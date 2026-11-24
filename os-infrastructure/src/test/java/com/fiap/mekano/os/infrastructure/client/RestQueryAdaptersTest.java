package com.fiap.mekano.os.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fiap.mekano.os.domain.model.TipoItemOs;
import com.fiap.mekano.os.infrastructure.client.dto.ClienteClientResponse;
import com.fiap.mekano.os.infrastructure.client.dto.PecaClientResponse;
import com.fiap.mekano.os.infrastructure.client.dto.ServicoClientResponse;
import com.fiap.mekano.os.infrastructure.client.dto.VeiculoClientResponse;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;

@ExtendWith(MockitoExtension.class)
class RestQueryAdaptersTest {

    private static final UUID CLIENTE = UUID.randomUUID();
    private static final UUID VEICULO = UUID.randomUUID();
    private static final UUID PECA = UUID.randomUUID();
    private static final UUID SERVICO = UUID.randomUUID();

    @Mock
    MekanoApiRestClient client;

    @Test
    void authorizationHeaderFactoryPropagaBearer() {
        var factory = new AuthorizationHeaderFactory();
        var incoming = new MultivaluedHashMap<String, String>();
        incoming.putSingle(HttpHeaders.AUTHORIZATION, "Bearer token-123");

        var out = factory.update(incoming, new MultivaluedHashMap<>());
        assertThat(out.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-123");
        assertThat(factory.update(null, new MultivaluedHashMap<>())).isEmpty();
    }

    @Test
    void clienteAdapterRetornaAtivoOuFalseEm404() {
        var adapter = new ClienteQueryRestAdapter(client);

        when(client.getCliente(CLIENTE)).thenReturn(new ClienteClientResponse(CLIENTE, true));
        assertThat(adapter.existsActiveById(CLIENTE)).isTrue();

        when(client.getCliente(CLIENTE)).thenReturn(new ClienteClientResponse(CLIENTE, false));
        assertThat(adapter.existsActiveById(CLIENTE)).isFalse();

        doThrow(new WebApplicationException(404)).when(client).getCliente(CLIENTE);
        assertThat(adapter.existsActiveById(CLIENTE)).isFalse();

        doThrow(new WebApplicationException(500)).when(client).getCliente(CLIENTE);
        assertThatThrownBy(() -> adapter.existsActiveById(CLIENTE)).isInstanceOf(WebApplicationException.class);
    }

    @Test
    void veiculoAdapterValidaAtivoEDono() {
        var adapter = new VeiculoQueryRestAdapter(client);

        when(client.getVeiculo(VEICULO)).thenReturn(new VeiculoClientResponse(VEICULO, CLIENTE, true));
        assertThat(adapter.existsActiveByIdAndCliente(VEICULO, CLIENTE)).isTrue();
        assertThat(adapter.existsActiveByIdAndCliente(VEICULO, UUID.randomUUID())).isFalse();

        when(client.getVeiculo(VEICULO)).thenReturn(new VeiculoClientResponse(VEICULO, CLIENTE, false));
        assertThat(adapter.existsActiveByIdAndCliente(VEICULO, CLIENTE)).isFalse();

        when(client.getVeiculo(VEICULO)).thenThrow(new WebApplicationException(404));
        assertThat(adapter.existsActiveByIdAndCliente(VEICULO, CLIENTE)).isFalse();
    }

    @Test
    void catalogoAdapterMapeiaPecaEServicoETrata404() {
        var adapter = new CatalogoQueryRestAdapter(client);

        assertThat(adapter.buscarItem(null, TipoItemOs.PECA)).isEmpty();
        assertThat(adapter.buscarItem(PECA, null)).isEmpty();

        doReturn(new PecaClientResponse(PECA, "Filtro", new BigDecimal("35.00"), true)).when(client).getPeca(PECA);
        var peca = adapter.buscarItem(PECA, TipoItemOs.PECA).orElseThrow();
        assertThat(peca.descricao()).isEqualTo("Filtro");
        assertThat(peca.valorUnitario()).isEqualByComparingTo("35.00");
        assertThat(peca.ativo()).isTrue();

        doReturn(new ServicoClientResponse(SERVICO, "Troca de óleo", "Completa", new BigDecimal("90.00"), true))
                .when(client).getServico(SERVICO);
        var servico = adapter.buscarItem(SERVICO, TipoItemOs.SERVICO).orElseThrow();
        assertThat(servico.descricao()).isEqualTo("Troca de óleo");
        assertThat(servico.valorUnitario()).isEqualByComparingTo("90.00");

        doThrow(new WebApplicationException(404)).when(client).getPeca(PECA);
        assertThat(adapter.buscarItem(PECA, TipoItemOs.PECA)).isEmpty();

        doThrow(new WebApplicationException(404)).when(client).getServico(SERVICO);
        assertThat(adapter.buscarItem(SERVICO, TipoItemOs.SERVICO)).isEmpty();
    }
}
