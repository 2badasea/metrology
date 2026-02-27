# CALI(교정관리) - Claude Code 작업 규칙 (통합본)

> 이 문서는 이 저장소에서 Claude Code가 작업할 때 지켜야 하는 **공통 규칙 / 금지사항 / 완료 기준(DoD)** 을 한 곳에 통합한 문서입니다.  
> **Backend / Frontend(SSR, Admin React)** 모두가 서로 강하게 연관되어 있으므로, 작업 시 본 문서를 기준으로 일관되게 진행합니다.

---

## 0) 프로젝트 개요(아키텍처)

CALI는 하이브리드 구조의 교정관리 웹 애플리케이션입니다.

- **Backend**: Spring Boot 3.5.x (Java 17) + REST API + Thymeleaf SSR 페이지
- **Frontend(Admin)**: React SPA (Vite) / `/admin/` 경로에서 제공
- **Frontend(일반/SSR)**: Thymeleaf 렌더링 기반 + jQuery/순수 JS 보조
- **DB**: MySQL 8.x
- **파일 스토리지**: Naver Cloud Platform Object Storage (S3 호환, AWS SDK v2)

> 버전/포트/설정은 바뀔 수 있습니다. 애매하면 `backend/build.gradle`, `frontend/package.json`, `vite.config.js`를 먼저 확인합니다.

---

## 1) 빌드/실행 명령(기본)

### Backend

```bash
cd backend
./gradlew clean bootJar          # JAR 빌드
./gradlew bootRun                # 개발 서버 실행(기본: 8050)
./gradlew test                   # 테스트(JUnit 5)
```

### Frontend(Admin)

```bash
cd frontend
npm install
npm run dev                      # 개발 서버 실행(기본: 3000, /api -> :8050 프록시)
npm run build                    # dist/ 빌드
npm run lint                     # ESLint
```

---

## 2) 작업 범위 원칙(가장 중요)

- **요청받은 범위만 수정**합니다. "같이 고치면 좋아 보이는 것"은 **제안만** 하고 임의 리팩토링은 금지합니다.
- 변경은 가능한 작은 단위로 쪼개어 **diff가 과도하게 커지지 않게** 합니다.
- "UI만" 요청이면 **API/비즈니스/DB/연동 로직은 구현하지 않습니다**
  → UI 구조/레이아웃/Toast Grid 정의/이벤트 바인딩(핸들러 골격)까지만.

### 페이지/기능 검토 지시 시 전체 스택 점검 원칙

- 특정 페이지 또는 기능에 대한 **검토/점검**을 지시받으면, 해당 기능과 관련된 **프론트엔드 및 백엔드 로직 전체**를 직접 읽고 점검합니다.
  - **프론트엔드**: Thymeleaf HTML 템플릿, JS 파일, CSS 파일
  - **백엔드**: Entity, DTO, SSR Controller, REST API Controller, Service, Repository, Security(인증/인가 핸들러 포함)
- 탐색 에이전트 요약만 참고하는 것은 점검으로 인정하지 않습니다. **파일을 직접 읽은 것만** 점검한 것으로 간주합니다.
- 점검 후 수정 사항은 "즉시 수정 항목"과 "제안 사항(승인 필요)"으로 구분하여 보고합니다.

---

## 3) 사전 승인 없이는 절대 하지 말 것(금지/주의)

다음 항목은 반드시 사용자 승인 후 진행합니다.

- DB 스키마/DDL 변경(테이블/컬럼/인덱스/제약)
- 보안 정책 변경(인증/인가, CSRF, CORS, Security 설정의 큰 변경)
- 의존성 추가/버전 변경
    - backend: `build.gradle`
    - frontend: `package.json`
- 코어 동작/공통 계약 변경(전역 라우팅, 공통 레이아웃 규칙, 공통 유틸 함수 계약 변경 등)

필요해 보이면 먼저 아래 3가지를 요약해 승인받습니다.

1. 필요 이유
2. 대안
3. 영향 범위(백/프론트/DB/보안 포함)

---

## 4) Git 규칙(필수)

- `git commit`, `git push`, merge/rebase는 **절대 자동 실행 금지**.
- 작업 종료 시 항상 아래 항목을 제공합니다.
    - Changed files: 변경 파일 목록(경로)
    - Summary: 핵심 변경 요약(3~12줄)
    - How to verify: 실행/화면/간단 점검 방법
    - Notes/Risks: 주의점/추가 확인 포인트
    - 권장 커밋 메시지(제안만, 한글로 작성할 것)

