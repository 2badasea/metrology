
---
# 📌 1번. @EnableMethodSecurity(prePostEnabled = true) 애너테이션

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

# 📌 2번.Spring Boot Jar 내부 구조 및 'classpath:' 의미

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

# 📌 3번. CSRF 비활성화 설정 요약

## 설정 코드
```java
http.csrf(AbstractHttpConfigurer::disable);
```

## 설명
- AbstractHttpConfigurer를 메서드 레퍼런스로 사용해 CSRF 기능을 비활성화하는 설정이다.
- Spring Security는 기본적으로 POST, PUT, DELETE 요청에 대해 CSRF 토큰 검증을 수행한다.
- 이 설정을 적용하면 이러한 CSRF 토큰 확인이 비활성화된다.
---

# 📌 4번. @Builder 애너테이션 요약

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

# 📌 5번. loadUserByUsername() 동작 요약

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

# 📌 6번. 시큐리티 내 예외(UsernameNotFoundException, LockedException) 처리 방식

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

# 📌 7번. 서버와 브라우저 간 http 통신에서 contentType, dataType 설정이 미치는 영향

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

# 📌 8번. Entity 클래스 차원에서 @Builder를 선언 VS 필드에서 @Builder.Default를 선언

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

# 📌 9번. Entity 클래스 내 Enum 타입의 필드 위에 @Enumerated 애너테이션을 명시하는 이유

```java
@Enumerated(EnumType.STRING)
@Column(name = "main_yn")
@Builder.Default
private YnType mainYn = YnType.n;
```
## 필드 기본값 vs @Enumerated 정리

### 필드 기본값
```java
private YnType mainYn = YnType.n;
```

- 이 코드는 자바 객체 내부의 기본 상태를 정한다.
  - 새로 생성된 엔티티 객체가 어떤 enum 상수를 가질지 결정.
- new AgentManager() 또는
  - JPA가 리플렉션으로 엔티티를 만들 때 적용되는 값이다.

- 역할: “객체 안에서 어떤 값으로 시작할 것인가?”

## @Enumerated(EnumType.STRING)
```java
@Enumerated(EnumType.STRING)
@Column(name = "main_yn")
private YnType mainYn;
```
- 이 애너테이션은 enum 필드를 DB에 어떤 방식으로 매핑할지를 정한다.
  - ORDINAL → 순서값(0, 1, 2…)
  - STRING → enum 이름 그대로("y", "n" 등)

- mainYn에 어떤 enum 상수가 들어 있든,
  - 그것을 DB에 숫자로 넣을지, 문자열로 넣을지는 이 설정에 따른다.

- 역할: “enum을 DB 컬럼에 어떻게 저장·조회할 것인가?”

## 요약
1. mainYn = YnType.n;
  - “엔티티의 기본 enum 값은 YnType.n이다”라는 뜻.
2. @Enumerated를 생략하면:
  - JPA 기본값은 EnumType.ORDINAL → 0, 1, 2 같은 숫자로 저장.
3. @Enumerated(EnumType.STRING)을 쓰면:
  - enum 이름 그대로 "y", "n" 같은 문자열로 저장.
4. DB 컬럼이 ENUM('y','n')이면:
  - EnumType.STRING이 가장 잘 맞는 매핑 방식이다.
5. 필드 기본값 설정과 DB 저장 형태는 서로 다른 층위이므로,
  - “기본값을 YnType.n으로 줬다” → enum 상수를 무엇으로 쓸지 결정.
  - “EnumType.STRING을 쓴다” → 그 enum 상수를 DB에 문자열로 어떻게 표현할지 결정.
<br><br>

---

# 📌 10번. jar와 .war의 차이

### 1. 기본 개념

- **JAR (Java ARchive)**
  - 자바 애플리케이션/라이브러리를 묶는 압축 형식
  - `java -jar app.jar` 로 **직접 실행 가능**(실행형 JAR인 경우)

- **WAR (Web Application ARchive)**
  - **웹 애플리케이션 배포용** 형식
  - Tomcat, JBoss 같은 **외부 WAS에 올려서 실행**하는 패키지


### 2. 스프링부트에서의 차이

