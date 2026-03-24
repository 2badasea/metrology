# cali-worker 프로젝트 — 작업 요청서 및 진행 현황

> 이 문서는 CALI 연동 워커 서버(`cali-worker-dev`)의 설계, 구현 현황, 다음 작업 목록을 정리한 **컨텍스트 요약서**입니다.
> 새 세션에서 작업 재개 시 이 문서를 먼저 읽힌 뒤 지시하세요.

---

## 0. 전체 흐름 요약

```
[브라우저 (SSR/Thymeleaf)]
      ↓  성적서 작성/결재 버튼 클릭
[CALI 서버 :8050]
  POST /api/report/jobs/batches
      → report_job_batch + report_job_item(n건) 생성 (READY 상태)
      → 워커 트리거 POST http://{workerUrl}/api/jobs/execute
      → 202 즉시 반환

[cali-worker-dev 서버 :8060]  ← ★ 이 프로젝트
  POST /api/jobs/execute 수신 → 202 반환 (즉시)
      ↓  비동기 처리 (@Async)
  GET  {callbackBaseUrl}/api/worker/batches/{batchId}     (item 목록 조회)
  각 item 처리:
    POST {callbackBaseUrl}/api/worker/items/{itemId}/callback  (PROGRESS 단계별)
    — 스토리지에서 파일 다운로드
    — 엑셀 처리 (데이터 삽입 or 서명 삽입)
    — PDF 변환 (reportLang에 따라 특정 시트만)
    — 완성 파일 스토리지 업로드
    POST {callbackBaseUrl}/api/worker/items/{itemId}/callback  (SUCCESS / FAIL)

[브라우저]
  GET /api/report/jobs/batches/{batchId}  (1~2초 Polling)
      → 진행상황 표시 (progress bar + item별 상태)
```

---

## 1. 두 프로젝트 개요

| 항목 | CALI (본체) | cali-worker-dev |
|---|---|---|
| 경로 | `C:\BadaDev\cali\backend` | `C:\BadaDev\cali-worker-dev` |
| 포트 | 8050 | 8060 |
| 패키지 | `com.bada.cali` | `com.bada.caliworker` |
| 역할 | 교정관리 웹앱 (Spring Boot + Thymeleaf) | 성적서 작업 워커 (독립 Spring Boot) |
| Spring Boot | 3.5.x | 3.5.7 |
| Java | 17 | 17 |

---

## 2. 구현 완료 현황 (2026-03 기준)

### 2-1. cali-worker-dev 파일 구조 (전체 구현 완료)

```
src/main/java/com/bada/caliworker/
├── CaliWorkerApplication.java
├── api/
│   └── JobController.java              — POST /api/jobs/execute → 202 즉시 반환
├── client/
│   └── CaliApiClient.java              — CALI API 호출 래퍼 (배치조회/콜백/sheetSetting/fillData)
├── config/
│   ├── ApiKeyInterceptor.java          — X-Worker-Api-Key 헤더 검증
│   ├── WebConfig.java                  — 인터셉터 등록
│   └── AsyncConfig.java                — @EnableAsync 스레드풀 (core=2, max=5, queue=100)
├── dto/
│   ├── JobDTO.java                     — TriggerReq/Res, BatchStatusRes, ItemStatusRes 등
│   ├── ResMessage.java                 — CALI API 응답 래퍼
│   └── WorkerDataDTO.java              — SheetSettingRes, ReportFillDataRes
└── service/
    ├── JobServiceImpl.java             — 배치 비동기 처리 핵심 (WRITE / WORK_APPROVAL)
    ├── StorageService.java             — S3 다운로드/업로드
    └── ExcelService.java               — Apache POI 기반 (데이터 삽입 + 서명 삽입 + PDF 변환)
```

### 2-2. 구현 완료된 jobType

| jobType | 단계 | 상태 |
|---|---|---|
| WRITE (성적서작성) | DOWNLOADING_TEMPLATE → FILLING_DATA → UPLOADING_ORIGIN → DONE | ✅ 완료 |
| WORK_APPROVAL (실무자결재) | DOWNLOADING_ORIGIN → INSERTING_SIGN → CONVERTING_PDF → UPLOADING_SIGNED → DONE | ✅ 구현 완료 (PDF 품질 이슈 있음) |
| MANAGER_APPROVAL | — | 🔲 미구현 |

### 2-3. CALI 측 구현 완료 항목

