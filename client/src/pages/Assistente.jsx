import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../api.js";
import TopBar from "../components/TopBar.jsx";

const BOAS_VINDAS = {
  autor: "bot",
  texto:
    "Olá! Sou o assistente do Embarcaí. Posso dizer como está uma linha, prever o risco de atraso, sugerir outra linha para o mesmo trajeto e avisar onde tem alagamento.",
  sugestoes: ["Como está a linha 020?", "Tem alagamento agora?", "Quais linhas existem?"],
};

export default function Assistente() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [mensagens, setMensagens] = useState([BOAS_VINDAS]);
  const [texto, setTexto] = useState("");
  const [linha, setLinha] = useState(params.get("linha") || "");
  const [enviando, setEnviando] = useState(false);
  const fim = useRef(null);

  useEffect(() => {
    fim.current?.scrollIntoView({ behavior: "smooth" });
  }, [mensagens]);

  async function perguntar(pergunta) {
    const conteudo = pergunta.trim();
    if (!conteudo || enviando) return;
    setTexto("");
    setMensagens((atual) => [...atual, { autor: "eu", texto: conteudo }]);
    setEnviando(true);
    try {
      const resposta = await api("/api/ia/assistente", {
        method: "POST",
        body: JSON.stringify({ mensagem: conteudo, linha }),
      });
      if (resposta.linha) setLinha(resposta.linha);
      setMensagens((atual) => [
        ...atual,
        { autor: "bot", texto: resposta.resposta, sugestoes: resposta.sugestoes, acao: resposta.acao },
      ]);
    } catch (err) {
      setMensagens((atual) => [...atual, { autor: "bot", texto: err.message }]);
    } finally {
      setEnviando(false);
    }
  }

  const ultima = mensagens[mensagens.length - 1];

  return (
    <section className="screen chat-screen">
      <TopBar to={linha ? `/linha/${linha}` : "/"} />
      <header>
        <p className="kicker">🤖 Assistente Embarcaí</p>
        <h1>Pergunte sobre sua linha</h1>
      </header>

      <ul className="chat">
        {mensagens.map((m, i) => (
          <li key={i} className={`balao ${m.autor}`}>
            {m.texto}
            {m.acao && (
              <button type="button" className="balao-acao" onClick={() => navigate(m.acao.rota)}>
                {m.acao.rotulo} ›
              </button>
            )}
          </li>
        ))}
        {enviando && <li className="balao bot digitando">digitando…</li>}
        <li ref={fim} aria-hidden="true" />
      </ul>

      {ultima.autor === "bot" && ultima.sugestoes && !enviando && (
        <div className="chips">
          {ultima.sugestoes.map((s) => (
            <button key={s} type="button" onClick={() => perguntar(s)}>
              {s}
            </button>
          ))}
        </div>
      )}

      <form
        className="chat-form"
        onSubmit={(e) => {
          e.preventDefault();
          perguntar(texto);
        }}
      >
        <input
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          placeholder="Ex.: vai atrasar às 18h?"
          maxLength={500}
          aria-label="Sua pergunta"
        />
        <button className="btn" type="submit" disabled={!texto.trim() || enviando}>
          Enviar
        </button>
      </form>
    </section>
  );
}
