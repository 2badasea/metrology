# 성적서 워커 애플리케이션 구현 전 실전 학습 문서
## Part D — 최종 요약 / 우선순위 TOP 10 / 체크리스트 / 구현 순서 / 흔한 오해

---

# 전체 아키텍처 최종 다이어그램

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        성적서 작업 시스템 전체 구조                             │
└──────────────────────────────────────────────────────────────────────────────┘

[Windows PC]                         [WSL2 / Ubuntu]
┌─────────────────────────────┐      ┌───────────────────────────────────────┐
│          CALI (메인앱)        │      │          cali-worker (작업서버)         │
│  Spring Boot 3.5.x          │      │  Spring Boot 3.x                      │
│  Port: 8050                  │      │  Port: 8060                           │
│                              │      │                                       │
│  [브라우저 → REST API]        │      │  POST /api/jobs/execute               │
│  POST /api/report/jobs/batches│─────▶│  ← 트리거 수신                        │
│                              │      │                                       │
│  GET /api/report/jobs/batches│◀─────│  GET /api/worker/batches/{id}         │
│  ← 배치 조회 (작업서버용)      │      │  → 작업서버가 item 목록 조회            │
│                              │      │                                       │
│  POST /api/worker/items/{id}/│◀─────│  POST callback 전송                   │
│  callback ← 콜백 수신         │      │  → 처리 단계마다                       │
│                              │      │                                       │
│  GET /api/report/jobs/batches│      │  [처리 흐름]                           │
│  ← 브라우저 Polling           │      │  1. 배치 정보 조회                      │
│                              │      │  2. 샘플 파일 다운로드                  │
│  [DB: MySQL]                 │      │  3. Apache POI 데이터 삽입             │
│  report_job_batch            │      │  4. origin.xlsx 생성                  │
│  report_job_item             │      │  5. 스토리지 업로드                     │
│  report                      │      │  6. CALI 콜백 전송                    │
│  file_info                   │      │                                       │
└─────────────────────────────┘      └───────────────────────────────────────┘
          │                                        │
          └────────────────────────────────────────┘
                                │
                    [NCP Object Storage]
                    dev/report/{reportId}/report_origin.xlsx
                    dev/sample/{sampleId}/sample.xlsx
                    dev/members/{memberId}/signature.png
```

---

# 최종 상태 전이 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                    report 상태 컬럼 3종                            │
└─────────────────────────────────────────────────────────────────┘

write_status:
  IDLE ──[배치생성]──▶ PROGRESS ──[SUCCESS콜백]──▶ SUCCESS
                                 └──[FAIL콜백]────▶ FAIL

work_status:
  IDLE ──[실무자결재시작]──▶ READY ──[처리중]──▶ PROGRESS ──▶ SUCCESS
                                                            └──▶ FAIL

approval_status:
  IDLE ──[기술책임자결재시작]──▶ READY ──[처리중]──▶ PROGRESS ──▶ SUCCESS
                                                                └──▶ FAIL

batch.status:
  READY ──[첫PROGRESS콜백]──▶ PROGRESS ──[전체완료]──▶ SUCCESS (0 FAIL)
                                                    └──▶ FAIL (1건+FAIL)

item.status:
  READY ──[처리시작]──▶ PROGRESS ──[성공]──▶ SUCCESS
                                 └──[실패]──▶ FAIL
  READY ──[트리거실패]──▶ CANCELED
```

---

# 최종 장애 상황별 복구 흐름

## 장애 1: 트리거 전송 실패

```
흐름:
  CALI: batch + items INSERT 완료
  CALI: cali-worker에 POST /api/jobs/execute 시도
  CALI: 연결 실패 (cali-worker 미구동)

복구:
  CALI: batch.status = FAIL
  CALI: 모든 item.status = CANCELED
  CALI: 모든 report.write_status = IDLE (원상복구)
  UI: 사용자에게 "작업서버에 연결할 수 없습니다" 표시
  사용자: 다시 버튼 클릭 → 새 배치 생성

주의: 트리거 실패 복구는 이미 CALI에 구현됨 (triggerWorkerServer)
```

