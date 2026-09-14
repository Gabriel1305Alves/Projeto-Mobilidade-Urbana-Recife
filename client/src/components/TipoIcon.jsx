export default function TipoIcon({ tipo }) {
  const mapa = {
    transito: "🚗",
    alagamento: "🌊",
    bloqueio: "🚧",
    acidente: "⚠️",
  };
  return <span className="tipo-icon">{mapa[tipo] || "•"}</span>;
}
