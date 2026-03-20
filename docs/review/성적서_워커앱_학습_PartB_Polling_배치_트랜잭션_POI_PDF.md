# 성적서 워커 애플리케이션 구현 전 실전 학습 문서
## Part B — Polling/SSE / 배치·상태머신 / 트랜잭션·동시성 / 풀링 / Apache POI / PDF 변환 (7~12장)

---

# 7장. Polling, SSE, 비동기 작업 추적

## 7-1. Polling이란 무엇인가

### 개념 정의

Polling은 클라이언트가 **일정 주기마다 서버에 상태를 물어보는** 방식이다.

```
브라우저                    CALI
   │                         │
   │── GET /batches/101 ───▶ │
   │◀── {status: PROGRESS} ──│
   │ (2초 대기)
   │── GET /batches/101 ───▶ │
   │◀── {status: PROGRESS} ──│
   │ (2초 대기)
   │── GET /batches/101 ───▶ │
   │◀── {status: SUCCESS} ───│
   │ (Polling 중지)
```

### 이번 프로젝트에서의 Polling

```javascript
// reportWrite.js (프론트엔드)
async function startPolling(batchId) {
    const interval = setInterval(async () => {
        const res = await fetch(`/api/report/jobs/batches/${batchId}`);
        const data = await res.json();

        updateProgressUI(data.data); // 진행상황 UI 업데이트

        // 완료 조건 체크
        const { status } = data.data;
        if (status === 'SUCCESS' || status === 'FAIL') {
            clearInterval(interval); // Polling 중지
            handleComplete(data.data);
        }
    }, 2000); // 2초 간격
}
```

---

## 7-2. 왜 초기 구현은 Polling이 좋은가

| 이유 | 설명 |
|---|---|
| 구현 단순 | 프론트: setInterval + fetch. 백엔드: GET 엔드포인트 하나 |
| 디버깅 용이 | 매 요청의 응답을 개발자 도구에서 확인 가능 |
| 서버 무상태 | 연결 유지 불필요 → 서버 재시작해도 Polling 재개 가능 |
| 실패 내성 | 일시적 네트워크 오류 시 다음 Polling에서 자동 복구 |
| 부하 예측 가능 | 요청 간격을 조절하여 서버 부하 제어 가능 |

### Polling의 단점

- **낭비**: 변경이 없어도 주기마다 요청 발생
- **지연**: 최대 1 interval 간격의 지연 발생 (2초 간격이면 최대 2초 지연)
- **N:1 부하**: 사용자가 많으면 서버에 요청이 N배 증가

### 이번 프로젝트에서 Polling 간격 권장값

```javascript
// 처리 시간 예상치에 따라 조정
// 성적서 1건당 약 5~30초 예상 시
const POLLING_INTERVAL = 2000; // 2초 (적절한 균형)

// 장시간 처리 예상 시
// 처음엔 짧게, 점점 길게 (adaptive polling)
let interval = 2000;
const maxInterval = 10000;
// ...
interval = Math.min(interval * 1.5, maxInterval);
```

---

## 7-3. SSE는 언제 고려하는가

### SSE(Server-Sent Events) 개념

서버가 클라이언트에게 **단방향으로 이벤트를 push**하는 방식.
HTTP를 유지한 채 서버가 데이터를 보내는 구조다.

```
브라우저                    CALI
   │                         │
   │── GET /batches/101/events ─▶ │
   │                         │ ← HTTP 연결 유지
   │◀── data: {step: FILLING} ─── │
   │◀── data: {step: UPLOADING} ─ │
   │◀── data: {status: SUCCESS} ─ │
   │   (연결 종료)
```

### Polling vs SSE 비교

| 항목 | Polling | SSE |
|---|---|---|
| 구현 복잡도 | 낮음 | 중간 |
| 실시간성 | 낮음 (interval 지연) | 높음 (이벤트 즉시 수신) |
| 서버 자원 | 요청마다 연결 생성/해제 | 연결 유지 (자원 점유) |
| 프록시/방화벽 | 문제 없음 | 일부 프록시에서 문제 |
| 재연결 | 수동 구현 필요 | 브라우저 자동 재연결 |
| 다방향 통신 | 불필요 시 적합 | 서버→클라이언트 단방향만 |
| 이번 프로젝트 적합성 | ✅ 초기 구현 | 🔄 추후 고려 |

### Spring에서 SSE 구현 방법

```java
// Spring SseEmitter 사용
@GetMapping(value = "/batches/{batchId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamBatchEvents(@PathVariable Long batchId) {
    SseEmitter emitter = new SseEmitter(180_000L); // 3분 타임아웃
    // emitter를 맵에 저장하고 콜백 수신 시 event 전송
    return emitter;
}
```

SSE는 연결이 끊기면 emitter 관리가 필요하고,
부하 분산(로드밸런서) 환경에서는 sticky session이나 Redis Pub/Sub 필요.
**지금 단계에서는 Polling으로 충분**하다.

---

## 7-4. 진행률 계산 방식

### batch 전체 진행률

```java
// BatchStatusRes에서 계산
int total = batch.getTotalCount();
int done = batch.getSuccessCount() + batch.getFailCount();
double progress = total > 0 ? (double) done / total * 100 : 0;
// 예: 20건 중 15건 처리 → 75%
```

### UI에서 표시