## 장애 2: 스토리지 업로드 실패

```
흐름:
  cali-worker: POI 작업 완료 → origin.xlsx 생성
  cali-worker: 스토리지 업로드 실패 (네트워크 오류, 인증 실패 등)

복구:
  cali-worker: FAIL 콜백 전송 (message에 실패 원인 포함)
  CALI: item.status = FAIL, report.write_status = FAIL
  CALI: batch.failCount++
  사용자: 해당 성적서만 FAIL 표시
  사용자: 실패 원인 확인 후 재처리

임시 파일: 실패한 작업 디렉터리는 24시간 보존 (디버깅용)
```

## 장애 3: cali-worker 비정상 종료 (처리 중)

```
흐름:
  cali-worker: item 처리 중 OOM 또는 예기치 않은 종료
  item: PROGRESS 상태로 DB에 남음
  콜백: 전송되지 않음

복구 방법 A (수동):
  관리자: PROGRESS 상태의 item을 FAIL로 수동 업데이트
  관리자: cali-worker 재시작 후 원인 파악

복구 방법 B (자동, 미구현):
  CALI 스케줄러: 30분 이상 PROGRESS인 item을 FAIL로 처리
  → @Scheduled로 주기적 체크

복구 방법 C (cali-worker 재시작 시 클린업):
  ApplicationReadyEvent 핸들러에서 이전 PROGRESS item FAIL 처리
```

---

# 지금 바로 공부해야 할 우선순위 TOP 10

다음은 **cali-worker 구현 전 반드시 이해해야 하는** 항목들이다.
순서대로 학습해야 뒤 내용이 이해된다.

## 1위. WSL2 네트워크 구조 (1~2시간)

```
왜: cali-worker를 WSL2에 배포하면 CALI와의 통신 경로가 핵심 문제가 됨
무엇을:
  - WSL2 NAT mode vs mirrored mode 차이
  - WSL2 → Windows 방향의 callbackBaseUrl 설정
  - localhost vs Windows IP 차이
확인 포인트:
  ip route show (WSL2에서 Windows IP 확인)
  mirrored mode 설정 (~/.wslconfig)
```

## 2위. Spring Boot JAR 배포와 외부 설정파일 (1시간)

```
왜: cali-worker 빌드 후 WSL2에 배포하는 실제 절차 이해 필요
무엇을:
  - ./gradlew bootJar 로 JAR 빌드
  - 외부 properties 파일로 민감정보 분리
  - java -jar 실행 옵션 (--spring.config.additional-location)
확인 포인트:
  WSL2 Ubuntu에서 java -version
  JDK 17 설치 여부 확인
```

## 3위. Apache POI 기본 패턴 (2~3시간)

```
왜: 핵심 기능 — 샘플 파일 열고 데이터 채우기
무엇을:
  - XSSFWorkbook으로 기존 파일 열기
  - Sheet, Row, Cell 접근 패턴
  - 셀 값 읽기/쓰기
  - try-with-resources로 닫기
  - 이미지 삽입 (서명)
실습:
  sample.xlsx 파일 열어서 특정 셀에 값 넣고 저장하기
```

## 4위. NCP Object Storage 연동 (1~2시간)

```
왜: 샘플 파일 다운로드 + 결과 파일 업로드가 핵심 I/O
무엇을:
  - AWS SDK v2 S3Client 설정
  - S3 호환 엔드포인트 설정
  - 파일 업로드/다운로드 코드
확인 포인트:
  CALI의 기존 ObjectStorageService 코드 분석
  동일 패턴으로 cali-worker 구현 가능
```

## 5위. HTTP 클라이언트 (RestClient) 사용법 (1시간)

