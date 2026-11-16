import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../api.js";
import { useAuth } from "../AuthContext.jsx";
import TopBar from "../components/TopBar.jsx";

export default function SolicitarLinha() {
  const navigate = useNavigate();
  const { usuario } = useAuth();
  const [params] = useSearchParams();
  const [form, setForm] = useState({
    codigo: params.get("codigo") || "",
    nome: "",
    origem: "",
    destino: "",
  });
  const [erro, setErro] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [ok, setOk] = useState(false);

  function set(campo, valor) {
    setForm((atual) => ({ ...atual, [campo]: valor }));
  }

  async function enviar(event) {
    event.preventDefault();
    if (!usuario) {
      navigate(`/login?next=${encodeURIComponent(`/solicitar-linha?codigo=${form.codigo.trim()}`)}`);
      return;
    }
    setErro("");
    setEnviando(true);
    try {
      await api("/api/solicitacoes-linha", {
        method: "POST",
        body: JSON.stringify(form),
      });
      setOk(true);
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  if (ok) {
    return (
      <section className="screen">
        <TopBar to="/" />
        <h1>Pedido enviado</h1>
        <p className="muted">
          A equipe vai analisar o cadastro da linha {form.codigo.trim().toUpperCase() || "pedida"}.
          Quando aprovar, ela aparece na busca.
        </p>
        <Link className="btn" to="/">Voltar à consulta</Link>
      </section>
    );
  }

  return (
    <section className="screen">
      <TopBar to="/" />
      <h1>Solicitar linha</h1>
      <p className="muted">
        Não achou sua linha? Peça o cadastro. A equipe confere e libera o QR Code.
      </p>
      {!usuario && (
        <p className="muted">
          É preciso{" "}
          <Link to={`/login?next=${encodeURIComponent(`/solicitar-linha?codigo=${form.codigo.trim()}`)}`}>
            entrar
          </Link>{" "}
          para enviar o pedido.
        </p>
      )}
      <form onSubmit={enviar}>
        {[
          ["codigo", "Código", "020"],
          ["nome", "Nome", "Bairro A / Bairro B"],
          ["origem", "Origem", "Terminal ou bairro"],
          ["destino", "Destino", "Terminal ou bairro"],
        ].map(([campo, label, placeholder]) => (
          <label className="field" key={campo}>
            <span>{label}</span>
            <input
              type="text"
              required
              autoComplete="off"
              placeholder={placeholder}
              value={form[campo]}
              onChange={(e) => set(campo, e.target.value)}
            />
          </label>
        ))}
        {erro && <p className="erro">{erro}</p>}
        <button className="btn" type="submit" disabled={enviando}>
          {enviando ? "Enviando…" : usuario ? "Enviar pedido" : "Entrar e enviar"}
        </button>
      </form>
    </section>
  );
}
