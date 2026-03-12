// src/pages/CompanyInfo.jsx
import { Fragment, useEffect, useRef, useState } from 'react';
import {
  adminFetch, adminToast, adminLoading, adminSuccess, adminAlert, adminConfirm,
  formatTel, formatHp, formatAgentNum,
  validateTel, validateEmail, validateAgentNum,
} from '../utils/adminCommon';

const PLACEHOLDER_IMG = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160"%3E%3Crect width="160" height="160" fill="%23e5e7eb"/%3E%3Ctext x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" font-size="13" fill="%239ca3af"%3E이미지 없음%3C/text%3E%3C/svg%3E';

const IMAGE_FIELDS = [
  { fieldKey: 'kolas',   urlKey: 'kolasUrl',   label: 'KOLAS 인증 이미지' },
  { fieldKey: 'ilac',    urlKey: 'ilacUrl',    label: '아일락(ILAC) 이미지' },
  { fieldKey: 'company', urlKey: 'companyUrl', label: '사내 로고' },
];

const FORM_ROWS = [
  [
    { key: 'name',     label: '회사명' },
    { key: 'nameEn',   label: '회사명(영문)' },
  ],
  [
    { key: 'ceo',      label: '대표자(CEO)' },
    { key: 'agentNum', label: '사업자등록번호' },
  ],
  [
    { key: 'tel',      label: '전화번호' },
    { key: 'fax',      label: 'FAX' },
  ],
  [
    { key: 'hp',       label: '휴대폰연락처' },
    { key: 'email',    label: '이메일' },
  ],
  [{ key: 'backAccount',        label: '거래은행(계좌번호)', colSpan: 3 }],
  [{ key: 'addr',               label: '주소',                colSpan: 3 }],
  [{ key: 'addrEn',             label: '주소(영문)',           colSpan: 3 }],
  [{ key: 'reportIssueAddr',    label: '성적서발행처주소',     colSpan: 3 }],
  [{ key: 'reportIssueAddrEn',  label: '성적서발행처주소(영문)', colSpan: 3 }],
  [{ key: 'siteAddr',           label: '소재지주소',           colSpan: 3 }],
  [{ key: 'siteAddrEn',         label: '소재지주소(영문)',      colSpan: 3 }],
];

const TEL_FIELDS = new Set(['tel', 'fax']);

// 이미지 상태 초기값
const initImageState = () => ({
  kolas:   { file: null, previewUrl: null },
  ilac:    { file: null, previewUrl: null },
  company: { file: null, previewUrl: null },
});

