# 📌 21번. FormData 객체와 스프링 API 컨트롤러 (FormData multipart/form-data 이슈)

## 1. 지금 무슨 일이 일어난 건지

- `g_ajax`에서 `data`가 `FormData`인 경우, 내부적으로 `processData = false`, `contentType = false`로 설정된다.
- 이렇게 설정하면 브라우저가 자동으로 요청의 `Content-Type`을  
  **`multipart/form-data; boundary=...`** 형태로 지정해서 전송한다.
- 따라서 현재 요청의 본문 타입은 **`multipart/form-data`**이다.
- 서버에서는 다음과 같은 에러 메시지가 발생한다.  
  → `Content-Type 'multipart/form-data; boundary=...;charset=UTF-8' is not supported`

이 에러는 보통 **컨트롤러에서 `@RequestBody`로 DTO를 받을 때** 자주 발생하는 유형이다.  
이 경우 스프링은 기본적으로 **JSON 혹은 일반 텍스트 형태의 요청 바디**가 올 것으로 기대한다.

## 2. 왜 에러가 나는지 (원인)

- `@RequestBody`가 붙은 파라미터는 스프링 내부에서 **`HttpMessageConverter`**를 통해 파싱된다.
- 이때 주로 처리 가능한 미디어 타입은 예를 들어 다음과 같다.
  - `application/json`
  - `text/plain`
  - 그 외, 메시지 컨버터가 지원하는 기타 타입들
- 그런데 실제 요청의 `Content-Type`은 **`multipart/form-data`**이다.
- 현재 설정에서는 이 타입을 처리할 **적절한 `HttpMessageConverter`가 없기 때문에**,  
  결과적으로 **`HttpMediaTypeNotSupportedException`** 이 발생한다.

정리하면:

> **`multipart/form-data`로 들어온 요청을, 스프링이 “JSON 같은 요청 바디”로 올 거라고 가정하고 처리하려다가 실패한 상황이다.**

## 3. 자주 헷갈리는 포인트 정리

- `dataType: 'json'` 설정은  
  **“서버에서 돌아오는 응답(response)을 JSON으로 해석하겠다”**는 의미다.
- 이는 **“요청(request)을 JSON 형식으로 보낸다”**는 뜻이 아니다.
- 요청을 JSON으로 보내고 싶다면:
  - 요청의 `Content-Type`을 **`application/json`** 으로 지정해야 하고,
  - 실제 요청 바디에 **JSON 문자열**을 넣어 보내야 한다.
- 반대로, `FormData`를 사용하면 브라우저가 자동으로  
  **`multipart/form-data`** 타입으로 요청을 전송하게 된다.
<br><br>

---

# 📌 22번. DAO VS DTO, 서비스계층, 리파지토리 계층 구분

# 서비스 계층 vs 리포지토리(DAO) 계층, 그리고 DTO 정리

## 1. 서비스 계층과 리포지토리 계층, 둘 다 DAO인가?

### 1-1. 결론

- **일반적으로 `DAO = 리포지토리 계층`** 으로 보는 게 맞다.
- **서비스 계층은 DAO라고 부르지 않는다.**
- 서비스는 **비즈니스 로직**,  
  리포지토리/DAO는 **DB 접근**을 담당한다.

---

### 1-2. 용어 정리

#### ✅ DAO (Data Access Object)

- 말 그대로 **데이터베이스에 접근하는 객체**.
- SQL을 직접 실행하거나, JPA/마이바티스를 통해 DB에서 데이터를 조회/저장하는 역할.
- 예시
  - MyBatis의 `XXXMapper` 인터페이스
  - Spring Data JPA의  
    `MemberRepository extends JpaRepository<Member, Long>`

#### ✅ Repository (리포지토리)

- DDD에서 나온 개념이지만, 실무에선 **DAO와 거의 비슷한 의미**로 사용.
- “엔티티들의 집합(컬렉션)을 추상화한 저장소” 느낌.
- Spring Data JPA의 `Repository`, `JpaRepository` 들이 사실상 DAO 역할 수행.

#### ✅ Service (서비스 계층)