```
왜: CALI API 호출 (배치 조회, 콜백 전송)이 워커의 주요 동작
무엇을:
  - Spring RestClient 기본 사용법
  - POST with JSON body
  - GET with path variable
  - 헤더 설정 (X-Worker-Api-Key)
  - 에러 응답 처리
```

## 6위. item별 독립 처리 구조 (1시간)

```
왜: 설계의 핵심 — 1건 실패가 전체 실패로 이어지지 않게
무엇을:
  - for loop + try/catch로 item별 독립 처리
  - 실패 시 FAIL 콜백 전송 후 다음 item 진행
  - batch 전체 완료 판단 로직
```

## 7위. systemd 서비스 등록 (30분)

```
왜: WSL2에서 JAR를 안정적으로 실행하기 위한 운영 기반
무엇을:
  - /etc/systemd/system/cali-worker.service 작성
  - daemon-reload, start, enable, status 명령어
  - journalctl 로그 확인
전제: WSL2에서 systemd 활성화 (wsl.conf 설정)
```

## 8위. LibreOffice headless PDF 변환 (1~2시간)

```
왜: 결재 처리 시 PDF 생성 필수
무엇을:
  - libreoffice --headless --convert-to pdf 명령어
  - Java ProcessBuilder로 외부 명령 실행
  - 한글 폰트 설치 확인
  - 동시 실행 시 프로파일 분리
실습:
  WSL2에서 libreoffice 설치 후 test.xlsx → test.pdf 변환 확인
```

## 9위. 스토리지 업로드 후 DB 반영 순서 (30분)

```
왜: 데이터 정합성의 핵심 — 순서가 틀리면 orphan 파일 또는 메타데이터 발생
무엇을:
  업로드 성공 → CALI 콜백(SUCCESS) → CALI가 DB 갱신
  업로드 실패 → CALI 콜백(FAIL) → CALI가 FAIL 상태 기록
```

## 10위. 로깅과 batchId/itemId 추적 (30분)

```
왜: 장애 발생 시 빠른 원인 파악을 위한 기반
무엇을:
  - MDC로 batchId/itemId 자동 포함
  - 각 처리 단계별 INFO 로그
  - 실패 시 ERROR 로그 + message 콜백 전송
```

---

# 지금은 훑기만 하고 나중에 깊게 볼 주제

## 당장 구현 불필요, 이해만 해도 충분

```
Redis / 메시지 큐
  → 단일 워커로 시작, 나중에 확장 시 검토
  → 지금은 "왜 나중에 필요한지" 정도만 파악

SSE (Server-Sent Events)
  → Polling이 먼저, SSE는 실시간성 필요 시
  → 지금은 Polling 구현에 집중

Docker / 컨테이너
  → 코드가 동작한 다음에 컨테이너화
  → 지금은 JAR 직접 배포로 충분

Kubernetes / 오케스트레이션
  → 다중 워커 확장 시, 훨씬 나중에

Presigned URL
  → 파일 다운로드 UI 개선 시
  → 지금은 CALI 서버 통해서 다운로드로 충분

Optimistic/Pessimistic Locking
  → 단일 워커, 순차 처리 시 불필요
  → 다중 워커 도입 시 검토

메트릭 / Prometheus / Grafana
  → 운영 안정화 후 모니터링 고도화 시
```

---

# 구현 전 결정해야 할 체크리스트

## 반드시 합의해야 함 (구현 전)