**DB 테이블:**
- `report_job_batch`: 배치 단위 (jobType, status, totalCount, successCount 등)
- `report_job_item`: 개별 성적서 단위 (status, step, message, retry_count)

**API:**
- `POST /api/report/jobs/batches` — 배치 생성 + 워커 트리거 (WRITE / WORK_APPROVAL 공용)
- `GET /api/report/jobs/batches/{batchId}` — 폴링용 진행상황 조회
- `GET /api/worker/batches/{batchId}` — 워커용 배치+item 목록 조회
- `POST /api/worker/items/{itemId}/callback` — item 콜백 수신
- `GET /api/worker/env/sheet-setting` — 시트설정 조회 (WRITE용)
- `GET /api/worker/reports/{id}/fill-data` — 삽입 데이터 조회 (WRITE/WORK_APPROVAL 공용)
- `GET /api/file/report/{reportId}/{fileType}` — 성적서 파일 다운로드 (origin/signed_xlsx/signed_pdf)

**프론트엔드:**
- `workApproval.js`: 결재 버튼 + 폴링 UI + 다중결재(workMemberId별 그룹핑) + 파일 다운로드
- `reportWrite.js`: 성적서작성 버튼 + 폴링 UI + 완료 시 workApproval 그리드 자동 리로드

---

## 3. S3 파일 저장 경로 규칙

| 파일 종류 | S3 objectKey |
|---|---|
| 성적서 원본 (작성 완료) | `{rootDir}/report/{reportId}/origin.xlsx` |
| 결재 완료 EXCEL | `{rootDir}/report/{reportId}/signed.xlsx` |
| 결재 완료 PDF | `{rootDir}/report/{reportId}/signed.pdf` |
| 서명 이미지 | `file_info.stored_path` 참조 (member 기준으로 조회) |

---

## 4. WORK_APPROVAL 설계 확정 사항

### 4-1. 서명 이미지 삽입

- 워커가 origin.xlsx 모든 시트에서 **shape 이름** 기준으로 삽입
  - WORK_APPROVAL: `writer_sign` shape → 실무자 서명 이미지
  - MANAGER_APPROVAL: `manager_sign` shape → 기술책임자 서명 이미지
- 같은 이름의 shape가 여러 시트에 있으면 **모두** 삽입
- 서명 이미지 objectKey: CALI가 트리거 시 `TriggerReq.workerSignImgKey` 필드에 포함
  - CALI에서 `file_info(ref_table_name='member', ref_table_id=workMemberId, is_visible='y')`로 조회

### 4-2. PDF 변환 시트 정책

| report_lang | 변환할 시트 (시트명 포함 문자열 기준) |
|---|---|
| KR | `(국문)` 포함 시트만 |
| EN | `(영문)` 포함 시트만 |
| BOTH | `(국문)` + `(영문)` 포함 시트 모두 |

- `데이터` 시트는 **항상 제외** (단, 해당 시트의 raw데이터를 참조하는 시트는 데이터가 채워진 상태로 출력되어야 함)
- 여러 시트 → 하나의 합본 PDF (`signed.pdf`)

### 4-3. TriggerReq 주요 필드

```java
// JobDTO.TriggerReq
Long   batchId
String callbackBaseUrl
String workerApiKey
String storageEndpoint / storageBucketName / storageRootDir / storageAccessKey / storageSecretKey
String workerSignImgKey  // WORK_APPROVAL 전용: 서명 이미지 objectKey
```

---

## 5. 이번 세션에서 수정된 버그 (2026-03-22)

### 5-1. 서명 이미지 조회 오류 (CALI - ReportJobBatchServiceImpl)

**증상:** `IllegalArgumentException: 실무자 서명 이미지가 등록되어 있지 않습니다.`
**원인:** 서명 이미지 조회 시 `user.getId()` (세션 사용자)를 사용 → 성적서의 `workMemberId`와 불일치
**수정:**
- 배치 내 모든 성적서의 `workMemberId`가 동일한지 검증
- `batchWorkMemberId = report.getWorkMemberId()`를 기준으로 서명 이미지 조회
- `workMemberId`가 null인 성적서 포함 시 예외

### 5-2. workMemberId 덮어쓰기 오류 (CALI - ReportJobBatchServiceImpl)