- **비즈니스 로직 담당 계층**.
- 여러 리포지토리/DAO를 조합해서 도메인 로직을 수행.
- 트랜잭션 단위, 업무 규칙, 시나리오를 구현하는 자리.
- 예:
  - “회원가입 시 업체도 생성하고, 담당자도 같이 생성”
  - “주문 생성 + 재고 차감 + 포인트 적립”


### 1-3. 계층 구조 그림

일반적인 웹 애플리케이션 계층 구조는 다음과 같다.

```text
[브라우저] 
   ↓ (요청/응답 DTO)
[Controller]
   ↓ (DTO, Entity 사용 가능)
[Service]       ← 비즈니스 로직, 트랜잭션
   ↓
[Repository/DAO]← DB 접근 전용
   ↓
[DB]
```
- DAO/Repository:
  - DB와 직접 통신하는 계층 → “DAO 계층”

- Service:
  - DAO/Repository를 이용해서 비즈니스 규칙을 구현하는 계층
    → DAO라고 부르지 않는다.
<br><br>

---

# 📌 23번. `String.isEmpty()` vs `String.isBlank()` 정리

## 1. 기본 개념 차이

### `isEmpty()`

- 문자열의 **길이가 0인지** 확인한다.
- 내부적으로는 `length() == 0` 인지만 보는 것과 같다.
- 아무 문자도 없을 때만 `true`가 된다.
- 공백 문자(스페이스, 탭, 개행 등)가 하나라도 있으면 길이가 0이 아니므로 `false`가 된다.

### `isBlank()` (Java 11+)

- 문자열이 다음 조건 중 하나이면 `true`:
  - 비어 있는 문자열 (길이 0)
  - 공백 문자(whitespace)로만 이루어진 문자열
- 공백 문자는 스페이스, 탭, 개행 등 `Character.isWhitespace()` 로 판단되는 문자들을 포함한다.

---

## 2. 공백 문자열에 대한 동작 비교

| 값                            | `str.isEmpty()` | `str.isBlank()` |
|-------------------------------|-----------------|-----------------|
| 빈 문자열 `""`                  | `true`          | `true`          |
| `" "` (스페이스 1개)             | `false`         | `true`          |
| `"   "` (스페이스 여러 개)       | `false`         | `true`          |
| `"\n\t"` (개행, 탭만 포함)       | `false`         | `true`          |
| `"abc"`                       | `false`         | `false`         |

정리하면:

- `isEmpty()`  
  → 정말 **“문자가 하나도 없는지”** 만 확인한다.
- `isBlank()`  
  → **“비어 있거나, 공백 계열 문자만 있는지”** 를 확인한다.

## 3. 검색 키워드 검증에 어떤 걸 쓰는 게 좋을까?

사용자가 검색창에 입력할 수 있는 값 예:

- 아무것도 입력하지 않은 빈 문자열 `""`
- 스페이스만 여러 개 `"   "`
- 개행만 입력한 경우 `"\n"`
- 스페이스 + 개행/탭 조합 `" \n\t"` 등

이런 것들을 모두 “검색어 없음”으로 처리하고 싶을 때:

- `isEmpty()` 만 사용하면:
  - 빈 문자열 `""` 만 걸러지고
  - 공백만 있는 `"   "`, 개행만 있는 `"\n"` 같은 값은 길이가 0이 아니므로 걸러지지 않는다.
- `isBlank()` 를 사용하면:
  - 빈 문자열, 공백 문자열, 개행·탭만 있는 문자열까지 한 번에 모두 “비어 있음”으로 처리할 수 있다.

따라서 **검색 키워드 검증 로직에는 `isBlank()`를 사용하는 것이 일반적으로 더 적절하다.**

일반적인 판단 기준은 다음과 같다.

- 키워드가 `null` 이거나
- 비어 있거나
- 공백 문자만 있는 경우

→ 이 경우 모두 “검색어 없음”으로 간주하고, 전체 조회 처리나 에러 처리로 분기하는 패턴이 자연스럽다.


## 4. `null` 처리 유의사항

`isEmpty()` 와 `isBlank()` 모두 **인스턴스 메서드**이므로, 대상 문자열이 `null` 인 상태에서 호출하면 `NullPointerException` 이 발생한다.

그래서 보통 다음과 같은 식으로 함께 사용한다.

- `keyword == null || keyword.isBlank()`
  - 키워드가 `null` 이거나, 비어 있거나, 공백만 있으면 “검색어 없음” 처리
