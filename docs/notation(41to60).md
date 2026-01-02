# 📌 41번. Java에서 날짜 데이터 파싱하는 방법

### 1. "yyyy-MM-dd" 문자열 → LocalDateTime 으로 변경
- 요청 파라미터가 "2025-12-01" 같은 형식일 때:
```java
String dateStr = request.getOrderStartDate(); // 예: "2025-12-01"

// 1) LocalDate로 파싱 (기본 ISO 포맷: yyyy-MM-dd)
LocalDate date = LocalDate.parse(dateStr);

// 2) LocalDateTime으로 변경 (예: 00:00:00 기준)
LocalDateTime dateTime = date.atStartOfDay(); // 2025-12-01T00:00:00
```

### 2. 검색 시작/종료 범위용 LocalDateTime 만들기
- 문자열 "yyyy-MM-dd" 두 개(시작일, 종료일)를 “하루 범위”로 바꾸는 패턴:
```java
String startStr = request.getOrderStartDate(); // 예: "2025-12-01"
String endStr   = request.getOrderEndDate();   // 예: "2025-12-12"

LocalDateTime startDateTime = null;
LocalDateTime endDateTime   = null;

if (startStr != null && !startStr.isBlank()) {
    LocalDate startDate = LocalDate.parse(startStr);   // 2025-12-01
    startDateTime = startDate.atStartOfDay();         // 2025-12-01T00:00:00
    
    // 아래 마지막 일을 구하는 과정에서 사용된 atTime(LocalTime.MIN)을 한 결과와 동일
    startDateTime = startDate.atTime(LocalTime.MIN);
}

if (endStr != null && !endStr.isBlank()) {
    LocalDate endDate = LocalDate.parse(endStr);      // 2025-12-12
    // 1) 하루의 끝까지 포함하는 방식
    endDateTime = endDate.atTime(LocalTime.MAX);      // 2025-12-12T23:59:59.999999999

    // 또는 2) 다음날 0시 직전까지
    // endDateTime = endDate.plusDays(1).atStartOfDay().minusNanos(1);
}
```

### 3. 문자열 "yyyy-MM-dd HH:mm:ss" → 날짜(LocalDate)만 뽑기
- 요청 값이 "2025-12-01 13:45:00" 이런 형태일 때, 날짜 부분만 쓰고 싶다면:
```java
String dateTimeStr = request.getSomeDateTime(); // "2025-12-01 13:45:00"

DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

// 1) LocalDateTime으로 파싱
LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);

// 2) 날짜 부분만 추출
LocalDate date = dateTime.toLocalDate();        // 2025-12-01
```

### 4. LocalDate 기준으로 그 달의 1일/말일 구하기
- 이미 LocalDate가 있을 때(예: 2025-12-10):
```java
LocalDate baseDate = LocalDate.of(2025, 12, 10);

// 1) 그 달의 첫 번째 일
LocalDate firstDay = baseDate.withDayOfMonth(1);
// 또는
// LocalDate firstDay = baseDate.with(TemporalAdjusters.firstDayOfMonth());

// 2) 그 달의 마지막 일
LocalDate lastDay = baseDate.with(TemporalAdjusters.lastDayOfMonth());
```

### 5. LocalDateTime 기준으로 그 달의 1일/말일을 “날짜”로 구하기
- LocalDateTime이 있을 때(예: 2025-12-10T13:45:00):
```java
LocalDateTime baseDateTime = LocalDateTime.of(2025, 12, 10, 13, 45);

// 1) LocalDate로 먼저 날짜 부분만 추출
LocalDate baseDate = baseDateTime.toLocalDate();

// 2) 그 달의 첫째 날/마지막 날
LocalDate firstDay = baseDate.withDayOfMonth(1);
LocalDate lastDay  = baseDate.with(TemporalAdjusters.lastDayOfMonth());
```

### 6. LocalDate → "yyyy-MM-dd" 문자열로 변경
- 가장 기본적인 방법은 그냥 toString():
```java
LocalDate date = LocalDate.of(2025, 12, 1);

String dateStr = date.toString(); // "2025-12-01" (ISO_LOCAL_DATE)
```

### 7. LocalDateTime → "yyyy-MM-dd HH:mm:ss" 문자열로 변경
```java
LocalDateTime dateTime = LocalDateTime.of(2025, 12, 1, 13, 45, 30);

DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

String dateTimeStr = dateTime.format(formatter); // "2025-12-01 13:45:30"
```
- 로그 남길 때, API 응답 JSON 만들 때, 템플릿에서 표시할 때 자주 쓰는 포맷.

<br><br>

---

