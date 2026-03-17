# SPA + Spring Security 세션 인증 가드 구현

> **작성 계기**: React Admin SPA(localhost:3000)가 Spring Security 인증 없이 접근 가능했던 이슈를 해결하면서,
> 하이브리드 아키텍처(SSR + SPA)에서의 인증 흐름과 설계 패턴을 정리함.

---

## 1. 이슈 배경 & 관련 배경지식

### 1-1. 프로젝트 아키텍처 구조

```
┌─────────────────────────────────────────────────────┐
│  브라우저                                             │
│                                                       │
│  localhost:3000 (React Admin SPA - Vite Dev Server)  │
│  localhost:8050 (Spring Boot - SSR + REST API)       │
└─────────────────────────────────────────────────────┘

개발 환경 요청 흐름:
  React → fetch('/api/...') → Vite 프록시 → Spring Boot(:8050)

운영 환경 요청 흐름:
  브라우저 → Nginx → /admin/* → React 빌드 결과물 (정적)
                   → /api/*   → Spring Boot
```

이 프로젝트는 **하이브리드(Hybrid) 아키텍처**다.

| 영역 | 렌더링 방식 | 인증 처리 |
|---|---|---|
| 일반 페이지 | Thymeleaf SSR | Spring Security 직접 제어 |
| Admin 페이지 | React SPA | **별도 처리 필요** |

---

### 1-2. Spring Security가 무엇을 보호하는가

Spring Security의 Filter Chain은 **Spring Boot가 서빙하는 요청(port:8050)** 에만 적용된다.

```
[요청] ──→ [Spring Security Filter Chain] ──→ [DispatcherServlet] ──→ [Controller]
           │
           ├─ 인증 확인
           ├─ 권한 확인
           └─ 미인증 시 → AuthenticationEntryPoint 실행
```

Thymeleaf SSR 페이지를 누군가 직접 `/dashboard` 같은 경로로 접근하면?
→ Spring Security가 가로채서 로그인 페이지로 보낸다. ✅

React SPA(localhost:3000)에 누군가 직접 접근하면?
→ **Vite Dev Server(Node.js)가 응답한다. Spring Security는 관여하지 않는다.** ❌

---

### 1-3. 세션 기반 인증 vs JWT 인증

두 방식의 차이를 이해하면 왜 프론트에서 서버에 물어봐야 하는지 알 수 있다.

#### 세션 기반 (이 프로젝트)
```
[로그인 성공]
  → 서버가 세션 생성 (메모리 또는 DB에 저장)
  → 브라우저에 JSESSIONID 쿠키 발급

[이후 모든 요청]
  → 브라우저가 쿠키 자동 첨부
  → 서버가 쿠키로 세션 조회 → 유효 여부 판단
```

**핵심**: 인증 상태가 **서버 메모리/DB에 존재**한다.
→ 프론트엔드가 "나 로그인 됐어" 여부를 자체적으로 알 수 없다.
→ **반드시 서버에 물어봐야 한다.**

#### JWT 기반
```
[로그인 성공]
  → 서버가 JWT(토큰) 생성 → 클라이언트에 전달
  → 브라우저가 localStorage 또는 쿠키에 저장

[이후 모든 요청]
  → 브라우저가 토큰을 Authorization 헤더에 첨부
  → 서버가 서명 검증만 수행 (DB 조회 불필요)
```

**핵심**: 인증 상태가 **토큰(클라이언트) 자체에 존재**한다.
→ 프론트엔드가 토큰 유무/만료 여부를 직접 확인할 수 있다.
→ 초기 서버 요청 없이 인증 상태 파악 가능.

> ✅ 이 프로젝트는 **세션 기반**이므로, 앱 초기화 시 서버 API를 호출해서 인증 상태를 확인해야 한다.

---

### 1-4. Vite 프록시의 역할

`vite.config.js`에 설정된 프록시:

```js
proxy: {
  "/api": {
    target: "http://localhost:8050",
    changeOrigin: true,
  },
},
```

이것이 하는 일:
- `localhost:3000/api/...` 요청을 → `localhost:8050/api/...`로 **전달(포워딩)**
- 브라우저 입장에서는 **모두 같은 origin(localhost:3000)** 으로 보임
- 따라서 CORS 문제가 발생하지 않음

