import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api.js";
import { useAuth } from "../AuthContext.jsx";
import TopBar from "../components/TopBar.jsx";

const ABAS = [
  { id: "pedidos", rotulo: "Pedidos" },
  { id: "nova", rotulo: "Nova linha" },
  { id: "linhas", rotulo: "Cadastradas" },
];

export default function Admin() {
  const navigate = useNavigate();
  const { usuario, sair } = useAuth();
  const primeiraCarga = useRef(true);
  const [aba, setAba] = useState("pedidos");
  const [form, setForm] = useState({
    codigo: "",
    nome: "",
    origem: "",
    destino: "",
  });
  const [erro, setErro] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [linhas, setLinhas] = useState([]);
  const [pedidos, setPedidos] = useState([]);
  const [analisando, setAnalisando] = useState(null);
  const [busca, setBusca] = useState("");
  const [pronto, setPronto] = useState(false);

  const carregar = useCallback(async () => {
    if (usuario?.role !== "ADMIN") return;
    try {
      const [listaLinhas, listaPedidos] = await Promise.all([
        api("/api/linhas"),
        api("/api/solicitacoes-linha"),
      ]);
      setLinhas(listaLinhas);
      setPedidos(listaPedidos);
      setErro("");
      if (primeiraCarga.current) {
        primeiraCarga.current = false;
        setAba(listaPedidos.length > 0 ? "pedidos" : "linhas");
      }
      setPronto(true);
    } catch (err) {
      setErro(err.message);
      setPronto(true);
    }
  }, [usuario]);

  useEffect(() => {
    carregar();
  }, [carregar]);

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

  async function aprovar(id) {
    setAnalisando(id);
    setErro("");
    try {
      const linha = await api(`/api/solicitacoes-linha/${id}/aprovar`, { method: "POST" });
      navigate(`/linha/${linha.codigo}/qr`);
    } catch (err) {
      setErro(err.message);
      await carregar();
    } finally {
      setAnalisando(null);
    }
  }

  async function recusar(id) {
    setAnalisando(id);
    setErro("");
    try {
      await api(`/api/solicitacoes-linha/${id}/recusar`, { method: "POST" });
      setPedidos((atual) => atual.filter((p) => p.id !== id));
    } catch (err) {
      setErro(err.message);
      await carregar();
    } finally {
      setAnalisando(null);
    }
  }

  if (usuario?.role !== "ADMIN") {
    return (
      <section className="screen">
        <TopBar to="/" />
        <h1>Área da equipe</h1>
        <p className="muted">Só a conta da equipe cadastra linhas e analisa pedidos.</p>
        <Link className="btn" to="/login?next=/admin">Entrar com a conta da equipe</Link>
      </section>
    );
  }

  const termo = busca.trim().toLowerCase();
  const linhasFiltradas = termo
    ? linhas.filter((linha) =>
        [linha.codigo, linha.nome, linha.origem, linha.destino]
          .join(" ")
          .toLowerCase()
          .includes(termo)
      )
    : linhas;

  return (
    <section className="screen admin-screen">
      <TopBar to="/" />

      <header className="admin-head">
        <p className="kicker">Área da equipe</p>
        <h1>{usuario.nome.split(" ")[0]}</h1>
        <p className="muted">Gerencie pedidos, cadastre linhas e abra o QR Code.</p>
        <div className="admin-resumo">
          <article>
            <strong>{pronto ? pedidos.length : "…"}</strong>
            <span>pendentes</span>
          </article>
          <article>
            <strong>{pronto ? linhas.length : "…"}</strong>
            <span>cadastradas</span>
          </article>
        </div>
      </header>

      <nav className="admin-abas" aria-label="Seções do admin">
        {ABAS.map((item) => (
          <button
            key={item.id}
            type="button"
            className={aba === item.id ? "on" : ""}
            onClick={() => {
              setErro("");
              setAba(item.id);
            }}
          >
            {item.rotulo}
            {item.id === "pedidos" && pedidos.length > 0 ? ` (${pedidos.length})` : ""}
          </button>
        ))}
      </nav>

      {erro && <p className="erro">{erro}</p>}

      {aba === "pedidos" && (
        <section className="admin-secao">
          <h2>Pedidos da comunidade</h2>
          <p className="muted">
            Passageiros pedem linhas que ainda não existem. Aprove para liberar a página e o QR.
          </p>
          {pedidos.length === 0 ? (
            <p className="empty">Nenhum pedido pendente no momento.</p>
          ) : (
            <ul className="ocorrencias">
              {pedidos.map((pedido) => (
                <li key={pedido.id} className="pedido-linha">
                  <div className="pedido-topo">
                    <span className="badge-num">{pedido.codigo}</span>
                    <div>
                      <strong>{pedido.nome}</strong>
                      <small>{pedido.origem} → {pedido.destino}</small>
                      <small>Pedido por {pedido.solicitante}</small>
                    </div>
                  </div>
                  <div className="pedido-acoes">
                    <button
                      type="button"
                      className="btn"
                      disabled={analisando === pedido.id}
                      onClick={() => aprovar(pedido.id)}
                    >
                      Aprovar
                    </button>
                    <button
                      type="button"
                      className="btn ghost"
                      disabled={analisando === pedido.id}
                      onClick={() => recusar(pedido.id)}
                    >
                      Recusar
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {aba === "nova" && (
        <section className="admin-secao">
          <h2>Cadastrar linha</h2>
          <p className="muted">Use quando a equipe já tiver o código e o trajeto conferidos.</p>
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
            <button className="btn" type="submit" disabled={enviando}>
              {enviando ? "Cadastrando…" : "Cadastrar e gerar QR"}
            </button>
          </form>
        </section>
      )}

      {aba === "linhas" && (
        <section className="admin-secao">
          <h2>Linhas cadastradas</h2>
          <p className="muted">Abra uma linha para ver alertas ou imprimir o QR Code.</p>
          <label className="field">
            <span>Buscar</span>
            <input
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
              placeholder="Código, nome ou bairro"
              autoComplete="off"
            />
          </label>
          {linhasFiltradas.length === 0 ? (
            <p className="empty">Nenhuma linha com esse filtro.</p>
          ) : (
            <ul className="ocorrencias">
              {linhasFiltradas.map((linha) => (
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
          )}
        </section>
      )}

      <nav className="admin-rodape">
        <Link className="text-link" to="/conta">Meus dados</Link>
        <button
          type="button"
          className="text-link"
          onClick={() => {
            sair();
            navigate("/");
          }}
        >
          Sair
        </button>
      </nav>
    </section>
  );
}