- **JAR 방식 (요즘 기본)**
  - Spring Boot가 **내장 톰캣**까지 포함한 `fat jar` 생성
  - 별도 톰캣 설치 없이:
    ```bash
    java -jar myapp.jar
    ```
  - 신규 서비스 / 개인 프로젝트 / 클라우드 배포에 거의 이 방식 사용

- **WAR 방식 (레거시/사내 WAS 환경)**
  - 회사에서 **이미 운영 중인 톰캣 서버**에 올려야 할 때 사용
  - `myapp.war` 파일을 톰캣 `webapps` 등에 배포해서 구동
  - 조직 표준이 “WAR + 외부 톰캣”인 레거시 환경에서 주로 사용


### 3. 정리

- “내 앱을 직접 실행시키는 서버까지 같이 들고 다니고 싶다” → JAR (내장 톰캣)
- “회사에서 운영 중인 톰캣 서버에만 올려야 한다” → WAR (외부 톰캣)
<br><br>

---

# 📌 11번. 소스저장/ 컴파일/ 빌드/ 런타임 구분 및 패캐징된 파일의 디렉토리 구조 ex) classes/ 경로의 의미?

## 1. 시점 정리 (저장 / 컴파일 / 빌드 / 런타임)

| 시점        | 하는 일                                         | IntelliJ에서 보통 언제 일어남 |
|------------|--------------------------------------------------|-------------------------------|
| 저장 시점   | `.java` 소스 파일만 디스크에 저장               | Ctrl + S, 자동 저장           |
| 컴파일 시점 | `.java` → `.class` 로 변환 (javac 실행)         | **Build → Make Project** 등   |
| 빌드 시점   | 컴파일 + 리소스 복사 + **JAR/WAR 패키징**       | Gradle `build`, `bootJar` 등  |
| 런타임 시점 | JVM이 `.class`(또는 JAR 내부)를 로딩해서 실행   | `java -jar xxx.jar` 등        |

- **저장 = 컴파일 아님**
  - 그냥 소스만 저장.
- **컴파일 = .class 생성**
  - IDE 빌드/Gradle 컴파일이 돌 때 발생.
- **빌드 = 패키징(JAR/WAR 생성)**
  - 이미 만들어진 `.class`들을 모아서 JAR/WAR로 싸는 과정.

## 2. `.class` 파일은 언제 생성되나?

- `.class`는 **무조건 “컴파일 시점”에 생성**됨.
- IntelliJ 기준:
  - `Build → Build Project` / `Make`를 하면  
    → 변경된 `.java`들에 대해 **javac가 돌아가고 `.class`가 생성**됨.
- 단순 **저장(Ctrl + S)** 만 해서는 `.class`가 생기지 않음  
  (단, “자동 빌드” 옵션을 켜두면 저장 직후 자동 컴파일이 붙을 수는 있음. 그래도 개념상은 “저장”과 “컴파일”은 다른 단계).


- `.jar`는 **빌드/패키징 단계**에서 생김.
  - 예: Gradle 기준
    - `./gradlew jar`
    - `./gradlew bootJar`
    - `./gradlew build` (내부적으로 위 작업 포함)
- 이때 이미 존재하는 **`.class` + 리소스(templates, static, properties 등)** 들을  
  JAR 내부 디렉토리 구조에 맞게 넣고, `MANIFEST.MF` 등도 함께 묶음.

## 4. JAR 내부의 `/classes` 경로는 뭐냐?

### 4-1. 일반 JAR (스프링부트 아닌 순수 JAR)

보통 구조 예:

```text
my-app.jar
├─ META-INF/
│   └─ MANIFEST.MF
└─ com/
    └─ example/
        └─ demo/
            └─ MyClass.class
```
#### 포인트
- JAR 내부에는 보통 /classes 디렉토리 자체는 없음.
- 그냥 패키지 경로 (com/example/demo) 로 바로 들어감.
- JAR는 src/main/java / src/main/resources 이런 소스 경로 개념을 안 들고 다님.
오직 “패키지 경로 + 리소스 경로”만 가짐.


