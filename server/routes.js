const { Router } = require("express");
const linhas = require("./controllers/linhas");
const relatos = require("./controllers/relatos");

const router = Router();
const envios = new Map();

function rateLimit(req, res, next) {
  const ip = req.ip || "local";
  const agora = Date.now();
  const lista = (envios.get(ip) || []).filter((t) => agora - t < 10 * 60 * 1000);
  if (lista.length >= 8) {
    return res.status(429).json({
      erro: "Muitos relatos seguidos deste aparelho. Espera alguns minutos e tenta de novo.",
    });
  }
  lista.push(agora);
  envios.set(ip, lista);
  next();
}

function admin(req, res, next) {
  const senha = req.headers["x-admin-senha"] || req.body?.senha;
  if (senha !== process.env.ADMIN_SENHA) {
    return res.status(401).json({ erro: "Senha de administração inválida." });
  }
  next();
}

router.get("/saude", relatos.saude);
router.get("/stats", relatos.stats);
router.get("/linhas", linhas.listar);
router.get("/linhas/:codigo", linhas.obter);
router.get("/linhas/:codigo/qrcode", linhas.qrcode);
router.post("/linhas/:codigo/relatos", rateLimit, relatos.criar);
router.post("/relatos/:id/confirmar", relatos.confirmar);
router.post("/linhas", admin, linhas.criar);
router.use((_req, res) => res.status(404).json({ erro: "Rota não encontrada." }));

module.exports = { router };
