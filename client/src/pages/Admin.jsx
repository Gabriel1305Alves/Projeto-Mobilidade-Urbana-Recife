import { useState } from "react";
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

  function set(campo, valor) {
    setForm((atual) => ({ ...atual, [campo]: valor }));
  }

  async function enviar(event) {
    event.preventDefault();
    setErro("");
    try {
      const linha = await api("/api/linhas", {
        method: "POST",
        body: JSON.stringify(form),
      });
      navigate(`/linha/${linha.codigo}/qr`);
    } catch (err) {
      setErro(err.message);
    }
  }

  return (
    <section className="screen">
      <TopBar to="/" />
      <h1>Cadastrar linha</h1>
      <p className="muted">Cada linha nova ganha página e QR Code.</p>
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
              value={form[campo]}
              onChange={(e) => set(campo, e.target.value)}
            />
          </label>
        ))}
        {erro && <p className="erro">{erro}</p>}
        <button className="btn" type="submit">Cadastrar</button>
      </form>
    </section>
  );
}
