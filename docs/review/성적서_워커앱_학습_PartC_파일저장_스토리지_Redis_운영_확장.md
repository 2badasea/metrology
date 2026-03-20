# 성적서 워커 애플리케이션 구현 전 실전 학습 문서
## Part C — 파일 저장·버전관리 / 스토리지 연동 / 멀티테넌시 / Redis·큐 / 운영·로깅 / 확장 전략 (13~18장)

---

# 13장. 파일 저장과 버전 관리

## 13-1. origin / signed 파일 구조

### 이번 프로젝트의 파일 유형 체계

```
file_info.type 값 체계:
  NULL    → 기타 첨부파일 (교정신청서 첨부, 샘플 파일 등)
  origin  → 원본 성적서 (성적서작성 결과 또는 직접 업로드)
  signed  → 서명 삽입본 (실무자결재 또는 기술책임자결재 결과)
```

### 파일 생성 흐름별 정리

```
[성적서작성]
  cali-worker가 생성
  → file_info INSERT: type='origin', extension='xlsx', report_id=?, is_visible='y'
  → report.write_status = SUCCESS

[실무자결재]
  cali-worker가 생성 (origin.xlsx 기반)
  → file_info INSERT: type='signed', extension='xlsx', is_visible='y'
  → file_info INSERT: type='signed', extension='pdf',  is_visible='y'
  → report.work_status = SUCCESS

[기술책임자결재]
  cali-worker가 생성 (signed.xlsx 기반)
  → 기존 signed 파일들 UPDATE: is_visible='n' (soft delete)
  → file_info INSERT: type='signed', extension='xlsx', is_visible='y'
  → file_info INSERT: type='signed', extension='pdf',  is_visible='y'
  → report.approval_status = SUCCESS
  → report.report_status = COMPLETE
```

---

## 13-2. file_info.type 설계 의미

### 왜 type 컬럼이 필요한가

```sql
-- type 없이 최신 서명파일을 조회하려면?
SELECT * FROM file_info
WHERE report_id = 501
  AND is_visible = 'y'
  AND extension IN ('xlsx', 'pdf')
ORDER BY create_datetime DESC
LIMIT 2;
-- → 어떤 파일이 signed인지, origin인지 구분 불가
```

```sql
-- type 있으면
SELECT * FROM file_info
WHERE report_id = 501
  AND type = 'signed'
  AND is_visible = 'y';
-- → 명확하게 서명파일만 조회
```

### 조회 패턴별 예시

```sql
-- 최신 origin 파일 조회 (성적서 다운로드용)
SELECT stored_path
FROM file_info
WHERE report_id = :reportId
  AND type = 'origin'
  AND is_visible = 'y'
ORDER BY create_datetime DESC
LIMIT 1;

-- 최신 signed xlsx 파일 조회 (결재 시 기반 파일)
SELECT stored_path
FROM file_info
WHERE report_id = :reportId
  AND type = 'signed'
  AND extension = 'xlsx'
  AND is_visible = 'y'
ORDER BY create_datetime DESC
LIMIT 1;

-- 이력 조회 (soft delete 포함)
SELECT *
FROM file_info
WHERE report_id = :reportId
  AND type IS NOT NULL
ORDER BY create_datetime;
```

---

## 13-3. Soft Delete 전략

### Soft Delete란

레코드를 실제로 삭제하지 않고, `is_visible='n'`으로 표시만 하는 방식.

```sql
-- Hard Delete (실제 삭제): 이력 없어짐
DELETE FROM file_info WHERE id = 1001;

-- Soft Delete (이번 프로젝트 방식): 이력 보존
UPDATE file_info SET is_visible = 'n' WHERE id = 1001;
```

### 왜 이력 보존이 중요한가

```
시나리오:
1. 기술책임자가 성적서에 결재
2. 이후 "이전 버전 파일이 필요해"라는 요청 발생
3. Soft Delete → DB에 이전 파일 경로 남아있음 → 스토리지에서 복구 가능
4. Hard Delete → 완전히 사라짐 → 복구 불가

또한:
- 감사 추적 (언제 어떤 파일이 있었는지)
- 실수로 덮어쓴 경우 이전 버전 파악
```

### origin 파일 교체 시

```java
// 실무자가 origin 파일을 재업로드할 때
// 1. 기존 origin 파일 soft delete
fileInfoRepository.softDeleteByReportIdAndType(reportId, "origin");
// UPDATE file_info SET is_visible='n' WHERE report_id=? AND type='origin' AND is_visible='y'

// 2. 새 origin 파일 insert
FileInfo newFile = FileInfo.builder()
    .reportId(reportId)
    .type("origin")
    .extension("xlsx")
    .storedPath("report/501/report_origin_v2.xlsx")
    .isVisible("y")
    .createDatetime(LocalDateTime.now())
    .build();
fileInfoRepository.save(newFile);
```