### 4-2. 스프링부트 Fat JAR (bootJar)
```java
my-app.jar
├─ BOOT-INF/
│   ├─ classes/      ← 여기 아래에 컴파일된 클래스들
│   │   └─ com/example/demo/...
│   └─ lib/          ← 의존성 JAR들
└─ META-INF/ ...
```
- 여기서 **BOOT-INF/classes/가 바로 “빌드 결과 클래스 루트 디렉토리”**라고 보면 됨.
- 이 classes/는: 빌드되기 전의 프로젝트 루트도 아니고, src/main 디렉토리도 아님.
- 그냥 “빌드 결과물(컴파일된 .class들)의 루트 경로”를 JAR 안에서 이렇게 이름 붙인 것뿐.


#### 참고) 소스코드 내 'classpath:'  경로의 의미
- 컴파일 및 jar로 빌드된 시점에 cali.jar/BOOT-INF/classes/ 를 가리키는 논리적 경로
<br><br>

---

# 📌 12번. 스프링 시큐리티 필터 체인 내에서 발생한 예외 흐름 및 필터 체인 내부에서 다시 예외를 던지는 경우

## **스프링 시큐리티 필터 체인 내에서 발생한 예외 흐름**
- 인증 과정(UsernamePasswordAuthenticationFilter 등)에서 던지는 예외들은 **로그인 성공/실패 핸들러(훅)** 쪽으로 전달됨.
- 따라서 **실패 핸들러(onAuthenticationFailure)** 내부에서 `exception instanceof UsernameNotFoundException` 처럼 구체 예외 타입 체크가 가능함.
- 참고로, `@ControllerAdvice` 기반 **전역 예외 처리기는** 기본적으로 *컨트롤러(@Controller / @RestController) 내부 메서드 내에서 발생한 예외*에 대해 동작함. (필터 체인 단계의 예외와는 범위가 다름)

## **필터 체인 내부(성공/실패 핸들러 포함)에서 다시 예외를 던지는 경우**
- 로그인 성공/실패 핸들러 내부에서 예외를 다시 던지면, 이 예외는 **시큐리티 필터 체인 밖으로 전파**될 수 있음.
- 그 결과, 서블릿 컨테이너(Tomcat 등) 수준에서 처리되면서 **HTTP 500 에러(기본 에러 페이지 또는 커스텀 에러 페이지)** 로 이어질 수 있음.
- 이 경우 예외는 이미 필터 체인 밖에서 터지기 때문에, **전역 예외 처리기(@ControllerAdvice)는 관여할 수 없음**.
<br><br>

---

# 📌 13번. DB에 대응하는 Entity 클래스 정의할 때 유의점

- **기본값은 가급적 엔티티 필드에서 먼저 설정**
    - `@Column(columnDefinition = "varchar(10) default 'basic'"` 처럼 DB 기본값만 두기보다는  
      **엔티티 필드에 직접 기본값을 부여**하는 편이 더 안전함. (필드에 기본값을 초기화하기)
    - 필요하다면
      - 엔티티 필드에 기본값 설정 +
      - DB 컬럼에도 기본값 설정  
      두 군데 모두 정의해서 **DB와 엔티티의 기본 상태를 일치**시키는 것도 방어적으로 괜찮은 선택.

- **JPA 관점에서 “엔티티 수준의 기본값 보장”이 중요**
    - DB에만 기본값이 있고, 엔티티 필드는 `null` 인 상태는 최대한 피하는 것이 좋음.
    - 애플리케이션 코드(JPA)가 다루는 것은 **엔티티 객체**이므로,  
      엔티티가 생성되는 시점부터 **일관된 기본 상태**를 가지도록 설계하는 게 안정적임.

- **모든 필드에 기본값을 줄 필요는 없음**
    - “항상 특정 의미를 가져야 하는 값” 혹은  
      “비워져 있으면 애매한 상태가 되는 값”에만 선택적으로 기본값을 부여하는 것이 좋음.
    - 예를 들어:
      - 상태값, 플래그(Y/N), 생성 타입 등은 기본값을 두는 편이 관리에 유리함.

- **Entity는 단순 DB 스키마 복사본이 아님**
    - Entity 설계의 목표는 “DB 구조를 그대로 옮겨 적는 것”이 아니라,  
      **애플리케이션에서 엔티티가 언제나 유효한 상태를 유지**하도록 만드는 것.
    - 즉, “데이터 전송/비즈니스 로직에서 이 객체가 정상 동작하려면  
      어떤 값들이 기본적으로 잡혀 있어야 하는가?”를 기준으로 필드와 기본값을 설계해야 함.

