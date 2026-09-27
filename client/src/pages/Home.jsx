import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api.js";
import { useAuth } from "../AuthContext.jsx";
import { obterPosicao } from "../geo.js";
import Logo from "../components/Logo.jsx";

export default function Home() {
  const navigate = useNavigate();
  const { usuario, sair } = useAuth();
  const [busca, setBusca] = useState("");
  const [linhas, setLinhas] = useState([]);
  const [erro, setErro] = useState("");
  const [perto, setPerto] = useState(null);
  const [localizando, setLocalizando] = useState(false);

  useEffect(() => {
    const timer = setTimeout(async () => {
      try {
        const params = new URLSearchParams();
        if (busca.trim()) params.set("q", busca.trim());
        if (perto) {
          params.set("lat", String(perto.lat));
          params.set("lng", String(perto.lng));
        }
        const query = params.toString() ? `?${params}` : "";
        setLinhas(await api(`/api/linhas${query}`));
        setErro("");
      } catch (err) {
        setErro(err.message);
      }
    }, 180);
    return () => clearTimeout(timer);
  }, [busca, perto]);

  const sugestao =
    linhas.find(
      (linha) =>
        linha.codigo.toLowerCase() === busca.trim().toLowerCase() ||
        (busca.trim().length >= 2 && linhas.length > 0)
    ) || (busca.trim() ? linhas[0] : linhas.find((l) => l.codigo === "020"));

  const lista = busca.trim() || perto ? linhas.slice(0, 6) : (sugestao ? [sugestao] : []);

  function consultar(event) {
    event.preventDefault();
    if (lista[0]) navigate(`/linha/${lista[0].codigo}`);
    else setErro("Digite o número da linha ou use sua localização.");
  }

  async function linhasPerto() {
    setLocalizando(true);
    setErro("");
    try {
      setPerto(await obterPosicao());
      setBusca("");
    } catch (err) {
      setErro(err.message);
    } finally {
      setLocalizando(false);
    }
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
            autoComplete="off"
          />
        </label>

        {lista.map((linha) => (
          <button
            key={linha.codigo}
            type="button"
            className="suggestion"
            onClick={() => navigate(`/linha/${linha.codigo}`)}
          >
            <span className="badge-num">{linha.codigo}</span>
            <span>
              <strong>{linha.nome}</strong>
              <small>
                {linha.distancia_km != null
                  ? `${linha.distancia_km.toFixed(1).replace(".", ",")} km de você`
                  : `${linha.origem} → ${linha.destino}`}
              </small>
            </span>
            <span className="chevron">›</span>
          </button>
        ))}

        {busca.trim() && !erro && linhas.length === 0 && (
          <p className="erro">Nenhuma linha cadastrada com esse código.</p>
        )}
        {erro && <p className="erro">{erro}</p>}
        <button className="btn" type="submit">Consultar</button>
        <button className="btn ghost" type="button" onClick={linhasPerto} disabled={localizando}>
          {localizando ? "Localizando…" : perto ? "Atualizar localização" : "Linhas perto de mim"}
        </button>
      </form>

      <nav className="home-links">
        {usuario ? (
          <button type="button" className="text-link" onClick={sair}>
            Sair ({usuario.nome.split(" ")[0]})
          </button>
        ) : (
          <Link to="/login">Entrar</Link>
        )}
        <Link to="/sobre">Como funciona</Link>
        <Link to="/admin">Cadastrar linha</Link>
      </nav>
    </section>
  );
}
