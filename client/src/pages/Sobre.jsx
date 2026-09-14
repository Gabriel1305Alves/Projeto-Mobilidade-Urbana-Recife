import { Link } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import TopBar from "../components/TopBar.jsx";

export default function Sobre() {
  return (
    <section className="screen sobre">
      <TopBar to="/" />
      <Logo size="md" />
      <p className="kicker">Equipe 2 · Grande Recife</p>
      <h1>O apagão de informações</h1>
      <img className="sobre-foto" src="/ponto.png" alt="Fila no ponto de ônibus" />
      <p>
        Hoje o passageiro é o último a saber. Chega na parada debaixo de chuva
        e não sabe se o ônibus atrasou, se a rua alagou ou se o veículo quebrou
        no caminho. Essa falta de previsibilidade gera atraso no trabalho,
        estresse e até risco físico.
      </p>
      <ul className="sobre-list">
        <li>
          <strong>QR Code no abrigo</strong>
          <span>Cola no poste. Sem baixar aplicativo pesado.</span>
        </li>
        <li>
          <strong>Status na hora</strong>
          <span>Quem já está no trajeto avisa quem ainda espera.</span>
        </li>
        <li>
          <strong>Colaborativo</strong>
          <span>A comunidade alimenta trânsito, alagamento, bloqueio e acidente.</span>
        </li>
        <li>
          <strong>Região Metropolitana</strong>
          <span>Feito para o dia a dia de quem se desloca no Grande Recife.</span>
        </li>
      </ul>
      <p className="muted equipe">
        Alessandra Feitosa · Arthur Allysson · Carlos Henrique · David Richard ·
        Gabriel Alves · Mayara Beatriz
      </p>
      <Link className="btn" to="/">Consultar uma linha</Link>
    </section>
  );
}
