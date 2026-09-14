# Projeto Mobilidade Urbana Recife

A mobilidade na Região Metropolitana do Recife sofre com o excesso de veículos e a malha defasada, gerando severos engarrafamentos diários. O problema piora sob fortes temporais, quando alagamentos frequentes paralisam o trânsito em toda a metrópole.

O **Embarcaí** é a solução da Equipe 2: alertas colaborativos no ponto, por QR Code, sem baixar app.

## Arquitetura

**MVC (Model–View–Controller)** em um modelo **client-server** com API REST.

- **Model** (`server/.../model` e `repository`) — entidades Java e PostgreSQL
- **View** (`client/src/pages`) — telas React
- **Controller** (`server/.../controller`) — API REST em Spring Boot

O React chama `/api`. O controller Java usa o model. O model fala com o banco.

## Stack

- Front: React
- Back: Java 21 + Spring Boot
- Banco: PostgreSQL

## Como rodar

```bash
brew services start postgresql@16
npm install
npm run dev
```

Abra [http://localhost:5173](http://localhost:5173) e consulte a linha **020**.

A API Java sobe em [http://localhost:8080](http://localhost:8080).
