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