그런데 Spring Security가 302 리다이렉트(`/member/login`)를 응답하면?
```
fetch('/api/admin/session')
  → Vite 프록시 → Spring Boot → 302 redirect to /member/login
  → 브라우저: fetch가 자동으로 리다이렉트 추적
  → GET localhost:3000/member/login (Vite가 응답 — 해당 경로 없음)
  → 404 또는 index.html(React 라우터)
  → res.ok = true (200)
  → JSON 파싱 실패 or 잘못된 응답 처리
```

이 때문에 **API 요청에는 302 대신 401 JSON을 반환**해야 프론트가 제대로 처리할 수 있다.

---

## 2. 기존 로직의 문제점

### 2-1. AuthenticationEntryPoint — "모든 미인증 요청 = 리다이렉트"

**변경 전:**
```java
@Bean
public AuthenticationEntryPoint unauthenticatedEntryPoint() {
    return (request, response, authException) -> {
        String redirectUrl = "/member/login" + ("/".equals(request.getRequestURI()) ? "" : "?required=-1");
        response.sendRedirect(redirectUrl);  // 항상 302 리다이렉트
    };
}
```

**문제점:**
| 요청 유형 | 기대 동작 | 실제 동작 |
|---|---|---|
| SSR 페이지 접근 | 302 → 로그인 페이지 | ✅ 정상 |
| REST API 호출 | 401 JSON | ❌ 302 리다이렉트 (HTML 응답으로 귀결) |

SSR 페이지에서는 302가 완벽하게 동작한다. 브라우저가 HTML을 렌더링하기 때문.
하지만 SPA의 `fetch()` 호출에 302를 보내면, 리다이렉트를 따라가서 HTML을 받고, `.json()` 파싱에 실패하거나 `res.ok = true`인 잘못된 상태가 된다.

---

### 2-2. AdminLayout — 인증 가드 없음

**변경 전:**
```jsx
export default function AdminLayout() {
  return (
    <div className="admin-shell">
      <Sidebar />
      <Topbar />
      <Outlet />  {/* 바로 렌더링 */}
    </div>
  );
}
```

**문제점:**
- 컴포넌트가 마운트되자마자 모든 하위 페이지가 렌더링된다.
- 세션 확인 로직이 없으므로 누구든 URL만 알면 UI를 볼 수 있다.
- API 데이터 로딩 실패(인증 오류)는 각 페이지가 개별적으로 처리해야 하는 구조 → 일관성 없음.

---

### 2-3. adminFetch — 401 처리 없음

**변경 전:**
```js
if (!res.ok) {
    const err = new Error(`HTTP ${res.status}`);
    err.status = res.status;
    // ... throw
}
```

**문제점:**
- 세션 만료 시 401이 오면 일반 에러와 동일하게 처리된다.
- 각 페이지 컴포넌트의 catch 블록이 "서버 오류"로 표시하거나 아무 처리도 안 할 수 있다.
- 사용자는 왜 데이터가 안 나오는지 알 수 없고, 로그인 페이지로 이동하지 않는다.

---

## 3. 개선 방향 & 해결 과정

### 3-1. 전체 해결 구조

```
[문제] 미인증 SPA 접근 허용
  ↓
[원인 1] AuthenticationEntryPoint가 API에도 302 리다이렉트 반환
[원인 2] AdminLayout에 인증 가드 없음
[원인 3] adminFetch가 401을 감지·처리하지 못함
  ↓
[해결]
  (백엔드) EntryPoint: /api/** → 401 JSON / 그 외 → 302 리다이렉트
  (백엔드) GET /api/admin/session 엔드포인트 추가
  (프론트) AdminLayout: 마운트 시 세션 확인, 미인증 → 리다이렉트
  (프론트) adminFetch: 401 감지 → 로그인 페이지로 전환
```

---

### 3-2. AuthenticationEntryPoint 개선