<br><br>

---

# 📌 14번. 제이쿼리 load 함수 작동 방식
- url과 callback을 보내는 경우엔 GET 방식으로 동작
- 다만 파라미터에 data가 포함된다면 POST 방식으로 요청을 보내게 된다.
  - g_modal을 호출할 때 애너테이션으로 GetMapping으로 받으면 에러가 나는 원인(g_modal은 데이터를 전달하도록 기본 설계)

<br><br>

---

# 📌 15번. RedirectAttribute을 사용할 때, addAttribute() VS addFlashAttribute() 속성값 두 종류 차이

### 1. `addAttribute()` 계열 – URL에 붙는 값

- 값이 **URL에 노출됨** (쿼리스트링 / 경로 변수로 표현)
- 새로고침해도 **같은 URL이면 값이 그대로 유지**
- 북마크 / 공유 시, 해당 값도 함께 전달됨
- **민감한 정보나 너무 긴 데이터에는 부적합**

### 2. `addFlashAttribute()` 계열 – 1회용 서버 저장

- 값이 **URL에 노출되지 않음**
  - 서버 측(세션)에 임시로 저장 후, 다음 요청의 `Model`로 복사됨
- **딱 “다음 한 번의 요청”에서만 사용 가능**  
  → 그 이후에는 자동 삭제 (1회성)
- 사용자 알림 메시지, 검증 실패 후 폼 값/오류 메시지, 일회성 안내에 적합
- 대표적으로 **PRG(Post-Redirect-Get) 패턴**에서 자주 사용
<br><br>

---

# 📌 16번. DispatcherServlet과 관련 컴포넌트 정리
스프링 MVC에서는 **`DispatcherServlet`이 모든 웹 요청의 진입점(Front Controller)** 역할을 함.  
아래 컴포넌트들이 함께 동작하면서 요청 → 컨트롤러 호출 → 응답 생성 흐름을 만든다.


### DispatcherServlet
- 서블릿 컨테이너(Tomcat 등)에서 등록되는 **프론트 컨트롤러(Front Controller)**.
- 모든 HTTP 요청을 먼저 받아서:
  1. 어떤 컨트롤러를 호출할지 찾고(HandlerMapping),
  2. 그 컨트롤러를 어떻게 실행할지 결정하고(HandlerAdapter),
  3. 실행 결과를 어떤 방식으로 응답으로 만들지(ViewResolver / HttpMessageConverter)
  를 **전체적으로 조율**하는 역할.
- Security FilterChain은 DispatcherServlet보다 먼저 실행된다.
  - 둘은 같은 레벨이 아니라 Security = 필터 레벨, DispatcherServlet = 서블릿 레벨
  - 서블릿 앞단의 관문 역할이라고 이해하면 딱 좋아.

### HandlerMapping
- 들어온 요청 URL, HTTP 메서드(GET/POST 등)에 따라  
  **어떤 컨트롤러 메서드(@RequestMapping, @GetMapping 등)** 를 호출할지 찾아주는 컴포넌트.
- 예: `/members/list` 요청 → `MemberController.list()` 메서드 매핑.

### HandlerAdapter

- HandlerMapping이 찾아준 “핸들러(컨트롤러 객체 + 메서드 정보)”를  
  **실제로 호출하는 방법을 아는 어댑터**.
- 컨트롤러 구현 스타일(@Controller, @RestController, @ResponseBody, 핸들러 메서드 시그니처 등)이 달라도,  
  DispatcherServlet 입장에선 **HandlerAdapter 하나를 통해 일관된 방식으로 실행**할 수 있게 해줌.


### Controller

- 실제 **비즈니스 로직 진입점**.
- 요청 파라미터를 받고, 서비스 호출하고, Model에 데이터 담고,  
  **뷰 이름 또는 응답 객체를 리턴**하는 역할.
- 예: `@Controller`, `@RestController` 로 작성하는 클래스/메서드들.


### ViewResolver

- 컨트롤러가 리턴한 **뷰 이름(String)** 을 보고  
  “어떤 뷰 파일을 사용할지” 결정하는 컴포넌트.
