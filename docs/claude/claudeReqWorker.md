# cali-worker 프로젝트 — 작업 요청서 및 진행 현황

> 이 문서는 PC 이전 후 새 환경에서 작업을 재개하기 위한 **전체 컨텍스트 요약서**입니다.
> CALI(교정관리) 연동 워커 서버(`cali-worker`)의 설계, 구현 현황, 다음 작업 목록을 모두 포함합니다.
> 이 문서를 Claude Code에게 먼저 읽힌 뒤 작업을 지시하세요.

---

## 0. 전체 흐름 요약

```
[브라우저(SSR/Thymeleaf)]
      ↓  성적서작성 버튼 클릭
[CALI 서버 :8050]
  POST /api/report/jobs/batches
      → report_job_batch + report_job_item(n건) 생성 (READY 상태)
      → 워커 트리거 POST http://localhost:8060/api/jobs/execute
      → 202 즉시 반환

[cali-worker 서버 :8060]  ← ★ 이 프로젝트
  POST /api/jobs/execute 수신 → 202 반환 (즉시)
      ↓  비동기 처리
  GET  {callbackBaseUrl}/api/worker/batches/{batchId}   (item 목록 조회)
  각 item 처리:
    POST {callbackBaseUrl}/api/worker/items/{itemId}/callback  (PROGRESS)
    — 스토리지에서 샘플 다운로드
    — 엑셀에 데이터 삽입
    — 완성 파일 스토리지 업로드
    — file_info DB 기록 요청 (콜백)
    POST {callbackBaseUrl}/api/worker/items/{itemId}/callback  (SUCCESS / FAIL)

[브라우저]
  GET /api/report/jobs/batches/{batchId}  (1~2초 Polling)
      → 진행상황 표시 (progress bar + item별 상태)
```

---

## 1. 두 프로젝트 개요

| 항목 | CALI (본체) | cali-worker |
|---|---|---|
| 경로 | `C:\BadaDev\cali\backend` | `C:\BadaDev\cali-worker` |
| 포트 | 8050 | 8060 |
| 패키지 | `com.bada.cali` | `com.bada.caliworker` |
| 역할 | 교정관리 웹앱 (Spring Boot + Thymeleaf) | 성적서 작업 워커 (독립 Spring Boot) |
| Spring Boot | 3.5.x | 3.5.7 |
| Java | 17 | 17 |
| Git repo | `BadaDev/cali` | 별도 repo (github에 생성 완료) |

---

## 2. CALI 측 구현 현황 (완료)

### 2-1. DB 테이블

**`report_job_batch`** — 성적서작업 배치(1회 클릭 = 1 batch)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | 배치 id |
| job_type | VARCHAR | WRITE / WORK_APPROVAL / MANAGER_APPROVAL |
| request_member_id | BIGINT FK | 요청한 사용자 |
| sample_id | BIGINT FK NULL | WRITE 타입 전용 — 선택한 샘플 id |
| total_count | INT | 전체 성적서 수 |
| success_count | INT | 성공 수 |
| fail_count | INT | 실패 수 |
| status | VARCHAR | READY / PROGRESS / SUCCESS / FAIL / CANCELED |
| create_datetime | DATETIME | 생성 시각 |
| start_datetime | DATETIME NULL | 워커가 처리 시작한 시각 |
| end_datetime | DATETIME NULL | 완료 시각 |

**`report_job_item`** — 개별 성적서 단위 작업

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | item id |
| batch_id | BIGINT FK | 소속 배치 |
| report_id | BIGINT FK | 대상 성적서 |
| status | VARCHAR | READY / PROGRESS / SUCCESS / FAIL / CANCELED |
| step | VARCHAR NULL | 현재 처리 단계 (아래 step 정의 참조) |
| message | VARCHAR NULL | 처리 메시지 / 실패 사유 |
| retry_count | INT | 재시도 횟수 |
| start_datetime | DATETIME NULL | |
| end_datetime | DATETIME NULL | |

### 2-2. step 값 정의

**WRITE(성적서작성) 단계:**
```
READY → PROGRESS → DOWNLOADING_TEMPLATE → FILLING_DATA → UPLOADING_ORIGIN → DONE
```

**WORK_APPROVAL(실무자결재) 단계 (향후):**
```
READY → PROGRESS → PREPARING → LOADING_ORIGIN → APPLYING_WORKER_SIGN → GENERATING_PDF → UPLOADING_RESULT → DONE
```

