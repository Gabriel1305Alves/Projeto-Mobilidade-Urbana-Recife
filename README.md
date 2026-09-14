# Embarcaí

Plataforma colaborativa para quem pega ônibus no Grande Recife.

## Arquitetura

**MVC (Model–View–Controller)** em cima de um modelo **client-server** com API REST.

- **Model** (`server/models`) — acesso ao PostgreSQL
- **View** (`client/src/pages` e `components`) — telas React
- **Controller** (`server/controllers`) — recebe a requisição HTTP e responde JSON

O React chama `/api`. O controller usa o model. O model fala com o banco.

## Como rodar

```bash
npm run dev
```

Abra [http://localhost:5173](http://localhost:5173) e consulte a linha **020**.
