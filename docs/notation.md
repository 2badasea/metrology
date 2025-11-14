
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


---