**변경 후:**
```java
@Bean
public AuthenticationEntryPoint unauthenticatedEntryPoint() {
    return (request, response, authException) -> {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            // API 요청 → 401 JSON
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":-1,\"msg\":\"인증이 필요합니다.\",\"data\":null}");
        } else {
            // 일반 페이지 → 302 리다이렉트
            String redirectUrl = "/member/login" + ("/".equals(uri) ? "" : "?required=-1");
            response.sendRedirect(redirectUrl);
        }
    };
}
```

**핵심 판단 기준**: 요청 URI가 `/api/`로 시작하는지 여부
→ REST API 클라이언트에게는 기계가 읽을 수 있는 JSON
→ 브라우저 페이지 요청에는 사람이 이해할 수 있는 리다이렉트

이것은 업계 표준 패턴이다. Spring Security 공식 문서에서도 "API 요청과 일반 요청을 구분하여 처리"할 것을 권장한다.

---

### 3-3. 세션 확인 엔드포인트 — GET /api/admin/session

```java
@GetMapping("/session")
public ResponseEntity<ResMessage<AdminDTO.SessionRes>> getSession(
        @AuthenticationPrincipal CustomUserDetails user
) {
    List<String> roles = user.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .toList();
    AdminDTO.SessionRes res = new AdminDTO.SessionRes(
            user.getId(), user.getUsername(), user.getName(), roles
    );
    return ResponseEntity.ok(new ResMessage<>(1, null, res));
}
```

**왜 `/api/admin/session`인가?**
- `/api/admin/**` 경로는 Security 설정상 GET은 인증된 사용자만, 쓰기는 ADMIN만 허용
- 미인증 → EntryPoint → 401 JSON → adminFetch 감지 → 로그인 이동
- 이 경로 하나로 "로그인 여부 확인" + "현재 유저 정보 반환" 두 역할을 동시에 수행

**`@AuthenticationPrincipal`의 동작:**
Spring Security가 SecurityContext에서 현재 인증된 사용자(`CustomUserDetails`)를 꺼내서
파라미터로 자동 주입한다. 별도의 세션 조회 코드가 필요 없다.

---

### 3-4. AdminLayout 인증 가드

```jsx
const [authStatus, setAuthStatus] = useState('checking');

useEffect(() => {
    adminFetch('/api/admin/session')
        .then(() => setAuthStatus('ok'))
        .catch(() => setAuthStatus('fail'));
}, []);

if (authStatus !== 'ok') return null;  // 확인 전엔 아무것도 렌더링 안 함
```

**상태 전이:**
```
'checking' (초기)
    → API 성공 → 'ok'    → 레이아웃 렌더링
    → API 실패 → 'fail'  → null 렌더링 (이미 리다이렉트 진행 중)
```

**왜 `return null`인가?**
`return null`은 React에서 아무것도 렌더링하지 않는 방법이다.
`'checking'` 동안 null을 반환해서 **대시보드가 잠깐이라도 보이는 현상(FOUC: Flash of Unstyled Content)** 을 방지한다.

---

### 3-5. adminFetch 401 전역 처리

```js
if (res.status === 401) {
    const loginUrl = (import.meta.env.VITE_BACKEND_ORIGIN || '') + '/member/login';
    window.location.href = loginUrl;
    throw new Error('Unauthorized');
}
```

**환경별 로그인 URL:**
| 환경 | `VITE_BACKEND_ORIGIN` | 결과 URL |
|---|---|---|
| 개발 (localhost:3000) | `http://localhost:8050` | `http://localhost:8050/member/login` |
| 운영 (동일 서버) | `` (빈 문자열) | `/member/login` (상대경로) |

`import.meta.env.VITE_BACKEND_ORIGIN`은 Vite의 환경변수 접근 방식이다.
`VITE_` 접두사가 붙은 변수만 클라이언트 코드에 노출된다 (보안 설계).

---

## 4. 추가로 알아두면 좋은 것들

### 4-1. FOUC(Flash of Unstyled/Unauth Content) 란?

인증 확인이 완료되기 전에 UI가 잠깐 보이는 현상.

```
[잘못된 구현]
  컴포넌트 마운트 → 대시보드 렌더링 → useEffect 실행 → 세션 확인 → 미인증 → 리다이렉트
                    ↑ 이 순간 대시보드가 보임 (보안 취약점 + UX 문제)

[올바른 구현]
  컴포넌트 마운트 → return null → 세션 확인 완료 → 렌더링 허용
                    ↑ 아무것도 안 보임
```

