import { useNavigate } from "react-router-dom";

export default function TopBar({ to, children = "Voltar" }) {
  const navigate = useNavigate();

  return (
    <button
      type="button"
      className="back"
      onClick={() => (to ? navigate(to) : navigate(-1))}
    >
      <span aria-hidden="true">‹</span> {children}
    </button>
  );
}