# 📌 42번. JPA에서 entity를 조회한 다음, 해당 영속성 entity를 대상으로 save() 처리 없이 데이터를 업데이트 하는 경우

## 예시코드(업체등록/수정 로직) 일부
```java
for (AgentManagerDTO.AgentManagerRowData row : managers) {
    
    // 등록
    if (row.getId() == null) {
        AgentManager newEntity = agentManagerMapper.toEntity(row);
        newEntity.setAgentId(id);
        newEntity.setCreateDatetime(now);
        newEntity.setCreateMemberId(user.getId());
        // 저장
        agentManagerRepository.save(newEntity);
    }
    // 수정
    else {
        // 기존 영속성 entity 객체 가져오기
        AgentManager existingEntity = agentManagerRepository.findById(row.getId()).orElseThrow(() -> new EntityNotFoundException("해당 업체 담당자를 찾지 못 했습니다."));
        
        // NOTE 별도로 save() 안 해도, @Transactional 이면 dirty checking으로 업데이트 함
        // mapstruct를 이용하여 값 덮어쓰기
        agentManagerMapper.updateEntity(row, existingEntity);
        existingEntity.setUpdateDatetime(now);
        existingEntity.setUpdateMemberId(user.getId());
    }
}

// 참고로 아래는 mapstruct interface에 정의된 메서드
AgentManager toEntity(AgentManagerDTO.AgentManagerRowData agentManagerRowData); 
void updateEntity(AgentManagerDTO.AgentManagerRowData agentManagerRowData, @MappingTarget AgentManager entity);
```

## 설명
- 우선 해당 로직을 포함하고 있는 서비스메서드에 **@Transactional** 애너테이션이 있다고 가정할 경우, 트랜잭션이 시작되면서 `영속성 컨텍스트`가 진행
- findById() 호출
  - JPA가 DB에서 AgentManager row를 읽어와서 영속성 컨텍스트에 등록하고, 그 객체 레퍼런스를 existingEntity로 돌려줌
  - 이 시점의 existingEntity는 “영속 상태(managed)” 엔티티
- agentManagerMapper.updateEntity(row, existingEntity);
  - MapStruct가 existingEntity.setName(...), existingEntity.setTel(...) 처럼 필드들을 직접 바꿔치기한다.  

- existingEntity.setUpdateDatetime(now); 등도 역시 같은 객체의 필드 변경.

- 서비스 메서드 끝나는 시점에 트랜잭션 커밋
  - JPA(Hibernate)가 영속성 컨텍스트에 있는 엔티티들을 쭉 훑으면서 “처음 가져왔을 때 스냅샷과 지금 값이 달라진 애들”을 찾는다. 이게 dirty checking (변경 감지).
  - 변경된 필드가 있는 엔티티들에 대해서만 UPDATE agent_manager SET ... WHERE id = ? SQL을 만들어서 DB에 날림.

- 이 과정 전체를 한 문장으로 요약하면:
    > findById()로 가져온 영속 상태 엔티티의 필드를 바꾸기만 해도, 트랜잭션이 끝날 때 JPA가 변경을 감지해서 자동으로 UPDATE 쿼리를 만든다. 그래서 save()를 다시 호출할 필요가 없다.

## 왜 save()는 수정에만? 
- 등록 시에 생성된 newEntity는 생성된 순간에는 영속성 컨텍스트에 등록되지 않은 "새 객체" 상태
- 이걸 DB에 INSERT 하도록 JPA에게 알려줘야 하니까 save()를 통해 엔티티를 영속 상태로 등록 + INSERT 쿼리 예약을 시켜야 함.

<br><br>

---

# 📌 43번.  @Transactional(readOnly = true)가 하는 일
- @Transactional(readOnly = true)가 하는 일

### 구체적인 효과 (JPA + Hibernate 기준)

1. **Flush 모드 완화 (dirty checking 최소화)**  
   - Spring이 Hibernate의 flush 모드를 `FlushMode.MANUAL` 등으로 조정  
   - 커밋 시점에 굳이 “뭐 바뀐 거 있나?” 하고 전체 엔티티를 스캔하지 않음  
   - 조회 전용 메서드에서 성능 이득

2. **JDBC Connection에 read-only 힌트 전달**
   - 드라이버 / DB에 따라 `connection.setReadOnly(true)`를 호출  
   - 어떤 DB/인프라에서는 이 힌트를 보고 **읽기 전용 슬레이브 DB로 라우팅**하기도 함  
     (물론 이는 별도 설정이 필요함)

3. **개발자 의도 표현 / 문서화**
   - “이 메서드는 조회 전용이다”라는 것을 코드 레벨에서 명확히 보여줌  
   - 나중에 이 메서드에 실수로 `save()` 같은 쓰기 로직을 넣으면  
     “여긴 readOnly인데?” 하고 눈에 띄기 쉬움


