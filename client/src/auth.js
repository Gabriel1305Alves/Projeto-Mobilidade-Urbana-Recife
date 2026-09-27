const KEY = "embarcai_sessao";

export function lerSessao() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) || null;
  } catch {
    return null;
  }
}

export function tokenAtual() {
  return lerSessao()?.token || "";
}

export function salvarSessao(dados) {
  localStorage.setItem(KEY, JSON.stringify(dados));
}

export function limparSessao() {
  localStorage.removeItem(KEY);
}
