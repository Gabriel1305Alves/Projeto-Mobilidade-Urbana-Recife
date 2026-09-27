import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api } from "../api.js";
import { useAuth } from "../AuthContext.jsx";
import Logo from "../components/Logo.jsx";
import TopBar from "../components/TopBar.jsx";

export default function Cadastro() {
  const navigate = useNavigate();
  const location = useLocation();
  const { entrar } = useAuth();
  const destino = new URLSearchParams(location.search).get("next") || "/";
  const [form, setForm] = useState({ nome: "", email: "", senha: "" });
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
      const sessao = await api("/api/cadastrar", {
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
      <TopBar to={`/login${location.search}`} />
      <Logo size="md" />
      <h1>Criar conta</h1>
      <p className="muted">Passageiro cadastrado consegue informar o que está acontecendo na linha.</p>
      <form onSubmit={enviar}>
        <label className="field">
          <span>Nome</span>
          <input
            type="text"
            required
            autoComplete="name"
            value={form.nome}
            onChange={(e) => set("nome", e.target.value)}
            placeholder="Seu nome"
          />
        </label>
        <label className="field">
          <span>E-mail</span>
          <input
            type="email"
            required
            autoComplete="email"
            value={form.email}
            onChange={(e) => set("email", e.target.value)}
            placeholder="voce@email.com"
          />
        </label>
        <label className="field">
          <span>Senha</span>
          <input
            type="password"
            required
            minLength={6}
            autoComplete="new-password"
            value={form.senha}
            onChange={(e) => set("senha", e.target.value)}
            placeholder="Mínimo 6 caracteres"
          />
        </label>
        {erro && <p className="erro">{erro}</p>}
        <button className="btn" type="submit" disabled={enviando}>
          {enviando ? "Criando…" : "Cadastrar"}
        </button>
      </form>
      <Link className="text-link" to={`/login${location.search}`}>
        Já tenho conta
      </Link>
    </section>
  );
}