```javascript
function updateProgressUI(batchData) {
    const { totalCount, successCount, failCount, status } = batchData;
    const done = successCount + failCount;
    const pct = totalCount > 0 ? Math.round(done / totalCount * 100) : 0;

    // 진행률 바 업데이트
    document.getElementById('progressBar').style.width = pct + '%';
    document.getElementById('progressText').textContent =
        `${done} / ${totalCount} (성공: ${successCount}, 실패: ${failCount})`;

    // item별 상태 목록
    batchData.items.forEach(item => {
        updateItemRow(item.reportId, item.status, item.step);
    });
}
```

---

# 8장. 배치 처리와 상태 머신

## 8-1. batch / item / step 개념

### 계층 구조

```
배치 (batch) — 사용자 1회 액션의 단위
  └── 아이템 (item) — 개별 성적서 처리 단위
        └── 스텝 (step) — item 처리 내부 단계 상세
```

### 각 레이어의 책임

| 레이어 | 추적 대상 | 상태 | 위치 |
|---|---|---|---|
| batch | 전체 작업 묶음 | READY/PROGRESS/SUCCESS/FAIL | report_job_batch.status |
| item | 개별 성적서 | READY/PROGRESS/SUCCESS/FAIL | report_job_item.status |
| step | 현재 처리 단계 | 문자열 열거값 | report_job_item.step |

---

## 8-2. 상태 전이 설계

### batch 상태 전이

```
[READY]
  ↓ (cali-worker가 처음 PROGRESS 콜백 수신 시)
[PROGRESS]
  ↓ (모든 item 완료 — success + fail >= total)
[SUCCESS]  → 전부 성공
[FAIL]     → 1건 이상 실패 (또는 트리거 실패)
```

### item 상태 전이

```
[READY]
  ↓ (cali-worker가 처리 시작)
[PROGRESS]
  ├── (처리 성공)
  │     ↓
  │   [SUCCESS]
  └── (처리 실패)
        ↓
      [FAIL]

트리거 실패 시:
[READY] → [CANCELED] (일괄 전환)
```

### step 전이 (WRITE 타입 예시)

```
DOWNLOADING_TEMPLATE
  ↓
FILLING_DATA
  ↓
UPLOADING_ORIGIN
  ↓
DONE
```

step은 item.step에 문자열로 저장된다.
상태(PROGRESS/SUCCESS/FAIL)는 따로 있고,
step은 "지금 어느 단계인지"를 더 세밀하게 알려주는 정보다.

---

## 8-3. 상태 전이 코드 예시

### CALI의 handleItemCallback 로직 (구현 완료)

```java
// PROGRESS 수신 시
case PROGRESS:
    // 첫 번째 PROGRESS 수신 시 batch 시작일시 기록
    if (batch.getStartDatetime() == null) {
        batch.setStartDatetime(LocalDateTime.now());
        batch.setStatus(BatchStatus.PROGRESS);
    }
    item.setStatus(JobItemStatus.PROGRESS);
    item.setStep(req.getStep());
    break;

// SUCCESS 수신 시
case SUCCESS:
    item.setStatus(JobItemStatus.SUCCESS);
    item.setEndDatetime(LocalDateTime.now());
    batch.setSuccessCount(batch.getSuccessCount() + 1);
    // report 상태 갱신
    report.setWriteStatus("SUCCESS");
    report.setWriteMemberId(batch.getRequestMemberId());
    report.setWriteDatetime(LocalDateTime.now());
    break;

// FAIL 수신 시
case FAIL:
    item.setStatus(JobItemStatus.FAIL);
    item.setMessage(req.getMessage());
    item.setEndDatetime(LocalDateTime.now());
    batch.setFailCount(batch.getFailCount() + 1);
    report.setWriteStatus("FAIL");
    break;
```

---

## 8-4. item 단위 독립 처리의 장점

### 왜 item을 각각 별도 트랜잭션으로 처리해야 하는가

성적서 5건을 일괄 처리한다고 가정하자.

**단일 트랜잭션으로 처리 시**:
```
처리: report_id=501 ✅ → 502 ✅ → 503 ❌ → 트랜잭션 전체 롤백
결과: 501, 502도 없던 일이 됨
```

**item별 독립 트랜잭션**:
```
처리: report_id=501 (트랜잭션1) ✅
      report_id=502 (트랜잭션2) ✅
      report_id=503 (트랜잭션3) ❌ → 503만 FAIL
결과: 501, 502는 SUCCESS 상태로 유지됨
```

### cali-worker에서의 구현 원칙

```java
// 각 item을 독립적으로 처리
for (ItemInfo item : items) {
    try {
        processItem(item, triggerReq); // 개별 처리
        callbackSuccess(item.getItemId(), step);
    } catch (Exception e) {
        log.error("[batch={}][item={}] 처리 실패", batchId, item.getItemId(), e);
        callbackFail(item.getItemId(), e.getMessage());
        // 다음 item으로 계속 (전체 중단 안 함)
    }
}
```

---

## 8-5. 부분 실패 처리

### batch 최종 상태 결정 로직

```java
// 모든 item 완료 시 batch 상태 결정
if (batch.getSuccessCount() + batch.getFailCount() >= batch.getTotalCount()) {
    batch.setEndDatetime(LocalDateTime.now());
    if (batch.getFailCount() > 0) {
        batch.setStatus(BatchStatus.FAIL);  // 1건이라도 실패
    } else {
        batch.setStatus(BatchStatus.SUCCESS); // 전부 성공
    }
}
```