- 예: `return "member/list";`  
  → ViewResolver가 `templates/member/list.html`(Thymeleaf) 같은 실제 뷰 파일 찾음.

### HttpMessageConverter

- 주로 `@ResponseBody` 또는 `@RestController` 에서 사용.
- 컨트롤러가 리턴한 객체를 **JSON, XML, 문자열 등으로 변환**하거나,  
  들어온 HTTP 요청의 Body(JSON 등)를 **자바 객체로 변환**해 주는 역할.
- 예:
  - 요청: JSON → 자바 객체 (`@RequestBody`)
  - 응답: 자바 객체 → JSON (`@ResponseBody` / `ResponseEntity<>`)
<br><br>

---

# 📌 17번. 전역 예외 처리(@ControllerAdvice)로 404를 못 잡는 이유


## **"컨트롤러 메서드 안에서 발생한 예외” 에 대해서만 동작한다. (@RestControllerAdvice도 마찬가지)**
```java
@Controller
public class TestController {

    @GetMapping("/sample")
    public String sample() {
        throw new RuntimeException("어이쿠");
    }
}
```
- 위처럼 매핑된 컨트롤러 메서드 내부에서 예외가 발생한 경우, 전역 예외에서 잡아서 처리

### 존재하지 않는 URL 요청 흐름
- 예: 브라우저에서 /member/notPage 요청
  1. 클라이언트 → /member/notPage 요청
  2. DispatcherServlet이 HandlerMapping을 통해 컨트롤러를 찾으려 함
  3. 해당 URL에 매핑된 컨트롤러/메서드가 없음
  4. 아예 컨트롤러 메서드 호출 단계까지 가지 못함
  5. 따라서 컨트롤러 내부에서 예외가 발생한 것이 아니므로 @ControllerAdvice + @ExceptionHandler 쪽으로 흐름이 들어오지 않음

- 결과적으로 @ControllerAdvice는 404(Not Found) 상황을 기본적으로 잡을 수 없음

### 요약
- 404는 “컨트롤러에서 예외가 던져진 것”이 아니라 “매핑되는 컨트롤러 자체가 없음” 이라서
- 전역 예외 처리 클래스까지 도달하지 않는다.


## **404 에러는 누가 처리하는가?**

### 기본 동작 – 스프링 부트 + `templates/error/404.html`

- 스프링 부트에서는 기본적으로 **에러 처리 전담 컨트롤러**인  
  `BasicErrorController`가 에러 응답을 담당한다.

### 404 발생 시 처리 흐름

404(Not Found)가 발생하면 스프링 부트는 다음 순서로 동작한다:

1. 스프링의 **에러 처리 메커니즘**이 동작한다.
2. 내부적으로 **`/error` 경로로 포워딩**된다.
3. `BasicErrorController`가 `"/error"` 요청을 처리한다.

### 404 뷰 탐색 규칙

- 우선적으로 **`templates/error/404.html`** 을 찾는다.
  - 존재할 경우 → 해당 뷰를 사용해 404 페이지를 렌더링.
- 없다면:
  - 스프링 부트 기본 제공 **Whitelabel Error Page** 가 노출되거나,
  - 추가로 설정된 다른 에러 뷰/에러 처리 설정에 의해 적절한 화면이 보여질 수 있다.

### 요약

- **404 에러는** `@ControllerAdvice`가 아니라  
  **스프링 부트의 에러 처리 메커니즘 + `BasicErrorController`** 가 처리한다.
- **커스텀 404 페이지**를 만들고 싶다면 보통  
  `templates/error/404.html` 파일을 생성해서 적용한다.
<br><br>

---

# 📌 18번. `throw new Exception` vs `throws Exception` 정리

### 1. `throw new Exception` – **실제 예외를 던지는 행위**

```java
public void doSomething() {
    // ... 어떤 로직
    throw new Exception("문제 발생!");
}
```
- throw는 메서드 내부에서 실제 예외 객체를 던지는 구문이다.
- 이 줄이 실행되는 순간:
  1. 현재 메서드는 즉시 종료
  2. 예외가 호출한 쪽으로 전파
  3. 위쪽 호출 스택 어딘가에서 try/catch로 잡지 않으면 최종적으로 톰캣/런타임까지 올라가서 500 에러 등으로 이어질 수 있음

