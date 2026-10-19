import { useEffect, useState } from "react";
import { api } from "../api.js";

function plural(n, singular, pluralTexto) {
  return `${n} ${n === 1 ? singular : pluralTexto}`;
}

export default function PrevisaoIA({ codigo, atualizacao }) {
  const [previsao, setPrevisao] = useState(null);

  useEffect(() => {
    api(`/api/ia/linhas/${encodeURIComponent(codigo)}/previsao`)
      .then(setPrevisao)
      .catch(() => setPrevisao(null));
  }, [codigo, atualizacao]);

  if (!previsao) return null;

  const tranquilo = previsao.nivel === "baixo" && previsao.alertas_ativos === 0;

  return (
    <section className={`previsao ${previsao.nivel}`}>
      <div className="previsao-head">
        <p className="kicker">🤖 Previsão da IA</p>
        <small>Chance de atraso ou problema no trajeto</small>
        <strong>
          {previsao.nivel_rotulo} · {previsao.probabilidade}%
        </strong>
        <small>
          {tranquilo
            ? "Trajeto deve seguir tranquilo"
            : `Problema mais provável: ${previsao.tipo_provavel_rotulo.toLowerCase()}`}
        </small>
      </div>
      <div className="previsao-horas" aria-label="Risco nas próximas horas">
        {previsao.proximas_horas.map((h, i) => (
          <div key={h.hora} className="previsao-hora">
            <small className="previsao-pct">{h.probabilidade}%</small>
            <span className={`barra ${h.nivel}`} style={{ height: `${Math.max(4, h.probabilidade * 0.65)}%` }} />
            <small>{i === 0 ? "Agora" : `${h.hora}h`}</small>
          </div>
        ))}
      </div>
      <ul className="previsao-fatores">
        {previsao.fatores.map((f) => (
          <li key={f.texto} className={f.efeito}>
            {f.texto}
          </li>
        ))}
      </ul>
      <small className="previsao-base">
        Com base em {plural(previsao.alertas_ativos, "alerta ativo", "alertas ativos")} e{" "}
        {plural(previsao.ocorrencias_analisadas, "ocorrência", "ocorrências")} das últimas 4 semanas.
      </small>
    </section>
  );
}
