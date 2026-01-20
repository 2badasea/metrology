// src/components/Sidebar.jsx
import { NavLink } from "react-router-dom";

const menus = [
  { to: "/dashboard", label: "DASHBOARD" },
  { to: "/company-accounts", label: "업체계정관리" },
  { to: "/login-history", label: "로그인 이력관리" },
  { to: "/work-history", label: "작업이력관리" },
  { to: "/menu-permissions", label: "사용자메뉴권한관리" },
  { to: "/notices", label: "업데이트공지" },
];

const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN;

export default function Sidebar() {
  return (
    <div className="sidebar">
      <div className="sidebar-brand">
        <div className="brand-mark" />
        <div className="brand-title">CALI ADMIN</div>
      </div>

      <nav className="sidebar-nav">
        {menus.map((m) => (
          <NavLink
            key={m.to}
            to={m.to}
            className={({ isActive }) => `sidebar-link ${isActive ? "active" : ""}`}
          >
            <span className="sidebar-bullet" />
            <span className="sidebar-text">{m.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <button className="sidebar-cta" type="button" onClick={ () => {
            location.href = `http://211.188.55.249/cali/caliOrder`;
            // location.href = `${backendOrigin}/cali/caliOrder`;
        }}>
          교정관리 돌아가기
        </button>
      </div>
    </div>
  );
}
