import { beforeEach, describe, expect, it, vi } from "vitest";
import { TIPOS, qrUrl, tempoAtras } from "./api.js";
import { limparSessao, lerSessao, salvarSessao, tokenAtual } from "./auth.js";
import { nomeDoLocal, obterPosicao } from "./geo.js";

const memoria = new Map();

beforeEach(() => {
  memoria.clear();
  globalThis.localStorage = {
    getItem: (chave) => (memoria.has(chave) ? memoria.get(chave) : null),
    setItem: (chave, valor) => memoria.set(chave, String(valor)),
    removeItem: (chave) => memoria.delete(chave),
  };
});

describe("tempoAtras", () => {
  it("mostra agora para um relato recém-enviado", () => {
    expect(tempoAtras(null)).toBe("agora");
    expect(tempoAtras(new Date().toISOString())).toBe("agora");
  });

  it("mostra minutos, horas e dias", () => {
    const ha = (ms) => new Date(Date.now() - ms).toISOString();
    expect(tempoAtras(ha(5 * 60 * 1000))).toBe("há 5 min");
    expect(tempoAtras(ha(3 * 60 * 60 * 1000))).toBe("há 3 h");
    expect(tempoAtras(ha(2 * 24 * 60 * 60 * 1000))).toBe("há 2 d");
  });
});

describe("sessão", () => {
  it("guarda e lê o token do passageiro", () => {
    expect(lerSessao()).toBeNull();
    salvarSessao({ token: "abc", usuario: { nome: "Maria" } });
    expect(tokenAtual()).toBe("abc");
    expect(lerSessao().usuario.nome).toBe("Maria");
    limparSessao();
    expect(tokenAtual()).toBe("");
  });

  it("ignora sessão corrompida", () => {
    localStorage.setItem("embarcai_sessao", "{nao-e-json");
    expect(lerSessao()).toBeNull();
  });
});

describe("consulta de linha", () => {
  it("monta a url do qr code", () => {
    expect(qrUrl("020")).toBe("/api/linhas/020/qrcode");
    expect(qrUrl("a b")).toBe("/api/linhas/a%20b/qrcode");
  });

  it("oferece os quatro tipos de ocorrência", () => {
    expect(TIPOS.map((tipo) => tipo.id)).toEqual(["transito", "alagamento", "bloqueio", "acidente"]);
  });
});

describe("localização", () => {
  it("prefere o nome da rua devolvido pelo mapa", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => ({
      json: async () => ({ address: { road: "Rua da Aurora" } }),
    })));
    await expect(nomeDoLocal(-8.05, -34.88)).resolves.toBe("Rua da Aurora");
    vi.unstubAllGlobals();
  });

  it("cai no texto padrão quando o mapa falha", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => { throw new Error("rede"); }));
    await expect(nomeDoLocal(0, 0)).resolves.toBe("Localização capturada");
    vi.unstubAllGlobals();
  });

  it("recusa quando o aparelho não tem gps", async () => {
    vi.stubGlobal("navigator", {});
    await expect(obterPosicao()).rejects.toThrow("Este aparelho não tem GPS.");
    vi.unstubAllGlobals();
  });
});
