<p align="center">
  <img src="./assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

<p align="center">
  <img src="./assets/readme/stacks/java.png" width="100" hspace="8" alt="Java" title="Java">
  <img src="./assets/readme/stacks/spring-security.png" width="100" hspace="8" alt="Spring Security" title="Spring Security">
  <img src="./assets/readme/stacks/jwt.png" width="100" hspace="8" alt="JWT" title="JWT">
  <img src="./assets/readme/stacks/mongodb.png" width="100" hspace="8" alt="MongoDB" title="MongoDB">
  <img src="./assets/readme/stacks/firebase.png" width="100" hspace="8" alt="Firebase" title="Firebase">
  <img src="./assets/readme/stacks/swagger.png" width="100" hspace="8" alt="Swagger" title="Swagger">
  <img src="./assets/readme/stacks/bruno.png" width="100" hspace="8" alt="Bruno" title="Bruno">
</p>

# Central de notificações

[![License](https://img.shields.io/github/license/Solierrr/api-notify)](https://github.com/Solierrr/api-notify/blob/main/LICENSE)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/Solierrr/api-notify)](https://github.com/Solierrr/api-notify/commits)
[![GitHub Pull Requests](https://img.shields.io/github/issues-pr/Solierrr/api-notify)](https://github.com/Solierrr/api-notify/pulls)
[![Release](https://img.shields.io/github/v/release/Solierrr/api-notify)](https://github.com/Solierrr/api-notify/releases)

Este serviço avisa as pessoas no celular quando algo importante acontece na plataforma, como uma mensagem nova ou uma mudança no andamento de um serviço contratado. Ele existe para que a pessoa não precise abrir o aplicativo para saber que há novidade. Além do aviso, guarda o histórico das notificações de cada pessoa por três meses, com o que já foi lido e o que ainda não foi.

Os outros serviços da plataforma pedem o envio, e este serviço entrega o aviso pelo Firebase Cloud Messaging a cada aparelho cadastrado da pessoa. O aplicativo cadastra o aparelho e consulta o histórico com o token JWT emitido pelo serviço de acesso. Os serviços que pedem envio se identificam com uma chave própria. As notificações e os aparelhos ficam no MongoDB.

## Aprofunde-se no Projeto!

- [OVERVIEW.md](./docs/OVERVIEW.md), o que o serviço oferece e as regras de cada notificação.
- [ARCHITECTURE.md](./docs/ARCHITECTURE.md), organização do código, integrações e decisões técnicas.
- [RUNNING.md](./docs/RUNNING.md), como executar e testar localmente.
- [API.md](./docs/API.md), como autenticar e chamar o serviço.
- [DEPLOY.md](./docs/DEPLOY.md), ambientes e publicação.
- [STATUS.md](./docs/STATUS.md), o que já funciona e o que falta.
- [Fluxos](./flows/), o cadastro de aparelho, o envio e a leitura das notificações.
- [Banco de dados](./database/MONGO.md), as coleções e os índices.

## Contribuindo

- [CONTRIBUTING.md](https://github.com/Solierrr/.github/blob/main/.github/CONTRIBUTING.md), convenções de commit, branch e Pull Request.
- [CODE_OF_CONDUCT.md](https://github.com/Solierrr/.github/blob/main/CODE_OF_CONDUCT.md), código de conduta.
- [SECURITY.md](https://github.com/Solierrr/.github/blob/main/SECURITY.md), como reportar uma vulnerabilidade.
