# Mekano OS Service — Clean Architecture Quarkus API

## Overview
Microserviço de **Ordem de Serviço (OS)** extraído do monolito Mekano-API. Java 17, Quarkus 3.40, Maven multi-módulo, Clean Architecture. Mesma stack e convenções do Mekano-API (veja `Mekano-API/AGENTS.md`).

Plano completo: `docs/PLANO_MICROSERVICO.md`.

## Escopo
- **Dentro:** `OrdemDeServico`, `ItemOs`, `StatusOS`, `StatusEntrega`, transições do ciclo de vida, `/os/**`.
- **Fora (outros repos):** Orçamento, Pagamento, Auditoria, WhatsApp.
- **Fica no monolito (consumido via REST, somente leitura):** Cliente, Veículo, Serviço (catálogo), Peça/Estoque.
- **Comunicação assíncrona:** SNS (tópico FIFO `os-events.fifo`) + SQS (uma fila por consumidor, com DLQ). Outbox no produtor, `processed_events` no consumidor.

## Módulos e dependências

```
os-rest (packaging quarkus — entrypoint)
  ├── os-application
  ├── os-infrastructure
  └── os-domain
os-application → os-domain
os-infrastructure → os-domain
os-domain — zero framework (só Lombok provided)
```

Pacote base: `com.fiap.mekano.os.{domain|application|infrastructure|rest}`. groupId Maven: `mekano-os`.

## Convenções (herdadas do Mekano-API)
| Tema | Regra |
|---|---|
| Entidades | POJO no domínio, `@Builder(access=PRIVATE)`, factories `create()` + `reconstitute()` |
| ID | PK `Long` + `UUID uuid` único (exposto na API) |
| VOs | Imutáveis, validam no construtor |
| Ports | Interfaces puras em `domain/port/in` e `domain/port/out`, sem anotações de framework |
| Services | `@ApplicationScoped`, **injeção por construtor**, `@Transactional` nos métodos de escrita, Command (record) |
| Resources | `@RequestScoped`, `@RolesAllowed`, nunca `@Transactional` |
| DTOs | Input = classe Lombok com Bean Validation; Output = `record` |
| Erros | `AppException(status)` + `ApiExceptionMapper` → `application/problem+json` (RFC 7807) |
| Mappers | MapStruct com `componentModel = "cdi"` (nunca `spring`) |
| Soft delete | `isActive` + `deletedAt` |
| Auditoria de colunas | `createdBy/updatedBy` via `AuditoriaListener` (`@EntityListeners` em `BaseEntity`) |
| Resiliência | `@Retry/@Timeout/@CircuitBreaker` em REST clients e repositórios |
| Config | YAMLs por tema em `os-rest/src/main/resources`, `${VAR:default}` |
| Migrations | Flyway em `os-infrastructure/src/main/resources/db/migration` (`V1` limpo) |
| Testes | JUnit 5 + Mockito + AssertJ; `@QuarkusTest` + REST Assured em `os-rest`; H2 no `%test`; JaCoCo ≥ 80% |
| Idioma | Javadoc em PT-BR para conceitos de domínio |

## Regras específicas deste serviço
- **Nunca** importar repositórios/entidades de outros contextos. Cliente/Veículo/Catálogo/Estoque entram por ports de consulta (`*QueryPort`) implementados com REST client.
- Itens da OS guardam **snapshot** (descrição e valor unitário) no momento da criação.
- Sem FKs cross-contexto: apenas `cliente_uuid`, `veiculo_uuid`, `referencia_uuid`, `orcamento_uuid`.
- Toda mudança de status grava evento na tabela outbox **na mesma transação**.
- Consumidores de evento são idempotentes (`processed_events`).
- Autenticação: valida JWT EdDSA (issuer `mekano-api`) com `publicKey.pem`; este serviço não emite tokens.
- Não portar dívidas do monolito (`OrdemDeServico3`, `OrdemServicoEntity` duplicada, ports PT-BR).

## Comandos
```
make up          # Postgres (5433) + LocalStack (SNS/SQS)
make dev         # quarkus:dev
make test        # todos os testes
make build       # verify + JaCoCo agregado
```
Swagger: `http://localhost:8080/q/swagger-ui` · Health: `/q/health`.
