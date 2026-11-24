package com.fiap.mekano.os.rest.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fiap.mekano.os.domain.model.TipoItemOs;
import com.fiap.mekano.os.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.os.domain.port.out.CatalogoQueryPort;
import com.fiap.mekano.os.domain.port.out.CatalogoQueryPort.CatalogoItemSnapshot;
import com.fiap.mekano.os.domain.port.out.ClienteQueryPort;
import com.fiap.mekano.os.domain.port.out.VeiculoQueryPort;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
class OrdemDeServicoResourceTest {

    private static final UUID CLIENTE = UUID.randomUUID();
    private static final UUID VEICULO = UUID.randomUUID();
    private static final UUID PECA = UUID.randomUUID();
    private static final UUID MECANICO = UUID.randomUUID();
    private static final UUID ORCAMENTO = UUID.randomUUID();

    @InjectMock
    ClienteQueryPort clienteQuery;

    @InjectMock
    VeiculoQueryPort veiculoQuery;

    @InjectMock
    CatalogoQueryPort catalogoQuery;

    @Inject
    OrdemDeServicoServicePort osService;

    @BeforeEach
    void setupMocks() {
        when(clienteQuery.existsActiveById(CLIENTE)).thenReturn(true);
        when(veiculoQuery.existsActiveByIdAndCliente(VEICULO, CLIENTE)).thenReturn(true);
        when(catalogoQuery.buscarItem(PECA, TipoItemOs.PECA)).thenReturn(Optional.of(
                new CatalogoItemSnapshot(PECA, TipoItemOs.PECA, "Pastilha de freio", new BigDecimal("120.00"), true)));
    }

    @Test
    @TestSecurity(user = "00000000-0000-0000-0000-000000000010", roles = { "admin" })
    void fluxoCompletoDoCicloDeVidaDaOs() {
        // 1. Criar OS
        String osIdStr = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "clienteUuid": "%s",
                          "veiculoUuid": "%s",
                          "descricaoProblema": "Barulho no freio",
                          "itens": [{"referenciaUuid": "%s", "tipo": "PECA", "quantidade": 2}]
                        }
                        """.formatted(CLIENTE, VEICULO, PECA))
                .when().post("/api/v1/os")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("status", equalTo("RECEBIDA"))
                .body("valorTotalItens", equalTo(240.00f))
                .body("itens", hasSize(1))
                .extract().path("id");

        UUID osId = UUID.fromString(osIdStr);

        // 2. Atualizar em RECEBIDA
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "clienteUuid": "%s",
                          "veiculoUuid": "%s",
                          "descricaoProblema": "Barulho no freio dianteiro",
                          "itens": [{"referenciaUuid": "%s", "tipo": "PECA", "quantidade": 1}]
                        }
                        """.formatted(CLIENTE, VEICULO, PECA))
                .when().put("/api/v1/os/{id}", osId)
                .then()
                .statusCode(200)
                .body("descricaoProblema", equalTo("Barulho no freio dianteiro"))
                .body("valorTotalItens", equalTo(120.00f));

        // 3. Listagem, filtro e busca por ID
        given().when().get("/api/v1/os").then().statusCode(200);
        given().queryParam("status", "RECEBIDA").when().get("/api/v1/os/filtro").then().statusCode(200);
        given().when().get("/api/v1/os/{id}", osId).then().statusCode(200).body("id", equalTo(osIdStr));

        // 4. Diagnóstico
        given().when().put("/api/v1/os/{id}/iniciar-diagnostico", osId)
                .then().statusCode(200).body("status", equalTo("EM_DIAGNOSTICO"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao": "Pastilha gasta", "itens": []}
                        """)
                .when().put("/api/v1/os/{id}/finalizar-diagnostico", osId)
                .then().statusCode(200).body("status", equalTo("AGUARDANDO_APROVACAO"));

        // 5. Aprovação via evento de Orçamento
        osService.registrarAprovacaoOrcamento(osId, ORCAMENTO);

        // 6. Execução
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"mecanicoUuid": "%s", "observacao": "Trocando"}
                        """.formatted(MECANICO))
                .when().put("/api/v1/os/{id}/iniciar-execucao", osId)
                .then().statusCode(200).body("status", equalTo("EM_EXECUCAO"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"observacao": "Concluído"}
                        """)
                .when().put("/api/v1/os/{id}/finalizar-execucao", osId)
                .then().statusCode(200).body("status", equalTo("FINALIZADA"));

        // 7. Entrega antes do pagamento -> 422 Problem Details
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"recebidoPor": "João"}
                        """)
                .when().patch("/api/v1/os/{id}/entregar", osId)
                .then()
                .statusCode(422)
                .contentType("application/problem+json")
                .body("status", equalTo(422));

        // 8. Pagamento confirmado -> libera entrega
        osService.liberarEntregaPorPagamento(osId);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"recebidoPor": "João"}
                        """)
                .when().patch("/api/v1/os/{id}/entregar", osId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ENTREGUE"))
                .body("statusEntrega", equalTo("ENTREGUE"));

        // 9. Tempo médio
        given().when().get("/api/v1/os/tempo-medio").then().statusCode(200);
    }

    @Test
    void consultaPublicaDeStatusFuncionaSemAutenticacaoEProtegidaExigeAuth() {
        var criada = osService.create(new com.fiap.mekano.os.domain.port.in.CreateOrdemDeServicoCommand(
                CLIENTE, VEICULO, "Revisão", java.util.List.of()));

        // Público (@PermitAll)
        given().when().get("/api/v1/os/{id}/status", criada.getId())
                .then()
                .statusCode(200)
                .body("status", equalTo("RECEBIDA"));

        // Protegido sem token -> 401
        given().when().get("/api/v1/os/{id}", criada.getId())
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "00000000-0000-0000-0000-000000000010", roles = { "atendente" })
    void cancelarENotFoundERegraDeRole() {
        String id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteUuid": "%s", "veiculoUuid": "%s", "descricaoProblema": "Alinhamento"}
                        """.formatted(CLIENTE, VEICULO))
                .when().post("/api/v1/os")
                .then().statusCode(201).extract().path("id");

        // Atendente não pode iniciar diagnóstico (role mecanico/admin) -> 403
        given().when().put("/api/v1/os/{id}/iniciar-diagnostico", id)
                .then().statusCode(403);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"motivo": "Cliente desistiu"}
                        """)
                .when().put("/api/v1/os/{id}/cancelar", id)
                .then().statusCode(200).body("status", equalTo("CANCELADA"));

        given().when().get("/api/v1/os/{id}", UUID.randomUUID())
                .then().statusCode(404).contentType("application/problem+json");
    }
}