---

## 13-4. 파일명 규칙 / 경로 규칙 / UUID 디렉터리

### 스토리지 저장 경로 설계 원칙

```
{rootDir}/report/{reportId}/{파일명}
예: dev/report/501/report_origin.xlsx

선택적 UUID 디렉터리 패턴:
dev/report/501/{UUID}/report_origin.xlsx
→ 같은 reportId에 여러 버전이 있어도 경로 충돌 없음
→ 단, UUID를 file_info에 저장해야 복구 가능
```

### 이번 프로젝트 기준 경로 규칙 (제안, 확정 필요)

```
성적서 origin:  {rootDir}/report/{reportId}/report_origin.xlsx
성적서 signed:  {rootDir}/report/{reportId}/report_signed.xlsx
                {rootDir}/report/{reportId}/report_signed.pdf
샘플 파일:      {rootDir}/sample/{sampleId}/sample.xlsx
회원 서명:      {rootDir}/members/{memberId}/signature.png
회사 로고:      {rootDir}/env/kolas.png
```

---

## 13-5. 임시 파일 정리

### 작업 완료 후 임시 파일 정리

```java
// 성공 시 즉시 삭제
void cleanupWorkDir(Path workDir) {
    try {
        Files.walk(workDir)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try { Files.delete(path); }
                catch (IOException e) {
                    log.warn("임시 파일 삭제 실패: {}", path);
                }
            });
    } catch (IOException e) {
        log.error("임시 디렉터리 정리 실패: {}", workDir);
    }
}

// 실패 시 일정 시간 후 정리 (스케줄러)
@Scheduled(fixedDelay = 60 * 60 * 1000) // 1시간마다
void cleanupFailedWorkDirs() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(24); // 24시간 이상 된 것
    Path workBaseDir = Paths.get(appWorkDir);
    // workBaseDir 하위에서 24시간 지난 디렉터리 삭제
}
```

---

## 13-6. Orphan 파일 문제

### Orphan File이란

스토리지에는 파일이 있는데 DB(file_info)에는 레코드가 없는 경우.
또는 DB에는 레코드가 있는데 스토리지에는 파일이 없는 경우.

### 발생 원인

```
시나리오 1 (orphan file):
  1. 스토리지 업로드 성공
  2. DB INSERT 실패 (네트워크 오류, 제약조건 위반 등)
  3. 스토리지엔 파일 있음, DB엔 레코드 없음

시나리오 2 (orphan metadata):
  1. DB INSERT 성공
  2. 이후 스토리지 파일 수동 삭제
  3. DB엔 레코드 있음, 스토리지엔 파일 없음
```

### 방지 전략

```
원칙: 스토리지 업로드 성공 후 DB INSERT
→ 업로드 성공 = 파일 존재 확인
→ DB INSERT 실패 시 스토리지에서 파일 삭제 (보상 처리)
```

```java
try {
    // 1. 스토리지 업로드
    storageClient.upload(storedPath, fileBytes);
    // 2. DB에 file_info 저장
    fileInfoRepository.save(fileInfo);
} catch (Exception e) {
    // 업로드 후 DB 저장 실패 시 스토리지 파일 삭제 (보상)
    try {
        storageClient.delete(storedPath);
    } catch (Exception deleteEx) {
        log.error("보상 삭제 실패 - orphan file 발생: {}", storedPath);
    }
    throw e;
}
```

---

# 14장. 스토리지 연동

## 14-1. Object Storage 개념

### Object Storage vs File Storage

| 항목 | Object Storage | File Storage |
|---|---|---|
| 저장 방식 | 키-값 (경로=키, 파일=값) | 계층적 디렉터리 |
| 접근 방식 | HTTP API (REST) | 파일시스템 API |
| 확장성 | 무제한 | 서버 용량 제한 |
| 예시 | AWS S3, NCP Object Storage | 로컬 디스크, NFS |
| 이번 프로젝트 | ✅ NCP Object Storage 사용 | 로컬 임시 파일 |

### NCP Object Storage

AWS S3 호환 API를 제공한다.
→ AWS SDK v2로 접근 가능 (동일한 코드 사용 가능)

---

## 14-2. 업로드 / 다운로드 흐름

### CALI의 스토리지 클라이언트 구조

이미 구현된 ObjectStorageService를 참고:

```java
// CALI에서 이미 사용 중인 스토리지 설정
ncp.storage.endpoint=https://kr.object.ncloudstorage.com
ncp.storage.bucket-name=cali
ncp.storage.root-dir=dev
ncp.storage.access-key=...
ncp.storage.secret-key=...
```

### cali-worker에서의 스토리지 접근

트리거 페이로드에 스토리지 접속정보가 포함되므로,
cali-worker는 별도 설정 없이 이 정보로 스토리지 클라이언트를 생성:

```java
// 트리거 수신 후 스토리지 클라이언트 초기화
S3Client s3Client = S3Client.builder()
    .region(Region.of("kr-standard"))
    .endpointOverride(URI.create(triggerReq.getStorageEndpoint()))
    .credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create(
            triggerReq.getStorageAccessKey(),
            triggerReq.getStorageSecretKey()
        )
    ))
    .build();
```

### 파일 다운로드 (샘플 파일)

```java
// 스토리지에서 샘플 파일 다운로드
public byte[] downloadFile(S3Client s3Client, String bucketName, String key) {
    GetObjectRequest request = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(key) // 예: dev/sample/10/sample.xlsx
        .build();

    ResponseBytes<GetObjectResponse> response =
        s3Client.getObjectAsBytes(request);
    return response.asByteArray();
}
```

### 파일 업로드 (origin.xlsx)

```java
// 완성된 파일을 스토리지에 업로드
public void uploadFile(S3Client s3Client, String bucketName, String key, byte[] fileBytes) {
    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key) // 예: dev/report/501/report_origin.xlsx
        .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .build();

    s3Client.putObject(request, RequestBody.fromBytes(fileBytes));
}
```

---

## 14-3. Presigned URL 개념

### 개념

스토리지 파일을 **일시적으로 공개 접근 가능하게 하는 서명된 URL**.

```
일반 접근:
브라우저 → 인증 없이 스토리지 URL → ❌ (비공개 버킷)

Presigned URL:
브라우저 → CALI에 다운로드 요청
CALI → 스토리지에 presigned URL 발급 요청 (서버에서)
CALI → 브라우저에 presigned URL 반환
브라우저 → presigned URL로 스토리지 직접 접근 → ✅
```

### 장점

- 파일이 크더라도 CALI 서버를 거치지 않으므로 서버 부하 없음
- URL에 만료 시간 설정 가능 (예: 5분)
- 다운로드 권한을 CALI가 통제

### Spring Boot에서 Presigned URL 생성

```java
// CALI에서 구현 (기존 ObjectStorageService 확장 가능)
S3Presigner presigner = S3Presigner.builder()
    .region(Region.of("kr-standard"))
    .endpointOverride(URI.create(storageEndpoint))
    .credentialsProvider(...)
    .build();

GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
    .signatureDuration(Duration.ofMinutes(5)) // 5분 유효
    .getObjectRequest(req -> req.bucket(bucketName).key(storedPath))
    .build();

PresignedGetObjectRequest presignedReq = presigner.presignGetObject(presignRequest);
String downloadUrl = presignedReq.url().toString();
```

---

## 14-4. 업로드 성공 후 DB 반영 순서

### 올바른 순서

```
1. 파일 처리 (POI 작업)
2. 로컬 임시 파일 생성
3. 스토리지 업로드 (시간 걸림)
4. 업로드 성공 확인
5. DB에 file_info INSERT (트랜잭션)
6. DB에 report 상태 업데이트 (트랜잭션)
7. CALI에 SUCCESS 콜백 전송
```

### 잘못된 순서와 결과

```
잘못된 예:
1. DB에 file_info INSERT
2. 스토리지 업로드 실패
→ DB엔 레코드 있으나 스토리지엔 파일 없음 (orphan metadata)

또는:
1. DB 상태 SUCCESS 업데이트
2. 스토리지 업로드 실패
→ 사용자에게 성공으로 보이지만 파일이 없음
```

### 이번 프로젝트 적용

cali-worker가 콜백을 보내면 CALI가 DB를 갱신한다.
따라서 cali-worker의 책임:
- 스토리지 업로드 성공 → SUCCESS 콜백 (storedPath 포함 가능)
- 스토리지 업로드 실패 → FAIL 콜백

CALI는 SUCCESS 콜백 수신 시 DB 갱신:
- file_info INSERT (storedPath 포함)
- report.write_status = SUCCESS

---

## 14-5. Checksum / 무결성

### 개념

파일 전송 중 손상되었는지 확인하는 방법.

```java
// 업로드 전 checksum 계산
MessageDigest md = MessageDigest.getInstance("MD5");
byte[] checksum = md.digest(fileBytes);
String checksumHex = HexFormat.of().formatHex(checksum);

// S3/NCP에 업로드 시 MD5 포함
PutObjectRequest request = PutObjectRequest.builder()
    .bucket(bucketName)
    .key(key)
    .contentMD5(Base64.getEncoder().encodeToString(checksum))
    .build();
// S3가 수신 후 MD5를 검증 — 불일치 시 업로드 거부
```

### 이번 프로젝트 필요성

성적서 파일 크기가 크지 않고 HTTPS로 전송하므로
초기에는 checksum 없이 진행해도 큰 문제 없음.
대용량 파일이나 신뢰성이 중요해지면 MD5 검증 추가.