---

## 5) 네이밍/코드 컨벤션(공통)

- 신규 코드/함수/변수는 **camelCase** 사용.
- 신규 스네이크 케이스(언더바 포함) 도입 금지.
- 기존 스네이크 케이스 정리는 **단계적 마이그레이션** 원칙
    - 전역 치환 금지
    - 호환 래퍼 제공 방식 권장 (예: `gAjax()`가 내부에서 기존 `g_ajax()` 호출)
- JavaScript 변수 선언은 **const 우선**, 필요 시 let 사용.

---

## 6) 보안/개인정보/품질(공통)

- 비밀정보(키/토큰/비번/내부 URL/IP)는 코드/로그/문서에 절대 하드코딩 금지.
- 사용자에게 스택트레이스/내부 정보 노출 금지.
- 변경 후 가능한 범위에서 **검증 방법**을 함께 제시합니다.
- CI/CD, 배포 경로, 버킷명, 서버 정보 등은 환경별로 달라질 수 있으므로 **민감값은 일반화**합니다.

---

## 7) 사용 언어/프레임워크 운영 규칙

- Java는 **17 기준**으로 작성합니다. (그 이상 버전 전용 문법 사용 금지)
- 프론트(일반/SSR)는 **jQuery 위주**, 필요 시 순수 JS 사용
    - 문법은 **ES6까지 허용**
- Admin 페이지는 **React + 순수 JS**, **TypeScript 사용 금지**
- ORM/조회는 **JPA + Querydsl 우선**
    - MyBatis는 사용자의 직접 요청이 있을 때만 사용

---

# Backend(Spring Boot) 규칙

## B1) 핵심 구조/주의사항

### (중요) Dual Controller 패턴

백엔드는 “같은 클래스명”을 서로 다른 패키지에 생성할 수 있습니다.  
컨트롤러 수정 시 **반드시 어떤 패키지인지 확인**합니다.

- `com.bada.cali.controller.*` : Thymeleaf SSR 페이지 컨트롤러(뷰 네임 반환)
- `com.bada.cali.api.*` : REST API 컨트롤러(JSON 반환)

### 패키지 구조(`com.bada.cali`)

- `entity/` : JPA 엔티티(도메인 모델)
- `repository/` : Spring Data JPA
    - `repository/projection/` : 목록/조회용 Projection 인터페이스
- `service/` : 비즈니스 로직(`*ServiceImpl.java`, 별도 인터페이스 없음)
- `dto/` : DTO(요청/응답)
- `mapper/` : MapStruct 매퍼
- `config/` : Security/스토리지/예외 처리 등
- `common/enums/` : 업무 enum

### DB 변경 관련

- DB 스키마는 `docs/db/schema.sql` 을 단일 기준(source of truth)으로 참고합니다.
- DDL 변경은 **사전 승인 없이는 금지**. 필요하면 “제안”까지만 합니다.

---

## B2) REST API 규칙

- 조회: GET
- 등록: POST
- 수정: PATCH(부분 수정 우선) / PUT(전체 교체일 때만)
- 삭제: DELETE
- 등록/수정을 하나의 엔드포인트에서 분기 처리하고 있다면 **REST 규칙에 맞게 분리**를 제안합니다.

### URI 규칙(권장)

- 복수형 자원: `/api/.../members`
- 단건: `/api/.../members/{id}`
- 하위 자원: `/api/.../members/{id}/roles`

### 상태코드(권장)

- POST 생성: 201 (+ 필요 시 Location)
- DELETE 성공: 204 또는 200(프로젝트 표준에 따름)
- 검증 실패: 400 또는 422(프로젝트 표준에 따름)
- 인증/인가: 401/403

---

## B3) Swagger(OpenAPI) 규칙

- Swagger 문서화는 **REST API 컨트롤러(`com.bada.cali.api.*`) 중심**으로 작성합니다.
- 최소 권장 애너테이션
    - Controller 메서드: `@Operation`(요약/설명) + `@ApiResponses`(응답코드)
    - DTO 필드: `@Schema`
    - 파라미터: `@Parameter`(필요 시)