### UI에서의 부분 실패 표시

```
배치 상태: FAIL (전체 5건 중 1건 실패)
┌────────┬────────────┬──────────┐
│ 성적서  │ 상태       │ 단계     │
├────────┼────────────┼──────────┤
│ 501    │ ✅ SUCCESS │ DONE     │
│ 502    │ ✅ SUCCESS │ DONE     │
│ 503    │ ❌ FAIL    │ FILLING  │ ← 실패 사유도 표시
│ 504    │ ✅ SUCCESS │ DONE     │
│ 505    │ ✅ SUCCESS │ DONE     │
└────────┴────────────┴──────────┘
```

---

## 8-6. 취소 전략

### 현재 구현 기준 취소 시나리오

CALI 트리거가 cali-worker에 도달하지 못했을 때:

```java
// triggerWorkerServer에서 실패 처리
try {
    // POST /api/jobs/execute 호출
} catch (Exception e) {
    // 트리거 실패 → 복원
    batch.setStatus(BatchStatus.FAIL);
    items.forEach(item -> item.setStatus(JobItemStatus.CANCELED));
    reports.forEach(r -> r.setWriteStatus("IDLE"));
}
```

### 사용자가 취소를 요청하는 경우 (미구현)

```
취소 요청 흐름 (설계 참고):
1. 사용자 취소 버튼 클릭
2. CALI → batch.status = CANCEL_REQUESTED
3. cali-worker가 배치 조회 시 CANCEL_REQUESTED 확인
4. 현재 item 완료 후 나머지 CANCELED 처리
5. batch.status = CANCELED
```

---

## 8-7. Crash Recovery 개념

### 문제: cali-worker가 처리 중 갑자기 죽으면?

```
item=PROGRESS 상태로 DB에 남음
cali-worker 재시작됨
→ 해당 item에 대한 후속 콜백이 없음
→ batch가 PROGRESS 상태로 고착됨
```

### 해결 방안 1: Timeout 기반 자동 FAIL

CALI에 스케줄러를 두어 일정 시간 이상 PROGRESS인 item을 FAIL 처리:

```java
@Scheduled(fixedDelay = 5 * 60 * 1000) // 5분마다
public void failStaleItems() {
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(30); // 30분 이상 PROGRESS
    List<ReportJobItem> staleItems = itemRepository.findStaleProgressItems(threshold);
    staleItems.forEach(item -> {
        item.setStatus(JobItemStatus.FAIL);
        item.setMessage("타임아웃: 작업서버 응답 없음");
    });
}
```

### 해결 방안 2: cali-worker 재시작 시 클린업

cali-worker 시작 시 PROGRESS 상태의 item들을 FAIL 처리 후 재처리:

```java
@EventListener(ApplicationReadyEvent.class)
public void onStartup() {
    // 이전 실행에서 PROGRESS 상태로 남은 item들을 FAIL 처리
    // → 이후 CALI가 재트리거하거나 사용자가 재시도
}
```

---

# 9장. 트랜잭션과 동시성

## 9-1. 왜 item 단위 독립 트랜잭션이 필요한가

8장에서 설명했듯 item 처리 실패가 전체 롤백으로 이어지면 안 된다.

### cali-worker에서 트랜잭션 경계

cali-worker는 CALI API를 호출하는 HTTP 클라이언트 역할도 한다.
트랜잭션은 cali-worker 자체 DB가 있을 때 의미있고,
CALI에 콜백만 보내는 경우엔 CALI의 handleItemCallback이 트랜잭션 단위다.

```java
// CALI: handleItemCallback
@Transactional  // item 1건 콜백 처리 = 트랜잭션 1건
public void handleItemCallback(Long itemId, ItemCallbackReq req) {
    // item 상태 변경
    // batch 카운트 변경
    // report 상태 변경
    // → 이 3개가 하나의 트랜잭션으로 묶임
    // → 실패 시 이 item에 대한 상태만 롤백
}
```

---

## 9-2. batch 전체 롤백이 왜 위험한가

### 시나리오

```
batch (20건) 처리 중 item 19번째에서 DB 저장 실패
→ 트랜잭션 롤백
→ 18건의 성공 기록도 모두 사라짐
→ 18건의 report.write_status도 IDLE로 돌아감
→ 사용자는 처음부터 다시 해야 함
```

### 올바른 구조

```
batch 생성: 트랜잭션 A (batch + items INSERT)
  ↓
트리거: 별도 트랜잭션 B (독립)
  ↓
item 1 콜백: 트랜잭션 C (item1 상태 + report1 상태)
item 2 콜백: 트랜잭션 D (item2 상태 + report2 상태)
...
item N 콜백: 트랜잭션 N+C
  ↓
batch 완료: 각 콜백 트랜잭션 안에서 완료 체크
```

이 구조에서 트랜잭션 D가 실패해도 C, E, F는 영향받지 않는다.

---

## 9-3. Race Condition

### 개념

여러 스레드나 프로세스가 동시에 같은 데이터를 수정하려 할 때 발생하는 문제.

### 이번 프로젝트에서의 Race Condition 위험

**상황**: cali-worker가 여러 스레드로 item을 병렬 처리한다면?
→ 같은 batch의 `success_count`를 동시에 업데이트하면 카운트 손실 가능.