**MANAGER_APPROVAL(기술책임자결재) 단계 (향후):**
```
READY → PROGRESS → PREPARING → LOADING_ORIGIN → APPLYING_MANAGER_SIGN → GENERATING_PDF → UPLOADING_RESULT → DONE
```

### 2-3. 구현된 API

**`ReportJobBatchController`** — `/api/report/jobs/**`

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/report/jobs/batches` | 성적서작성 배치 생성 + 워커 트리거 |
| GET | `/api/report/jobs/batches/{batchId}` | 배치 진행상황 조회 (브라우저 Polling) |

**`WorkerCallbackController`** — `/api/worker/**`

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/worker/batches/{batchId}` | 배치 + item 목록 조회 (워커용) |
| POST | `/api/worker/items/{itemId}/callback` | item 처리 결과 콜백 수신 |

> `SecurityConfig`에서 `/api/worker/**` 경로는 `permitAll`로 설정되어 있음.
> 세션 인증 대신 `X-Worker-Api-Key` 헤더로 검증.

**기타 관련 API**

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/report/validateWrite` | 성적서작성 전 검증 (소분류 동일 여부, write_status 확인 등) |

### 2-4. CALI 관련 설정값 (`application-dev.properties`)

```properties
# 워커 서버 URL (비어있으면 트리거 생략 — 개발 모드)
app.worker.url=http://localhost:8060

# CALI ↔ 워커 공용 API 키 (워커의 inbound-api-key와 동일한 값으로 설정)
app.worker.api-key=cw-api-key-dev-2026

# CALI 콜백 베이스 URL (워커가 콜백 시 사용)
app.cali.callback-base-url=http://localhost:8050
```

> 이 값들은 gitignore 대상인 외부 설정파일에 존재. PC마다 로컬에서 직접 설정 필요.

### 2-5. 프론트엔드 (reportWrite.js / workApproval.js)

**`reportWrite.js`** 샘플 그리드 클릭 플로우 (4단계):

1. `POST /api/report/validateWrite` — 선택된 성적서 유효성 검증 (소분류 동일, write_status 등)
2. `adminConfirm` — 사용자 최종 확인 다이얼로그
3. `POST /api/report/jobs/batches` — 배치 생성 + 워커 트리거
4. Polling 시작 — `GET /api/report/jobs/batches/{batchId}` 1.5초 간격

**`workApproval.js`** 성적서수정 모달에 '성적서작성' 버튼 추가 완료.

---

## 3. cali-worker 구현 현황

### 3-1. 생성 완료된 파일 ✅

```
C:\BadaDev\cali-worker\
├── .gitignore
├── README.md
├── settings.gradle
├── build.gradle
├── gradle\wrapper\gradle-wrapper.properties
└── src\main\
    ├── resources\application.properties
    └── java\com\bada\caliworker\
        ├── CaliWorkerApplication.java
        └── dto\
            ├── ResMessage.java          ← CALI API 응답 역직렬화용
            └── JobDTO.java              ← TriggerReq/Res, BatchStatusRes, ItemCallbackReq 등
```

**주요 내용 요약:**

- `CaliWorkerApplication.java`: `@SpringBootApplication` + `@EnableAsync`
- `ResMessage<T>`: CALI API 응답 래퍼 (code > 0 = 정상)
- `JobDTO.TriggerReq`: CALI → 워커 트리거 요청 (batchId, callbackBaseUrl, workerApiKey, 스토리지 접속정보)
- `JobDTO.TriggerRes`: 202 응답
- `JobDTO.BatchStatusRes` / `ItemStatusRes`: 배치 조회 응답 역직렬화
- `JobDTO.ItemCallbackReq`: item 결과 콜백 페이로드 (status, step, message)

### 3-2. 아직 생성 안 된 파일 🔲

```
C:\BadaDev\cali-worker\
├── CLAUDE.md                                    ← 워커 전용 Claude Code 작업 규칙
├── gradlew                                      ← 필수! gradle wrapper 실행 파일
├── gradlew.bat                                  ← 필수! (Windows용)
├── gradle\wrapper\gradle-wrapper.jar            ← 필수! wrapper jar
└── src\main\java\com\bada\caliworker\
    ├── api\
    │   └── JobController.java                   ← POST /api/jobs/execute 수신
    ├── client\
    │   └── CaliApiClient.java                   ← CALI REST API 호출 클라이언트
    ├── config\
    │   ├── ApiKeyInterceptor.java               ← 인바운드 API 키 검증 인터셉터
    │   ├── WebConfig.java                       ← 인터셉터 등록
    │   └── AsyncConfig.java                     ← @EnableAsync 스레드풀 설정
    └── service\
        └── JobServiceImpl.java                  ← 배치 비동기 처리 로직 (핵심)
```

> **gradlew 파일 생성 방법**: cali-worker 디렉토리에서 아래 중 하나 실행
> - `gradle wrapper` (Gradle이 로컬에 설치된 경우)
> - 또는 `cali\backend`의 gradlew/gradlew.bat/gradle-wrapper.jar를 복사 후 사용

---

## 4. 다음 작업: Phase 2-1 (통신 테스트 스텁)

**목표**: 실제 엑셀 작업 없이 CALI ↔ cali-worker 통신 흐름만 검증

### 4-1. 구현 범위

| 파일 | 구현 내용 |
|---|---|
| `ApiKeyInterceptor.java` | `X-Worker-Api-Key` 헤더 검증. `app.inbound-api-key` 비어있으면 개발 모드(검증 생략) |
| `WebConfig.java` | `/api/**` 경로에 `ApiKeyInterceptor` 등록 |
| `AsyncConfig.java` | `@EnableAsync` 스레드풀 설정 (core=2, max=5, queue=100) |
| `CaliApiClient.java` | CALI API 호출 래퍼. `getBatchStatus()`, `sendCallback()` 구현 |
| `JobController.java` | `POST /api/jobs/execute` 수신 → 202 즉시 반환 + `@Async` 처리 위임 |
| `JobServiceImpl.java` | `@Async` 비동기 처리. item 1건씩 PROGRESS → SUCCESS 콜백 (스텁: 실제 엑셀 없음) |

### 4-2. JobController 동작 명세

```
POST /api/jobs/execute
  Header: X-Worker-Api-Key: {inboundApiKey}
  Body: JobDTO.TriggerReq

처리:
  1. 인터셉터에서 API 키 검증
  2. 요청 검증 (@Valid)
  3. 202 Accepted 즉시 반환 (JobDTO.TriggerRes)
  4. jobService.processAsync(req) 비동기 실행
```

### 4-3. JobServiceImpl 스텁 동작 명세

```java
@Async
public void processAsync(TriggerReq req) {
    // 1. CALI에서 배치 + item 목록 조회
    BatchStatusRes batch = caliApiClient.getBatchStatus(req.getBatchId(), ...);

    // 2. item 루프
    for (ItemStatusRes item : batch.getItems()) {
        // PROGRESS 콜백 (단계: DOWNLOADING_TEMPLATE)
        caliApiClient.sendCallback(item.getItemId(), "PROGRESS", "DOWNLOADING_TEMPLATE", null, ...);
        Thread.sleep(500); // 스텁 딜레이

        // PROGRESS 콜백 (단계: FILLING_DATA)
        caliApiClient.sendCallback(item.getItemId(), "PROGRESS", "FILLING_DATA", null, ...);
        Thread.sleep(500);

        // SUCCESS 콜백
        caliApiClient.sendCallback(item.getItemId(), "SUCCESS", "DONE", "스텁 처리 완료", ...);
    }
}
```

### 4-4. CaliApiClient 구현 명세

```java
// 배치 조회: GET {callbackBaseUrl}/api/worker/batches/{batchId}
// Header: X-Worker-Api-Key: {workerApiKey}
BatchStatusRes getBatchStatus(Long batchId, String callbackBaseUrl, String workerApiKey);

// 콜백 전송: POST {callbackBaseUrl}/api/worker/items/{itemId}/callback
// Header: X-Worker-Api-Key: {workerApiKey}
// Body: ItemCallbackReq {status, step, message}
void sendCallback(Long itemId, String status, String step, String message,
                  String callbackBaseUrl, String workerApiKey);
```

HTTP 클라이언트: `java.net.http.HttpClient` (Java 11+, 의존성 추가 불필요)
또는 `RestTemplate` (Spring Web에 포함)

---

## 5. API 통신 계약 (전체)

### CALI → 워커

```
POST http://localhost:8060/api/jobs/execute
Header: X-Worker-Api-Key: cw-api-key-dev-2026
Content-Type: application/json

{
  "batchId": 123,
  "callbackBaseUrl": "http://localhost:8050",
  "workerApiKey": "cw-api-key-dev-2026",
  "storageEndpoint": "https://kr.object.ncloudstorage.com",
  "storageBucketName": "cali-bucket",
  "storageRootDir": "dev",
  "storageAccessKey": "...",
  "storageSecretKey": "..."
}

응답: 202 Accepted
{
  "batchId": 123,
  "callbackBaseUrl": "http://localhost:8050",
  "message": "배치 작업이 접수되었습니다. 비동기로 처리를 시작합니다."
}
```

### 워커 → CALI: 배치 조회

```
GET http://localhost:8050/api/worker/batches/{batchId}
Header: X-Worker-Api-Key: cw-api-key-dev-2026

응답: ResMessage<BatchStatusRes>
{
  "code": 1,
  "msg": "배치 조회 성공",
  "data": {
    "batchId": 123,
    "jobType": "WRITE",
    "status": "READY",
    "totalCount": 3,
    "successCount": 0,
    "failCount": 0,
    "sampleId": 5,
    "items": [
      { "itemId": 1, "reportId": 101, "status": "READY", "step": null, ... },
      { "itemId": 2, "reportId": 102, "status": "READY", "step": null, ... }
    ]
  }
}
```

### 워커 → CALI: item 콜백

```
POST http://localhost:8050/api/worker/items/{itemId}/callback
Header: X-Worker-Api-Key: cw-api-key-dev-2026
Content-Type: application/json

PROGRESS 예시:
{ "status": "PROGRESS", "step": "DOWNLOADING_TEMPLATE", "message": null }

SUCCESS 예시:
{ "status": "SUCCESS", "step": "DONE", "message": "성적서작성 완료" }

FAIL 예시:
{ "status": "FAIL", "step": "DOWNLOADING_TEMPLATE", "message": "샘플 파일을 찾을 수 없습니다." }
```

---

## 6. 설계 결정 사항 (변경 금지)

### 6-1. 멀티 테넌트 설계

cali, bada-cali, sujeong-cali 등 n개 CALI 서버가 같은 워커를 사용 가능.
스토리지 접속정보와 callbackBaseUrl은 **트리거 요청마다 페이로드에 포함**되어 전달.
워커는 DB나 설정파일에 테넌트별 정보를 저장하지 않음.

### 6-2. 인증 방식

- **워커 인바운드 (CALI → 워커)**: `X-Worker-Api-Key` 헤더, `app.inbound-api-key` 값으로 검증
- **워커 아웃바운드 (워커 → CALI)**: `X-Worker-Api-Key` 헤더, 트리거 페이로드의 `workerApiKey`로 검증
- 두 키는 같은 값(`cw-api-key-dev-2026`)을 공유해도 되고 달라도 됨 (논리적으로는 별개)
- 키 값이 비어있으면 개발 모드 (검증 생략)

### 6-3. 트랜잭션 원칙

- 개별 성적서(item) 단위로 독립 처리 — 1건 실패해도 나머지 계속 진행
- 배치 전체를 하나의 트랜잭션으로 묶지 않음

### 6-4. 브라우저 독립성

- 브라우저를 닫아도 서버 작업은 계속 진행
- 나중에 다시 접속하면 배치 상태 조회 가능
- 전체 화면 로딩 락 방식 지양 → 관련 버튼만 비활성화 + 진행상황 패널 제공

### 6-5. 성적서작성 후 file_info 생성 순서

1. 파일 생성
2. 스토리지 업로드 성공
3. `file_info` insert (ref_table_id=reportId, ref_table_name='report', type='origin')
4. `report.write_member_id`, `write_datetime` 업데이트

### 6-6. 로컬 저장 옵션 (WSL2 전용 개발 편의)

`app.local-save-enabled=true` + `app.local-save-base-path=/mnt/c/데이터시트`
- WSL2 내부에서 Windows C드라이브에 직접 파일 저장 가능
- 홈서버/클라우드 배포 시 반드시 `false` 유지

---

## 7. build.gradle 의존성

```groovy
// Spring Boot Web, Validation, Actuator, Swagger
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.3'

// Lombok
compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'

// NCP 오브젝트 스토리지 (S3 호환, AWS SDK v2)
implementation(platform("software.amazon.awssdk:bom:2.27.21"))
implementation("software.amazon.awssdk:s3")

// Apache POI (엑셀 처리 — Phase 2-2 이후 실제 사용)
implementation 'org.apache.poi:poi-ooxml:5.3.0'
```

---

## 8. application.properties

```properties
spring.application.name=cali-worker
server.port=8060

# 인바운드 API 키 (비어있으면 개발 모드 — 검증 생략)
app.inbound-api-key=cw-api-key-dev-2026

# 엑셀 작업 임시 디렉토리 루트
# WSL2: /home/{linux-username}/cali-worker-tmp
# 운영: /opt/cali-worker/tmp
app.work-dir=/tmp/cali-worker

# 로컬 저장 옵션 (WSL2 개발용)
app.local-save-enabled=false
app.local-save-base-path=/mnt/c/데이터시트

# Actuator
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never

# 로깅
logging.level.com.bada.caliworker=DEBUG
```

---

## 9. Phase 2-2 이후 — 실제 성적서작성 구현 (예정)

Phase 2-1 스텁 통신 검증 완료 후 진행할 작업:

### 9-1. StorageService 구현

- `TriggerReq`에 담긴 스토리지 접속정보(`endpoint`, `bucket`, `rootDir`, `accessKey`, `secretKey`)로 S3Client 동적 생성
- 샘플 파일 다운로드: `GET {storageRootDir}/sample/{sampleId}/{filename}`
- 완성 파일 업로드: `PUT {storageRootDir}/report/{reportId}/report_origin.xlsx`

### 9-2. ExcelService 구현 (Apache POI)

- 샘플 파일 오픈 (`XSSFWorkbook`)
- `데이터시트` 시트에 성적서별 데이터 삽입 (신청업체, 발행처, 실무자, 기술책임자, 온도, 습도 등)
- 완성 파일 임시 저장 경로: `{app.work-dir}/{UUID}/report_origin.xlsx`
- 작업 후 임시 디렉토리 삭제

### 9-3. 데이터시트 시트 구조

- 데이터시트 구조는 `docs/claude/claudeReqSheetSetting.md` 참조
- 셀 위치(행/열 번호)는 별도 기획 문서 확인 필요

### 9-4. 후처리 (성공 시)

CALI에 SUCCESS 콜백 시 `file_info` 생성과 `report.write_member_id` 업데이트는
CALI 서버의 `ReportJobBatchServiceImpl.handleItemCallback()` 에서 처리됨.
워커는 콜백만 보내면 됨.

---

## 10. WSL2 배포 및 로컬 테스트 방법

```bash
# 1. cali-worker 빌드 (Windows에서)
cd C:\BadaDev\cali-worker
.\gradlew.bat clean bootJar

# 2. JAR을 WSL2로 복사
cp build/libs/cali-worker-0.0.1-SNAPSHOT.jar /경로/

# 3. WSL2에서 실행
java -jar cali-worker-0.0.1-SNAPSHOT.jar \
  --app.inbound-api-key=cw-api-key-dev-2026 \
  --app.work-dir=/home/{linux계정}/cali-worker-tmp

# 4. 헬스체크
curl http://localhost:8060/actuator/health

# 5. CALI application-dev.properties 설정 (gitignore 대상)
app.worker.url=http://localhost:8060
app.worker.api-key=cw-api-key-dev-2026
app.cali.callback-base-url=http://localhost:8050

# 6. CALI 서버 실행 후 reportWrite 화면에서 성적서 선택 → 성적서작성 클릭
# → Polling으로 진행상황 확인
```

---

## 11. 다음 세션 시작 시 Claude Code에게 전달할 지시

```
이 파일(claudeReqWorker.md)과 CLAUDE.md 를 읽어줘.
cali-worker 프로젝트가 C:\BadaDev\cali-worker\ 에 있어.
지금 아직 생성 안 된 파일들이 있거든:
  - gradlew, gradlew.bat, gradle\wrapper\gradle-wrapper.jar
  - CLAUDE.md
  - api/JobController.java
  - client/CaliApiClient.java
  - config/ApiKeyInterceptor.java, WebConfig.java, AsyncConfig.java
  - service/JobServiceImpl.java

일단 Phase 2-1 (통신 테스트 스텁) 목표로 위 파일들을 생성해줘.
실제 엑셀 작업 없이, 워커가 CALI의 배치 목록을 조회하고
각 item에 PROGRESS → SUCCESS 콜백을 보내는 스텁 구현이 목표야.
JobServiceImpl은 실제 엑셀 처리 없이 딜레이(500ms)만 넣어서 흐름만 시뮬레이션해줘.
```

---

## 12. 참고 문서 (cali 프로젝트 내)

| 경로 | 내용 |
|---|---|
| `docs/claude/claudeReq.md` | 실무자결재 1차/2차 작업 요청서 + 성적서작성/결재 설계 전체 검토 |
| `docs/claude/claudeReqSheetSetting.md` | 데이터시트 엑셀 시트 구조 |
| `docs/claude/claudeReqSample.md` | 샘플관리 작업 요청서 |
| `docs/review/` | 설계 검토 학습 문서 모음 |
| `docs/db/schema.sql` | DB 전체 스키마 |
| `docs/db/versions/` | DB 버전별 델타 파일 |
| `CLAUDE.md` | Claude Code 공통 작업 규칙 (CALI 전체) |