- 또는 `keyword != null && !keyword.isBlank()`
  - 유효한 검색어가 들어온 경우에만 검색 로직 수행

항상 `null` 가능성을 고려해서 조건을 작성하는 것이 안전하다.


## 5. Apache Commons `StringUtils` 사용 시 (참고)

`org.apache.commons.lang3.StringUtils` 를 사용하면 `null` 까지 포함해 간단히 체크할 수 있다.

- `StringUtils.isEmpty(str)`  
  → `str` 이 `null` 이거나 길이가 0이면 `true`.
- `StringUtils.isBlank(str)`  
  → `str` 이 `null`, 빈 문자열, 공백 문자만 있는 문자열인 경우 모두 `true`.

검색 키워드 검증에서:

- `StringUtils.isBlank(keyword)` 한 줄로  
  `null`, 빈 문자열, 공백 문자열까지 모두 “검색어 없음”으로 처리할 수 있다.


## 6. 한 줄 요약

- **공백만 있는 문자열도 “비어 있다”라고 보고 싶다면 → `isBlank()` 사용**
- **정말로 길이 0인지만 보고 싶다면 → `isEmpty()` 사용**
- `null` 가능성이 있다면  
  `keyword == null || keyword.isBlank()`  
  혹은 `StringUtils.isBlank(keyword)` 같은 패턴이 가장 안전하다.

<br><br>

---

# 📌 24번. 전역 `@ControllerAdvice` 와 `title` 우선순위 정리

## 1. 상황 정리

- 개별 컨트롤러 메서드에서 모델에 다음과 같이 속성을 넣고 있다.  
  - 예: `title` 에 `"홈"` 같은 값을 직접 설정.
- 동시에, 전역 설정용으로 `@ControllerAdvice` 클래스에서  
  `@ModelAttribute` 메서드를 통해 공통 모델 속성을 넣고 있다.
- 원하는 동작:
  - **기본값**: 아무 데서도 `title` 을 설정하지 않으면 `"교정관리"` 로 표시.
  - **예외 페이지**: 특정 컨트롤러 메서드에서만 `"홈"` 같은 값으로 덮어쓰기.

## 2. 실행 순서

요청 한 번이 들어왔을 때 Spring MVC 의 처리 흐름(단순화):

1. `@ControllerAdvice` 안에 있는 `@ModelAttribute` 메서드들이 먼저 실행된다.  
   → 이 시점에서 전역 기본값(예: `title = "교정관리"`)이 `Model` 에 들어간다.
2. 특정 컨트롤러 클래스 내부에 `@ModelAttribute` 메서드가 있다면 그 다음에 실행된다.
3. 실제 요청을 처리하는 컨트롤러 핸들러 메서드  
   (예: `@GetMapping("/home")` 같은 메서드) 가 실행된다.  
   → 여기서 다시 `title` 을 설정할 수 있다.
4. 최종적으로 `Model` 이 뷰로 전달되고, 타임리프에서 `${title}` 같은 표현식으로 사용된다.

정리하면:

- 전역 → (컨트롤러 전역) → 개별 컨트롤러 메서드  
  순으로 `Model` 이 채워진다고 보면 된다.

## 3. 같은 key 에 값을 여러 번 넣을 때 동작

- `Model` 내부는 사실상 `Map<String, Object>` 와 비슷한 구조이다.
- 같은 key 로 여러 번 `addAttribute` 를 호출하면:
  - **마지막에 넣은 값이 최종값** 이 된다.  
    (이전 값은 새 값으로 덮어써지는 효과)

따라서 다음과 같은 흐름이 된다.

- 전역 `@ControllerAdvice` 의 `@ModelAttribute` 메서드에서:
  - `title` 에 `"교정관리"` 를 넣는다 → 기본값.
- 이후 어떤 컨트롤러 메서드에서:
  - `title` 에 `"홈"` 을 넣는다 → 전역 기본값을 덮어씀.

결과적으로:

- 해당 컨트롤러 메서드가 처리하는 뷰에서는 `${title}` 이 `"홈"` 으로 보이고,
- 아무 것도 설정하지 않은 다른 컨트롤러 메서드는  
  여전히 전역 기본값 `"교정관리"` 가 사용된다.

<br><br>

---