```
스레드 A: success_count = 5 읽음
스레드 B: success_count = 5 읽음
스레드 A: success_count = 6 저장
스레드 B: success_count = 6 저장  ← 7이 되어야 하는데 6으로 저장됨
```

### 해결 방법 1: 순차 처리 (현재 구현 적합)

cali-worker가 item을 순차적으로 처리하면 race condition 없음.
단일 워커, 순차 처리라면 이 방법이 가장 단순하고 안전하다.

### 해결 방법 2: 낙관적 잠금 (Optimistic Locking)

```java
// Entity에 @Version 추가
@Entity
public class ReportJobBatch {
    @Version
    private Long version; // JPA가 자동으로 버전 관리
}

// 동시 수정 시 ObjectOptimisticLockingFailureException 발생
// → catch 후 재시도
```

### 해결 방법 3: 비관적 잠금 (Pessimistic Locking)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM ReportJobBatch b WHERE b.id = :id")
Optional<ReportJobBatch> findByIdWithLock(@Param("id") Long id);
```

- SELECT FOR UPDATE와 동일
- 동시성이 높을 때 데드락 위험

### 해결 방법 4: DB 원자적 업데이트

```sql
-- success_count를 읽지 않고 직접 증가
UPDATE report_job_batch
SET success_count = success_count + 1
WHERE id = ?;
```

**이번 프로젝트 권장**: 단일 워커, 순차 처리 → race condition 실제 발생 가능성 낮음.
현재 구현이 JPA 엔티티 수정 방식이라면 단일 스레드 처리를 유지하는 것이 안전.

---

## 9-4. 중복 실행 방지

### 같은 batch에 대해 트리거가 2번 발송되면?

```
상황:
CALI가 네트워크 오류로 트리거 응답을 못 받음
→ 재시도 로직으로 트리거를 2번 발송
→ cali-worker가 같은 batchId로 작업을 2번 시작
```

### 방어 코드

```java
// cali-worker의 트리거 수신 핸들러
@PostMapping("/api/jobs/execute")
public ResponseEntity<?> execute(@RequestBody WorkerTriggerReq req) {
    // CALI에서 배치 상태 조회
    BatchStatusRes batch = fetchBatchStatus(req.getBatchId());

    // 이미 처리 중이거나 완료된 배치는 스킵
    if (batch.getStatus() != BatchStatus.READY) {
        log.warn("이미 처리 중이거나 완료된 배치: batchId={}, status={}",
                 req.getBatchId(), batch.getStatus());
        return ResponseEntity.ok("이미 처리 중");
    }
    // 처리 시작
    startProcessing(req);
    return ResponseEntity.accepted().build();
}
```

---

## 9-5. 상태 전이 가드

### 잘못된 상태 전이 방지

```java
// handleItemCallback에서 이미 완료된 item 중복 처리 방지
public void handleItemCallback(Long itemId, ItemCallbackReq req) {
    ReportJobItem item = itemRepository.findById(itemId)
        .orElseThrow(() -> new EntityNotFoundException("item 없음"));

    // 이미 최종 상태(SUCCESS/FAIL)인 item은 처리 안 함
    if (item.getStatus() == JobItemStatus.SUCCESS
            || item.getStatus() == JobItemStatus.FAIL) {
        log.warn("이미 완료된 item에 콜백 수신 무시: itemId={}, status={}",
                 itemId, item.getStatus());
        return;
    }
    // ... 상태 변경
}
```

---

# 10장. 풀링(Pooling)의 이해

## 10-1. 풀링이란 무엇인가

### 개념

풀(Pool)은 **미리 만들어 놓은 자원의 집합**이다.
자원을 사용할 때마다 새로 생성하고 사용 후 버리는 대신,
풀에서 빌려 쓰고 반납하는 방식으로 효율을 높인다.

```
자원 생성 비용이 크다 → 풀에서 빌려 쓰고 반납

풀 없이:
요청 → [DB 연결 생성(느림)] → 쿼리 → [연결 닫기] → 완료

풀 있이:
요청 → [풀에서 기존 연결 대여(빠름)] → 쿼리 → [풀에 반납] → 완료
```

---

## 10-2. DB Connection Pool — HikariCP

### HikariCP란

Spring Boot가 기본으로 사용하는 DB 연결 풀 라이브러리.
Java DB 연결 풀 중 가장 빠르고 널리 사용됨.

### 동작 원리

```
애플리케이션 시작 시:
  DB 연결 N개를 미리 생성하여 풀에 보관

요청 발생 시:
  풀에서 유휴 연결 1개 대여 → 쿼리 실행 → 연결 반납

풀이 가득 찼을 때:
  대기 큐에서 기다림 (connectionTimeout 초과 시 예외)
```

### 주요 설정값

```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=10       # 최대 연결 수
spring.datasource.hikari.minimum-idle=5             # 최소 유지 연결 수
spring.datasource.hikari.connection-timeout=30000   # 연결 대기 타임아웃 (ms)
spring.datasource.hikari.idle-timeout=600000        # 유휴 연결 유지 시간 (ms)
spring.datasource.hikari.max-lifetime=1800000       # 연결 최대 수명 (ms, 30분)
```

### 이번 프로젝트에서의 풀 크기 권장

```
CALI (메인 앱):
  - 동시 요청 수에 맞게 설정
  - 기본 10개로 시작, 모니터링 후 조정

cali-worker:
  - 순차 처리 기준으로 최소한의 연결로 충분
  - maximum-pool-size=5 정도로 시작