---

# 15장. 멀티테넌시와 업체별 설정

## 15-1. 업체별 envInfo

### 현재 구조

```
env 테이블 (단일 row, id=1):
  - name, name_en, ceo, tel, fax ...
  - kolas (로고 이미지 경로)
  - ilac (로고 이미지 경로)
  - sheet_info_setting (JSON — 셀 위치 매핑)
```

현재는 **단일 업체** (1개 회사)만 지원하는 구조다.
여러 업체가 같은 시스템을 사용한다면 멀티테넌시 구조가 필요하다.

### 멀티테넌시 확장 방향 (참고)

```sql
-- 단일 업체 현재:
env 테이블 (id=1)

-- 멀티테넌시 확장 시:
tenant (id, name, ...)
env (id, tenant_id, ...) -- tenant별 설정
```

단, 이번 프로젝트는 단일 업체 기준이므로 복잡한 멀티테넌시 설계 불필요.

---

## 15-2. 업체별 스토리지 경로 분리

### 현재 분리 방식

```
{rootDir} 값으로 환경 분리:
  dev/   → 개발 환경
  prod/  → 운영 환경
```

### 업체별 스토리지 분리 (확장 시 참고)

```
방법 1: 같은 버킷, 다른 prefix
  bucket/tenant-A/report/...
  bucket/tenant-B/report/...

방법 2: 다른 버킷
  bucket-A/report/...
  bucket-B/report/...

방법 3: 다른 스토리지 계정
  각 업체별 별도 NCP 계정
```

---

## 15-3. 민감정보 분리

### 민감정보의 흐름

```
DB에 저장되는 것:
  env.sheet_info_setting (비민감 - 셀 위치 정보)
  env.name, tel 등 (비민감 - 공개 정보)

외부 설정파일에만 존재하는 것:
  ncp.storage.access-key (민감)
  ncp.storage.secret-key (민감)
  app.worker.api-key (민감)

CALI → cali-worker 트리거 시 전달 (메모리에서 메모리로):
  storageAccessKey (외부 설정파일에서 읽어서 전달)
  storageSecretKey

주의: 이 정보들이 DB에 저장되거나 로그에 출력되면 안 됨
```

### 로그에 민감정보 노출 방지

```java
// 나쁜 패턴
log.info("트리거 요청: {}", req); // storageSecretKey가 toString()에 포함될 수 있음

// 좋은 패턴
log.info("트리거 요청: batchId={}, callbackBaseUrl={}", req.getBatchId(), req.getCallbackBaseUrl());
// 민감 필드는 로그에서 제외

// 또는 @ToString(exclude = {...}) lombok 애너테이션 활용
@ToString(exclude = {"storageAccessKey", "storageSecretKey"})
public class WorkerTriggerReq { ... }
```

---

# 16장. 큐와 메시징

## 16-1. 현재는 왜 메시지큐 없이도 가능한가

### 이번 구조에서 "큐"의 역할을 하는 것

```
전통적인 큐 구조:
  CALI → [Redis/Kafka 큐] → cali-worker

이번 구조:
  CALI → [DB: report_job_item (status=READY)] → cali-worker
```

DB가 큐 역할을 한다:
- READY 상태 item 목록 = 처리 대기 큐
- cali-worker가 CALI API 호출로 item 조회 = 큐에서 꺼내기
- item 상태 변경 = 큐 처리 완료 표시

### 이것이 가능한 조건

| 조건 | 현재 상태 |
|---|---|
| 작업서버 1대 | ✅ 단일 워커 |
| 요청 동시성 낮음 | ✅ 사용자 수 적음 |
| 처리 순서 엄격하지 않음 | ✅ item 간 순서 무관 |
| 재시도 정책 단순 | ✅ 실패 = FAIL 상태 |

---

## 16-2. 언제 Redis Queue가 필요해지는가

### 전용 큐가 필요해지는 신호

```
1. 작업서버를 2대 이상으로 늘리고 싶을 때
   → 여러 워커가 같은 item을 동시에 처리하면 안 됨
   → DB 기반으로는 동시성 제어 복잡

2. 처리량이 증가하여 DB polling이 병목이 될 때
   → DB에 item 조회 쿼리가 너무 자주 실행됨

3. 실시간성이 중요해질 때
   → 트리거 없이 큐에 메시지 넣으면 즉시 처리 시작

4. 복잡한 재시도 정책이 필요할 때
   → 지수 백오프, dead letter queue 등
```

---

## 16-3. Redis란 무엇인가

### 개념

Redis(Remote Dictionary Server)는 **인메모리 데이터 저장소**다.
키-값 구조로 데이터를 메모리에 저장하므로 매우 빠르다.