- **`@ApiResponses` 작성 전 반드시 아래 두 가지를 직접 확인합니다.**
    1. `GlobalApiExceptionHandler` — 예외 → 상태코드 변환 패턴
    2. 해당 엔드포인트의 서비스 내부 로직 — 실제로 던질 수 있는 예외 종류
- **이 프로젝트의 `@ApiResponses` 명시 기준** (`GlobalApiExceptionHandler` 분석 결과)
    - `500`: 모든 엔드포인트에 항상 포함 (최종 방패 `Exception.class` → 500)
    - `400`: `@Valid` 파라미터가 있거나 `@RequestBody`를 받는 엔드포인트에만 포함
    - `404`: 서비스에서 `EntityNotFoundException`을 throw하는 경우에만 포함
    - `403`: 서비스에서 `ForbiddenAdminModifyException`을 throw하는 경우에만 포함
    - `401`: 명시 안 함 (미인증 접근 시 Security 필터가 로그인 페이지로 리다이렉트 처리, JSON 응답 아님)
    - ※ `IllegalArgumentException`은 전용 핸들러 없어 500으로 처리됨 (400으로 착각 주의)
    - ※ DELETE의 ADMIN 권한 체크는 Security 필터 레벨에서 처리되며 `ResMessage` 형식이 아니므로 `@ApiResponses` 대상 아님
- Swagger UI 접근 정책(운영 노출 여부)은 **임의로 변경하지 않습니다**(사전 승인).
- description 부분을 작성할 때는 서술형/문어체가 보통 단답/명료하게 작성할 것
  - ex) "Spring Security 필터가 요청을 가로채어 처리하므로 이 핸들러는 실행되지 않습니다." => "Spring Security 필터가 요청을 가로채어 처리하므로 이 핸들러는 실행되지 않음"

---

## B4) 트랜잭션 원칙

- 조회 로직은 기본적으로 `@Transactional(readOnly=true)`을 사용
- 등록/수정/삭제는 `@Transactional` 사용.
- Lazy 이슈는 projection/fetch join/DTO 매핑으로 회피하는 것을 우선합니다.
- 트랜잭션 경계는 **Service 레이어**에 둡니다(Controller 남발 금지).

---

## B5) 등록/수정/삭제 로그 정책

- 등록/수정/삭제는 로그를 남깁니다.
- 저장소: `log` 테이블 (스키마 변경은 사전 승인)
- `refTableId`
    - 등록: 생성된 ID
    - 수정: 수정된 ID
    - 삭제: 삭제된 ID
- `contents`
    - 고유번호 목록 포함 예: `고유번호 - [1, 11, 14]`
- 민감정보/개인정보/토큰/비번은 contents에 절대 기록 금지.
- 대량 처리 병목 개선이 필요하면 **개선안 제안까지만**(비동기/배치 구조 변경은 사전 승인).

---

## B6) DTO / Entity 분리 규칙

- DTO와 Entity는 명확히 분리합니다.
- DTO 구성(권장)
    - 엔티티별 대표 DTO: `MemberDTO`
    - 요청/응답: Inner class
        - `MemberDTO.MemberCreateReq`
        - `MemberDTO.MemberUpdateReq`
        - `MemberDTO.MemberDetailRes`
- 간단한 요청/응답은 `record` 사용 가능.
- 목록 조회 응답은 Projection 인터페이스 활용
    - 위치: `repository/projection`
    - 네이밍: `{Domain}{UseCase}Row` (예: `MemberListRow`)
- Toast Grid 전용 응답 DTO는 필요 시 별도 분리하되, 과도한 계층/상속은 지양합니다.

---

## B7) 검증/에러 처리

- 요청 DTO는 Bean Validation + Controller에서 `@Valid`.
- 에러 응답 포맷은 가능한 한 **표준화**합니다.
- 401/403은 프론트에서 처리 가능한 형태(코드/메시지)로 내려줍니다.
- 예외는 무시하지 말고 “의미 있는 메시지 + 로그”로 처리합니다(스택트레이스 노출 금지).
- 전역예외처리 @RestControllerAdvice, @ControllerAdvice 를 활용하여 예외를 처리하여 적절한 응답코드와 메시지를 ResMessage를 활용하여 응답할 수 있도록 할 것

---