### 중요한 포인트

- `readOnly = true`라고 해서 JPA가 **무조건 강제로 쓰기를 금지**시키는 것은 아님
- 일부 DB / `JdbcTemplate` / MyBatis 조합에서는 DML 수행 시 예외가 날 수 있지만,  
  JPA/Hibernate는 쓰기 금지라기보다는  
  > “**쓰기 안 할 거라고 가정하고 최적화**”  
  에 가깝다.
- 따라서 `readOnly = true`를 **쓰기 금지 검증 용도**로 믿고 쓰면 안 됨
- 정말로  
  > “여긴 진짜 조회만 하는 메서드다”  
  라는 곳에만 붙여주는 것이 베스트

<br><br>

---

# 📌 44번. `The database returned no natively generated value` 에러 정리
#### 1. 에러의 의미

- JPA/Hibernate가 **"DB가 기본키를 자동 생성해 줄 것"** 이라고 기대하고 INSERT를 수행했는데,
- INSERT 이후에 DB로부터 **생성된 PK 값이 전혀 돌아오지 않아서** 발생하는 예외.
- 보통 다음과 같은 경우에 발생:
  - 엔티티에는 `@GeneratedValue`가 선언되어 있고
  - DB 테이블 쪽에는 `AUTO_INCREMENT` / IDENTITY / SEQUENCE 설정이 없거나 잘못된 경우.

#### 2. 내부 동작 흐름 (개념)

1. 엔티티의 PK가 `null`이고, `@GeneratedValue`가 선언되어 있음  
   → JPA는 **DB가 PK를 생성해 줄 것**으로 믿음.
2. JPA가 PK 컬럼을 제외한 INSERT SQL 실행.
3. INSERT 후, JDBC의 `getGeneratedKeys()` 등을 통해  
   **"방금 INSERT된 row의 PK 값을 DB에게 요청"**.
4. 그런데 DB가 생성된 값을 전혀 반환하지 않음 →  
   → Hibernate가  
   `The database returned no natively generated value` 예외를 던짐.

#### 3. 대표적인 원인

1. **DB 테이블에 AUTO_INCREMENT / IDENTITY 설정 누락**
   - 엔티티:
     - `@Id`, `@GeneratedValue(...)` 등으로 "자동 생성 전략"을 사용하고 있음.
   - DB:
     - PK 컬럼이 단순 `BIGINT NOT NULL` 등으로만 선언되어 있고  
       `AUTO_INCREMENT`(MySQL/MariaDB) 또는 그에 상응하는 설정이 없음.
   - 결과:
     - JPA는 DB가 PK를 만들어줄 거라 믿고 INSERT  
     - DB는 실제로는 PK를 생성하지 않음  
     - 생성된 키 조회 결과가 비어 있어 예외 발생.

#### 4. 체크해야 할 것

- 엔티티:
  - `@Id`와 `@GeneratedValue(strategy = ...)` 선언 여부
  - 사용 중인 `strategy`가 DB 종류와 맞는지  
    - MySQL / MariaDB → 보통 `IDENTITY` + `AUTO_INCREMENT`
    - Oracle / PostgreSQL → `SEQUENCE` 등

- DB 테이블 DDL:
  - PK 컬럼이 **실제 자동 생성되도록** 설정되어 있는지
    - MySQL/MariaDB: `AUTO_INCREMENT`
    - 그 외 DB: SEQUENCE, IDENTITY 컬럼, 트리거 등
  - PK 제약 조건(Primary Key) 제대로 걸려 있는지

#### 5. 요약

- 이 에러는 **"엔티티는 PK를 DB가 자동 생성해줄 거라고 믿는데, DB는 아무 값도 안 돌려준 상황"**에서 터지는 예외다.
- 대부분은:
  - 엔티티에 `@GeneratedValue`를 달아놨지만
  - DB 테이블 PK 컬럼에 `AUTO_INCREMENT`(또는 해당 DB의 자동생성 설정)를 빼먹었을 때 발생한다.
- 해결책:
  - PK 자동 생성 전략을 **엔티티와 DB DDL 양쪽에서 일관되게** 맞춰주는 것.

<br><br>

---

# 📌 45번. MYSQL 컬레이션 정리

## **부제: utf8mb3 → utf8mb4_0900_ai_ci 변경 시 차이**

### 1) 문자셋(Character Set) 차이: utf8mb3 vs utf8mb4
- utf8mb3
  - UTF-8의 1~3바이트 문자만 저장 (4바이트 문자 저장 불가: 이모지/일부 확장문자 등)
  - MySQL에서 deprecated(향후 제거 예정)
