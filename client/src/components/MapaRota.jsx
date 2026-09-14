export default function MapaRota({ origem, destino }) {
  return (
    <div className="mapa">
      <span className="ao-vivo">Ao vivo</span>
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
      <div className="mapa-legend">
        <span>{origem}</span>
        <span>{destino}</span>
      </div>
    </div>
  );
}
