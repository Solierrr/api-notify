# Finalidade do repositório

O `api-notify` é o serviço de notificações push da Solaria, usado pelo app mobile. Ele registra os aparelhos de cada pessoa (identificados pelo Firebase Installation ID, o FID, junto do token FCM atual), guarda o histórico de notificações de cada usuário no MongoDB e expõe uma rota interna para que outros serviços (`api-messenger`, `api-core`) peçam o disparo de uma notificação. As rotas de usuário são protegidas pelo JWT emitido pelo `api-auth`; a rota interna é protegida por uma chave de serviço (`X-Service-Key`) por serviço chamador.

O envio push em si fica atrás da interface `PushSender`. Na Fase 1 existe apenas uma implementação que registra o envio em log, sem chamar o Firebase; o envio real via FCM (`firebase-admin`) entra na Fase 2.

<p>

[![License](https://img.shields.io/github/license/Solierrr/api-notify)](https://github.com/Solierrr/api-notify/blob/main/LICENSE)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/Solierrr/api-notify)](https://github.com/Solierrr/api-notify/commits)
[![GitHub Issues](https://img.shields.io/github/issues/Solierrr/api-notify)](https://github.com/Solierrr/api-notify/issues)
[![GitHub Pull Requests](https://img.shields.io/github/issues-pr/Solierrr/api-notify)](https://github.com/Solierrr/api-notify/pulls)

</p>

<div align="center">

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=java,springboot,spring,mongodb,swagger" height="48" alt="Stack do api-notify">
  </a>
</p>

<p>

[![Java](https://img.shields.io/badge/Java_21-437291?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![MongoDB](https://img.shields.io/badge/MongoDB-47A248?logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Swagger](https://img.shields.io/badge/Swagger-85EA2D?logo=swagger&logoColor=black)](https://swagger.io/)

</p>

</div>

## Rotas

### `/notify/**`: JWT de usuário (`api-auth`)

| Método | Rota | Descrição |
|---|---|---|
| `PUT` | `/notify/devices/{installationId}` | Registra ou atualiza o aparelho (corpo `{fcmToken, platform, appVersion?}`). Se o FID era de outra pessoa, passa a ser de quem chamou. |
| `DELETE` | `/notify/devices/{installationId}` | Remove o aparelho (logout). Só o dono remove; qualquer outro caso responde `404`. |
| `GET` | `/notify/notifications?page&size&unreadOnly` | Histórico paginado, mais recente primeiro. `size` acima de 100 é limitado a 100. |
| `GET` | `/notify/notifications/unread-count` | Quantidade de notificações não lidas. |
| `PATCH` | `/notify/notifications/{id}/read` | Marca uma notificação como lida (`404` se não for do usuário). |
| `PATCH` | `/notify/notifications/read-all` | Marca todas como lidas. |

### `/internal/**`: chave de serviço (`X-Service-Key`)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/internal/notifications` | Grava a notificação, envia o push aos aparelhos do destinatário e responde `201` com `{id, sent, failed, removedDevices}`. |

O contrato completo está em [`openapi.yaml`](./openapi.yaml) e, com o serviço no ar, em `/swagger-ui.html`.

## Variáveis de ambiente

Veja [`.env.example`](./.env.example). Os valores reais vêm do Infisical em runtime; localmente ficam num `.env` que não é versionado.

| Variável | Uso |
|---|---|
| `DB_MONGO_URI` | URI base do MongoDB (padrão `mongodb://localhost:27017`). |
| `DB_MONGO_NOTIFY` | Nome do banco (padrão `solaria_notify_db`). |
| `JWT_JWK_SET_URI` | JWKS do `api-auth` para validar o JWT de usuário (padrão `http://localhost:8081/.well-known/jwks.json`). |
| `SERVICE_KEY_API_MESSENGER` | Chave de serviço do `api-messenger`. Vazia, o serviço nunca autentica. |
| `SERVICE_KEY_API_CORE` | Chave de serviço do `api-core`. Vazia, o serviço nunca autentica. |
| `FIREBASE_ENABLED` | Reservada para a Fase 2. Mantenha `false`: com `true` não há `PushSender` configurado e a aplicação não sobe. |
| `OTEL_EXPORTER_OTLP_ENDPOINT`, `SERVICE_VERSION`, `DEPLOYMENT_ENVIRONMENT`, `OTEL_TRACES_SAMPLING_PROBABILITY` | Observabilidade (OpenTelemetry). |

## Aprofunde-se no Projeto!

- [ARCHITECTURE.md](./ARCHITECTURE.md), decisões de desenho, modelagem no MongoDB e árvore do repositório.
- [RUNNING.md](./RUNNING.md), como subir o `api-notify` localmente e rodar os testes.
- [DEPLOYMENT.md](https://github.com/Solierrr/.github/blob/main/.github/DEPLOYMENT.md), pipeline de deploy padrão da organização.
