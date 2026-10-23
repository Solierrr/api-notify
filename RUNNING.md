# Rodando o Projeto Localmente

Este repositório é Java 21 + Spring Boot, buildado com Maven. O processo local é sempre o mesmo: clonar, baixar as dependências via `mvnw` e subir a aplicação. O `api-notify` depende de um MongoDB acessível e, para as rotas de usuário, de um `api-auth` no ar expondo o JWKS. Veja os impedimentos abaixo antes de iniciar.

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=java,springboot,spring,github,mongodb" height="48" alt="Rodando o Projeto, api-notify">
  </a>
</p>

## Possíveis Impedimentos

- **JDK 21 instalado localmente**, a mesma versão usada no [`Dockerfile`](./Dockerfile). Rodar fora do container exige essa versão configurada como `JAVA_HOME`.
- **MongoDB acessível**, o serviço se conecta via `DB_MONGO_URI` (padrão `mongodb://localhost:27017`) no banco `DB_MONGO_NOTIFY` (padrão `solaria_notify_db`). Sem Mongo a aplicação não sobe: a criação automática de índices no startup espera o servidor por cerca de 30 segundos e então aborta com `Timed out while waiting for a server`. O jeito mais simples de ter um Mongo local é `docker run -d -p 27017:27017 --name mongo-notify mongo:7.0`.
- **`api-auth` local para o JWT de usuário**, `JWT_JWK_SET_URI` aponta por padrão para `http://localhost:8081/.well-known/jwks.json`. Sem o `api-auth` nessa porta, toda rota `/notify/**` responde `401`, porque o JWKS não pode ser buscado para validar a assinatura.
- **Chave de serviço**, `SERVICE_KEY_API_CORE` e `SERVICE_KEY_API_MESSENGER` não têm valor padrão. Sem elas, qualquer chamada a `/internal/**` responde `401`. Chaves configuradas precisam ter pelo menos 32 caracteres, senão a aplicação não sobe. Para testar localmente, defina uma chave qualquer no `.env` e envie o mesmo valor no header `X-Service-Key`.
- **`FCM_ENABLED=false`**, o padrão local: o `PushSender` só registra o envio em log, sem falar com o Firebase. Com `FCM_ENABLED=true` o envio é real e exige `FCM_CREDENTIALS_BASE64` (conta de serviço do `solaria-authenticator` em base64, gerada com `base64 -w0 <arquivo>.json` direto para o `.env`); `FIREBASE_PROJECT_ID` é opcional e, em branco, o ID vem do `project_id` da conta de serviço; credencial vazia ou inválida impede a aplicação de subir. Use `FCM_DRY_RUN=true` para validar credencial e payload no FCM sem entregar push.
- **Secrets locais**, as variáveis injetadas em runtime pelo [Infisical](https://infisical.com) precisam ser criadas manualmente em `.env` na raiz do projeto (veja [`.env.example`](./.env.example)).

## Instalação do Projeto

### Iniciando o repositório com o Github

```Comandos para clonar o repositório
git clone https://github.com/Solierrr/api-notify.git
cd ./api-notify
```

### Instalando dependências e subindo a aplicação

Use sempre o wrapper (`mvnw`/`mvnw.cmd`) em vez de um Maven instalado globalmente, para garantir a mesma versão usada no CI. Copie `.env.example` para `.env` e preencha as variáveis descritas acima.

```Comandos para instalação de dependências e execução
./mvnw dependency:go-offline
./mvnw spring-boot:run
```

A aplicação sobe por padrão na porta `8080`. `GET /actuator/health` confirma que o serviço está no ar e que o Mongo responde.

### Testando os endpoints

O Swagger UI fica em `/swagger-ui.html`, sem autenticação. As rotas `/notify/**` exigem `Authorization: Bearer {token}` com o access token do `api-auth`; as rotas `/internal/**` exigem o header `X-Service-Key`. Exemplo de disparo interno:

```Disparo de uma notificação
curl -X POST http://localhost:8080/internal/notifications \
  -H "X-Service-Key: $SERVICE_KEY_API_CORE" \
  -H "Content-Type: application/json" \
  -d '{"authId":"<uuid do usuário>","type":"NEW_MESSAGE","title":"Nova mensagem","body":"Você recebeu uma mensagem","data":{"chatId":"1"}}'
```

A coleção Bruno em [`http/`](./http) traz requests de exemplo; selecione o ambiente `local` e preencha tokens e chaves somente no Bruno local.

### Rodando os testes

```Comandos de teste
./mvnw test
```

`NotificationFlowTests` roda o fluxo completo (aparelho, disparo, histórico, leitura, isolamento entre usuários e índices) contra um MongoDB real, por isso precisa de um Mongo em `localhost:27017`. Ele usa o banco isolado `solaria_notify_flow_test` e o apaga ao terminar. No CI, o `ci.yml` sobe um `mongo:7.0` como service container.
