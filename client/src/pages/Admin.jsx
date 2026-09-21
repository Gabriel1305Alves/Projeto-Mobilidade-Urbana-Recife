import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api.js";
import TopBar from "../components/TopBar.jsx";

export default function Admin() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    senha: "",
    codigo: "",
    nome: "",
    origem: "",
    destino: "",
  });
  const [erro, setErro] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [linhas, setLinhas] = useState([]);

  useEffect(() => {
    api("/api/linhas")
      .then(setLinhas)
      .catch((err) => setErro(err.message));
  }, []);

  function set(campo, valor) {
    setForm((atual) => ({ ...atual, [campo]: valor }));
  }

  async function enviar(event) {
    event.preventDefault();
    setErro("");
    setEnviando(true);
    try {
      const linha = await api("/api/linhas", {
        method: "POST",
        body: JSON.stringify(form),
      });
      navigate(`/linha/${linha.codigo}/qr`);
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section className="screen">
      <TopBar to="/" />
      <h1>Cadastrar linha</h1>
      <p className="muted">Cada linha nova ganha página e QR Code. Senha da equipe: admin123</p>
      <form onSubmit={enviar}>
        {[
          ["senha", "Senha de administração", "password"],
          ["codigo", "Código", "text"],
          ["nome", "Nome", "text"],
          ["origem", "Origem", "text"],
          ["destino", "Destino", "text"],
        ].map(([campo, label, type]) => (
          <label className="field" key={campo}>
            <span>{label}</span>
            <input
              type={type}
              required
              autoComplete="off"
              placeholder={campo === "senha" ? "admin123" : campo === "codigo" ? "020" : ""}
              value={form[campo]}
              onChange={(e) => set(campo, e.target.value)}
            />
          </label>
        ))}
        {erro && <p className="erro">{erro}</p>}
        <button className="btn" type="submit" disabled={enviando}>
          {enviando ? "Cadastrando…" : "Cadastrar"}
        </button>
      </form>

      {linhas.length > 0 && (
        <div>
          <h2 className="label">Linhas já cadastradas</h2>
          <ul className="ocorrencias">
            {linhas.map((linha) => (
              <li key={linha.codigo}>
                <button
                  type="button"
                  className="suggestion"
                  onClick={() => navigate(`/linha/${linha.codigo}`)}
                >
                  <span className="badge-num">{linha.codigo}</span>
                  <span>
                    <strong>{linha.nome}</strong>
                    <small>{linha.origem} → {linha.destino}</small>
                  </span>
                  <span className="chevron">›</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
