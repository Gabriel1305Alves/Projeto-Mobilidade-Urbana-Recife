import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api.js";

export default function AlternativasIA({ codigo, atualizacao }) {
  const [recomendacao, setRecomendacao] = useState(null);

  useEffect(() => {
    api(`/api/ia/linhas/${encodeURIComponent(codigo)}/alternativas`)
      .then(setRecomendacao)
      .catch(() => setRecomendacao(null));
  }, [codigo, atualizacao]);

  if (!recomendacao || recomendacao.alternativas.length === 0) return null;

  return (
    <section className="alternativas">
      <p className="kicker">🤖 Linhas alternativas recomendadas</p>
      <ul>
        {recomendacao.alternativas.map((a) => (
          <li key={a.codigo}>
            <Link to={`/linha/${encodeURIComponent(a.codigo)}`} className="alternativa">
              <div className="alternativa-head">
                <strong>{a.codigo}</strong>
                <span>{a.nome}</span>
                <span className={`alternativa-risco ${a.nivel}`}>{a.probabilidade}%</span>
              </div>
              <small>{a.motivos.join(" · ")}</small>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