**증상:** WORK_APPROVAL 결재 완료 후 성적서의 `work_member_id`가 결재를 요청한 세션 유저 ID로 변경됨
**원인:** SUCCESS 콜백 처리 시 `report.setWorkMemberId(batch.getRequestMemberId())` 잘못된 코드 존재
**수정:** 해당 라인 제거. `workMemberId`는 성적서에 원래 지정된 실무자 — 결재 처리로 변경 불가

### 5-3. 다중결재 시 서로 다른 실무자 처리 오류

**증상:** 서로 다른 `workMemberId`를 가진 성적서들을 한 번에 결재 요청하면 백엔드 예외 발생
**원인:** 백엔드는 배치 내 `workMemberId`가 모두 동일해야 한다는 제약 → 프론트에서 분리 없이 전송
**수정 (workApproval.js):**
- 다중결재 시 `workMemberId`별로 그룹핑
- 그룹별로 각각 `doWorkApproval()` 순차 호출

### 5-4. 시트 필터링 로직 오류 (cali-worker - ExcelService)

**원인:** `contains("EN")`으로 영문 시트 필터링 → 한글 시트명 `(영문)` 미매칭
**수정:** `contains("(국문)")` / `contains("(영문)")` 으로 변경

---

## 6. 현재 미해결 이슈 — PDF 변환 품질

### 6-1. 현재 상태

- `ExcelService.convertToPdf()`: LibreOffice headless 사용 중
- `ExcelService.prepareXlsxForConvert()`: `setSheetHidden()`으로 시트 숨김 처리
  - **문제**: LibreOffice는 `setSheetHidden()` 상태를 무시하고 모든 시트를 PDF로 변환
  - → `데이터` 시트가 PDF에 계속 포함됨
- **품질 문제**: LibreOffice는 Excel 인쇄 설정(인쇄 영역, 레이아웃)을 완벽 재현하지 못함
  - 일부 영역 잘림, 머리글/꼬리글 추가 표시, 레이아웃 틀어짐

### 6-2. 검토된 대안

| 방법 | 품질 | 비용 | 병렬처리 | 결론 |
|---|---|---|---|---|
| LibreOffice | ★★☆☆☆ | 무료 | ✅ | 현재 사용 중. 품질 불허용 수준 |
| Aspose.Cells for Java (Perpetual OEM) | ★★★★★ | ~540만원 일시불 | ✅ | 고품질, 비용 부담 있음 |
| Spire.XLS for Java (Perpetual OEM) | ★★★★☆ | ~83만원 일시불 | ✅ | 테스트 필요 |
| Windows Server + Excel COM | ★★★★★ | VM+라이선스 비용 | ❌ | GUI 다이얼로그 hung, 병렬 불가 → 제외 |

### 6-3. 결정된 방향

**Spire.XLS 평가 먼저** → 품질 수용 가능하면 채택, 아니면 Aspose.Cells로 상향

**Spire.XLS 핵심 동작 (테스트 예정):**
```java
Workbook wb = new Workbook();
wb.loadFromFile("signed.xlsx");
// 대상 외 시트 뒤에서부터 제거 (데이터 시트 포함)
for (int i = wb.getWorksheets().getCount() - 1; i >= 0; i--) {
    String name = wb.getWorksheets().get(i).getName();
    boolean keep = (lang.equals("KR") && name.contains("(국문)"))
                || (lang.equals("EN") && name.contains("(영문)"))
                || (lang.equals("BOTH") && (name.contains("(국문)") || name.contains("(영문)")));
    if (!keep) wb.getWorksheets().removeAt(i);
}
wb.saveToFile("output.pdf", FileFormat.PDF);
wb.dispose();
```

- 시트 제거 전 수식 계산은 Spire.XLS가 내부적으로 처리 (명시적 `calculateFormula()` 필요 여부 테스트)
- 스레드별 독립 Workbook 인스턴스 → 병렬 처리 가능

---

## 7. 다음 작업 목록

### 7-1. [우선] Spire.XLS 평가판 테스트

```groovy
// cali-worker-dev/build.gradle 추가
repositories {
    maven { url 'https://repo.e-iceblue.com/nexus/content/groups/public/' }
}
dependencies {
    // 유료 평가 모드 (워터마크 있음, 기능 제한 없음)
    implementation 'e-iceblue:spire.xls:14.x.x'
}
```

1. 실제 `signed.xlsx`로 변환 테스트
2. 레이아웃 / 데이터 값 / 시트 선택 품질 확인
3. 수용 가능 → 채택 결정 + `ExcelService.convertToPdf()` 교체

### 7-2. ExcelService PDF 변환 교체

