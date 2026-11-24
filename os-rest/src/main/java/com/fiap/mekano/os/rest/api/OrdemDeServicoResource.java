package com.fiap.mekano.os.rest.api;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fiap.mekano.os.domain.model.OrdemDeServico;
import com.fiap.mekano.os.domain.model.StatusOS;
import com.fiap.mekano.os.domain.port.in.CancelarOSCommand;
import com.fiap.mekano.os.domain.port.in.EntregarOSCommand;
import com.fiap.mekano.os.domain.port.in.FinalizarDiagnosticoCommand;
import com.fiap.mekano.os.domain.port.in.FinalizarExecucaoCommand;
import com.fiap.mekano.os.domain.port.in.IniciarExecucaoCommand;
import com.fiap.mekano.os.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.os.rest.api.dto.CreateOrdemDeServicoRequest;
import com.fiap.mekano.os.rest.api.dto.FinalizarDiagnosticoRequest;
import com.fiap.mekano.os.rest.api.dto.FinalizarExecucaoRequest;
import com.fiap.mekano.os.rest.api.dto.IniciarExecucaoRequest;
import com.fiap.mekano.os.rest.api.dto.MotivoRequest;
import com.fiap.mekano.os.rest.api.dto.OrdemDeServicoPageResponse;
import com.fiap.mekano.os.rest.api.dto.OrdemDeServicoResponse;
import com.fiap.mekano.os.rest.api.dto.RecebidoPorRequest;
import com.fiap.mekano.os.rest.api.dto.TempoMedioResponse;
import com.fiap.mekano.os.rest.api.mapper.OrdemDeServicoDtoMapper;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/** Resource REST para o ciclo de vida das Ordens de Serviço. */
@Path("/os")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Ordens de Serviço", description = "Gerenciamento de OS")
public class OrdemDeServicoResource {

    private final OrdemDeServicoServicePort osService;
    private final OrdemDeServicoDtoMapper mapper;

