import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api.js";
import Logo from "../components/Logo.jsx";

export default function Home() {
  const navigate = useNavigate();
  const [busca, setBusca] = useState("");
  const [linhas, setLinhas] = useState([]);
  const [erro, setErro] = useState("");

  useEffect(() => {
    const timer = setTimeout(async () => {
      try {
        const query = busca.trim() ? `?q=${encodeURIComponent(busca.trim())}` : "";
        setLinhas(await api(`/api/linhas${query}`));
        setErro("");
      } catch (err) {
        setErro(err.message);
      }
    }, 180);
    return () => clearTimeout(timer);
  }, [busca]);

  const sugestao =
    linhas.find(
      (linha) =>
        linha.codigo.toLowerCase() === busca.trim().toLowerCase() ||
        (busca.trim().length >= 2 && linhas.length > 0)
    ) || (busca.trim() ? linhas[0] : linhas.find((l) => l.codigo === "020"));

  function consultar(event) {
    event.preventDefault();
    if (sugestao) navigate(`/linha/${sugestao.codigo}`);
    else setErro("Digite o número da linha para consultar.");
  }

  return (
    <section className="screen home">
      <Logo size="lg" />
      <div className="home-copy">
        <h1>Consulte sua linha</h1>
        <p>Depois de escanear o QR no abrigo, veja a linha antes do ônibus chegar.</p>
      </div>

      <form onSubmit={consultar}>
        <label className="field">
          <span>Número da linha</span>
          <input
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            placeholder="020"
            inputMode="numeric"
            autoComplete="off"
          />
        </label>

        {sugestao && (
          <button
            type="button"
            className="suggestion"
            onClick={() => navigate(`/linha/${sugestao.codigo}`)}
          >
            <span className="badge-num">{sugestao.codigo}</span>
            <span>
              <strong>{sugestao.nome}</strong>
              <small>Linha urbana · GRANDE RECIFE</small>
            </span>
            <span className="chevron">›</span>
          </button>
        )}

        {erro && <p className="erro">{erro}</p>}
        <button className="btn" type="submit">Consultar</button>
      </form>

      <nav className="home-links">
        <Link to="/sobre">Como funciona</Link>
        <Link to="/admin">Cadastrar linha</Link>
      </nav>
    </section>
  );
}
