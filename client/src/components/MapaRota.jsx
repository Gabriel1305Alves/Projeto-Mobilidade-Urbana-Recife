export default function MapaRota({
  origem,
  destino,
  latOrigem,
  lngOrigem,
  latDestino,
  lngDestino,
}) {
  const pontos = [
    [latOrigem, lngOrigem],
    [latDestino, lngDestino],
  ].filter(([lat, lng]) => lat != null && lng != null);
  const temMapa = pontos.length > 0;
  const lats = pontos.map(([lat]) => lat);
  const lngs = pontos.map(([, lng]) => lng);
  const minLat = Math.min(...lats) - 0.03;
  const maxLat = Math.max(...lats) + 0.03;
  const minLng = Math.min(...lngs) - 0.03;
  const maxLng = Math.max(...lngs) + 0.03;
  const mapaUrl = temMapa
    ? `https://www.openstreetmap.org/export/embed.html?bbox=${minLng}%2C${minLat}%2C${maxLng}%2C${maxLat}&layer=mapnik&marker=${pontos[0][0]}%2C${pontos[0][1]}`
    : "";

  return (
    <div className="mapa">
      <span className="ao-vivo">Ao vivo</span>
      {temMapa ? (
        <iframe
          className="mapa-iframe"
          title={`Rota ${origem} — ${destino}`}
          src={mapaUrl}
        />
      ) : (
        <svg viewBox="0 0 320 92" className="rota-svg">
          <path
            d="M20 62 C 90 62, 90 28, 160 28 S 230 62, 300 36"
            fill="none"
            stroke="#93c5fd"
            strokeWidth="6"
            strokeDasharray="2 10"
            strokeLinecap="round"
          />
          <circle cx="20" cy="62" r="6" fill="#3b82f6" />
          <circle cx="300" cy="36" r="6" fill="#3b82f6" />
          <circle cx="168" cy="26" r="11" fill="#fbbf24" stroke="#fff" strokeWidth="3" />
        </svg>
      )}
      <div className="mapa-legend">
        <span>{origem}</span>
        <span>{destino}</span>
      </div>
    </div>
  );
}
