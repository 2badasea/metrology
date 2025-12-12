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

# 📌 45번. 

<br><br>

---