```

---

## 10-3. 풀 크기를 잘못 잡으면 생기는 문제

### 너무 작게 설정 시

```
상황: maximum-pool-size=2, 동시 요청 5개
흐름:
  요청1 → 연결1 사용
  요청2 → 연결2 사용
  요청3, 4, 5 → 대기 (connectionTimeout 초과 시 예외)

증상: HikariPool-1 - Connection is not available, request timed out after 30000ms
```

### 너무 크게 설정 시

```
상황: maximum-pool-size=100, DB 서버가 처리 가능한 연결 = 50
흐름:
  100개 연결 생성 시도 → DB 서버가 "too many connections" 반환
  이후 모든 요청 실패

또한: 사용 안 하는 연결도 메모리와 DB 서버 자원을 점유
```

### 황금률: CPU 코어 수 기반 계산

HikariCP 공식 권장:
```
pool size = (CPU 코어 수 × 2) + 1
예: 4코어 CPU → 9개
```

단, 이 공식은 CPU 바운드 작업 기준이다.
I/O 바운드(DB 대기 시간이 긴) 작업은 더 많이 설정할 수 있다.

---

## 10-4. Thread Pool

### Spring Boot의 기본 Thread Pool (Tomcat)

```
HTTP 요청이 들어오면 Tomcat이 스레드를 할당
동시 요청 수 = 동시 처리 가능한 스레드 수
```

```properties
# application.properties
server.tomcat.threads.max=200           # 최대 스레드 수 (기본 200)
server.tomcat.threads.min-spare=10      # 최소 유지 스레드 수
server.tomcat.accept-count=100          # 큐에 대기 가능한 요청 수
```

### cali-worker에서의 Thread Pool

cali-worker가 item을 병렬 처리하려면 ExecutorService 필요:

```java
// 병렬 처리 시 (선택적 구현)
ExecutorService executor = Executors.newFixedThreadPool(4); // 4개 스레드

for (ItemInfo item : items) {
    executor.submit(() -> {
        try {
            processItem(item);
        } catch (Exception e) {
            log.error("item 처리 실패", e);
        }
    });
}
executor.shutdown();
executor.awaitTermination(1, TimeUnit.HOURS);
```

**이번 초기 구현**: 순차 처리 권장 (동시성 문제 없음, 구현 단순)

---

# 11장. Apache POI와 Excel 처리

## 11-1. XSSF vs SXSSF

### Apache POI 모델 계층

```
Apache POI 엑셀 API
  ├── HSSF (Horrible SpreadSheet Format) → .xls (구형, Excel 97-2003)
  └── XSSF (XML SpreadSheet Format)      → .xlsx (현재 표준)
        └── SXSSF (Streaming XSSF)       → .xlsx (대용량, 스트리밍)
```

### XSSF vs SXSSF 비교

| 항목 | XSSF | SXSSF |
|---|---|---|
| 메모리 방식 | 전체 파일을 메모리에 로드 | 슬라이딩 윈도우 방식 (일부만 메모리) |
| 메모리 사용량 | 높음 (파일 크기 × ~10배) | 낮음 |
| 셀 읽기 | 가능 | 제한적 (이미 넘어간 행은 못 읽음) |
| 랜덤 접근 | 가능 | 불가 (순차 쓰기만) |
| 수식 | 완전 지원 | 제한적 |
| 셀 스타일 | 완전 지원 | 완전 지원 |
| 이미지 삽입 | 가능 | 제한적 |
| 이번 프로젝트 적합 | ✅ (적절한 파일 크기) | ⚠️ 대용량 시 고려 |

### 이번 프로젝트 기준

성적서 1건 = 엑셀 1파일 (수십 ~ 수백 행 수준)
→ 파일 크기가 크지 않으므로 **XSSF로 충분**
→ 10,000행 이상이 되면 SXSSF 고려

---

## 11-2. 템플릿 기반 Excel 생성

### 개념

빈 파일을 새로 만드는 대신, **미리 서식이 갖춰진 템플릿 파일을 열어서 데이터만 채운다**.

```java
// NCP 스토리지에서 샘플 파일 다운로드 후 처리
try (InputStream sampleStream = storageClient.download("sample/sample_template.xlsx");
     XSSFWorkbook workbook = new XSSFWorkbook(sampleStream)) {

    XSSFSheet sheet = workbook.getSheetAt(0); // 첫 번째 시트

    // 데이터 삽입
    setCellValue(sheet, 5, 2, reportData.getDeviceName()); // 6행 3열
    setCellValue(sheet, 5, 3, reportData.getDeviceNumber()); // 6행 4열

    // 완성된 파일을 출력 스트림에 저장
    try (OutputStream out = new FileOutputStream("/tmp/work/report_" + reportId + ".xlsx")) {
        workbook.write(out);
    }
}
```

### 핵심 패턴

```java
// 셀 값 설정 헬퍼
private void setCellValue(XSSFSheet sheet, int rowIdx, int colIdx, String value) {
    Row row = sheet.getRow(rowIdx);
    if (row == null) {
        row = sheet.createRow(rowIdx);
    }
    Cell cell = row.getCell(colIdx);
    if (cell == null) {
        cell = row.createCell(colIdx);
    }
    cell.setCellValue(value != null ? value : "");
}
```

---

## 11-3. JSON 기반 셀 위치 매핑 (sheet_info_setting)

### 개념

어떤 데이터를 어느 셀에 넣을지를 **코드에 하드코딩하지 않고 JSON으로 관리**한다.

```json
// env.sheet_info_setting 예시 (미확정, 참고용)
{
  "sheets": [
    {
      "sheetIndex": 0,
      "fields": [
        {"field": "deviceName",   "row": 5, "col": 2},
        {"field": "deviceNumber", "row": 5, "col": 3},
        {"field": "calibDate",    "row": 6, "col": 2},
        {"field": "calibResult",  "row": 7, "col": 2}
      ]
    }
  ]
}
```

### 이점

- 셀 위치가 바뀌어도 **코드 변경 없이 JSON만 수정**
- 업체/소분류별로 다른 템플릿에 다른 매핑 적용 가능
- 비개발자도 JSON 수정으로 셀 위치 조정 가능

### 구현 패턴

```java
@Data
public class SheetInfoSetting {
    private List<SheetConfig> sheets;

