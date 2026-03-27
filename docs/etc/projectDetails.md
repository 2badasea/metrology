# CALI 프로젝트 상세 설명서

> 계측기기 교정 전문 업체를 위한 업무 관리 시스템 (사이드 프로젝트)
> 작성일: 2026-03-26

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택 & 버전](#2-기술-스택--버전)
3. [시스템 아키텍처](#3-시스템-아키텍처)
4. [디렉토리 구조](#4-디렉토리-구조)
5. [Spring Security 설정 (인증/인가)](#5-spring-security-설정-인증인가)
6. [핵심 설계 패턴](#6-핵심-설계-패턴)
7. [REST API 설계](#7-rest-api-설계)
8. [예외 처리 구조](#8-예외-처리-구조)
9. [데이터베이스 설계](#9-데이터베이스-설계)
10. [React Admin + Spring Boot 연동](#10-react-admin--spring-boot-연동)
11. [파일 업로드 / Object Storage 연동](#11-파일-업로드--object-storage-연동)
12. [성적서 작업서버(cali-worker) 연동](#12-성적서-작업서버cali-worker-연동)
13. [주요 설정 파일 분석](#13-주요-설정-파일-분석)
14. [주요 이슈 & 해결 방식 (상세)](#14-주요-이슈--해결-방식-상세)
15. [구현 기능 현황](#15-구현-기능-현황)
16. [면접 예상 질문 & 모범 답변](#16-면접-예상-질문--모범-답변)

---

## 1. 프로젝트 개요

**CALI**는 계측기기 교정 전문 업체의 업무 흐름을 디지털화한 웹 기반 업무 관리 시스템이다.
교정 접수 → 성적서 작성 → 결재 → 파일 발행까지의 전 흐름을 처리한다.

### 1.1 핵심 특징

- **하이브리드 구조**: 일반 업무 화면은 Thymeleaf SSR, 관리자 화면은 React SPA로 구분
- **단일 Spring Boot 서버**가 SSR 렌더링과 REST API를 모두 담당
- **외부 작업 서버(cali-worker)** 와 HTTP 콜백 구조로 Excel 성적서 자동 생성 연동
- **Naver Cloud Platform** 인프라 기반 (서버 + Object Storage)
- 채용담당자/면접관 시연 환경: 개발 서버에서 주요 기능까지 접근 가능

### 1.2 비즈니스 도메인 (교정이란?)

계측기기(측정 장비)의 정확도를 국가 표준과 비교·확인하는 작업.
교정 업체는 고객사로부터 기기를 접수하여 교정을 수행하고, 그 결과를 **성적서(교정 성적서)** 형태로 발행한다.
CALI는 이 흐름에서 발생하는 접수, 샘플(기기) 관리, 성적서 작성, 파일 관리, 업체/담당자 정보 관리를 처리한다.

---

## 2. 기술 스택 & 버전

### 2.1 Backend

| 항목 | 버전 / 설명 |
|------|------------|
| Java | 17 (LTS) |
| Spring Boot | 3.5.7 |
| Spring Security | 6.x (Spring Boot 3.x 내장) |
| Spring Data JPA | Hibernate 6.x |
| Querydsl | JPA 조회 최적화 |
| Thymeleaf | SSR 템플릿 엔진 |
| MapStruct | 1.6.3 (Entity ↔ DTO 매핑) |
| Springdoc OpenAPI | 2.8.3 (Swagger UI) |
| Apache POI | 5.3.0 (교정신청서 Excel 처리) |
| AWS SDK v2 | 2.27.21 (NCP Object Storage S3 호환) |
| Lombok | 코드 간결화 (@Getter, @Builder 등) |
| Gradle | 빌드 도구 |
| MySQL Connector | 8.0.33 |

### 2.2 Frontend (Admin SPA)

| 항목 | 버전 / 설명 |
|------|------------|
| React | 18.3.1 |
| React Router DOM | 7.10.1 (BrowserRouter, basename="/admin/") |
| Vite | 7.2.4 (빌드 도구, dev proxy) |
| Toast UI Grid | 4.21.22 (리스트 테이블) |
| sweetalert2 | 11.26.22 (모달, Confirm, 로딩) |
| react-toastify | 11.0.5 (토스트 메시지) |
| TypeScript | 사용 안 함 (순수 JS) |

### 2.3 Infra / DevOps

| 항목 | 내용 |
|------|------|
| 서버 | Naver Cloud Platform (micro 인스턴스) |
| 웹서버 | Nginx (리버스 프록시) |
| WAS | Spring Boot 내장 Tomcat (포트 8050) |
| DB | MySQL 8.x (cafe24 가상호스트) |
| 파일 스토리지 | Naver Object Storage (S3 호환 API) |
| CI/CD | GitHub Actions (`deploy-dev.yml`, `deploy-prod.yml`) |
| 배포 방식 | SSH → JAR 배포 → systemd 재시작 |

---

## 3. 시스템 아키텍처

### 3.1 전체 구성도

```
[브라우저]
    │
    ▼
[Nginx (리버스 프록시)]
    │
    ├─ /admin/**      → Spring Boot (React SPA 정적 파일 서빙)
    ├─ /api/**        → Spring Boot REST API
    └─ /** (그 외)    → Spring Boot Thymeleaf SSR

[Spring Boot :8050]
    │
    ├─ Thymeleaf 렌더링 (SSR 페이지)
    ├─ REST API (React Admin 포함)
    ├─ Spring Security (인증/인가/필터)
    ├─ JPA + MySQL (비즈니스 데이터)
    └─ AWS SDK v2 → Naver Object Storage (첨부파일)

[cali-worker (별도 서버)]
    │
    └─ HTTP 콜백 구조로 성적서 Excel 자동 생성
       → cali-worker가 작업 완료 시 CALI 콜백 URL로 결과 전송
```

### 3.2 개발 환경 vs 운영 환경 차이

| 항목 | 개발 (로컬/dev) | 운영 (prod) |
|------|----------------|------------|
| React 경로 | `localhost:3000/admin/` (Vite dev server) | `/admin/` (빌드 후 Spring Boot 서빙) |
| Object Storage 경로 | `cali-dev/` | `cali/` |
| DB | 개발 DB (공유) | 운영 DB (별도) |
| cali-worker | `app.worker.url` 비워두면 데모 모드 | 실제 연동 |

### 3.3 요청 흐름 (SSR 페이지)

```
브라우저 → Nginx → Spring Boot
  → Spring Security 필터 (인증 체크, 메뉴 접근 제어)
  → DispatcherServlet
  → GlobalViewNameAdvice (공통 모델 세팅 - 사이드메뉴, 유저정보)
  → SSR Controller (@Controller) → 뷰 이름 반환
  → Thymeleaf 렌더링 → HTML 응답
```

### 3.4 요청 흐름 (React Admin API 호출)

```
React (브라우저) → adminFetch('/api/...')
  → [개발] Vite proxy → :8050
  → [운영] 직접 동일 origin
  → Spring Security 필터 (JWT 없음, 쿠키 세션 기반)
  → @RestController → Service → Repository → DB
  → JSON 응답 (ResMessage 형식)
```

---

## 4. 디렉토리 구조

```
cali/
├── backend/                              # Spring Boot 서버
│   ├── src/main/java/com/bada/cali/
│   │   ├── api/                          # REST API 컨트롤러 (@RestController)
│   │   │   ├── MemberController.java     # @RestController("ApiMemberController")
│   │   │   ├── ReportController.java
│   │   │   ├── BusinessTripController.java
│   │   │   └── ...
│   │   ├── controller/                   # SSR 페이지 컨트롤러 (@Controller)
│   │   │   ├── MemberController.java     # @Controller (SSR 전용)
│   │   │   ├── CaliOrderController.java
│   │   │   └── ...
│   │   ├── entity/                       # JPA 엔티티
│   │   │   ├── Member.java
│   │   │   ├── BusinessTrip.java
│   │   │   ├── Report.java
│   │   │   └── ...
│   │   ├── repository/                   # Spring Data JPA Repository
│   │   │   ├── MemberRepository.java
│   │   │   ├── BusinessTripRepository.java
│   │   │   └── projection/               # Projection 인터페이스
│   │   │       ├── BusinessTripListRow.java
│   │   │       └── ...
│   │   ├── service/                      # 비즈니스 로직
│   │   │   ├── MemberServiceImpl.java    # 인터페이스 없이 Impl 직접 사용
│   │   │   ├── BusinessTripServiceImpl.java
│   │   │   └── ...
│   │   ├── dto/                          # 요청/응답 DTO
│   │   │   ├── BusinessTripDTO.java      # Inner class 방식
│   │   │   └── ...
│   │   ├── mapper/                       # MapStruct 매퍼
│   │   ├── security/                     # 인증/인가 관련
│   │   │   ├── CustomUserDetails.java    # UserDetails 구현체 (Serializable)
│   │   │   ├── CustomUserDetailService.java
│   │   │   ├── LoginSuccessHandler.java
│   │   │   ├── LoginFailureHandler.java
│   │   │   └── MenuAccessInterceptor.java
│   │   ├── config/                       # 설정 클래스
│   │   │   ├── CustomSecurityConfig.java # Spring Security 설정
│   │   │   ├── GlobalApiExceptionHandler.java
│   │   │   ├── GlobalViewExceptionHandler.java
│   │   │   ├── GlobalViewNameAdvice.java
│   │   │   ├── WebConfig.java
│   │   │   ├── CacheConfig.java
│   │   │   ├── NcloudS3Config.java
│   │   │   └── AsyncConfig.java
│   │   ├── common/
│   │   │   ├── ResMessage.java           # 공통 API 응답 래퍼
│   │   │   ├── Utils.java
│   │   │   └── enums/                    # 업무 Enum (AuthType, YnType 등)
│   │   └── exceptions/                   # 커스텀 예외 클래스
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── templates/                    # Thymeleaf 템플릿
│   │   │   ├── cali/                     # 주요 업무 화면
│   │   │   ├── basic/                    # 기본정보 화면
│   │   │   ├── agent/                    # 업체 관리 화면
│   │   │   └── fragments/                # 공통 레이아웃 fragment
│   │   └── static/
│   │       ├── css/                      # 페이지별 CSS
│   │       ├── js/                       # 페이지별 JS (jQuery 기반)
│   │       └── vendor/                   # 외부 라이브러리 (CDN 비의존)
│   └── build.gradle
│
├── frontend/                             # React Admin SPA
│   ├── src/
│   │   ├── pages/                        # React 페이지 컴포넌트
│   │   │   ├── CompanyInfo.jsx
│   │   │   ├── Notices.jsx
│   │   │   ├── MenuPermissions.jsx
│   │   │   └── Dashboard.jsx
│   │   ├── components/
│   │   │   ├── Sidebar.jsx
│   │   │   └── Topbar.jsx
│   │   ├── layouts/
│   │   │   └── AdminLayout.jsx
│   │   ├── utils/
│   │   │   └── adminCommon.js            # fetch 래퍼 + UI 함수 모음
│   │   ├── App.jsx                       # 라우팅 설정
│   │   └── main.jsx                      # Vite 엔트리
│   ├── vite.config.js
│   └── package.json
│
├── docs/
│   ├── db/
│   │   ├── schema.sql                    # Full Schema (유일한 진실)
│   │   └── versions/                     # 델타 파일 (v_YYMMDD.sql)
│   ├── review/                           # 학습/기술 분석 문서
│   └── etc/                              # 기타 문서 (이 파일 포함)
│
├── .github/workflows/
│   ├── deploy-dev.yml
│   └── deploy-prod.yml
│
├── CLAUDE.md                             # 작업 규칙 (AI 협업용)
└── README.md
```

---

## 5. Spring Security 설정 (인증/인가)

### 5.1 SecurityConfig 핵심 구조

`CustomSecurityConfig.java`가 `SecurityFilterChain`을 빈으로 등록하여 전체 보안 정책을 정의한다.

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)  // @PreAuthorize 활성화
public class CustomSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)  // REST API + SPA 환경에서 CSRF 비활성화

            .authorizeHttpRequests(auth -> auth
                // DELETE: 전체 /api/** → ADMIN만 허용
                .requestMatchers(DELETE, "/api/**").hasRole("ADMIN")
                // POST/PATCH/PUT: /api/admin/** → ADMIN만 허용
                .requestMatchers(POST,  "/api/admin/**").hasRole("ADMIN")
                .requestMatchers(PATCH, "/api/admin/**").hasRole("ADMIN")
                .requestMatchers(PUT,   "/api/admin/**").hasRole("ADMIN")
                // 공개 경로 (인증 불필요)
                .requestMatchers(
                    "/member/login", "/member/memberJoin",
                    "/api/member/**",           // 로그인·회원가입 API
                    "/swagger-ui/**", "/v3/api-docs/**",
                    "/actuator/health",         // CI/CD 헬스체크
                    "/api/worker/**"            // cali-worker 내부 콜백
                ).permitAll()
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/member/login")
                .loginProcessingUrl("/api/member/login")  // Security 필터가 가로챔 (컨트롤러 없음)
                .successHandler(LoginSuccessHandler)
                .failureHandler(LoginFailureHandler)
            )

            .rememberMe(rm -> rm
                .key("cali0803")
                .tokenRepository(persistentTokenRepository())  // DB에 토큰 저장
                .tokenValiditySeconds(60 * 60 * 24 * 30)      // 30일
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((req, res, auth) -> {
                    // JSON 응답 (React가 감지하여 이동)
                    res.getWriter().write("{\"ok\":true,\"redirect\":\"/member/login?logout\"}");
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(unauthenticatedEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            );

        return http.build();
    }
}
```

### 5.2 인증 흐름 (로그인)

```
POST /api/member/login (Security 필터가 가로챔)
  │
  ▼
CustomUserDetailService.loadUserByUsername(loginId)
  │   ├─ member 테이블에서 사용자 조회
  │   ├─ is_active = 'N' → "관리자 승인 필요" 예외
  │   ├─ 로그인 제한 중이면 ("10분 후 재시도") 예외
  │   └─ member_permission_read 에서 readableMenuIds 로드
  │
  ▼
CustomUserDetails 생성 (id, username, password, name, authorities, readableMenuIds, lastPwdUpdated)
  │
  ▼
비밀번호 검증 (BCrypt)
  │
  ├─ 실패 → LoginFailureHandler (실패 횟수 증가, DB 업데이트)
  └─ 성공 → LoginSuccessHandler
              ├─ 로그인 횟수/시각 업데이트
              ├─ lastPwdUpdated 기준 90일 초과 시 비밀번호 변경 권고 응답
              ├─ ADMIN이면 redirectUrl = "/admin/" 포함
              └─ JSON 응답 (ResMessage 형식)
```

### 5.3 CustomUserDetails 설계

```java
public class CustomUserDetails implements UserDetails, Serializable {
    private final Long id;                      // PK
    private final String username;              // 로그인 ID
    private final String password;             // BCrypt 해시
    private final String name;                 // 실명
    private final Collection<? extends GrantedAuthority> authorities;  // [ROLE_USER] or [ROLE_ADMIN]
    private final Set<Long> readableMenuIds;   // 접근 가능한 메뉴 ID 집합
    private final LocalDateTime lastPwdUpdated;  // 비밀번호 마지막 변경일
}
```

**`Serializable` 구현 이유**: Spring Session이 HTTP 세션 객체를 직렬화하여 저장하기 때문. Remember-Me 쿠키 기반 재인증 시에도 역직렬화가 필요하므로 반드시 구현해야 한다.

### 5.4 메뉴 접근 제어 (MenuAccessInterceptor)

Security 필터 이후 별도 **Spring MVC 인터셉터**가 메뉴 단위 접근 제어를 수행한다.

```
요청 URI
  │
  ▼
MenuAccessInterceptor.preHandle()
  ├─ 메뉴 테이블에 해당 URL이 없는 경우 → 통과 (서브페이지, 폼 등)
  ├─ ADMIN 권한 → 무조건 통과
  └─ 메뉴 테이블에 있는 URL → CustomUserDetails.readableMenuIds 에 포함 여부 확인
        ├─ 포함 → 통과
        └─ 미포함 → MenuAccessDeniedException → 403 페이지
```

**캐싱 처리**: `MenuQueryService.getAllVisibleMenus()`에 `@Cacheable("allVisibleMenus")`를 적용하여, 매 요청마다 메뉴 전체를 DB 조회하지 않도록 처리. `ConcurrentMapCacheManager` 사용.

**정책**: 메뉴 권한 변경은 재로그인 후 적용 (SecurityContext의 `readableMenuIds`를 새로 로드해야 하기 때문).

### 5.5 인증 예외 처리 (EntryPoint vs Handler)

| 상황 | 처리 클래스 | 동작 |
|------|------------|------|
| 미인증 + `/api/**` 요청 | `unauthenticatedEntryPoint` | JSON 401 응답 |
| 미인증 + 일반 페이지 요청 | `unauthenticatedEntryPoint` | 로그인 페이지 리다이렉트 |
| 인증됨 + 권한 부족 (403) | `accessDeniedHandler` | JSON 403 응답 |

**핵심 포인트**: `@RestControllerAdvice`는 Spring Security 필터 단계에서 발생하는 예외를 잡지 못한다. 401/403 처리는 반드시 `AuthenticationEntryPoint`와 `AccessDeniedHandler`로 별도 구현해야 한다.

### 5.6 CSRF 비활성화 이유

이 프로젝트는 세션 기반이지만 CSRF를 비활성화했다. 이유는:
1. Admin 화면이 React SPA이므로 CSRF 토큰을 폼에 포함하는 전통적 방식이 맞지 않는다.
2. CORS를 같은 origin으로 제한하여 외부 사이트에서 요청 자체가 불가하다.
3. 도메인이 단일 서버 구조이므로 CSRF 공격 표면이 제한적이다.

### 5.7 Remember-Me (자동 로그인)

`PersistentTokenRepository` → `JdbcTokenRepositoryImpl` 사용.
DB `persistent_logins` 테이블에 토큰 저장. 30일 유효.
인메모리 방식(`InMemoryTokenRepository`)을 사용하지 않아, 서버 재시작 후에도 자동 로그인 유지.

---

## 6. 핵심 설계 패턴

### 6.1 Dual Controller 패턴

같은 비즈니스 도메인에 대해 **SSR 컨트롤러**와 **REST API 컨트롤러** 두 가지가 존재한다.

```
com.bada.cali.controller.MemberController   → @Controller         (SSR 뷰 반환)
com.bada.cali.api.MemberController          → @RestController(...)  (JSON 반환)
```

**빈 이름 충돌 방지**: Spring은 빈 이름을 기본적으로 클래스명의 camelCase로 등록한다. 두 패키지에 같은 클래스명이 존재하면 기동 시 충돌 오류가 발생하므로, `api/` 패키지 컨트롤러는 반드시 빈 이름을 명시한다.

```java
// ✅ api 패키지: 빈 이름 명시
@RestController("ApiMemberController")
public class MemberController { ... }

// ✅ controller 패키지: 기본 이름 사용
@Controller
public class MemberController { ... }
```

### 6.2 ResMessage 공통 응답 포맷

```java
public class ResMessage<T> {
    private int code;   // 200 (성공) or -1 (실패)
    private String msg;
    private T data;
}
```

```json
// 성공
{ "code": 200, "msg": "조회 성공", "data": { ... } }

// 실패
{ "code": -1, "msg": "오류 메시지", "data": null }
```

### 6.3 GlobalViewNameAdvice (SSR 공통 모델)

`@ControllerAdvice`로 모든 SSR 컨트롤러에 공통 모델을 주입한다.

```java
@ControllerAdvice(basePackages = "com.bada.cali.controller")
public class GlobalViewNameAdvice {
    @ModelAttribute
    public void addCommonAttributes(Model model, Authentication auth) {
        // 사이드메뉴 목록 (캐싱된 전체 메뉴 + 유저 권한 필터링)
        // 현재 로그인 유저 정보
        // 페이지별 CSS 파일 존재 여부 (hasCss flag)
        // 현재 요청 URI (사이드메뉴 활성화 표시용)
    }
}
```

### 6.4 Soft Delete 패턴

민감한 데이터(member, business_trip, file_info 등)는 물리 삭제 대신 `delete_datetime`을 세팅하는 **Soft Delete**를 사용한다.

```sql
-- 조회 시 항상 필터 적용
WHERE delete_datetime IS NULL
```

### 6.5 DTO Inner Class 패턴

각 도메인의 요청/응답 DTO를 한 파일에 Inner class로 관리한다.

```java
public class BusinessTripDTO {
    public record CreateReq(@NotBlank String title, ...) {}   // 등록 요청
    public record UpdateReq(...) {}                            // 수정 요청
    public record DetailRes(...) {}                            // 상세 조회 응답
    public record CalendarEventRes(...) {}                     // 캘린더용 응답

    @Getter @Setter
    public static class GetListReq extends TuiGridDTO.Request {  // 리스트 요청 (상속)
        private String searchType;
        private String keyword;
    }
}
```

### 6.6 Projection 패턴 (목록 조회 최적화)

목록 조회 시 Entity 전체를 로드하지 않고, 필요한 컬럼만 인터페이스로 정의하여 조회한다.

```java
// 인터페이스 정의 (repository/projection/)
public interface BusinessTripListRow {
    Long getId();
    String getTitle();
    LocalDateTime getStartDatetime();
    Long getFileCnt();       // 서브쿼리 COUNT 결과
    String getTravelerNames(); // GROUP_CONCAT 결과
}

// Repository에서 Native Query + Projection 사용
@Query(value = "SELECT b.id, b.title, ..., COUNT(f.id) AS fileCnt, GROUP_CONCAT(m.name) AS travelerNames FROM ...", nativeQuery = true)
List<BusinessTripListRow> findList(...);
```

**이점**: 불필요한 컬럼 조회 없이 필요한 데이터만 가져오며, Entity를 통하지 않으므로 Lazy 로딩 이슈가 없다.

### 6.7 PATCH + Multipart 처리 (WebConfig)

HTTP 표준상 `multipart/form-data`는 POST 메서드에서만 파싱된다.
파일 첨부가 필요한 **수정(PATCH)** 요청을 처리하기 위해 WebConfig에서 커스텀 MultipartResolver를 등록한다.

```java
// WebConfig.java
@Bean
public MultipartResolver multipartResolver() {
    return new StandardServletMultipartResolver() {
        @Override
        public boolean isMultipart(HttpServletRequest request) {
            // PATCH 요청도 multipart로 처리하도록 오버라이드
            String method = request.getMethod().toUpperCase();
            return ("POST".equals(method) || "PATCH".equals(method))
                && request.getContentType() != null
                && request.getContentType().startsWith("multipart/");
        }
    };
}
```

### 6.8 트랜잭션 원칙

```java
// 조회: readOnly=true → 불필요한 dirty checking 비활성화
@Transactional(readOnly = true)
public List<...> getList(...) { ... }

// 등록/수정/삭제: 기본 @Transactional
@Transactional
public Long create(...) { ... }
```

트랜잭션 경계는 반드시 **Service 레이어**에 둔다. Controller에서 `@Transactional`을 남발하지 않는다.

---

## 7. REST API 설계

### 7.1 HTTP 메서드 원칙

| 메서드 | 용도 | 성공 상태코드 |
|--------|------|-------------|
| GET | 조회 | 200 |
| POST | 등록 | 201 (+ Location 헤더 선택) |
| PATCH | 부분 수정 | 200 |
| PUT | 전체 교체 | 200 |
| DELETE | 삭제 | 200 또는 204 |

### 7.2 URI 설계 원칙

- 복수 자원: `/api/businessTrips`
- 단건: `/api/businessTrips/{id}`
- 하위 자원: `/api/businessTrips/{id}/equipments`
- Admin 전용: `/api/admin/businessTrip`

### 7.3 주요 API 엔드포인트 목록

| 태그 | 기본 경로 | 주요 역할 |
|------|----------|----------|
| 회원 | `/api/member` | 로그인, 회원가입, 직원 CRUD, 메뉴 권한 관리 |
| 교정신청 | `/api/caliOrder` | 접수 등록/조회, 신청서 Excel 다운로드 |
| 성적서 | `/api/report` | 성적서 CRUD, 필수항목 검증, 통합 수정 |
| 성적서 작업 | `/api/report/jobs` | 배치 생성 트리거 및 상태 폴링 |
| 기본정보 | `/api/basic` | 업체/분류코드/부서 조회 |
| 샘플 | `/api/sample` | 샘플(기기) 등록/수정/파일 관리 |
| 표준장비 | `/api/equipment` | CRUD |
| 품목 | `/api/item` | 품목 CRUD, 수수료 이력 |
| 파일 | `/api/file` | 파일 다운로드/삭제 |
| 회사정보 (ADMIN) | `/api/admin/env` | 환경설정, 로고 이미지 업로드 |
| 출장일정 (ADMIN) | `/api/admin/businessTrip` | 캘린더, 등록/수정/삭제, 장비 중복 체크 |
| 작업서버 콜백 | `/api/worker` | cali-worker ↔ CALI 내부 통신 |

### 7.4 출장일정 API 상세 (신규 구현)

```
GET    /api/admin/businessTrip/calendar?rangeStart=&rangeEnd=
       → FullCalendar용 기간 내 출장 목록

GET    /api/admin/businessTrip/{id}
       → 상세 조회 (수정 폼 초기 데이터)

POST   /api/admin/businessTrip             [ADMIN 필수]
       → 등록 (multipart: createReq JSON + files)

PATCH  /api/admin/businessTrip/{id}        [ADMIN 필수]
       → 수정 (multipart: updateReq JSON + files)

POST   /api/admin/businessTrip/checkConflict
       → 지정 기간에 장비가 겹치는 다른 출장 체크

GET    /api/admin/businessTrip/memberOptions
       → 출장자 선택용 직원 목록

GET    /api/admin/businessTrip/list
       → 리스트 조회 (검색 + 날짜 범위 + 페이지네이션)

DELETE /api/admin/businessTrip             [ADMIN 필수]
       → 일괄 삭제 (RequestBody: {ids: [1,2,3]})
```

### 7.5 Swagger (OpenAPI) 운영

- URL: `http://{host}/swagger-ui/index.html`
- `GlobalApiExceptionHandler`를 먼저 분석하여 실제 상태코드와 `@ApiResponses` 일치 여부를 확인하고 작성
- `@ApiResponses` 명시 기준:
  - `500`: 모든 엔드포인트 (최종 방패 `Exception.class`)
  - `400`: `@Valid` 파라미터 있거나 `@RequestBody` 받는 경우
  - `404`: 서비스에서 `EntityNotFoundException`을 throw하는 경우
  - `403`: 서비스에서 `ForbiddenAdminModifyException`을 throw하는 경우
  - `401`: 명시 안 함 (Security 필터가 처리, JSON 응답 아님)

---

## 8. 예외 처리 구조

### 8.1 GlobalApiExceptionHandler

`@RestControllerAdvice(annotations = RestController.class)` — REST API 컨트롤러 전용

```java
@ExceptionHandler(MethodArgumentNotValidException.class) → 400
@ExceptionHandler(HttpMessageNotReadableException.class)  → 400
@ExceptionHandler(ForbiddenAdminModifyException.class)    → 403
@ExceptionHandler(EntityNotFoundException.class)          → 404
@ExceptionHandler(NoSuchKeyException.class)               → 404 (Object Storage)
@ExceptionHandler(MaxUploadSizeExceededException.class)   → 413
@ExceptionHandler(Exception.class)                        → 500 (최종 방패)
```

**중요**: `IllegalArgumentException`은 전용 핸들러가 없으므로 500으로 처리된다. 400으로 착각하지 말 것.

### 8.2 GlobalViewExceptionHandler

`@ControllerAdvice` — SSR 페이지 컨트롤러 전용

```java
@ExceptionHandler(MenuAccessDeniedException.class)        → error/403 뷰 렌더링
@ExceptionHandler(ForbiddenAdminModifyException.class)    → error/403 뷰 렌더링
```

### 8.3 Security 필터 레벨 예외

`@RestControllerAdvice`는 Security 필터 단계에서 발생하는 예외를 잡지 못한다.
아래 두 경우는 Security 설정에서 별도 핸들러를 등록해야 한다.

- 미인증 (`AuthenticationException`) → `AuthenticationEntryPoint`
- 권한 부족 (`AccessDeniedException`) → `AccessDeniedHandler`

### 8.4 Multipart 파싱 예외

`MaxUploadSizeExceededException`은 컨트롤러 진입 전 DispatcherServlet 단계에서 발생한다.
이는 `@RestControllerAdvice`가 잡지 못하는 영역이지만, Spring Boot가 이 예외를 DispatcherServlet까지 전파하는 방식으로 처리하여 결과적으로 `GlobalApiExceptionHandler`에서 잡힌다.
(Spring Boot 2.x 이후 `DefaultHandlerExceptionResolver` 처리 방식 변경 덕분)

### 8.5 예외 처리 레이어 다이어그램

```
[요청]
  │
  ▼
[Spring Security Filter]
  ├─ AuthenticationException → AuthenticationEntryPoint (401)
  └─ AccessDeniedException   → AccessDeniedHandler (403)
  │
  ▼
[DispatcherServlet]
  ├─ MaxUploadSizeExceededException → GlobalApiExceptionHandler (413)
  │
  ▼
[MVC 인터셉터 (MenuAccessInterceptor)]
  └─ MenuAccessDeniedException → GlobalViewExceptionHandler (403 뷰)
  │
  ▼
[컨트롤러]
  │
  ▼
[서비스/리포지토리]
  ├─ EntityNotFoundException        → GlobalApiExceptionHandler (404)
  ├─ ForbiddenAdminModifyException  → GlobalApiExceptionHandler (403)
  ├─ MethodArgumentNotValidException→ GlobalApiExceptionHandler (400)
  └─ Exception (모든 예외)          → GlobalApiExceptionHandler (500)
```

---

## 9. 데이터베이스 설계

### 9.1 주요 테이블 목록

| 테이블 | 설명 |
|--------|------|
| `member` | 직원 계정 (로그인, 권한, 활성화 여부) |
| `menu` | 사이드메뉴 (계층 구조, URL, 캐싱) |
| `member_permission_read` | 직원별 메뉴 읽기 권한 (N:M) |
| `cali_order` | 교정 접수 (채번 시퀀스 포함) |
| `sample` | 교정 대상 기기(샘플) 정보 |
| `report` | 교정 성적서 |
| `agent` | 업체(거래처, 비트플래그로 종류 구분) |
| `agent_member` | 업체 담당자 |
| `item` | 품목 정보 |
| `standard_equipment` | 표준장비 |
| `business_trip` | 출장 일정 |
| `standard_equipment_ref` | 출장일정-표준장비 연결 |
| `file_info` | 첨부파일 메타데이터 |
| `log` | 등록/수정/삭제 이력 |
| `env` | 회사 환경설정 |
| `notice` | 공지사항 |
| `dept`, `position`, `duty` | 부서, 직급, 직무 |
| `persistent_logins` | Spring Security Remember-Me 토큰 저장 |

### 9.2 member 테이블 주요 컬럼

```sql
CREATE TABLE member (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    login_id        VARCHAR(100) UNIQUE NOT NULL,   -- 로그인 ID (사업자번호)
    pwd             VARCHAR(255) NOT NULL,           -- BCrypt 해시
    name            VARCHAR(100) NOT NULL,
    auth            ENUM('user','admin') DEFAULT 'user',
    is_active       ENUM('Y','N') DEFAULT 'N',       -- 가입 승인 여부
    login_count     INT DEFAULT 0,
    last_login_fail_datetime DATETIME,               -- 로그인 실패 시각 (10분 제한)
    last_pwd_updated         DATETIME,               -- 비밀번호 변경일 (90일 경고)
    delete_datetime DATETIME,                        -- Soft delete
    delete_member_id BIGINT
);
```

### 9.3 agent 테이블의 비트플래그 설계

```sql
agent_flag TINYINT UNSIGNED NOT NULL DEFAULT 0
-- bit 0: 고객사 (1)
-- bit 1: 대리점 (2)
-- bit 2: 성적서발행처 (4)
-- bit 3: 기타 (8)
-- 복합: 고객사 + 성적서발행처 = 5
```

한 업체가 여러 역할을 동시에 가질 수 있으므로 비트플래그로 표현.

### 9.4 DB 버전 관리 방식

```
docs/db/
├── schema.sql              # Full Schema (유일한 진실, 새 환경 셋업 시 이것만 실행)
└── versions/
    ├── v_260316.sql        # 2026-03-16 변경분
    ├── v_260317.sql        # 2026-03-17 변경분
    └── v_260326.sql        # 2026-03-26 변경분 (business_trip 신규)
```

`schema.sql`은 항상 최신 상태를 유지. `versions/`는 변경 이력 추적용.

### 9.5 GROUP_CONCAT 활용 패턴

출장자 이름 목록을 한 번의 쿼리로 조회하기 위해 MySQL `GROUP_CONCAT`을 사용한다.

```sql
-- business_trip.traveler_ids = "1,5,12" (콤마 구분 member.id)
SELECT GROUP_CONCAT(m.name ORDER BY m.id SEPARATOR ', ')
FROM member m
WHERE FIND_IN_SET(m.id, b.traveler_ids) > 0
-- → "홍길동, 김철수, 이영희"
```

**주의**: `FIND_IN_SET`은 인덱스를 사용하지 못하므로 출장자 수가 매우 많아지면 성능 이슈가 될 수 있다. 현재 규모에서는 문제없음.

---

## 10. React Admin + Spring Boot 연동

### 10.1 연동 방식 개요

React Admin과 Spring Boot는 **쿠키 기반 세션 인증**으로 통신한다. JWT를 사용하지 않는다.

```
개발 환경:
  React (localhost:3000) → Vite dev proxy → Spring Boot (localhost:8050)
  → 같은 세션 쿠키(JSESSIONID)로 인증

운영 환경:
  React는 빌드 후 dist/ → Spring Boot static 리소스로 서빙
  → 동일 origin (same-origin), 쿠키 자연스럽게 공유
```

### 10.2 Vite 개발 프록시 설정

```javascript
// vite.config.js
export default defineConfig({
  base: '/admin/',           // React SPA 기본 경로
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8050',  // Spring Boot
        changeOrigin: true,
      },
      '/logout': {                         // 로그아웃도 백엔드로 프록시
        target: 'http://localhost:8050',
        changeOrigin: true,
      },
    },
  },
});
```

**CORS 처리**: 개발 환경에서는 Vite 프록시가 cross-origin 문제를 해결한다. 운영 환경에서는 동일 origin이므로 CORS 설정이 불필요하다.

### 10.3 adminFetch() 내 401 처리

```javascript
export const adminFetch = async (url, options = {}) => {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });

  if (!res.ok) {
    if (res.status === 401) {
      // 세션 만료 → 백엔드 로그인 페이지로 강제 이동 (현재 경로 redirect 포함)
      const currentPath = window.location.pathname + window.location.search;
      const loginUrl = '/member/login?redirect=' + encodeURIComponent(currentPath) + '&required=1';
      window.location.href = loginUrl;
      throw new Error('Unauthorized');
    }
    const err = new Error(`HTTP ${res.status}`);
    err.status = res.status;
    err.data = await res.json().catch(() => null);
    throw err;
  }

  if (res.status === 204) return null;  // No Content
  return res.json();
};
```

### 10.4 파일 업로드 (multipart) 처리 주의

`adminFetch`는 기본으로 `Content-Type: application/json` 헤더를 설정한다.
파일 업로드 시 이 헤더를 넣으면 브라우저가 자동으로 설정해야 하는 **`multipart/form-data; boundary=...`** 값이 덮어써져 서버 파싱이 실패한다.

```javascript
// ❌ 잘못된 방법 - adminFetch 사용
await adminFetch('/api/...', { method: 'POST', body: formData });  // Content-Type 충돌

// ✅ 올바른 방법 - 직접 fetch 사용, Content-Type 명시하지 않음
const res = await fetch('/api/...', { method: 'POST', body: formData });
// 브라우저가 multipart/form-data; boundary=xxx 를 자동 설정
```

### 10.5 React 라우팅 (BrowserRouter)

```javascript
// App.jsx
<BrowserRouter basename="/admin/">
  <Routes>
    <Route path="/" element={<AdminLayout />}>
      <Route index element={<Dashboard />} />
      <Route path="companyInfo" element={<CompanyInfo />} />
      <Route path="notices" element={<Notices />} />
      <Route path="menuPermissions" element={<MenuPermissions />} />
    </Route>
  </Routes>
</BrowserRouter>
```

**운영 배포 시 주의**: `BrowserRouter` 사용으로 `/admin/companyInfo` 같은 직접 URL 접근이 가능하려면, Nginx에서 `/admin/**` → `index.html` 폴백을 설정해야 한다.

```nginx
location /admin/ {
    try_files $uri $uri/ /admin/index.html;
}
```

### 10.6 로그아웃 처리 흐름

```
React → POST /logout (Vite proxy → :8050)
  → Spring Security LogoutFilter 처리
  → 세션 무효화, remember-me 쿠키 삭제
  → JSON 응답: {"ok":true, "redirect":"/member/login?logout"}
  → React에서 redirect 경로로 window.location 이동
```

---

## 11. 파일 업로드 / Object Storage 연동

### 11.1 NCP Object Storage (S3 호환)

Naver Cloud Platform의 Object Storage는 AWS S3 호환 API를 제공한다.
따라서 **AWS SDK v2**를 그대로 사용하되 endpoint만 NCP 주소로 변경하면 동작한다.

```java
// NcloudS3Config.java
@Bean
public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))  // NCP endpoint
        .region(Region.of(regionName))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            )
        )
        .build();
}
```

### 11.2 파일 저장 구조

```
버킷: cali
  ├─ dev/                 # 로컬/개발 환경
  │   ├─ report/          # 성적서 관련 파일
  │   │   ├─ {id}/
  │   │   │   ├─ original.xlsx
  │   │   │   └─ signed.pdf
  │   ├─ sample/          # 샘플(기기) 첨부파일
  │   └─ businessTrip/    # 출장일정 첨부파일
  └─ cali/                # 운영 환경 (ncp.storage.root-dir=cali)
```

### 11.3 file_info 테이블

```sql
CREATE TABLE file_info (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    ref_table_name  VARCHAR(100) NOT NULL,   -- 참조 테이블명 ('report', 'sample', ...)
    ref_table_id    BIGINT NOT NULL,          -- 참조 레코드 PK
    origin_name     VARCHAR(300),            -- 원본 파일명
    name            VARCHAR(300),            -- 저장 파일명 (UUID 기반)
    extension       VARCHAR(50),
    file_size       BIGINT,
    content_type    VARCHAR(200),
    type            VARCHAR(50),             -- 'origin', 'signed', NULL
    dir             VARCHAR(500),            -- 버킷 내 하위 경로
    is_visible      ENUM('y','n') DEFAULT 'y'  -- Soft delete
);
```

---

## 12. 성적서 작업서버(cali-worker) 연동

### 12.1 연동 구조

```
[사용자] → 성적서 작성 요청 → CALI 서버
  → (app.worker.url이 있으면) cali-worker로 HTTP 요청
  → cali-worker: Excel COM Automation으로 성적서 생성
  → 완료 시 CALI 콜백 URL로 결과 전송
  → CALI: 결과 저장 (배치 상태 업데이트)
  → [사용자]: 폴링(3초 주기)으로 상태 확인
```

### 12.2 폴링 전략 (무한 대기 방지)

```javascript
// 프론트엔드 폴링 (최대 100회, 약 5분)
let pollCount = 0;
const MAX_POLL = 100;

const pollStatus = async () => {
    if (pollCount >= MAX_POLL) {
        showError('작업 시간이 초과되었습니다. 나중에 결과를 확인해주세요.');
        return;
    }
    pollCount++;

    const res = await adminFetch(`/api/report/jobs/${jobId}`);
    if (res.data.status === 'DONE') {
        showSuccess('성적서 작성 완료');
    } else if (res.data.status === 'FAILED') {
        showError(res.data.errorMsg);
    } else {
        setTimeout(pollStatus, 3000);  // 3초 후 재확인
    }
};
```

### 12.3 데모 모드

`app.worker.url`이 비어있으면 작업서버 연동을 건너뛰고 데모 응답을 반환한다.
채용담당자 시연 시 별도 서버 없이도 성적서 작업 흐름을 시연 가능.

### 12.4 API 키 인증 (서버 간 내부 통신)

cali-worker → CALI 콜백 요청 시 `X-Worker-Api-Key` 헤더로 인증.
Security 설정에서 `/api/worker/**`는 `permitAll()`로 공개하되, 컨트롤러에서 API 키를 검증한다.

```java
@PostMapping("/api/worker/callback")
public ResponseEntity<?> callback(
    @RequestHeader("X-Worker-Api-Key") String apiKey,
    @RequestBody WorkerCallbackReq req) {

    if (!workerApiKey.equals(apiKey)) {
        return ResponseEntity.status(403).body(new ResMessage<>(-1, "인증 실패", null));
    }
    // ... 처리
}
```

---

## 13. 주요 설정 파일 분석

### 13.1 application.properties 핵심 항목

```properties
# 서버 포트
server.port=8050

# 세션 타임아웃 (기본 30분 → 8시간으로 연장)
server.servlet.session.timeout=8h

# Remember-Me 자동 로그인 키 (쿠키 서명 검증용)
security.remember-me.key=cali0803

# JPA open-in-view 비활성화
# → DB 커넥션을 뷰 렌더링까지 점유하지 않도록 명시 (성능 이점)
spring.jpa.open-in-view=false

# 파일 업로드 크기 제한
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=100MB

# 임시 디렉토리 (교정신청서 Excel 생성 시 사용)
app.temp.dir=./temp

# Admin 페이지 URL (환경별로 오버라이드)
# 로컬: React dev server, 운영/개발 서버: 빈값 → DB 값 사용
app.admin.url=http://localhost:3000/admin/

# 성적서 작업서버 연동 (비워두면 데모 모드)
app.worker.url=
app.worker.api-key=cali-worker-key-dev
app.cali.callback-base-url=http://localhost:8050

# NCP Object Storage
ncp.storage.endpoint=https://kr.object.ncloudstorage.com
ncp.storage.bucket-name=cali
ncp.storage.root-dir=dev   # 운영 환경: cali
```

### 13.2 build.gradle 주요 의존성 (주석 포함 의도)

```gradle
// Bean Validation은 Spring Boot 3.x에서 starter-web에 포함되지 않으므로 별도 추가
implementation 'org.springframework.boot:spring-boot-starter-validation'

// MapStruct: 컴파일 타임 코드 생성 → annotationProcessor 필수
implementation "org.mapstruct:mapstruct:1.6.3"
annotationProcessor "org.mapstruct:mapstruct-processor:1.6.3"

// AWS SDK v2 BOM: 서브 모듈 버전을 BOM으로 통합 관리
implementation(platform("software.amazon.awssdk:bom:2.27.21"))
implementation("software.amazon.awssdk:s3")

// Actuator: /actuator/health 엔드포인트 → CI/CD 배포 후 기동 확인용
implementation 'org.springframework.boot:spring-boot-starter-actuator'

// thymeleaf-extras-springsecurity6: 타임리프에서 sec:authorize 태그 사용 가능
implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'

// thymeleaf-layout-dialect: th:layout 레이아웃 방언 (공통 레이아웃 적용)
implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.2.1'
```

### 13.3 GitHub Actions CI/CD 흐름

```yaml
# deploy-dev.yml (예시 흐름)
on:
  push:
    branches: [develop]       # develop 브랜치 푸시 시 자동 배포

jobs:
  deploy:
    steps:
      - checkout
      - gradle bootJar          # JAR 빌드
      - npm run build            # React 빌드 (dist/ 생성)
      - scp JAR to server        # 서버로 전송
      - SSH → systemctl restart cali  # 서버 재시작
      - curl /actuator/health    # 헬스체크 확인
```

---

## 14. 주요 이슈 & 해결 방식 (상세)

### 이슈 1 — SSR 컨트롤러와 REST API 컨트롤러 빈 이름 충돌

**원인**: Spring은 `@Controller`와 `@RestController`를 클래스명 기반으로 빈 이름을 등록한다.
`com.bada.cali.controller.MemberController`와 `com.bada.cali.api.MemberController`가 함께 존재하면 빈 이름 `memberController`가 중복되어 기동 시 오류 발생.

**해결**: `api/` 패키지의 컨트롤러 전체에 `@RestController("ApiMemberController")` 형식으로 빈 이름을 명시.

**학습 포인트**: Spring Bean의 기본 이름 규칙(클래스명 camelCase)과 충돌 방지 방법.

---

### 이슈 2 — Multipart 파싱 단계 예외가 `@RestControllerAdvice`에서 미처리

**원인**: `MaxUploadSizeExceededException`은 파일 크기 초과 시 Tomcat의 `CommonsMultipartResolver` 파싱 단계에서 발생한다. 이 시점은 `@RestControllerAdvice`가 동작하는 컨트롤러 진입 전이므로, 전역 핸들러가 이를 잡지 못할 수 있다.

**해결**: Spring Boot의 `DefaultHandlerExceptionResolver`가 이 예외를 DispatcherServlet 레벨에서 처리하도록 전파하고, 결과적으로 `@RestControllerAdvice`에서 `@ExceptionHandler(MaxUploadSizeExceededException.class)`로 잡힌다.
명시적으로 핸들러를 추가하여 `ResMessage` 형식으로 413 응답을 반환하도록 처리.

**학습 포인트**: Spring MVC 예외 처리 계층 구조와 `@RestControllerAdvice` 한계.

---

### 이슈 3 — React Admin 이미지 업로드 시 Multipart boundary 누락

**원인**: `adminFetch()`가 `Content-Type: application/json`을 기본 헤더로 설정하므로, `FormData` 객체를 body로 전달해도 이 헤더가 우선 적용된다.
브라우저가 `multipart/form-data; boundary=----WebKitFormBoundary...` 를 자동 생성하려면 `Content-Type` 헤더가 없어야 한다.

**해결**: 파일 업로드 엔드포인트에서는 `adminFetch` 대신 `fetch`를 직접 사용하고 `Content-Type` 헤더를 완전히 제거한다.

```javascript
// 파일 업로드 시
const formData = new FormData();
formData.append('updateReq', new Blob([JSON.stringify(data)], { type: 'application/json' }));
formData.append('files', file);

// Content-Type 헤더 없이 fetch 직접 사용
await fetch('/api/admin/env/image', { method: 'POST', body: formData });
```

**학습 포인트**: `multipart/form-data` 요청에서 `Content-Type` 헤더와 boundary 자동 생성 원리.

---

### 이슈 4 — Spring Security 세션 만료 시 React Admin에 HTML 응답 반환

**원인**: Spring Security의 기본 미인증 처리는 로그인 페이지로 302 리다이렉트다.
React SPA에서 `fetch`로 API를 호출하면, 302 리다이렉트 후 `/member/login` HTML 페이지가 응답으로 수신된다.
`response.ok`는 200(로그인 페이지 HTML)이므로 에러가 감지되지 않고, JSON 파싱 시도 후 오류가 발생한다.

**해결**: `AuthenticationEntryPoint`에서 요청 URI를 확인하여 `/api/**` 경로에는 JSON 401을 반환한다.

```java
@Bean
public AuthenticationEntryPoint unauthenticatedEntryPoint() {
    return (request, response, authException) -> {
        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":-1,\"msg\":\"인증이 필요합니다.\",\"data\":null}");
        } else {
            response.sendRedirect("/member/login?redirect=...");
        }
    };
}
```

React의 `adminFetch`는 401 수신 시 로그인 페이지로 이동한다.

**학습 포인트**: SPA + 세션 인증 혼용 시 미인증 응답 처리 전략.

---

### 이슈 5 — 채번 시퀀스 동시 요청 시 중복 발급 가능성

**원인**: 교정신청서 채번 로직이 `SELECT MAX(seq_num) → +1 → INSERT` 구조로 구성되어 있어, 두 요청이 동시에 `MAX` 값을 조회하면 같은 번호를 발급할 수 있다 (Race Condition).

**해결**: `@Transactional` + `SELECT ... FOR UPDATE` (비관적 락)를 적용하여 조회 시 락을 획득하고 순차 처리를 보장한다.

```java
@Transactional
public Long generateSeqNum() {
    // SELECT ... FOR UPDATE → 다른 트랜잭션 대기
    CaliOrderSeq seq = caliOrderSeqRepository.findByYearForUpdate(year);
    seq.increment();
    return seq.getSeqNum();
}
```

**학습 포인트**: 낙관적 락 vs 비관적 락 선택 기준. 동시성이 높지 않고 충돌 발생 시 비용이 크므로 비관적 락 선택.

---

### 이슈 6 — 성적서 작업 폴링 중 작업 서버 장애 시 무한 대기

**원인**: cali-worker가 응답하지 않으면, 배치 상태가 `PROCESSING`에서 변경되지 않아 프론트엔드 폴링이 무한 반복된다.

**해결**: 최대 폴링 횟수(100회, 약 5분)를 제한하고, 초과 시 사용자에게 안내 메시지를 표시하고 폴링을 종료한다.
서버 측 배치 상태는 보존되므로 재접속 후 결과 확인이 가능하다.

**추후 과제**: 작업서버 장애 감지 시 자동 재시도 또는 Webhook 기반 알림 도입.

---

### 이슈 7 — `@Valid` 검증 실패와 `IllegalArgumentException`의 응답 코드 혼동

**원인**: 두 예외 모두 "잘못된 요청"으로 느껴져 400으로 착각하기 쉽다.
하지만 `IllegalArgumentException`은 `GlobalApiExceptionHandler`에 전용 핸들러가 없으므로, 최종 방패 `Exception.class` → 500으로 처리된다.

**해결**: `GlobalApiExceptionHandler` 분석 결과를 바탕으로 Swagger `@ApiResponses` 작성 기준을 명문화:
- `400`: `@Valid` 파라미터 or `@RequestBody` 있는 경우만
- `500`: 모든 엔드포인트 (최종 방패)
- `IllegalArgumentException` = 500 (400 아님)

**학습 포인트**: 예외 유형별 상태코드 매핑을 코드 수준에서 정확히 파악하는 것의 중요성.

---

### 이슈 8 — 서버 사이드 PDF 변환 품질 한계 (전략 변경)

**원인**: iText, PDFBox 등 Java 기반 PDF 라이브러리는 Excel 스타일을 완전히 재현하지 못해 교정 성적서 수준의 품질이 나오지 않는다.

**대응**: 서버 사이드 PDF 방식을 중단하고, **Excel COM Automation 기반의 외부 작업 서버(cali-worker)**로 전략을 전환한다.
Windows 환경에서 Excel 자체를 자동화하면 완전한 스타일 재현이 가능하다.
cali-worker는 C# WPF 기반 미들웨어로 구현 예정.

**학습 포인트**: 기술 선택 시 품질/성능 트레이드오프 분석과 방향 전환 결정 과정.

---

## 15. 구현 기능 현황

### 15.1 완성된 기능 (2026-03 기준)

| 기능 | 화면 종류 | 주요 특징 |
|------|----------|----------|
| 회원가입 / 로그인 | SSR | 사업자번호 기반, 관리자 승인 필요, Remember-Me 30일, 10분 로그인 잠금 |
| 메뉴 권한 관리 | React Admin | 직원별 메뉴 읽기 권한 부여/해제, MenuAccessInterceptor 기반 접근 제어 |
| 교정 신청(접수) 관리 | SSR | 접수 등록/조회, 채번 시퀀스(비관적 락), 교정신청서 Excel 다운로드 |
| 성적서 관리 | SSR | CRUD, 다중 선택 통합수정, cali-worker 연동 Excel 자동생성, 폴링 |
| 업체 / 담당자 관리 | SSR | 업체 정보 및 담당자 CRUD, 비트플래그 기반 업체 종류 관리 |
| 품목 / 분류코드 관리 | SSR | 품목 CRUD, 수수료 이력 관리, 분류코드 계층 관리 |
| 표준장비 관리 | SSR | CRUD, 교정 작업 시 사용 장비 이력 추적 |
| 기본정보 관리 | SSR | 부서/직급/직무 CRUD |
| 직원 관리 | React Admin | 직원 CRUD, Soft delete, 메뉴 권한 관리 연동 |
| 업데이트 공지 관리 | React Admin | 공지 CRUD |
| 회사정보 관리 | React Admin | 회사 정보 + 로고(KOLAS/ILAC/사내) 이미지 업로드 (ADMIN 전용) |

### 15.2 개발 중인 기능

| 기능 | 상태 | 비고 |
|------|------|------|
| 출장일정 관리 | 진행 중 | 백엔드 API 완성, SSR 화면 초기 구현 중 |

### 15.3 구현 예정 기능

| 기능 | 설명 |
|------|------|
| 견적서 발행 | 업체 대상 교정 견적서 생성 및 발송 |
| 완료통보서 발행 | 교정 완료 후 공식 통보서 자동 생성 |
| 작업 이력 관리 | 처리 단계별 이력 추적 |
| 모니터링 대시보드 | 진행 현황 통계, 로그인 이력 (React Admin) |
| cali-worker C# 전환 | COM Automation 기반 Excel 자동생성 미들웨어 |

---

## 16. 면접 예상 질문 & 모범 답변

### [Spring Security / 인증·인가]

---

**Q1. Spring Security의 SecurityFilterChain은 무엇이고, 어떻게 동작하나요?**

A. `SecurityFilterChain`은 Spring Security가 HTTP 요청을 처리하는 필터들의 체인입니다. 모든 요청은 `DelegatingFilterProxy`를 통해 Spring의 `FilterChainProxy`로 전달되고, 설정된 URL 패턴에 맞는 `SecurityFilterChain`이 선택되어 순서대로 필터들이 실행됩니다.

CALI에서는 `filterChain()` 빈을 직접 정의하여 CSRF 비활성화, 경로별 권한 규칙, 폼 로그인 설정, Remember-Me, 로그아웃, 예외 처리를 한 곳에서 구성했습니다. `@Bean`으로 등록하기 때문에 여러 FilterChain을 `@Order`로 우선순위를 지정하여 중복 등록도 가능합니다.

---

**Q2. UserDetails와 UserDetailsService는 어떤 역할을 하나요?**

A. `UserDetailsService`는 Spring Security가 로그인 시 사용자 정보를 로드하기 위해 호출하는 인터페이스입니다. `loadUserByUsername(String username)`을 구현하여 DB에서 사용자를 조회하고 `UserDetails` 객체를 반환합니다.

CALI에서는 `CustomUserDetails`로 `UserDetails`를 확장하여, 기본 필드(username, password, authorities) 외에 **readableMenuIds**(메뉴 접근 권한 목록)와 **lastPwdUpdated**(비밀번호 변경일)를 추가했습니다. 이 정보를 로그인 시 한 번에 로드하여 SecurityContext에 저장함으로써, 이후 요청마다 DB 재조회 없이 권한 체크가 가능합니다.

---

**Q3. `CustomUserDetails`에 `Serializable`을 구현한 이유가 무엇인가요?**

A. Spring Security는 인증 정보(Authentication)를 HTTP 세션에 저장합니다. HTTP 세션은 Servlet 컨테이너(Tomcat)가 관리하는데, 서버 재시작이나 세션 클러스터링 환경에서는 세션 객체를 직렬화하여 저장합니다.

`CustomUserDetails`는 `Authentication` 안의 `principal` 필드에 들어가기 때문에, 직렬화 대상이 됩니다. `Serializable`을 구현하지 않으면 `NotSerializableException`이 발생할 수 있습니다. 특히 Remember-Me 기능처럼 세션 복원이 필요한 경우에도 반드시 구현해야 합니다.

---

**Q4. CSRF 토큰을 비활성화한 이유와, 그 경우 어떻게 보안을 유지하나요?**

A. CSRF는 외부 사이트에서 로그인된 사용자의 쿠키를 이용해 요청을 위조하는 공격입니다. 이를 막는 전통적 방법이 폼에 CSRF 토큰을 포함하는 것인데, React SPA 환경에서는 서버가 렌더링한 폼을 사용하지 않아 이 방식이 맞지 않습니다.

대신 다음으로 보안을 유지합니다:
1. **Same-Origin 제한**: 운영 환경에서 동일 도메인에서만 API를 호출하므로 외부 사이트 요청이 불가합니다.
2. **세션 쿠키의 SameSite 속성**: 브라우저의 SameSite 정책으로 크로스 사이트 쿠키 전송이 차단됩니다.
3. **CORS 미허용**: 별도 CORS 설정이 없어 외부 origin의 요청은 브라우저 레벨에서 차단됩니다.

---

**Q5. JWT와 세션 인증 중 세션을 선택한 이유는 무엇인가요?**

A. CALI는 단일 서버 구조이고 확장성(수평 스케일아웃)이 현재 요구사항에 없습니다. 또한 React Admin과 Thymeleaf SSR이 혼재하는 하이브리드 구조에서 세션 기반 인증이 통합이 훨씬 자연스럽습니다.

세션 인증의 장점: 즉각적인 로그아웃(서버 측 세션 삭제), 탈취 토큰 무효화 용이, 추가 구현 없이 CSRF(SameSite)로 대응 가능.

JWT는 MSA, 서버리스, 수평 확장 환경에서 적합하지만, 토큰 탈취 시 만료 전까지 무효화가 어렵고 Blacklist 관리가 필요합니다. 현재 프로젝트 규모와 요구사항에서는 세션 인증이 적합하다고 판단했습니다.

---

**Q6. `@PreAuthorize`와 SecurityFilterChain 권한 설정 차이는 무엇인가요? 언제 무엇을 사용하나요?**

A. 둘 다 접근 제어를 하지만 적용 레벨이 다릅니다.

- **SecurityFilterChain**: URL 패턴 기반. Security 필터 단계에서 요청을 차단. 컨트롤러 진입 전에 처리.
- **`@PreAuthorize`**: 메서드 기반. Spring AOP를 이용해 메서드 호출 시점에 권한 체크.

CALI에서는 `/api/admin/**` 경로는 SecurityFilterChain에서 일괄 처리하고, 비-admin 경로에 admin 전용 기능이 추가될 경우 `@PreAuthorize("hasRole('ADMIN')")`를 메서드에 추가합니다. 두 방식을 상황에 따라 병행 사용하되, SecurityFilterChain이 우선 적용되므로 중복 설정 시에도 문제는 없습니다.

---

### [Spring Boot / JPA / 설계]

---

**Q7. `@Transactional(readOnly = true)`를 조회에 사용하는 이유가 무엇인가요?**

A. `readOnly = true`로 설정하면 Hibernate가 해당 트랜잭션에서 **dirty checking(변경 감지)**을 비활성화합니다. 이로 인해:
1. 스냅샷 저장 및 비교 비용이 줄어 메모리 효율이 높아집니다.
2. DB 드라이버에 따라 읽기 전용 힌트를 전달하여 최적화가 가능합니다.
3. 의도치 않은 데이터 변경을 방지하는 안전장치 역할도 합니다.

조회만 하는 서비스 메서드에 습관적으로 적용하면 성능과 코드 의도 명확성 모두 챙길 수 있습니다.

---

**Q8. JPA에서 `spring.jpa.open-in-view=false`를 설정한 이유는 무엇인가요?**

A. `open-in-view`가 `true`(기본값)이면, HTTP 요청이 들어올 때 DB 커넥션을 열어 뷰가 렌더링 완료될 때까지 유지합니다. 이로 인해 커넥션 풀이 고갈될 수 있고, 레이어 경계(서비스 → 컨트롤러 → 뷰)가 흐릿해져 Lazy 로딩이 어디서든 실행되는 문제가 생깁니다.

`false`로 설정하면 트랜잭션이 끝나는 시점에 커넥션을 반환합니다. 이때 Lazy 로딩 이슈가 발생할 수 있지만, CALI에서는 Projection 인터페이스, Fetch Join, DTO 매핑으로 해결합니다. 레이어 경계가 명확해지고 커넥션 자원을 효율적으로 사용할 수 있습니다.

---

**Q9. Entity와 DTO를 분리하는 이유는 무엇이고, MapStruct를 선택한 이유는?**

A. Entity는 DB 테이블과 직접 매핑되는 도메인 객체로, JPA가 관리합니다. 이를 API 응답에 그대로 사용하면:
1. 모든 필드가 외부에 노출됩니다 (비밀번호, 내부 audit 컬럼 등).
2. JSON 직렬화 중 Lazy 로딩이 트리거되어 N+1 문제나 LazyInitializationException이 발생합니다.
3. API 계약과 DB 스키마가 결합되어 변경 시 서로 영향을 줍니다.

MapStruct는 컴파일 타임에 매핑 코드를 생성하므로 리플렉션 기반 라이브러리(ModelMapper 등)보다 성능이 빠르고, 컴파일 시점에 매핑 오류를 발견할 수 있습니다. IDE 지원도 우수합니다.

---

**Q10. Projection 인터페이스를 사용하는 이유와, Entity 조회와의 차이점은?**

A. 목록 조회 시 Entity를 사용하면 테이블의 모든 컬럼을 `SELECT`하고, JPA 1차 캐시에 올려 dirty checking 대상이 됩니다. 하지만 목록 화면에서는 전체 필드 중 일부만 필요합니다.

Projection 인터페이스는 필요한 필드만 `SELECT`하는 인터페이스입니다. Spring Data JPA가 이를 인식하여 실제 SQL의 `SELECT` 절을 최적화합니다.

CALI에서는 `BusinessTripListRow`처럼 서브쿼리(`COUNT(*)`) 결과나 `GROUP_CONCAT` 결과도 Projection에 포함할 수 있어, Native Query와 함께 사용하면 복잡한 목록 조회도 깔끔하게 처리됩니다.

---

**Q11. 비관적 락(Pessimistic Lock)을 사용한 이유와, 낙관적 락과의 차이는?**

A. 교정신청서 채번은 `SELECT MAX(seq_num)` → `+1` → `INSERT` 흐름인데, 동시 요청 시 두 트랜잭션이 같은 `MAX` 값을 읽어 중복 번호를 발급할 수 있습니다.

**낙관적 락**은 `@Version` 필드를 사용해 수정 시점에 충돌을 감지하고, 충돌 시 예외를 던져 재시도를 요구합니다. 충돌이 드문 경우 적합합니다.

**비관적 락**은 `SELECT FOR UPDATE`로 조회 시점에 락을 획득하여 다른 트랜잭션이 대기하도록 합니다. 충돌 가능성이 높거나 충돌 비용(중복 채번)이 크므로 비관적 락을 선택했습니다.

---

**Q12. Dual Controller 패턴(같은 클래스명, 다른 패키지)을 사용하는 이유는?**

A. 같은 도메인(예: Member)에 대해 SSR 페이지 처리와 REST API 처리가 필요한 경우, 하나의 컨트롤러에 모으면 역할이 혼재되어 코드가 복잡해집니다.

패키지를 분리하여 `controller/`는 뷰 이름 반환, `api/`는 JSON 반환으로 역할을 명확히 구분했습니다. 단, Spring의 기본 빈 이름 규칙(클래스명 camelCase)으로 인한 충돌을 방지하기 위해 `api/` 패키지 컨트롤러에는 `@RestController("ApiXxxController")`로 빈 이름을 명시합니다.

---

**Q13. Soft Delete를 사용하는 이유는 무엇이고, 어떤 단점이 있나요?**

A. Soft Delete는 레코드를 실제 삭제하지 않고 `delete_datetime` 컬럼에 삭제 시각을 기록하는 방식입니다.

**장점**: 실수로 삭제한 데이터 복구 가능, 삭제 이력 추적, 참조 무결성 이슈 감소.

**단점**: 모든 조회 쿼리에 `WHERE delete_datetime IS NULL` 필터가 필요합니다. 누락 시 삭제된 데이터가 노출됩니다. 대용량 테이블에서 인덱스 활용도가 떨어질 수 있습니다.

CALI에서는 `@Where(clause = "delete_datetime IS NULL")`을 Entity에 적용하거나, Native Query에서는 직접 조건을 명시하는 방식으로 처리합니다.

---

### [React / Frontend]

---

**Q14. React Admin과 Spring Boot를 같은 서버에서 서빙하는 방법은?**

A. 개발 환경에서는 Vite dev server(포트 3000)에서 React를 실행하고, `/api/**` 요청을 Spring Boot(포트 8050)로 프록시합니다. 이로 인해 CORS 없이 개발이 가능합니다.

운영 환경에서는 `npm run build`로 생성된 `dist/` 폴더를 Spring Boot의 `static/admin/` 경로에 복사합니다. 그러면 Spring Boot가 `/admin/**` 요청에 대해 React 빌드 파일을 서빙하고, `/api/**` 요청은 REST API로 처리합니다.

Nginx에서는 `/admin/**` 경로에 BrowserRouter 폴백(`try_files $uri /admin/index.html`)을 설정하여 SPA 라우팅을 지원합니다.

---

**Q15. adminFetch가 401을 받았을 때 로그인 페이지로 이동하는 로직은 어떻게 구현했나요?**

A. `adminFetch` 내부에서 `response.ok`가 false이고 `res.status === 401`이면, 현재 URL(`window.location.pathname + search`)을 `redirect` 파라미터로 인코딩하여 `/member/login?redirect=...&required=1` 경로로 이동합니다.

Spring Boot 로그인 성공 핸들러는 `redirect` 파라미터가 있으면 로그인 후 해당 경로로 이동합니다. 이로 인해 세션 만료 후 재로그인 시 원래 작업하던 페이지로 자동 복귀가 가능합니다.

이 방식은 JWT의 Refresh Token 없이도 세션 만료를 사용자 친화적으로 처리하는 방법입니다.

---

**Q16. Toast UI Grid를 선택한 이유는 무엇인가요?**

A. 관리자 페이지의 핵심은 데이터 목록 조회와 관리입니다. 복잡한 컬럼 설정, 서버사이드 페이지네이션, 정렬, 필터, 셀 편집 등을 지원하는 그리드 컴포넌트가 필요했습니다.

Toast UI Grid는 국내 네이버에서 만든 오픈소스로 한국어 문서가 풍부하고, 서버사이드 페이지네이션, 체크박스 선택, 커스텀 렌더러 등 업무 화면에 필요한 기능을 대부분 지원합니다. jQuery 기반(SSR)과 React Admin 모두에서 사용할 수 있어 일관성 있는 UX를 제공합니다.

---

**Q17. SweetAlert2와 react-toastify를 함께 쓰는 이유는?**

A. 두 라이브러리는 용도가 다릅니다.

- **react-toastify (adminToast)**: 입력값 검증 실패나 간단 안내처럼 **사용자 흐름을 막지 않는** 알림. 우측 상단에 잠깐 표시되고 자동 소멸.
- **SweetAlert2 (adminConfirm, adminAlert, adminSuccess)**: 저장/삭제 전 확인 다이얼로그처럼 **사용자 응답이 필요한** 중요 알림. 화면 중앙에 표시되어 집중을 요구.

상황에 맞는 알림 방식을 선택함으로써 UX를 개선했습니다. `adminConfirm`은 `Promise<boolean>`을 반환하여 `await` 패턴으로 자연스럽게 사용할 수 있습니다.

---

**Q18. Vite를 빌드 도구로 선택한 이유는?**

A. Create React App(CRA)은 내부적으로 Webpack을 사용하는데, 대규모 프로젝트에서 개발 서버 시작 시간이 길어지는 단점이 있습니다. Vite는 esbuild 기반으로 개발 서버를 ESM(ES Module) 방식으로 구동하여 시작 속도가 매우 빠릅니다.

또한 Vite의 `proxy` 설정이 간단하여 개발 중 CORS 없이 Spring Boot와 통신하기 편리합니다. `base: '/admin/'` 설정 하나로 빌드 결과물 경로를 Spring Boot 서빙 경로에 맞출 수 있는 것도 장점입니다.

---

### [인프라 / 운영]

---

**Q19. GitHub Actions CI/CD 파이프라인은 어떻게 구성했나요?**

A. 두 개의 워크플로우로 구성했습니다.

- `deploy-dev.yml`: `develop` 브랜치 푸시 시 자동 실행. Gradle 빌드 → SSH로 JAR 전송 → systemd 재시작 → `/actuator/health` 헬스체크.
- `deploy-prod.yml`: 수동 트리거(`workflow_dispatch`) 또는 main 브랜치 머지 시 실행. 동일 흐름에 추가 확인 단계.

`/actuator/health`를 CI/CD 파이프라인의 최종 검증 단계로 사용하여, 배포 후 서버가 정상 기동됐는지 자동으로 확인합니다.

---

**Q20. Naver Object Storage를 S3 SDK로 사용하는 방법은?**

A. NCP Object Storage는 AWS S3와 호환되는 API를 제공합니다. AWS SDK v2에서 `S3Client` 빌더의 `endpointOverride`로 NCP endpoint를 지정하면 동일하게 사용할 수 있습니다.

```java
S3Client.builder()
    .endpointOverride(URI.create("https://kr.object.ncloudstorage.com"))
    .region(Region.of("kr-standard"))
    .credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create(accessKey, secretKey)
    ))
    .build();
```

파일 업로드, 다운로드, 삭제 모두 AWS SDK v2 표준 API를 그대로 사용합니다. 이로 인해 나중에 AWS S3로 전환하더라도 endpoint URL과 인증 정보만 변경하면 됩니다.

---

**Q21. 개발 서버와 운영 서버의 설정을 어떻게 분리하나요?**

A. Spring Boot의 외부 설정 파일 우선순위를 활용합니다.

JAR 내부 `application.properties`에는 공통 기본값을 두고, 서버별 민감값(DB 접속정보, API 키 등)은 서버의 외부 경로(`/opt/cali/application-{profile}.properties`)에만 두고 git에 커밋하지 않습니다.

```
우선순위 (높음 → 낮음):
/opt/cali/application-dev.properties   ← 서버 외부 설정 (git 제외)
/opt/cali/application.properties       ← 서버 공통 외부 설정
JAR 내부 application.properties        ← 공통 기본값
```

실행 시 `--spring.profiles.active=dev`를 지정하면 `application-dev.properties`를 자동으로 로드합니다.

---

### [설계 / 아키텍처 철학]

---

**Q22. 프로젝트에서 가장 어려웠던 부분은 무엇이고, 어떻게 해결했나요?**

A. Spring Security와 React SPA를 세션 기반으로 통합하는 과정이 가장 어려웠습니다. Spring Security의 기본 미인증 처리가 HTML 리다이렉트이므로, React fetch API가 HTML을 받아 파싱 오류가 발생했습니다.

해결을 위해 Spring Security의 예외 처리 흐름을 `@ControllerAdvice`와 `AuthenticationEntryPoint`로 구분하여 이해했고, API 경로(`/api/**`)에는 JSON 401을, 일반 경로에는 리다이렉트를 반환하도록 `AuthenticationEntryPoint`를 커스터마이징했습니다.

이 과정에서 Spring Security 필터 체인의 동작 방식, `DispatcherServlet`과의 관계, 예외 전파 경로를 깊이 이해하게 됐습니다.

---

**Q23. 이 프로젝트에서 개선하고 싶은 부분이 있다면?**

A. 크게 세 가지입니다.

1. **메뉴 권한 정책**: 현재 재로그인 시에만 권한 변경이 반영됩니다. 실시간 반영이 필요하다면 권한 변경 시 해당 사용자의 세션을 서버에서 강제 만료시키는 방식(Spring Session + SessionRegistry)을 고려하겠습니다.

2. **성적서 작업서버 연동**: 현재 폴링 방식이지만, WebSocket이나 Server-Sent Events(SSE)로 전환하면 실시간 진행 상태를 더 효율적으로 전달할 수 있습니다.

3. **테스트 코드**: 현재 비즈니스 로직에 대한 단위 테스트가 부족합니다. Service 레이어 중심으로 `@SpringBootTest`보다 `@ExtendWith(MockitoExtension.class)` 기반 단위 테스트를 추가하여 빌드 속도와 테스트 신뢰도를 높이고 싶습니다.

---

**Q24. 하이브리드(SSR + SPA) 구조를 선택한 이유는?**

A. 완전한 SPA나 완전한 SSR 대신 하이브리드를 선택한 이유는 두 화면의 요구사항이 다르기 때문입니다.

- **일반 업무 화면(SSR)**: 교정신청, 성적서 관리 등은 복잡한 폼과 파일 다운로드가 중심입니다. Thymeleaf SSR이 초기 로딩 속도, SEO(내부 시스템이므로 불필요하지만), 서버 사이드 렌더링 자체가 더 자연스럽습니다.

- **관리자 화면(React SPA)**: 직원 관리, 권한 관리 등은 동적 인터랙션이 많고 상태 관리가 복잡합니다. React의 컴포넌트 기반 구조가 적합합니다.

단일 Spring Boot 서버가 두 방식을 모두 처리하므로 인프라 복잡도를 낮추면서 화면 특성에 맞는 기술을 사용할 수 있습니다.

---

**Q25. 프로젝트를 진행하며 Spring Security를 가장 깊이 이해한 부분은 어디인가요?**

A. 예외 처리 계층 구조를 이해하는 것이었습니다.

처음에는 `@RestControllerAdvice`로 모든 예외를 처리할 수 있다고 생각했습니다. 하지만 실제로는:
- Security 필터에서 발생하는 `AuthenticationException`과 `AccessDeniedException`은 `@ControllerAdvice`가 잡지 못합니다.
- `MaxUploadSizeExceededException`은 DispatcherServlet 진입 전에 발생하지만, Spring Boot가 이를 전파하여 `@RestControllerAdvice`에서 잡힌다는 것도 버전에 따라 다릅니다.

이를 해결하면서 Spring의 요청 처리 파이프라인(Filter → DispatcherServlet → HandlerMapping → Controller)과 각 단계에서의 예외 전파 경로를 명확히 이해하게 됐습니다.

---

**Q26. 메뉴 접근 제어를 Spring Security와 별도 MVC 인터셉터로 분리한 이유는?**

A. Spring Security는 **URL 패턴 기반** 접근 제어에 특화되어 있습니다. 반면 메뉴 접근 제어는 DB에 저장된 메뉴 목록과 사용자별 권한을 동적으로 비교해야 합니다.

이를 Security FilterChain에 구현하면 동적 DB 조회와 Security 설정 코드가 결합되어 변경이 어렵고 테스트하기 힘들어집니다.

MVC 인터셉터(`MenuAccessInterceptor`)로 분리하면:
1. Security(인증/기본 인가)와 비즈니스 권한(메뉴 접근)의 관심사를 분리합니다.
2. 인터셉터는 Spring 빈으로 등록되어 DI가 쉽습니다.
3. 메뉴 캐시(`ConcurrentMapCacheManager`)와 결합하여 성능도 확보했습니다.

---

**Q27. `@ControllerAdvice`의 `basePackages`를 지정한 이유는?**

A. `GlobalViewNameAdvice`는 SSR 페이지의 공통 모델을 세팅하는 `@ControllerAdvice`입니다. `@ControllerAdvice`는 기본적으로 모든 컨트롤러에 적용됩니다.

`basePackages = "com.bada.cali.controller"`를 지정하지 않으면 REST API 컨트롤러(`api/` 패키지)에도 적용되어, JSON 응답에 불필요한 Thymeleaf 관련 Model 속성이 추가되거나 성능 저하가 발생할 수 있습니다.

반면 `GlobalApiExceptionHandler`는 `@RestControllerAdvice(annotations = RestController.class)`로 `@RestController`가 붙은 클래스에만 적용되도록 제한했습니다.

---

**Q28. 채번 시퀀스를 DB AUTO_INCREMENT 대신 커스텀으로 구현한 이유는?**

A. 교정신청서 채번은 단순 증가가 아닌 **연도+순번 조합** 형식이 필요합니다. (예: `2026-001`, `2026-002`)

DB `AUTO_INCREMENT`는 전역 증가 번호만 제공하므로 이 형식을 만족하지 못합니다. 커스텀 시퀀스 테이블(`cali_order_seq`)을 두어 연도별로 번호를 관리하고, `SELECT FOR UPDATE`로 동시성을 보장했습니다.

---

**Q29. 출장일정의 출장자를 별도 테이블(N:M) 대신 콤마 구분 문자열로 저장한 이유는?**

A. 현재 출장자 정보는 조회 시 이름 표시 외에 복잡한 조인이 필요하지 않습니다.

N:M 테이블을 만들면 등록/수정 시 관계 테이블을 별도로 관리해야 하고 코드가 복잡해집니다. 현재 규모에서는 `GROUP_CONCAT(FIND_IN_SET)` 방식으로 충분합니다.

단, 출장자별 이력 조회, 특정 직원의 출장 내역 조회처럼 출장자 기반 쿼리가 복잡해진다면 N:M 테이블로 정규화를 고려해야 합니다. 현재는 기능 우선으로 단순한 방식을 선택했습니다.

---

**Q30. 이 프로젝트를 통해 가장 크게 성장한 부분은?**

A. Spring Security의 동작 원리와 실제 비즈니스 요구사항(세션 인증 + SPA + SSR 혼용)에 맞게 커스터마이징하는 능력입니다.

교과서적인 설정(`.formLogin().permitAll()`)을 넘어서, 미인증 시 API와 일반 페이지를 구분하여 다르게 처리하고, 메뉴 단위 접근 제어를 DB 기반으로 구현하고, Remember-Me를 DB 영속 토큰으로 관리하는 등 실제 서비스 수준의 인증/인가 시스템을 직접 설계하고 구현해봤습니다.

이 과정에서 "왜 이렇게 동작하는가"를 이해하기 위해 Spring Security 소스코드를 직접 확인하고, 예외 처리 흐름을 디버깅하며 학습한 것이 큰 성장이었습니다.

---

---

## 17. CS / 백엔드(Spring) / 프론트엔드(React·JS) / 네트워크 / 자료구조 파트별 예상 질문

---

### Part A — CS 기본 (운영체제 · 메모리 · 컴퓨터 구조)

---

**A1. 프로세스(Process)와 스레드(Thread)의 차이는?**

A. 프로세스는 OS가 메모리에 올려 실행하는 프로그램의 인스턴스입니다. 독립된 메모리 공간(Code, Data, Heap, Stack)을 가집니다.

스레드는 프로세스 내에서 실행되는 실행 단위입니다. 같은 프로세스 내 스레드는 Code·Data·Heap 영역을 공유하고, Stack만 각자 가집니다.

**메모리 공유로 인한 차이**:
- 스레드 간 통신은 공유 메모리를 직접 사용하므로 빠르지만, 동기화(Race Condition) 문제가 생깁니다.
- 프로세스 간 통신(IPC)은 파이프, 소켓, 공유 메모리 등 별도 메커니즘이 필요합니다.

Spring Boot에서는 Tomcat이 요청마다 스레드를 할당합니다. 스레드 풀 크기(`server.tomcat.threads.max`, 기본 200)가 동시 처리 가능 요청 수를 결정합니다.

---

**A2. 교착상태(Deadlock)란 무엇이고, 발생 조건 4가지는?**

A. 두 개 이상의 프로세스/스레드가 서로 상대방이 점유한 자원을 기다리며 무한 대기하는 상태입니다.

**발생 조건 4가지 (Coffman 조건)**:
1. **상호 배제(Mutual Exclusion)**: 자원을 한 번에 하나만 사용
2. **점유 대기(Hold and Wait)**: 자원을 점유한 채로 다른 자원을 기다림
3. **비선점(No Preemption)**: 강제로 자원을 빼앗을 수 없음
4. **순환 대기(Circular Wait)**: A→B, B→C, C→A 형태의 순환 의존

**예방**: 4가지 조건 중 하나를 제거. (예: 자원에 순서를 부여하여 순환 대기 방지)
**탐지 및 복구**: 주기적으로 탐지하고 프로세스를 종료하거나 자원을 선점.

DB에서의 교착상태: CALI의 채번 시퀀스처럼 `SELECT FOR UPDATE`를 사용할 때, 두 트랜잭션이 서로 다른 순서로 락을 획득하면 데드락이 발생할 수 있습니다. 접근 순서를 일관되게 유지하는 것이 예방법입니다.

---

**A3. 쿠키(Cookie)와 세션(Session)의 차이는?**

A. 둘 다 HTTP의 무상태(Stateless) 특성을 보완하기 위해 상태 정보를 유지하는 방법입니다.

| 항목 | 쿠키 | 세션 |
|------|------|------|
| 저장 위치 | 클라이언트(브라우저) | 서버 메모리/DB |
| 보안 | 노출·위변조 위험 | 서버에 저장되므로 상대적으로 안전 |
| 크기 제한 | 약 4KB | 서버 메모리 한도 |
| 만료 | 설정한 시각/브라우저 종료 시 | 서버 타임아웃 또는 명시적 삭제 |
| 서버 부하 | 없음 | 세션 수만큼 서버 메모리 사용 |

**CALI의 방식**: `JSESSIONID` 쿠키에 세션 ID를 저장하고, 서버 메모리에 세션 데이터(CustomUserDetails)를 유지합니다. Remember-Me는 `persistent_logins` DB 테이블에 토큰을 저장하고, 30일 유효한 쿠키로 발급합니다.

---

**A4. 동기(Synchronous)와 비동기(Asynchronous), 블로킹(Blocking)과 논블로킹(Non-blocking)의 차이는?**

A. 많이 혼동하는 개념입니다. 두 축이 독립적으로 존재합니다.

**동기/비동기 — 결과를 기다리는 주체**
- **동기**: 호출한 쪽이 결과가 올 때까지 기다림 (호출 흐름이 결과에 종속)
- **비동기**: 결과를 나중에 콜백·이벤트·폴링으로 받음 (호출 후 바로 다음 작업)

**블로킹/논블로킹 — 제어권 반환 여부**
- **블로킹**: 호출된 함수가 작업을 마칠 때까지 제어권을 돌려주지 않음
- **논블로킹**: 호출 즉시 제어권을 돌려줌 (작업 완료 여부와 무관)

**조합**:
- 동기+블로킹: 일반 메서드 호출
- 비동기+논블로킹: Node.js 이벤트 루프, Java NIO
- 비동기+블로킹: 잘못된 설계 (의미가 없음)

CALI의 cali-worker 폴링은 **비동기+블로킹의 경계**입니다. 프론트엔드 관점에서는 비동기(결과를 나중에 받음)이지만, 폴링 자체는 주기적 동기 조회입니다.

---

**A5. 스택(Stack)과 힙(Heap) 메모리의 차이는?**

A. 둘 다 RAM 영역이지만 할당 방식과 생명주기가 다릅니다.

| 항목 | 스택 | 힙 |
|------|------|-----|
| 할당 | 컴파일 타임, 자동 | 런타임, 수동(Java: GC) |
| 크기 | 작음 (수 MB) | 큼 (수 GB) |
| 속도 | 빠름 (SP 레지스터 증감) | 상대적으로 느림 |
| 저장 내용 | 지역 변수, 메서드 호출 정보 | 객체, 배열 |
| 생명주기 | 메서드 종료 시 자동 해제 | GC가 참조가 없을 때 회수 |

Java에서 `new`로 생성한 객체는 힙에, 기본 타입(int, boolean)과 참조 변수는 스택에 저장됩니다. `StackOverflowError`는 재귀 호출이 너무 깊어 스택이 넘칠 때 발생합니다.

---

**A6. 가비지 컬렉션(GC)이란 무엇이고, Java의 GC 동작 방식은?**

A. GC는 더 이상 사용되지 않는 힙 메모리 객체를 자동으로 회수하는 메커니즘입니다. 개발자가 직접 메모리를 해제하지 않아도 됩니다.

**Java의 세대별 GC (Generational GC)**:
```
힙 구조:
├── Young Generation
│   ├── Eden (새 객체 생성)
│   ├── Survivor 0
│   └── Survivor 1
└── Old Generation (오래된 객체)
```

1. 새 객체 → Eden에 할당
2. Eden 가득 차면 **Minor GC**: 살아남은 객체 → Survivor, 죽은 객체 제거
3. Survivor를 여러 번 살아남은 객체 → Old Generation으로 승격(Promotion)
4. Old Generation 가득 차면 **Major GC(Full GC)**: 전체 힙 탐색 (STW 시간이 길어 성능에 영향)

**Stop-The-World(STW)**: GC 실행 중 모든 애플리케이션 스레드가 일시 중지. 짧을수록 좋음.

Spring Boot 3.x에서는 기본 GC로 **G1GC**를 사용합니다. 힙을 동일 크기의 Region으로 나누어 STW를 최소화합니다.

---

**A7. 데이터베이스 트랜잭션의 ACID 속성은?**

A.
- **Atomicity(원자성)**: 트랜잭션 내 모든 연산은 전부 성공하거나 전부 실패합니다. (All or Nothing)
- **Consistency(일관성)**: 트랜잭션 전후 DB는 항상 일관된 상태를 유지합니다. (무결성 제약 유지)
- **Isolation(격리성)**: 동시 실행 트랜잭션들이 서로 간섭하지 않습니다. (격리 수준에 따라 다름)
- **Durability(지속성)**: 커밋된 트랜잭션의 결과는 시스템 오류 후에도 유지됩니다. (WAL 로그 등)

**트랜잭션 격리 수준** (낮음 → 높음):
1. **READ UNCOMMITTED**: Dirty Read 허용 (커밋 전 데이터 읽기)
2. **READ COMMITTED**: Dirty Read 방지, Non-Repeatable Read 허용
3. **REPEATABLE READ**: Non-Repeatable Read 방지, Phantom Read 허용 (MySQL InnoDB 기본)
4. **SERIALIZABLE**: 완전 격리, 성능 저하

CALI는 MySQL InnoDB 기본값인 **REPEATABLE READ**를 사용합니다. 채번 시퀀스 동시성은 애플리케이션 레벨에서 비관적 락으로 추가 보장합니다.

---

**A8. DB 인덱스(Index)란 무엇이고, B-Tree 인덱스의 동작 원리는?**

A. 인덱스는 테이블 조회 속도를 높이기 위해 특정 컬럼에 만드는 별도의 자료구조입니다. 책의 색인과 같습니다.

**B-Tree 인덱스**:
- 균형 이진 탐색 트리의 변형으로, 각 노드에 여러 키와 자식 포인터를 가집니다.
- 탐색, 삽입, 삭제 모두 O(log N) 시간 복잡도
- `=`, `BETWEEN`, `<`, `>`, `ORDER BY` 등에 효과적

**인덱스 단점**:
- 삽입/수정/삭제 시 인덱스도 업데이트 → 쓰기 성능 저하
- 추가 저장 공간 필요
- 카디널리티(고유값 수)가 낮은 컬럼(예: Y/N 컬럼)에는 효과 없음

CALI의 `business_trip` 테이블에는 `(start_datetime, end_datetime)` 복합 인덱스와 `create_member_id` 인덱스를 추가했습니다. 캘린더 기간 조회와 작성자 기반 조회가 빈번하기 때문입니다.

---

**A9. N+1 문제란 무엇이고, 어떻게 해결하나요?**

A. N+1 문제는 ORM에서 목록 조회 쿼리(1번) 후, 각 항목의 연관 엔티티를 개별 조회(N번)하여 총 N+1번 쿼리가 실행되는 문제입니다.

```java
// 예시: 100개 주문 조회 후 각 주문의 회원 이름 접근
List<Order> orders = orderRepository.findAll(); // 쿼리 1번
orders.forEach(o -> o.getMember().getName());   // 쿼리 100번 → 총 101번
```

**해결 방법**:
1. **Fetch Join**: `SELECT o FROM Order o JOIN FETCH o.member` → 1번 쿼리로 해결
2. **@EntityGraph**: 메서드 단위로 Fetch Join 지정
3. **Projection 인터페이스**: 필요한 컬럼만 조회, Lazy 로딩 없음 (CALI에서 주로 사용)
4. **Batch Size 설정**: `@BatchSize` → IN 절로 N개를 한 번에 조회

CALI에서는 목록 조회에 Projection + Native Query를 우선 사용하여 N+1 자체가 발생하지 않도록 설계했습니다.

---

**A10. OS의 페이지 교체 알고리즘(LRU 등)이란?**

A. 가상 메모리에서 물리 메모리가 부족할 때 어떤 페이지를 교체할지 결정하는 알고리즘입니다.

- **FIFO**: 가장 먼저 들어온 페이지 교체. 구현 간단하지만 오래된 페이지가 자주 쓰일 수 있음.
- **LRU (Least Recently Used)**: 가장 오랫동안 사용되지 않은 페이지 교체. 실제로 가장 많이 사용.
- **LFU (Least Frequently Used)**: 사용 빈도가 가장 낮은 페이지 교체.
- **OPT (Optimal)**: 앞으로 가장 오랫동안 사용되지 않을 페이지 교체. 이론상 최적이지만 미래 예측 불가.

**실무 연관**: Redis, Caffeine Cache 등 캐시 라이브러리의 Eviction Policy가 LRU를 많이 사용합니다. CALI의 `ConcurrentMapCacheManager`는 별도 Eviction 정책이 없으므로 메뉴 데이터가 늘어날 경우 Caffeine Cache로 교체를 고려할 수 있습니다.

---

**A11. 컴파일러와 인터프리터의 차이, JVM의 JIT 컴파일이란?**

A.
- **컴파일러**: 소스 코드 전체를 한 번에 기계어로 변환 (C, C++). 실행 전 번역 완료, 빠름.
- **인터프리터**: 소스 코드를 줄 단위로 번역하며 실행 (Python, JS). 별도 컴파일 불필요, 상대적으로 느림.

**JVM의 JIT (Just-In-Time) 컴파일**: Java는 `.java` → 바이트코드(`.class`) → JVM이 실행하는 혼합 방식입니다.
JVM은 초기에 인터프리터로 바이트코드를 실행하다가, 자주 실행되는 Hot Path를 감지하면 **JIT 컴파일러**로 네이티브 기계어로 컴파일하여 캐시합니다. 이후 해당 코드는 네이티브 속도로 실행됩니다.

Spring Boot 애플리케이션이 처음 기동 후 일정 시간이 지나면 빨라지는 이유가 JIT 워밍업 때문입니다.

---

**A12. 동시성(Concurrency)과 병렬성(Parallelism)의 차이는?**

A.
- **동시성**: 여러 작업이 빠르게 번갈아 실행되어 동시에 실행되는 것처럼 보임. 단일 코어에서도 가능.
- **병렬성**: 실제로 동시에 여러 코어에서 각기 다른 작업을 실행. 멀티 코어 필수.

비유: 동시성은 한 사람이 여러 일을 빠르게 왔다 갔다 처리, 병렬성은 여러 사람이 각자 다른 일을 동시에 처리.

**Java에서**:
- 스레드는 동시성을 지원 (단일 코어에서도 컨텍스트 스위칭)
- `parallelStream()`, `ForkJoinPool`은 병렬성 활용

CALI의 Tomcat 스레드 풀은 동시성으로 여러 요청을 처리합니다. `@Async`가 적용된 파일 업로드 후처리는 별도 스레드 풀에서 비동기로 실행됩니다.

---

**A13. 함수형 인터페이스와 람다 표현식 (Java 8+)이란?**

A. **함수형 인터페이스**는 추상 메서드가 하나인 인터페이스입니다. `@FunctionalInterface` 어노테이션으로 명시합니다. (`Runnable`, `Callable`, `Comparator`, `Predicate<T>`, `Function<T,R>` 등)

**람다 표현식**: 익명 함수를 간결하게 작성하는 문법.
```java
// 기존 익명 클래스
Comparator<String> c = new Comparator<String>() {
    @Override
    public int compare(String a, String b) { return a.compareTo(b); }
};

// 람다
Comparator<String> c = (a, b) -> a.compareTo(b);
// 메서드 참조
Comparator<String> c = String::compareTo;
```

CALI 서비스 코드에서 Stream + 람다를 적극 활용합니다.
```java
// Projection 목록을 DTO로 변환
return rows.stream()
    .map(row -> new BusinessTripDTO.CalendarEventRes(...))
    .collect(Collectors.toList());
```

---

**A14. Optional이란? NullPointerException을 어떻게 줄이나?**

A. `Optional<T>`는 Java 8에서 도입된 Null을 명시적으로 표현하는 컨테이너 타입입니다. Null을 반환하는 대신 `Optional.empty()`를 반환하여 NPE를 방지합니다.

```java
// ❌ NPE 위험
Member member = memberRepository.findById(id);
member.getName();  // member가 null이면 NPE

// ✅ Optional 활용
Optional<Member> opt = memberRepository.findById(id);
opt.orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다."));
```

**주요 메서드**:
- `orElse(T other)`: 비어있으면 other 반환
- `orElseGet(Supplier)`: 비어있으면 Supplier 실행 결과 반환
- `orElseThrow(Supplier)`: 비어있으면 예외 던짐
- `ifPresent(Consumer)`: 값이 있으면 Consumer 실행
- `map(Function)`: 값이 있으면 변환

CALI에서는 `findById()` 결과에 `.orElseThrow(() -> new EntityNotFoundException(...))`을 일관되게 사용하여 `GlobalApiExceptionHandler`에서 404를 반환합니다.

---

**A15. 디자인 패턴 중 자주 쓰이는 것을 설명해보세요. (싱글톤, 팩토리, 전략 패턴)**

A.
**싱글톤(Singleton)**: 클래스 인스턴스가 오직 하나. Spring 빈의 기본 스코프가 싱글톤입니다. `@Service`, `@Repository`, `@Controller`로 등록된 빈은 기본적으로 단 하나의 인스턴스를 모든 요청에서 공유합니다.

**팩토리(Factory)**: 객체 생성 로직을 별도로 분리. 어떤 구체 클래스를 생성할지 결정을 캡슐화. Spring의 `@Bean` 메서드가 팩토리 패턴입니다.

**전략(Strategy)**: 알고리즘을 인터페이스로 정의하고, 구체 구현을 런타임에 교체 가능하게 함. Spring Security의 `AuthenticationSuccessHandler`, `AuthenticationFailureHandler`가 전략 패턴입니다. CALI에서 `LoginSuccessHandler`와 `LoginFailureHandler`를 별도 클래스로 주입받는 것이 이에 해당합니다.

**빌더(Builder)**: 복잡한 객체를 단계적으로 생성. Lombok `@Builder`가 자동 생성. CALI Entity 생성 시 `BusinessTrip.builder().title(req.title()).build()` 형태로 사용.

---

### Part B — 네트워크

---

**B1. HTTP와 HTTPS의 차이는? SSL/TLS 핸드셰이크 과정은?**

A. **HTTP**는 평문 통신, **HTTPS**는 SSL/TLS 레이어를 추가하여 암호화·인증·무결성을 보장합니다.

**TLS 핸드셰이크 (간략)**:
1. **Client Hello**: 클라이언트가 지원하는 암호화 알고리즘(Cipher Suite) 목록 전송
2. **Server Hello**: 서버가 알고리즘 선택 + 인증서(공개키 포함) 전송
3. **인증서 검증**: 클라이언트가 CA 체인으로 서버 인증서 유효성 확인
4. **키 교환**: Pre-master secret 생성 후 서버 공개키로 암호화하여 전송
5. **세션 키 생성**: 양측이 Pre-master secret으로 동일한 대칭키 생성
6. **완료**: 이후 대칭키로 암호화 통신

CALI는 Nginx에서 SSL Termination을 처리합니다. 브라우저 ↔ Nginx는 HTTPS, Nginx ↔ Spring Boot는 내부 HTTP(보안 영역 내부이므로 허용).

---

**B2. HTTP/1.1, HTTP/2, HTTP/3의 차이는?**

A.
**HTTP/1.1**: 요청-응답이 순차적. 하나의 TCP 연결에서 한 번에 하나의 요청만 처리(Head-of-Line Blocking). Keep-Alive로 연결 재사용은 가능하지만 파이프라인 사용이 제한적.

**HTTP/2**:
- 단일 TCP 연결에서 여러 요청을 동시에 처리(**멀티플렉싱**). Head-of-Line Blocking 해소.
- **헤더 압축(HPACK)**: 반복되는 헤더 전송량 감소.
- **Server Push**: 클라이언트 요청 없이 서버가 리소스 미리 전송.
- 이진(Binary) 프레이밍.

**HTTP/3**:
- TCP 대신 **QUIC(UDP 기반)** 사용. TCP의 Head-of-Line Blocking(패킷 손실 시 전체 대기)을 완전히 해소.
- 0-RTT 연결: 이전 연결 정보로 핸드셰이크 없이 바로 데이터 전송.
- 모바일 환경의 잦은 네트워크 전환(Wi-Fi → LTE)에서 연결 유지.

---

**B3. REST API 설계 원칙(RESTful)이란?**

A. REST(Representational State Transfer)는 HTTP를 최대한 활용한 아키텍처 스타일입니다.

**6가지 제약 조건**:
1. **클라이언트-서버 분리**: UI와 데이터 저장 로직을 분리
2. **무상태(Stateless)**: 각 요청은 독립적, 서버는 클라이언트 상태를 저장하지 않음
3. **캐시 가능**: 응답에 캐시 가능 여부를 명시할 수 있음
4. **균일한 인터페이스**: 자원(URI), 표현, 자기 기술 메시지, HATEOAS
5. **계층화 시스템**: 클라이언트는 중간 서버(프록시, 게이트웨이) 유무를 몰라도 됨
6. **Code-on-Demand(선택)**: 서버가 실행 가능한 코드(JS)를 전송할 수 있음

**RESTful URI 설계**:
- 자원은 명사: `/users/{id}` (동사 사용 금지: `/getUser`)
- HTTP 메서드로 행위 표현: GET(조회), POST(생성), PATCH(수정), DELETE(삭제)
- 복수형: `/users`, `/reports`
- 계층 표현: `/users/{id}/reports`

CALI는 이 원칙을 따르되, `/api/admin/businessTrip/checkConflict`처럼 RPC성 액션은 명사형 자원으로 표현하기 어려워 예외적으로 동사를 허용했습니다.

---

**B4. GET과 POST의 차이는? 멱등성(Idempotency)이란?**

A.
| 항목 | GET | POST |
|------|-----|------|
| 목적 | 데이터 조회 | 데이터 생성/처리 |
| 파라미터 위치 | URL Query String | Request Body |
| 캐시 | 가능 | 불가 |
| 브라우저 히스토리 | 저장 | 저장 안 됨 |
| 멱등성 | O | X |
| 안전성(Side Effect 없음) | O | X |

**멱등성(Idempotency)**: 같은 요청을 여러 번 보내도 결과가 동일한 성질.
- GET, PUT, DELETE: 멱등 (여러 번 해도 결과 같음)
- POST: 비멱등 (호출할 때마다 새 자원 생성)
- PATCH: 상황에 따라 다름 (특정 필드를 특정 값으로 설정하는 경우 멱등, 카운터 증가는 비멱등)

---

**B5. 브라우저에서 URL을 입력하면 화면이 표시되기까지의 과정은?**

A.
1. **URL 파싱**: 브라우저가 프로토콜, 호스트, 경로 분리
2. **DNS 조회**: 호스트명 → IP 주소 변환 (브라우저 캐시 → OS 캐시 → DNS 서버 순으로 조회)
3. **TCP 연결**: 3-Way Handshake (SYN → SYN-ACK → ACK)
4. **TLS 핸드셰이크** (HTTPS): 인증서 검증, 대칭키 협상
5. **HTTP 요청 전송**: GET / HTTP/1.1
6. **서버 처리**: Nginx → Spring Boot → DB 조회 → 응답 생성
7. **HTTP 응답 수신**: HTML + 상태코드
8. **HTML 파싱 & DOM 생성**: HTML → DOM 트리
9. **CSS 파싱 & CSSOM 생성**: CSS → CSSOM 트리
10. **Render Tree 생성**: DOM + CSSOM 결합
11. **Layout(Reflow)**: 각 요소의 위치/크기 계산
12. **Paint**: 픽셀로 그리기
13. **Composite**: 레이어 합성, 화면에 표시
14. **JS 실행**: `<script>` 태그 순서로 실행 (async/defer로 조정 가능)

---

**B6. CORS(Cross-Origin Resource Sharing)란? 어떻게 해결하나?**

A. 브라우저의 **Same-Origin Policy**는 다른 Origin(프로토콜+호스트+포트가 하나라도 다른 경우)으로의 요청을 기본적으로 차단합니다.

CORS는 서버가 허용하는 Origin을 명시하여 브라우저가 차단을 해제하도록 하는 메커니즘입니다.

**Preflight 요청**: `Content-Type: application/json`처럼 Simple Request가 아닌 경우, 실제 요청 전 `OPTIONS` 요청을 보내 서버의 허용 여부를 확인합니다.

```http
OPTIONS /api/data
Origin: https://frontend.com
Access-Control-Request-Method: POST

→ 서버 응답:
Access-Control-Allow-Origin: https://frontend.com
Access-Control-Allow-Methods: POST
Access-Control-Allow-Credentials: true
```

**CALI에서의 CORS 처리**:
- 개발 환경: Vite dev proxy가 `/api` 요청을 Spring Boot로 포워딩 → Same-Origin이 되어 CORS 없음
- 운영 환경: React가 빌드 후 Spring Boot에서 서빙 → 동일 Origin → CORS 없음
- 별도 Spring CORS 설정 불필요

---

**B7. TCP와 UDP의 차이는? 언제 각각 사용하나?**

A.
| 항목 | TCP | UDP |
|------|-----|-----|
| 연결 | 연결 지향 (3-Way Handshake) | 비연결 |
| 신뢰성 | 순서 보장, 재전송, 오류 감지 | 보장 안 함 |
| 속도 | 상대적으로 느림 | 빠름 |
| 헤더 크기 | 20 bytes | 8 bytes |

**TCP 사용**: HTTP/HTTPS, 이메일, 파일 전송 (데이터 무결성이 중요한 경우)
**UDP 사용**: 실시간 스트리밍, 게임, DNS, HTTP/3(QUIC) (속도가 중요하고 약간의 손실이 허용되는 경우)

---

**B8. JWT(JSON Web Token)란? 구조와 장단점은?**

A. JWT는 JSON 데이터를 Base64URL 인코딩하여 서명한 토큰입니다. 서버가 상태를 저장하지 않고 인증 정보를 클라이언트에 위임합니다.

**구조** (`{Header}.{Payload}.{Signature}`):
```
eyJhbGciOiJIUzI1NiJ9.       ← Header (알고리즘)
eyJzdWIiOiIxMjM0In0.        ← Payload (사용자 데이터)
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c  ← Signature
```

**장점**: 무상태(Stateless) → 서버 확장 용이, 서버 간 인증 공유 쉬움
**단점**: 토큰 탈취 시 만료 전까지 무효화 어려움 (Blacklist 필요), Payload 크기 제한

**세션 vs JWT 선택 기준**: CALI는 단일 서버 + 세션 인증을 선택했습니다. MSA나 수평 확장이 필요하면 JWT가 적합합니다. (자세한 내용은 Q5 참고)

---

**B9. 로드밸런싱(Load Balancing)이란? 주요 알고리즘은?**

A. 여러 서버에 요청을 분산하여 특정 서버에 과부하가 걸리지 않게 하는 기술입니다.

**알고리즘**:
- **Round Robin**: 순서대로 배분. 가장 단순. 서버 성능이 동일할 때 적합.
- **Least Connection**: 현재 연결 수가 가장 적은 서버로 배분.
- **IP Hash**: 클라이언트 IP 기반으로 항상 같은 서버로 (세션 유지에 유리).
- **Weighted**: 서버 성능에 따라 가중치 부여.

**세션과 로드밸런싱**: 세션 기반 인증에서 로드밸런싱 시 같은 사용자의 요청이 다른 서버로 가면 세션을 찾지 못합니다. 해결책: IP Hash, **Sticky Session**, 또는 **Spring Session(Redis)**으로 세션을 외부 저장소에서 공유.

---

### Part C — Spring / Spring Boot 심화

---

**C1. Spring IoC(Inversion of Control)와 DI(Dependency Injection)란?**

A. **IoC(제어의 역전)**: 객체 생성과 의존성 관리 제어권을 개발자가 아닌 프레임워크(Spring 컨테이너)가 가져가는 원칙.

**DI(의존성 주입)**: IoC를 구현하는 방식. 객체가 필요한 의존성을 직접 생성하지 않고, 컨테이너가 주입해줍니다.

```java
// 직접 생성 (결합도 높음)
public class MemberService {
    private MemberRepository repo = new MemberRepositoryImpl(); // ❌ 직접 생성
}

// DI (결합도 낮음)
@Service
@RequiredArgsConstructor
public class MemberServiceImpl {
    private final MemberRepository memberRepository; // ✅ 컨테이너가 주입
}
```

**DI 방식 3가지**:
1. **생성자 주입** (권장): `@RequiredArgsConstructor` + `final`. 불변성, 테스트 편의성, 순환 의존성 감지.
2. **세터 주입**: 선택적 의존성에 사용.
3. **필드 주입** (`@Autowired`): 테스트 어렵고, final 사용 불가 → 지양.

CALI는 모든 서비스/컨트롤러에 Lombok `@RequiredArgsConstructor` + `final` 생성자 주입을 사용합니다.

---

**C2. Spring Boot가 Legacy Spring과 비교해 어떤 점이 편리한가요?**

A.
| 항목 | Legacy Spring | Spring Boot |
|------|--------------|-------------|
| 설정 | XML 또는 Java Config 수동 작성 | Auto-configuration으로 자동 설정 |
| 서버 | WAR 빌드 후 외부 Tomcat 배포 | 내장 Tomcat, JAR 실행 |
| 의존성 관리 | 각 라이브러리 버전 직접 관리 | Starter + BOM으로 호환 버전 자동 관리 |
| 보일러플레이트 | 많음 (web.xml, applicationContext.xml 등) | 최소화 (`application.properties`만으로 설정) |
| 운영 편의 | 없음 | Actuator (헬스체크, 메트릭, 환경 정보) 내장 |

**Auto-configuration 원리**: `@SpringBootApplication` = `@EnableAutoConfiguration` + `@ComponentScan` + `@Configuration`. 클래스패스에 특정 라이브러리가 있으면 관련 빈을 자동으로 등록합니다. (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`)

**예시**: `spring-boot-starter-data-jpa`를 추가하면 `JpaAutoConfiguration`이 자동으로 `EntityManagerFactory`, `JpaTransactionManager` 빈을 등록합니다.

---

**C3. Spring AOP란 무엇이고, 어떤 경우에 사용하나요?**

A. **AOP(Aspect-Oriented Programming)**: 핵심 비즈니스 로직과 부가 기능(로깅, 트랜잭션, 보안 등)을 분리하는 프로그래밍 패러다임입니다.

**주요 용어**:
- **Aspect**: 부가 기능 모듈 (예: 로깅 Aspect)
- **Advice**: 실행 시점 (Before, After, Around 등)
- **Pointcut**: Advice를 적용할 지점 (특정 메서드, 패키지 등)
- **JoinPoint**: Advice가 실제로 적용된 시점

```java
@Aspect
@Component
public class LoggingAspect {
    @Around("execution(* com.bada.cali.service.*.*(..))")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        log.info("[Service 호출] {}", pjp.getSignature());
        Object result = pjp.proceed();
        log.info("[Service 완료]");
        return result;
    }
}
```

**Spring에서의 AOP 활용**: `@Transactional`, `@Cacheable`, `@PreAuthorize`가 모두 AOP로 구현됩니다. `@Transactional`이 달린 메서드를 호출하면, Spring이 프록시 객체를 생성하여 트랜잭션 시작/커밋/롤백을 Around Advice로 감쌉니다.

---

**C4. `@Transactional`의 동작 원리와 주의사항은?**

A. `@Transactional`은 AOP 프록시를 통해 동작합니다. Spring 컨테이너는 `@Transactional`이 붙은 빈의 프록시를 만들고, 메서드 호출 전에 트랜잭션을 시작하고, 완료 후 커밋/롤백합니다.

**주의사항**:

1. **같은 클래스 내부 호출 문제**: 프록시를 거치지 않는 내부 호출에서는 `@Transactional`이 동작하지 않습니다.
```java
public void methodA() {
    this.methodB(); // ❌ 프록시 우회 → @Transactional 무시
}
@Transactional
public void methodB() { ... }
```

2. **`private` 메서드**: 프록시가 오버라이드할 수 없어 `@Transactional` 동작 안 함.

3. **RuntimeException vs Checked Exception**: 기본적으로 `RuntimeException`과 `Error`에서만 롤백. Checked Exception에서 롤백하려면 `@Transactional(rollbackFor = Exception.class)`.

4. **트랜잭션 전파(Propagation)**:
   - `REQUIRED` (기본): 기존 트랜잭션이 있으면 참여, 없으면 생성
   - `REQUIRES_NEW`: 항상 새 트랜잭션 생성 (기존 트랜잭션 일시 중단)
   - `NESTED`: 기존 트랜잭션 내에 중첩 트랜잭션

---

**C5. Spring Bean 스코프의 종류와 싱글톤 빈에서 상태를 저장하면 안 되는 이유는?**

A. **빈 스코프 종류**:
- `singleton` (기본): 컨테이너당 하나의 인스턴스
- `prototype`: 요청할 때마다 새 인스턴스 생성
- `request`: HTTP 요청당 하나 (Web)
- `session`: HTTP 세션당 하나 (Web)
- `application`: ServletContext당 하나 (Web)

**싱글톤 빈의 상태 저장 금지**:
`@Service`, `@Controller` 등은 기본 싱글톤으로, 모든 요청이 하나의 인스턴스를 공유합니다.
인스턴스 변수에 사용자별 데이터를 저장하면 Thread-Unsafe 상태가 됩니다.

```java
@Service
public class OrderService {
    private Member currentMember; // ❌ 싱글톤에서 상태 저장 → 스레드 간 데이터 오염

    public void process(Member member) {
        this.currentMember = member; // 다른 스레드의 currentMember를 덮어씀
    }
}
```

대신 상태는 메서드 파라미터나 지역 변수로 전달해야 합니다.

---

**C6. Spring Data JPA의 `findById()`와 `getById()`(구: `getOne()`)의 차이는?**

A.
- **`findById(id)`**: 즉시 DB를 조회하고 `Optional<T>`를 반환합니다. 없으면 `Optional.empty()`.
- **`getById(id)` (구 `getOne()`)**: Lazy 로딩. 즉시 DB를 조회하지 않고 프록시 객체를 반환합니다. 실제 필드에 접근하는 시점에 DB를 조회합니다. 없으면 `EntityNotFoundException`이 나중에 발생.

```java
// findById: 즉시 조회
Member member = memberRepository.findById(id)
    .orElseThrow(() -> new EntityNotFoundException("없음")); // 명시적 예외

// getById: 즉시는 조회 안 함 (Lazy)
Member member = memberRepository.getById(id); // 프록시 반환
member.getName(); // 이 시점에 DB 조회
```

**주의**: 트랜잭션 밖에서 `getById()`로 가져온 프록시에 접근하면 `LazyInitializationException` 발생. CALI에서는 `findById().orElseThrow()`를 일관되게 사용합니다.

---

**C7. MapStruct란? 리플렉션 기반 매퍼(ModelMapper)와의 차이는?**

A. **MapStruct**는 컴파일 타임에 Entity ↔ DTO 변환 코드를 자동 생성하는 라이브러리입니다.

```java
@Mapper(componentModel = "spring")
public interface MemberMapper {
    MemberDTO.MemberDetailRes toDetailRes(Member entity);
    Member toEntity(MemberDTO.MemberCreateReq req);
}
```
컴파일 시 `MemberMapperImpl`이 자동 생성됩니다.

**ModelMapper와의 비교**:
| 항목 | MapStruct | ModelMapper |
|------|-----------|------------|
| 코드 생성 시점 | 컴파일 타임 | 런타임 (리플렉션) |
| 성능 | 빠름 (일반 Java 코드) | 상대적으로 느림 |
| 오류 감지 | 컴파일 시 발견 | 런타임에 발견 |
| 디버깅 | 생성된 코드 직접 확인 가능 | 어려움 |
| 설정 복잡도 | 명시적 매핑 필요 | 자동 매핑 (편리하지만 예측 어려움) |

---

**C8. Spring의 `@Cacheable`은 어떻게 동작하나요? 캐시 무효화는?**

A. `@Cacheable`은 AOP 기반으로 동작합니다. 메서드 호출 시 캐시에 결과가 있으면 메서드를 실행하지 않고 캐시 값을 바로 반환합니다.

```java
@Cacheable("allVisibleMenus")           // 캐시 이름
public List<Menu> getAllVisibleMenus() { // 첫 호출 시 DB 조회, 이후 캐시 반환
    return menuRepository.findAllByIsVisible(YnType.Y);
}

@CacheEvict("allVisibleMenus")          // 캐시 무효화
public void evictMenuCache() {
    // 메뉴 데이터 변경 시 호출
}
```

CALI에서는 `ConcurrentMapCacheManager`(인메모리 ConcurrentHashMap)를 사용합니다. 서버 재시작 시 캐시가 초기화됩니다.

**주의사항**:
- 같은 클래스 내부 호출 시 AOP 프록시 우회로 `@Cacheable` 미동작
- 메뉴 데이터 변경 시 반드시 `evictMenuCache()` 호출 필요
- 분산 서버 환경에서는 Redis 등 공유 캐시 사용 필요

---

**C9. Querydsl은 무엇이고, JPQL이나 Native Query 대비 장점은?**

A. Querydsl은 Java 코드로 타입 안전하게 쿼리를 작성하는 라이브러리입니다.

```java
// JPQL (문자열, 컴파일 타임 오류 감지 불가)
@Query("SELECT m FROM Member m WHERE m.name = :name AND m.isActive = 'Y'")
List<Member> findByName(@Param("name") String name);

// Querydsl (Java 코드, IDE 지원, 타입 안전)
queryFactory
    .selectFrom(member)
    .where(member.name.eq(name).and(member.isActive.eq(YnType.Y)))
    .fetch();
```

**장점**:
1. 컴파일 타임에 오류 감지 (오타, 타입 불일치)
2. IDE 자동완성 지원
3. 동적 쿼리 작성이 자연스러움 (`BooleanBuilder`)
4. 복잡한 조인/서브쿼리를 Java 코드로 가독성 있게 작성

CALI는 JPA + Querydsl을 우선 사용하고, GROUP_CONCAT이나 FIND_IN_SET처럼 MySQL 전용 함수가 필요한 경우에만 Native Query를 사용합니다.

---

**C10. Spring의 이벤트 처리(`@EventListener`, `ApplicationEvent`)란?**

A. Spring의 이벤트 시스템은 발행-구독(Pub-Sub) 패턴으로, 컴포넌트 간 느슨한 결합을 지원합니다.

```java
// 이벤트 정의
public class MemberJoinedEvent {
    private final Long memberId;
    public MemberJoinedEvent(Long memberId) { this.memberId = memberId; }
}

// 발행
@Service
public class MemberServiceImpl {
    private final ApplicationEventPublisher publisher;
    public void join(MemberDTO.JoinReq req) {
        Member saved = memberRepository.save(...);
        publisher.publishEvent(new MemberJoinedEvent(saved.getId())); // 이벤트 발행
    }
}

// 구독
@Component
public class WelcomeEmailHandler {
    @EventListener
    public void handleJoin(MemberJoinedEvent event) {
        // 이메일 발송 처리
    }
}
```

이벤트 처리는 기본적으로 동기·같은 트랜잭션 내에서 실행됩니다. `@Async` + `@EventListener` 조합으로 비동기 처리도 가능합니다.

CALI에서는 현재 직접 이벤트를 사용하지 않지만, 로그 기록이나 알림 발송 같은 부가 작업을 서비스 로직에서 분리할 때 적용할 수 있습니다.

---

### Part D — React / JavaScript / Ajax

---

**D1. React의 Virtual DOM이란? 실제 DOM과 어떻게 다른가요?**

A. **DOM(Document Object Model)**: 브라우저가 HTML을 파싱하여 생성하는 트리 구조. DOM 조작은 브라우저 렌더링을 트리거하므로 비용이 큽니다.

**Virtual DOM**: React가 메모리에 유지하는 DOM의 가상 복사본(JavaScript 객체 트리). 실제 DOM 조작보다 훨씬 빠릅니다.

**동작 방식**:
1. 상태(state)가 변경되면 새 Virtual DOM 트리를 생성
2. 이전 Virtual DOM과 비교(**Diffing 알고리즘**)
3. 변경된 부분만 찾아(**Reconciliation**) 실제 DOM에 최소한으로 반영(**Batch Update**)

**이점**: 잦은 상태 변경 시에도 실제 DOM 조작을 최소화하여 성능 최적화.

CALI의 React Admin에서 직원 목록 그리드나 폼 상태 관리 시 이 원리로 불필요한 DOM 재렌더링을 방지합니다.

---

**D2. React Hooks — `useState`, `useEffect`, `useRef`, `useCallback`의 역할과 사용법은?**

A.

**`useState`**: 컴포넌트의 상태 관리.
```javascript
const [count, setCount] = useState(0);
// count: 현재 값, setCount: 업데이트 함수 (호출 시 리렌더링)
```

**`useEffect`**: 사이드 이펙트 처리 (데이터 조회, DOM 조작, 이벤트 구독).
```javascript
useEffect(() => {
    fetchData();          // 마운트 시 실행
    return () => cleanup(); // 언마운트 시 cleanup
}, [dependency]);        // dependency 변경 시 재실행, [] → 마운트 1회만
```

**`useRef`**: DOM 직접 접근 또는 렌더링 없이 값 보관.
```javascript
const gridRef = useRef(null);
// gridRef.current → 실제 DOM 또는 Toast UI Grid 인스턴스 접근
```

CALI의 React Admin에서 Toast UI Grid 초기화 시 `useRef`로 컨테이너 DOM을 참조합니다.

**`useCallback`**: 함수 메모이제이션. 의존성이 변경될 때만 함수를 재생성하여 자식 컴포넌트의 불필요한 리렌더링 방지.
```javascript
const handleSave = useCallback(async () => {
    await adminFetch('/api/...', { method: 'PATCH', body: JSON.stringify(form) });
}, [form]); // form이 변경될 때만 새 함수 생성
```

---

**D3. React의 컴포넌트 생명주기(Lifecycle)는?**

A. 함수형 컴포넌트에서는 `useEffect`로 생명주기를 처리합니다.

```
마운트(Mount) → 업데이트(Update) → 언마운트(Unmount)
```

| 클래스형 생명주기 | 함수형 대응 |
|-----------------|------------|
| `componentDidMount` | `useEffect(() => { ... }, [])` |
| `componentDidUpdate` | `useEffect(() => { ... }, [dep])` |
| `componentWillUnmount` | `useEffect(() => { return () => cleanup() }, [])` |

```javascript
useEffect(() => {
    // 마운트: 데이터 로드, 이벤트 구독, Toast UI Grid 초기화
    const grid = new Grid({ el: gridRef.current, ... });

    return () => {
        // 언마운트: Grid 인스턴스 정리, 이벤트 리스너 제거
        grid.destroy();
    };
}, []); // 빈 배열: 마운트/언마운트 시에만
```

---

**D4. React의 props와 state의 차이는? 언제 각각 사용하나요?**

A.
| 항목 | props | state |
|------|-------|-------|
| 소유자 | 부모 컴포넌트가 전달 | 컴포넌트 자신 |
| 변경 | 읽기 전용 (자식이 변경 불가) | `setState`/`useState`로 변경 가능 |
| 목적 | 컴포넌트 간 데이터 전달 | 컴포넌트 내부 상태 관리 |
| 변경 시 리렌더링 | 부모가 바꾸면 재렌더링 | 변경 시 해당 컴포넌트 재렌더링 |

```javascript
// 부모
<MemberRow member={member} onDelete={handleDelete} />

// 자식
function MemberRow({ member, onDelete }) { // props로 받음
    const [isExpanded, setIsExpanded] = useState(false); // 자체 state
    return <div onClick={() => setIsExpanded(!isExpanded)}>...</div>;
}
```

**단방향 데이터 흐름**: React는 부모 → 자식으로만 props가 흐릅니다. 자식에서 부모 상태를 변경하려면 부모에서 콜백 함수를 props로 내려보냅니다.

---

**D5. 이벤트 루프(Event Loop)와 JavaScript의 비동기 처리는?**

A. JavaScript는 **단일 스레드**입니다. 하지만 비동기 처리가 가능한 이유는 이벤트 루프 덕분입니다.

**동작 방식**:
```
Call Stack (실행 중인 코드)
  ↓ (비동기 작업 만나면)
Web APIs (setTimeout, fetch, DOM 이벤트 등 브라우저 처리)
  ↓ (완료 시)
Callback Queue (또는 Microtask Queue for Promise)
  ↑ Call Stack이 비면
Event Loop가 Queue에서 꺼내 Call Stack에 push
```

**Microtask vs Macrotask**:
- **Microtask** (우선순위 높음): `Promise.then`, `queueMicrotask`
- **Macrotask**: `setTimeout`, `setInterval`, `fetch 콜백`

```javascript
console.log('1');          // 동기
setTimeout(() => console.log('2'), 0);  // Macrotask
Promise.resolve().then(() => console.log('3')); // Microtask
console.log('4');          // 동기
// 출력: 1 → 4 → 3 → 2
```

---

**D6. `async/await`과 Promise의 관계는? 에러 처리는?**

A. `async/await`은 Promise를 더 읽기 쉽게 작성하는 문법 설탕(Syntactic Sugar)입니다.

```javascript
// Promise 체인
fetchUser(id)
  .then(user => fetchOrders(user.id))
  .then(orders => renderOrders(orders))
  .catch(err => showError(err));

// async/await (동일 동작)
const handleLoad = async () => {
    try {
        const user = await fetchUser(id);
        const orders = await fetchOrders(user.id);
        renderOrders(orders);
    } catch (err) {
        showError(err);
    }
};
```

**병렬 실행**: `await`을 순서대로 쓰면 순차 실행입니다. 독립적인 요청은 `Promise.all`로 병렬 처리합니다.
```javascript
// 순차 (느림)
const user = await fetchUser(id);
const menu = await fetchMenu();

// 병렬 (빠름)
const [user, menu] = await Promise.all([fetchUser(id), fetchMenu()]);
```

CALI `adminFetch`는 `async/await`과 `try/catch`를 조합하여 API 에러를 일관되게 처리합니다.

---

**D7. 클로저(Closure)란 무엇인가요?**

A. 클로저는 함수가 선언된 시점의 **렉시컬 환경(변수 범위)**을 기억하는 함수입니다. 외부 함수가 종료된 후에도 외부 변수에 접근할 수 있습니다.

```javascript
function createCounter() {
    let count = 0;          // 외부 변수
    return function() {     // 클로저 (count를 기억)
        return ++count;
    };
}
const counter = createCounter();
counter(); // 1
counter(); // 2
// count 변수는 외부에서 직접 접근 불가 → 캡슐화 효과
```

**React에서 클로저 주의사항**: `useEffect` 내부의 함수가 오래된 state 값을 캡처하는 **Stale Closure** 문제.
```javascript
useEffect(() => {
    const timer = setInterval(() => {
        console.log(count); // 클로저 캡처 시점의 count 값 (오래된 값 가능)
    }, 1000);
    return () => clearInterval(timer);
}, []); // 의존성 배열에 count 없으면 stale closure
```

---

**D8. Ajax란 무엇이고, `fetch` API와 `XMLHttpRequest`의 차이는?**

A. **Ajax(Asynchronous JavaScript and XML)**: 페이지 새로고침 없이 서버와 비동기로 데이터를 교환하는 방식. 현대에는 XML 대신 JSON을 주로 사용합니다.

**XMLHttpRequest (구식)**:
```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/members');
xhr.onreadystatechange = function() {
    if (xhr.readyState === 4 && xhr.status === 200) {
        const data = JSON.parse(xhr.responseText);
    }
};
xhr.send();
```

**fetch API (현대)**:
```javascript
fetch('/api/members')
    .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    })
    .then(data => console.log(data))
    .catch(err => console.error(err));
```

**fetch의 특징**:
- Promise 기반 (async/await 사용 가능)
- `Response.ok`로 HTTP 에러 확인 (fetch는 네트워크 오류만 reject, HTTP 4xx/5xx는 reject 안 함)
- 쿠키는 기본 미포함 (`credentials: 'include'` 필요)

CALI의 `adminFetch`는 `fetch`를 래핑하여 `res.ok` 체크 + 401 처리를 공통화했습니다.

---

**D9. ES6+에서 자주 사용하는 문법을 설명해보세요.**

A.
```javascript
// 구조 분해 할당
const { id, name } = user;
const [first, ...rest] = array;

// 전개 연산자(Spread)
const newObj = { ...existingObj, name: '변경' };
const newArr = [...arr1, ...arr2];

// 템플릿 리터럴
const msg = `안녕하세요, ${user.name}님`;

// Arrow function (this 바인딩 없음)
const double = (n) => n * 2;

// Optional chaining (?.)
const city = user?.address?.city; // user 또는 address가 null이면 undefined

// Nullish coalescing (??)
const name = user.name ?? '이름 없음'; // null/undefined만 걸러냄 (falsy 전체가 아님)

// 비동기/대기
const data = await adminFetch('/api/...');

// Promise.all
const [a, b] = await Promise.all([fetch('/a'), fetch('/b')]);

// 구조 분해 + 기본값
const { name = '기본값', age = 0 } = user;
```

---

**D10. React에서 상태 관리 방법은? Context API와 외부 라이브러리(Redux, Zustand)의 차이는?**

A.
**로컬 상태 (`useState`)**: 컴포넌트 자신의 상태. 다른 컴포넌트와 공유 불필요할 때.

**Context API**: React 내장. 컴포넌트 트리 전체에 데이터를 전달 (Props Drilling 방지).
```javascript
const AuthContext = createContext(null);

// 제공
<AuthContext.Provider value={{ user, logout }}>
    <App />
</AuthContext.Provider>

// 소비
const { user } = useContext(AuthContext);
```

**Redux**: 전역 상태를 단일 Store에서 관리. Action → Reducer → Store → 컴포넌트 업데이트. 예측 가능하지만 보일러플레이트가 많음.

**Zustand**: 가볍고 간단한 전역 상태 관리. Context API보다 리렌더링 최적화 우수.

CALI의 React Admin은 현재 각 페이지가 독립적으로 API를 호출하는 단순 구조이므로, `useState` + `useEffect`로 충분합니다. 공통 사용자 정보(isAdmin 등)가 여러 곳에서 필요해지면 Context API 도입을 고려할 수 있습니다.

---

**D11. 브라우저 렌더링 성능 최적화 방법은?**

A.
1. **Reflow/Repaint 최소화**: DOM 읽기와 쓰기를 분리. 읽기를 먼저 일괄 처리 후 쓰기.
2. **React.memo / useMemo / useCallback**: 불필요한 리렌더링과 재계산 방지.
3. **가상화(Virtualization)**: 수천 개의 리스트 항목 중 화면에 보이는 것만 렌더링 (react-window, Toast UI Grid 서버 페이지네이션).
4. **코드 스플리팅**: `React.lazy` + `Suspense`로 필요할 때만 청크 로드.
5. **이미지 최적화**: lazy loading, WebP 형식, 적절한 크기.
6. **번들 최적화**: Vite의 트리쉐이킹으로 사용하지 않는 코드 제거.

---

**D12. 로컬 스토리지(localStorage), 세션 스토리지(sessionStorage), 쿠키의 차이는?**

A.
| 항목 | localStorage | sessionStorage | 쿠키 |
|------|-------------|----------------|------|
| 크기 | ~5MB | ~5MB | ~4KB |
| 만료 | 명시적 삭제 전 영구 | 탭/브라우저 종료 시 | 설정한 만료일 |
| 서버 전송 | 안 됨 | 안 됨 | 모든 요청에 자동 포함 |
| 접근 | JS | JS | JS (HttpOnly 아니면) |
| HttpOnly | 불가 | 불가 | 가능 |

**CALI에서의 활용**:
- 인증: `JSESSIONID` 쿠키 (HttpOnly) → JS로 접근 불가, 보안
- Remember-Me: `remember-me` 쿠키 → 30일 유지
- localStorage/sessionStorage: CALI에서는 별도로 사용하지 않음 (서버 세션 의존)

---

**D13. CSR(Client Side Rendering)과 SSR(Server Side Rendering)의 차이와 각각의 장단점은?**

A.
**CSR (Client Side Rendering)**:
- 빈 HTML + JS 번들 전송 → 브라우저에서 JS 실행 → DOM 생성
- 초기 로딩 느림(TTV 느림), 이후 인터랙션 빠름
- SEO 불리 (크롤러가 빈 HTML을 볼 수 있음)
- 예: CALI의 React Admin SPA

**SSR (Server Side Rendering)**:
- 서버에서 완성된 HTML 생성하여 전송 → 브라우저 즉시 표시
- 초기 로딩 빠름(TTV 빠름), 페이지 이동 시마다 서버 요청
- SEO 유리
- 예: CALI의 Thymeleaf 기반 일반 업무 화면

**CALI의 선택 이유**: 관리자 화면(React CSR)은 SEO가 필요 없고 인터랙션이 많으므로 SPA가 적합. 일반 업무 화면(Thymeleaf SSR)은 초기 로딩 속도와 보안이 중요하므로 SSR이 적합.

---

**D14. 이벤트 버블링(Bubbling)과 캡처링(Capturing)이란? `stopPropagation`은?**

A. DOM 이벤트는 **캡처링 → 타깃 → 버블링** 3단계로 전파됩니다.

- **캡처링**: 루트에서 타깃 방향으로 전파 (기본: addEventListener 3번째 인자 `true`)
- **버블링**: 타깃에서 루트 방향으로 전파 (기본 동작)

```html
<div id="outer">
    <button id="inner">클릭</button>
</div>
```
```javascript
document.getElementById('outer').addEventListener('click', () => {
    console.log('outer 클릭');  // 버블링으로 실행됨
});
document.getElementById('inner').addEventListener('click', (e) => {
    console.log('inner 클릭');
    e.stopPropagation(); // outer까지 버블링 중단
});
```

**이벤트 위임(Event Delegation)**: 부모 요소에 하나의 이벤트 리스너를 등록하고, 버블링을 통해 자식 이벤트를 처리하는 패턴. 동적으로 생성되는 요소나 다수의 요소에 효율적.

```javascript
// 1000개 버튼에 각각 리스너 대신
document.querySelector('#list').addEventListener('click', (e) => {
    if (e.target.tagName === 'BUTTON') {
        handleButtonClick(e.target.dataset.id);
    }
});
```

---

### Part E — 자료구조 & 알고리즘

---

**E1. 시간 복잡도 Big-O 표기법이란? 주요 자료구조의 시간 복잡도는?**

A. Big-O는 알고리즘의 최악 시나리오 성능을 나타내는 점근적 표기법입니다. 입력 크기 N에 대한 연산 수의 상한을 표현합니다.

**자주 쓰이는 표기**:
- `O(1)`: 상수 시간 (배열 인덱스 접근, 해시맵 조회)
- `O(log N)`: 로그 시간 (이진 탐색, B-Tree)
- `O(N)`: 선형 시간 (배열 순차 탐색)
- `O(N log N)`: 효율적 정렬 (병합 정렬, 퀵 정렬 평균)
- `O(N²)`: 이중 루프 (버블 정렬, 삽입 정렬)

**주요 자료구조 시간 복잡도**:
| 자료구조 | 탐색 | 삽입 | 삭제 |
|---------|------|------|------|
| 배열(Array) | O(N) | O(N) | O(N) |
| 해시맵(HashMap) | O(1) 평균 | O(1) 평균 | O(1) 평균 |
| 이진 탐색 트리(BST) | O(log N) 평균 | O(log N) 평균 | O(log N) 평균 |
| 링크드 리스트 | O(N) | O(1) 헤드 | O(1) 헤드 |
| 스택/큐 | O(N) | O(1) | O(1) |

---

**E2. 배열(Array)과 링크드 리스트(Linked List)의 차이는?**

A.
| 항목 | 배열 | 링크드 리스트 |
|------|------|-------------|
| 메모리 구조 | 연속 공간 | 불연속 (포인터 연결) |
| 인덱스 접근 | O(1) | O(N) |
| 삽입/삭제 (중간) | O(N) (이동 필요) | O(1) (포인터 변경) |
| 삽입/삭제 (끝) | O(1) amortized | O(N) 단방향, O(1) 이중 |
| 메모리 효율 | 좋음 (포인터 없음) | 낮음 (각 노드에 포인터) |
| 캐시 지역성 | 좋음 | 나쁨 |

Java의 `ArrayList`는 배열 기반, `LinkedList`는 이중 연결 리스트 기반입니다.
CALI에서 서비스 레이어의 목록 반환 타입은 대부분 `List<T>` 인터페이스를 사용하고, 구현체는 `ArrayList`입니다.

---

**E3. 스택(Stack)과 큐(Queue)의 구조와 활용 예시는?**

A.
**스택 (LIFO - Last In First Out)**:
- push (삽입), pop (제거), peek (조회)
- 활용: 함수 호출 스택(Call Stack), 뒤로 가기, 수식 계산, DFS

**큐 (FIFO - First In First Out)**:
- enqueue (삽입), dequeue (제거), peek (조회)
- 활용: 작업 대기열, BFS, 프린터 스풀, 이벤트 처리 큐

```java
// Java Stack
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1); stack.push(2);
int top = stack.pop(); // 2

// Java Queue
Queue<Integer> queue = new LinkedList<>();
queue.offer(1); queue.offer(2);
int front = queue.poll(); // 1
```

CALI의 이벤트 루프도 콜백 큐(FIFO) 구조를 사용합니다.

---

**E4. 해시맵(HashMap)의 동작 원리는? 충돌(Collision) 해결 방법은?**

A. 해시맵은 Key → Hash 함수 → 버킷 인덱스 → Value를 저장하는 자료구조입니다.

**동작**:
1. `key.hashCode()` 계산
2. `index = hashCode % bucketSize` 로 버킷 인덱스 결정
3. 해당 버킷에 Key-Value 쌍 저장

**충돌 해결**:
- **체이닝(Chaining)**: 같은 버킷에 링크드 리스트(또는 트리)로 여러 항목 저장. Java HashMap의 방식. 8개 이상이면 TreeMap으로 변환 (O(N) → O(log N)).
- **오픈 어드레싱(Open Addressing)**: 충돌 시 다른 빈 버킷으로 이동.

**Java HashMap 주요 특성**:
- 기본 로드 팩터: 0.75 (75% 채워지면 버킷 2배 확장)
- 해시 충돌이 많으면 O(1) 평균이지만 최악 O(N) 또는 O(log N)

CALI의 `ConcurrentMapCacheManager`는 `ConcurrentHashMap`을 사용합니다. 스레드 세이프하게 구현되어 있어 멀티스레드 환경에서 메뉴 캐시를 안전하게 읽고 쓸 수 있습니다.

---

**E5. 이진 탐색(Binary Search)이란? 조건은?**

A. 정렬된 배열에서 중간 값과 비교하며 탐색 범위를 절반씩 줄이는 알고리즘. O(log N).

**전제 조건**: 배열이 **정렬**되어 있어야 합니다.

```java
int binarySearch(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2; // 오버플로우 방지
        if (arr[mid] == target) return mid;
        if (arr[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1; // 없음
}
```

**활용**: DB B-Tree 인덱스가 이진 탐색 원리를 활용합니다. `Spring Data JPA`의 `findById()`도 PK 인덱스를 통한 O(log N) 탐색입니다.

---

**E6. DFS(깊이 우선 탐색)와 BFS(너비 우선 탐색)의 차이는?**

A.
**DFS(Depth-First Search)**: 한 경로를 끝까지 탐색 후 되돌아옵니다. 스택(또는 재귀)으로 구현.
**BFS(Breadth-First Search)**: 현재 노드에서 가까운 노드부터 탐색합니다. 큐로 구현.

```
그래프:
    A
   / \
  B   C
 / \
D   E

DFS 방문 순서: A → B → D → E → C
BFS 방문 순서: A → B → C → D → E
```

**활용**:
- **DFS**: 경로 존재 여부, 연결 요소 확인, 조합/순열 생성
- **BFS**: 최단 경로(가중치 없는 그래프), 레벨 단위 탐색

CALI의 메뉴 계층 구조(tree) 탐색 시 DFS/BFS를 활용합니다. 예를 들어 부모-자식 메뉴 계층 전체를 구성할 때 BFS로 레벨별 처리를 합니다.

---

**E7. 정렬 알고리즘 비교 (버블, 선택, 삽입, 병합, 퀵 정렬)**

A.
| 알고리즘 | 평균 | 최악 | 공간 | 안정 |
|---------|------|------|------|------|
| 버블 정렬 | O(N²) | O(N²) | O(1) | ✅ |
| 선택 정렬 | O(N²) | O(N²) | O(1) | ❌ |
| 삽입 정렬 | O(N²) | O(N²) | O(1) | ✅ |
| 병합 정렬 | O(N log N) | O(N log N) | O(N) | ✅ |
| 퀵 정렬 | O(N log N) | O(N²) | O(log N) | ❌ |
| 힙 정렬 | O(N log N) | O(N log N) | O(1) | ❌ |

**Java의 `Arrays.sort()`**: 기본 타입은 Dual-Pivot Quicksort, 객체는 Timsort(삽입 정렬 + 병합 정렬의 하이브리드, 안정 정렬).

**안정 정렬(Stable Sort)**: 같은 값을 가진 원소의 상대적 순서가 정렬 후에도 유지되는 것. 다중 기준 정렬 시 중요합니다.

---

**E8. 재귀(Recursion)와 동적 프로그래밍(DP)의 차이는?**

A.
**재귀**: 함수가 자기 자신을 호출. 문제를 작은 부분 문제로 분할.
```java
int fibonacci(int n) {
    if (n <= 1) return n;
    return fibonacci(n-1) + fibonacci(n-2); // 중복 계산 多
}
```

**동적 프로그래밍(DP)**: 중복 부분 문제를 메모이제이션(캐싱)하여 한 번만 계산.
```java
// Memoization (Top-Down)
int[] memo = new int[n+1];
int fibonacci(int n) {
    if (n <= 1) return n;
    if (memo[n] != 0) return memo[n]; // 캐시 확인
    memo[n] = fibonacci(n-1) + fibonacci(n-2);
    return memo[n];
}

// Tabulation (Bottom-Up)
int[] dp = new int[n+1];
dp[0] = 0; dp[1] = 1;
for (int i = 2; i <= n; i++) {
    dp[i] = dp[i-1] + dp[i-2];
}
```

**적용 기준**: 부분 문제가 중복되고, 최적 부분 구조(전체 최적해 = 부분 최적해 조합)를 가질 때 DP 적용.

---

**E9. 그리디(Greedy) 알고리즘이란? DP와 언제 구분하나요?**

A. **그리디**: 각 단계에서 현재 가장 좋아 보이는 선택(지역 최적)을 반복하여 전체 최적해를 구하는 방법.

**특징**:
- 한 번 선택하면 되돌아가지 않음 (되추적 없음)
- 구현이 단순하고 빠름
- 항상 최적해를 보장하지는 않음

**그리디가 통하는 경우**: 활동 선택 문제, 최소 스패닝 트리(Kruskal, Prim), 허프만 코딩, 거스름돈 문제(단, 특정 동전 체계에서만)

**DP가 필요한 경우**: 그리디 선택이 미래에 영향을 주거나, 지역 최적이 전역 최적을 보장하지 않을 때.

---

**E10. 트리(Tree)와 그래프(Graph)의 차이, 그리고 힙(Heap)이란?**

A.
**그래프**: 정점(Vertex)과 간선(Edge)으로 이루어진 자료구조. 사이클이 있을 수 있고, 방향/무방향, 가중치가 있을 수 있음.

**트리**: 그래프의 특수한 형태. **사이클이 없고, 루트가 하나인 연결 그래프**. N개 노드, N-1개 간선.

**힙(Heap)**: 완전 이진 트리 기반의 자료구조.
- **최대 힙**: 부모 ≥ 자식. 루트가 항상 최댓값.
- **최소 힙**: 부모 ≤ 자식. 루트가 항상 최솟값.
- 삽입/삭제: O(log N), 최대/최솟값 조회: O(1)

**활용**: 우선순위 큐, 힙 정렬, 다익스트라 알고리즘

Java의 `PriorityQueue`는 최소 힙으로 구현됩니다.
```java
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
minHeap.offer(3); minHeap.offer(1); minHeap.offer(2);
minHeap.poll(); // 1 (가장 작은 값)
```

---

*문서 끝 — CALI 프로젝트 상세 설명서 (v_260326)*