- utf8mb4
  - UTF-8의 1~4바이트 전체 저장 가능 (이모지 포함)
  - 신규/현업 표준 선택지

실무 영향
- 사용자 입력/외부 데이터에서 이모지 등이 들어오면 utf8mb3는 저장 오류가 날 수 있음
- utf8mb4는 이를 근본적으로 해소

### 2) Collation(정렬/비교 규칙) 차이: … → utf8mb4_0900_ai_ci
- utf8mb4_0900_ai_ci 의미
  - 0900: Unicode Collation Algorithm(UCA) 9.0.0 기반 규칙
  - ai_ci: accent-insensitive + case-insensitive (악센트/대소문자 구분 안 함)
  - NO PAD: 문자열 끝(후행) 공백(trailing spaces)을 "의미 있는 문자"로 취급

실무 영향(중요 포인트)
1) UNIQUE/중복 판정 변화 가능
   - ai_ci 특성상 악센트/대소문자를 구분하지 않으므로,
     기존 데이터에 따라 유니크 충돌이 새로 발생할 수 있음
2) 후행 공백 비교 변화(NO PAD)
   - 'a' 와 'a '를 다르게 볼 수 있어
     과거(PAD SPACE 기반)와 결과가 달라질 수 있음
3) 호환성(버전/엔진) 이슈
   - MySQL 8 계열 콜레이션이라
     MySQL 5.7 또는 MariaDB로 덤프/이관 시 "Unknown collation" 문제가 생길 수 있음

## MySQL 실무에서 자주 쓰는 collation 종류와 특징

### A. 범용 기본값(신규/일반 업무 시스템)
- utf8mb4_0900_ai_ci
  - MySQL 8의 기본값(대부분 무난)
  - ai_ci + NO PAD 특성 주의(유니크/후행공백)

### B. 구분을 더 엄격히 하고 싶을 때
- utf8mb4_0900_as_ci
  - 악센트 구분(as), 대소문자는 미구분(ci)
- utf8mb4_0900_as_cs
  - 악센트(as) + 대소문자(cs) 모두 구분
  - 아이디/닉네임 정책에서 "정확히 구분"이 필요할 때 고려

### C. 완전 동일성(바이트 단위) 비교가 필요할 때
- utf8mb4_0900_bin 또는 utf8mb4_bin
  - 토큰/해시/서명값/정확 매칭 키 등에 적합

### D. 호환성(구버전 MySQL / MariaDB 이관 가능성)이 중요할 때
- utf8mb4_unicode_ci
  - 레거시/이관 호환성 목적의 타협안으로 자주 사용

### E. 과거에 흔했지만 신규 기본값으로는 덜 추천
- utf8mb4_general_ci
  - 예전엔 널리 사용
  - 정렬/언어 처리의 정교함 측면에서 0900 계열 대비 단순한 편

### 선택 가이드(실무 요령)
- 대부분: utf8mb4_0900_ai_ci
- “대소문자/악센트까지 구분해야 함”: utf8mb4_0900_as_cs
- “바이트까지 완전 동일해야 함”: utf8mb4_0900_bin(또는 utf8mb4_bin)
- “MySQL 5.7/MariaDB로 옮길 가능성이 큼”: utf8mb4_unicode_ci 고려


<br><br>

---

# 📌 46번. 금액 필드 기본값(0) 처리 정리

## 예시코드 (Report Entity)
```java
	// 교정수수료
	@Column(name = "cali_fee", nullable = false)
	@Builder.Default
	private Long caliFee = 0L;
	
	// 추가금액
	@Column(name = "additional_fee", nullable = false)
	@Builder.Default
	private Long additionalFee = 0L;

```

### 1) 현재 엔티티 선언은 OK
- `nullable = false` + `0L` 초기값: `null` 방지에 유효
- `@Builder.Default`: Lombok Builder 사용 시 기본값(0) 유지

### 2) “DB 기본값 0”을 원하면 DB에도 DEFAULT 0을 반드시 적용
- 엔티티 초기값만으로는 DB 스키마에 `DEFAULT 0`이 확실히 생긴다고 보장되지 않음(환경/DDL 설정 영향)
- 권장 컬럼 정의 예:
  - `cali_fee BIGINT NOT NULL DEFAULT 0`
  - `additional_fee BIGINT NOT NULL DEFAULT 0`

### 3) 주의: 부분수정(PATCH)에서 의도치 않은 0 덮어쓰기 가능
- 요청 DTO에서 값 미전송(=변경 없음) 상황이 `0` 업데이트로 이어질 수 있음
- 대응(택1):
  - 금액은 항상 명시적으로 보내는 정책
  - DTO는 nullable로 두고 `null이면 set하지 않기`(MapStruct면 null 무시 전략 적용)

