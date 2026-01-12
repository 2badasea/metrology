// src/layouts/AdminLayout.jsx
import { Outlet } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import Topbar from "../components/Topbar";

export default function AdminLayout() {
  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <Sidebar />
      </aside>

      <header className="admin-topbar">
        <Topbar />
      </header>

      <main className="admin-content">
        <Outlet />
      </main>
    </div>
  );
}
