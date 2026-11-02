<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Visão de produto

A central de notificações avisa a pessoa no celular quando algo acontece na plataforma e guarda o histórico desses avisos, atendendo apenas o aplicativo mobile, sem notificação no navegador.

## O que a pessoa consegue fazer

- Receber um aviso no celular quando chega uma mensagem nova ou quando um serviço contratado é agendado ou muda de situação.
- Ver a lista das notificações dos últimos três meses, da mais recente para a mais antiga.
- Ver quantas notificações ainda não leu.
- Marcar uma notificação como lida, ou todas de uma vez.
- Usar mais de um aparelho na mesma conta e receber o aviso em todos.

## Regras

- O histórico guarda cada notificação por 90 dias. Depois disso ela some sozinha, porque o aplicativo só mostra o período recente.
- A notificação é gravada mesmo quando o aviso no celular falha ou quando a pessoa não tem aparelho cadastrado. Ela aparece no histórico do mesmo jeito.
- Um aparelho pertence a uma conta por vez. Se outra pessoa entra no mesmo aparelho, ele passa a ser só dela, e quem saiu deixa de receber avisos ali.
- Ao sair da conta, o aplicativo remove o aparelho e os avisos param.
- Uma pessoa só vê e só altera as próprias notificações e os próprios aparelhos. Tentar acessar os de outra pessoa tem a mesma resposta de algo que não existe.
- Marcar de novo uma notificação já lida não muda a data em que ela foi lida.

## Tipos de notificação

| Tipo | Quando acontece |
|---|---|
| Mensagem nova | alguém mandou mensagem numa conversa da pessoa |
| Serviço agendado | um serviço contratado ganhou data |
| Mudança no serviço | um serviço contratado mudou de situação |



## Relação com o resto da plataforma

Quem decide que uma notificação deve existir são os outros serviços, como o de conversas e o cadastro central. A central só guarda e entrega. Hoje nenhum deles pede envio ainda, e o aplicativo ainda não cadastra o aparelho, por isso os avisos não chegam às pessoas. O histórico e as regras acima já funcionam.
