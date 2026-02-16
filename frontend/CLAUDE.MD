## 1) UI 기본 원칙

- UI는 Bootstrap 기반.
- 모든 리스트는 Toast UI Grid 사용.
- 등록/수정/삭제 전 최종 confirm 필수.
    - (비-Admin) `g_message` 등 공통 메시지 유틸 우선 사용
- 반응형 기본:
    - PC 최적화 우선
    - 태블릿/Safari/모바일 Chrome에서 “심하게 깨지지 않도록” 최소 대응

---

## 2) Admin(React) vs SSR(Thymeleaf/일반 페이지) 경계

- Admin은 React SPA(타입스크립트 X, Thymeleaf X).
- SSR/일반 페이지는 backend의 공통 로딩/레이아웃 규칙(common.js/common.css/fragment)을 존중합니다.

### 모달 전략

- SSR/일반: `g_modal` 사용
    - g_modal을 호출하는 경우, load() 메서드를 호출하기에 페이지 조회성이지만 컨트롤러 애너테이션은 @PostMapping 애너테이션 명시해서 사용.
- Admin React: React 친화 모달 사용하되 UX는 동일 수준 유지
    - 부드러운 오픈/클로즈(애니메이션)
    - 포커스 관리
    - 스크롤 락
- 기존 모달 계약을 깨는 변경은 사전 승인 필요.

---

## 3) 프론트 구조(관리자)

- base path: `/admin/` (Vite + Router 설정)
- Layout: `AdminLayout`(Sidebar + Topbar)
- Dev Proxy: `/api` 요청을 backend `localhost:8050`으로 프록시(실제 설정은 `vite.config.js` 확인)

---

## 4) API 호출 규칙

- Admin(React): axios 사용
    - 권장: axios instance + interceptor
        - 401/403/5xx 공통 처리
        - 로딩/토스트 정책
- 그 외 페이지: fetch 사용
- 라이브러리 요구(Toast Grid 조회 등) 제외하면 공통 호출 유틸을 우선 사용합니다.
- API 요청 또는 응답에 문제가 있는 경우 common.js의 gErrorHandler()를 활용해서 처리할 것 (API 요청 외 일반 입력값 검증에서 명시적으로 메시지와 함께 에러를 던지는 경우도 포함) 
- 아래는 사용 예시
    ```javascript
        // 입력검증
        try {
        if (pwd !== pwdConfirm) throw new Error('비밀번호 확인값과 일치하지 않습니다');
        } catch (e) {
        await gErrorHandler(e, { toastType: 'warning' });
        return false;
        }
        // fetch 요청
        try {
        const data = await g_fetch_json('/api/member/memberSave', { method: 'POST', body: formData });
        } catch (e) {
        await gErrorHandler(e);
        }
        // axios 요청
        try {
            await axios.post('/api/member/memberSave', formData);
            } catch (e) {
            await gErrorHandler(e);
            }
        // g_ajax(jQuery) 요청 -> 추후 gAjax로 함수명 변경 예정
        try {
        await g_ajax('/api/xxx', payload);
        } catch (e) {
        await gErrorHandler(e);
        }
    ```

---

## 5) 네이밍/마이그레이션 규칙

- 신규 코드는 camelCase 유지(스네이크 케이스 신규 금지).
- 기존 스네이크 케이스 정리는 단계적으로:
    - 전역 치환 금지

---

## 6) 입력값 검증/에러 처리

- 저장/수정은 try/catch로 감싸고 입력값 검증 포함.
- common.js의 검증 함수(예: `check_input`)와 같이 여러 화면에서 공통적으로 사용할 수 있는 함수를 재사용하거나 자주 사용될 것 같은 함수는 필요 시 정의해서 사용할 것
    - 공통함수에 대한 변경해야 할 사항이나 보완할 사항이 있다면 항상 확인받을 것
- 서버 에러는 사용자 친화 메시지로 표출(내부 정보/스택트레이스 노출 금지).

---

## 7) Toast UI Grid 규칙

- 리스트 화면은 Grid 중심으로 구성.
- 컬럼/정렬/필터/페이징은 “화면 요구사항 + 백엔드 계약”을 깨지 않게 유지.
- (비-Admin) 가능하면 `g_grid` 래퍼 우선 사용.
- (Admin React) 그리드 초기화/리사이즈/데이터 로딩은 컴포넌트 생명주기 기준으로 안정화.

---

## 8) 프론트 작업 완료 기준(DoD)

- 변경 파일 목록 + 핵심 변경 요약
- 화면 검증 체크리스트(클릭 경로/기대 결과)
- 오류 케이스 1~2개 확인 포인트(검증 실패, 401/403, 서버 에러 등)

## 9) 스크립트 파일 관련

- 스크립트 상단에 파일 경로를 명시하는 console.log() 부분은 수정대상 아님(불필요한 부분이라고 삭제 권유 X) ex) console.log('++ member/memberJoin.js');

## 10) 에러 처리 관련
- try/catch 문에서 입력값 검증의 경우 common.js의 gErrorHandler() 함수 사용
- api 요청에 대한 에러처리에 대해선 common.js의 gApiErrorHandler() 함수 사용
