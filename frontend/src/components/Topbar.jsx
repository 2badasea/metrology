// src/components/Topbar.jsx
import { adminConfirm, adminLoading, adminCloseLoading, adminAlert, adminFetch } from '../utils/adminCommon';

// 운영: 동일 origin / 개발: Vite 프록시가 /logout을 백엔드로 전달
const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN || '';

/**
 * 로그아웃 처리
 * 1. SweetAlert2 confirm → 2. POST /logout → 3. 백엔드 로그인 페이지로 이동
 * /logout은 Vite 프록시 대상이 아니므로 backendOrigin을 직접 명시
 */
async function handleLogout() {
  const confirmed = await adminConfirm('로그아웃', '로그아웃 하시겠습니까?');
  if (!confirmed) return;

  adminLoading('로그아웃 중...');
  try {
    // CSRF 비활성화 상태이므로 토큰 불필요, Content-Type은 adminFetch 기본값 사용
    // 개발: Vite 프록시(/logout → localhost:8050) / 운영: 동일 origin → 상대경로로 통일
    const res = await adminFetch('/logout', { method: 'POST' });
    adminCloseLoading();

    if (res?.ok === true) {
      // res.redirect = "/member/login?logout" (백엔드 상대경로)
      window.location.href = backendOrigin + res.redirect;
    } else {
      await adminAlert('로그아웃 실패', '로그아웃 중 오류가 발생했습니다.');
    }
  } catch (err) {
    console.error('logout error:', err);
    adminCloseLoading();
    await adminAlert('로그아웃 실패', '로그아웃 중 오류가 발생했습니다.');
  }
}

export default function Topbar() {
  return (
    <div className="topbar">
      <div className="topbar-left">
        <div className="topbar-title">Dashboard</div>

        <div className="topbar-search">
          <input placeholder="Search" />
        </div>
      </div>

      <div className="topbar-right">
        <button className="topbar-btn" type="button">Account</button>
        <button className="topbar-btn" type="button">Dropdown</button>
        <button className="topbar-btn primary" type="button" onClick={handleLogout}>로그아웃</button>
      </div>
    </div>
  );
}