- 요약
  - throw = “지금 여기서 진짜 예외를 터뜨린다”
  
### 2. throws Exception – 예외를 던질 수 있다는 선언
```java
public void doSomething() throws Exception {
    // 이 메서드는 Exception을 던질 수 있다
}
```
- throws는 메서드 시그니처에 붙는 선언부이다.

- 의미:
  - “이 메서드는 Exception(또는 그 하위 타입)을 밖으로 던질 수도 있다”라는 약속/알림.
- throws가 있다고 해서 무조건 예외가 발생하는 것은 아님
  →  실제로는 메서드 내부에서 throw 하거나,
  → 내부에서 호출한 다른 메서드가 던진 예외를 그대로 전파할 때 발생.

- 요약
  - throws = “이 메서드를 호출하는 사람아, 이런 예외가 나갈 수도 있으니 대비해라”라는 선언

### 3. Checked Exception vs Unchecked Exception

### Checked Exception

- 예: `IOException`, `SQLException`, `Exception` 등
- **컴파일 시점에 반드시 처리해야 하는 예외**
- 메서드 안에서 이런 예외를 던질 수 있다면:
  - `try/catch`로 처리 **하거나**
  - 메서드 시그니처에 `throws`로 선언해야 함

```java
public void readFile() throws IOException {
    // 파일 읽기 로직
}
```
### Unchecked Exception (RuntimeException 계열)

- 예: RuntimeException, NullPointerException, IllegalArgumentException 등

- 컴파일러가 강제하지 않는 예외 (처리 여부는 개발자 선택)

- throws RuntimeException 처럼 선언할 수는 있지만, 굳이 메서드 시그니처에 throws를 붙일 필요는 없음
```java
public void doWork() {
    throw new RuntimeException("런타임 예외");
}
```
<br><br>

---

# 📌 19번. DTO와 Entity, 그리고 관련 애너테이션 정리

# DTO와 Entity 그리고 애너테이션 정리

## 1. DTO의 기본 개념

- DTO는 **계층 간 데이터 전달용 객체**.
- 특히 **응답 DTO**는 한 번 생성된 후 값을 변경하지 않는 **read-only** 용도로 쓰는 경우가 많음.
- **요청 DTO**는 스프링 바인딩 과정에서 값이 세팅되므로, 보통 **mutable(값이 변하는)**하게 설계되는 편.

## 2. Lombok 생성자 애너테이션

- `@NoArgsConstructor`
  - **파라미터가 하나도 없는 기본 생성자**를 자동 생성.
  - 예: 스프링 MVC/Jackson이
    1. 기본 생성자로 객체를 만들고
    2. 이후 `setXxx()` 메서드로 값을 채우는 패턴에서 사용됨.

- `@AllArgsConstructor`
  - **모든 필드를 파라미터로 받는 생성자**를 자동 생성.
  - 테스트 코드나, 간단한 객체 생성 시 유용하지만  
    필드가 많을수록 가독성이 떨어질 수 있음 → 실무에서는 `@Builder`와 함께 쓰는 경우가 많음.

## 3. 요청 DTO(Request DTO)

- 특징
  - **클라이언트 → 서버 방향** 데이터 전달.
  - 스프링의 데이터 바인딩 시, 기본 설정 기준으로
    - 기본 생성자로 인스턴스를 만들고
    - `setXxx()` 메서드를 통해 값이 채워짐.
  - 그래서 보통:
    - `@NoArgsConstructor` + `@Setter` + `@Getter` 조합이 자주 사용됨.
    - **전형적인 mutable 객체**로 두어도 크게 문제되지 않음.

- 정리
  - 한 번 만들어서 “반환”하기보다는,
    - **스프링이 만들어서 컨트롤러 파라미터로 넘겨주는 용도**.
  - `@Setter`가 있으면 스프링 바인딩 코드가 사용하기 편하고,  
    검증 로직 등에서 값 수정이 필요한 경우에도 유연함.

## 4. 응답 DTO(Response DTO)

