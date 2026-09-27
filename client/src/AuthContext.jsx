import { createContext, useContext, useMemo, useState } from "react";
import { limparSessao, lerSessao, salvarSessao } from "./auth.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [sessao, setSessao] = useState(() => lerSessao());

  const value = useMemo(
    () => ({
      usuario: sessao?.usuario || null,
      token: sessao?.token || "",
      entrar(dados) {
        salvarSessao(dados);
        setSessao(dados);
      },
      sair() {
        limparSessao();
        setSessao(null);
      },
    }),
    [sessao]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth precisa do AuthProvider");
  return ctx;
}
