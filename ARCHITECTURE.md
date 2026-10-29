# Arquitetura do Repositório

O `api-notify` segue uma arquitetura em camadas típica de um serviço Spring Boot (`controller` → `service` → `repository` → `model`), sem separação `service`/`service.impl`. Os `controller` implementam uma interface `openapi` dedicada por recurso (`DeviceOpenApi`, `NotificationOpenApi`, `InternalNotificationOpenApi`), usada só para as anotações do Springdoc. O `OpenApiConfig` monta os metadados do contrato (versão lida do build), declara os esquemas `bearerAuth` e `serviceKey`, tira as rotas `/internal/` do contrato servido em `/openapi.json` e aplica o `ProblemDetail` a toda resposta de erro documentada; o `openapi.yaml` é exportado com as rotas internas pelo `OpenApiExportTests` (`make openapi`). A persistência é 100% MongoDB: `Device` e `Notification` são `@Document` independentes, sem transação entre eles.

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=java,springboot,mongodb" height="48" alt="Arquitetura do api-notify">
  </a>
</p>

## Decisões de desenho

- **Duas `SecurityFilterChain` independentes, segregadas por `@Order`** (`SecurityConfig`). A chain `@Order(1)` cobre só `/internal/**` e autentica pelo header `X-Service-Key` (`ServiceKeyAuthFilter`); a chain `@Order(2)` cobre o restante e é um resource server OAuth2 RS256 que valida o JWT de usuário do `api-auth` via JWKS (`app.jwt.jwk-set-uri`, emissor `solaria-auth`). O `authId` do usuário é a claim `sub` lida como UUID. Quem não é serviço na chain interna recebe `401`, nunca `403`.
- **`X-Service-Key` por serviço chamador** (`ServiceKeyProperties`, `app.service-keys.*`). Cada serviço tem a sua chave (`api-messenger`, `api-core`), comparada em tempo constante com `MessageDigest.isEqual`. Chave não configurada (vazia) nunca autentica. O nome do chamador vira o `sourceService` da notificação (`CurrentUser.serviceName()`), então o body nunca informa quem pediu.
- **Mensagem push só de dados.** O `PushMessage` carrega `notificationId`, `type`, `title`, `body` e o mapa `data`, sem o bloco `notification` do FCM. Assim o app monta a notificação Android (canal, ícone) e decide se exibe, usando as preferências locais do usuário (tipo desligado, tela correspondente aberta).
- **Histórico com TTL de 90 dias.** Cada `Notification` grava `expireAt = createdAt + 90 dias`, com índice TTL (`@Indexed(expireAfter = "0s")`) que faz o próprio Mongo remover o documento quando a data passa. Não há job de limpeza.
- **`PushSender` como ponto de extensão.** O `NotificationService` grava a notificação, busca os aparelhos do destinatário e chama `PushSender.send(tokens, message)`. A falha no envio nunca derruba a gravação, e usuário sem aparelhos grava e responde `201` com `sent=0`. Tokens que o sender devolve como inválidos têm o aparelho removido (`removedDevices`). Há duas implementações, exclusivas entre si: o `LoggingPushSender`, que registra o envio sem o token (`FCM_ENABLED=false`, padrão), e o `FcmPushSender`, que usa `firebase-admin` com a conta de serviço em `FCM_CREDENTIALS_BASE64` (`FCM_ENABLED=true`). O FCM recebe mensagem só de dados, em lotes de 500 tokens; `UNREGISTERED`, `INVALID_ARGUMENT` e `SENDER_ID_MISMATCH` removem o aparelho e os demais erros por token só contam como `failed`. Erros de credencial e de rede chegam por token (contam como `failed`, o aparelho fica e há um `WARN` com o resumo dos códigos); a exceção da chamada inteira é praticamente só de interrupção, e nesse caso o lote conta seus tokens como `failed` sem interromper os seguintes, e o `PushDeliveryException` só sai quando todos os lotes falham. Se todas as respostas forem `SENDER_ID_MISMATCH`, nenhum aparelho é removido (credencial provavelmente de outro projeto, `ERROR` no log). Payload acima de 3800 bytes UTF-8 responde `422` antes de gravar (`@FcmPayloadSize`). `FCM_DRY_RUN=true` valida sem entregar (tokens válidos contam em `sent` e os inválidos ainda removem o aparelho). Conexão e leitura HTTP têm timeout (`FCM_CONNECT_TIMEOUT_MS`, `FCM_READ_TIMEOUT_MS`).
- **Aparelhos identificados pelo FID.** `Device` usa o Firebase Installation ID como `_id` e guarda o token FCM atual, a plataforma (`ANDROID` ou `WEB`, esta para navegadores com Web Push) e a versão do app (em `WEB`, o nome do navegador). Como o FID é a chave, um aparelho tem no máximo um dono.
- **Troca de conta reatribui o aparelho.** Se a pessoa B registra um FID que era da pessoa A, o `PUT` apenas troca o `authId`: o aparelho passa a ser só de B e A deixa de listar ou receber push por ele.
- **Autorização por ownership.** Marcar como lida ou remover aparelho de outra pessoa responde `404`, igual a recurso inexistente, para não revelar a existência.
- **Tratamento de erro em `ProblemDetail`** (`GlobalExceptionHandler`, `ProblemDetailFactory`, `ProblemDetailAuthenticationEntryPoint`, `ProblemDetailAccessDeniedHandler`), sem vazar mensagem ou stacktrace (`spring.web.error.include-*=never`), no contrato de erro da organização: `type` `about:blank`, `errorCode` do catálogo, `timestamp`, `instance` e `traceId`, e `422` para corpo ou parâmetro inválido.
- **Índices no MongoDB** (`@Indexed`/`@CompoundIndex` e `spring.data.mongodb.auto-index-creation=true`): `auth_id` em `devices`; em `notifications`, o composto `{auth_id: 1, created_at: -1}` (histórico paginado) e o TTL em `expire_at`. `MongoConfig` fixa a representação de UUID como `STANDARD`.
- **Observabilidade** com Actuator (`health` e `info`, readiness incluindo `mongo`), Micrometer Tracing e exportação OTLP para o Collector.