```
□ 1. callbackBaseUrl 설정값
     Q: WSL2에서 CALI(Windows 8050)에 접근하는 URL은?
     옵션A: mirrored mode 설정 후 localhost:8050 사용
     옵션B: ip route로 Windows IP 확인 후 고정 설정
     옵션C: host.docker.internal (Docker 사용 시)
     → 결정 필요 (WSL2 네트워크 모드에 따라)

□ 2. app.worker.work-dir (임시 작업 디렉터리 경로)
     Q: WSL2 내 어느 경로에 임시 파일을 만들 것인가?
     제안: /opt/cali-worker/work/
     → 경로 생성 및 권한 확인 필요

□ 3. report 상세 데이터 조회 API
     Q: cali-worker가 성적서 raw 데이터를 조회하는 API 경로는?
     제안: GET /api/worker/reports/{reportId}
     → CALI에 추가 구현 필요 (현재 미구현)
     → 응답에 어떤 필드를 포함할지 결정 필요

□ 4. env.sheet_info_setting JSON 구조 확정
     Q: 어떤 데이터를 어느 셀에 넣을지 정의하는 JSON 구조는?
     → 실제 sample.xlsx의 레이아웃 분석 후 결정
     → 현재 env 테이블에 컬럼 추가 필요 여부 확인

□ 5. FAIL item 임시 파일 보존 정책
     Q: 처리 실패한 item의 임시 디렉터리를 얼마나 보존?
     제안: 24시간 보존 후 스케줄러 삭제
     → 보존 기간 결정 필요

□ 6. cali-worker 포트
     Q: 8060으로 확정? 방화벽/포트포워딩 설정 필요?
     → WSL2 기준으로는 별도 설정 불필요 (내부망)
```

## 확인 가능하면 미리 확인 권장

```
□ 7. WSL2에 JDK 17 설치 여부
     java -version (WSL2 내부에서)

□ 8. WSL2에 LibreOffice 설치 여부 (결재 기능 필요 시)
     libreoffice --version (WSL2 내부에서)

□ 9. NCP 스토리지 접속정보가 외부 properties에 있는지
     (app.worker.api-key, ncp.storage.* 등)

□ 10. sample.xlsx 파일이 스토리지에 있는지
      GET /api/worker/batches/{batchId} 응답의 sampleId 값 존재 여부
```

---

# 실제 구현 단계 진입 순서 제안

## Step 1: 환경 준비 (1일)

```
1-1. WSL2 Ubuntu 기본 환경 확인
  - Java 17 설치: sudo apt install openjdk-17-jdk
  - 버전 확인: java -version

1-2. WSL2 네트워크 방식 결정 및 설정
  - mirrored mode 또는 NAT mode 결정
  - callbackBaseUrl 테스트 (WSL2에서 Windows 8050 접근 확인)
  - 예: curl http://<Windows-IP>:8050/actuator/health

1-3. 디렉터리 생성
  sudo mkdir -p /opt/cali-worker/work
  sudo mkdir -p /opt/cali-worker/logs

1-4. systemd 활성화 확인 (wsl.conf)
  cat /etc/wsl.conf
  systemctl --version
```

## Step 2: cali-worker 프로젝트 생성 (반나절)

```
2-1. Spring Boot 프로젝트 생성
  - Spring Initializr 또는 수동
  - 의존성: Spring Web, Spring Boot Actuator, Lombok, Log4j2
  - 빌드: Gradle (CALI와 동일)

2-2. 기본 설정파일 구성
  # application.properties (기본값)
  server.port=8060
  app.worker.work-dir=/opt/cali-worker/work
  app.cali.callback-base-url=http://localhost:8050  # 개발 모드 기본값

  # application-dev.properties
  app.cali.callback-base-url=http://<Windows-IP>:8050

2-3. 기본 health endpoint 확인
  ./gradlew bootRun
  curl http://localhost:8060/actuator/health → {"status":"UP"}
```

## Step 3: 트리거 수신 엔드포인트 구현 (반나절)

