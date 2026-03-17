// src/layouts/AdminLayout.jsx
import { useEffect, useState } from "react";
import { Outlet } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import Topbar from "../components/Topbar";
import { adminFetch } from "../utils/adminCommon";

export default function AdminLayout() {
  // 'checking' : 세션 확인 중 (초기값, 렌더링 보류)
  // 'ok'       : 인증 확인됨 → 정상 렌더링
  // 'fail'     : 미인증 또는 오류 (adminFetch 내부에서 리다이렉트 진행 중)
  const [authStatus, setAuthStatus] = useState('checking');

  useEffect(() => {
    // 앱 마운트 시 세션 유효성 확인
    // - 인증된 경우: 200 응답 → 'ok' 상태로 전환하여 렌더링 허용
    // - 미인증인 경우: adminFetch 내부에서 401 감지 → 로그인 페이지로 리다이렉트 → 'fail' 전환
    adminFetch('/api/admin/session')
      .then(() => setAuthStatus('ok'))
      .catch(() => setAuthStatus('fail'));
  }, []);

  // 세션 확인 전 또는 리다이렉트 진행 중엔 아무것도 렌더링하지 않음 (화면 깜빡임 방지)
  if (authStatus !== 'ok') return null;

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