    public OrdemDeServicoResource(OrdemDeServicoServicePort osService, OrdemDeServicoDtoMapper mapper) {
        this.osService = osService;
        this.mapper = mapper;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "atendente" })
    @Operation(summary = "Criar nova OS")
    @APIResponse(responseCode = "201", description = "OS criada com sucesso")
    public Response create(@Valid CreateOrdemDeServicoRequest request, @Context UriInfo uriInfo) {
        OrdemDeServico os = osService.create(mapper.toCommand(request));
        OrdemDeServicoResponse response = mapper.toResponse(os);
        URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
        return Response.created(location).entity(response).build();
    }

    @GET
    @RolesAllowed({ "admin", "atendente", "mecanico" })
    @Operation(summary = "Listar OS (paginado)")
    public Response listAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sort") @DefaultValue("createdAt,desc") String sort) {
        var content = osService.findAll(page, size, sort).stream().map(mapper::toResponse).toList();
        long total = osService.countAll();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return Response.ok(new OrdemDeServicoPageResponse(content, page, size, total, totalPages)).build();
    }

    @GET
    @Path("/filtro")
    @RolesAllowed({ "admin", "atendente", "mecanico" })
    @Operation(summary = "Listar OS com filtros ordenadas por prioridade de status")
    public Response findAllWithFilters(
            @QueryParam("status") StatusOS status,
            @QueryParam("clienteId") UUID clienteId,
            @QueryParam("veiculoId") UUID veiculoId,
            @QueryParam("dataInicio") LocalDate dataInicio,
            @QueryParam("dataFim") LocalDate dataFim,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        var content = osService.findAllWithFilters(status, clienteId, veiculoId, inicio, fim, page, size)
                .stream().map(mapper::toResponse).toList();
        return Response.ok(content).build();
    }

    @GET
    @Path("/tempo-medio")
    @RolesAllowed({ "admin", "atendente" })
    @Operation(summary = "Tempo médio de execução de OS com breakdown por mecânico")
    public Response getTempoMedio(
            @QueryParam("dataInicio") LocalDate dataInicio,
            @QueryParam("dataFim") LocalDate dataFim) {
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        var tempoMedio = osService.calcularTempoMedioExecucao(inicio, fim).orElse(null);
        var porMecanico = osService.calcularTempoMedioPorMecanico(inicio, fim);
        return Response.ok(new TempoMedioResponse(tempoMedio, porMecanico)).build();
    }

    @GET
    @Path("/{id}/status")
    @PermitAll
    @Operation(summary = "Consultar status da OS (endpoint público)")
    public Response getStatus(@PathParam("id") UUID id) {
        return Response.ok(mapper.toStatusResponse(osService.findById(id))).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "admin", "atendente", "mecanico" })
    @Operation(summary = "Buscar OS por ID")
    public Response getById(@PathParam("id") UUID id) {
        return Response.ok(mapper.toResponse(osService.findById(id))).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "atendente" })
    @Operation(summary = "Atualizar dados da OS (permitido apenas em RECEBIDA)")
    public Response update(@PathParam("id") UUID id, @Valid CreateOrdemDeServicoRequest request) {
        OrdemDeServico os = osService.update(id, mapper.toCommand(request));
        return Response.ok(mapper.toResponse(os)).build();
    }

    @PUT
    @Path("/{id}/iniciar-diagnostico")
    @RolesAllowed({ "mecanico", "admin" })
    @Operation(summary = "Iniciar diagnóstico da OS")
    public Response iniciarDiagnostico(@PathParam("id") UUID id) {
        return Response.ok(mapper.toResponse(osService.iniciarDiagnostico(id))).build();
    }

    @PUT
    @Path("/{id}/finalizar-diagnostico")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "mecanico", "admin" })
    @Operation(summary = "Finalizar diagnóstico e publicar evento para Orçamento")
    public Response finalizarDiagnostico(@PathParam("id") UUID id, @Valid FinalizarDiagnosticoRequest request) {
        var command = new FinalizarDiagnosticoCommand(
                id, request.getDescricao(), mapper.toItemCommands(request.getItens()));
        return Response.ok(mapper.toResponse(osService.finalizarDiagnostico(command))).build();
    }

    @PUT
    @Path("/{id}/iniciar-execucao")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "mecanico", "admin" })
    @Operation(summary = "Iniciar execução da OS")
    public Response iniciarExecucao(@PathParam("id") UUID id, @Valid IniciarExecucaoRequest request) {
        var command = new IniciarExecucaoCommand(id, request.getMecanicoUuid(), request.getObservacao());
        return Response.ok(mapper.toResponse(osService.iniciarExecucao(command))).build();
    }

    @PUT
    @Path("/{id}/finalizar-execucao")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "mecanico", "admin" })
    @Operation(summary = "Finalizar execução da OS")
    public Response finalizarExecucao(@PathParam("id") UUID id, FinalizarExecucaoRequest request) {
        String observacao = request != null ? request.getObservacao() : null;
        return Response.ok(mapper.toResponse(osService.finalizarExecucao(new FinalizarExecucaoCommand(id, observacao))))
                .build();
    }

    @PUT
    @Path("/{id}/cancelar")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "atendente" })
    @Operation(summary = "Cancelar OS")
    public Response cancelar(@PathParam("id") UUID id, @Valid MotivoRequest request) {
        return Response.ok(mapper.toResponse(osService.cancelar(new CancelarOSCommand(id, request.getMotivo()))))
                .build();
    }

    @PATCH
    @Path("/{id}/entregar")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "atendente" })
    @Operation(summary = "Registrar entrega do veículo ao cliente")
    public Response entregar(@PathParam("id") UUID id, @Valid RecebidoPorRequest request) {
        return Response.ok(mapper.toResponse(osService.entregar(new EntregarOSCommand(id, request.getRecebidoPor()))))
                .build();
    }
}