## Modelagem no MongoDB

`devices`: `_id` (FID), `auth_id`, `fcm_token`, `platform`, `app_version`, `created_at`, `updated_at`.

`notifications`: `_id` (UUID), `auth_id`, `type` (`NEW_MESSAGE`, `SERVICE_SCHEDULED`, `SERVICE_STATUS_CHANGED`), `title`, `body`, `data` (mapa texto-texto), `source_service`, `created_at`, `read_at` (nulo = não lida), `expire_at`.

## Endpoints expostos

### `/notify/**`: autenticado por JWT de usuário (`api-auth`)

- `PUT /notify/devices/{installationId}`, registra ou atualiza o aparelho.
- `DELETE /notify/devices/{installationId}`, remove o aparelho do dono.
- `GET /notify/notifications`, histórico paginado (`page`, `size`, `unreadOnly`).
- `GET /notify/notifications/unread-count`, total de não lidas.
- `PATCH /notify/notifications/{id}/read`, marca uma como lida.
- `PATCH /notify/notifications/read-all`, marca todas como lidas.

### `/internal/**`: autenticado por `X-Service-Key`

- `POST /internal/notifications`, grava e dispara a notificação; responde `201` com `{id, sent, failed, removedDevices}`.

```Tree do Repositório
├── .github/
│   └── workflows/
├── src/
│   ├── main/
│   │   ├── java/com/solaria/notify/
│   │   │   ├── NotifyApplication.java
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── exception/
│   │   │   │   └── handler/
│   │   │   ├── model/
│   │   │   │   └── enums/
│   │   │   ├── observability/
│   │   │   ├── openapi/
│   │   │   ├── push/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/com/solaria/notify/
│       └── resources/
├── http/
├── .env.example
├── ARCHITECTURE.md
├── Dockerfile
├── README.md
├── RUNNING.md
├── mvnw
├── mvnw.cmd
├── openapi.yaml
└── pom.xml
```
