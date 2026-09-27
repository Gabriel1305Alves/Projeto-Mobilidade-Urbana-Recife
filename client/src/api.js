export const API_URL = "";

export const TIPOS = [
  { id: "transito", rotulo: "Trânsito" },
  { id: "alagamento", rotulo: "Alagamento" },
  { id: "bloqueio", rotulo: "Bloqueio" },
  { id: "acidente", rotulo: "Acidente" },
];

function tokenAtual() {
  try {
    return JSON.parse(localStorage.getItem("embarcai_sessao"))?.token || "";
  } catch {
    return "";
  }
}

export async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  const token = tokenAtual();
  if (token) headers.Authorization = `Bearer ${token}`;

  let resposta;
  try {
    resposta = await fetch(`${API_URL}${path}`, {
      ...options,
      headers,
    });
  } catch {
    throw new Error("A API Java não está no ar. Rode npm run dev e tente de novo.");
  }
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