```
Redis 특성:
  - 읽기/쓰기 속도: 마이크로초 단위 (DB 대비 100~1000배 빠름)
  - 데이터 구조: String, List, Set, Hash, Sorted Set, Stream 등
  - 영속성: 디스크에 주기적으로 저장 가능 (RDB/AOF)
  - 메시지 큐: List, Pub/Sub, Streams 사용 가능
```

---

## 16-4. Redis Pub/Sub vs Redis Streams vs Redis List

### Redis List (간단한 큐)

```
LPUSH queue "batchId=101"   # 왼쪽에 추가 (producer)
BRPOP queue 0               # 오른쪽에서 blocking 대기 후 꺼내기 (consumer)
```

- 단순, 구현 쉬움
- 메시지 재전송 어려움
- consumer 장애 시 메시지 유실 가능

### Redis Pub/Sub

```
Publisher → [채널] → Subscriber1
                   → Subscriber2
```

- 메시지 브로드캐스트 (여러 subscriber 동시 수신)
- subscriber가 오프라인이면 메시지 유실 (Fire and Forget)
- 이번 프로젝트에서는 큐가 아닌 알림용으로 적합

### Redis Streams (권장, 엔터프라이즈 급)

```
Stream: 이벤트 로그 (append-only)
Consumer Group: 여러 consumer가 분산 처리
ACK: 처리 완료 확인
```

| 항목 | Pub/Sub | Streams |
|---|---|---|
| 메시지 유실 | 오프라인 시 유실 | 영속화 가능 |
| 재처리 | 불가 | 가능 (pending 목록) |
| Consumer Group | 없음 | 있음 |
| 처리 확인 | 없음 | ACK 필요 |
| 복잡도 | 낮음 | 중간 |
| 이번 프로젝트 | 알림용 | 큐 도입 시 권장 |

---

## 16-5. Consumer Group 개념

### 왜 필요한가

worker가 2대일 때 같은 메시지를 중복 처리하면 안 된다.

```
메시지: [item1, item2, item3, item4]
Worker A: item1 처리
Worker B: item2 처리
Worker A: item3 처리
Worker B: item4 처리
→ 각 item이 한 번씩만 처리됨
```

Redis Streams의 Consumer Group이 이를 보장한다:
- 각 메시지는 group 내 하나의 consumer에게만 배달
- 처리 후 ACK → 완료 표시
- ACK 없으면 pending → 재처리 가능

---

## 16-6. Dead Letter Queue (DLQ)

### 개념

처리에 계속 실패하는 메시지를 별도 큐(Dead Letter Queue)로 옮겨
정상 처리 흐름을 방해하지 않는 패턴.

```
정상 처리 흐름:
  [main-queue] → worker → ACK → 완료

실패 처리:
  [main-queue] → worker → 실패 → retry (3회)
              → 계속 실패 → [dead-letter-queue]
              → 관리자 확인 및 수동 처리
```

이번 프로젝트에서의 DLQ 역할:
→ FAIL 상태의 item이 DLQ 역할을 함
→ 관리자가 FAIL 상태 목록을 확인하고 수동 재처리

---

## 16-7. 이 프로젝트에서의 Redis 도입 시점 판단 기준

```
도입 필요 없음 (현재):
  ✓ 작업서버 1대
  ✓ 하루 처리 건수 100건 미만
  ✓ 트리거 방식 유지
  ✓ DB 기반 상태 관리 충분

도입 고려 시점:
  ○ 작업서버를 2대로 늘리려 할 때
  ○ DB 조회 빈도가 초당 10회 이상일 때
  ○ 복잡한 재시도 정책이 필요할 때
  ○ 트리거 없이 이벤트 기반으로 전환할 때
```

---

# 17장. 운영, 로깅, 모니터링

## 17-1. Structured Logging

### 개념

로그를 단순 문자열이 아닌 **구조화된 형태(JSON 등)**로 출력하여
검색, 필터링, 분석이 쉽게 하는 방식.

### 이번 프로젝트 로그 포맷 권장

```
[timestamp] [level] [batchId] [itemId] [reportId] 메시지
```

```java
// logback 패턴 설정 (logback-spring.xml)
<pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level [batch=%X{batchId}][item=%X{itemId}] %logger{36} - %msg%n</pattern>
```

### MDC(Mapped Diagnostic Context) 활용

```java
// 처리 시작 시 MDC에 컨텍스트 설정
MDC.put("batchId", String.valueOf(batchId));
MDC.put("itemId", String.valueOf(itemId));
try {
    // 처리 로직
    log.info("샘플 파일 다운로드 시작");   // 자동으로 [batch=101][item=1001] 포함
    log.info("데이터 삽입 완료");
} finally {
    MDC.clear(); // 반드시 정리
}
```

### 로그 레벨 기준

