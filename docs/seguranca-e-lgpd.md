# Segurança da informação e LGPD — Embarcaí

Critério: boas práticas de segurança da informação e a Lei Geral de Proteção de Dados aplicadas ao projeto de mobilidade.

O Embarcaí trata dados de quem consulta linha de ônibus e publica alerta no Grande Recife. Este documento descreve quais dados são esses, para que servem e o que o sistema faz com eles.

## 1. Dados pessoais

| Dado | Onde nasce | Para que serve | O que não fazemos |
| --- | --- | --- | --- |
| Nome | Cadastro | Assinar o alerta enquanto a conta existe | Não vai para propaganda |
| E-mail | Cadastro e login | Identificar a conta e impedir cadastro duplicado | Não aparece no alerta público |
| Senha | Cadastro e login | Autenticar o passageiro | Não é gravada em texto puro e não volta na API |
| Data do consentimento | Aceite no cadastro | Registrar que a conta foi criada com autorização | Não substitui o texto da página de privacidade |
| Localização | GPS, só se o navegador permitir | Mostrar o ponto do alerta ou as linhas perto | Não é coletada em segundo plano |
| Alerta | Relato na linha | Avisar trânsito, alagamento, bloqueio ou acidente | Some da listagem ativa depois de 2 horas |

A base usada no protótipo é o consentimento do cadastro (art. 7º, I, da LGPD). Sem o aceite, `POST /api/cadastrar` responde 400 e a conta não é criada.

## 2. Práticas aplicadas

- Senha com BCrypt antes de ir ao banco. A resposta de cadastro, login e `GET /api/me` não inclui a senha.
- Relato e confirmação exigem token JWT. Sem login, a API responde 401.
- O mesmo passageiro não repete o mesmo tipo de alerta na mesma linha por 30 minutos. Há também limite de envios por aparelho.
- O login aceita no máximo 8 tentativas a cada 10 minutos por endereço. A nona recebe 429. O checklist e a vulnerabilidade corrigida estão em [checklist-seguranca.md](checklist-seguranca.md).
- A página de administração não mostra a senha da equipe, e a mensagem de erro também não revela qual é a senha.
- A página `/privacidade` explica o tratamento em linguagem do projeto. O cadastro só segue com a caixa de consentimento marcada.
- Em `/conta`, o passageiro vê nome, e-mail e papel. `DELETE /api/me` apaga a conta, apaga o vínculo das confirmações e troca o autor dos alertas para "Passageiro". O aviso da linha permanece, porque os outros passageiros ainda precisam dele, mas deixa de identificar a pessoa.
- A sessão sai do navegador no logout e também depois que a conta é apagada.

## 3. Como ver no sistema

1. Abra `/privacidade`.
2. Em `/cadastro`, tente criar a conta sem marcar o aceite. O botão permanece desabilitado. Se a API receber o cadastro sem `"consentimento": true`, responde 400.
3. Entre e abra `/conta`. Os dados exibidos vêm de `GET /api/me`.
4. Apague a conta. O login deixa de funcionar e o alerta antigo fica com o autor "Passageiro".
