import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api } from "../api.js";
import { useAuth } from "../AuthContext.jsx";
import Logo from "../components/Logo.jsx";
import TopBar from "../components/TopBar.jsx";

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { entrar } = useAuth();
  const destino = new URLSearchParams(location.search).get("next") || "/";
  const [form, setForm] = useState({ email: "", senha: "" });
  const [erro, setErro] = useState("");
  const [enviando, setEnviando] = useState(false);

  function set(campo, valor) {
    setForm((atual) => ({ ...atual, [campo]: valor }));
  }

  async function enviar(event) {
    event.preventDefault();
    setErro("");
    setEnviando(true);
    try {
      const sessao = await api("/api/login", {
        method: "POST",
        body: JSON.stringify(form),
      });
      entrar(sessao);
      navigate(destino, { replace: true });
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section className="screen">
      <TopBar to="/" />
      <Logo size="md" />
      <h1>Entrar</h1>
      <p className="muted">Use seu e-mail para reportar ocorrências na linha.</p>
      <form onSubmit={enviar}>
        <label className="field">
          <span>E-mail</span>
          <input
            type="email"
            required
            autoComplete="email"
            value={form.email}
            onChange={(e) => set("email", e.target.value)}
            placeholder="maria@embarcai.com"
          />
        </label>
        <label className="field">
          <span>Senha</span>
          <input
            type="password"
            required
            autoComplete="current-password"
            value={form.senha}
            onChange={(e) => set("senha", e.target.value)}
            placeholder="••••••"
          />
        </label>
        {erro && <p className="erro">{erro}</p>}
        <button className="btn" type="submit" disabled={enviando}>
          {enviando ? "Entrando…" : "Entrar"}
        </button>
      </form>
      <Link className="text-link" to={`/cadastro${location.search}`}>
        Criar conta
      </Link>
    </section>
  );
}
