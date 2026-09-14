import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api, TIPOS } from "../api.js";
import Logo from "../components/Logo.jsx";
import TipoIcon from "../components/TipoIcon.jsx";
import TopBar from "../components/TopBar.jsx";

export default function Relatar() {
  const { codigo } = useParams();
  const navigate = useNavigate();
  const [linha, setLinha] = useState(null);
  const [tipo, setTipo] = useState("");
  const [mensagem, setMensagem] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState("");

  useEffect(() => {
    api(`/api/linhas/${encodeURIComponent(codigo)}`)
      .then(setLinha)
      .catch((err) => setErro(err.message));
  }, [codigo]);

  async function enviar(event) {
    event.preventDefault();
    if (!tipo) return;
    setEnviando(true);
    setErro("");
    try {
      await api(`/api/linhas/${encodeURIComponent(codigo)}/relatos`, {
        method: "POST",
        body: JSON.stringify({ tipo, mensagem }),
      });
      navigate(`/linha/${codigo}/sucesso`);
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section className="screen">
      <TopBar to={`/linha/${codigo}`} />
      <h1>Informar ocorrência</h1>
      <p className="muted">
        Linha {codigo}
        {linha ? ` — ${linha.nome}` : ""}
      </p>

      <form onSubmit={enviar}>
        <p className="label">O que aconteceu?</p>
        <div className="tipo-grid">
          {TIPOS.map((item) => (
            <button
              key={item.id}
              type="button"
              className={tipo === item.id ? "tipo-card on" : "tipo-card"}
              onClick={() => setTipo(item.id)}
            >
              <TipoIcon tipo={item.id} />
              {item.rotulo}
            </button>
          ))}
        </div>

        <label className="field">
          <span>Descrição (opcional)</span>
          <textarea
            rows="3"
            maxLength="280"
            placeholder="Descreva o que aconteceu..."
            value={mensagem}
            onChange={(e) => setMensagem(e.target.value)}
          />
        </label>

        {erro && <p className="erro">{erro}</p>}
        <button className="btn" disabled={!tipo || enviando} type="submit">
          Enviar
        </button>
      </form>
      <Logo size="sm" />
    </section>
  );
}