    @Data
    public static class SheetConfig {
        private int sheetIndex;
        private List<FieldMapping> fields;
    }

    @Data
    public static class FieldMapping {
        private String field;  // 데이터 필드명
        private int row;       // 0-based 행 인덱스
        private int col;       // 0-based 열 인덱스
    }
}

// 적용
ObjectMapper mapper = new ObjectMapper();
SheetInfoSetting setting = mapper.readValue(envInfo.getSheetInfoSetting(), SheetInfoSetting.class);

Map<String, String> reportData = getReportDataMap(reportId); // 필드명→값 맵

for (SheetConfig sheetConfig : setting.getSheets()) {
    XSSFSheet sheet = workbook.getSheetAt(sheetConfig.getSheetIndex());
    for (FieldMapping mapping : sheetConfig.getFields()) {
        String value = reportData.get(mapping.getField());
        setCellValue(sheet, mapping.getRow(), mapping.getCol(), value);
    }
}
```

---

## 11-4. 스타일/수식/병합셀/이미지 처리

### 셀 스타일 복사

템플릿에 이미 적용된 스타일을 유지하면서 값만 바꾸려면:

```java
// 기존 셀의 스타일을 유지하면서 값만 변경
Cell cell = sheet.getRow(rowIdx).getCell(colIdx);
// cell.getCellStyle()은 그대로 → 스타일 변경 없이 값만 설정
cell.setCellValue(newValue);
```

### 수식 셀 처리

```java
// 수식 셀 강제 재계산
workbook.setForceFormulaRecalculation(true);

// 특정 셀에 수식 설정
cell.setCellFormula("SUM(B1:B10)");
```

### 병합 셀 확인

```java
// 셀이 병합되었는지 확인
for (CellRangeAddress range : sheet.getMergedRegions()) {
    if (range.isInRange(rowIdx, colIdx)) {
        // 병합 영역의 첫 셀에만 값 설정해야 함
        row = sheet.getRow(range.getFirstRow());
        cell = row.getCell(range.getFirstColumn());
        break;
    }
}
```

### 이미지 삽입 (서명 이미지)

```java
// 이미지 파일을 바이트로 읽기
byte[] imageBytes = FileUtils.readFileToByteArray(signatureFile);

// 워크북에 이미지 추가
int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);

// 앵커 설정 (이미지 위치)
Drawing<?> drawing = sheet.createDrawingPatriarch();
ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
anchor.setCol1(colStart); anchor.setRow1(rowStart);
anchor.setCol2(colEnd);   anchor.setRow2(rowEnd);

// 이미지 삽입
Picture picture = drawing.createPicture(anchor, pictureIdx);
picture.resize(); // 원본 크기로 리사이즈 (선택)
```

---

## 11-5. 대용량 파일과 메모리 문제

### 왜 메모리 문제가 생기는가

XSSF는 파일 전체를 메모리에 올린다.
50MB 엑셀 파일이라면 JVM 힙에 500MB+ 필요할 수 있다.

### 메모리 사용량 모니터링

```bash
# JVM 힙 사용량 실시간 확인
jstat -gc <pid> 1s

# GC 로그 활성화
java -jar cali-worker.jar -Xlog:gc:file=/opt/cali-worker/gc.log:time
```

### 메모리 이슈 대응

```properties
# JVM 힙 설정
# -Xmx: 최대 힙 크기
# -Xms: 초기 힙 크기
# ExecStart에 추가:
# java -Xmx512m -Xms256m -jar cali-worker.jar
```

### 파일 처리 후 반드시 닫기

```java
// try-with-resources로 자동 닫기 (필수)
try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
    // 처리
    try (OutputStream out = new FileOutputStream(outputPath)) {
        workbook.write(out);
    }
} // workbook.close() 자동 호출
```

XSSFWorkbook을 닫지 않으면 임시 파일(`poi-xxx.tmp`)이 남고 메모리 누수 발생.

---

## 11-6. temp file 관리

### Apache POI의 임시 파일

대용량 엑셀을 처리할 때 POI가 내부적으로 `/tmp/poi-xxx.tmp` 파일을 생성한다.

```java
// 시스템 종료 시 임시 파일 정리 설정
System.setProperty("poi.poi.temp.dir", "/opt/cali-worker/tmp");
```

### cali-worker 작업 임시 디렉터리

```
작업 디렉터리 구조:
/opt/cali-worker/work/
  └── {UUID}/               ← 배치별 또는 item별 작업 공간
        ├── sample.xlsx      ← 다운로드한 샘플 파일
        ├── report_501.xlsx  ← 완성된 성적서
        └── ...