## B8) 보안 관련 메모(임의 변경 금지)

- 로그인 페이지: `/member/login`
- 로그인 처리 URL: `/api/member/login` (Security 필터에서 처리, 컨트롤러 아님)
- 공개 경로/권한/DELETE 제한 등은 `SecurityConfig`를 확인한 뒤 작업합니다.
- 보안 정책 변경은 **사전 승인 없이는 금지**합니다.

---

## B9) 타임리프 기반 화면(SSR)

- Bootstrap 기반으로 반응형 동작을 우선합니다.
- Thymeleaf 레이아웃 구조를 최대한 활용합니다.
- 구조 개선이 필요하면 “제안” 후 승인받고 진행합니다.

---

# Frontend 규칙(SSR/일반 + Admin React)

## F1) UI 기본 원칙

- UI는 Bootstrap 기반.
- 모든 리스트는 Toast UI Grid 사용.
- 등록/수정/삭제 전 **최종 confirm 필수** (`g_message` 등 공통 유틸 우선)
- 반응형 기본
    - PC 최적화 우선
    - 태블릿/Safari/모바일 Chrome에서 “심하게 깨지지 않도록” 최소 대응

---

## F2) Admin(React) vs SSR(Thymeleaf/일반 페이지) 경계

- Admin은 React SPA(타입스크립트 X, Thymeleaf X).
- SSR/일반 페이지는 backend의 공통 로딩/레이아웃 규칙(`common.js/common.css/fragment`)을 존중합니다.

### 모달 전략

- SSR/일반: `g_modal` 사용(기존 계약 유지)
    - 기존 모달 계약을 깨는 변경은 사전 승인 필요.
- Admin React: React 친화 모달 사용(UX 수준 유지)
    - 부드러운 오픈/클로즈(애니메이션)
    - 포커스 관리
    - 스크롤 락

> 모달 관련 매핑 방식(GET/POST 등)은 프로젝트 기존 흐름을 우선 따르고, 변경이 필요하면 사전 승인 후 진행합니다.

---

## F3) 프론트 구조(관리자)

- base path: `/admin/` (Vite + Router 설정)
- Layout: `AdminLayout`(Sidebar + Topbar)
- Dev Proxy: `/api` 요청을 backend `localhost:8050`으로 프록시 (`vite.config.js` 확인)

---

## F4) API 호출 규칙

- Admin(React): axios 사용 권장
    - 가능하면 axios instance + interceptor로 공통 처리(401/403/5xx, 로딩/메시지 정책)
- SSR/일반 페이지: fetch 또는 `g_ajax` 사용
- 공통 호출 유틸이 있으면 우선 사용하되, 계약 변경은 사전 승인.

### 에러 처리(중요: 함수 분리 운영)

- 입력값 검증(로컬) 에러: **`gErrorHandler()`** 사용 → `g_toast` 기반
- API 요청/응답 에러: **`gApiErrorHandler()`** 사용 → `g_message` 기반
- fetch를 직접 사용할 때는, **HTTP 에러를 catch로 보내기 위해** `if (!res.ok) throw res;` 패턴을 적용합니다.

---

## F5) 입력값 검증/에러 처리

- 저장/수정은 try/catch로 감싸고 입력값 검증 포함.
- `common.js`의 검증 함수(예: `check_input`)는 재사용을 우선합니다.
- 공통함수 계약 변경(시그니처/동작 변경)은 항상 사전 승인.

---

## F6) Toast UI Grid 규칙

- 리스트 화면은 Grid 중심으로 구성.
- 컬럼/정렬/필터/페이징은 “화면 요구사항 + 백엔드 계약”을 깨지 않게 유지.
- (비-Admin) 가능하면 `g_grid` 래퍼 우선 사용.
- (Admin React) 그리드 초기화/리사이즈/데이터 로딩은 컴포넌트 생명주기 기준으로 안정화.

---

## F7) 스크립트 파일 관련

- 스크립트 상단에 파일 경로를 출력하는 `console.log()` 는 **수정 대상 아님**
    - 불필요하다고 삭제 권유 금지
    - 예: `console.log('++ member/memberJoin.js');`

---

## F8) 프론트 작업 완료 기준(DoD)

- Changed files + 핵심 변경 요약
- 화면 검증 체크리스트(클릭 경로/기대 결과)
- 오류 케이스 1~2개 확인 포인트(검증 실패, 401/403, 서버 에러 등)

