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

# 📌 22번. 서비스 계층 vs 리포지토리(DAO) 계층, 그리고 DTO 정리

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

# 📌 25번. 전역 `@ModelAttribute` 와 `@RequestBody` 의 차이
## 부제: contentType과 processType, 일반 JSON과 JSON.stringify() 처리에 있어서의 차이점

## 1. 상황 정리

- 커스텀 함수 `g_ajax`에서  
  데이터가 **FormData 객체**일 경우:
  - `processData` 를 `false`
  - `contentType` 을 `false`  
    로 설정하고 있다.
- 궁금했던 점:
  1. 이렇게 하면 브라우저가 `Content-Type` 헤더를 **`multipart/form-data`** 형식으로 알아서 설정해 준다는 말이 맞는지?
  2. 만약 FormData인데도 `processData`와 `contentType`을 기본값 그대로 두고 요청을 보내면,  
     `Content-Type` 은 어떤 값으로 설정되는지?


## 2. FormData + `processData: false` + `contentType: false` 인 경우

- `contentType: false` 의 의미  
  → **jQuery가 `Content-Type` 헤더를 직접 설정하지 말라**는 뜻이다.
- 이 경우 실제 HTTP 요청 시:
  - 브라우저(XHR)가 데이터를 보고 적절한 `Content-Type` 을 **자동으로 설정**한다.
  - 이때 설정되는 값은 보통  
    **`multipart/form-data; boundary=----랜덤값`**  
    형식이다.
- 따라서 다음과 같은 이해는 **맞는 말**이다.
  - “FormData를 쓰면서 `processData: false`, `contentType: false`를 주면  
    브라우저가 알아서 `multipart/form-data`로 보낸다.”


## 3. FormData인데 `processData`·`contentType` 을 기본값으로 둘 때

- jQuery의 기본값:
  - `processData`: `true`
  - `contentType`:  
    **`application/x-www-form-urlencoded; charset=UTF-8`**
- FormData를 넘기면서도 이 둘을 **손대지 않고** 요청을 보내면:
  1. `processData: true` 상태에서  
     → jQuery가 데이터를 **쿼리스트링 형태로 직렬화하려고 시도**한다.  
       (FormData와 맞지 않는 동작)
  2. `contentType` 은 기본값 그대로  
     → **`application/x-www-form-urlencoded; charset=UTF-8`** 이 설정된다.

즉,

- FormData라고 해서 **자동으로 `multipart/form-data`가 되는 것이 아니다.**
- **직접 `contentType: false`, `processData: false` 를 지정했을 때만**  
  브라우저가 적절한 `multipart/form-data` 헤더를 채워 넣는다.


## 4. 핵심 요약

1. **FormData + `processData: false` + `contentType: false`**

   - jQuery가 `Content-Type` 헤더를 설정하지 않는다.
   - 브라우저가 자동으로  
     `multipart/form-data; boundary=...`  
     형식으로 설정한다.
   - 파일 업로드 등에서 사용하는 **정석 패턴**이다.

2. **FormData인데 두 옵션을 기본값으로 두는 경우**

   - `processData: true`
   - `contentType: application/x-www-form-urlencoded; charset=UTF-8`
   - `multipart/form-data`가 아니라  
     **폼 URL 인코딩 방식**으로 인식된다.
   - FormData 전송 방식과 맞지 않기 때문에 **비추천**이다.

## 5. 추가 요약

### 5.1 컨트롤러와 요청 데이터 처리

- 컨트롤러는 URL 요청에 대해 다음 두 축으로 동작한다.
  - **뷰 반환**: ViewResolver를 통해 뷰 이름을 해석하고 화면을 렌더링한다.
  - **데이터 반환(JSON/텍스트 등)**: HttpMessageConverter를 통해 객체를 JSON, 텍스트 등으로 변환한다.
- 요청 데이터는 **HTTP Body** 에 담겨 오며, `Content-Type` 헤더 값에 따라
  - `application/json`
  - `text/plain`
  - 기타 미디어 타입  
  등으로 인식되어 **알맞은 HttpMessageConverter**가 선택되어 파싱된다.


### 5.2 JSON / FormData 전송 시 Content-Type 기본 동작

- 클라이언트에서 다음과 같이 보낼 때:
  - 순수 JS 객체
  - `JSON.stringify()`로 만든 문자열
  - `FormData` 객체  
  이들을 **별도의 `contentType` 설정 없이 `$.ajax` 기본 설정으로 전송**하면,
  - jQuery의 기본 스펙에 따라 `Content-Type` 은  
    **`application/x-www-form-urlencoded; charset=UTF-8`** 으로 설정된다.
- 이 상태에서:
  - **FormData** 는 원래의 `multipart/form-data` 구조와 맞지 않게 전송되고,
  - **JSON 문자열** 역시 `application/json` 이 아니므로  
    `@RequestBody` 가 기대하는 형식과 맞지 않게 된다.
    - `@RequestBody`는 일반적으로 `application/json` 으로 오는 JSON 바디를 기대한다.
- 따라서 **FormData** 를 전송할 때는:
  - `processData = false`
  - `contentType = false`  
  로 설정하여,
  - jQuery가 `Content-Type` 을 직접 설정하지 않도록 하고,
  - 브라우저가 자동으로 적절한 **`multipart/form-data; boundary=...`** 헤더를 붙여서 전송하게 만드는 것이 정석이다.


### 5.3 JSON.stringify() 처리가 필요한 이유

- HTTP 요청 Body는 본질적으로 **문자(문자열) / 바이트 덩어리**다.
- 따라서 객체 형태의 데이터를 JSON으로 보내려면:
  - 먼저 **문자열(JSON 텍스트)** 로 직렬화한 뒤 (`JSON.stringify()`),
  - `Content-Type` 을 **`application/json`** 으로 설정해야 한다.