### 4) 타입 선택 가이드
- 원 단위 정수: `Long` 유지(단순/권장)
- 소수점 필요(세금/환율/단가 등): `BigDecimal` 고려

### 결론(권장 조합)
- 엔티티: `0L` 기본값 + `nullable=false` 유지
- DB: `NOT NULL DEFAULT 0`로 스키마에도 기본값을 고정
- 로직: 부분수정 시 `null` 처리 정책을 명확히



<br><br>

---

# 📌 47번. 검색필터(all/ 빈값/ NULL)와 서버에서의 처리 방식

## 1) 개념 구분
- **searchType**
  - “어떤 기준(컬럼/방식)으로 검색할지”를 의미하는 **검색 방식 값**
- **검색필터(orderType 등)**
  - “조건 적용/미적용”이 가능한 **필터 값**
  - **전체선택(=조건 미적용)** 상태가 존재함

## 2) 서버에서 NULL로 정규화하면 쿼리가 단순해짐
- 리포지토리 조건식 예시
  - `(:orderType IS NULL OR o.orderType = :orderType)`
- `orderType`이 **NULL**이면 조건을 “미적용”으로 처리할 수 있어 쿼리 작성이 깔끔해진다.
- 따라서 프론트에서 어떤 값을 보내든(예: `all`, `''`) 서버에서 **NULL로 통일(정규화)**하는 편이 좋다.

## 3) 필터 값으로 `all`은 Enum 타입에서 위험
- 서버 필터가 **Enum**이라면 보통 `ACCREDDIT`, `TESTING`처럼 **대문자 상수**만 유효
- 프론트가 `all`을 보내면:
  - Enum 변환 과정에서 유효하지 않은 값 → **type mismatch / 변환 오류** 발생 가능
- 그래서 “전체선택” 표현은 `all`보다 **빈 문자열 `''`**가 더 안전한 경우가 많다.
  - **왜냐하면 빈 문자열을 `Enum` 필드에 매핑하게 될 경우, NULL로 할당되기 때문.**

## 4) 빈 문자열이 NULL로 바인딩되는지는 “요청 바인딩 방식”에 따라 다름 (핵심)
### A. 폼/쿼리스트링 바인딩
- `@ModelAttribute`, `@RequestParam`
- `orderType=`처럼 **빈 값**은
  - 상황에 따라 **null로 들어오거나**
  - 서버에서 **null로 정규화하기 쉬운 형태**로 들어온다

### B. JSON 바인딩
- `@RequestBody`
- `{ "orderType": "" }` 형태에서 DTO 필드가 **Enum**이면
  - JSON 역직렬화 과정에서 **실패**하는 경우가 많다
- 결론:
  - `@RequestBody`를 쓸 때는 **Enum으로 바로 받지 말고 String으로 받는 설계**가 안전할 수 있다
  - 또는 커스텀 역직렬화/컨버터, 필드 omit 전략 등을 고려해야 한다

## 5) 프로젝트 적용 결론(Toast Grid 기준)
- Toast Grid 요청은 보통 `@RequestBody`가 아니라 **쿼리스트링/폼 바인딩 형태**로 들어온다(프로젝트 기준).
- 따라서 “전체선택” 검색필터 값은 프론트에서 **빈 문자열 `''`로 보내는 것이 안전**
- 서버에서는 이를 **NULL로 정규화**한 뒤,
  - `(:param IS NULL OR ...)` 패턴으로 조건을 깔끔하게 처리한다


<br><br>

---

# 📌 48번. 자바스크립트 배열, 객체에 대한 typeof 연산과 const로 선언된 객체, 배열 데이터에 대한 수정

## 예시코드
```javascript
const ary = [1,2,3, 4];
typeof ary;   // 'Object'

// Array.isArray(ary); // true

```
- JS에서 데이터셋의 형태가 배열인지 확인하기 위해선 typeof으로는 구분이 되지 않는다.
  - 자바스크립트에선 배열 또한 객체로 간주되기 때문에 typeof을 통해서는 구분이 되지 않음
- **Array.isArray(변수)** 형태로 사용 권장
  - 데이터셋이 배일인 경우, true를 리턴하게 된다.

## 배열이나 객체 데이터셋에 대해서 const 키워드를 통해 변수를 사용한 경우, 데이터 조작은 하면 안 된다? 
- `const`의 의미는 내부 데이터 값이나 구조를 변경하면 안 된다는 의미보다는 동일한 변수명으로 재할당이 안 된다는 의미로 사용된다.
  - 따라서, `const`로 선언하더라도 요소를 추가하거나 구조를 변경하는 것은 무관하다.

