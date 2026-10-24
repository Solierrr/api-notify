# Finalidade do repositório

O `api-notify` é o serviço de notificações push da Solaria, usado pelo app mobile. Ele registra os aparelhos de cada pessoa (identificados pelo Firebase Installation ID, o FID, junto do token FCM atual), guarda o histórico de notificações de cada usuário no MongoDB e expõe uma rota interna para que outros serviços (`api-messenger`, `api-core`) peçam o disparo de uma notificação. As rotas de usuário são protegidas pelo JWT emitido pelo `api-auth`; a rota interna é protegida por uma chave de serviço (`X-Service-Key`) por serviço chamador.

O envio push em si fica atrás da interface `PushSender`. Há duas implementações: a padrão registra o envio em log, sem chamar o Firebase, e com `FCM_ENABLED=true` o envio real é feito via FCM (`firebase-admin`).

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

O contrato completo, com a rota interna, está em [`openapi.yaml`](./openapi.yaml), gerado do código por `make openapi` e conferido no CI. Com o serviço no ar, o Swagger UI fica em `/docs` e o contrato em `/openapi.json`, sem a rota interna; `DOCS_ENABLED=false` desliga os dois (PROD).

Todo erro responde `application/problem+json` no [contrato de erro](https://github.com/Solierrr/docs-warehouse/blob/main/docs/standards/ERRORS.md) da organização, com `errorCode` do [catálogo](https://github.com/Solierrr/docs-warehouse/blob/main/docs/standards/ERROR_CODES.md): corpo ou parâmetro inválido responde `422` (`VALIDATION_FAILED` ou `MALFORMED_REQUEST`), recurso de outra pessoa ou inexistente `404` (`NOT_FOUND`) e falta de credencial `401` (`UNAUTHENTICATED`).

## Variáveis de ambiente

Veja [`.env.example`](./.env.example). Os valores reais vêm do Infisical em runtime; localmente ficam num `.env` que não é versionado.

| Variável | Uso |
|---|---|
| `DB_MONGO_URI` | URI base do MongoDB (padrão `mongodb://localhost:27017`). |
| `DB_MONGO_NOTIFY` | Nome do banco (padrão `solaria_notify_db`). |
| `JWT_JWK_SET_URI` | JWKS do `api-auth` para validar o JWT de usuário (padrão `http://localhost:8081/.well-known/jwks.json`). |
| `CORS_ALLOWED_ORIGINS` | Origens aceitas no CORS, separadas por vírgula (padrão `http://localhost:3000,http://localhost:5173`). No QA inclui a origem do `web-sandbox`, que abre o Swagger. |
| `DOCS_ENABLED` | `true` (padrão) expõe o Swagger em `/docs` e o contrato em `/openapi.json`; `false` desliga os dois. Em PROD fica `false`. |
| `SERVICE_KEY_API_MESSENGER` | Chave de serviço do `api-messenger`. Vazia, o serviço nunca autentica. |
| `SERVICE_KEY_API_CORE` | Chave de serviço do `api-core`. Vazia, o serviço nunca autentica. |
| `FCM_ENABLED` | `true` liga o envio real via Firebase Cloud Messaging; `false` (padrão) usa o `PushSender` que só registra o envio em log. O valor deve ser exatamente `true` ou `false`: qualquer outro (ou vazio) deixa a aplicação sem `PushSender` e ela não sobe. Com `true`, `FCM_CREDENTIALS_BASE64` é obrigatória, senão a aplicação não sobe. |
| `FCM_CREDENTIALS_BASE64` | Conta de serviço do projeto `solaria-authenticator` em base64, numa pasta própria do Infisical (`/fcm`). Gere com `base64 -w0 <arquivo>.json` direto para o `.env` local, sem colar o conteúdo em chat, log ou repositório. |
| `FIREBASE_PROJECT_ID` | Opcional. ID do projeto Firebase (`solaria-authenticator`, o mesmo do `api-auth`); em branco, o ID vem do `project_id` da conta de serviço. |
| `FCM_DRY_RUN` | `true` envia ao FCM em modo de validação: credencial e payload são checados, mas nenhum push é entregue. Padrão `false`. Nesse modo os tokens válidos entram em `sent` mesmo sem entrega, e os aparelhos de token inválido continuam sendo removidos. |
| `FCM_CONNECT_TIMEOUT_MS` | Timeout de conexão HTTP com o FCM, em milissegundos. Padrão `5000`. |
| `FCM_READ_TIMEOUT_MS` | Timeout de leitura HTTP com o FCM, em milissegundos. Padrão `10000`. |
| `OTEL_EXPORTER_OTLP_ENDPOINT`, `SERVICE_VERSION`, `DEPLOYMENT_ENVIRONMENT`, `OTEL_TRACES_SAMPLING_PROBABILITY` | Observabilidade (OpenTelemetry). |

### Envio via FCM

- **Mensagem só de dados**, sem o bloco `notification`, com prioridade Android `HIGH`. As chaves fixas são `notificationId`, `type`, `title` e `body`, seguidas das chaves de `data` da requisição.
- **Lotes de 500 tokens** por chamada ao FCM. `sent`, `failed` e `removedDevices` somam todos os lotes.
- **Token inválido remove o aparelho**: `UNREGISTERED`, `INVALID_ARGUMENT` e `SENDER_ID_MISMATCH`. Qualquer outro erro por token (como `UNAVAILABLE` ou `QUOTA_EXCEEDED`) conta como `failed` e mantém o aparelho.
- **Falhas por token**: o SDK devolve erros de credencial (`UNAUTHENTICATED`, `PERMISSION_DENIED`) e de rede por token, não pela chamada. Esses tokens contam como `failed`, o aparelho fica e o envio loga um `WARN` com o resumo dos códigos (`codes={PERMISSION_DENIED=3, ...}`), nunca os tokens.
- **Falha da chamada inteira**: a exceção da chamada (`PushDeliveryException`) é praticamente só de interrupção da thread. Se um lote lança, os tokens dele contam como `failed` e os seguintes continuam; só quando todos os lotes lançam o envio é falha total (`failed` igual ao número de aparelhos, nenhum removido). Em qualquer caso a notificação continua gravada.
- **Credencial de outro projeto**: se todas as respostas do envio forem `SENDER_ID_MISMATCH`, os aparelhos não são removidos e o envio loga um `ERROR` avisando que a credencial provavelmente é de outro projeto Firebase.
- **Limite de payload**: a soma dos bytes UTF-8 de todas as chaves e valores (fixos mais `data`) não pode passar de 3800 bytes, margem sob os 4096 do FCM. Acima disso a rota responde `422` antes de gravar. O limite fica na descrição da operação no `openapi.yaml`, não no schema.
- O token FCM, a credencial e as chaves de serviço nunca são logados.

## Health e versão

- Vivo: `GET /actuator/health/liveness`, só o processo.
- Pronto: `GET /actuator/health/readiness`, que inclui o Mongo; responde `503` quando o Mongo não responde. É o caminho do `healthCheckPath` do Render e do `environment-status`.
- Versão: `GET /actuator/info` mostra `build.version`, a mesma do `pom.xml` e do `info.version` do contrato, que o release-please atualiza a cada release.

## Deploy

- **QA**: o Render faz deploy automático a partir da branch `qa`.
- **PROD**: a imagem `solarianetwork/api-notify` é publicada no push em `main` e o ArgoCD a implanta a partir do `infra-gitops`, no host `api-notify.solarianetwork.site`.

## Aprofunde-se no Projeto!

- [ARCHITECTURE.md](./ARCHITECTURE.md), decisões de desenho, modelagem no MongoDB e árvore do repositório.
- [RUNNING.md](./RUNNING.md), como subir o `api-notify` localmente e rodar os testes.
- [DEPLOYMENT.md](https://github.com/Solierrr/.github/blob/main/.github/DEPLOYMENT.md), pipeline de deploy padrão da organização.
