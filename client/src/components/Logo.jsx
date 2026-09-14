import { Link } from "react-router-dom";

export default function Logo({ size = "md" }) {
  return (
    <Link to="/" className={`logo logo-${size}`}>
      <span className="logo-mark" aria-hidden="true">
        <svg viewBox="0 0 64 64" fill="none">
          <rect x="8" y="22" width="12" height="26" rx="2" fill="#1e3a5f" />
          <rect x="18" y="14" width="10" height="34" rx="2" fill="#2b4d73" />
          <rect x="26" y="18" width="9" height="30" rx="2" fill="#1b2430" />
          <rect x="10" y="34" width="44" height="16" rx="5" fill="#3b82f6" />
          <rect x="16" y="38" width="10" height="7" rx="2" fill="#dbeafe" />
          <rect x="38" y="38" width="10" height="7" rx="2" fill="#dbeafe" />
          <circle cx="20" cy="52" r="4" fill="#1b2430" />
          <circle cx="44" cy="52" r="4" fill="#1b2430" />
          <circle cx="20" cy="52" r="1.6" fill="#93c5fd" />
          <circle cx="44" cy="52" r="1.6" fill="#93c5fd" />
        </svg>
      </span>
      <span className="logo-word">Embarcaí</span>
    </Link>
  );
}