- 서버(Spring)에서 `@RequestBody + DTO` 로 받을 때:
  - 스프링은 **HttpMessageConverter + Jackson(ObjectMapper)** 를 사용해
    - JSON 문자열 → DTO 객체로 **역직렬화**한다.
  - 이 과정에서 Jackson은:
    - 인스턴스를 만들 **기본 생성자(@NoArgsConstructor)** 와
    - 필드에 값을 넣을 **통로(@Setter 등)** 를 필요로 한다.
  - 그래서 **요청 DTO** 에는 보통:
    - `@NoArgsConstructor`
    - `@Setter` (또는 `@Data`)  
    를 붙여주는 패턴이 많이 사용된다.


### 5.4 JSON.stringify() 를 하지 않았을 때의 처리 방식

- `JSON.stringify()` 를 하지 않고 JS 객체를 그대로 `data`에 넣고,
  - `contentType` 을 따로 설정하지 않으면,
  - jQuery가 이를 **폼 데이터 형식(`a=1&b=2`)** 으로 변환하여 전송한다.
- 이 경우 서버에서는 **폼/쿼리 파라미터**로 처리되며,
  - `@ModelAttribute`
  - `@RequestParam`  
  으로 받는 것이 자연스럽다.

#### @ModelAttribute

- `@ModelAttribute` 로 받는 경우:
  - `name=이바다&age=30` 같은 **폼 데이터**
  - `?name=이바다&age=30` 같은 **쿼리스트링**  
    에서 파라미터들을 읽어와,
  - 같은 이름을 가진 **자바 객체의 필드(프로퍼티)** 에 매핑한다.
- 즉, 여러 개의 파라미터를 **하나의 자바 객체**에 모아서 바인딩할 때 적합하다.

#### @RequestParam

- `@RequestParam` 은 개별 파라미터에 대응하는 방식이다.
  - 예: `name` 하나, `age` 하나처럼 각각의 단일 값에 매핑.
- 여러 값을 받고 싶으면:
  - `@RequestParam` 여러 개 선언
  - 또는 `Map<String, String>` 등으로 받을 수 있다.
- 반면 `@ModelAttribute` 는 **여러 파라미터 → 하나의 자바 객체(필드들)** 로 묶어서 받는 역할을 한다.

#### 스프링 시큐리티 로그인 필터 이슈 ($.ajax의 contentType 속성값에 의한)

- 로그인 요청에 대한 $.ajax의 contentType 속성값을 application/json으로 설정했을 경우, 시큐리티 필터가 로그인 요청을 가로챘을 때 데이터를 빈 값으로 받았던 이슈가 존재
  - 기본적으로 시큐리티이 formLogin은 폼 파라미터 형식으로 데이터가 넘어오는 것을 기대 ex) request.getParameter("username")
  - 필터는 자체적으로 JSON 바디를 파싱하지 않기 때문에 브라우저에서 application/json으로 넘어온 값에 대해 읽을 수가 없게 된다.

<br><br>

---

# 📌 26번. 요청 DTO를 MapStruct를 활용하여 Entity로 변환하는 과정에서 값을 추가하고 싶은 경우

## 1. 문제 상황

- 브라우저에서 넘어온 **요청 DTO**를 **Entity로 변환**하는 과정에서  
  기존 DTO에는 없는 값을 **추가로 세팅**하고 싶을 때가 있다.
- 이때 DTO에다가 억지로 `setter`를 호출해서 임의의 값을 넣은 뒤 Entity로 변환하는 방식은  
  **요청 DTO의 순수성을 해치고, 사이드 이펙트를 만들 수 있어서 비추천**이다.

---

## 2. 권장 패턴: MapStruct의 `source`/`target` 매핑 활용

MapStruct를 사용하고 있다면,  
**매퍼 메서드의 파라미터**를 통해 추가 값을 넘기고,  
`@Mapping(target = ..., source = ...)` 로 **Entity 필드에 주입**하는 방식이 깔끔하다.

### 2.1 호출부 예시

```java
YnType isVisible = YnType.y;

// memberJoinReq(요청 DTO)에 isVisible 필드는 없지만,
// 매퍼 호출 시 별도의 파라미터로 같이 넘긴다.
agentMapper.toAgentFromMemberJoinReq(memberJoinReq, isVisible);
```

### 2.2 Mapper 인터페이스 예시
```java
@Mapping(target = "isVisible", source = "isVisible")
Agent toAgentFromMemberJoinReq(MemberDTO.MemberJoinReq memberJoinReq, YnType isVisible);
```

- memberJoinReq
→ 브라우저에서 넘어온 원본 요청 DTO

- YnType isVisible
→ 서버에서 추가로 주입하고 싶은 값

- @Mapping(target = "isVisible", source = "isVisible")
→ 메서드 파라미터로 받은 isVisible 값을 Agent 엔티티의 isVisible 필드에 매핑하겠다는 의미

## 3. 동작 결과

- agentMapper.toAgentFromMemberJoinReq(memberJoinReq, YnType.y)를 호출하면,
  - MapStruct가 내부적으로 Agent 엔티티를 생성하면서
  - isVisible 필드에 YnType.y 값을 세팅한다.

- 즉, 요청 DTO는 건드리지 않고,
  - Entity 생성 시점에만 필요한 추가 값을 깔끔하게 주입할 수 있다.

## 4. 정리

- ✅ 요청 DTO에 임의로 setter를 호출해서 값 넣는 방식은 지양

- ✅ MapStruct 매퍼 메서드의 추가 파라미터 + @Mapping(target, source) 패턴을 사용

