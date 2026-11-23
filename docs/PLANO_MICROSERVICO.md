# Plano: Microserviço de Ordem de Serviço (`Mekano-OS-Service-API`)

> Base: `Mekano-API/AGENTS.md`, `.planning/codebase/*`, `docs/*` (RF, EventStorming, sequence diagrams), código-fonte e repos irmãos (`Mekano-Infrastructure-K8s`, `Mekano-Serverless`, `Mekano-Infrastructure-Database`).
> Escopo decidido: este repo contém apenas o contexto de Ordem de Serviço. Orçamento, Pagamento, Auditoria e WhatsApp ficam em outros repos.

## 1. Stack e arquitetura a replicar (do Mekano-API)

| Item | Padrão a seguir |
|---|---|
| Linguagem / Framework | Java 17, Quarkus 3.40, Maven wrapper 3.9.x |
| Estrutura | Maven multi-módulo: `*-domain` → `*-application` → `*-infrastructure` → `*-rest` (único com packaging `quarkus`) |
| Domínio | POJO sem framework (só Lombok `provided`), `@Builder(access=PRIVATE)`, factories `create()`/`reconstitute()`, VOs imutáveis, ports `in`/`out`, eventos de domínio |
| Application | `@ApplicationScoped`, injeção por construtor, `@Transactional` nos métodos de escrita, Command pattern (records) |
| Infra | Panache + Hibernate ORM, MapStruct (`componentModel="cdi"`), Flyway, `AuditoriaListener` (createdBy/updatedBy), `@Retry/@Timeout/@CacheResult` nos repositórios, soft delete |
| REST | `@RequestScoped`, `@RolesAllowed`, DTO in = Lombok class, DTO out = record, `ApiExceptionMapper` (RFC 7807), OpenAPI |
| ID | PK `Long` + `UUID` exposto na API |
| Auth | JWT EdDSA (Ed25519), issuer `mekano-api`, só **valida** com a chave pública (emissão fica no monolito/Lambda) |
| Config | YAML por tema (`datasource-config.yml`, `auth-config.yml`, ...), `${VAR:default}` |
| Testes | JUnit 5, Mockito, AssertJ, `@QuarkusTest` + REST Assured, H2 em teste, JaCoCo `report-aggregate` ≥ 80% |
| Entrega | Docker (JVM), docker-compose, CI GitHub Actions (`mvn verify -pl <rest> -am` + Newman), K8s/Terraform no repo de infra |

Pacote base sugerido: `com.fiap.mekano.os` (mantendo o prefixo `com.fiap.mekano`).

## 2. Escopo (decidido): somente "Ordem de Serviço"

**Entra neste repo:**
- Models: `OrdemDeServico`, `ItemOs`, `StatusOS`, `StatusEntrega` (e transições/`OsTransitionedEvent`)
- Service: `OrdemDeServicoService`
- Resource `/os`: criar, listar/filtrar, detalhar, atualizar, `/{id}/status` (público), `iniciar-diagnostico`, `finalizar-diagnostico`, `iniciar-execucao`, `finalizar-execucao`, `entregar`, `cancelar`, `detalhamento`, `tempo-medio`
- Tabelas: `ordens_de_servico`, `os_itens`, `processed_events`, `outbox_events` (Flyway `V1` limpo)
- Banco: **RDS exclusivo** (Terraform no repo de infra de banco)

**Vai para outros repos (fora deste plano):**

| Contexto | Destino |
|---|---|
| Orçamento | Repo próprio (Orcamento-Service) |
| Pagamento / Cobrança | Repo próprio (pode ser o mesmo do Orçamento; a definir) |
| Auditoria de OS | Repo próprio (consumidor de eventos) |
| WhatsApp / Evolution API | Repo próprio (consumidor de eventos) |

**Permanece no monolito:** Users/Auth, Cliente, Veículo, Serviço (catálogo), Peça/Estoque, Requisição de Compra, NF Entrada (acessados por REST).

## 3. Acoplamentos a quebrar

O `OrdemDeServicoService` depende hoje de `ClienteRepositoryPort`, `VeiculoRepositoryPort`, `PecaRepositoryPort`, `ServicoRepositoryPort`, **`OrcamentoRepositoryPort`** e `OsAuditEventPublisher`.

