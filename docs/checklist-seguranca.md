# Checklist de segurança — Embarcaí

Revisão feita no código que o passageiro usa para consultar a linha e publicar alerta.

## Vulnerabilidades corrigidas

`POST /api/login` aceitava tentativas sem fim. O mesmo endereço agora tenta o login 8 vezes em 10 minutos. A nona recebe 429.

`POST /api/linhas` aceitava qualquer pessoa que soubesse a senha compartilhada, sem conferir se era administrador. Agora só entra quem está logado com o papel `ADMIN`. Passageiro recebe 403. Sem login, 401. Não existe rota para apagar o relato de outra pessoa.

O CORS aceitava qualquer site. Agora só aceita o endereço do Embarcaí (`app.base-url`, `http://localhost:5173` e `http://127.0.0.1:5173`).

A descrição do relato acima de 280 caracteres é recusada. Os caracteres `<` e `>` são removidos antes de gravar. A tela já mostra o texto como texto, sem HTML.

## O que a revisão do grupo perguntou

| Pergunta | Resultado no Embarcaí |
| --- | --- |
| Passageiro ou anônimo cadastra linha? | Corrigido. Só `ADMIN` autenticado. |
| Passageiro apaga relato de outro? | Não há essa rota. |
| Senha em texto puro? | Não. O cadastro grava BCrypt. |
| `GET` da linha devolve o usuário inteiro, e-mail ou hash? | Não. A resposta é um DTO: tipo, mensagem, nome público, confirmações e horário. |
| Segredos no `application.properties`? | A senha do banco e a chave do JWT podem ser trocadas por `DB_PASSWORD` e `JWT_SECRET`. Sem a variável, o protótipo local usa um valor só de desenvolvimento. |
| CORS com `*`? | Corrigido. |
| Texto livre do relato sem tamanho máximo? | Corrigido. Máximo de 280 caracteres na API e no formulário. |

## Checklist

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
| Rota desconhecida da API recusada | Aplicado |
| Cadastro só com consentimento | Aplicado |
| Página `/privacidade` descreve os dados e a finalidade | Aplicado |
| `/conta` mostra os dados e permite apagar a conta | Aplicado |
| Ao apagar a conta, o alerta fica na linha sem o nome da pessoa | Aplicado |
| Logout remove a sessão do navegador | Aplicado |
