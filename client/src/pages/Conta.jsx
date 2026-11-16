import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api.js";
import { useAuth } from "../AuthContext.jsx";
import TopBar from "../components/TopBar.jsx";

export default function Conta() {
  const navigate = useNavigate();
  const { usuario, sair } = useAuth();
  const [dados, setDados] = useState(usuario);
  const [erro, setErro] = useState("");
  const [apagando, setApagando] = useState(false);

  useEffect(() => {
    if (!usuario) return;
    api("/api/me")
      .then(setDados)
      .catch((err) => setErro(err.message));
  }, [usuario]);

  async function apagar() {
    if (!window.confirm("Apagar sua conta? O nome sai dos alertas e o e-mail é excluído.")) return;
    setApagando(true);
    setErro("");
    try {
      await api("/api/me", { method: "DELETE" });
      sair();
      navigate("/", { replace: true });
    } catch (err) {
      setErro(err.message);
      setApagando(false);
    }
  }

  if (!usuario) {
    return (
      <section className="screen">
        <TopBar to="/" />
        <h1>Meus dados</h1>
        <p className="muted">Entre para ver e apagar o que a sua conta guarda.</p>
        <Link className="btn" to="/login?next=/conta">Entrar</Link>
      </section>
    );
  }

  return (
    <section className="screen sobre">
      <TopBar to="/" />
      <h1>Meus dados</h1>
      <p className="muted">Isto é o que o Embarcaí guarda da sua conta. A senha não aparece.</p>
      <ul className="sobre-list">
        <li>
          <strong>Nome</strong>
          <span>{dados?.nome || usuario.nome}</span>
        </li>
        <li>
          <strong>E-mail</strong>
          <span>{dados?.email || usuario.email}</span>
        </li>
        <li>
          <strong>Papel</strong>
          <span>{dados?.role || usuario.role}</span>
        </li>
      </ul>
      {erro && <p className="erro">{erro}</p>}
      <button className="btn danger" type="button" onClick={apagar} disabled={apagando}>
        {apagando ? "Apagando…" : "Apagar minha conta"}
      </button>
      <Link className="text-link" to="/privacidade">Como tratamos esses dados</Link>
    </section>
  );
}
