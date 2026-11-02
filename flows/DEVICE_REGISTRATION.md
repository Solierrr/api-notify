<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Cadastro de aparelho

Acontece quando a pessoa entra no aplicativo e quando o Firebase troca o token do aparelho. O resultado é o aparelho ligado à conta dela e pronto para receber avisos. Ao sair da conta, o aplicativo remove o aparelho.

```mermaid
sequenceDiagram
    participant App as App mobile
    participant Notify as api-notify
    participant Mongo as MongoDB

    App->>Notify: PUT /notify/devices/{installationId} com token, plataforma e versão (JWT)
    Notify->>Mongo: busca o aparelho pelo installationId
    alt aparelho novo
        Notify->>Mongo: cria o aparelho da pessoa
    else aparelho já existe, de qualquer conta
        Notify->>Mongo: atualiza token e passa o aparelho para a pessoa atual
    end
    Notify-->>App: 200 com o aparelho

    App->>Notify: DELETE /notify/devices/{installationId} ao sair (JWT)
    alt aparelho da pessoa
        Notify->>Mongo: remove
        Notify-->>App: 204
    else aparelho de outra pessoa ou inexistente
        Notify-->>App: 404 NOT_FOUND
    end
```

## Erros

- Sem JWT ou com JWT inválido, a resposta é `401 UNAUTHENTICATED`.
- Token, plataforma ou versão fora do formato dão `422 VALIDATION_FAILED`.