export default function CompanyInfo() {
  const [form,        setForm]        = useState({});
  const [loading,     setLoading]     = useState(true);
  const [imageStates, setImageStates] = useState(initImageState);
  const fileInputRefs = useRef({ kolas: null, ilac: null, company: null });

  // ── 데이터 조회 ──────────────────────────────────────────────────────────────

  const fetchEnv = () => {
    setLoading(true);
    return adminFetch('/api/admin/env')
      .then((res) => setForm(res?.data ?? {}))
      .catch(() => adminAlert('조회 실패', '회사정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchEnv(); }, []);

  // ── 입력폼 핸들러 ─────────────────────────────────────────────────────────────

  const handleChange = (e) => {
    const { name, value } = e.target;
    let formatted = value;
    if (TEL_FIELDS.has(name))    formatted = formatTel(value);
    else if (name === 'hp')      formatted = formatHp(value);
    else if (name === 'agentNum') formatted = formatAgentNum(value);
    setForm((prev) => ({ ...prev, [name]: formatted }));
  };

  const handleSave = async () => {
    const v = (key) => form[key]?.trim() || null;

    if (v('tel')      && !validateTel(v('tel')))           { adminToast('전화번호 형식이 올바르지 않습니다.', 'error'); return; }
    if (v('hp')       && !validateTel(v('hp')))            { adminToast('휴대폰연락처 형식이 올바르지 않습니다.', 'error'); return; }
    if (v('email')    && !validateEmail(v('email')))       { adminToast('이메일 형식이 올바르지 않습니다.', 'error'); return; }
    if (v('agentNum') && !validateAgentNum(v('agentNum'))) { adminToast('사업자등록번호 형식이 올바르지 않습니다. (000-00-00000)', 'error'); return; }

    const ok = await adminConfirm('회사정보 저장', '저장하시겠습니까?');
    if (!ok) return;

    adminLoading('저장 중...');
    try {
      await adminFetch('/api/admin/env', {
        method: 'PATCH',
        body: JSON.stringify({
          name:              v('name'),
          nameEn:            v('nameEn'),
          ceo:               v('ceo'),
          tel:               v('tel'),
          fax:               v('fax'),
          hp:                v('hp'),
          addr:              v('addr'),
          addrEn:            v('addrEn'),
          email:             v('email'),
          reportIssueAddr:   v('reportIssueAddr'),
          reportIssueAddrEn: v('reportIssueAddrEn'),
          siteAddr:          v('siteAddr'),
          siteAddrEn:        v('siteAddrEn'),
          agentNum:          v('agentNum'),
          backAccount:       v('backAccount'),
        }),
      });
      fetchEnv();
      adminSuccess('저장 완료', '회사정보가 저장되었습니다.');
    } catch (err) {
      adminAlert('저장 실패', err.data?.msg || '저장 중 오류가 발생했습니다.');
    }
  };

  // ── 이미지 핸들러 ─────────────────────────────────────────────────────────────

  const clearImageState = (fieldKey) => {
    setImageStates((prev) => {
      if (prev[fieldKey].previewUrl) URL.revokeObjectURL(prev[fieldKey].previewUrl);
      return { ...prev, [fieldKey]: { file: null, previewUrl: null } };
    });
    if (fileInputRefs.current[fieldKey]) fileInputRefs.current[fieldKey].value = '';
  };

  const handleFileChange = (fieldKey, file) => {
    if (!file) return;
    const previewUrl = URL.createObjectURL(file);
    setImageStates((prev) => {
      if (prev[fieldKey].previewUrl) URL.revokeObjectURL(prev[fieldKey].previewUrl);
      return { ...prev, [fieldKey]: { file, previewUrl } };
    });
  };

  const handleImageSave = async (fieldKey) => {
    const { file } = imageStates[fieldKey];
    if (!file) { adminToast('저장할 이미지를 먼저 선택해주세요.', 'warning'); return; }

    // 클라이언트 검증
    if (!file.type.startsWith('image/')) { adminToast('이미지 파일만 업로드 가능합니다.', 'error'); return; }
    if (file.size > 10 * 1024 * 1024)   { adminToast('이미지 크기는 10MB를 초과할 수 없습니다.', 'error'); return; }

    const ok = await adminConfirm('이미지 저장', '이미지를 저장하시겠습니까?');
    if (!ok) return;

    const formData = new FormData();
    formData.append('file', file);

    adminLoading('저장 중...');
    try {
      // multipart 전송: Content-Type 헤더를 직접 지정하지 않아야 브라우저가 boundary 포함 자동 설정
      const res = await fetch(`/api/admin/env/images/${fieldKey}`, { method: 'POST', body: formData });
      if (!res.ok) {
        let data = null;
        try { data = await res.json(); } catch { /* noop */ }
        adminAlert('저장 실패', data?.msg || '이미지 저장 중 오류가 발생했습니다.');
        return;
      }
      clearImageState(fieldKey);
      fetchEnv();
      adminSuccess('저장 완료', '이미지가 저장되었습니다.');
    } catch {
      adminAlert('저장 실패', '이미지 저장 중 오류가 발생했습니다.');
    }
  };

  const handleImageDelete = async (fieldKey) => {
    const ok = await adminConfirm('이미지 삭제', '이미지를 삭제하시겠습니까?');
    if (!ok) return;

    adminLoading('삭제 중...');
    try {
      await adminFetch(`/api/admin/env/images/${fieldKey}`, { method: 'DELETE' });
      clearImageState(fieldKey);
      fetchEnv();
      adminSuccess('삭제 완료', '이미지가 삭제되었습니다.');
    } catch (err) {
      adminAlert('삭제 실패', err.data?.msg || '이미지 삭제 중 오류가 발생했습니다.');
    }
  };

  // ── 렌더 ─────────────────────────────────────────────────────────────────────

  if (loading) {
    return <div className="page" style={{ padding: '32px', color: 'var(--muted)' }}>불러오는 중...</div>;
  }

  return (
    <div className="page">

      {/* 페이지 제목 */}
      <h5 className="env-page-title">회사정보</h5>

      {/* 기본정보 카드 */}
      <div className="card panel" style={{ marginBottom: '16px' }}>
        <div className="env-card-header">
          <span className="panel-title">기본 정보</span>
          <button className="env-btn env-btn-primary" onClick={handleSave}>저장</button>
        </div>
        <table className="env-table">
          <colgroup>
            <col style={{ width: '130px' }} />
            <col />
            <col style={{ width: '130px' }} />
            <col />
          </colgroup>
          <tbody>
            {FORM_ROWS.map((row, ri) => (
              <tr key={ri}>
                {row.map(({ key, label, colSpan }) => (
                  <Fragment key={key}>
                    <th>{label}</th>
                    <td colSpan={colSpan}>
                      <input
                        className="env-table-input"
                        type="text"
                        name={key}
                        value={form[key] ?? ''}
                        onChange={handleChange}
                        autoComplete="off"
                      />
                    </td>
                  </Fragment>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* 이미지 정보 카드 */}
      <div className="card panel">
        <div className="panel-title" style={{ marginBottom: '14px' }}>이미지 정보</div>
        <div className="env-image-row">
          {IMAGE_FIELDS.map(({ fieldKey, urlKey, label }) => {
            const { previewUrl } = imageStates[fieldKey];
            const imgSrc = previewUrl || form[urlKey] || PLACEHOLDER_IMG;
            return (
              <div className="env-image-block" key={fieldKey}>
                <div className="env-image-label">{label}</div>
                <div className="env-image-box">
                  <img
                    src={imgSrc}
                    alt={label}
                    className="env-image-preview"
                    onError={(e) => { e.target.src = PLACEHOLDER_IMG; }}
                  />
                </div>
                <div className="env-image-actions">
                  <label className="env-btn env-btn-secondary" style={{ cursor: 'pointer' }}>
                    이미지선택
                    <input
                      type="file"
                      accept="image/*"
                      style={{ display: 'none' }}
                      ref={(el) => { fileInputRefs.current[fieldKey] = el; }}
                      onChange={(e) => handleFileChange(fieldKey, e.target.files[0] || null)}
                    />
                  </label>
                  <button className="env-btn env-btn-primary" onClick={() => handleImageSave(fieldKey)}>저장</button>
                  <button className="env-btn env-btn-danger"  onClick={() => handleImageDelete(fieldKey)}>삭제</button>
                </div>
              </div>
            );
          })}
        </div>
      </div>

    </div>
  );
}
