# Claude 작업 메모

> 단위 작업 완료 후 트러블슈팅 과정, CS/Java/Spring 관련 학습 내용을 정리합니다.
> 신규 내용은 **아래에 누적**합니다.
>
> **학습 레벨**: `[기초]` 기본 개념 / `[중급]` 프레임워크 동작 이해 필요 / `[고급]` 내부 구조 분석 필요
> **유지보수 포인트**: 각 항목 끝에 "이 코드를 수정해야 할 때" 가이드 포함

---

## 2026-02-20 | 직원관리 즉시 수정 항목 처리 + 등록/수정 REST 분리

### 커밋 메시지 (권장)
```
직원 등록/수정 API REST 분리 및 안정성 개선

- POST /api/member/members (등록 201) / PATCH /api/member/members/{id} (수정 200) 분리
- MemberDTO SaveMemberInfo → CreateMemberReq / UpdateMemberReq 분리
- MemberServiceImpl memberCreate() / memberUpdate() 분리 및 로그 기록 추가
- WebConfig PATCH + multipart 처리용 MultipartResolver 오버라이드 추가
- memberModify.js fetch URL·method 등록/수정 분기 처리
- memberManage.js deleteAgentBtn 복붙 잔재 제거
- getMemberInfo() null 체크, itemAuthData null 체크 (NPE 방어)
- .gitattributes 추가 (프로젝트 전체 LF 통일)
```

---

### 1. CRLF / LF — 줄 바꿈과 `.gitattributes` `[기초]`

#### 개념
OS마다 줄 바꿈 문자가 다릅니다.

```
Windows : \r\n  (CR + LF, 2바이트)
Unix/Mac: \n    (LF, 1바이트)
```

#### 문제 상황
`git config core.autocrlf=true`인 Windows 환경에서 checkout하면
git이 자동으로 `\n` → `\r\n`으로 변환합니다.
이로 인해 도구(Claude Code Edit 툴 등)의 문자열 exact match가 실패할 수 있습니다.

```js
// 파일 실제 내용 (CRLF)
"location.href = `/member/memberModify`;\r\n"

// Edit 툴이 찾으려는 문자열 (LF)
"location.href = `/member/memberModify`;\n"

// → 매칭 실패 → "String to replace not found" 오류
```

#### 해결: `.gitattributes`로 강제 통일

```gitattributes
# 모든 텍스트 파일 LF 통일
* text=auto eol=lf

# Windows 스크립트만 CRLF 유지
*.bat text eol=crlf

# 바이너리는 변환 금지
*.jar binary
*.png binary
```

적용 후 기존 파일 정규화:
```bash
git add --renormalize .
```

> `.gitattributes`는 개인 git 설정(`core.autocrlf`)보다 **우선 적용**됩니다.
> 팀원 전체에게 동일하게 적용되므로 커밋 전 팀 공유 필요합니다.

#### 유지보수 포인트
- 새로운 바이너리 확장자가 생기면 (예: `*.xlsx`) → `.gitattributes`에 `binary` 항목 추가
- `git add --renormalize .` 실행 후 `git status`에서 변경으로 잡힌 파일이 많으면 → 내용 변경 없이 줄 바꿈만 변환된 것이므로 정상. 한 번에 커밋

---

### 2. REST API 설계 원칙 — URI는 명사, 메서드가 행위 `[기초]`

#### 나쁜 예 (RPC 스타일)
```
POST /api/member/memberSave      ← "저장" 동사가 URI에
POST /api/member/getMemberList   ← "가져오기" 동사가 URI에
```

#### 좋은 예 (REST 스타일)
```
POST   /api/member/members        ← 생성 (HTTP 메서드가 행위 표현)
PATCH  /api/member/members/{id}   ← 부분 수정
GET    /api/member/members        ← 목록 조회
GET    /api/member/members/{id}   ← 단건 조회
DELETE /api/member/members/{id}   ← 삭제
```

#### PUT vs PATCH
| 메서드 | 의미 | 보내지 않은 필드 처리 |
|---|---|---|
| `PUT` | 자원 전체 교체 | null / 기본값으로 덮어씀 |
| `PATCH` | 자원 부분 수정 | 기존 값 유지 |

직원 수정은 일부 필드만 업데이트하고 `loginId`는 변경하지 않으므로 `PATCH`가 적합합니다.

#### 유지보수 포인트
- 새 엔드포인트를 추가할 때 → URI에 동사(save, get, delete 등) 대신 명사 자원명 사용
- 수정 요청인데 전체 필드를 반드시 보내야 한다면 → `PATCH` 대신 `PUT` 고려
- Spring Controller에서 `@PostMapping("/memberSave")` 형태가 보이면 → REST 분리 대상