<br><br>

---

# 📌 49번. `@RequestBody` vs `@ModelAttribute` 핵심 차이 정리

## 1) 질문 배경(컨텍스트)
- 삭제 대상 성적서를 검증하기 위해
  - “타입별 성적서 ID 목록”
  - “접수 ID” 등을 **서버로 함께 전달**해야 하는 상황
- 프론트에서 데이터를 `JSON.stringify()`로 직렬화하고, 요청의 `Content-Type`을 `application/json`으로 설정해 전송하는 흐름

## 2) 결론부터: `Content-Type`이 무엇이냐가 바인딩 방식 선택을 사실상 결정한다
### `application/json`이면 → `@RequestBody`가 정석
- JSON은 **요청 본문(HTTP Body)** 에 담겨 전달된다.
- Spring은 `@RequestBody`를 통해 Body 내용을 읽고,
  **JSON 역직렬화(deserialization)** 를 수행해 DTO로 매핑한다.

### `@ModelAttribute`는 “파라미터 기반 데이터”에 적합
- `@ModelAttribute`는 다음 유형과 궁합이 좋다:
  - 쿼리스트링(Query String)
  - form-data(멀티파트 포함)
  - `application/x-www-form-urlencoded`
- 즉, “URL 파라미터 / 폼 전송”처럼 **key=value 중심의 파라미터 데이터셋**을 객체로 묶는 성격이 강하다.

## 3) `@RequestBody`가 하는 일(핵심 메커니즘)
- 요청 본문(Body)의 JSON 문자열을 읽는다.
- JSON을 자바 객체로 변환하는 **역직렬화 과정**을 거친다.
- 이때 DTO의 **필드명(또는 JSON key)** 을 기준으로 매핑된다.
  - 결과적으로 “DTO 필드명 ↔ JSON key”가 일치해야 자연스럽게 바인딩된다.
- 이 과정은 단순히 “문자열 파라미터를 끼워 맞추는” 방식이 아니라,
  **객체 변환(파싱) 단계**가 개입된다는 점이 중요하다.

## 4) 실무적으로 기억할 포인트
- `application/json` + `JSON.stringify()`로 보내는 요청은
  - **Body 중심 통신**
  - **역직렬화 기반 매핑**
  - 따라서 `@RequestBody`가 맞다.
- 반면 `@ModelAttribute`는
  - URL/폼 기반의 **파라미터 집합을 객체로 묶는 용도**에 적합하다.
- 즉, 둘의 차이는 단순한 “문법 차이”가 아니라,
  **데이터가 실려 오는 위치(Body vs Parameter)** 와 **바인딩 방식(역직렬화 vs 파라미터 바인딩)** 의 차이라고 보면 된다.

<br><br>

---

# 📌 50번. Spring Data JPA 메서드명 규칙: `Containing` vs `In`

## 1) 조회 메서드 예시
- `findByIdInOrParentIdInOrParentScaleIdIn(deleteIds, deleteIds, deleteIds)`
- 목적: 삭제 대상 ID 목록(`deleteIds: List<Long>`)에 대해
  - `id`가 포함되거나
  - `parentId`가 포함되거나
  - `parentScaleId`가 포함되는
  레코드들을 한 번에 조회

## 2) `List<Long>` 조건에는 `In`을 사용
- Spring Data JPA에서 **컬렉션(List/Set 등)을 조건으로 전달**해서
  “해당 컬럼 값이 이 목록 안에 포함되는지”를 체크하려면 메서드명에 `...In` 패턴을 사용한다.
- 의미:
  - SQL의 `IN (...)` 조건과 동일한 역할

## 3) `Containing`은 문자열 검색 용도
- `Containing`은 기본적으로 **문자열 컬럼**에서 “부분 포함(Like)” 검색을 의미한다.
- 즉, `"abc"`가 `"xxabcxx"`에 포함되는지 같은 **부분 문자열 매칭**에 사용된다.
- 따라서 `List<Long>` 같은 컬렉션 기반 조건에는 `Containing`이 맞지 않고,
  의도한 동작(=IN 조건)을 표현하지 못한다.

## 4) 정리
- `List<Long>`로 “목록 포함 여부” 조건을 걸고 싶다 → **`In`**
- 문자열 컬럼에서 “부분 문자열 포함” 검색을 하고 싶다 → **`Containing`**


<br><br>

---

# 📌 51번. JPQL에서 Enum 상수를 쓸 때 “풀 패키지 경로(FQN)”가 필요한 경우 정리