성공 시: 디렉터리 전체 삭제
실패 시: 일정 기간(예: 24시간) 보존 후 삭제 (디버깅용)
```

```java
// 임시 디렉터리 생성
Path workDir = Paths.get(appWorkDir, UUID.randomUUID().toString());
Files.createDirectories(workDir);

// 처리 완료 후 정리
try {
    processAllItems(workDir);
    deleteDirectory(workDir); // 성공 시 즉시 삭제
} catch (Exception e) {
    log.warn("작업 실패, 임시 파일 보존: {}", workDir);
    // 실패 시 보존 (나중에 스케줄러가 정리)
}
```

---

## 11-7. 파일 깨짐 방지 포인트

### 1. OutputStream을 완전히 닫은 후 이동

```java
// 잘못된 패턴
workbook.write(outputStream); // 아직 flush 안 됨
Files.move(tempFile, finalFile); // 파일이 불완전한 상태에서 이동

// 올바른 패턴
try (OutputStream out = new FileOutputStream(tempFile)) {
    workbook.write(out);
    out.flush(); // 명시적 flush
} // 여기서 close 완료 후
Files.move(tempFile, finalFile, StandardCopyOption.ATOMIC_MOVE); // 이동
```

### 2. 인코딩 주의

```java
// 셀에 한글 입력 시
cell.setCellValue("교정성적서"); // 문제없음 (Java String은 UTF-16)
// XSSF 자체가 UTF-8 xlsx 파일 생성
```

### 3. 스타일 개수 제한

```java
// 나쁜 패턴: 셀마다 새 스타일 생성 (64000개 제한)
CellStyle style = workbook.createCellStyle(); // 반복마다 새 스타일 생성 → 위험

// 좋은 패턴: 스타일 재사용
Map<String, CellStyle> styleCache = new HashMap<>();
CellStyle getOrCreateStyle(Workbook wb, String key) {
    return styleCache.computeIfAbsent(key, k -> {
        CellStyle style = wb.createCellStyle();
        // 설정...
        return style;
    });
}
```

### 4. 행 인덱스 확인

```java
// POI는 0-based 인덱스 사용
// 엑셀 1행 = POI row index 0
// 엑셀 3열(C) = POI col index 2
```

---

# 12장. PDF 변환과 문서 후처리

## 12-1. LibreOffice Headless 개념

### 왜 LibreOffice를 사용하는가

Java에서 엑셀(.xlsx)을 PDF로 변환하는 방법:

| 방법 | 장점 | 단점 |
|---|---|---|
| LibreOffice headless | 변환 품질 높음, 무료 | 별도 설치 필요, 프로세스 실행 |
| Apache POI + iText | Java 전용, 별도 설치 불필요 | 복잡한 레이아웃 재현 어려움 |
| Aspose | 높은 품질 | 유료 라이선스 |
| docx4j | Java 네이티브 | 엑셀 → PDF 제한적 |

**이번 프로젝트 선택**: LibreOffice headless (미확정이지만 현실적 선택)

### LibreOffice Headless 설치 (Linux)

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install libreoffice

# 버전 확인
libreoffice --version
```

---

## 12-2. xlsx → PDF 변환 실행

### 변환 명령어

```bash
# 기본 변환
libreoffice --headless --convert-to pdf input.xlsx --outdir /output/

# 실행 예:
libreoffice --headless --convert-to pdf /opt/cali-worker/work/report_501.xlsx \
  --outdir /opt/cali-worker/work/
```

### Java에서 프로세스 실행

```java
public void convertToPdf(Path xlsxPath, Path outputDir) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(
        "libreoffice",
        "--headless",
        "--convert-to", "pdf",
        xlsxPath.toAbsolutePath().toString(),
        "--outdir", outputDir.toAbsolutePath().toString()
    );
    pb.redirectErrorStream(true);

    Process process = pb.start();

    // 출력 로그 캡처
    String output;
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {
        output = reader.lines().collect(Collectors.joining("\n"));
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
        throw new IOException("PDF 변환 실패: " + output);
    }
    log.info("PDF 변환 완료: {}", output);
}
```

### 타임아웃 처리

```java
boolean finished = process.waitFor(60, TimeUnit.SECONDS);
if (!finished) {
    process.destroyForcibly();
    throw new IOException("PDF 변환 타임아웃 (60초 초과)");
}
```

---

## 12-3. 운영 이슈들

### 한글 폰트 문제

LibreOffice로 PDF 변환 시 한글이 깨지는 경우:

```bash
# 한글 폰트 설치
sudo apt install fonts-nanum

# 폰트 캐시 갱신
fc-cache -fv

# 설치된 한글 폰트 확인
fc-list | grep -i nanum
```

### 엑셀 레이아웃 차이

LibreOffice가 Microsoft Excel과 완전히 동일하게 렌더링하지 않을 수 있다:
- 줄 간격, 열 너비 약간 다를 수 있음
- 일부 차트는 다르게 표시될 수 있음
- **해결책**: 변환 결과를 사전에 검증하고, 문제있는 셀 서식을 단순화

### LibreOffice 동시 실행 문제

LibreOffice는 기본적으로 사용자 프로파일을 하나 사용하므로
여러 변환 작업을 동시에 실행하면 충돌이 발생할 수 있다.

