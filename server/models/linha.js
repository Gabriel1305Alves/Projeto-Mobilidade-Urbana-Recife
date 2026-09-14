const { pool } = require("../db");

function termo(q) {
  return `%${String(q).normalize("NFD").replace(/\p{M}/gu, "")}%`;
}

async function listar(q) {
  const params = [];
  let sql = "SELECT id, codigo, nome, origem, destino FROM linhas";
  if (q) {
    params.push(termo(q));
    sql += ` WHERE sem_acento(codigo) LIKE sem_acento($1)
              OR sem_acento(nome) LIKE sem_acento($1)
              OR sem_acento(origem) LIKE sem_acento($1)
              OR sem_acento(destino) LIKE sem_acento($1)`;
  }
  sql += " ORDER BY codigo";
  const { rows } = await pool.query(sql, params);
  return rows;
}

async function buscarPorCodigo(codigo) {
  const { rows } = await pool.query(
    "SELECT id, codigo, nome, origem, destino FROM linhas WHERE codigo = $1",
    [codigo]
  );
  return rows[0] || null;
}

async function criar({ codigo, nome, origem, destino }) {
  const { rows } = await pool.query(
    `INSERT INTO linhas (codigo, nome, origem, destino)
     VALUES ($1, $2, $3, $4)
     RETURNING id, codigo, nome, origem, destino`,
    [codigo.trim().toUpperCase(), nome.trim(), origem.trim(), destino.trim()]
  );
  return rows[0];
}

module.exports = { listar, buscarPorCodigo, criar };
