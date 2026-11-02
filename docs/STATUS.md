<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Situação

## Funciona

- Cadastro e remoção de aparelhos, histórico, contagem de não lidas e leitura, com o JWT do `api-auth` (2026-10-24).
- Rota interna de envio com `X-Service-Key`, gravação e push pelo Firebase (2026-10-24).
- Contrato OpenAPI no padrão da organização, erros em `ProblemDetail` e health checks (2026-10-25).
- QA no ar no Render (2026-10-25).

## Falta

- Nenhum serviço chama a rota de envio ainda. O `api-messenger` e o `api-core` precisam passar a pedir as notificações (2026-10-29).
- O app mobile ainda não cadastra o aparelho nem recebe push (2026-10-29).
- O QA ainda não aceita chamadas do portal de testes (CORS do `web-sandbox`) (2026-10-29).
- O Swagger do QA lista uma seção "Interno" vazia e seleciona o servidor local por padrão (2026-10-25).
- Sem coletor OpenTelemetry, o log do QA registra erros de exportação a cada poucos segundos (2026-10-25).
