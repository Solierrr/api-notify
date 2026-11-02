<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Leitura das notificações

Acontece quando a pessoa abre a lista de notificações no aplicativo. O resultado é o histórico exibido e as notificações marcadas como lidas.

```mermaid
flowchart TD
  A[Pessoa abre as notificações] --> B[GET /notify/notifications]
  B --> C{Só não lidas?}
  C -->|sim| D[Lista as não lidas, mais recentes primeiro]
  C -->|não| E[Lista todas, mais recentes primeiro]
  D --> F[Pessoa toca numa notificação]
  E --> F
  F --> G[PATCH /notify/notifications/id/read]
  G --> H{É da pessoa?}
  H -->|não| I[404 NOT_FOUND]
  H -->|sim, já lida| J[Mantém a data de leitura original]
  H -->|sim, não lida| K[Grava a data de leitura]
  A --> L[Marcar todas como lidas]
  L --> M[PATCH /notify/notifications/read-all]
```

O ícone de não lidas usa `GET /notify/notifications/unread-count`.