| Dependência atual | Substituição |
|---|---|
| Validar cliente/veículo | Ports `ClienteQueryPort`/`VeiculoQueryPort` → REST client para o monolito (`@Retry/@Timeout/@CircuitBreaker`) |
| Descrição/valor de Serviço e Peça | `CatalogoQueryPort` (REST) + **snapshot** em `os_itens` na criação |
| `finalizarDiagnostico` cria o Orçamento | Passa a publicar `DiagnosticoFinalizado` (com itens e valores). O serviço de Orçamento cria o orçamento e responde com `OrcamentoAprovado`/`OrcamentoReprovado`/`OrcamentoExpirado` |
| Status `AGUARDANDO_APROVACAO` / `EM_EXECUCAO` / `CANCELADA` por orçamento | OS **consome** os eventos de orçamento e transiciona (idempotente) |
| `confirmar-pagamento` e cobrança | OS publica `OSFinalizada`; **consome** `PagamentoConfirmado` para liberar a entrega. Endpoint `/os/{id}/confirmar-pagamento` migra para o serviço de Pagamento |
| `findOrcamentoUuidByOsId`, `buscarItensOrcados` | Guardar `orcamento_uuid` na OS (recebido por evento); itens orçados vêm do Orçamento via REST se necessário |
| `OsAuditEventPublisher`/`OsAuditLogObserver` | Remover; Auditoria consome `OsTransitionedEvent` |
| `PecaOrcamentoObserver` (estoque) | Estoque (monolito) consome eventos de orçamento/OS |
| `WhatsApp*Observer` | Removidos; repo de WhatsApp consome eventos |

## 4. Comunicação

- **Por que broker:** OS, Orçamento, Pagamento, Auditoria, WhatsApp e Estoque agora são deploys independentes. Eventos CDI in-process não cruzam serviços; REST direto acoplaria a OS à disponibilidade de todos. Broker + outbox dá entrega garantida, reprocessamento e consumidores novos sem mexer na OS.
- **Eventos publicados pela OS:** `OrdemDeServicoCriada`, `DiagnosticoFinalizado`, `OSExecucaoIniciada`, `OSFinalizada`, `OSEntregue`, `OSCancelada`, `OsTransitioned`.
- **Eventos consumidos pela OS:** `OrcamentoGerado`, `OrcamentoAprovado`, `OrcamentoReprovado`, `OrcamentoExpirado`, `PagamentoConfirmado`.
- **Padrões:** Transactional Outbox no produtor, `processed_events` no consumidor, envelope com `eventId`, `eventType`, `occurredAt`, `correlationId`, `version`. Contratos documentados em `docs/events/`.
- **REST síncrono (somente leitura)** ao monolito: cliente, veículo, catálogo, estoque.
- **Auth:** valida JWT EdDSA com a chave pública (issuer `mekano-api`); `/os/{id}/status` continua público. Chamadas service-to-service com token técnico.
- **Gateway/Ingress:** `/os/**` roteado para este serviço (repo `Mekano-Infrastructure-K8s`).

## 5. Dados

- RDS **exclusivo** `mekano_os`, sem FKs cross-contexto (apenas `cliente_uuid`, `veiculo_uuid`, `referencia_uuid`, `orcamento_uuid`).
- Flyway `V1` consolidado (não copiar as 36 migrations do monolito).
- Migração: dump das tabelas de OS do monolito, carga no novo RDS, corte com feature flag no gateway (Strangler Fig); monolito read-only para OS durante a virada.

## 6. Decisões

| # | Tema | Decisão |
|---|---|---|
| 1 | Escopo | Só OS. Orçamento, Pagamento e Auditoria em outro(s) repo(s) |
| 2 | Broker | **SNS + SQS** (LocalStack no dev local) |
| 3 | WhatsApp | Outro repo |
| 4 | Banco | RDS exclusivo |
| 5 | Cliente/Veículo/Catálogo/Estoque | Ficam no monolito, via REST |
| 6 | Quarkus | 3.40 |