- `prepareXlsxForConvert()`: LibreOffice용 시트 숨김 로직 → 불필요 (Spire가 직접 처리)
- `convertToPdf()`: LibreOffice 프로세스 호출 → Spire.XLS `saveToFile(pdf)` 로 교체
- `app.libreoffice-path` 설정값 → `app.pdf-engine` 등으로 변경 또는 제거

### 7-3. MANAGER_APPROVAL 구현

실무자결재와 동일한 파이프라인. 차이점:
- shape 이름: `manager_sign`
- `TriggerReq` 필드: `workerManagerSignImgKey` (기술책임자 서명 이미지)
- `report.manager_status` / `manager_datetime` 업데이트

### 7-4. 재시도 / 실패 복구

- 배치 FAIL 시 재시도 버튼 (현재 없음)
- `retry_count` 컬럼 활용

---

## 8. 설계 확정 사항 (변경 금지)

### 8-1. 멀티 테넌트

n개 CALI 서버가 같은 워커를 공유 가능. 스토리지 접속정보와 callbackBaseUrl은 **트리거 요청 페이로드에 포함**.
워커는 DB/설정파일에 테넌트별 정보 저장 안 함.

### 8-2. 인증 방식

- 워커 인바운드 (CALI → 워커): `X-Worker-Api-Key` 헤더, `app.inbound-api-key` 값으로 검증
- 워커 아웃바운드 (워커 → CALI): `X-Worker-Api-Key` 헤더, 트리거 페이로드의 `workerApiKey`로 검증
- 키가 비어있으면 개발 모드 (검증 생략)

### 8-3. 처리 원칙

- item 단위 독립 처리 — 1건 실패해도 나머지 계속 진행
- 브라우저 종료와 무관하게 서버 작업 계속 진행 (비동기)
- 동일 `workMemberId` 성적서만 하나의 배치로 처리 가능

### 8-4. 로컬 개발 설정 (`application-dev.properties` — gitignore 대상)

```properties
# CALI 측
app.worker.url=http://localhost:8060
app.worker.api-key=cw-api-key-dev-2026
app.cali.callback-base-url=http://localhost:8050

# 워커 측
app.inbound-api-key=cw-api-key-dev-2026
app.work-dir=/tmp/cali-worker
app.libreoffice-path=          # 비워두면 PDF 변환 생략 (개발 모드)
```

---

## 9. API 통신 계약

### CALI → 워커 트리거

```json
POST /api/jobs/execute
Header: X-Worker-Api-Key: {apiKey}

{
  "batchId": 123,
  "callbackBaseUrl": "http://localhost:8050",
  "workerApiKey": "cw-api-key-dev-2026",
  "storageEndpoint": "https://kr.object.ncloudstorage.com",
  "storageBucketName": "cali-bucket",
  "storageRootDir": "dev",
  "storageAccessKey": "...",
  "storageSecretKey": "...",
  "workerSignImgKey": "dev/member/8/sign.png"   // WORK_APPROVAL 전용
}
→ 202 Accepted
```

### 워커 → CALI 콜백

```json
POST {callbackBaseUrl}/api/worker/items/{itemId}/callback
Header: X-Worker-Api-Key: {apiKey}

PROGRESS: { "status": "PROGRESS", "step": "INSERTING_SIGN",  "message": null }
SUCCESS:  { "status": "SUCCESS",  "step": "DONE",            "message": null }
FAIL:     { "status": "FAIL",     "step": "CONVERTING_PDF",  "message": "오류 내용" }
```

---

## 11. ⚠️ 전략 변경 — cali-worker 추가 개발 중단 (2026-03-23)

### 11-1. 결정 배경

이번 세션에서 Spire.XLS 기반 PDF 변환을 실제 성적서로 테스트한 결과:

| 이슈 | 내용 | 해결 여부 |
|---|---|---|
| 특정 시트만 PDF 변환 | 2단계 파이프라인(POI → Spire.XLS)으로 해결 | ✅ |
| 서명 이미지 개체인쇄 누락 | `XmlCursor`로 `printed="1"` 강제 설정 | ✅ |
| fit-to-page 미적용 → 페이지 overflow | POI 단계에서 `setFitToPage(true)` 강제 설정 | ✅ 적용 but 효과 없음 |
| 헤더(머리글) margin 겹침 | Spire.XLS 렌더링 엔진 자체 한계 | ❌ 해결 불가 |

