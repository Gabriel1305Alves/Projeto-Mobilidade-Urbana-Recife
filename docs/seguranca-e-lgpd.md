# Segurança da informação e LGPD — Embarcaí

O Embarcaí trata dados de quem consulta linha de ônibus e publica alerta no Grande Recife. Este documento registra quais dados são esses, o checklist de segurança aplicado e as falhas que foram corrigidas.

## 1. Dados pessoais

| Dado | Onde nasce | Para que serve | O que não fazemos |
| --- | --- | --- | --- |
| Nome | Cadastro | Assinar o alerta enquanto a conta existe | Não vai para propaganda |
| E-mail | Cadastro e login | Identificar a conta e impedir cadastro duplicado | Não aparece na tela pública da linha |
| Senha | Cadastro e login | Autenticar o passageiro | Não é gravada em texto puro e não volta na API |
| Data do consentimento | Aceite no cadastro | Registrar que a conta foi criada com autorização | Não substitui o texto da página de privacidade |
| Localização | GPS, só se o navegador permitir | Mostrar o ponto do alerta ou as linhas perto | Não é coletada em segundo plano |
| Alerta | Relato na linha | Avisar trânsito, alagamento, bloqueio ou acidente | Some da listagem ativa depois de 2 horas |

A base usada no protótipo é o consentimento do cadastro (art. 7º, I, da LGPD). Sem o aceite, `POST /api/cadastrar` responde 400 e a conta não é criada.

## 2. Vulnerabilidades corrigidas

`POST /api/login` aceitava tentativas sem fim. O mesmo endereço agora tenta o login 8 vezes em 10 minutos. A nona recebe 429.

`POST /api/linhas` aceitava qualquer pessoa que soubesse a senha compartilhada, sem conferir se era administrador. Agora só entra quem está logado com o papel `ADMIN`. Passageiro recebe 403. Sem login, 401. Não existe rota para apagar o relato de outra pessoa.

O CORS aceitava qualquer site. Agora só aceita o endereço do Embarcaí (`app.base-url`, `http://localhost:5173` e `http://127.0.0.1:5173`).

A descrição do relato acima de 280 caracteres é recusada. Os caracteres `<` e `>` são removidos antes de gravar.

## 3. Checklist

| Controle | Estado |
| --- | --- |
| Senha do passageiro gravada com BCrypt, nunca devolvida pela API | Aplicado |
| Relato e confirmação só com login | Aplicado |
| O mesmo alerta não se repete na linha por 30 minutos | Aplicado |
| No máximo 8 relatos em 10 minutos por aparelho | Aplicado |
| No máximo 8 logins em 10 minutos por endereço | Corrigido |
| Cadastro de linha só com papel ADMIN | Corrigido |
| CORS restrito ao endereço do Embarcaí | Corrigido |
| Descrição do relato limitada a 280 caracteres | Corrigido |
| A listagem pública não devolve e-mail nem hash da senha | Aplicado |
| Rota desconhecida da API recusada | Aplicado |
| Cadastro só com consentimento | Aplicado |
| Página `/privacidade` descreve os dados e a finalidade | Aplicado |
| `/conta` mostra os dados e permite apagar a conta | Aplicado |
| Ao apagar a conta, o alerta fica na linha e o autor vira "Passageiro" | Aplicado |
| Logout remove a sessão do navegador | Aplicado |

A senha do banco e a chave do JWT podem ser trocadas por `DB_PASSWORD` e `JWT_SECRET`. Sem a variável, o protótipo local usa um valor só de desenvolvimento.

## 4. Como ver no sistema

1. Abra `/privacidade`.
2. Em `/cadastro`, tente criar a conta sem marcar o aceite. O botão permanece desabilitado. Se a API receber o cadastro sem `"consentimento": true`, responde 400.
3. Entre e abra `/conta`. Os dados exibidos vêm de `GET /api/me`.
4. Apague a conta. O login deixa de funcionar e o alerta antigo fica com o autor "Passageiro".