**Pendente:** qual broker? Recomendo **SNS + SQS** (já estamos na AWS com EKS/RDS/Lambda; gerenciado, sem operar cluster), usando `quarkus-amazon-sns/sqs` ou SmallRye Reactive Messaging. Alternativas: Kafka (MSK, mais caro/complexo) ou RabbitMQ (Amazon MQ). Para dev local: LocalStack no docker-compose.

> [!IMPORTANT]
> Os repos de Orçamento/Pagamento/Auditoria/WhatsApp precisam respeitar os contratos de eventos acima. Sugiro definir os schemas em `docs/events/` antes da Fase 3.

## 7. Fases de execução

| Fase | Entregáveis |
|---|---|
| **0. Fundação** | Copiar `pom.xml` pai, wrapper, `.editorconfig`, `AGENTS.md` por módulo, JaCoCo, CI (`ci.yml`), Dockerfile, docker-compose (Postgres + keygen), `.env.example`; 4 módulos vazios compilando; health/OpenAPI |
| **1. Domínio** | Portar models/VOs/eventos/ports de OS + testes unitários (ver §2); remover dependências de Cliente/Veículo/Peça/Serviço no domínio (ports de consulta) |
| **2. Application** | Portar services; trocar repositórios cross-contexto por query ports; snapshot de itens; testes com Mockito |
| **3. Infrastructure** | Entities/Panache/MapStruct/Flyway `V1`; `AuditoriaListener`; REST clients para o monolito com FT; outbox + publisher; consumers idempotentes |
| **4. REST** | Resource `/os`; DTOs; `ApiExceptionMapper`; JWT/roles; `@QuarkusTest` + coleção Newman adaptada de `Mekano-API/newman` |
| **5. Mudanças no monolito** | Endpoints de leitura (cliente, veículo, catálogo, estoque); consumidores de eventos de estoque; depois remover código de OS. Orçamento/Pagamento/Auditoria/WhatsApp: extração em seus próprios repos (fora deste plano), alinhada aos contratos de eventos |
| **6. Infra** | Manifests K8s (deployment, service, hpa, configmap, secret, ingress), rotas no gateway, Terraform do RDS exclusivo, fila/tópicos do broker, pipeline CD, observabilidade (`Mekano-infrastructure-Observability`) |
| **7. Migração e corte** | Script de dados, feature flag, validação (Newman + comparação de contagens), rollback documentado |
| **8. Docs** | README, `docs/` com diagramas (C4, sequência dos fluxos entre serviços, EventStorming atualizado), contratos de eventos (schemas), ADRs |

## 8. Estrutura inicial proposta

```
Mekano-OS-Service-API/
├── pom.xml  mvnw  Makefile  docker-compose.yml  .env.example
├── AGENTS.md  README.md  docs/  newman/  .github/workflows/ci.yml
├── os-domain/          # model, valueobject, event, port/in, port/out, exception
├── os-application/     # service/ordemdeservico
├── os-infrastructure/  # entity, repository, mapper, audit (AuditoriaListener), event (outbox + consumers), client (REST), db/migration
└── os-rest/            # api/OrdemDeServicoResource, dto, mapper, exception, observability (quarkus packaging)
```

## 9. Riscos

- Consistência distribuída (estoque × OS) → outbox + idempotência + compensações.
- Latência/indisponibilidade do monolito → timeouts, circuit breaker, snapshot de dados.
- Duplicação de VOs/Models de referência → manter só IDs e snapshots; evitar biblioteca compartilhada de domínio.
- Dívidas conhecidas do monolito (`OrdemDeServico3`, `OrdemServicoEntity` duplicada, ports PT-BR) → **não** portar; limpar na migração.
- Migrations com histórico "remendado" no monolito → começar com `V1` limpo.

## 10. Critérios de pronto

- Todos os fluxos de OS (RF04–RF10) passam na coleção Newman contra o serviço isolado.
- Cobertura JaCoCo ≥ 80%; CI verde.
- Sem imports de repositories de Cliente/Veículo/Peça/Serviço/Orçamento no serviço.
- Eventos publicados via outbox e consumidos de forma idempotente; contratos em `docs/events/` validados com os repos de Orçamento, Pagamento, Auditoria e WhatsApp.
- Deploy em K8s com probes, HPA e rota no gateway; rollback testado.