---

### 3. PATCH + multipart/form-data 문제와 해결 `[중급]`

#### 문제 원인
Servlet 스펙과 Tomcat은 기본적으로 `POST` 요청만 multipart body를 파싱합니다.

```java
// Tomcat 내부 동작 (대략적)
public Collection<Part> getParts() {
    if (!"POST".equals(getMethod())) {
        // PATCH, PUT 등은 여기서 예외 발생
        throw new IllegalStateException("multipart는 POST에서만 지원");
    }
}
```

#### 1차 시도 — method만 감싸기 (실패 원인 포함)

```java
// POST로 위장 → Tomcat 파싱은 성공
// BUT: 반환된 MultipartHttpServletRequest 객체도 "POST"를 보고
// → Spring MVC가 @PatchMapping을 찾지 못해 405 Method Not Allowed 발생
return super.resolveMultipart(wrapper);
```

Spring MVC의 요청 처리 순서:
```
1. DispatcherServlet.checkMultipart(request)  ← MultipartResolver가 request를 래핑
2. DispatcherServlet.getHandler(processedRequest)  ← 래핑된 request로 핸들러 탐색
   ↑ 이 시점에 request.getMethod()가 "POST"이므로 @PatchMapping 매칭 실패 → 405
```

#### 2차 시도 — 파싱 후 method 복원 (해결)

```java
// WebConfig.java
@Bean
public MultipartResolver multipartResolver() {
    return new StandardServletMultipartResolver() {
        @Override
        public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request)
                throws MultipartException {
            if ("PATCH".equalsIgnoreCase(request.getMethod())) {
                final String originalMethod = request.getMethod();

                // 1단계: Tomcat이 multipart를 파싱하도록 POST로 위장
                HttpServletRequestWrapper postFaked = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getMethod() { return "POST"; }
                };
                MultipartHttpServletRequest parsed = super.resolveMultipart(postFaked);

                // 2단계: 파싱 결과(파일·필드 데이터)는 유지,
                //        getMethod()만 원래 PATCH로 복원 → @PatchMapping 매칭 가능
                return new MethodRestoredMultipartRequest(parsed, originalMethod);
            }
            return super.resolveMultipart(request);
        }
    };
}
```

#### 처리 흐름
```
PATCH 요청 (multipart/form-data)
  └─ MultipartResolver.resolveMultipart()
       ├─ postFaked wrapper → getMethod() = "POST"
       │     └─ Tomcat: multipart 파싱 성공 ✓
       └─ MethodRestoredMultipartRequest
             ├─ getMethod() → "PATCH"  ← Spring 라우팅용
             └─ 파일/필드 데이터 → parsed에 위임
                     ↓
         Spring MVC: @PatchMapping("/members/{id}") 매칭 성공 ✓
```

#### 유지보수 포인트
- `PUT` 메서드에서도 multipart가 필요해지면 → `"PATCH".equalsIgnoreCase(...)` 조건에 `"PUT"` 추가
- `MethodRestoredMultipartRequest` 내부 메서드가 컴파일 오류 나면 → Spring Boot 버전 업그레이드 후 `MultipartHttpServletRequest` 인터페이스 메서드 변경 여부 확인
- multipart 없는 PATCH 요청(JSON body 등)은 → `isMultipart()` 에서 false 반환되므로 이 코드 경로를 타지 않음. 영향 없음

---

### 4. Spring Framework 버전별 API 변화 주의 `[중급]`

`getMultipartHeaders` 반환 타입이 버전에 따라 달라졌습니다.

```java
// Spring 5.x (Spring Boot 2.x)
MultiValueMap<String, String> getMultipartHeaders(String paramOrFileName);

// Spring 6.x (Spring Boot 3.x)
HttpHeaders getMultipartHeaders(String paramOrFileName);
```

`HttpHeaders`는 내부적으로 `MultiValueMap`을 구현하지만
제네릭 타입 파라미터가 다르기 때문에 잘못된 타입으로 override하면 컴파일 오류가 발생합니다.

```java
// ❌ 잘못된 override (Spring 6.x에서 컴파일 오류)
@Override
public MultiValueMap<String, String> getMultipartHeaders(String name) { ... }

// ✅ 올바른 override (Spring 6.x / Spring Boot 3.x)
@Override
public HttpHeaders getMultipartHeaders(String name) { ... }
```

