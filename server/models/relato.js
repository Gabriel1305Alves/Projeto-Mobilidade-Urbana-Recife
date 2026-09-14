const { pool } = require("../db");

const TIPOS = ["transito", "alagamento", "bloqueio", "acidente"];
const ROTULOS = {
  transito: "Trânsito intenso",
  alagamento: "Alagamento",
  bloqueio: "Bloqueio",
  acidente: "Acidente",
  atencao: "Atenção no trajeto",
  ok: "Trajeto tranquilo",
};

function rotulo(tipo) {
  return ROTULOS[tipo] || tipo;
}

function status(relatos) {
  const limite = Date.now() - 2 * 60 * 60 * 1000;
  const recentes = relatos.filter((r) => new Date(r.created_at).getTime() >= limite);
  if (!recentes.length) {
    return { tipo: "ok", rotulo: rotulo("ok"), atualizado_em: null, total_recentes: 0 };
  }
  return {
    tipo: "atencao",
    rotulo: rotulo("atencao"),
    atualizado_em: recentes[0].created_at,
    total_recentes: recentes.length,
  };
}

async function listarRecentes() {
  const { rows } = await pool.query(
    `SELECT linha_id, tipo, created_at FROM relatos
     WHERE created_at >= NOW() - INTERVAL '2 hours'
     ORDER BY created_at DESC`
  );
  return rows;
}

async function daLinha(linhaId) {
  const { rows } = await pool.query(
    `SELECT id, tipo, mensagem, autor, confirmacoes, created_at
     FROM relatos
     WHERE linha_id = $1 AND created_at >= NOW() - INTERVAL '2 hours'
     ORDER BY created_at DESC
     LIMIT 40`,
    [linhaId]
  );
  return rows.map((r) => ({ ...r, rotulo: rotulo(r.tipo) }));
}

async function criar({ linhaId, tipo, mensagem, autor }) {
  const { rows } = await pool.query(
    `INSERT INTO relatos (linha_id, tipo, mensagem, autor)
     VALUES ($1, $2, $3, $4)
     RETURNING id, tipo, mensagem, autor, confirmacoes, created_at`,
    [linhaId, tipo, mensagem || null, autor || null]
  );
  return { ...rows[0], rotulo: rotulo(rows[0].tipo) };
}

async function confirmar(id) {
  const { rows } = await pool.query(
    `UPDATE relatos SET confirmacoes = confirmacoes + 1
      WHERE id = $1 RETURNING id, confirmacoes`,
    [id]
  );
  return rows[0] || null;
}

async function stats() {
  const [hoje, autores, confirmados, total] = await Promise.all([
    pool.query("SELECT COUNT(*)::int AS n FROM relatos WHERE created_at >= CURRENT_DATE"),
    pool.query("SELECT COUNT(DISTINCT COALESCE(autor, 'anon'))::int AS n FROM relatos WHERE created_at >= NOW() - INTERVAL '7 days'"),
    pool.query("SELECT COUNT(*)::int AS n FROM relatos WHERE confirmacoes >= 2"),
    pool.query("SELECT COUNT(*)::int AS n FROM relatos"),
  ]);
  const n = total.rows[0].n;
  return {
    relatos_hoje: hoje.rows[0].n,
    usuarios_ativos: autores.rows[0].n,
    precisao: n ? Math.round((confirmados.rows[0].n / n) * 100) : 0,
  };
}

module.exports = { TIPOS, rotulo, status, listarRecentes, daLinha, criar, confirmar, stats };
