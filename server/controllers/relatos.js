const Linha = require("../models/linha");
const Relato = require("../models/relato");
const { pool } = require("../db");

const jaConfirmou = new Map();

async function saude(_req, res) {
  try {
    await pool.query("SELECT 1");
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ ok: false, erro: err.message });
  }
}

async function stats(_req, res) {
  try {
    res.json(await Relato.stats());
  } catch (err) {
    console.error(err);
    res.status(500).json({ erro: "Não foi possível carregar as estatísticas." });
  }
}

async function criar(req, res) {
  try {
    const codigo = String(req.params.codigo || "").trim();
    const tipo = String(req.body?.tipo || "").trim();
    const mensagem = String(req.body?.mensagem || "").trim().slice(0, 280);
    const autor = String(req.body?.autor || "").trim().slice(0, 40);

    if (!Relato.TIPOS.includes(tipo)) {
      return res.status(400).json({ erro: "Escolhe o que aconteceu na linha." });
    }

    const linha = await Linha.buscarPorCodigo(codigo);
    if (!linha) return res.status(404).json({ erro: "Linha não encontrada." });

    const relato = await Relato.criar({
      linhaId: linha.id,
      tipo,
      mensagem,
      autor,
    });
    res.status(201).json(relato);
  } catch (err) {
    console.error(err);
    res.status(500).json({ erro: "Não foi possível salvar a ocorrência." });
  }
}

async function confirmar(req, res) {
  try {
    const id = Number(req.params.id);
    const chave = `${req.ip || "local"}:${id}`;
    if (jaConfirmou.has(chave)) {
      return res.status(409).json({ erro: "Você já confirmou esta ocorrência." });
    }
    const atualizado = await Relato.confirmar(id);
    if (!atualizado) return res.status(404).json({ erro: "Ocorrência não encontrada." });
    jaConfirmou.set(chave, Date.now());
    res.json(atualizado);
  } catch (err) {
    console.error(err);
    res.status(500).json({ erro: "Não foi possível confirmar." });
  }
}

module.exports = { saude, stats, criar, confirmar };
