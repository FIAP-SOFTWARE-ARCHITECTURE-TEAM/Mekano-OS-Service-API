# Mekano OS Service API

Microserviço de **Ordem de Serviço** do ecossistema Mekano, extraído do monolito [Mekano-API](../Mekano-API).

- Java 17 · Quarkus 3.40 · Maven multi-módulo · Clean Architecture
- PostgreSQL (RDS exclusivo) · Flyway · Panache · MapStruct
- Mensageria: SNS + SQS (LocalStack em dev)
- Autenticação: JWT EdDSA validado com chave pública

## Módulos

| Módulo | Responsabilidade |
|---|---|
| `os-domain` | Modelos, VOs, eventos, ports (sem framework) |
| `os-application` | Services / casos de uso |
| `os-infrastructure` | JPA, repositórios, outbox, REST clients, mensageria |
| `os-rest` | Resources JAX-RS, DTOs, configuração, entrypoint Quarkus |

## Rodando localmente

```bash
cp .env.example .env
make up      # Postgres (porta 5433) + LocalStack (SNS/SQS)
make dev     # http://localhost:8080
```

- Swagger UI: `http://localhost:8080/q/swagger-ui`
- Health: `http://localhost:8080/q/health`
- API: prefixo `/api/v1`

> O monolito também usa a porta 8080. Ao rodar os dois, defina `QUARKUS_HTTP_PORT` em um deles.

## Testes e build

```bash
make test
make build   # verify + relatório JaCoCo agregado (os-rest/target/site/jacoco-aggregate)
```

## Documentação
- [Plano do microserviço](docs/PLANO_MICROSERVICO.md)
- [AGENTS.md](AGENTS.md) — convenções para contribuidores e agentes