- ✅ 이렇게 하면 요청 DTO는 불변에 가깝게 유지하면서,
  Entity 생성 시 필요한 서버 측 값만 안전하게 주입할 수 있다.


<br><br>

---

# 📌 27번. Spring Security 요청 흐름 정리 (FilterChain ↔ UserDetailsService) 

### 1) 결론부터
- **UserDetailsService가 FilterChain보다 먼저 실행되는 구조가 아니다.**
- **항상 요청은 먼저 SecurityFilterChain(필터 체인)을 탄다.**
- `UserDetailsService.loadUserByUsername()`는 **필터 체인 내부에서 “인증이 필요한 순간”에만 호출**된다.


### 2) 로그인 요청 시(인증 시도 시) 순서
1. 클라이언트 요청 진입
2. **SecurityFilterChain 실행**
3. 로그인 처리용 인증 필터가 로그인 요청을 감지
4. 인증 매니저/프로바이더로 인증 시도
5. 이 시점에 프로바이더가 사용자 조회가 필요하면  
   → **UserDetailsService.loadUserByUsername(username) 호출**
6. 반환된 사용자 정보를 기반으로 비밀번호/계정상태/권한 등을 검사
7. 성공하면 인증 정보(Authentication)를 생성하고 **SecurityContext에 저장**(보통 세션에도 저장)

> 포인트: 로그인 과정에서 `UserDetailsService`는 “필터 체인 다음이 아니라”,  
> **필터 체인 내부 인증 흐름 중간에서 호출**된다.


### 3) 로그인 이후 일반 요청 시(이미 로그인된 상태) 순서
1. 요청 진입
2. **SecurityFilterChain 실행**
3. 세션 등에 저장된 SecurityContext를 복원
4. 이미 Authentication이 존재하면  
   → **대부분 UserDetailsService를 다시 호출하지 않는다**
5. 권한/인가 체크 후 컨트롤러로 통과

> 예외적으로 remember-me 복원, 다른 인증 방식(Basic 등)에서는 다시 호출될 수 있다.


### 4) loadUserByUsername()의 반환값은 무엇인가?
- `loadUserByUsername(username)`의 **반환 타입은 UserDetails**가 맞다.
- 기본 구현은 프레임워크가 제공하는 UserDetails 구현체를 반환하지만,
- 너처럼 `CustomUserDetails`를 만들어 `UserDetails`를 `implements` 했다면  
  **loadUserByUsername()는 CustomUserDetails를 반환**할 수 있다.


### 5) CustomUserDetails에 값을 더 담는 게 가능한 이유
- 로그인 인증 과정(위의 로그인 요청 흐름 5~7단계)에서  
  반환된 `UserDetails` 객체가 인증 정보의 “principal”로 사용된다.
- 따라서 `CustomUserDetails`에 `memberId` 같은 추가 정보를 담아두면,
  로그인 이후 매 요청에서 principal을 통해 그 값을 꺼내 쓸 수 있다.
  - `principal`는 현재 인증된 사용자 본체(신원 정보) => **"누가 로그인했는가?’에 대한 대표 객체"**


### 한 줄 요약
- **FilterChain → (인증 시도 시) UserDetailsService 호출 → UserDetails(=CustomUserDetails) 반환 → 인증 성공 시 SecurityContext 저장**

<br><br>

---

# 📌 28번.  @ResponseBody vs @ResponseEntity

### 1. 공통점

- 둘 다 **뷰 이름을 리턴하지 않고**,  
  **HTTP 응답 바디에 직접 데이터(객체, 문자열 등)를 실어서 반환**할 때 사용.
- 내부적으로 둘 다 **HttpMessageConverter**를 통해  
  자바 객체 → JSON / XML / String 등으로 **직렬화**해서 전송.
- 주로 **REST API 응답**에서 사용.

---

### 2. @ResponseBody

- **역할**
  - 컨트롤러 메서드의 **리턴 값을 그대로 HTTP 응답 바디에 작성**하라고 스프링에게 알려주는 애너테이션.
  - 주로 `@Controller` + `@ResponseBody` 조합으로 사용.
  - `@RestController` 사용 시에는 **클래스 레벨에서 자동으로 @ResponseBody가 적용**되는 것과 동일한 효과.

- **상태 코드 / 헤더**
  - 별도로 설정하지 않으면 **기본으로 200 OK**로 응답.
  - 상태 코드를 바꾸고 싶으면,
    - `@ResponseStatus` 애너테이션으로 고정값 지정하거나,
    - 예외 처리(전역 예외 처리기)에서 상태 코드 조정.

- **언제 주로 쓰나**
  - 단순히 **“바디에 이 객체/문자열만 담아서 보내면 된다”** 수준일 때.
  - 상태코드·헤더를 세밀하게 제어할 필요가 적은 경우.

---

### 3. @ResponseEntity

- **역할**
  - **응답 바디 + HTTP 상태 코드 + 응답 헤더**를 한 번에 담는 **응답용 객체**.
  - 리턴 타입을 `ResponseEntity<T>`로 선언하면,
    - `T` → 실제 응답 바디에 들어갈 객체
    - `HttpStatus` → 상태 코드
    - `HttpHeaders` → 헤더
    를 함께 제어할 수 있음.

- **상태 코드 / 헤더**
  - 컨트롤러 메서드에서 **상태 코드, 헤더를 동적으로 결정**할 때 유용.
    - 예: 생성 시 201, 유효성 실패 시 400, 권한 문제 시 403 등 상황별로 제어.
    - 특정 헤더(`Location`, `Content-Type`, 커스텀 헤더 등)를 추가하고 싶을 때.

