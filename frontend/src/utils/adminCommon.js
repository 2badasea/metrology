// src/utils/adminCommon.js
console.log('++ utils/adminCommon.js');

import Swal from 'sweetalert2';
import { toast } from 'react-toastify';

/* ─────────────────────────────────────────────
 * HTTP
 * ───────────────────────────────────────────── */

/**
 * fetch 래퍼
 * - JSON Content-Type 기본 설정 (multipart 전송 시 headers에서 제외할 것)
 * - HTTP 에러(response.ok=false) 시 Error throw
 * - 응답 JSON 자동 파싱 (204 No Content는 null 반환)
 *
 * @param {string} url
 * @param {RequestInit} options
 * @returns {Promise<any>}
 */
export const adminFetch = async (url, options = {}) => {
  const { headers, ...rest } = options;

  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
    ...rest,
  });

  if (!res.ok) {
    const err = new Error(`HTTP ${res.status}`);
    err.status = res.status;
    try {
      err.data = await res.json();
    } catch {
      err.data = null;
    }
    throw err;
  }

  if (res.status === 204) return null;
  return res.json();
};

/* ─────────────────────────────────────────────
 * UI — Toast (입력 검증·간단 안내, 우측 상단)
 * ───────────────────────────────────────────── */

/**
 * 우측 상단 토스트 (입력값 검증 오류·간단 안내용)
 * - react-toastify 기반, ToastContainer가 앱에 마운트되어 있어야 함
 *
 * @param {string} msg
 * @param {'info'|'success'|'error'|'warning'} type
 */
export const adminToast = (msg, type = 'info') => toast[type](msg);

/* ─────────────────────────────────────────────
 * UI — 로딩
 * ───────────────────────────────────────────── */

/**
 * 로딩 다이얼로그 표시 (gLoadingMessage 역할)
 * - adminCloseLoading() 으로 닫을 것
 *
 * @param {string} title
 */
export const adminLoading = (title = '처리 중...') => {
  Swal.fire({
    title,
    allowOutsideClick: false,
    showConfirmButton: false,
    didOpen: () => Swal.showLoading(),
  });
};

/**
 * 로딩 다이얼로그 닫기
 */
export const adminCloseLoading = () => Swal.close();

/* ─────────────────────────────────────────────
 * UI — 성공 메시지
 * ───────────────────────────────────────────── */

/**
 * 성공 메시지 (중앙, 확인 버튼 + 3초 타이머 자동 닫힘, gMessage 역할)
 *
 * @param {string} title  - 굵은 제목 (예: "저장 완료")
 * @param {string} html   - 본문 내용 (예: "회사정보가 저장되었습니다.")
 * @returns {Promise}
 */
export const adminSuccess = (title, html = '') =>
  Swal.fire({
    title,
    html: html || undefined,
    icon: 'success',
    timer: 3000,
    timerProgressBar: true,
    showConfirmButton: true,
    confirmButtonText: '확인',
  });

/* ─────────────────────────────────────────────
 * UI — 오류/안내 메시지
 * ───────────────────────────────────────────── */

/**
 * 오류·안내 메시지 (중앙, 확인 클릭 시 닫힘, gMessage alert 역할)
 *
 * @param {string} title  - 굵은 제목 (예: "저장 실패")
 * @param {string} html   - 본문 내용 (예: "저장 중 오류가 발생했습니다.")
 * @param {'error'|'warning'|'info'} icon
 * @returns {Promise}
 */
export const adminAlert = (title, html = '', icon = 'error') =>
  Swal.fire({
    title,
    html: html || undefined,
    icon,
    confirmButtonText: '확인',
  });

/* ─────────────────────────────────────────────
 * UI — Confirm
 * ───────────────────────────────────────────── */