```
3-1. WorkerTriggerReq DTO 정의
  - batchId, callbackBaseUrl, workerApiKey
  - storageEndpoint, storageBucketName, storageRootDir, storageAccessKey, storageSecretKey

3-2. POST /api/jobs/execute 구현
  - API 키 검증
  - 즉시 202 Accepted 응답 (처리는 비동기)
  - 처리 로직 호출 (일단 log.info로 수신 확인만)

3-3. 엔드투엔드 통신 테스트
  - CALI 빌드/실행 (app.worker.url=http://localhost:8060 설정)
  - 성적서 선택 후 "성적서작성" 클릭
  - CALI: 배치 생성 → 트리거 전송
  - cali-worker 로그: "트리거 수신 batchId=XXX" 확인
  - CALI: batch.status = PROGRESS (첫 콜백 수신 후) 확인
```

## Step 4: 배치 정보 조회 + 콜백 전송 (반나절)

```
4-1. CALI API 호출 클라이언트 구현
  - RestClient로 GET /api/worker/batches/{batchId} 호출
  - 응답 파싱 (BatchStatusRes 기준)

4-2. PROGRESS 콜백 전송 구현
  - RestClient로 POST /api/worker/items/{itemId}/callback
  - {"status":"PROGRESS","step":"DOWNLOADING_TEMPLATE"}

4-3. 테스트
  - 트리거 수신 → 배치 조회 → 각 item에 PROGRESS 콜백 전송 확인
  - CALI DB: item.status = PROGRESS 확인
```

## Step 5: 스토리지 연동 (1일)

```
5-1. S3Client 설정
  - AWS SDK v2 의존성 추가 (build.gradle)
  - 트리거 페이로드의 스토리지 설정으로 S3Client 생성

5-2. 샘플 파일 다운로드 구현
  - GET {storageEndpoint}/{bucketName}/{rootDir}/sample/{sampleId}/sample.xlsx
  - 임시 작업 디렉터리에 저장

5-3. 결과 파일 업로드 구현
  - PUT {storageEndpoint}/{bucketName}/{rootDir}/report/{reportId}/report_origin.xlsx

5-4. 테스트
  - 실제 샘플 파일 다운로드 성공 확인
  - 빈 xlsx 파일 업로드 성공 확인 (POI 전)
```

## Step 6: Apache POI 데이터 삽입 (1~2일)

```
6-1. POI 의존성 추가
  # build.gradle
  implementation 'org.apache.poi:poi-ooxml:5.x.x'

6-2. 샘플 파일 열기 + 기본 값 삽입 구현
  - XSSFWorkbook으로 sample.xlsx 열기
  - 특정 셀에 하드코딩 값 삽입 후 저장 (sheet_info_setting 전에 먼저)
  - 저장된 파일 스토리지 업로드

6-3. sheet_info_setting JSON 파싱 구현
  - CALI report 상세 API 응답에서 envInfo 포함 예정
  - ObjectMapper로 JSON 파싱 → 셀 위치 매핑

6-4. 실제 성적서 데이터 삽입
  - CALI GET /api/worker/reports/{reportId} 호출 (구현 필요)
  - 응답 데이터를 sheet_info_setting 기반으로 삽입
```

## Step 7: SUCCESS/FAIL 콜백 완성 + 테스트 (반나절)

```
7-1. SUCCESS 콜백 전송
  - 스토리지 업로드 성공 후 전송
  - storedPath를 콜백 body에 포함 (file_info 생성 위해)

7-2. FAIL 콜백 전송 개선
  - 각 단계별 실패 원인 message 포함

7-3. 전체 흐름 테스트
  - 실제 성적서 3건 선택 → 성적서작성 클릭
  - CALI batch = SUCCESS 확인
  - 스토리지에 origin.xlsx 생성 확인
  - report.write_status = SUCCESS 확인
  - 브라우저 Polling에서 완료 표시 확인
```

## Step 8: WSL2 → systemd 배포 (반나절)

