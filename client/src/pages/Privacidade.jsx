import { Link } from "react-router-dom";
import TopBar from "../components/TopBar.jsx";

export default function Privacidade() {
  return (
    <section className="screen sobre">
      <TopBar to="/" />
      <p className="kicker">Equipe 2 · Embarcaí</p>
      <h1>Seus dados na linha</h1>
      <p>
        O Embarcaí trata só o que precisa para o passageiro consultar a linha
        e avisar quem ainda está no ponto. Não vendemos dados e não usamos
        e-mail para propaganda.
      </p>
      <ul className="sobre-list">
        <li>
          <strong>Conta</strong>
          <span>Nome, e-mail e senha. A senha fica gravada só como hash BCrypt, nunca em texto puro.</span>
        </li>
        <li>
          <strong>Alerta</strong>
          <span>Tipo, mensagem e o nome de quem avisou. O alerta some da listagem ativa depois de 2 horas.</span>
        </li>
        <li>
          <strong>Localização</strong>
          <span>Só entra se você permitir o GPS, para mostrar o ponto do alerta ou as linhas perto de você.</span>
        </li>
        <li>
          <strong>Consentimento</strong>
          <span>A conta só é criada depois que você aceita este tratamento. A data fica registrada.</span>
        </li>
        <li>
          <strong>Seus direitos</strong>
          <span>Em Meus dados você vê o que a conta guarda e pode apagá-la. O alerta continua na linha, sem o seu nome.</span>
        </li>
        <li>
          <strong>Acesso</strong>
          <span>Relato e confirmação exigem login. Há limite de envios e a mesma ocorrência não se repete em 30 minutos.</span>
        </li>
      </ul>
      <Link className="btn" to="/conta">Ver meus dados</Link>
    </section>
  );
}
