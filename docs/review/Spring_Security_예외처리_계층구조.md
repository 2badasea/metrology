# Spring Security 예외처리 계층 구조

> **질문 계기**: `AccessDeniedHandler`를 Bean으로 등록한 이유가 뭔지,
> `exceptions/` 디렉토리의 커스텀 예외 및 `GlobalApiExceptionHandler`와 어떤 관계인지 궁금했다.

---

## 1. 선행 지식

### Servlet Filter vs DispatcherServlet

Spring MVC 요청은 크게 두 단계를 거친다.

| 단계 | 구성 요소 | 역할 |
|---|---|---|
| **1단계** | Servlet Filter (FilterChain) | 요청이 Controller에 도달하기 **전** 처리 |
| **2단계** | DispatcherServlet → Controller → Service | 실제 비즈니스 로직 처리 |

Spring Security는 **1단계(Filter)**에서 동작한다.
`@RestControllerAdvice`는 **2단계(DispatcherServlet 이후)**에서만 예외를 감지할 수 있다.

### @RestControllerAdvice 동작 범위

```
Filter → DispatcherServlet → [Controller → Service → 예외 발생]
                                                          ↑
                                          @RestControllerAdvice가 여기서 잡음
```

Filter 단계에서 발생한 예외는 DispatcherServlet까지 오지 않으므로 **@RestControllerAdvice로 처리 불가**.

---

## 2. 요청 처리 흐름 전체 그림

```
HTTP 요청
    │
    ▼
┌──────────────────────────────────────┐
│       Security Filter Chain          │  ← Spring Security가 여기 있음
│                                      │
│  ① 인증(Authentication) 체크         │
│     └─ 미인증 → AuthenticationEntryPoint
│                  (로그인 페이지 리다이렉트)
│                                      │
│  ② 인가(Authorization) 체크          │
│     └─ 권한 부족 → AccessDeniedHandler
│                    (403 JSON 응답)   │
└──────────────────────────────────────┘
    │ (인증/인가 통과한 요청만 아래로)
    ▼
┌──────────────────────────────────────┐
│       DispatcherServlet              │
│   → Controller → Service → Repository
│                                      │
│   Service 내부에서 예외 throw 가능   │
└──────────────────────────────────────┘
    │ (예외 발생 시)
    ▼
┌──────────────────────────────────────┐
│   @RestControllerAdvice              │  ← GlobalApiExceptionHandler
│   (Controller 실행 이후 예외 처리)   │
│                                      │
│   ForbiddenAdminModifyException → 403│
│   EntityNotFoundException       → 404│
│   Exception (최종 방패)         → 500│
└──────────────────────────────────────┘
```

---

## 3. 각 계층의 역할과 사용 시점

### AccessDeniedHandler (Bean 등록)

- **발생 위치**: Security Filter 단계
- **발생 조건**: `hasRole()`, `hasAuthority()` 같은 URL 패턴 기반 권한 체크 실패
- **왜 Bean으로**: `@RestControllerAdvice`는 이 단계에 접근 불가 → 별도 핸들러 필수
- **이 프로젝트 설정**:
  ```java
  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
      return (request, response, accessDeniedException) -> {
          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
          response.setContentType("application/json;charset=UTF-8");
          response.getWriter().write("{\"code\":-1,\"msg\":\"권한이 없습니다.\",\"data\":null}");
      };
  }
  ```

### @RestControllerAdvice + 커스텀 예외

- **발생 위치**: Controller/Service 실행 중
- **발생 조건**: Service 내부에서 비즈니스 규칙 위반 시 직접 `throw`
- **`exceptions/` 디렉토리의 역할**: 의미 있는 상황을 명확하게 표현하기 위한 커스텀 예외 클래스 모음

**예시 — `ForbiddenAdminModifyException`:**
```java
// Service 내부
if (member.getAuth() == AuthType.admin) {
    // "권한 없음"이 아닌 "admin 계정은 수정 불가"라는 도메인 규칙
    throw new ForbiddenAdminModifyException("관리자 계정은 수정할 수 없습니다.");
}

// GlobalApiExceptionHandler가 catch → 403 응답
@ExceptionHandler(ForbiddenAdminModifyException.class)
public ResponseEntity<?> handleForbidden(ForbiddenAdminModifyException ex) {
    return ResponseEntity.status(403).body(new ResMessage<>(-1, ex.getMessage(), null));
}
```

---

## 4. 403이 발생하는 두 가지 경로 비교

| 구분 | 발생 조건 | 처리 방법 |
|---|---|---|
| **URL 패턴 권한 부족** | `hasRole("ADMIN")` 설정된 경로에 ROLE_USER 접근 | `AccessDeniedHandler` 빈 |
| **@PreAuthorize 실패** | 메서드에 `@PreAuthorize("hasRole('ADMIN')")` + 권한 없는 사용자 접근 | `@RestControllerAdvice`에서 `AccessDeniedException` 처리 가능 |
| **비즈니스 규칙 403** | Service에서 `throw new ForbiddenAdminModifyException(...)` | `@RestControllerAdvice` |

> **주의**: `@PreAuthorize`로 발생하는 `AccessDeniedException`은 DispatcherServlet 이후에 발생하므로
> `@RestControllerAdvice`가 잡을 수 **있다**. 이 경우 `GlobalApiExceptionHandler`에 핸들러를 추가하면 된다:
> ```java
> @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
> public ResponseEntity<ResMessage<Object>> handleAccessDenied(AccessDeniedException ex) {
>     return ResponseEntity.status(HttpStatus.FORBIDDEN)
>             .body(new ResMessage<>(-1, "권한이 없습니다.", null));
> }
> ```

---

## 5. 이 프로젝트의 전체 권한 체크 구조 요약

```
DELETE /api/**          → Security Filter → ROLE_ADMIN 필수
POST·PATCH·PUT /api/admin/** → Security Filter → ROLE_ADMIN 필수
  └─ 권한 부족 시 → AccessDeniedHandler → {"msg":"권한이 없습니다."}

@PreAuthorize 메서드    → DispatcherServlet 이후 → (향후 GlobalApiExceptionHandler에 추가 예정)

Service 내 도메인 규칙  → ForbiddenAdminModifyException → GlobalApiExceptionHandler → 403
```

---

## 6. 참고 — Spring Security 주요 인터페이스

| 인터페이스 | 역할 |
|---|---|
| `AuthenticationEntryPoint` | 미인증 요청 처리 (로그인 유도) |
| `AccessDeniedHandler` | 인증은 됐지만 권한 부족 처리 |
| `AuthenticationSuccessHandler` | 로그인 성공 후 처리 |
| `AuthenticationFailureHandler` | 로그인 실패 후 처리 |
| `UserDetailsService` | DB에서 사용자 정보 로딩 |

---

*작성일: 2026-03-12 | 관련 파일: `CustomSecurityConfig.java`, `GlobalApiExceptionHandler.java`, `exceptions/`*
