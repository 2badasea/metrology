# CALI(교정관리) - Codex 작업 규칙 (AGENTS.md)

> 이 문서는 CALI 저장소에서 **OpenAI Codex CLI**가 작업할 때 따라야 하는 공통 규칙/금지사항/완료 기준(DoD)
> Backend / Frontend(SSR, Admin React) 모두 강하게 연관되어 있으므로, 본 문서 기준으로 일관되게 작업합니다.

---

## 0) Project overview (architecture)

CALI는 하이브리드 구조의 교정관리 웹 애플리케이션입니다.

- **Backend**: Spring Boot 3.5.x (Java 17) + REST API + Thymeleaf SSR
- **Frontend(Admin)**: React SPA (Vite) `/admin/`
- **Frontend(SSR/일반)**: Thymeleaf + jQuery/순수 JS 보조
- **DB**: MySQL 8.x
- **Object Storage**: Naver Cloud Platform Object Storage (S3 compatible, AWS SDK v2)

> 버전/포트/설정이 애매하면 `backend/build.gradle`, `frontend/package.json`, `vite.config.js`를 먼저 확인합니다.

---

## 1) Setup / build / run commands

### Backend
```bash
cd backend
./gradlew clean bootJar          # JAR build
./gradlew bootRun                # dev server (default: 8050)
./gradlew test                   # tests (JUnit 5)
```

### Frontend (Admin)
```bash
cd frontend
npm install
npm run dev                      # dev server (default: 3000, /api -> :8050 proxy)
npm run build                    # build dist/
npm run lint                     # ESLint
```

---

## 2) Scope & change policy (MOST IMPORTANT)

- **요청받은 범위만 수정**합니다.
- “같이 고치면 좋아 보이는 것”은 **제안만** 하고, **무단 리팩토링 금지**.
- 변경은 작은 단위로 쪼개어 **diff가 과도하게 커지지 않게** 합니다.
- 사용자가 “UI만” 요청한 경우:
  - **API/비즈니스/DB/연동 로직 구현 금지**
  - UI 구조/레이아웃/Toast Grid 정의/이벤트 바인딩(핸들러 골격)까지만.

### Page/feature review 요청 시 점검 원칙
- 특정 페이지/기능에 대한 “검토/점검” 지시 시, 관련된 **전체 스택 파일을 직접 읽고** 점검합니다.
  - **Frontend**: Thymeleaf HTML, JS, CSS
  - **Backend**: Entity, DTO, SSR Controller, REST API Controller, Service, Repository, Security(인증/인가 핸들러 포함)
- “요약/추정”만으로 점검했다고 말하지 말 것. **직접 읽은 파일만** 점검으로 인정합니다.
- 점검 후 수정 사항은 아래로 구분하여 보고합니다.
  - **즉시 수정 항목**
  - **제안 사항(승인 필요)**

---

## 3) Requires explicit approval (DO NOT DO WITHOUT ASKING)

아래 항목은 **반드시 사전 승인 후** 진행합니다.

- DB 스키마/DDL 변경(테이블/컬럼/인덱스/제약)
- 보안 정책 변경(인증/인가, CSRF, CORS, Security 설정의 큰 변경)
- 의존성 추가/버전 변경
  - backend: `build.gradle`
  - frontend: `package.json`
- 코어 동작/공통 계약 변경
  - 전역 라우팅
  - 공통 레이아웃 규칙
  - 공통 유틸 함수 계약 변경 등

승인이 필요하다고 판단되면, 진행 전 아래 3가지를 먼저 요약 보고합니다.
1) 필요 이유  
2) 대안  
3) 영향 범위(백/프론트/DB/보안 포함)

---

## 4) Git rules (MANDATORY)

- `git commit`, `git push`, merge/rebase는 **절대 자동 실행 금지**.
- 작업 종료 시 항상 아래를 제공합니다.
  - **Changed files**: 변경 파일 목록(경로)
  - **Summary**: 핵심 변경 요약(3~7줄)
  - **How to verify**: 실행/화면/간단 점검 방법
  - **Notes/Risks**: 주의점/추가 확인 포인트
  - **Recommended commit message**: 제안만(실행 금지)

> 구현/수정 후에는 가능하면 `git diff`를 기준으로 변경 내용을 검증합니다.

---

## 5) Naming & code conventions (common)

- 신규 코드/함수/변수: **camelCase** 사용.
- 신규 스네이크 케이스(언더바 포함) 도입 금지.
- 기존 스네이크 케이스 정리는 **단계적 마이그레이션** 원칙
  - 전역 치환 금지
  - 호환 래퍼 제공 방식 권장  
    - 예: `gAjax()`가 내부에서 기존 `g_ajax()` 호출
- JavaScript 변수 선언: **const 우선**, 필요 시 let.

---

## 6) Security / privacy / quality (common)

