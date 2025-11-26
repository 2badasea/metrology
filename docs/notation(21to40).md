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

