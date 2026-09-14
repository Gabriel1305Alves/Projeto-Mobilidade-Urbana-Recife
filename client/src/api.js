export const API_URL = import.meta.env.DEV ? "http://localhost:3002" : "";

export const TIPOS = [
  { id: "transito", rotulo: "Trânsito" },
  { id: "alagamento", rotulo: "Alagamento" },
  { id: "bloqueio", rotulo: "Bloqueio" },
  { id: "acidente", rotulo: "Acidente" },
];

export async function api(path, options = {}) {
  const resposta = await fetch(`${API_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });
  const dados = await resposta.json().catch(() => ({}));
  if (!resposta.ok) {
    throw new Error(dados.erro || "Não foi possível concluir o pedido.");
  }
  return dados;
}

export function qrUrl(codigo) {
  return `${API_URL}/api/linhas/${encodeURIComponent(codigo)}/qrcode`;
}

export function tempoAtras(data) {
  if (!data) return "agora";
  const segundos = Math.max(0, Math.floor((Date.now() - new Date(data).getTime()) / 1000));
  if (segundos < 45) return "agora";
  if (segundos < 3600) return `há ${Math.floor(segundos / 60)} min`;
  if (segundos < 86400) return `há ${Math.floor(segundos / 3600)} h`;
  return `há ${Math.floor(segundos / 86400)} d`;
}