| 레벨 | 사용 기준 |
|---|---|
| TRACE | 매우 세밀한 디버깅 (개발 시만) |
| DEBUG | 상세 처리 내용, 변수값 |
| INFO | 주요 처리 완료, 상태 변경 |
| WARN | 예상된 문제 (재시도, 개발모드 경고) |
| ERROR | 예상치 못한 오류, 처리 실패 |

```java
log.info("[item={}] DOWNLOADING_TEMPLATE 완료", itemId);     // INFO
log.warn("app.worker.api-key 미설정 — API 키 검증 생략");      // WARN (개발 모드)
log.error("[item={}] 처리 실패: {}", itemId, e.getMessage()); // ERROR (스택트레이스 별도)
```

---

## 17-2. batchId / itemId 기반 로그 추적

### 예시 로그 흐름

```
2026-03-20 10:30:00 INFO  [batch=101][item=  0] 트리거 수신 - batchId=101
2026-03-20 10:30:01 INFO  [batch=101][item=  0] 배치 정보 조회 완료 - items=5
2026-03-20 10:30:01 INFO  [batch=101][item=1001] DOWNLOADING_TEMPLATE 시작 - reportId=501
2026-03-20 10:30:02 INFO  [batch=101][item=1001] DOWNLOADING_TEMPLATE 완료 - size=245678 bytes
2026-03-20 10:30:03 INFO  [batch=101][item=1001] FILLING_DATA 시작
2026-03-20 10:30:04 INFO  [batch=101][item=1001] FILLING_DATA 완료 - fields=23
2026-03-20 10:30:05 INFO  [batch=101][item=1001] UPLOADING_ORIGIN 시작
2026-03-20 10:30:06 INFO  [batch=101][item=1001] UPLOADING_ORIGIN 완료 - path=dev/report/501/...
2026-03-20 10:30:06 INFO  [batch=101][item=1001] SUCCESS 콜백 전송 완료
2026-03-20 10:30:07 INFO  [batch=101][item=1002] DOWNLOADING_TEMPLATE 시작 - reportId=502
...
2026-03-20 10:31:05 ERROR [batch=101][item=1003] FILLING_DATA 실패 - NullPointerException: sheet_info_setting.fields is null
2026-03-20 10:31:05 INFO  [batch=101][item=1003] FAIL 콜백 전송 완료
```

이 로그만 보면 batch 101의 전체 처리 흐름, 어느 item에서 어느 단계에서 실패했는지 즉시 파악 가능.

---

## 17-3. Metrics — 성공률 / 실패율 / 처리시간

### Spring Boot Actuator + Micrometer

```properties
# application.properties
management.endpoints.web.exposure.include=health,metrics,info
```

### 사용자 정의 메트릭

```java
// Micrometer Counter / Timer 사용
@Autowired
MeterRegistry registry;

// 처리 결과 카운터
Counter successCounter = Counter.builder("worker.item.success")
    .description("성공 처리 건수")
    .register(registry);

Counter failCounter = Counter.builder("worker.item.fail")
    .description("실패 처리 건수")
    .register(registry);

// 처리 시간 타이머
Timer processingTimer = Timer.builder("worker.item.duration")
    .description("item 처리 소요 시간")
    .register(registry);

// 사용
Timer.Sample sample = Timer.start(registry);
try {
    processItem(item);
    successCounter.increment();
} catch (Exception e) {
    failCounter.increment();
} finally {
    sample.stop(processingTimer);
}
```

### 메트릭 조회

```bash
# Spring Actuator Metrics 엔드포인트
curl http://localhost:8060/actuator/metrics/worker.item.success
# {"name":"worker.item.success","measurements":[{"statistic":"COUNT","value":15.0}],...}
```

---

## 17-4. Health Endpoint

```properties
# 헬스체크 설정
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=when-authorized  # 인증된 사용자만 상세 표시
```

### 커스텀 Health Indicator

```java
@Component
public class StorageHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            // 스토리지 연결 테스트
            storageClient.listBuckets();
            return Health.up().withDetail("storage", "연결됨").build();
        } catch (Exception e) {
            return Health.down().withDetail("storage", "연결 실패: " + e.getMessage()).build();
        }
    }
}
```

```bash
curl http://localhost:8060/actuator/health
# {"status":"UP","components":{"storage":{"status":"UP","details":{"storage":"연결됨"}}}}
```

---

## 17-5. 장애 분석 흐름

### 장애 상황 1: 배치가 PROGRESS 상태에서 멈춤