## 1) 언제 패키지 경로까지 써야 하나?
### JPQL 문자열 안에 Enum 상수(리터럴)를 “직접 박는 경우”
- 예: `... and o.caliType = CaliType.STANDARD` 처럼 쓰고 싶어도,
  JPQL은 자바의 `import` 개념이 없어서 보통 아래처럼 **풀네임(FQN)** 이 필요하다.
- 즉, **JPQL 문자열 내에서 Enum 상수 자체를 직접 참조할 때만** 패키지 경로가 필요하다고 보면 된다.


## 2) 언제 패키지 경로를 생략할 수 있나?
### A. Enum을 “파라미터로 바인딩”해서 비교할 때 (권장)
- `... and o.caliType = :caliType`
- 이때는 컨트롤러/서비스에서 `CaliType.STANDARD` 값을 파라미터로 넘기면 되고,
  JPQL 문자열 안에서는 **패키지 경로를 전혀 쓰지 않는다.**
- 장점
  - JPQL이 단순해짐
  - 문자열에 상수를 박는 방식보다 변경/리팩터링에 안전
  - 테스트/재사용에 유리

### B. Querydsl / Criteria API처럼 “JPQL 문자열을 직접 쓰지 않는 방식”
- 이 경우는 자바 코드 레벨에서 Enum을 참조하므로,
  당연히 JPQL 안에 FQN을 쓸 일이 없다.

### C. Spring Data의 SpEL을 통해 `T()`로 참조하는 경우(선택지)
- JPQL을 직접 박는 대신, 스프링 표현식으로 Enum 상수를 가져오는 패턴이 있다.
- 다만 이 방식은 **결국 타입 참조에 FQN이 들어가므로**, “완전 생략”이라기보다는
  “JPQL 리터럴 직접 박기”를 조금 더 관리 가능한 형태로 바꾸는 정도로 이해하면 된다.


## 3) 왜 `@Enumerated(EnumType.STRING)`를 권장하나?
### JPQL이 통과해도 “DB 저장 방식” 때문에 런타임 이슈가 날 수 있음
- Enum을 DB에 저장하는 방식은 대표적으로 2가지가 있다.
  - `EnumType.STRING` : `"STANDARD"` 같은 **이름** 저장
  - `EnumType.ORDINAL` : `0, 1, 2` 같은 **순서(인덱스)** 저장
- `ORDINAL`은 아래 리스크가 크다.
  - Enum 상수의 **순서가 바뀌거나 중간에 추가**되면 기존 데이터 의미가 뒤틀림
  - 장애로 이어질 수 있음
- 그래서 일반적으로는 **`EnumType.STRING`이 더 안전**하다.

> 단, STRING 방식도 Enum 상수명을 바꾸면 기존 데이터와 불일치가 발생할 수 있으니  
> “상수명 변경은 곧 데이터 마이그레이션 이슈”라는 점은 인지해야 한다.


## 4) 보충: 더 안정적으로 가려면(선택)
- Enum 이름 자체를 저장하기보다,
  Enum 내부에 **고정 코드값(code)** 을 두고 DB에는 그 코드를 저장하는 방식이 가장 견고하다.
  - 예: `STANDARD`의 표시/저장 코드는 `"STD"` 처럼 고정
- 이때는 `@Enumerated` 대신 `@Converter(AttributeConverter)`로 매핑하는 패턴을 많이 쓴다.
- 특히 `YnType`처럼 DB가 `'y'/'n'` 등 **레거시 문자값**을 쓰는 경우,
  Converter가 실무적으로 더 자연스럽고 안전하다.


## 5) 한 줄 결론
- **JPQL 문자열에 Enum 상수를 직접 쓰면(FQN 필요)**
- **가능하면 Enum은 파라미터로 받아 비교하고(FQN 불필요, 권장)**
- **DB 저장은 기본적으로 `EnumType.STRING` 또는 Converter 기반 “고정 코드”를 추천**


## 6) 추가 정리
```java
ORDER BY
  case
      when r.orderType = com.bada.cali.common.enums.OrderType.ACCREDDIT then 0
      when r.orderType = com.bada.cali.common.enums.OrderType.UNACCREDDIT then 1
      when r.orderType = com.bada.cali.common.enums.OrderType.TESTING then 2
          else 9
  end asc,
r.id asc
```
- 위와 같은 경우, 패키지명을 쓸 수밖에 없는 상황이지만, 리파지토리 계층으로로 보낼 때, 서비스 계층에서 각 Enum에 해당하는 값을 0, 1, 2 형태의 int타입의 파라미터로 보내는 방법도 있다.
```java
int orderTypeOrder = OrderType.ACCREDDIT ? 0 : 1; // 등의 형태로 orderTypeOrder만 리파지토리에 넘기는 방식

// 대충 아래와 같은 형태로 가능하다.
ORDER BY
  case
      when r.orderType = :orderTypeOrder
 else 9
 end asc  
```