```
8-1. JAR 빌드 및 WSL2 복사
  # Windows
  .\gradlew.bat bootJar

  # WSL2
  cp /mnt/c/BadaDev/cali-worker/build/libs/cali-worker-0.0.1-SNAPSHOT.jar \
     /opt/cali-worker/cali-worker.jar

8-2. 외부 설정파일 작성
  nano /opt/cali-worker/application-prod.properties
  # app.cali.callback-base-url=http://<Windows-IP>:8050
  # ncp.storage.access-key=...
  # ncp.storage.secret-key=...
  # app.worker.api-key=cali-worker-key-dev

8-3. systemd 서비스 등록
  sudo nano /etc/systemd/system/cali-worker.service
  sudo systemctl daemon-reload
  sudo systemctl start cali-worker
  sudo systemctl status cali-worker
  journalctl -u cali-worker -f
```

---

# 흔한 오해 / 잘못된 접근 방식 정리

## 오해 1: "CALI에서 직접 엑셀 파일을 만들면 되지 않나?"

```
잘못된 이해:
  CALI API가 엑셀 파일을 생성해서 바로 응답으로 내려주면 간단하다.

왜 안 되는가:
  1. 파일 생성 시간: 성적서 20건 처리 = 수분 소요 → HTTP 타임아웃
  2. 서버 자원: 파일 처리는 CPU/메모리 집약적 → CALI 전체 성능 저하
  3. 브라우저 종료 시 작업 중단: 요청-응답이 끊기면 처리 중단
  4. 부분 실패 처리 불가: 20건 중 1건 실패 시 전체 응답 실패

올바른 이해:
  파일 처리는 별도 서버(cali-worker)에서 비동기로 처리.
  CALI는 상태 관리 + 트리거 발송 + 결과 수신만.
```

## 오해 2: "WSL2에서는 localhost로 Windows에 접근하면 된다"

```
잘못된 이해:
  WSL2에서 localhost:8050으로 CALI에 접근 가능하다.

실제 동작:
  NAT mode: WSL2 → Windows 방향은 localhost가 작동 안 할 수 있음
  Windows → WSL2 방향은 localhost 가능 (자동 포워딩)
  WSL2 → Windows 방향은 Windows IP 필요

올바른 접근:
  mirrored mode 설정: ~/.wslconfig에 networkingMode=mirrored
  또는 ip route show로 Windows IP 확인 후 설정
  또는 /etc/resolv.conf의 nameserver IP 사용
```

## 오해 3: "item 처리 실패 시 배치 전체를 롤백해야 한다"

```
잘못된 이해:
  1건이라도 실패하면 @Transactional 롤백으로 전체를 되돌려야 한다.

왜 안 되는가:
  이미 성공한 10건의 처리 결과가 모두 사라짐.
  스토리지에 업로드된 파일은 DB 롤백으로 삭제되지 않음 → 불일치.

올바른 접근:
  item 1건 = 트랜잭션 1건.
  실패한 item만 FAIL 상태로 기록.
  다른 item은 계속 처리.
  batch.status = FAIL (1건이라도 실패 시).
```

## 오해 4: "트리거 API가 완료될 때까지 기다려야 한다"

```
잘못된 이해:
  cali-worker가 모든 처리를 완료하고 나서 CALI에 응답해야 한다.

왜 안 되는가:
  처리가 수분 걸린다면 HTTP 타임아웃 발생.
  CALI의 RestClient가 응답 대기 중 → 스레드 점유.

올바른 접근:
  cali-worker: 트리거 수신 즉시 202 Accepted 응답.
  실제 처리는 별도 스레드에서 비동기 실행.
  처리 결과는 콜백 API로 전송.
```

## 오해 5: "sheet_info_setting JSON을 코드에 하드코딩하면 된다"

```
잘못된 이해:
  셀 위치를 코드에 직접 쓰면 빠르고 간단하다.

왜 문제인가:
  샘플 파일의 레이아웃이 바뀌면 코드를 수정하고 재배포해야 함.
  소분류마다 다른 레이아웃을 사용하면 코드가 복잡해짐.

올바른 접근:
  env.sheet_info_setting JSON으로 셀 위치 외부화.
  코드는 JSON을 파싱해서 매핑 적용.
  레이아웃 변경 = JSON만 수정 (재배포 불필요).
```

