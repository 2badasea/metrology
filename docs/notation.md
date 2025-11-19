
---
# 📌1번. @EnableMethodSecurity(prePostEnabled = true) 애너테이션

스프링 시큐리티에서 메서드 단위 보안(권한 검사)을 활성화하는 애너테이션이다.  
이를 사용하면 서비스 계층이나 컨트롤러 계층의 메서드에 `@PreAuthorize`, `@PostAuthorize`, `@Secured` 같은 애너테이션을 적용할 수 있다.

## 1. 역할
- HTTP 요청 단위의 URL 기반 보안 외에도, 메서드 호출 시점에서 권한을 검사하도록 지원한다.
- 예: `@PreAuthorize("hasRole('ADMIN')")`  
  → 이 코드를 사용하려면 `@EnableMethodSecurity`가 선언되어 있어야 한다.
- `@Configuration` 애너테이션이 붙은 곳에 함께 선언해야 한다.  
  다만, 스프링 시큐리티 6버전 이후부터는 기본값이 true이므로 반드시 명시할 필요는 없다.

## 2. 동작 방식
Spring AOP(프록시)를 사용해 메서드 호출 전에 SecurityInterceptor가 권한 체크 로직을 수행한다.

## 간단한 예시 코드

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
}

@Service
public class MemberService {

