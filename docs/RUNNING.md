<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Executando localmente

O passo a passo comum dos serviços Java está em [JAVA.md](https://github.com/Solierrr/docs-warehouse/blob/main/readme/running/JAVA.md). Aqui fica só o que é deste serviço.

## Pré-requisitos

- Java 21.
- MongoDB local na porta 27017, por exemplo com `docker run -d -p 27017:27017 mongo:7`.
- `api-auth` rodando na porta 8081, para validar o JWT. Sem ele, só as rotas internas e o health funcionam.
- Infisical CLI logado, para gerar o `.env`.

## Variáveis

`make vault` grava o `.env` com as pastas `/database`, `/auth`, `/fcm`, `/otel` e `/service-urls` do Infisical. Sem o `.env`, o serviço sobe com os padrões locais, ou seja, Mongo em `localhost:27017`, banco `solaria_notify_db` e envio de push só no log.

Para testar o envio real pelo Firebase, ligue `FCM_ENABLED=true`. Com `FCM_DRY_RUN=true`, o Firebase valida a mensagem sem entregar.

## Comandos

| Comando | O que faz |
|---|---|
| `make dev` | sobe o serviço na porta 8080 |
| `make test` | roda os testes (precisa do Mongo local) |
| `make lint` | roda o Checkstyle, como no CI |
| `make openapi` | gera de novo o `openapi.yaml` a partir do código |

## Testando uma chamada

- Swagger em http://localhost:8080/docs, com o botão Authorize para colar o JWT.
- Coleção Bruno na pasta `http/` do repositório, com as rotas de aparelhos, notificações, interna e health.
- `GET /actuator/health/readiness` responde `UP` quando o Mongo está acessível.

## Problemas conhecidos

- Sem coletor OpenTelemetry local, o log mostra erros de exportação para `localhost:4318`. Eles não afetam o serviço.
