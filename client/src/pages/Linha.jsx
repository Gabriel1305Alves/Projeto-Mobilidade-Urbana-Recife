import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, tempoAtras } from "../api.js";
import { useAuth } from "../AuthContext.jsx";
import Logo from "../components/Logo.jsx";
import MapaRota from "../components/MapaRota.jsx";
import TipoIcon from "../components/TipoIcon.jsx";
import TopBar from "../components/TopBar.jsx";

export default function Linha() {
  const { codigo } = useParams();
  const navigate = useNavigate();
  const { usuario } = useAuth();
  const [linha, setLinha] = useState(null);
  const [erro, setErro] = useState("");

  const carregar = useCallback(async () => {
    try {
      setLinha(await api(`/api/linhas/${encodeURIComponent(codigo)}`));
      setErro("");
    } catch (err) {
      setErro(err.message);
    }
  }, [codigo]);

  useEffect(() => {
    carregar();
    const timer = setInterval(carregar, 30000);
    return () => clearInterval(timer);
  }, [carregar]);

  async function confirmar(id) {
    if (!usuario) {
      navigate(`/login?next=/linha/${codigo}`);
      return;
    }
    try {
      await api(`/api/relatos/${id}/confirmar`, { method: "POST" });
      await carregar();
    } catch (err) {
      setErro(err.message);
    }
  }

  function informar() {
    const destino = `/linha/${codigo}/relatar`;
    if (!usuario) navigate(`/login?next=${encodeURIComponent(destino)}`);
    else navigate(destino);
  }

  if (erro && !linha) {
    return (
      <section className="screen">
        <TopBar to="/" />
        <p className="erro">{erro}</p>
      </section>
    );
  }

  if (!linha) {
    return (
      <section className="screen">
        <TopBar to="/" />
        <p className="muted">Carregando linha…</p>
      </section>
    );
  }

  return (
    <section className="screen linha-screen">
      <TopBar to="/" />
      {erro && <p className="erro">{erro}</p>}

      <header className="linha-head">
        <div>
          <p className="kicker">Linha</p>
          <h1>{linha.codigo}</h1>
          <p className="muted">{linha.nome}</p>
        </div>
        <span className={`status-pill ${linha.status.tipo}`}>
          {linha.status.tipo === "atencao" ? "⚠ " : "✓ "}
          {linha.status.rotulo}
        </span>
      </header>

      <MapaRota
        origem={linha.origem}
        destino={linha.destino}
        latOrigem={linha.lat_origem}
        lngOrigem={linha.lng_origem}
        latDestino={linha.lat_destino}
        lngDestino={linha.lng_destino}
      />

      <div className="ocorrencias-head">
        <h2>Ocorrências ativas</h2>
        <span className="count">{linha.relatos.length}</span>
      </div>

      <ul className="ocorrencias">
        {linha.relatos.length === 0 && (
          <li className="empty">Nenhuma ocorrência nas últimas 2 horas.</li>
        )}
        {linha.relatos.map((relato) => (
          <li key={relato.id} className={`ocorrencia ${relato.tipo}`}>
            <TipoIcon tipo={relato.tipo} />
            <div>
              <strong>{relato.rotulo}</strong>
              <p>{relato.mensagem || "Sem detalhe extra."}</p>
              <small>Registrada {tempoAtras(relato.created_at)}</small>
              {relato.local && <small>📍 {relato.local}</small>}
              <span className={`selo ${relato.confiabilidade === "confirmado_comunidade" ? "ok" : ""}`}>
                {relato.confiabilidade_rotulo || "Não confirmado"}
              </span>
            </div>
            <button type="button" onClick={() => confirmar(relato.id)}>
              {relato.confirmacoes} confirmações
            </button>
          </li>
        ))}
      </ul>

      <button className="btn ghost" type="button" onClick={informar}>
        + Informar ocorrência
      </button>
      <Link className="text-link" to={`/linha/${linha.codigo}/qr`}>
        QR Code desta linha
      </Link>
      <Logo size="sm" />
    </section>
  );
}