- **언제 주로 쓰나**
  - REST API에서 **상태 코드가 의미를 가지는 설계**를 하고 싶을 때.
  - 요청 처리 결과에 따라 상태 코드·헤더를 다양하게 바꿔야 할 때.
  - 파일 다운로드, 리다이렉트(Location 헤더), 캐시 제어, 쿠키 설정 등  
    **HTTP 레벨을 디테일하게 다루는 경우**.

---

### 4. 정리 비교

| 항목                | @ResponseBody                                    | @ResponseEntity                                     |
|---------------------|--------------------------------------------------|-----------------------------------------------------|
| 리턴 타입           | 보통 도메인 객체, DTO, String 등                | `ResponseEntity<T>`                                 |
| 바디 직렬화         | O (HttpMessageConverter 사용)                    | O (내부적으로 동일 메커니즘 사용)                  |
| HTTP 상태 코드 제어 | 기본 200, `@ResponseStatus` 등으로 간접 제어     | **메서드마다 직접 설정 가능**                       |
| 헤더 제어           | 컨트롤러 레벨에서 직접 제어하기 어려움          | **응답마다 HttpHeaders로 자유롭게 지정 가능**      |
| 주 사용처           | 단순 JSON 응답, 기본 상태코드로 충분한 경우     | REST API에서 상태 코드, 헤더를 세밀하게 다룰 때    |

> 한 줄 요약  
> - **간단한 JSON 응답만 필요** → `@ResponseBody`  
> - **상태 코드/헤더까지 HTTP 응답 전체를 컨트롤** → `ResponseEntity<T>`


<br><br>

---

# 📌 29번. @ResponseEntity<?> 와 같이 '?'를 사용하게 되는 것의 효과

## 상황에 따라 응답 body의 형태가 달라질 수 있을 때 ?를 명시한다. 
```java
@GetMapping("/api/members/{id}")
public ResponseEntity<?> getMember(@PathVariable Long id) {
    MemberDto member = service.findById(id);

    if (member == null) {
        // 에러 JSON
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "code", "NOT_FOUND",
                    "message", "해당 회원을 찾을 수 없습니다."
                ));
    }

    // 정상 응답 JSON
    return ResponseEntity.ok(member);
}
```
- '?'가 아닌 구체적인 타입(DTO)를 명시한 경우, 스프링은 런타임 시점에 해당 타입의 객체를 받아서 Jackson으로 JSON 직렬화 -> HTTP 응답 바디에 넣어줌.
  - 컴파일 시점에 미리 정보를 주게 되는 셈.

- '?'를 명시할 경우, 위에 구체타입을 명시한 것과 동일한 형태로 응답. 즉 , ok(member)에 담은 데이터타입에 맞추어 알아서 JSON으로 만들어주는 것.

- 어차피 타입을 어떻게 명시하든 간에, 클라이언트에서 받는 것은 key, value 형태의 값이기 때문.
- 실무에선 가급적이면 타입을 구체적으로 명시해주는 것이 좋음

<br><br>

---

# 📌 30번. Java 9부터 추가된 정적 팩토리 메서드 ex) Map.of(), List.of() 등

```java
Map<String, Object> map = Map.of(
    "code", "NOT_FOUND",
    "message", "해당 회원을 찾을 수 없습니다."
);
```
- 특징
  - 반환 타입: 불변 Map (put/remove 불가, 하면 UnsupportedOperationException)
  - null 키/값 허용 안 함 → NullPointerException
  - 같은 키 중복으로 넣으면 → IllegalArgumentException
  - 작은 사이즈의 “간단한 맵” 만들 때 쓰기 좋음

### 다른 컬렉션 예시
```java
List<String> list = List.of("A", "B", "C");     // 불변 List
Set<Integer> set = Set.of(1, 2, 3);             // 불변 Set
Map<String, Integer> map = Map.of("A", 1, "B", 2);
```
공통적으로: 불변 컬렉션, null 요소 넣으면 안 됨, Set.of는 중복 요소 있으면 IllegalArgumentException 반환. 

<br><br>

---

# 📌 31번. `@RequestBody` vs `@ModelAttribute` 정리

### 1. 개념 요약

- **`@RequestBody`**
  - HTTP 요청 **바디 전체**를 읽어서 객체로 변환하는 방식.
  - 주로 JSON, XML 같은 **바디 기반 요청**에서 사용.
  - 내부적으로 `HttpMessageConverter`를 통해 역직렬화가 이뤄진다.

- **`@ModelAttribute`**
  - HTTP 요청에 흩어져 있는 **파라미터들**을 모아 객체의 프로퍼티에 바인딩하는 방식.
  - 쿼리스트링, 폼 전송 값 등에 사용.
  - 내부적으로 `DataBinder`가 요청 파라미터를 객체 필드/세터에 매핑한다.

### 2. 값을 어디서 읽는가?

#### `@ModelAttribute`

- 값의 출처:
  - 쿼리 파라미터 (`?name=...&age=...`)
  - `application/x-www-form-urlencoded` 폼 데이터
  - `multipart/form-data` (파일 업로드 시 일반 필드들)
- 특징:
  - “요청 파라미터 이름 ↔ 객체 필드 이름” 기준으로 바인딩.
  - URL 쿼리, 폼 필드 등 **여러 위치에 흩어진 값들을 한 번에 합쳐서** 객체로 만들어준다.
  - HTTP 메서드(GET/POST 등)와는 무관하게, “파라미터”라는 개념이면 전부 대상이 된다.

#### `@RequestBody`

- 값의 출처:
  - HTTP 요청의 **raw body**(본문) 전체
- 특징:
  - 쿼리 파라미터는 무시하고, 오직 **바디 내용**만으로 객체를 만든다.
  - JSON, XML, 텍스트 등 바디 포맷을 읽어, 해당 포맷에 맞게 역직렬화한다.
  - 필드 매핑은 “JSON 키(또는 XML 태그) ↔ 객체 필드 이름”을 기준으로 한다.