/**
 * Confirm 다이얼로그 (중앙, gMessage confirm 역할)
 * - 확인 → true, 취소/외부클릭 → false
 * - Promise<boolean> 반환 (await 사용 가능)
 *
 * @param {string} title  - 굵은 제목 (예: "회사정보 저장")
 * @param {string} html   - 본문 내용 (예: "저장하시겠습니까?")
 * @returns {Promise<boolean>}
 */
export const adminConfirm = (title, html = '') =>
  Swal.fire({
    title,
    html: html || undefined,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: '확인',
    cancelButtonText: '취소',
  }).then((r) => r.isConfirmed);

/* ─────────────────────────────────────────────
 * 포맷터
 * ───────────────────────────────────────────── */

/**
 * 전화번호 하이픈 자동포맷 (common.js tel 핸들러 동일 로직)
 * - 02 시작: 최대 10자리, 02-XXXX-XXXX
 * - 그 외:  최대 11자리, 0XX-XXX(X)-XXXX
 *
 * @param {string} value - 입력 문자열 (숫자 외 포함 가능)
 * @returns {string}
 */
export const formatTel = (value) => {
  let digits = value.replace(/\D/g, '');

  if (digits.startsWith('02')) {
    digits = digits.slice(0, 10);
    const len = digits.length;
    if (len <= 2) return digits;
    if (len <= 5) return digits.slice(0, 2) + '-' + digits.slice(2);
    return digits.slice(0, 2) + '-' + digits.slice(2, len - 4) + '-' + digits.slice(len - 4);
  }

  digits = digits.slice(0, 11);
  const len = digits.length;
  if (len <= 3) return digits;
  if (len <= 6) return digits.slice(0, 3) + '-' + digits.slice(3);
  if (len <= 10) return digits.slice(0, 3) + '-' + digits.slice(3, len - 4) + '-' + digits.slice(len - 4);
  return digits.slice(0, 3) + '-' + digits.slice(3, 7) + '-' + digits.slice(7);
};

/**
 * 휴대폰번호 하이픈 자동포맷 (3-4-4)
 * - 최대 11자리, 010-XXXX-XXXX
 *
 * @param {string} value
 * @returns {string}
 */
export const formatHp = (value) => {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  const len = digits.length;
  if (len <= 3) return digits;
  if (len <= 7) return digits.slice(0, 3) + '-' + digits.slice(3);
  return digits.slice(0, 3) + '-' + digits.slice(3, 7) + '-' + digits.slice(7);
};

/**
 * 사업자등록번호 하이픈 자동포맷 (3-2-5)
 * - 최대 10자리, 000-00-00000
 *
 * @param {string} value
 * @returns {string}
 */
export const formatAgentNum = (value) => {
  const digits = value.replace(/\D/g, '').slice(0, 10);
  const len = digits.length;
  if (len <= 3) return digits;
  if (len <= 5) return digits.slice(0, 3) + '-' + digits.slice(3);
  return digits.slice(0, 3) + '-' + digits.slice(3, 5) + '-' + digits.slice(5);
};

/* ─────────────────────────────────────────────
 * 검증
 * ───────────────────────────────────────────── */

/**
 * 전화번호 형식 검증 (값이 있을 때만 사용)
 * 허용: 02-XXXX-XXXX, 0XX-XXX-XXXX, 0XX-XXXX-XXXX, 01X-XXX(X)-XXXX
 *
 * @param {string} v
 * @returns {boolean}
 */
export const validateTel = (v) =>
  /^(02-\d{3,4}-\d{4}|0[3-9]\d-\d{3,4}-\d{4}|01[0-9]-\d{3,4}-\d{4})$/.test(v);

/**
 * 이메일 형식 검증 (값이 있을 때만 사용)
 *
 * @param {string} v
 * @returns {boolean}
 */
export const validateEmail = (v) =>
  /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v);

/**
 * 사업자등록번호 형식 검증 (값이 있을 때만 사용)
 * 허용: 000-00-00000
 *
 * @param {string} v
 * @returns {boolean}
 */
export const validateAgentNum = (v) =>
  /^\d{3}-\d{2}-\d{5}$/.test(v);
