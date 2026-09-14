# Projeto Mobilidade Urbana Recife

A mobilidade na Região Metropolitana do Recife sofre com o excesso de veículos e a malha defasada, gerando severos engarrafamentos diários. O problema piora sob fortes temporais, quando alagamentos frequentes paralisam o trânsito em toda a metrópole.

O **Embarcaí** é a solução da Equipe 2: alertas colaborativos no ponto, por QR Code, sem baixar app.

## Arquitetura

**MVC (Model–View–Controller)** em um modelo **client-server** com API REST.

- **Model** (`server/models`) — acesso ao PostgreSQL
- **View** (`client/src/pages` e `components`) — telas React
- **Controller** (`server/controllers`) — recebe a requisição HTTP e responde JSON

O React chama `/api`. O controller usa o model. O model fala com o banco.

## Como rodar

```bash
npm install
npm run dev
```

Abra [http://localhost:5173](http://localhost:5173) e consulte a linha **020**.