### 3. 요청 Content-Type 관점 비교

#### `@ModelAttribute`와 잘 어울리는 Content-Type

- GET 요청 (Content-Type 없음, 쿼리스트링만 사용)
- POST/PUT 등에서의:
  - `application/x-www-form-urlencoded`
  - `multipart/form-data`

주 용도는 **전통적인 웹 페이지 폼 전송**이다.

스프링은 이러한 요청들을:
- 폼 필드/쿼리스트링을 “요청 파라미터”로 인식하고
- 이를 `@ModelAttribute`로 선언된 객체에 바인딩한다.

#### `@RequestBody`와 잘 어울리는 Content-Type

- REST API에서 주로 사용하는 바디 기반 타입:
  - `application/json`
  - `application/xml`
  - `text/plain` (적절한 MessageConverter가 있을 때)

스프링은 이런 요청에 대해:
- `HttpMessageConverter`를 선택해
- 요청 바디 전체를 읽고 자바 객체로 변환한 뒤
- 그 결과를 `@RequestBody` 파라미터에 주입한다.

### 4. 주 사용 시나리오

#### 1) 웹 페이지 + 폼 전송 (Thymeleaf, JSP 등)

- `@ModelAttribute` 사용 비중이 높다.
- GET:
  - 검색 조건, 필터 값 → 쿼리스트링으로 전달 → `@ModelAttribute`로 바인딩.
- POST:
  - 회원가입/수정 폼 → `application/x-www-form-urlencoded` 전송 → `@ModelAttribute`로 바인딩.

#### 2) REST API + JSON

- `@RequestBody` 사용 비중이 높다.
- 프론트엔드(React, Vue 등)나 모바일 앱이 JSON으로 요청을 보내면:
  - 서버에서 `@RequestBody`로 DTO를 받고,
  - 응답은 `@ResponseBody` 또는 `ResponseEntity`로 JSON을 반환하는 구조.

### 5. 한눈에 보는 비교표

| 항목                          | `@ModelAttribute`                                                   | `@RequestBody`                                                   |
|-----------------------------|---------------------------------------------------------------------|------------------------------------------------------------------|
| 값 읽는 위치                  | 쿼리스트링, 폼 필드, multipart 등 **요청 파라미터**                   | HTTP 요청 **바디 전체**                                          |
| 주 사용 Content-Type         | `application/x-www-form-urlencoded`, `multipart/form-data`, GET 쿼리 | `application/json`, `application/xml`, 기타 바디 기반 타입        |
| 바인딩 기준                  | 파라미터 이름 ↔ 객체 필드 이름 (DataBinder)                         | 바디 포맷(JSON/XML) ↔ 객체 필드 이름 (HttpMessageConverter)     |
| 주 사용 시나리오              | 전통적인 웹 폼, 검색 조건, URL 파라미터                              | REST API에서의 입력 DTO                                          |
| 쿼리 파라미터도 함께 받는가? | 예. 쿼리 + 폼 값 모두 바인딩 대상                                    | 아니오. 바디만 사용                                               |

### 6. 요약

- **폼/쿼리 파라미터 기반 입력**: `@ModelAttribute`
  - 화면 폼 전송, 검색 조건, URL 파라미터 등을 객체로 묶을 때 적합하다.
- **JSON 바디 기반 입력**: `@RequestBody`
  - REST API에서 요청 바디(JSON 등)를 DTO로 받는 데 최적화되어 있다.

이 기준만 기억해도 대부분의 상황에서 적절한 애너테이션을 선택할 수 있다.

<br><br>

---

# 📌 32번. Stream API의 `toList()` vs `collect(Collectors.toList())`

### 1. 공통점

- 둘 다 **Stream의 요소를 `List`로 모을 때** 사용하는 방식이다.
- 예) `someStream.toList()`, `someStream.collect(Collectors.toList())`
- 두 방식 모두:
  - 스트림의 **순서를 유지**한다(순차 스트림 기준).
  - 스트림에 있던 요소들을 복사해서 새로운 리스트를 만든다  
    (원본 컬렉션과는 별개로 동작).

### 2. 가장 큰 차이: 가변(mutable) vs 불변(unmodifiable)

#### `stream.collect(Collectors.toList())`

- **“가변(mutable)” 리스트**를 반환하는 것이 일반적이다.
- 리턴받은 리스트에 대해:
  - `add`, `remove`, `clear`, `set` 등의 **수정 작업이 가능**하다.
- 명세 상 구체적인 구현 타입(ArrayList 등)이 보장되지는 않지만,
  - 관례적으로 **수정 가능한 리스트**가 온다는 전제 하에 쓰는 경우가 많다.
- 한마디로:
  > “스트림 → 수정 가능한 리스트 하나 만들어줘.”

#### `stream.toList()`

- Java 16부터 추가된 메서드.
- **“수정 불가(unmodifiable)” 리스트**를 반환하는 것이 명시되어 있다.
- 리턴받은 리스트에 대해:
  - `add`, `remove`, `clear`, `set` 등을 호출하면  
    → `UnsupportedOperationException` 발생.
- 단, 리스트가 **불변**인 것이지,
  - 리스트 안에 들어 있는 **객체 자체가 불변이라는 뜻은 아니다.**  
    (객체 필드 변경은 여전히 가능)

- 한마디로:
  > “스트림 → 읽기 전용 리스트 하나 만들어줘.”

### 3. “불변 vs 가변” 관점에서 정리

