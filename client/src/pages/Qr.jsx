import { useParams } from "react-router-dom";
import { qrUrl } from "../api.js";
import Logo from "../components/Logo.jsx";
import TopBar from "../components/TopBar.jsx";

export default function Qr() {
  const { codigo } = useParams();

  return (
    <section className="screen qr-screen">
      <TopBar to={`/linha/${codigo}`} />
      <Logo size="md" />
      <h1>Linha {codigo}</h1>
      <p>Cole no ponto. Não precisa baixar app.</p>
      <img
        className="qr-img"
        src={qrUrl(codigo)}
        alt={`QR Code da linha ${codigo}`}
      />
      <button className="btn" type="button" onClick={() => window.print()}>
        Imprimir cartaz
      </button>
    </section>
  );
}
