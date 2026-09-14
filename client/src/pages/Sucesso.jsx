import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api.js";
import Logo from "../components/Logo.jsx";
import TopBar from "../components/TopBar.jsx";

function formatar(n) {
  if (n >= 1000) return `${(n / 1000).toFixed(1).replace(".0", "")}k`;
  return String(n);
}

export default function Sucesso() {
  const { codigo } = useParams();
  const [stats, setStats] = useState(null);

  useEffect(() => {
    api("/api/stats").then(setStats).catch(() => {});
  }, []);

  return (
    <section className="screen sucesso">
      <TopBar to={`/linha/${codigo}`} />
      <div className="check">✓</div>
      <h1>Ocorrência enviada!</h1>
      <p>
        Obrigado por ajudar outros passageiros. Sua contribuição torna o
        transporte mais seguro e previsível para todos.
      </p>

      {stats && (
        <div className="stats">
          <article>
            <strong>{formatar(stats.relatos_hoje)}</strong>
            <span>Relatos hoje</span>
          </article>
          <article>
            <strong>{formatar(stats.usuarios_ativos)}</strong>
            <span>Usuários ativos</span>
          </article>
          <article>
            <strong>{stats.precisao}%</strong>
            <span>Precisão</span>
          </article>
        </div>
      )}

      <Link className="btn" to={`/linha/${codigo}`}>
        Voltar para a linha
      </Link>
      <Logo size="sm" />
    </section>
  );
}