| 항목                          | `collect(Collectors.toList())`             | `toList()`                               |
|-----------------------------|--------------------------------------------|------------------------------------------|
| 반환 리스트 구조 수정 가능 여부 | 가능 (일반적으로 가변 리스트)                | 불가능 (구조 수정 시 예외)                |
| 의도                         | 이후 값 추가/삭제/변경이 필요한 컬렉션        | 결과를 “읽기 전용”으로 다루고 싶을 때       |
| 사용 가능 Java 버전          | Java 8 이상                                | Java 16 이상                             |

- **구조 수정**: `add`, `remove`, `clear`, `set` 등
- **구조는 불변이지만 요소는 불변 아님**:  
  `toList()`로 만든 리스트에 들어있는 객체들은 여전히 mutable일 수 있다.

### 4. `toList()`와 `List.of(...)`의 차이

- 공통점:
  - 둘 다 **수정 불가(unmodifiable)** 리스트를 반환한다.
- 차이점:
  - `List.of(...)`는 **`null` 요소를 허용하지 않는다**.  
    `List.of(1, null)` → `NullPointerException`
  - `stream.toList()`는 **스트림 안에 이미 `null`이 있다면 그걸 그대로 담을 수 있다.**
    (Stream 자체가 `null` 요소를 담고 있는 경우)

> 정리:  
> `List.of()`는 “상수 리스트” 만드는 용도에 가깝고,  
> `toList()`는 “스트림 결과를 읽기 전용 리스트로 받고 싶을 때” 쓰는 느낌.

### 5. “리턴값에서 수정이 되는지 여부” 정리

1. **수정 가능한 리스트가 필요**한 경우
   - `collect(Collectors.toList())` 사용  
   - 또는 타입을 명시하고 싶다면  
     `collect(Collectors.toCollection(ArrayList::new))` 같이 사용

2. **결과를 외부에서 건드리지 못하게 하고 싶을 때**
   - Java 16 이상: `stream.toList()` 사용
   - 또는 명시적으로 `Collections.unmodifiableList(...)` 적용

### 6. 실무에서의 선택 기준

- **Java 8 프로젝트**
  - `collect(Collectors.toList())`가 기본 선택지.
  - 불변 리스트가 필요하면 추후에 감싸기:
    - `Collections.unmodifiableList(list)` 등.

- **Java 16 이상 프로젝트**
  - “읽기 전용 결과 리스트”가 필요하면 `toList()` 선호.
  - “결과 리스트를 계속 수정할 컬렉션”이 필요하면 여전히  
    `collect(Collectors.toList())` 또는 `toCollection(ArrayList::new)` 사용.

### 7. 한 줄 요약

- `collect(Collectors.toList())`  
  > 수정 가능한 리스트가 필요할 때 쓰는, 전통적인 수집 패턴.

- `toList()`  
  > 결과를 건드리지 않을 읽기 전용 리스트로 받고 싶을 때 쓰는, 최신 문법.
  > 수정을 하기 위해 set(값)을 호출하는 경우, 바로 예외가 발생한다.


### 8. 예시 코드
```java
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StreamToListExample {

    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        // 1) collect(Collectors.toList()) : 보통 "가변 리스트" 반환
        List<Integer> mutableList = numbers.stream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toList());

        System.out.println("mutableList = " + mutableList); // [2, 4]

        // 수정 시도 - 일반적으로 정상 동작 (요소 추가 가능)
        mutableList.add(100);
        System.out.println("mutableList after add = " + mutableList); // [2, 4, 100]


        // 2) toList() : "수정 불가(unmodifiable) 리스트" 반환 (Java 16+)
        List<Integer> immutableList = numbers.stream()
                .filter(n -> n % 2 == 0)
                .toList();

        System.out.println("immutableList = " + immutableList); // [2, 4]

        // 수정 시도 - 런타임에 UnsupportedOperationException 발생
        immutableList.add(100); // 여기서 예외 발생
    }
}
```

<br><br>

---

# 📌 33번. @Modifying 옵션 정리 (flushAutomatically / clearAutomatically)

## 예시코드
```java
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
				UPDATE Agent a
				SET a.groupName = :groupName
				WHERE a.id in :ids
	""")
	int updateAgentGroupName(
			@Param("ids") List<Integer> ids,
			@Param("groupName") String groupName
	);
```

### 1) 기본값
- `flushAutomatically`, `clearAutomatically` **둘 다 기본값은 `false`**
- 필요할 때만 `true`로 켜는 옵션이다.

### 2) 각 옵션이 하는 일

#### `flushAutomatically = true`
- 수정 쿼리 실행 **전에** 영속성 컨텍스트를 **flush**(쓰기 지연 SQL을 DB에 반영)한다.
- 장점
  - 벌크 수정 쿼리 실행 전에 쌓여 있던 변경사항이 반영되어, **DB 기준 쿼리가 더 정확**해질 수 있다.
- 단점
  - “아직 DB에 나가면 안 되는 변경”까지 **의도치 않게 먼저 반영**될 수 있다(성능/제약조건/중간상태 노출 리스크).

#### `clearAutomatically = true`
- 수정 쿼리 실행 **후에** 영속성 컨텍스트를 **clear**(1차 캐시 비움, 관리 엔티티 detach)한다.
- 장점
  - 벌크 update/delete는 영속성 컨텍스트의 엔티티 상태를 자동으로 동기화하지 않는데,
    clear로 이후 조회가 DB에서 다시 일어나 **stale(묵은) 데이터 문제를 예방**한다.
- 단점
  - 현재 관리 중이던 엔티티들이 전부 detach되어, 이후 로직에서 **변경감지(dirty checking) 기대가 깨질 수 있음**.
  - flush되지 않은 변경이 있었다면 위험해질 수 있다.