이 프로젝트에서는 `authStatus !== 'ok'`이면 `return null`로 처리해서 FOUC를 방지했다.
더 세련되게는 로딩 스피너를 보여줄 수도 있다:
```jsx
if (authStatus === 'checking') return <LoadingSpinner />;
if (authStatus !== 'ok') return null;
```

---

### 4-2. 세션 확인을 "언제" 하는가 — 빈도 전략

| 전략 | 방식 | 장점 | 단점 |
|---|---|---|---|
| **앱 초기화 시 1회** (이번 구현) | `AdminLayout` 마운트 때만 | 간단, 불필요한 요청 없음 | 세션 만료를 즉시 감지 못함 |
| 라우트 변경마다 | `useEffect([location])` | 빠른 감지 | API 요청 증가 |
| 주기적 폴링 | `setInterval` | 거의 실시간 감지 | 불필요한 트래픽 |
| **API 응답에서 감지** (추가 보완) | `adminFetch` 401 처리 | 실제 만료 시 즉시 처리 | 단독으로 사용 불가 |

**현재 구현은 "초기화 1회 + API 응답 감지" 조합**이다.
대부분의 실무 프로젝트에서 이 조합이 표준이다.

---

### 4-3. 운영 환경에서의 보안 강화 가능성

개발 환경(포트 분리)에서는 프론트엔드 가드가 필수지만,
운영 환경(Nginx를 통해 같은 서버에서 서빙)에서는 추가 방어선을 둘 수 있다.

```nginx
# 운영 Nginx 설정 예시
location /admin/ {
    # 세션 쿠키가 없으면 백엔드로 인증 확인 후 서빙
    auth_request /api/admin/session;
    error_page 401 = @login_redirect;

    try_files $uri $uri/ /admin/index.html;
}

location @login_redirect {
    return 302 /member/login;
}
```

이렇게 하면 React 앱 자체(index.html)를 서빙하기 전에 Nginx 레벨에서 인증을 확인한다.
현재 CALI 운영 환경이 이 구조가 아니더라도, 알아두면 좋은 패턴이다.

---

### 4-4. 현재 구현의 한계 & 우려사항

#### ① 토큰/쿠키 직접 탈취 시나리오
현재 구현은 **세션이 유효한지 서버에 확인**하는 방식이다.
만약 공격자가 `JSESSIONID` 쿠키를 탈취하면 (XSS, 네트워크 스니핑 등), 서버는 정상 세션으로 판단한다.

**대응책:**
- `Secure` 쿠키 속성 (HTTPS에서만 전송)
- `HttpOnly` 쿠키 속성 (JavaScript 접근 불가 → XSS 방어)
- Spring Security는 기본적으로 `HttpOnly`를 적용하므로 `Secure`는 운영 서버 HTTPS 설정으로 보완

#### ② CSRF(Cross-Site Request Forgery)
이 프로젝트는 `csrf(AbstractHttpConfigurer::disable)`로 CSRF를 비활성화했다.
SPA + REST API 구조에서는 일반적인 선택이지만, 아래 조건이 충족되어야 안전하다:
- 모든 API는 CORS 설정으로 허용된 Origin만 접근 가능해야 함
- 쿠키 `SameSite=Strict` 또는 `Lax` 설정으로 크로스사이트 요청 차단

현재 `SameSite` 설정은 별도 확인이 필요하다.

#### ③ 초기 세션 체크 실패 시 UX
현재 서버가 다운됐거나 네트워크 오류가 발생하면 `catch(() => setAuthStatus('fail'))`로 처리된다.
그런데 `adminFetch` 내부에서 401이 아닌 경우에는 리다이렉트가 발생하지 않는다.
→ 결과: **빈 화면**이 표시된다.

개선 가능한 코드:
```jsx
.catch((err) => {
    if (err.message === 'Unauthorized') {
        setAuthStatus('redirecting'); // 리다이렉트 진행 중
    } else {
        setAuthStatus('error'); // 서버 연결 오류
    }
});

if (authStatus === 'checking' || authStatus === 'redirecting') return null;
if (authStatus === 'error') return <div>서버에 연결할 수 없습니다. 잠시 후 다시 시도하세요.</div>;
```