<br><br>

---

# 📌 52번. JPA/JPQL에서 Entity가 아닌 DTO/Record/Interface로 “바로 받는 방법” 정리

## 1) 큰 그림: “엔티티 조회” vs “프로젝션(Projection) 조회”
### A. 엔티티로 조회
- JPA의 기본 동작: `select e from Entity e` 형태로 **엔티티를 영속성 컨텍스트에 로딩**
- 이후 서비스 계층에서 DTO로 변환(MapStruct 등)할 수 있음
- 이 경우에는 JPQL이 DTO 생성자를 호출할 필요가 없고,
  **변환은 애플리케이션 코드에서 수행**됨

### B. 엔티티가 아닌 형태로 조회(Projection)
- DB 결과를 엔티티가 아니라
  - 인터페이스
  - record
  - DTO 클래스
  같은 형태로 바로 받는 방식
- 이때는 **JPQL/쿼리 단계에서 결과를 “어떤 형태로 만들지” 결정**해야 하므로,
  표준적으로는 “생성자 호출” 또는 “프로젝션 규칙”을 따른다


## 2) Projection 방식의 종류(핵심 3가지)

## (1) Interface-based Projection (튜플/별칭 기반)
- select 절에서 필요한 필드들을 **명시적으로 나열**하고,
  각 필드에 **별칭(alias)** 을 맞춘 뒤,
  결과를 인터페이스의 `getXxx()` 메서드로 매핑하는 방식
- 특징
  - “튜플 형태(필드 묶음)”로 받아오는 느낌
  - DTO 생성자 호출이 아니라 **getter 매핑 규칙**으로 바인딩됨
- 장점
  - 간단하고 빠르게 작성 가능
  - 필요한 필드만 가져오므로 성능에 유리할 수 있음
- 주의
  - 별칭과 getter 이름 매칭이 중요
  - 복잡한 변환 로직은 한계가 있음


## (2) DTO/Record로 바로 받기: Constructor Expression (표준 JPA 방식)
- JPQL에서 `new`를 사용해 **DTO 생성자를 직접 호출**하는 방식
- 핵심 문장:
  - **“JPQL에서 엔티티가 아닌 DTO/record를 바로 받으려면 표준 JPA 메커니즘은 constructor expression이다.”**
- 특징
  - select 결과를 DTO 생성자 파라미터 순서대로 매핑
  - record도 “생성자(=컴팩트 생성자)”가 있으므로 동일한 원리로 동작 가능
- 장점
  - 원하는 DTO 형태로 “즉시” 반환 → 서비스 변환 코드 감소
  - 불필요한 엔티티 로딩을 피할 수 있음(필드만 select)
- 주의(실무에서 자주 터지는 포인트)
  - **파라미터 개수/순서/타입이 DTO 생성자와 정확히 일치**해야 함
  - JPQL 문자열에 FQN(패키지 포함 DTO 클래스명) 필요
  - join/alias/enum 타입 변환 등에서 타입 미스매치가 발생하면 런타임 오류로 이어짐


## (3) 엔티티로 받은 뒤 MapStruct 등으로 변환
- 조회는 엔티티로 하고, 반환은 DTO로 바꾸는 방식
- 장점
  - JPQL이 단순해짐(특히 복잡한 조인/조건식이 있을 때)
  - 매핑 로직을 MapStruct로 표준화 가능(유지보수/테스트에 유리)
- 단점/주의
  - 엔티티 로딩 범위가 커지면 성능/지연로딩 이슈가 생길 수 있음
  - “필요한 필드만 가져오기”가 어려워질 수 있음(튜닝 필요)


## 3) 질문 내용의 요지(정리)
- **엔티티가 아닌 결과 타입(인터페이스/record/DTO)로 직접 받으려면**
  - (표준) **constructor expression(`new ...`)** 또는
  - (스프링 데이터 패턴) **interface projection(getter 매핑)** 이 필요하다
- 반대로 **엔티티로 조회하고 DTO로 변환**하는 방식은
  - JPQL에서 DTO 생성자를 호출할 필요가 없고
  - 서비스 계층에서 매핑(MapStruct 등)으로 해결한다


## 4) 실무 선택 가이드(가볍게)
- 리스트 화면/그리드처럼 “필드 일부만 필요” + “대량 조회” → **Projection/Constructor Expression 선호**
- 도메인 로직/연관관계 활용/수정까지 이어질 가능성 → **엔티티 조회 후 DTO 변환 선호**
- 복잡한 조회인데도 DTO로 바로 받고 싶다 → **Querydsl/Projection 조합 고려**

<br><br>

---
