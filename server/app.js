const fs = require("fs");
const path = require("path");
const express = require("express");
const { router } = require("./routes");

function criarApp() {
  const app = express();
  app.set("trust proxy", 1);
  app.use((req, res, next) => {
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Headers", "Content-Type, x-admin-senha");
    res.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    if (req.method === "OPTIONS") return res.sendStatus(204);
    next();
  });
  app.use(express.json({ limit: "16kb" }));
  app.use("/api", router);

  const dist = path.join(__dirname, "../client/dist");
  if (fs.existsSync(path.join(dist, "index.html"))) {
    app.use(express.static(dist));
    app.get(/^(?!\/api).*/, (_req, res) => {
      res.sendFile(path.join(dist, "index.html"));
    });
  }

  return app;
}

module.exports = { criarApp };
