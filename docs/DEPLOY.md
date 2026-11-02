<p align="center">
  <img src="../assets/readme/banners/render.png" alt="Render" width="100%">
</p>

# Deploy

O processo comum está em [QA_RENDER.md](https://github.com/Solierrr/docs-warehouse/blob/main/readme/deploy/QA_RENDER.md) e em [PROD_GKE_ARGOCD.md](https://github.com/Solierrr/docs-warehouse/blob/main/readme/deploy/PROD_GKE_ARGOCD.md). Aqui fica só o que é deste serviço.

## Ambientes

| Ambiente | Onde | Endereço |
|---|---|---|
| Local | máquina de quem desenvolve | http://localhost:8080 |
| QA | Render, branch `qa` | https://api-notify-i637.onrender.com |
| Produção | GKE, pelo Argo CD | manifestos no `infra-gitops` |

## Particularidades

- No Render, o `healthCheckPath` deve ser `/actuator/health/readiness`.
- O serviço lê os segredos do Infisical na partida, pelo `entrypoint.sh`, quando recebe `INFISICAL_CLIENT_ID` e `INFISICAL_CLIENT_SECRET`.
- O banco no QA e em produção é o MongoDB Atlas.
- A partida no plano gratuito do Render leva cerca de dois minutos e meio.