    @PreAuthorize("hasRole('ADMIN')")
    public String getAdminData() {
        return "관리자 전용 데이터";
    }
}
```

---

# 📌2번.Spring Boot Jar 내부 구조 및 'classpath:' 의미

## 1. 정적 리소스(Javascript, CSS, 이미지)
- 빌드 시 다음 경로로 묶임  
  `src/main/resources/static/**`  
  → `classpath:/static/`  
  → Jar 내부: `BOOT-INF/classes/static/**`
- Spring은 `classpath:` 기준으로 자원을 읽으며, 이는 Jar 내부의 `BOOT-INF/classes/`를 의미함.

## 2. Java 소스(.java)
- 컴파일 후 `.class` 파일로 변환되어 패키지 구조 그대로 저장됨.
- 예:  
  `src/main/java/com/example/config/WebConfig.java`  
  → `BOOT-INF/classes/com/example/config/WebConfig.class`

## 3. Spring Boot Jar 전체 구조 예시
your-app.jar
├── BOOT-INF
│ ├── classes ← classpath 실제 내용
│ │ ├── com/example/... (컴파일된 .class)
│ │ ├── static/... (정적 리소스)
│ │ ├── templates/... (Thymeleaf)
│ │ └── application.yml
│ └── lib/ (외부 라이브러리)
└── META-INF


## 4. classes 아래 포함되는 것
- 컴파일된 `.class` 파일  
- `resources` 디렉토리의 모든 파일(static, templates, application.yml 등)

## 5. classpath: 의 의미
- 논리적으로 **BOOT-INF/classes/** 를 가리키는 경로.
---

# 📌3번. CSRF 비활성화 설정 요약

## 설정 코드
```java
http.csrf(AbstractHttpConfigurer::disable);
```

## 설명
- AbstractHttpConfigurer를 메서드 레퍼런스로 사용해 CSRF 기능을 비활성화하는 설정이다.
- Spring Security는 기본적으로 POST, PUT, DELETE 요청에 대해 CSRF 토큰 검증을 수행한다.
- 이 설정을 적용하면 이러한 CSRF 토큰 확인이 비활성화된다.
---

# 📌4번. @Builder 애너테이션 요약

## 설명
- `@Builder`는 빌더 패턴을 자동 생성해 주는 애너테이션이다.
- 필요한 필드만 선택적으로 지정해 객체를 생성할 수 있다.

## 예시
```java
Member member = Member.builder()
        .loginId("testuser")
        .pwd("$2b$10$hashedpassword")
        .auth(Member.AuthType.user)
        .isActive((byte)0)
        .build();
```
---

# 📌5번. loadUserByUsername() 동작 요약

## 핵심 내용
- `loadUserByUsername()`는 **비밀번호 비교를 하지 않고**,  
  username으로 **사용자 정보를 조회하여 UserDetails로 반환**하는 역할만 수행한다.
- 비밀번호 검증은 `AuthenticationProvider`(= `DaoAuthenticationProvider`)가  
  `PasswordEncoder.matches()`를 통해 **자동으로 처리**한다.
- 따라서 이 메서드에서는 **DB에 저장된 비밀번호 해시를 그대로 UserDetails에 넣어야 한다.**

## 예시 코드 요약
```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    Member member = memberRepository.findByLoginId(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

    return User.withUsername(member.getLoginId())
            .password(member.getPwd())                      // DB의 해시 그대로
            .accountLocked(member.getIsActive() != 1)       // is_active = 0 → 로그인 불가
            .authorities("ROLE_USER")                       // 기본 권한 설정
            .build();
}
```

## 정리
- loadUserByUsername() → 유저 조회 + UserDetails 생성
- 비밀번호 비교 → 시큐리티 내부에서 자동 처리
- PasswordEncoder는 SecurityConfig에서 빈으로 등록해 두면 자동 사용됨


---

# 📌6번. 시큐리티 내 예외(UsernameNotFoundException, LockedException) 처리 방식

### 1. `UsernameNotFoundException` → `BadCredentialsException`

- `UserDetailsService#loadUserByUsername()` 안에서  
  `UsernameNotFoundException` 을 던져도, **실패 핸들러(훅)에서는 그대로 받을 수 없다.**
- Spring Security 내부에서 이 예외를 **`BadCredentialsException`으로 변환**해서 다룬다.
  - 실패 훅 내부에서 **`exception of UsernameNotFoundException`** 의 값이 비어있음

#### 왜 이렇게 할까?

- 보안상, 클라이언트에게  
  > “아이디가 없습니다” vs “비밀번호가 틀렸습니다”  
  를 구분해서 알려주면 안 되기 때문.
- 내부적으로는  
  - *존재하지 않는 계정* 이든 *비밀번호가 틀린 계정* 이든  
  모두 **동일한 자격 증명 실패(`BadCredentialsException`)** 로 취급해서  
  공격자가 계정 존재 여부를 추측하지 못하게 한다.

> 결과적으로, 실패 핸들러(훅)에서는 `UsernameNotFoundException`이 아니라  
> `BadCredentialsException` 기준으로 분기하는 것이 자연스럽다.


### 2. `LockedException` → `InternalAuthenticationServiceException(원인 예외)` 

- `loadUserByUsername()` 같은 내부에서 `LockedException`을 직접 던져도 실패 핸들러까지 **그대로 전달되지 않는다.**
- Spring Security는 이 예외를 보통 **`InternalAuthenticationServiceException`으로 감싸서(래핑해서)** 전달한다.

```java
if (exception instanceof InternalAuthenticationServiceException
    && exception.getCause() instanceof LockedException lockedException) {
    // lockedException.getMessage() 사용
}
```

#### 왜 이렇게 할까?
- 인증 내부 구현을 캡슐화하기 위해서다.
  - 구체적인 예외 타입을 외부에 그대로 노출하지 않고,
  - 시큐리티 필터 체인 입장에서는 “인증 서비스 내부에서 난 문제”로 묶어서 처리.
- 대신, 실제 원인 예외는 exception.getCause()에 보관해 두기 때문에, 커스텀 AuthenticationFailureHandler에서는 cause를 보고 잠금(계정 차단/승인 필요 등)에 대한 맞춤 메시지를 꺼내 쓸 수 있다.

### 참고) 스프링 시큐리티 예외 상속 관계
#### AuthenticationException 계층 구조 (계정 상태 관련)

- `AuthenticationException`  
  - `AccountStatusException`  
    - `LockedException` : 계정 잠금 상태  
    - `DisabledException` : 비활성 계정(사용 중지)  
    - `AccountExpiredException` : 계정 유효기간 만료  
    - `CredentialsExpiredException` : 비밀번호(자격 증명) 유효기간 만료  

---

# 📌7번. 서버와 브라우저 간 http 통신에서 contentType, dataType 설정이 미치는 영향

### 1. 결론 한 줄 요약

- **요청(Request) 쪽**  
  - `contentType` 옵션 → 최종적으로 **요청 헤더의 `Content-Type`** 으로 서버에 전송되는 **진짜 값**
- **응답(Response) 쪽**  
  - 서버의 `setContentType(...)` 또는 `@PostMapping(produces = ...)` → **응답 헤더의 `Content-Type`** 을 결정하는 **진짜 값**
- **`dataType` (jQuery Ajax)**  
  - 서버로 **전송되지 않는다.**  
  - 오직 **클라이언트가 응답을 어떻게 파싱할지(해석할지)에 대한 힌트/설정**일 뿐이다.


### 2. HTTP 기본 구조 정리

- 요청(Request)
  - 헤더: `Content-Type`, `Accept`, ...
  - 바디: JSON, 폼데이터 등 실제 데이터
- 응답(Response)
  - 헤더: `Content-Type`, ...
  - 바디: JSON, HTML, 텍스트 등 실제 응답 데이터

Spring Security의 필터(`filterChain`)가 로그인 요청을 가로채더라도,  
**“하나의 HTTP 요청/응답” 구조 자체는 변하지 않는다.**  
단지 그 요청·응답을 중간에서 가공/검사할 뿐이다.


## 3. 서버(Spring) 입장 – `setContentType`, `produces`, 그리고 `@RestController`

서버는 **응답(Response)의 타입을 `Content-Type` 헤더로 결정**한다.  
이 헤더를 어떻게 세팅하느냐에 따라, 클라이언트가 응답을 무엇으로 인식할지가 달라진다.

### 4-1. 서블릿 스타일 (`HttpServletResponse` 직접 사용)

```java
@GetMapping("/sample")
public void sample(HttpServletResponse response) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"result\":\"ok\"}");
}
```
- response.setContentType(...)
  - 응답 헤더의 Content-Type을 직접 설정.
- 스프링 MVC의 메시지 컨버터를 거치지 않고,
  - 내가 직접 문자열/바이너리 데이터를 써 넣을 때 주로 사용하는 방식.
- 최종 Content-Type 은 항상 서버가 여기서 설정한 값이 기준이 된다.

### 4-2. 스프링 MVC 스타일 – @Controller + @ResponseBody / ResponseEntity

```java
@Controller
public class LoginController {

    @PostMapping(value = "/login", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public LoginResponse login(...) {
        return new LoginResponse("ok");
    }

    @PostMapping("/login2")
    public ResponseEntity<LoginResponse> login2(...) {
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginResponse("ok"));
    }
}
```
- produces = "application/json;charset=UTF-8"
  - 이 핸들러 메서드가 어떤 Content-Type으로 응답할지를 선언.
  - 메시지 컨버터(예: MappingJackson2HttpMessageConverter)가 해당 타입에 맞게 객체를 JSON으로 변환.
- ResponseEntity를 사용하면:
  - HTTP status, header, body 를 코드에서 더 세밀하게 제어할 수 있다.
  - contentType(...) 으로 Content-Type 을 명시할 수도 있고, 생략하면 타입 추론 + 메시지 컨버터의 기본 동작을 따른다.
- 이 경우에도 최종 응답 헤더의 Content-Type은 스프링이 설정한 값이 진짜다. 
  클라이언트는 이 헤더를 보고 응답을 해석한다.

### 4-3. 스프링 Web 스타일 – @RestController 인 경우

```java
@RestController
public class LoginRestController {

    @PostMapping(value = "/login", produces = "application/json;charset=UTF-8")
    public LoginResponse login(...) {
        return new LoginResponse("ok");
    }

    @GetMapping("/text")
    public String text() {
        return "hello";
    }
}
```
- @RestController = @Controller + @ResponseBody
  - 즉, 모든 메서드 반환값이 곧 HTTP 응답 바디가 된다.
  - 별도로 @ResponseBody 를 붙이지 않아도 됨.
- produces 동작은 @Controller + @ResponseBody 와 완전히 동일:
  - produces = "application/json" 이면 → JSON 응답으로 처리
  - produces = "text/plain" 이면 → 텍스트 응답으로 처리

- 반환 타입에 따라 메시지 컨버터가 자동으로 선택된다:
  - String → 보통 text/plain (또는 상황에 따라 text/html)
  - 객체(LoginResponse) → Jackson이 있다면 application/json 으로 JSON 직렬화


## 5. 자주 헷갈리는 케이스 요약

### 5-1. 서버 JSON, 클라이언트 `dataType: 'json'`
```js
$.ajax({
  url: '/login',
  method: 'POST',
  contentType: 'application/json; charset=utf-8',
  dataType: 'json'
});
```
```java
response.setContentType("application/json;charset=UTF-8");
```
- 가장 이상적인 매칭, 문제 없음.

### 5-2. 서버 JSON, 클라이언트 dataType: 'text' 또는 dataType 생략
```js
// case 1: dataType 명시 - text
$.ajax({
  url: '/login',
  method: 'POST',
  dataType: 'text'
});

// case 2: dataType 생략
$.ajax({
  url: '/login',
  method: 'POST'
});
```
```java
response.setContentType("application/json;charset=UTF-8");
```
- 서버: 헤더 상으로 JSON (Content-Type: application/json)
- 클라이언트:
  - dataType: 'text'
    - 응답을 “그냥 문자열”로 취급
    - JSON 자동 파싱 안 함 (필요하면 JSON.parse()를 직접 호출)
  - dataType 생략
    - jQuery가 서버의 Content-Type을 보고
    - application/json 이면 자동으로 JSON으로 파싱

- 서버는 동일하게 JSON을 보내지만,
“어떻게 해석할지”는 오로지 클라이언트(dataType/자동판단)에 달려 있다.

### 5-3. 서버 텍스트, 클라이언트 dataType: 'json'
```js
$.ajax({
  url: '/login',
  method: 'POST',
  dataType: 'json'
});
```
```java
response.setContentType("text/plain;charset=UTF-8");
response.getWriter().write("hello world");
```
결과
- jQuery가 "hello world" 를 JSON으로 파싱하려다가 실패
➜ success가 아니라 error 콜백으로 떨어짐
- 서버 입장에서는 응답 자체는 문제 없음
➜ 에러의 원인은 전적으로 클라이언트의 파싱 전략(dataType) 설정


---

# 📌8번. Entity 클래스 차원에서 @Builder를 선언 VS 필드에서 @Builder.Default를 선언

## Lombok `@Builder`와 `@Builder.Default` 정리

### 1. 핵심 결론

- **`@Builder`를 쓰는 순간, 필드에 적어둔 기본값 초기화 코드는 빌더에선 무시된다.**
- 빌더를 사용할 때도 그 기본값을 그대로 쓰고 싶다면  
  **반드시 `@Builder.Default`를 필드에 함께 붙여야 한다.**

---

### 2. 예시 코드로 보는 동작 차이

```java
@Builder
public class Agent {

    private Integer id;

    @Builder.Default
    private String createType = "basic";
}
```

위처럼 @Builder.Default를 붙인 경우:
- new Agent() → createType == "basic"
- Agent.builder().build() → createType == "basic" ✅

아래처럼 @Builder.Default 없이 기본값만 준 경우:
```java
@Builder
public class Agent {

    private Integer id;
    private String createType = "basic";
}
```
- new Agent() → createType == "basic" ✅
(생성자 방식에서는 필드 초기화식이 적용됨)
- Agent.builder().build() → createType == null ❌
(빌더에서는 기본값이 적용되지 않음)

### 3. 왜 이런 차이가 생길까?
- Lombok은 @Builder를 쓰면 내부적으로 별도의 빌더 클래스를 생성한다.
- 이 빌더 클래스 안의 필드들은 전부 기본값이 null / 0 으로 시작한다.
- build() 호출 시:
  - “필드 초기화식에 적어둔 값”을 보지 않고,
  - 빌더 내부에 저장된 값(설정한 것만)을 그대로 생성자에 넘긴다.
- 그래서 필드에 = "basic" 이라고 써둔 초기값은 빌더 경로에선 전혀 사용되지 않는다.

### 4. 정리
- 엔티티/DTO에 @Builder를 쓰면서 기본값도 유지하고 싶다면:
  - private String createType = "basic";
➕ @Builder.Default 를 함께 사용해야 한다.
- 그렇지 않으면:
  - 생성자 호출(new) 경로와 빌더 호출 경로의 기본값 동작이 달라져서 
  버그/헷갈림의 원인이 될 수 있다.

---

