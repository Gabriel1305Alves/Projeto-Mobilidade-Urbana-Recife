require("dotenv").config();
const { Pool } = require("pg");

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

async function migrate() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS linhas (
      id SERIAL PRIMARY KEY,
      codigo VARCHAR(20) UNIQUE NOT NULL,
      nome VARCHAR(160) NOT NULL,
      origem VARCHAR(120) NOT NULL,
      destino VARCHAR(120) NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS relatos (
      id SERIAL PRIMARY KEY,
      linha_id INTEGER NOT NULL REFERENCES linhas(id) ON DELETE CASCADE,
      tipo VARCHAR(30) NOT NULL,
      mensagem TEXT,
      autor VARCHAR(40),
      confirmacoes INTEGER NOT NULL DEFAULT 1,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE INDEX IF NOT EXISTS idx_relatos_linha_created
      ON relatos (linha_id, created_at DESC);

    CREATE OR REPLACE FUNCTION sem_acento(t text)
    RETURNS text AS $$
      SELECT translate(
        lower(coalesce(t, '')),
        'áàâãäéèêëíìîïóòôõöúùûüçñ',
        'aaaaaeeeeiiiiooooouuuucn'
      );
    $$ LANGUAGE sql IMMUTABLE;
  `);

  await pool.query(`
    ALTER TABLE relatos
    ADD COLUMN IF NOT EXISTS confirmacoes INTEGER NOT NULL DEFAULT 1
  `);

  const { rows } = await pool.query("SELECT COUNT(*)::int AS total FROM linhas");
  if (rows[0].total === 0) await seed();
  await garantirLinha020();
}

async function seed() {
  const linhas = [
    ["020", "Barra de Jangada / Rio Doce", "Barra de Jangada", "Rio Doce"],
    ["101", "Recife / Boa Viagem", "Recife", "Boa Viagem"],
    ["040", "Caxangá / Joana Bezerra", "TI Caxangá", "TI Joana Bezerra"],
    ["052", "Dois Unidos / Derby", "Dois Unidos", "Derby"],
    ["163", "Ibura / Boa Vista", "TI Ibura", "Boa Vista"],
    ["190", "Cajueiro Seco / Conde da Boa Vista", "TI Cajueiro Seco", "Conde da Boa Vista"],
    ["201", "Macaxeira / Caxangá", "TI Macaxeira", "Caxangá"],
    ["321", "PE-15 / Cais de Santa Rita", "TI PE-15", "Cais de Santa Rita"],
    ["411", "CDU / Boa Vista", "TI CDU", "Boa Vista"],
    ["513", "PRG / Rio Doce", "TI PRG", "Rio Doce"],
    ["1900", "Circular Centro", "Rua do Sol", "Avenida Guararapes"],
  ];

  for (const linha of linhas) {
    await pool.query(
      "INSERT INTO linhas (codigo, nome, origem, destino) VALUES ($1, $2, $3, $4)",
      linha
    );
  }
}

async function garantirLinha020() {
  await pool.query(
    `INSERT INTO linhas (codigo, nome, origem, destino)
     VALUES ('020', 'Barra de Jangada / Rio Doce', 'Barra de Jangada', 'Rio Doce')
     ON CONFLICT (codigo) DO UPDATE
       SET nome = EXCLUDED.nome, origem = EXCLUDED.origem, destino = EXCLUDED.destino`
  );

  const { rows } = await pool.query("SELECT id FROM linhas WHERE codigo = '020'");
  const { rows: demo } = await pool.query(
    `SELECT 1 FROM relatos WHERE linha_id = $1 AND mensagem ILIKE '%Agamenon%' LIMIT 1`,
    [rows[0].id]
  );
  if (demo.length) return;

  await pool.query(
    `INSERT INTO relatos (linha_id, tipo, mensagem, autor, confirmacoes, created_at)
     VALUES
       ($1, 'transito', 'Av. Agamenon Magalhães, sentido centro', 'Comunidade', 8, NOW() - INTERVAL '14 minutes'),
       ($1, 'alagamento', 'Rua da Aurora, próximo ao Terminal', 'Comunidade', 5, NOW() - INTERVAL '32 minutes')`,
    [rows[0].id]
  );
}

module.exports = { pool, migrate };