> 인터넷 예제 코드의 상당수가 Spring 5.x 기준입니다.
> 복사해서 쓸 때 반드시 현재 프로젝트의 Spring Boot 버전(`build.gradle` 확인)과 맞는지 체크하세요.

#### 유지보수 포인트
- Spring Boot 버전 업그레이드 시 → `MultipartHttpServletRequest` 구현 클래스(`MethodRestoredMultipartRequest`) 인터페이스 메서드 시그니처 재확인
- 버전 확인 방법: `backend/build.gradle`의 `id 'org.springframework.boot' version '...'` 확인

---

### 5. MapStruct — `@BeanMapping` null 무시 전략 `[기초]`

직원 수정 매퍼에 적용된 옵션입니다.

```java
// MemberMapper.java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updateMemberByReq(MemberDTO.UpdateMemberReq req, @MappingTarget Member member);
```

| 전략 | 동작 |
|---|---|
| `SET_TO_NULL` (기본값) | source 필드가 null이면 target 필드도 null로 덮어씀 |
| `IGNORE` | source 필드가 null이면 target의 기존 값 유지 |

수정 API는 보내지 않은 필드(null)가 기존 DB 값을 덮어쓰면 안 되므로 `IGNORE`가 필수입니다.

```java
// 예: name만 수정 요청, 나머지 필드는 null
UpdateMemberReq req = new UpdateMemberReq("홍길동", null, null, ...);

// SET_TO_NULL이면 → member.nameEng = null, member.hp = null → 기존 데이터 유실 ✗
// IGNORE이면     → member.name = "홍길동", 나머지는 기존값 그대로 유지 ✓
```

#### 유지보수 포인트
- 새 필드를 `UpdateMemberReq`에 추가했는데 수정이 안 되는 것 같다면 → `IGNORE` 전략 때문에 null로 보낸 것. 프론트에서 값을 실제로 보내고 있는지 확인
- 특정 필드만 `IGNORE` 전략에서 제외하고 싶다면 → 해당 필드에 별도 `@Mapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)` 추가

---

## 16. `updatable=false` + MySQL `ON UPDATE CURRENT_TIMESTAMP` 패턴 [중급]

**배경**: `Agent`, `AgentManager` 등 모든 엔티티의 `update_datetime` 컬럼에 `@Column(updatable=false)`가 붙어 있는데, 서비스에서 `setUpdateDatetime(now)`를 호출하고 있어 "의도된 코드인지, 버그인지" 판단이 필요했다.

**분석 결과**:

- DB(`schema.sql`)에서 해당 컬럼이 `ON UPDATE CURRENT_TIMESTAMP`로 선언되어 있어 MySQL이 UPDATE 시 자동 갱신
- `updatable=false`는 JPA가 UPDATE 쿼리의 SET 절에 이 컬럼을 포함하지 않도록 의도적으로 설정한 것
- 서비스에서의 `setUpdateDatetime(now)` 호출은 영속성 컨텍스트의 필드 값을 바꾸지만 JPA는 `updatable=false` 때문에 SQL에 포함하지 않음 → **dead code**
- 실제 DB에는 MySQL 자동 갱신으로 반영되므로 동작에는 문제 없음

**제거 대상 서비스 목록** (프로젝트 전체):
- `AgentServiceImpl`, `ReportServiceImpl`, `MemberServiceImpl`
- `ItemServiceImpl`, `ItemCodeServiceImpl`, `EquipmentServiceImpl`, `CaliOrderServiceImpl`

```java
// Before (dead code - JPA가 UPDATE 절에 포함하지 않음)
entity.setUpdateDatetime(now);
entity.setUpdateMemberId(userId);

// After (DB가 자동 갱신하므로 setUpdateDatetime 호출 불필요)
entity.setUpdateMemberId(userId);
```

**핵심 메커니즘**:
```sql
-- schema.sql
update_datetime DATETIME NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP
```
```java
// Entity
@Column(name = "update_datetime", insertable = false, updatable = false)
private LocalDateTime updateDatetime;
```

#### 유지보수 포인트
- 새 엔티티에 `update_datetime` 컬럼을 추가할 때 → `updatable=false` + schema.sql `ON UPDATE CURRENT_TIMESTAMP` 쌍으로 선언
- `setUpdateDatetime()`을 다시 서비스에 추가해도 동작은 하지만 dead code → 추가하지 않을 것
- MySQL이 아닌 다른 DB로 전환 시 → `ON UPDATE CURRENT_TIMESTAMP` 지원 여부 확인 필요. 없으면 `updatable=false` 제거하고 서비스에서 직접 세팅해야 함

---