```
원인 파악 단계:
1. CALI DB 확인
   SELECT * FROM report_job_batch WHERE id = 101;
   → status=PROGRESS, start_datetime 확인

2. cali-worker 로그 확인
   journalctl -u cali-worker --since "2026-03-20 10:00:00"
   → [batch=101] 마지막 로그 확인

3. item 상태 확인
   SELECT * FROM report_job_item WHERE batch_id = 101;
   → 어느 item에서 멈췄는지 확인

4. 원인 판단
   → cali-worker가 죽었는가? → systemctl status cali-worker
   → 스토리지 접근 실패? → 스토리지 API 응답 확인
   → DB 연결 실패? → DB 접속 확인
```

### 장애 상황 2: item이 FAIL 상태로 끝남

```
원인 파악 단계:
1. item 상세 조회
   SELECT message FROM report_job_item WHERE id = 1003;
   → "PDF 변환 실패: libreoffice: command not found"

2. 원인 파악
   → LibreOffice 미설치 → sudo apt install libreoffice

3. 재처리 방법
   → 해당 item만 READY로 되돌리고 다시 트리거 (미구현)
   → 또는 사용자가 다시 배치 생성
```

### 장애 상황 3: CALI 콜백 전송 실패

```
원인 파악 단계:
1. cali-worker 로그에서 콜백 실패 확인
   [item=1001] CALI 콜백 실패: Connection refused to localhost:8050

2. CALI 상태 확인
   curl http://localhost:8050/actuator/health

3. 네트워크 확인 (WSL2 환경)
   → WSL2 → Windows 방향의 callbackBaseUrl 문제
   → Windows IP로 변경 필요

4. 임시 조치
   → cali-worker가 재시작 후 PROGRESS item을 FAIL로 처리
   → 사용자가 재시도
```

---

# 18장. WSL2 → 홈서버 → 클라우드 확장

## 18-1. 무엇이 그대로 유지되는가

### 환경이 바뀌어도 변하지 않는 것

```
1. 애플리케이션 코드 (JAR 파일)
   → Java 코드는 어느 Linux 환경에서도 동일 실행

2. 통신 프로토콜
   → REST API, HTTP 헤더, JSON 페이로드 구조 변경 없음

3. DB 스키마
   → MySQL은 어느 환경에도 동일

4. 스토리지 API
   → NCP Object Storage S3 호환 API 변경 없음

5. 비즈니스 로직
   → batch/item 처리 로직, 상태 머신 변경 없음
```

### 무엇이 달라지는가

```
1. 설정값 (application.properties)
   → callbackBaseUrl: localhost:8050 → 실제 서버 IP/도메인
   → storageEndpoint: 동일할 수 있음 (NCP 사용 유지 시)

2. 네트워크 구조
   → WSL2: Windows 내부 통신
   → 홈서버: 같은 공유기 내부 통신
   → 클라우드: 인터넷 통신 (HTTPS 필요)

3. 서비스 관리
   → WSL2: 수동 실행 또는 systemd
   → 홈서버: systemd (항상 실행)
   → 클라우드: Docker, 오케스트레이션 가능

4. 보안 강화 필요
   → 클라우드에서는 API Key만으로는 부족 → HTTPS, IP 화이트리스트 추가
```

---

## 18-2. 홈서버로 가면 무엇이 달라지는가

### 네트워크 변경사항

```
WSL2 구성:
  CALI: localhost:8050 (Windows)
  cali-worker: localhost:8060 (WSL2, NAT)
  callbackBaseUrl: http://172.x.x.x:8050 (Windows IP)

홈서버 구성:
  CALI: 192.168.1.100:8050 (Windows PC)
  cali-worker: 192.168.1.200:8060 (미니PC Ubuntu)
  callbackBaseUrl: http://192.168.1.100:8050
  app.worker.url: http://192.168.1.200:8060
```

### 설정 변경 내용 (외부 properties만 수정)

```properties
# /opt/cali-worker/application-prod.properties (홈서버)
app.cali.callback-base-url=http://192.168.1.100:8050

# CALI의 외부 properties (Windows)
app.worker.url=http://192.168.1.200:8060
```

---

## 18-3. 환경별 비교표

| 항목 | WSL2 (현재) | 홈서버 | 클라우드 |
|---|---|---|---|
| 비용 | 없음 | 전기세 + 하드웨어 | 월정액 |
| 안정성 | Windows 재시작 시 중단 | UPS 없으면 정전 시 중단 | 가용성 99.9%+ |
| 외부 접근 | 어려움 | 포트포워딩 필요 | 쉬움 (퍼블릭 IP) |
| 확장성 | 없음 | 제한적 | 자유로움 |
| 보안 | 내부망 | 내부망 | 인터넷 노출 |
| 관리 편의 | 개발 친화적 | 직접 관리 | 관리 도구 풍부 |
| 이번 단계 | ✅ 현재 | 🔄 다음 | 🔲 추후 |

---

## 18-4. Docker를 붙일 때 달라지는 점

### Docker의 이점