- 비밀정보(키/토큰/비번/내부 URL/IP) 하드코딩 금지(코드/로그/문서 포함).
- 사용자에게 스택트레이스/내부 정보 노출 금지.
- 변경 후 가능한 범위에서 **검증 방법**을 함께 제시합니다.
- CI/CD, 배포 경로, 버킷명, 서버 정보 등은 환경별로 달라질 수 있으므로 **민감값은 일반화**합니다.

---

## 7) Language / framework rules

- Java: **17 기준** (상위 버전 전용 문법 금지)
- Front(SSR/일반): **jQuery 위주**, 필요 시 순수 JS (ES6까지)
- Admin: **React + 순수 JS**, TypeScript 금지
- ORM/조회: **JPA + Querydsl 우선**
  - MyBatis는 사용자의 직접 요청이 있을 때만 사용

---

# Backend (Spring Boot) rules

## B1) Structure / key notes

### Dual Controller pattern (IMPORTANT)
컨트롤러 수정 시 **패키지 확인 필수**.

- `com.bada.cali.controller.*` : Thymeleaf SSR controller (view name)
- `com.bada.cali.api.*` : REST API controller (JSON)

### Package layout (`com.bada.cali`)
- `entity/` : JPA entities
- `repository/` : Spring Data JPA
  - `repository/projection/` : projection interfaces
- `service/` : business logic (`*ServiceImpl.java`, interface 없음)
- `dto/` : request/response DTO
- `mapper/` : MapStruct
- `config/` : security/storage/exception handling
- `common/enums/` : business enums

### DB changes
- 스키마 단일 기준: `docs/db/schema.sql`
- DDL 변경은 사전 승인 없이는 금지. 필요하면 “제안”까지만.

---

## B2) REST API rules

- 조회: GET
- 등록: POST
- 수정: PATCH(부분 수정 우선) / PUT(전체 교체일 때만)
- 삭제: DELETE
- 등록/수정을 하나의 엔드포인트에서 분기 처리 중이면 **REST 규칙에 맞게 분리**를 제안합니다.

### URI conventions
- collection: `/api/.../members`
- item: `/api/.../members/{id}`
- sub-resource: `/api/.../members/{id}/roles`

### Status codes (recommended)
- POST create: 201 (+ optional Location)
- DELETE success: 204 or 200 (project standard)
- validation fail: 400 or 422 (project standard)
- auth/authz: 401/403

---

## B3) Swagger (OpenAPI) rules

- Swagger 문서화는 **REST API 컨트롤러(`com.bada.cali.api.*`) 중심**으로 작성합니다.
- Minimum recommended annotations
  - Controller methods: `@Operation` + `@ApiResponses`
  - DTO fields: `@Schema`
  - Params: `@Parameter` (when needed)
- `GlobalApiExceptionHandler` 표준 에러 응답 코드(400/403/404/500)는 Swagger `@ApiResponses`에 포함합니다.
- Swagger UI 접근 정책(운영 노출 여부)은 임의 변경 금지(사전 승인).

### Description style (VERY IMPORTANT)
- 서술형/문어체 금지. **단답/명료**하게 작성.
- 금지 표현: `~합니다`, `~됩니다`, `~하므로`, 긴 원인 설명.
- 예시
  - ✅ `실행되지 않음`
  - ✅ `Security 필터 처리`
  - ✅ `인증 필요`
  - ❌ `Spring Security 필터가 요청을 가로채어 처리하므로 이 핸들러는 실행되지 않습니다.`
  - ✅ `실행되지 않음 - Security 필터`

---

## B4) Transaction boundary

- 조회: 기본적으로 `@Transactional(readOnly = true)`
- 등록/수정/삭제: `@Transactional`
- Lazy 이슈: projection/fetch join/DTO mapping 우선으로 회피
- 트랜잭션 경계: **Service 레이어** (Controller 남발 금지)

---

## B5) Create/Update/Delete logging policy

- 등록/수정/삭제는 로그 남김
- 저장소: `log` 테이블 (스키마 변경은 사전 승인)
- `refTableId`
  - 등록: 생성된 ID
  - 수정: 수정된 ID
  - 삭제: 삭제된 ID
- `contents`
  - 고유번호 목록 포함 예: `고유번호 - [1, 11, 14]`
- 민감정보/개인정보/토큰/비번은 `contents`에 기록 금지.
- 대량 처리/비동기/배치 구조 변경은 **제안까지만** (사전 승인 필요)

---

## B6) DTO / Entity separation

- DTO와 Entity는 명확히 분리.
- DTO 구성(권장)
  - 엔티티별 대표 DTO: `MemberDTO`
  - 요청/응답: Inner class
    - `MemberDTO.MemberCreateReq`
    - `MemberDTO.MemberUpdateReq`
    - `MemberDTO.MemberDetailRes`
- 간단한 요청/응답: `record` 사용 가능
- 목록 조회 응답: Projection interface
  - 위치: `repository/projection`
  - 네이밍: `{Domain}{UseCase}Row` (예: `MemberListRow`)