### 3) 언제 `true`가 유리한가?
- **벌크 UPDATE/DELETE를 실행한 뒤**, 같은 트랜잭션에서 **바로 이어서 조회/검증/응답**까지 해야 해서
  “반드시 DB 기준 최신값”이 필요할 때
  - 이때는 보통 `clearAutomatically=true`가 특히 중요하다.

### 4) “무조건 true”가 위험한 대표 상황
- 같은 트랜잭션에서 다른 엔티티들을 수정 중인데 아직 DB에 쓰고 싶지 않은 상태에서
  - `flushAutomatically=true`로 인해 **의도치 않은 flush**가 발생할 수 있음
  - `clearAutomatically=true`로 인해 **관리 상태가 끊겨** 이후 로직이 흔들릴 수 있음

### 5) 실무 감각(추천)
- “벌크 수정 후 같은 트랜잭션에서 다시 조회/로직”이 있으면 → `clearAutomatically=true`를 우선 고려
- 트랜잭션 내에 다른 변경도 섞여 있으면 → `flushAutomatically=true`는 신중하게 사용
- 통제력이 필요하면 옵션 대신 필요한 지점에서 **명시적으로 flush/clear를 호출**하는 방식도 많이 쓴다.

### 6) 추가설명
- update, delete문에 `@Modifying` 애너테이션을 명시하지 않으면 오류를 반환.
- https://pgmjun.tistory.com/126 링크 참조

<br><br>

---

# 📌 34번. 이벤트 객체에 preventDefault() 메서드가 의미가 있는 경우
- `<form>` 태그 내에 button이 존재할 때. 물론, 이때도 type이 button이면 문제가 없음.
  - 기본 타입이 submit이기 때문에, 자동으로 form 전송 이벤트가 발생하는 걸 막기 위해선 preventDefault() 함수 호출
- `<a>`가 버튼으로만 쓰이고, href 속성값으로 이동하는 기본이벤트를 방지하고 싶을 때만.

<br><br>

---

# 📌 35번. radio에서 체크된 요소의 값을 가져오는 방법

## 예시코드
```javascript
// 내가 사용한 방식 (잘못된 방식) => name이 applyType인 요소 중 첫 번째를 가져오게 됨
const applyType = $('input[name=applyType]', $modal).val();

// 수정방향 (체크된 것을 명시해주어야 함)
const applyType = $('input[name=applyType]:checked', $modal).val();

// 또 다른 잘못된 표현 => is() 메서는 boolean을 리턴함. 불리언에 대해선 val() 제공 X
$('input[name=applyType]:checked', $modal).is(":checked").val();
```