- 특징
  - **서버 → 클라이언트 방향** 데이터 전달.
  - 서버에서 직접 값을 채워서 **내보내기만 하는 용도** → 외부에서 변경할 이유가 거의 없음.
  - **불변(immutable)에 가깝게 설계**하는 것을 선호.
  - JSON 직렬화(Jackson)는 **게터만 있어도 동작**하기 때문에:
    - 기본 생성자(`@NoArgsConstructor`)가 반드시 필요하지는 않음  
      (역직렬화가 아니라 직렬화만 한다면).

- 권장 패턴
  - `@Getter`만 사용하고, **`@Setter`는 붙이지 않음**.
  - 생성자 또는 `@Builder`로만 값을 세팅:
    - 응답 DTO를 만들 때만 필드를 채우고,
    - 그 이후로는 **읽기 전용**으로 사용.
  - 예시:
    ```java
    @Getter
    public class MemberResponseDto {

        private final Long id;
        private final String name;
        private final String email;

        @Builder
        public MemberResponseDto(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }
    ```

- 왜 불변이 좋은가?
  - `@Setter`가 있으면, 어디선가 실수로 `setXxx()`가 호출되어  
    응답 데이터가 **중간에 바뀌는 사이드 이펙트**가 발생할 수 있음.
  - 응답 객체는 보통:
    - “필요한 값을 모두 채운 **새 인스턴스**를 만들고”
    - 그 객체를 그대로 리턴하는 방식으로 사용.
  - 따라서:
    - `builder + no setter + final 필드` 조합을 쓰면  
      **생성 시점에만 값 세팅 → 이후에는 read-only** 로 유지 가능.

> 참고  
> `new` 연산자로 생성하더라도, **`@Setter`가 없고 모든 필드가 `final`이면**  
> 생성 이후에는 값을 바꿀 수 없음.  
> 즉, “`new`로 생성 = 위험” 이 아니라,  
> “**Setter가 있는 가변 필드가 있으면** 중간에 값이 바뀔 수 있다”가 핵심.

## 5. Entity와의 관계 (간단 참고)

- Entity는 **DB 테이블과 직접 매핑**되는 객체.
- JPA 스펙 때문에:
  - 기본 생성자(보통 `protected` 수준)가 필수.
  - 변경 감지(dirty checking)를 위해 **필드 값 변경(세터/비즈니스 메서드)** 이 허용되는 구조.
- 반면 DTO는:
  - **DB와 직접 매핑되지 않고**, 계층 간 데이터 전달 전용.
  - 요청 DTO는 mutable, 응답 DTO는 immutable에 가깝게 설계하는 식으로  
    **역할에 맞게 다르게 설계**하는 것이 일반적인 패턴.
<br><br>

---

# 📌 20번. Jackson 라이브러리의 역할 (요청/응답)

## Jackson(ObjectMapper)가 하는 두 가지 역할

스프링 MVC에서 Jackson(ObjectMapper)는 크게 **두 가지**에 사용된다.

### 1) 요청 바디(JSON) → 자바 객체 (역직렬화, Deserialization)

- 클라이언트가 전송한 **JSON 요청 바디**를
- 컨트롤러 메서드의 파라미터 타입에 맞는 **자바 객체로 변환**해 준다.
- 예: `@RequestBody`가 붙은 DTO에 JSON 데이터를 채워 넣는 과정.

### 2) 자바 객체 → 응답 바디(JSON) (직렬화, Serialization)

- 컨트롤러에서 반환한 **자바 객체**를
- 클라이언트로 내려보낼 **JSON 응답 바디**로 변환해 준다.
- 예: `@RestController`에서 객체를 리턴하면, Jackson이 자동으로 JSON으로 바꿔 응답.

### 한 줄 정리

> Jackson(ObjectMapper)는  
> **“들어오는 JSON → 자바 객체”**,  
> **“나가는 자바 객체 → JSON”**  
> 두 방향 변환을 담당하는 직렬화/역직렬화 도구이다.

- 직렬화
  -  프로그램 안에 있는 객체를 **저장하거나 전송하기 좋은 형태(문자열/바이트 덩어리)**로 바꾸는 것.
  - jackson 기준으로 보면, 직렬화는 자바 객체 -> JSON 응답 바디, 역직렬화는 JSON 요청 바디를 java 객체로 변경하는 것. 
  - `역직렬화` 기준에서 보면 클라이언트 단에서 application/json 형태로 넘어오는 것이 중요
<br><br>

---




