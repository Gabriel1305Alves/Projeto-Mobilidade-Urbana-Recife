import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api, TIPOS } from "../api.js";
import { useAuth } from "../AuthContext.jsx";
import { nomeDoLocal, obterPosicao } from "../geo.js";
import Logo from "../components/Logo.jsx";
import TipoIcon from "../components/TipoIcon.jsx";
import TopBar from "../components/TopBar.jsx";

export default function Relatar() {
  const { codigo } = useParams();
  const navigate = useNavigate();
  const { usuario } = useAuth();
  const [linha, setLinha] = useState(null);
  const [tipo, setTipo] = useState("");
  const [mensagem, setMensagem] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState("");
  const [gps, setGps] = useState(null);

  useEffect(() => {
    if (!usuario) {
      navigate(`/login?next=${encodeURIComponent(`/linha/${codigo}/relatar`)}`, { replace: true });
      return;
    }
    api(`/api/linhas/${encodeURIComponent(codigo)}`)
      .then(setLinha)
      .catch((err) => setErro(err.message));
    obterPosicao()
      .then(async (pos) => {
        const local = await nomeDoLocal(pos.lat, pos.lng);
        setGps({ ...pos, local });
      })
      .catch(() => setGps({ erro: "Não foi possível pegar o GPS. O relato ainda pode ser enviado." }));
  }, [codigo, usuario, navigate]);

  async function enviar(event) {
    event.preventDefault();
    if (!tipo) return;
    setEnviando(true);
    setErro("");
    try {
      await api(`/api/linhas/${encodeURIComponent(codigo)}/relatos`, {
        method: "POST",
        body: JSON.stringify({
          tipo,
          mensagem,
          latitude: gps?.lat,
          longitude: gps?.lng,
          local: gps?.local,
        }),
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

        <p className={`geo-status ${gps?.local ? "ok" : ""}`}>
          {gps?.local
            ? `📍 ${gps.local}`
            : gps?.erro
              ? gps.erro
              : "Obtendo sua localização…"}
        </p>

        {erro && <p className="erro">{erro}</p>}
        <button className="btn" disabled={!tipo || enviando} type="submit">
          Enviar
        </button>
      </form>
      <Logo size="sm" />
    </section>
  );
}