**핵심 결론**: 서버 사이드 PDF 변환 라이브러리(Spire.XLS, Aspose.Cells 등)는 Microsoft Excel 렌더링 엔진을 완벽히 재현하지 못함. 헤더 margin, 인쇄 배율, 폰트 힌팅 등에서 구조적 차이 발생. "거의 동일"이 최선이며 "완전 동일"은 불가능.

### 11-2. 검토된 대안들

| 방법 | 품질 | 결론 |
|---|---|---|
| Spire.XLS (현재) | ★★★☆☆ | 헤더 겹침 해결 불가 |
| Aspose.Cells for Java | ★★★★☆ | ~$1,500, 100% 보장 불가, 비용 대비 불확실 |
| Microsoft Graph API | ★★★★★ | Rate Limit, 파일이 MS 클라우드 경유, 멀티테넌트 SaaS 구조에 부적합 |
| **클라이언트 미들웨어 (Excel COM)** | ★★★★★ | **채택 결정** |

### 11-3. 클라이언트 미들웨어 전환 결정

**방향**: C# WPF 기반 Windows 클라이언트 애플리케이션

```
기존 (서버 워커)
  CALI 서버 ──HTTP 트리거──▶ cali-worker ──Spire.XLS──▶ PDF (품질 불완전)

변경 (클라이언트 미들웨어)
  CALI 서버 ◀──폴링/SignalR──▶ 클라이언트 미들웨어 ──Excel COM──▶ PDF (100% 품질)
```

**기술 스택 확정:**
- 언어/프레임워크: C# / .NET 8 / WPF
- UI 라이브러리: MaterialDesignInXamlToolkit (Material Design 스타일)
- Excel 작업: Microsoft.Office.Interop.Excel (COM Automation)
- PDF 암호화: PdfSharp (오픈소스)
- 서버 통신: HttpClient 폴링 (초기) → SignalR WebSocket (이후)
- 배포: 단일 self-contained .exe + 라이선스 키 인증

**구현 가능 기능 목록:**
- Excel 직접 PDF 변환 (100% 품질 — 실제 Excel 엔진 사용)
- 데이터 삽입 / 서명 이미지 삽입 (Excel COM)
- 로컬 특정 디렉토리 자동 저장
- 결재 완료 파일 자동 오픈
- PDF 암호화
- N건 일괄 인쇄
- 실시간 작업 로그 (GUI 패널)
- 오류 한글 안내 (COM 예외 → 원인 분석 → 한글 메시지)
- 시스템 트레이 최소화
- Windows 시작 시 자동 실행

**오류 처리 방식 (중요):**
영어 코드 오류 대신 원인을 분석하여 한글로 안내.
```csharp
// 예: 여러 시트 선택 상태 감지
if (wb.ActiveSheet is not Worksheet)
    return "오류: 엑셀 파일이 여러 시트를 동시에 선택한 상태로 저장되어 있습니다.\n해결: 단일 시트만 선택 후 저장해주세요.";
```

### 11-4. cali-worker 현재 소스 보존

cali-worker 소스는 삭제하지 않고 보존.
- Excel 데이터 삽입 로직 (POI 기반) → 클라이언트 미들웨어 구현 시 참고
- 통신 계약(API 명세) → CALI 서버 측은 재활용 가능성 있음

---

## 12. 비즈니스 방향 (2026-03-23 논의)

### 12-1. 타겟 시장

교정/계측(300~400개 업체) 외 동일한 워크플로우를 가진 1순위 분야:

| 분야 | 추정 업체 수 | 워크플로우 유사도 |
|---|---|---|
| 환경측정대행업 | 400~600개 | ★★★★★ (의뢰→수행→성적서→보관) |
| 비파괴검사(NDT) | 500~800개 | ★★★★★ |
| 건설 품질검사 | 수백~1,000개+ | ★★★★☆ |
| 시험/검사기관(KOLAS) | 200~400개 | ★★★★★ |
| 식품검사 | 100~200개 | ★★★★☆ |

**공통점**: 규제 산업 → 법적 문서 보관 의무 → 단순 편의가 아닌 컴플라이언스 필수 도구로 포지셔닝 가능

### 12-2. 전략

- 교정/계측을 시작점으로 파일럿 운영 → 피드백 반영
- 동일 구조의 환경측정/NDT/시험기관으로 수평 확장
- 미들웨어에 라이선스 키 인증 적용 → 업체별 구독 모델

### 12-3. 선행 과제 (다음 세션 전 필수)

