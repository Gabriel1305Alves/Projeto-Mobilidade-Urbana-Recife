const QRCode = require("qrcode");
const Linha = require("../models/linha");
const Relato = require("../models/relato");

function baseUrl(req) {
  if (process.env.BASE_URL) return process.env.BASE_URL.replace(/\/$/, "");
  return `${req.protocol}://${req.get("host")}`;
}

async function listar(req, res) {
  try {
    const q = String(req.query.q || "").trim();
    const linhas = await Linha.listar(q);
    const relatos = await Relato.listarRecentes();
    const porLinha = new Map();
    for (const r of relatos) {
      if (!porLinha.has(r.linha_id)) porLinha.set(r.linha_id, []);
      porLinha.get(r.linha_id).push(r);
    }
    res.json(
      linhas.map((linha) => ({
        ...linha,
        status: Relato.status(porLinha.get(linha.id) || []),
      }))
    );
  } catch (err) {
    console.error(err);
    res.status(500).json({ erro: "Erro ao listar as linhas." });
  }
}

async function obter(req, res) {
  try {
    const codigo = String(req.params.codigo || "").trim();
    const linha = await Linha.buscarPorCodigo(codigo);
    if (!linha) return res.status(404).json({ erro: "Linha não encontrada." });
    const relatos = await Relato.daLinha(linha.id);
    res.json({ ...linha, status: Relato.status(relatos), relatos });
  } catch (err) {
    console.error(err);
    res.status(500).json({ erro: "Erro ao carregar a linha." });
  }
}

async function criar(req, res) {
  try {
    const { codigo, nome, origem, destino } = req.body || {};
    if (!codigo || !nome || !origem || !destino) {
      return res.status(400).json({ erro: "Preenche código, nome, origem e destino." });
    }
    const linha = await Linha.criar({ codigo, nome, origem, destino });
    res.status(201).json(linha);
  } catch (err) {
    if (err.code === "23505") {
      return res.status(409).json({ erro: "Já existe uma linha com esse código." });
    }
    console.error(err);
    res.status(500).json({ erro: "Não foi possível cadastrar a linha." });
  }
}

async function qrcode(req, res) {
  try {
    const codigo = String(req.params.codigo || "").trim();
    const linha = await Linha.buscarPorCodigo(codigo);
    if (!linha) return res.status(404).json({ erro: "Linha não encontrada." });
    const png = await QRCode.toBuffer(`${baseUrl(req)}/linha/${encodeURIComponent(codigo)}`, {
      type: "png",
      width: 640,
      margin: 1,
      errorCorrectionLevel: "H",
      color: { dark: "#1b2430", light: "#ffffff" },
    });
    res.setHeader("Content-Type", "image/png");
    res.send(png);
  } catch (err) {
    console.error(err);
    res.status(500).json({ erro: "Não foi possível gerar o QR Code." });
  }
}

module.exports = { listar, obter, criar, qrcode };
