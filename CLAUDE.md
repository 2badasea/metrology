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
  - **런타임 설정**: `application.properties` — multipart 크기 제한, DB 설정 등 런타임 동작에 영향을 주는 설정값 반드시 확인
  - **예외처리 커버리지**: `GlobalApiExceptionHandler` — 해당 기능에서 발생 가능한 예외가 모두 핸들링되는지 확인. 컨트롤러 실행 이전(예: multipart 파싱 단계)에 던져지는 예외는 `@RestControllerAdvice`가 잡지 못할 수 있으므로 특히 주의
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

**빈 이름 충돌 방지 규칙**
두 패키지에 같은 클래스명이 존재하면 Spring이 빈 이름 충돌 오류를 냅니다.
`api/` 패키지 컨트롤러를 생성할 때는 반드시 `@RestController(“ApiXxxController”)` 형식으로 빈 이름을 명시합니다.

```java
// api 패키지 — 빈 이름 명시 필수
@RestController(“ApiMemberController”)   // ✅
@RestController                          // ❌ controller 패키지와 이름 충돌 가능

// controller 패키지 — 기본 이름 사용 (변경 금지)
@Controller                              // ✅ 빈 이름 = 클래스명 camelCase
```

> `EnvController`처럼 `controller/` 패키지에 동명 클래스가 없으면 빈 이름 명시는 선택이지만,
> 신규 생성 시에는 **일관성을 위해 항상 명시**합니다.

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

- DB 스키마는 **하이브리드 방식**으로 관리합니다.
    - `docs/db/schema.sql` — **단일 정오답(Full Schema)**. 항상 최신 전체 스키마 상태를 유지. 새 환경 셋업 시 이 파일만 실행하면 됨.
    - `docs/db/versions/v_YYMMDD.sql` — **델타(변경분만)**. 해당 버전에서 추가/변경된 DDL만 포함. 히스토리 추적 용도.
- 스키마 변경 작업 시 두 곳 모두 업데이트합니다: `schema.sql` 최신화 + `versions/` 에 델타 파일 추가.
- DDL 변경은 **사전 승인 없이는 금지**. 필요하면 “제안”까지만 합니다.
- **작업 완료 전 반드시 확인**: 해당 작업에서 테이블/컬럼 추가·변경이 발생했다면,
  DoD의 “DB schema” 항목에 따라 두 파일을 최신화한 뒤 작업 완료로 간주합니다.

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

## B8) Admin 전용 API 권한 규칙

관리자 페이지(React Admin SPA)에서 호출하는 **등록·수정·삭제 API**는 반드시 `ROLE_ADMIN` 권한이 있어야 동작해야 한다.

### 기본 원칙

- **조회(GET)**: 인증된 사용자 전체 허용 (ADMIN 한정 불필요)
- **등록(POST) · 수정(PATCH/PUT) · 삭제(DELETE)**: `ROLE_ADMIN` 필수

### 처리 방식 (두 계층 병행)

**1. `/api/admin/**` 경로 → Security 필터에서 자동 처리 (권장)**

```
/api/admin/**  →  POST·PATCH·PUT·DELETE 모두 ROLE_ADMIN 강제
               →  GET은 인증된 사용자 전체 허용
```

- Admin 전용 신규 API 엔드포인트는 **반드시 `/api/admin/**` 경로**로 정의할 것
- 별도 애너테이션 없이 Security 필터에서 자동으로 권한 체크됨

**2. `/api/admin/**` 외 경로에서 Admin 전용 write op → `@PreAuthorize` 명시**

```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<?> create(...) { ... }
```

- 기존 컨트롤러(비-admin 경로)에 admin 전용 쓰기 기능이 추가될 경우 사용
- 메서드 단위로 명확하게 표시되므로 가독성 유지

### 적용 현황 (Security 필터 기준)

| HTTP 메서드 | 경로 | 권한 |
|---|---|---|
| DELETE | `/api/**` | ROLE_ADMIN |
| POST · PATCH · PUT | `/api/admin/**` | ROLE_ADMIN |
| GET | 모든 경로 | 인증된 사용자 |

### 주의사항

- Security 필터 규칙 변경은 **사전 승인 없이 금지** (B9 참조)
- `/api/admin/**` 경로를 추가할 때마다 Security 설정을 별도로 수정할 필요 없음 (이미 일괄 적용됨)
- `@PreAuthorize` 누락 시 비 ADMIN 유저가 write 가능해지므로 코드 리뷰 시 반드시 확인

---

## B9) 보안 관련 메모(임의 변경 금지)

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

## B10) 서버 배포 고려사항 (신규 코드 작성 시 필수 체크)

코드에서 아래 항목이 추가/변경되면, **DoD의 “Server deployment” 항목에 반드시 명시**합니다.

