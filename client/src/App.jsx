import { Navigate, Route, Routes } from "react-router-dom";
import PhoneShell from "./components/PhoneShell.jsx";
import Home from "./pages/Home.jsx";
import Linha from "./pages/Linha.jsx";
import Relatar from "./pages/Relatar.jsx";
import Sucesso from "./pages/Sucesso.jsx";
import Sobre from "./pages/Sobre.jsx";
import Admin from "./pages/Admin.jsx";
import Qr from "./pages/Qr.jsx";
import Login from "./pages/Login.jsx";
import Cadastro from "./pages/Cadastro.jsx";

export default function App() {
  return (
    <PhoneShell>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="/linha/:codigo" element={<Linha />} />
        <Route path="/linha/:codigo/relatar" element={<Relatar />} />
        <Route path="/linha/:codigo/sucesso" element={<Sucesso />} />
        <Route path="/linha/:codigo/qr" element={<Qr />} />
        <Route path="/sobre" element={<Sobre />} />
        <Route path="/admin" element={<Admin />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </PhoneShell>
  );
}