당장 필요하진 않지만, UX 개선 시 고려할 사항이다.

#### ④ 세션 만료 시간과 Remember-Me
현재 `remember-me` 토큰 유효기간은 30일(`tokenValiditySeconds(60 * 60 * 24 * 30)`)이다.
일반 세션(remember-me 없이 로그인)은 브라우저 종료 시 만료된다.

사용자가 탭을 오래 열어두고 작업하다가 세션이 만료되는 경우:
- API 요청 시 401 → `adminFetch`에서 감지 → 로그인 페이지로 이동
- **현재 작업 중이던 내용은 유실**된다

실무에서는 이 경우 "세션이 만료되었습니다. 저장 후 다시 로그인해주세요." 같은 모달을 먼저 띄우고, 저장 후 로그인 페이지로 보내는 방식을 쓰기도 한다. 현재는 즉시 이동으로 처리.

---

### 4-5. 이 패턴이 적용되는 다른 상황들

동일한 패턴(초기 세션 확인 + 가드)은 다음 상황에도 동일하게 적용된다:

1. **권한에 따른 접근 제어** — 현재 가드를 확장하면, ADMIN 여부를 확인하고 비 ADMIN은 `/admin/` 자체를 막을 수 있다.
```jsx
.then((data) => {
    if (!data.data.roles.includes('ROLE_ADMIN')) {
        // ADMIN이 아니면 메인 페이지로
        window.location.href = (import.meta.env.VITE_BACKEND_ORIGIN || '') + '/';
        return;
    }
    setAuthStatus('ok');
})
```

2. **중첩 레이아웃의 각 단계 가드** — 예: SuperAdmin 전용 섹션이 추가될 경우, 해당 레이아웃에 동일 패턴 적용

3. **모바일 앱(React Native), Next.js 등** — 서버 인증 방식이 세션이라면 동일한 "초기화 시 인증 확인" 패턴이 필요하다.

---

## 5. 전체 흐름 다이어그램

```
[미인증 상태로 localhost:3000 접근]
         │
         ▼
  AdminLayout 마운트
         │
         ▼
  authStatus = 'checking'
  return null (아무것도 렌더링 안 함)
         │
         ▼
  useEffect: GET /api/admin/session
         │
    ┌────┴────┐
    │         │
  인증됨     미인증
    │         │
    │         ▼
    │   unauthenticatedEntryPoint
    │   /api/ 경로 감지
    │   401 JSON 반환
    │         │
    │         ▼
    │   adminFetch: res.status === 401
    │   window.location.href = 'http://localhost:8050/member/login'
    │   throw Error('Unauthorized')
    │         │
    │         ▼
    │   catch → setAuthStatus('fail')
    │   return null (이미 리다이렉트 중)
    │
    ▼
  setAuthStatus('ok')
  AdminLayout 정상 렌더링
  <Sidebar /> <Topbar /> <Outlet />


[작업 중 세션 만료]
         │
         ▼
  임의의 API 호출
         │
         ▼
  401 JSON 응답
         │
         ▼
  adminFetch: 401 감지
  window.location.href → 로그인 페이지
```

---

## 6. 핵심 요약

| 문제 | 원인 | 해결 |
|---|---|---|
| 미인증으로 Admin SPA 접근 가능 | Spring Security는 8050만 보호, SPA는 별도 서버 | AdminLayout에서 마운트 시 세션 확인 |
| API 401이 아닌 302 반환 | EntryPoint가 모든 요청에 리다이렉트 | URI 기반으로 API/페이지 분기 처리 |
| 세션 만료 시 UX 미처리 | adminFetch에 401 핸들러 없음 | 401 감지 → 로그인 페이지 이동 |
| 화면 깜빡임(FOUC) | 인증 확인 전 렌더링 | 'checking' 상태에서 return null |

**기억할 한 줄:**
> Spring Security는 서버 요청만 보호한다. SPA가 별도 서버로 분리된 경우, 프론트엔드 자체적인 인증 가드는 필수다.