**계측/교정 업계 프로세스 현장 조사**
- 이전 회사와 차별화된 설계를 위해 현장에서 실제 불편함 재파악
- 조사 완료 후 미들웨어 + CALI 서버 API 재설계 진행

---

## 13. 데모 모드 구현 — cali-worker 미구동 시 샘플 파일 직접 업로드 (2026-03-24)

### 13-1. 배경

이력서 포트폴리오 시연 목적으로, cali-worker를 구동하지 않아도 성적서작성·실무자결재 워크플로우를 완전히 시연할 수 있도록 데모 모드를 구현.

### 13-2. 설계 원칙

- **인터페이스 보존**: cali-worker 관련 소스(triggerWorkerServer, WorkerCallbackController 등) 는 그대로 유지
- **모드 전환**: `application.properties`의 `app.worker.url` 하나로 전환
  - 비워두면 → 데모 모드 (`DemoJobProcessorService`)
  - URL 설정 시 → 실제 cali-worker 호출 (기존 동작)
- **폴링 UI 그대로 동작**: 데모 프로세서가 단계별 딜레이 + 상태 업데이트를 직접 수행하므로 프론트엔드 변경 없음

### 13-3. 추가/변경된 파일 (CALI backend)

| 파일 | 변경 내용 |
|---|---|
| `config/AsyncConfig.java` | 신규 — `@EnableAsync` + 스레드풀(core=2, max=5) |
| `service/DemoJobProcessorService.java` | 신규 — 비동기 데모 처리 서비스 |
| `service/ReportJobBatchServiceImpl.java` | `triggerWorkerServer()` 분기 추가 / `createWorkApprovalBatch()` 서명이미지 스킵 조건 추가 |
| `repository/FileInfoRepository.java` | `@Modifying` 메서드 2개에 `@Transactional` 추가 (데모 서비스의 외부 트랜잭션 없는 호출 대응) |
| `application.properties` | `app.worker.url=` (비움), `app.demo.file-dir=C:/BadaDev/cali/etc` 추가 |

### 13-4. 데모 파일 위치

```
C:\BadaDev\cali\etc\
├── BD26-0004-0001.xlsx    ← WRITE 시 origin.xlsx / WORK_APPROVAL 시 signed.xlsx 로 업로드
└── BD26-0004-0001.pdf     ← WORK_APPROVAL 시 signed.pdf 로 업로드
```

### 13-5. S3 업로드 경로 규칙 (데모 모드 동일)

| 작업 | S3 objectKey |
|---|---|
| WRITE 완료 | `{rootDir}/report/{reportId}/origin.xlsx` |
| WORK_APPROVAL 완료 (엑셀) | `{rootDir}/report/{reportId}/signed.xlsx` |
| WORK_APPROVAL 완료 (PDF) | `{rootDir}/report/{reportId}/signed.pdf` |

### 13-6. 단계별 딜레이 (폴링 UI 표시용)

**WRITE**
```
DOWNLOADING_TEMPLATE (1.5s) → FILLING_DATA (2s) → UPLOADING_ORIGIN (0.8s) → DONE
```

**WORK_APPROVAL**
```
DOWNLOADING_ORIGIN (1.5s) → INSERTING_SIGN (2s) → CONVERTING_PDF (2s) → UPLOADING_SIGNED (0.8s) → DONE
```

### 13-7. 데모 모드에서의 서명이미지 처리

`createWorkApprovalBatch()` 에서 `workerUrl` 이 비어있으면 서명이미지 DB 조회를 생략.
데모 프로세서는 서명 삽입 없이 샘플 파일을 그대로 업로드하므로 `workerSignImgKey = null` 로 처리됨.

### 13-8. 모드 전환 방법

```properties
# 데모 모드 (cali-worker 미구동)
app.worker.url=

# 실제 cali-worker 전환 시
app.worker.url=http://localhost:8060
```

---

## 10. 참고 문서

| 경로 | 내용 |
|---|---|
| `docs/claude/claudeReq.md` | 실무자결재 설계 전체 검토 |
| `docs/claude/claudeReqSheetSetting.md` | 데이터시트 시트 구조 |
| `docs/claude/claudeReqSample.md` | 샘플관리 작업 요청서 |
| `docs/review/` | 설계 검토 학습 문서 모음 |
| `docs/db/schema.sql` | DB 전체 스키마 |
| `CLAUDE.md` | Claude Code 공통 작업 규칙 |