## 오해 6: "작업서버와 CALI가 같은 DB를 써야 한다"

```
잘못된 이해:
  cali-worker도 CALI 같은 MySQL에 직접 접근해서 상태를 업데이트하면 된다.

왜 문제인가:
  DB 직접 접근은 보안 구멍 (DB 포트 외부 노출 필요).
  나중에 cali-worker가 다른 서버로 분리될 때 DB 접근 불가.
  트랜잭션 경계가 복잡해짐.
  CALI의 비즈니스 로직이 외부에서 우회됨.

올바른 접근:
  cali-worker는 CALI REST API를 통해서만 상태 변경.
  CALI가 유일한 DB 소유자.
  cali-worker는 HTTP 클라이언트 역할만.
```

## 오해 7: "성공 콜백을 보내면 CALI가 자동으로 file_info를 생성한다"

```
현재 구현 확인 필요:
  handleItemCallback에서 SUCCESS 처리 시 report 상태만 갱신.
  file_info 생성은 별도 구현 필요.

확인 포인트:
  storedPath를 콜백 페이로드에 포함해서 전달?
  또는 cali-worker가 별도 file_info 생성 API 호출?
  → 구현 전 CALI 팀(본인)과 합의 필요
```

---

# 핵심 설계 원칙 요약 (변하지 않는 것)

```
┌─────────────────────────────────────────────────────────────────┐
│  나중에 Redis, SSE, Docker, 클라우드를 붙여도 유지되는 핵심 설계  │
└─────────────────────────────────────────────────────────────────┘

1. 비동기 분리
   브라우저 요청-응답 ≠ 파일 처리 작업
   언제나 별도 서버, 별도 스레드, 비동기

2. batch / item 구조
   사용자 액션 → batch 1건
   개별 성적서 → item n건
   이 구조는 어떤 큐/메시지 브로커로 교체해도 유지

3. item 단위 트랜잭션
   1건 실패 ≠ 전체 실패
   각 item 처리 = 독립 트랜잭션

4. 상태 머신
   IDLE → PROGRESS → SUCCESS/FAIL
   write_status / work_status / approval_status 동일 패턴
   앱이 어떻게 바뀌어도 이 상태 컬럼은 유지

5. 파일과 메타데이터 분리
   스토리지 = 파일 저장
   DB(file_info) = 메타데이터 저장
   항상 스토리지 업로드 성공 후 DB 반영

6. 민감정보는 코드/DB 비포함
   API 키, 스토리지 키 등은 외부 properties
   트리거 페이로드로 전달 시 메모리에서 메모리로만
```

---

# 전체 학습 문서 목차 요약

| 파일 | 내용 |
|---|---|
| PartA | 아키텍처 전체 / WSL2 개념 / 네트워크 구조 / Linux 기초 / Java 운영 / 통신 설계 |
| PartB | Polling vs SSE / 배치·상태머신 / 트랜잭션·동시성 / 풀링(HikariCP) / Apache POI / PDF 변환 |
| PartC | 파일 저장·버전관리 / 스토리지 연동 / 멀티테넌시 / Redis·큐 / 운영·로깅 / 확장 전략 |
| PartD (이 문서) | 최종 요약 / 우선순위 TOP 10 / 구현 전 체크리스트 / 구현 순서 / 흔한 오해 |

---

> 이 학습 문서는 cali-worker 구현 시작 전 기술 전체 지형 파악을 목적으로 작성되었다.
> 실제 구현 시에는 CALI의 기존 코드 패턴(ObjectStorageService, RestClient 사용법 등)을 먼저 분석하고,
> 동일한 스타일로 cali-worker를 구성하는 것을 권장한다.