- Toast Grid 전용 응답 DTO는 필요 시 별도 분리(과도한 계층/상속 지양)

---

## B7) Validation & error handling

- 요청 DTO: Bean Validation + Controller에서 `@Valid`
- 에러 응답 포맷은 가능한 한 **표준화**
  - 프로젝트 표준 응답 래퍼: `ResMessage` (계약 변경은 사전 승인 필요)
- 401/403: 프론트에서 처리 가능한 형태(코드/메시지)로 제공
- 예외는 무시하지 말고 “의미 있는 메시지 + 로그”로 처리
  - 스택트레이스 노출 금지
- 전역 예외 처리:
  - `@RestControllerAdvice`, `@ControllerAdvice`를 활용
  - 예외를 적절한 응답코드/메시지로 매핑
  - `ResMessage`를 활용하여 응답할 수 있도록 구성

---

## B8) Security notes (DO NOT CHANGE WITHOUT APPROVAL)

- 로그인 페이지: `/member/login`
- 로그인 처리 URL: `/api/member/login` (Security 필터에서 처리, 컨트롤러 아님)
- 공개 경로/권한/DELETE 제한 등은 `SecurityConfig` 확인 후 작업
- 보안 정책 변경은 사전 승인 없이는 금지

---

## B9) Thymeleaf SSR pages

- Bootstrap 기반 반응형 우선
- Thymeleaf 레이아웃 구조 최대 활용
- 구조 개선 필요 시 “제안 → 승인 후 진행”

---

# Frontend rules (SSR + Admin React)

## F1) UI basics

- UI: Bootstrap
- 모든 리스트: Toast UI Grid
- 등록/수정/삭제 전 **최종 confirm 필수**
  - 공통 유틸(`g_message` 등) 우선 사용
- 반응형 기본
  - PC 최적화 우선
  - 태블릿/Safari/모바일 Chrome에서 “심하게 깨지지 않도록” 최소 대응

---

## F2) Admin(React) vs SSR boundary

- Admin: React SPA (TypeScript X, Thymeleaf X)
- SSR/일반: backend 공통 로딩/레이아웃 규칙(`common.js/common.css/fragment`) 존중

### Modal strategy
- SSR/일반: `g_modal` 사용(기존 계약 유지)
  - 모달 계약 변경은 사전 승인 필요
- Admin React: React 친화 모달(UX 수준 유지)
  - 부드러운 open/close
  - focus 관리
  - scroll lock

> 모달 매핑(GET/POST 등)은 기존 흐름 우선. 변경 필요 시 사전 승인 후 진행.

---

## F3) Admin structure

- base path: `/admin/` (Vite + Router)
- layout: `AdminLayout` (Sidebar + Topbar)
- dev proxy: `/api` -> backend `localhost:8050` (고정) (`vite.config.js`)

---

## F4) API calling rules

- Admin(React): axios 권장
  - 가능하면 axios instance + interceptor로 공통 처리(401/403/5xx, loading/message)
- SSR/일반: fetch 또는 `g_ajax`
- 공통 호출 유틸이 있으면 우선 사용(계약 변경은 사전 승인)

### Error handling split (IMPORTANT)
- 입력값 검증(로컬) 에러: **`gErrorHandler()`** (toast 기반)
- API 요청/응답 에러: **`gApiErrorHandler()`** (message 기반)
- fetch 직접 사용 시: `if (!res.ok) throw res;` 패턴 적용

---

## F5) Input validation & save flow

- 저장/수정은 try/catch + 입력값 검증 포함
- `common.js` 검증 함수(예: `check_input`) 재사용 우선
- 공통함수 계약 변경(시그니처/동작) = 사전 승인 필요

---

## F6) Toast UI Grid rules

- 리스트 화면은 Grid 중심
- 컬럼/정렬/필터/페이징은 요구사항 + 백엔드 계약을 깨지 않게 유지
- (SSR/일반) 가능하면 `g_grid` 래퍼 우선
- (Admin React) grid init/resize/data loading은 lifecycle 기준 안정화

---

## F7) Script log policy

- 스크립트 상단의 파일 경로 출력 `console.log()`는 **수정 대상 아님**
  - 삭제/정리 제안 금지
  - 예: `console.log('++ member/memberJoin.js');`

---

## F8) Front DoD

- Changed files + 핵심 변경 요약
- 화면 검증 체크리스트(클릭 경로/기대 결과)
- 오류 케이스 1~2개 확인 포인트(검증 실패, 401/403, 서버 에러 등)

---

# 8) Work completion template (DoD)

모든 작업 응답은 마지막에 반드시 아래를 포함합니다.

- **Changed files**: (path list)
- **Summary**: (3~10 lines)
- **How to verify**
  - (backend) build/run/basic checks
  - (frontend) run/ui checks
- **Notes/Risks**: (risks / follow-ups)
- **Recommended commit message**: suggestion only