### 서버 디렉토리 생성 필요
- 새로운 파일 경로(`app.temp.dir`, 로그 경로 등)를 도입할 경우, 해당 디렉토리가 서버에 존재해야 함
- 서비스 실행 유저(`www-data`)가 쓰기 권한을 가져야 함
- 생성 명령 예시:
  ```bash
  sudo mkdir -p /opt/cali/{디렉토리}
  sudo chown www-data:www-data /opt/cali/{디렉토리}
  ```

### 외부 설정 파일 추가 필요 (`application.properties` 계열)
- `application.properties`에 새 `app.*` 프로퍼티를 추가할 경우:
  - **기본값이 있으면**: `application.properties`에 기본값 명시 → 서버 작업 불필요
  - **기본값이 없거나 서버마다 다른 값**이면: 서버의 외부 설정 파일에 추가 필요
    - 개발서버: `/opt/cali/application-dev.properties`
    - 운영서버: `/opt/cali/application-prod.properties` (또는 동일 경로의 환경별 파일)
- 외부 설정 파일 위치 (Spring Boot 우선순위 높은 순):
  ```
  /opt/cali/config/application-{profile}.properties
  /opt/cali/application-{profile}.properties   ← 주로 사용
  /opt/cali/application.properties
  JAR 내부 (번들)                               ← 최하위
  ```

### 민감값 처리 원칙
- DB 접속정보, API 키, 시크릿 등은 **git에 커밋하지 않음**
- 서버의 외부 설정 파일에만 존재해야 하며, 해당 파일은 `.gitignore` 대상
- 코드에 기본값으로 노출되어서는 안 됨

### GitHub Actions 워크플로우 연동
- 새 디렉토리나 외부 설정이 필요하면, **`deploy-dev.yml`의 “Deploy + restart” 스텝에 디렉토리 생성 명령도 함께 추가**할 것
  ```yaml
  sudo mkdir -p /opt/cali/{새디렉토리}
  sudo chown www-data:www-data /opt/cali/{새디렉토리}
  ```

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

- Admin(React): `fetch` 기반 `adminFetch()` 사용
    - 위치: `frontend/src/utils/adminCommon.js`
    - async/await + ES6+ 문법 기반
    - 공통 함수 추가가 필요하면 `adminCommon.js`에 추가하되, **사용자 제안 및 승인 후 진행**
- SSR/일반 페이지: fetch 또는 `g_ajax` 사용
- 공통 호출 유틸이 있으면 우선 사용하되, 계약 변경은 사전 승인.

### Admin(React) 공통 함수 목록 (`adminCommon.js`)

위치: `frontend/src/utils/adminCommon.js`
의존 패키지: `sweetalert2`, `react-toastify` (ToastContainer는 `main.jsx`에 전역 등록)

**HTTP**

| 함수 | 설명 |
|---|---|
| `adminFetch(url, options)` | fetch 래퍼. JSON 기본값, 에러 throw, 응답 JSON 자동 파싱 |

**UI — 토스트 (react-toastify, 우측 상단, 3초 자동 소멸)**

| 함수 | 용도 |
|---|---|
| `adminToast(msg, type)` | 입력값 검증 실패, 간단 안내. type: `'info'`/`'success'`/`'error'`/`'warning'` |

**UI — SweetAlert2 (화면 중앙)**

| 함수 | 설명 |
|---|---|
| `adminLoading(title)` | 로딩 스피너 (gLoadingMessage 역할). `adminCloseLoading()`으로 닫음 |
| `adminCloseLoading()` | 로딩 닫기 |
| `adminSuccess(title, html)` | 성공 메시지. 확인 버튼 + 3초 타이머 자동 닫힘 |
| `adminAlert(title, html, icon)` | 오류·안내 메시지. 확인 클릭 시 닫힘. icon 기본값: `'error'` |
| `adminConfirm(title, html)` | 확인/취소 다이얼로그. `Promise<boolean>` 반환 |

- `title`: 굵은 제목 (예: `"회사정보 저장"`)
- `html`: 본문 내용 (예: `"저장하시겠습니까?"`) — 긴 문장은 여기에 두어 개행 문제 방지

**포맷터 / 검증**

| 함수 | 설명 |
|---|---|
| `formatTel(value)` | 전화번호 하이픈 자동 포맷 (02 / 기타 지역 / 010) |
| `formatHp(value)` | 휴대폰번호 하이픈 자동 포맷 (3-4-4) |
| `formatAgentNum(value)` | 사업자등록번호 하이픈 자동 포맷 (3-2-5) |
| `validateTel(v)` | 전화번호 형식 검증 |
| `validateEmail(v)` | 이메일 형식 검증 |
| `validateAgentNum(v)` | 사업자등록번호 형식 검증 |

### Admin(React) 코드 규칙

- `common.js`(jQuery 기반)는 React 환경에서 사용 불가 — 재사용 금지
- `gAjax`, `gToast`, `gErrorHandler` 등은 React에서 사용 불가 → `adminCommon.js` 함수로 대체
- 스크립트 문법은 **ES6+ 기반** (const/let, arrow function, async/await, template literal 등)

