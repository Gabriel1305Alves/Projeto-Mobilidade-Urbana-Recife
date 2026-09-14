require("dotenv").config();
const { migrate } = require("./db");
const { criarApp } = require("./app");

const PORT = Number(process.env.PORT) || 3000;

async function start() {
  try {
    await migrate();
    criarApp().listen(PORT, "0.0.0.0", () => {
      console.log(`Embarcaí API em http://localhost:${PORT}`);
    });
  } catch (err) {
    console.error("Não consegui conectar no PostgreSQL.");
    console.error(err.message);
    process.exit(1);
  }
}

start();
