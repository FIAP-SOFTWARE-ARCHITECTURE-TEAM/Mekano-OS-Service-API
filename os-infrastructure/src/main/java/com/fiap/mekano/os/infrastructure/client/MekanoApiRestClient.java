package com.fiap.mekano.os.infrastructure.client;

import java.util.UUID;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.fiap.mekano.os.infrastructure.client.dto.ClienteClientResponse;
import com.fiap.mekano.os.infrastructure.client.dto.PecaClientResponse;
import com.fiap.mekano.os.infrastructure.client.dto.ServicoClientResponse;
import com.fiap.mekano.os.infrastructure.client.dto.VeiculoClientResponse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/** REST Client somente-leitura para consultas ao monolito Mekano-API. */
@Path("/api/v1")
@RegisterRestClient(configKey = "mekano-api")
@RegisterClientHeaders(AuthorizationHeaderFactory.class)
public interface MekanoApiRestClient {

    @GET
    @Path("/clientes/{id}")
    ClienteClientResponse getCliente(@PathParam("id") UUID id);

    @GET
    @Path("/veiculos/{id}")
    VeiculoClientResponse getVeiculo(@PathParam("id") UUID id);

    @GET
    @Path("/servicos/{id}")
    ServicoClientResponse getServico(@PathParam("id") UUID id);

    @GET
    @Path("/pecas/{id}")
    PecaClientResponse getPeca(@PathParam("id") UUID id);
}