```bash
# 각 실행마다 별도 프로파일 디렉터리 지정
libreoffice --headless \
  -env:UserInstallation=file:///tmp/libreoffice-profile-$$ \
  --convert-to pdf input.xlsx
```

Java에서 UUID로 프로파일 경로 생성:

```java
String profileDir = "file:///tmp/lo-profile-" + UUID.randomUUID();
ProcessBuilder pb = new ProcessBuilder(
    "libreoffice",
    "--headless",
    "-env:UserInstallation=" + profileDir,
    "--convert-to", "pdf",
    ...
);
```

---

## 12-4. 서명 이미지 삽입

### 성적서 결재 시 서명 이미지 위치

```
[실무자결재]
  origin.xlsx → 실무자 서명 이미지 삽입 → signed.xlsx

[기술책임자결재]
  signed.xlsx → 기술책임자 서명 이미지 추가 삽입 → signed.xlsx (교체)
```

### 서명 이미지 삽입 코드 (Apache POI)

```java
// 서명 이미지 파일을 스토리지에서 다운로드
byte[] signatureBytes = storageClient.download("members/" + memberId + "/signature.png");

// 워크북에 이미지 추가
int pictureIdx = workbook.addPicture(signatureBytes, Workbook.PICTURE_TYPE_PNG);

// 서명 위치 설정 (sheet_info_setting에서 읽어올 값)
Drawing<?> drawing = sheet.createDrawingPatriarch();
ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
anchor.setCol1(signatureColStart);
anchor.setRow1(signatureRowStart);
anchor.setCol2(signatureColEnd);
anchor.setRow2(signatureRowEnd);

Picture picture = drawing.createPicture(anchor, pictureIdx);
// picture.resize(); // 필요 시 크기 조정
```

---

## 12-5. QR 코드 생성

### 기술책임자결재 시 QR 코드 삽입

QR 코드는 성적서 URL이나 고유번호를 담아 위변조 방지에 사용된다.

```java
// ZXing 라이브러리 사용 (의존성 추가 필요)
// build.gradle: implementation 'com.google.zxing:core:3.x.x'

QRCodeWriter writer = new QRCodeWriter();
BitMatrix bitMatrix = writer.encode(
    qrContent,           // QR에 담을 내용 (URL 또는 성적서번호 등)
    BarcodeFormat.QR_CODE,
    200, 200             // 크기 (픽셀)
);

// BitMatrix → BufferedImage → byte[]
BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ImageIO.write(bufferedImage, "PNG", baos);
byte[] qrBytes = baos.toByteArray();

// 엑셀에 삽입 (서명 이미지와 동일한 방식)
int qrPictureIdx = workbook.addPicture(qrBytes, Workbook.PICTURE_TYPE_PNG);
// ... anchor 설정 후 삽입
```

---

## 12-6. 변환 실패 대응

### 실패 케이스와 대응

| 실패 원인 | 증상 | 대응 |
|---|---|---|
| LibreOffice 미설치 | IOException: 명령어 없음 | 설치 확인 |
| 한글 폰트 없음 | PDF에 빈 칸 또는 □ 기호 | 폰트 설치 |
| 메모리 부족 | OutOfMemoryError | JVM 힙 증가 |
| 파일 잠금 | 파일 접근 실패 | 전용 디렉터리 사용 |
| 타임아웃 | 변환이 오래 걸림 | 타임아웃 + 재시도 |

### 실패 처리 흐름

```java
try {
    convertToPdf(xlsxPath, outputDir);
    // CALI에 성공 콜백
} catch (IOException e) {
    log.error("[item={}] PDF 변환 실패: {}", itemId, e.getMessage());
    // CALI에 실패 콜백 (message에 원인 포함)
    callbackFail(itemId, "GENERATING_PDF", "PDF 변환 실패: " + e.getMessage());
}
```

---

## 12-7. 핵심 요약 (7~12장)

### Polling
- 초기 구현의 현실적 선택
- 2초 간격, 상태 완료 시 중지
- SSE는 실시간성이 필요해질 때 도입

### 배치/상태머신
- batch(묶음) + item(개별) + step(상세 단계) 3계층
- item 단위 독립 트랜잭션 → 부분 실패 허용
- 상태 전이 가드 → 중복 처리 방지

### 트랜잭션/동시성
- item 1건 = 트랜잭션 1건 (CALI의 handleItemCallback)
- 단일 워커 순차 처리 → race condition 없음
- Crash recovery: PROGRESS 고착 방지를 위한 타임아웃 FAIL 처리 필요

### 풀링
- HikariCP 기본값으로 시작, 부하 측정 후 조정
- 너무 작으면 타임아웃, 너무 크면 DB 서버 부하
- 풀 크기 ≈ CPU 코어 × 2 + 1 (기본값)

### Apache POI
- XSSF로 충분 (성적서 파일 크기 기준)
- 템플릿 기반 생성 (origin 파일 열고 데이터 채우기)
- JSON(sheet_info_setting) 기반 셀 매핑 → 유연성
- try-with-resources로 반드시 닫기
- 스타일 재사용 (64000개 제한)

### PDF 변환
- LibreOffice headless → 한글 폰트 설치 필수
- 동시 실행 시 프로파일 디렉터리 분리
- 타임아웃 처리 필수

---

*→ Part C (13~18장): 파일 저장/버전관리, 스토리지 연동, 멀티테넌시, 큐/Redis, 운영·로깅, 확장 전략*