### 에러 처리(중요: 함수 분리 운영)

**SSR/일반 페이지**
- 입력값 검증(로컬) 에러: **`gErrorHandler()`** 사용 → `g_toast` 기반
- API 요청/응답 에러: **`gApiErrorHandler()`** 사용 → `g_message` 기반

**Admin(React) 페이지**
- 입력값 검증 / 간단 안내: **`adminToast()`** → react-toastify, 우측 상단
- API 로딩 중: **`adminLoading()`** → SweetAlert2, 중앙 스피너
- API 성공: **`adminSuccess(title, html)`** → SweetAlert2, 중앙
- API 오류: **`adminAlert(title, html)`** → SweetAlert2, 중앙
- 저장/삭제 전 확인: **`adminConfirm(title, html)`** → SweetAlert2, 중앙

**공통**
- fetch를 직접 사용할 때는, **HTTP 에러를 catch로 보내기 위해** `if (!res.ok) throw res;` 패턴을 적용합니다.
- multipart(파일 업로드) 전송 시 `Content-Type` 헤더를 지정하지 않아야 브라우저가 boundary를 자동 포함합니다. `adminFetch` 대신 `fetch` 직접 사용.

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

## 9) 학습 문서 정리 요청 (`docs/review/`)

사용자가 특정 내용을 `docs/review/`에 문서로 정리해달라고 요청하면 아래 기준으로 작성한다.

1. **파일 위치**: `docs/review/` 하위
2. **파일명**: 한글로, 주제를 잘 나타내는 이름 (예: `Spring_Security_예외처리_계층구조.md`)
3. **형식**: Markdown (`.md`)
4. **내용 구성**:
   - 질문 계기 한 줄 요약 (상단)
   - 선행 지식 / 배경 개념
   - 핵심 설명 (다이어그램·표 적극 활용)
   - 이 프로젝트에서의 실제 적용 예시
   - 참고 지식 / 관련 인터페이스·개념 정리
5. **분량**: 너무 길지 않게, 핵심 위주로 작성

---

## 10) 작업 진행 방식

1. **단위별 분리**: 변경이 여러 파일에 걸칠 경우, 파일 단위로 Edit 도구를 호출한다.
   Edit 도구 호출 시 IDE에서 diff 뷰(좌우 분할)가 열리고 사용자가 approve/deny 버튼으로 직접 승인한다.
   사용자가 deny 하거나 별도 지시가 없으면 다음 파일로 넘어가지 않는다.
2. **주석**: 구현한 코드에는 로직의 흐름, 파라미터 의도, 주의사항 등 충분한 주석을 남긴다.
3. **피드백**: 작업 요청에 대해 효율성·유지보수성·표준 방식 측면에서 개선 의견이 있으면
   작업 전 또는 완료 후 사용자에게 제안한다. (임의 적용 금지, 제안만)
4. **CSS 분리 원칙**: 스타일은 가급적 별도 CSS 파일로 분리한다.
   - 간단한 인라인 `style=""` 속성(width, display 등 단순값)은 허용
   - `<style>` 태그를 HTML 파일 내부에 직접 작성하는 것은 **금지**
   - CSS가 필요한 경우 페이지 전용 CSS 파일을 생성한다
     - 경로 규칙: Thymeleaf 템플릿 경로와 동일하게 `static/css/` 하위에 위치
     - 예: `templates/cali/reportWrite.html` → `static/css/cali/reportWrite.css`
   - CSS 파일은 해당 HTML에서 `<link>` 또는 레이아웃 fragment를 통해 로드한다

---

## 8) 작업 완료 기준(DoD) - 공통 템플릿

모든 작업 응답은 마지막에 반드시 아래를 포함합니다.

- **Changed files**: (경로 목록)
- **Summary**: (핵심 변경 3~7줄)
- **How to verify**
    - (backend) 빌드/실행/간단 점검
    - (frontend) 실행/화면 점검
- **DB schema**: 이번 작업에서 테이블/컬럼 추가·변경이 있었다면 아래 두 파일 최신화 여부 확인
    - `docs/db/schema.sql` — 전체 스키마 반영
    - `docs/db/versions/v_YYMMDD.sql` — 델타(변경분만) 파일 추가
    - 변경 없으면 "없음"으로 명시
- **Server deployment**: 서버에서 별도 작업이 필요한 항목 명시 (B10 기준)
    - 새로 생성해야 할 디렉토리 및 권한 설정 명령
    - 외부 설정 파일(`/opt/cali/application-{profile}.properties`)에 추가해야 할 프로퍼티
    - GitHub Actions 워크플로우(`deploy-dev.yml` / `deploy-prod.yml`) 수정 필요 여부
    - 해당 없으면 "없음"으로 명시
- **Notes/Risks**: (주의점/추가 확인 필요 사항)