```
Dockerfile:
  FROM openjdk:17-slim
  COPY cali-worker.jar /app/cali-worker.jar
  ENTRYPOINT ["java", "-jar", "/app/cali-worker.jar"]

docker-compose.yml:
  services:
    cali-worker:
      build: .
      ports:
        - "8060:8060"
      environment:
        - SPRING_PROFILES_ACTIVE=prod
      volumes:
        - /opt/cali-worker/application-prod.properties:/app/application-prod.properties
      restart: unless-stopped
```

- 환경 일관성: 어느 서버에서도 동일한 실행 환경
- 버전 관리: 이미지 태그로 롤백 가능
- 격리: 다른 앱과 환경 격리

### Docker 도입 시 네트워크 변경

```
Docker 컨테이너 내부에서 외부 접근:
  host.docker.internal → 호스트 머신 접근 (Windows/Mac)
  gateway IP → Linux 호스트 접근

callbackBaseUrl 변경 필요:
  기존: http://localhost:8050
  Docker: http://host.docker.internal:8050
```

---

## 18-5. 확장 순서 추천

### 단계별 확장 로드맵

```
Phase A (현재): WSL2 로컬 개발
  → CALI: Windows 8050
  → cali-worker: WSL2 8060
  → 목적: 기능 개발 및 엔드투엔드 테스트

Phase B (다음): 홈서버(미니PC) 배포
  → CALI: 기존 Windows PC 유지
  → cali-worker: 미니PC Ubuntu, systemd 서비스
  → 목적: 항상 켜져 있는 안정적인 워커

Phase C (선택): Docker 컨테이너화
  → cali-worker를 Docker 이미지로 빌드/배포
  → 목적: 배포 표준화, 버전 관리 편의성

Phase D (필요 시): 퍼블릭 클라우드
  → NCP, AWS, GCP 등 클라우드 인스턴스
  → HTTPS, 로드밸런서 추가
  → Redis Queue 도입 고려

Phase E (필요 시): 오케스트레이션
  → Kubernetes, Docker Swarm
  → 다중 워커 인스턴스
  → Redis Consumer Group으로 작업 분산
```

---

## 18-6. Redis/Queue/SSE 도입 시 달라지는 점

### SSE 전환 시

```java
// 현재 (Polling):
GET /api/report/jobs/batches/{batchId}  → 주기적 호출

// SSE 전환 후:
GET /api/report/jobs/batches/{batchId}/events  → 연결 유지, 이벤트 push
```

CALI의 handleItemCallback에서 SseEmitter로 이벤트 push:
```java
// 콜백 수신 시
emitterMap.get(batchId).send(SseEmitter.event()
    .name("item-update")
    .data(itemStatus)
);
```

변경되는 것: 프론트 Polling → EventSource API, 백엔드 GET API → SSE 엔드포인트
변경 안 되는 것: DB 구조, 콜백 API, 트리거 구조

### Redis Queue 도입 시

```
변경되는 것:
  - cali-worker의 item 조회 방식
    → CALI API 호출 → Redis에서 메시지 직접 소비
  - 트리거 방식
    → REST 호출 → Redis에 메시지 publish

변경 안 되는 것:
  - CALI의 콜백 수신 API (/api/worker/items/{id}/callback)
  - DB의 batch/item 상태 관리
  - 스토리지 연동 방식
  - 파일 처리 로직 (POI, PDF 변환)
```

---

## 18-7. 핵심 요약 (13~18장)

### 파일 저장
- origin(원본) / signed(서명본) type 컬럼으로 명확히 구분
- Soft Delete → is_visible='n', 이력 보존
- 스토리지 업로드 성공 후 DB INSERT (역순 금지)
- Orphan 파일 방지: 실패 시 스토리지 파일 보상 삭제

### 스토리지
- NCP Object Storage = S3 호환 → AWS SDK v2 사용
- 트리거 페이로드로 접속정보 전달 (DB 비저장)
- Presigned URL = 브라우저 직접 다운로드 (서버 부하 없음)

### Redis/큐
- 현재: DB 기반 상태 관리로 충분 (단일 워커)
- 도입 시점: 다중 워커, 처리량 급증, 복잡한 재시도 필요 시
- Redis Streams = 안정적인 큐 (ACK, Consumer Group 지원)

### 운영/로깅
- MDC로 batchId/itemId 자동 포함
- 장애 분석 순서: DB 상태 → cali-worker 로그 → 네트워크 확인
- Health endpoint로 스토리지 연결 상태 포함

### 확장
- JAR 코드와 비즈니스 로직은 환경 변경 시 불변
- 설정값(properties)만 변경으로 환경 전환 가능
- Docker 도입 → 배포 표준화
- SSE/Redis 도입 → API 구조 일부 변경, 핵심 로직 유지

---

*→ Part D: 최종 요약 / 우선순위 TOP 10 / 구현 전 체크리스트 / 흔한 오해*