### 요약
- 선택자로 지정해주면서 `:checked` 를 통해서 가져오면 된다.
- .is(':checked).val() 표현은 애초에 잘못되었고, is() 메서드의 반환타입은 boolean이다.

<br><br>

---

# 📌 36번. java에서 List<Integer>에 있는 값을 문자열로 변경하기 및 String.format()을 활용하여 원하는 형태의 메시지 생성하기

### 예시코드
```java
List<Integer> ids = List.of(1, 2, 3, 4, 5); // 불변객체
String agentName = "바다솔루션";

String strIds = ids.stream().map(String::valueOf).collect(Collectors.joining(", "));
String logContent = String.format("업체명이 %s로 수정되었습니다. 변경된 아이디: %s", agentName, strIds);
```

<br><br>

---

# 📌 37번. Stream API: `stream()`, `map()`, `collect()` 정리

### 1. `stream()` 메서드

- `stream()`은 `java.util.Collection` 인터페이스에 정의된 **디폴트 메서드**이다.
- 따라서 **`Collection`을 구현한 모든 객체**에서 호출할 수 있다.
  - 예: `List`, `Set`, `Queue` 등
- 리턴 타입은 제네릭 기반의 `Stream<T>` 이다.
  - 예: `List<Integer>` → `Stream<Integer>`
- 역할:
  - 컬렉션을 **스트림 파이프라인의 시작점**으로 변환해 주는 메서드이다.

### 2. `map()` 메서드

- `map()`은 `java.util.stream.Stream`에서 제공하는 **인스턴스 메서드**이다.
- 즉, **스트림 객체(예: `Stream<T>`)** 에서만 호출 가능하다.
- 호출 조건:
  - 레퍼런스 타입이 `Stream<T>` 여야 한다.
- 특징:
  - 스트림의 각 요소를 주어진 함수에 따라 다른 값으로 **변환**하는 연산이다.
  - `String::valueOf` 같은 메서드 레퍼런스를 인자로 받을 수 있다.
- 리턴 타입:
  - `Stream<R>`
  - 요소 타입이 바뀐 **새 스트림을 반환하는 중간 연산(intermediate operation)**이다.
  - 예: `Stream<Integer> → map(String::valueOf) → Stream<String>`

### 3. `collect()` 메서드

- `collect()` 역시 `Stream<T>`의 인스턴스 메서드이며,  
  스트림을 **실제 결과로 모으는 종단 연산(terminal operation)**이다.
- 호출 조건:
  - 레퍼런스 타입이 `Stream<T>` 여야 한다.
- 형태:
  - `collect(Collector<? super T, A, R> collector)`
- 리턴 타입:
  - 넘겨주는 **`Collector`의 정의에 따라 최종 타입 `R`이 달라진다.**
  - 예시:
    - `Collectors.joining(", ")` → `String`
    - `Collectors.toList()` → `List<T>`
    - `Collectors.toSet()` → `Set<T>`
- 요약:
  - 스트림에 담긴 요소들을 어떤 그릇(List, Set, String 등)에 **최종적으로 수집할지** 결정하는 단계이다.

### 한 줄 요약

1. `stream()`  
   → 컬렉션(`Collection<T>`)을 **`Stream<T>`로 바꾸는 시작점**

2. `map()`  
   → `Stream<T>`에서 요소를 변환해 **다른 타입의 `Stream<R>`을 만드는 중간 연산**

3. `collect()`  
   → `Stream<T>`를 **List, Set, String 등 최종 결과로 모으는 종단 연산**  
     (리턴 타입은 어떤 `Collector`를 사용하느냐에 따라 결정된다)

<br><br>

---

# 📌 38번. JS, Jquery 의 요소를 순회하는 함수에 대한 정리

### 1. `$(제이쿼리객체).each(function (index, item) {})`
- 문법적으로 배열도 순회 가능하지만 **비추천**.
- 주 용도: **DOM 요소 순회**에 적합.
  - 예: `$('form > div').each(function (idx, item) { ... });`

### 2. `$.each(배열 or 객체, function (index, item) {})`
- jQuery에서 **배열/객체 데이터 순회용**으로 유효.
- 배열일 때:
  - `index` → 인덱스
  - `item` → 요소
  - `this` → 현재 요소
- 예:
```js
  const checkedRows = $modal.grid.getCheckedRows(); // 배열 데이터
  $.each(checkedRows, function (index, item) {
    // this === item
  });
```

### 3. 자바스크립트 for ... of
- 바닐라 JS에서 배열 순회에 가장 적합한 구문.
- 값(value) 중심 순회.
- 예:
```js
for (const row of checkedRows) {
  // row가 각 요소
}
```

### 4. 자바스크립트 for ... in
- 배열 순회에는 부적합.
- 객체의 key(프로퍼티 이름) 순회용.
- 배열에 사용 시 인덱스(문자열 key)를 순회:
```js
const checkedRows = $modal.grid.getCheckedRows();
for (const index in checkedRows) {
  const row = checkedRows[index];
}
```
- 그리드 데이터 / 배열 순회에는 굳이 사용할 필요 없음.

### 5. 자바스크립트 forEach
- 콜백 시그니처: (value, index, array)
- 첫 번째 인자: value
- 두 번째 인자: index
- 배열, 유사 배열 모두 순회 가능.
- 특징:
  > 콜백에서 return false 해도 해당 콜백만 종료, 순회는 계속.
  > break, continue 사용 불가 (단순 반복문이 아니라 함수 콜백이기 때문).
  > 중간에 순회를 종료하는 기능이 없다 → 중간 종료가 필요하면 for, for...of, some, every 등을 사용하는 것이 좋다.

<br><br>

---

# 📌 39번. git 형상관리 기본

- 원칙
  > 기능별 브랜치 생성 feature/agent-manage (케밥형태)
  > 최초 push할 때 git push -u origin feature/agent-manage 로 입력
  > -u 를 입력하는 건 해당 기능별 로컬브랜치와 원격브랜치를 연결해주는 역할
  > 이후엔 단순 git pull, git push만 입력해도 됨

- 형상관리 루틴
  1. 기능별 브랜치에서 작업 후 커밋&푸쉬
  2. git fetch origin
      > 원격(origin)의 최신 커밋/브랜치 정보를 로컬에 내려받기만 하는 명령어
      > 현재 작업중인 브랜치와 무관하게 입력 가능
      > 단순히 원격상태를 최신화하는 명령어
  3. main에 병합하고, 해당 main의 최신소스를 다시 feature 브랜치에 반영하는 방법
      > 위 작업 이후, GitHub 웹에서 PR 생성 ( git fetch origin 이후)
      > 문제없으면  Merge pull request 버튼 클릭 → main에 병합 완료
  4. 각 PC에서도 main 최신화
      ```bash
      git checkout main
      git pull origin main
      ```
      - 위 명령어를 입력하면 로컬main도 깃허브 최신 main과 동일해짐.
  5. main을 먼저 업데이트하고, 그걸 feature에 merge
      ```bash
        # 1) 원격 최신 상태 가져오기
        git fetch origin

        # 2) main으로 이동해서 최신화
        git checkout main
        git pull origin main

        # 3) 다시 기능 브랜치로 이동
        git checkout feature/agent-manage

        # 4) 기능 브랜치에 main 내용 합치기
        git merge main  # 
        # === 혹은 ===
        # git rebase main   # (이건 히스토리 깔끔, 대신 조금 더 어렵고 push -f 필요)
      ```
      > git merge main: 현재 브랜치(feature/agent-manage)로 main의 변경사항을 한 번에 합쳐주는 것


<br><br>

---

# 📌 40번. JPA `save()` 호출 결과의 `null` 체크가 불필요한 이유

### save() 메서드 스펙
```java
// save() 메서드 시그니처 스펙은 아래와 같이 NotNull을 보장하고 있음. 
@NotNull <S extends T> S save(S entity);
```

- JPA Repository의 `save()`는 **성공적으로 반환되는 경우 `null`을 반환하지 않는 계약(Contract)** 을 가짐.
  - 시그니처에 `@NotNull`이 붙어 있어 **리턴값이 non-null** 임을 명시한다.
- 저장에 실패하는 상황(제약조건 위반, 트랜잭션/DB 오류 등)에서는 `null`을 반환하는 방식이 아니라, 보통 **런타임 예외가 발생**하며 서비스 계층 밖으로 전파된다.
- 따라서 서비스 계층에서는 `save()` 호출 직후에 리턴값 `null` 여부를 검사하기보다는,
  - 정상 흐름에서는 **바로 다음 로직을 이어서 작성**하고
  - 실패 흐름은 **예외 처리(전역 예외 처리, 트랜잭션 롤백 정책 등)** 로 다루는 방식이 일반적이다.

> 참고: “리턴값이 null이 아니다”와 “DB에 커밋까지 완료됐다”는 같은 의미는 아니다. 커밋/flush 시점은 트랜잭션 경계에 따라 달라질 수 있다.


 
