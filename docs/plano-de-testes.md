# Plano de testes — Embarcaí

Critério: teste de software com plano de testes, testes unitários e testes de integração aplicados ao projeto. Evidência de execução automatizada em 8 de novembro de 2026.

## 1. Objetivo

Garantir que as regras do Embarcaí e os fluxos da API se comportam como o passageiro espera: alerta expira, alagamento só vira "confirmado pela comunidade" com cinco pessoas, relato exige login e não pode ser repetido em 30 minutos, e a linha 020 responde com status e QR Code.

## 2. Escopo

Entram no plano:

- regras de negócio (janela de 2 horas, anti-spam, confiabilidade, distância)
- status da linha e tipos de ocorrência
- classificação de relatos e previsão de risco (IA)
- interface: tempo relativo, sessão, URL do QR Code e leitura da localização
- API no ar: cadastro, login, consulta de linha, relato, confirmação, linha administrativa e classificador

Ficam de fora desta suíte automatizada: layout das telas, o banco PostgreSQL da máquina de desenvolvimento e o site publicado. A integração sobe a aplicação Spring Boot de verdade, com segurança, JPA e HTTP, em um banco em memória carregado com as mesmas linhas de demonstração.

## 3. Níveis

| Nível | O que prova | Onde está |
| --- | --- | --- |
| Unitário | Uma regra ou função, sem subir servidor nem banco | `server/src/test/java` (regras, status, IA) e `client/src/api.test.js` |
| Integração | Controller, segurança, banco e resposta HTTP juntos | `server/src/test/java/br/com/embarcai/integracao/ApiIntegracaoTest.java` |

## 4. Ambiente

- Java 21, Spring Boot 3.3.4, JUnit 5
- Banco dos testes de integração: H2 em memória (`application-test.properties`), perfil `test`
- Front: Vitest 2, Node 18+
- Comando único: `npm test`

## 5. Casos

### Unitários — regras e interface

| ID | Caso | Resultado esperado |
| --- | --- | --- |
| U01 | Relato com 90 minutos | Continua ativo |
| U02 | Relato com mais de 2 horas, ou nulo | Sai da listagem |
| U03 | Alagamento com 5 confirmações | `confirmado_comunidade` |
| U04 | Alagamento com 4, ou trânsito com 20 | `nao_confirmado` |
| U05 | Distância Barra de Jangada → Rio Doce | Entre 25 km e 40 km |
| U06 | Passageiro no destino da linha | Distância 0,0 km |
| U07 | Tipos transito, alagamento, bloqueio, acidente | Válidos; `chuva` e nulo são inválidos |
| U08 | Busca "Trânsito Intenso" | Vira `transito intenso` |
| U09 | Linha só com alerta antigo | Status `ok` |
| U10 | Linha com alerta recente | Status `atencao` |
| U11 | Frases de chuva, trânsito, batida e interdição | Classificador aponta o tipo certo |
| U12 | Texto sem palavras conhecidas | Sem sugestão |
| U13 | Horário de pico contra madrugada | Pico tem mais risco |
| U14 | Relato acabou de ser enviado | "agora"; depois "há 5 min", "há 3 h", "há 2 d" |
| U15 | Sessão salva e sessão corrompida | Lê o token; JSON inválido vira sessão vazia |
| U16 | QR da linha 020 | `/api/linhas/020/qrcode` |
| U17 | Mapa devolve a rua, ou a rede falha | "Rua da Aurora", ou "Localização capturada" |
| U18 | Aparelho sem GPS | Erro "Este aparelho não tem GPS." |

### Integração — API com banco e segurança

| ID | Caso | Resultado esperado |
| --- | --- | --- |
| I01 | `GET /api/saude` | 200 e `ok: true`, depois de consultar a tabela de relatos |
| I02 | `GET /api/linhas?lat=-8.2275&lng=-34.9378` | A 020 vem primeiro, a 0,0 km |
| I03 | `GET /api/linhas/020` | Status `atencao` e relatos da semente |
| I04 | `GET /api/linhas/9999` | 404 "Linha não encontrada." |
| I05 | `GET /api/linhas/020/qrcode` | PNG |
| I06 | Cadastro, `GET /api/me` e senha errada | 201 com token, perfil do e-mail, login inválido 401 |
| I07 | Relato sem login, depois o mesmo relato duas vezes | 401; o primeiro 201; o segundo 429 |
| I08 | Autor confirma o próprio alerta; outra pessoa confirma | 409; depois `confirmacoes: 2` |
| I09 | Criar linha com senha errada e com `admin123` | 401; depois 201 com código `777` |
| I10 | `POST /api/ia/classificar` com texto de alagamento | `tipo: alagamento` e `confiavel: true` |

## 6. Como executar

```bash
npm test
```

`npm run test:web` roda só o front. `npm run test:api` roda só o Java. Os relatórios do Maven ficam em `server/target/surefire-reports/`. Uma cópia da última execução bem-sucedida está em `docs/evidencias/`.

## 7. Evidência — 8 de novembro de 2026, 22:20 (−03:00)

Comando: `npm test` (Vitest e `mvn -f server/pom.xml test`).
Java: OpenJDK 21.0.12.1. Perfil ativo na integração: `test`. Banco: `jdbc:h2:mem:embarcai`.

| Suíte | Testes | Falhas |
| --- | --- | --- |
| RegrasNegocioTest | 6 | 0 |
| StatusLinhaTest | 4 | 0 |
| ClassificadorRelatosTest | 11 | 0 |
| IntencoesTest | 11 | 0 |
| PrevisaoRiscoTest | 8 | 0 |
| RecomendadorLinhasTest | 6 | 0 |
| ApiIntegracaoTest | 10 | 0 |
| api.test.js (Vitest) | 9 | 0 |
| **Total** | **65** | **0** |

Trecho do Vitest:

```
✓ client/src/api.test.js (9 tests) 4ms
Test Files  1 passed (1)
     Tests  9 passed (9)
```

Trecho do Maven para a integração:

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- in br.com.embarcai.integracao.ApiIntegracaoTest
```

A primeira execução do `StatusLinhaTest` encontrou um defeito: tipo de ocorrência nulo estourava `NullPointerException` e a API responderia 500 em vez de recusar o relato. `StatusLinha.tipoValido` passou a tratar nulo como tipo inválido. A suíte foi executada de novo e fechou com 65 testes e 0 falhas.

