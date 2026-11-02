<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Envio de notificação

Acontece quando outro serviço decide avisar uma pessoa. O resultado é a notificação gravada no histórico e o aviso entregue em cada aparelho dela. A gravação vem antes do envio e nunca é desfeita por falha no push.

```mermaid
sequenceDiagram
    participant Svc as Serviço chamador
    participant Notify as api-notify
    participant Mongo as MongoDB
    participant FCM as Firebase Cloud Messaging

    Svc->>Notify: POST /internal/notifications (X-Service-Key)
    Notify->>Notify: confere a chave e o chamador
    Notify->>Mongo: grava a notificação com validade de 90 dias
    Notify->>Mongo: busca os aparelhos da pessoa
    alt pessoa sem aparelho
        Notify-->>Svc: 201 com nenhum envio
    else há aparelhos
        Notify->>FCM: envia para cada token
        FCM-->>Notify: entregues, falhas e tokens inválidos
        opt tokens inválidos
            Notify->>Mongo: remove os aparelhos cujo token ainda é o mesmo
        end
        Notify-->>Svc: 201 com entregues, falhas e removidos
    end
```

O pedido entre serviços segue o fluxo central de [chamada entre serviços](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/SERVICE_TO_SERVICE_AUTH.md).

## Erros

- Chave ausente ou desconhecida dá `401 UNAUTHENTICATED`, e chamador não aceito dá `403 FORBIDDEN`.
- Campos fora dos limites dão `422 VALIDATION_FAILED`.
- Falha no Firebase não gera erro para o chamador. A notificação fica gravada e a resposta informa as falhas.