---

# 8) 작업 완료 기준(DoD) - 공통 템플릿

모든 작업 응답은 마지막에 반드시 아래를 포함합니다.

- **Changed files**: (경로 목록)
- **Summary**: (핵심 변경 3~7줄)
- **How to verify**
    - (backend) 빌드/실행/간단 점검
    - (frontend) 실행/화면 점검
- **Notes/Risks**: (주의점/추가 확인 필요 사항)

---

# 9) 작업 단위 완료 후 학습 메모 규칙

단위 작업이 완료되고 **테스트까지 통과된 시점**에 아래를 수행합니다.

### 메모 작성 대상 (`/docs/memoByClaude.md`)
- 권장 커밋 메시지
- CS / Java / Spring / 이슈 관련 **알아두면 도움이 될 만한 내용** (너무 단순한 내용은 생략)
    - 예: 프레임워크 동작 원리, 버전별 API 차이, 설계 패턴, 트러블슈팅 과정
- 필요 시 예시 소스코드 포함 (markdown 코드블록)
- 내용은 **시간 순으로 누적** (오래된 내용 위, 신규 내용 아래)
- 각 항목에 **학습 레벨 태그** 표시: `[기초]` / `[중급]` / `[고급]`
- 각 항목 끝에 **유지보수 포인트** 섹션 포함
    - 이 코드를 나중에 수정해야 할 때 어디를 봐야 하는지
    - 버전 업그레이드·조건 변경 시 영향 받는 부분

### 오류 발생 시 처리
- 테스트 중 오류가 발생하면 메모 작성을 **보류**합니다.
- 문제 해결 및 최종 수정이 완료된 시점에 메모를 작성합니다.
- 최초 시도에서 잘못된 내용이 있었다면 **수정된 내용 기준**으로 작성합니다.

### 복잡한 구현 시 인라인 주석 강화 규칙
- 일반적으로 주석은 최소화하지만, **동작 원리를 코드만으로 파악하기 어려운 부분**은 예외입니다.
- 복잡한 블록에는 **"왜 이 구조인지"** 를 설명하는 주석을 추가합니다.
    - 단순 "무엇" 설명이 아닌, 설계 의도·대안 대비 이유를 간결하게 기록
- 판단 기준: 코드 관리자가 6개월 후 이 코드를 보고 수정 포인트를 바로 찾을 수 있는가

---

# 통합 과정에서 정리/수정한 사항과 이유

1. **Swagger(OpenAPI) 규칙을 “REST API 중심”으로 명확화**
    - 기존 문구에는 `@Controller/@RestController` 모두 Swagger 사용 전제로 읽힐 여지가 있었습니다.
    - 통합본에서는 **`com.bada.cali.api.*`(JSON 반환) 중심으로 문서화**하도록 정리했습니다.  
      **이유:** Swagger/OpenAPI는 일반적으로 “API 계약(클라이언트가 호출하는 JSON 엔드포인트)”을 문서화하는 목적이 강하고, SSR 화면 라우팅까지 포함하면 문서가 과밀해져 유지보수성이 떨어집니다.

2. **프론트 에러 핸들러 규칙을 최신 합의(함수 분리)로 정리**
    - 통합본에서는 **입력검증은 `gErrorHandler()`**, **API 요청/응답은 `gApiErrorHandler()`**로 구분하도록 명확히 정리했습니다.  
      **이유:** 최근 대화에서 “단일 함수로 자동 판별” 방식이 복잡해진다고 판단되어, **역할 분리(토스트 vs 메시지)** 로 단순하고 안정적인 운영을 선택했기 때문입니다.

3. **모달 매핑(GET/POST) 관련 문장은 “기존 계약 우선 + 변경 시 승인”으로 완화**
    - 기존 문구 중 모달 로딩 방식과 매핑 애너테이션(POST) 관련 내용은 프로젝트 사정에 따라 달라질 수 있어, 통합본에서는 “기존 흐름 우선”으로 표현을 정리했습니다.  
      **이유:** 실제 구현(예: `.load()`는 기본 GET)과의 괴리가 있을 수 있어, 문서를 원인으로 잘못된 강제 변경이 발생하는 것을 방지하기 위해서입니다.

---
