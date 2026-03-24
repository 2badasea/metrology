# 워커 서버 인바운드 API 키 인증 설계

> **작성 계기**: cali-worker 애플리케이션이 cali, bada-cali 등 여러 CALI 서버로부터
> `POST /api/jobs/execute` 요청을 받을 때, 어떻게 요청 출처를 인증할지 결정하기 위해 정리.

---

## 1. 선행 지식 — 인바운드 vs 아웃바운드

API 통신에서 방향성을 기준으로 두 개념을 구분한다.

| 용어 | 방향 | 이 프로젝트에서의 예시 |
|---|---|---|
| **인바운드(Inbound)** | 외부 → 나(서버) | CALI 서버 → cali-worker (`POST /api/jobs/execute`) |
| **아웃바운드(Outbound)** | 나(서버) → 외부 | cali-worker → CALI 서버 (`POST /api/worker/items/{id}/callback`) |

**인바운드 인증**이란, 워커 서버가 자신에게 들어오는 요청이
"신뢰할 수 있는 출처(즉, 정상적인 CALI 서버)"로부터 온 것인지 확인하는 행위다.

인증 없이 `POST /api/jobs/execute`를 열어두면:
- 누구든 워커에게 임의의 배치 작업을 실행시킬 수 있음
- 스토리지 접속 정보(accessKey, secretKey)를 위조 요청으로 전달 가능
- 워커의 CPU, 디스크, 네트워크 자원을 악용할 수 있음

---

## 2. API 키 인증의 기본 개념

**API 키(API Key)**는 서버 간 통신(S2S, Server-to-Server)에서 가장 흔하게 쓰이는 인증 수단이다.
클라이언트(요청 측)가 HTTP 헤더에 사전에 약속된 비밀 문자열을 담아 보내고,
서버(수신 측)가 이 값을 검사하여 요청을 허용하거나 거부한다.

```
POST /api/jobs/execute
X-Worker-Api-Key: cali-worker-key-dev-xxxx   ← 이게 API 키
Content-Type: application/json

{ "batchId": 123, ... }
```

### API 키 vs JWT vs OAuth2

| 방식 | 특징 | 적합한 상황 |
|---|---|---|
| **API 키** | 단순 문자열, 발급/검증이 매우 간단 | 서버 간 내부 통신, 소규모 신뢰 관계 |
| JWT | 서명 기반, 만료시간·클레임 포함 가능 | 사용자 인증, 단기 토큰 |
| OAuth2 | 복잡한 인가 흐름, 토큰 갱신 | 외부 공개 API, 대형 플랫폼 |

cali-worker는 **서버 간 내부 통신**이 목적이므로 API 키가 가장 적합하다.

---

## 3. 현재 프로젝트의 통신 구조

```
[CALI 서버 A]  ──POST /api/jobs/execute──▶  [cali-worker]
               (X-Worker-Api-Key: key-A)

[CALI 서버 B]  ──POST /api/jobs/execute──▶  [cali-worker]
               (X-Worker-Api-Key: key-B)

               ◀──POST /api/worker/items/{id}/callback──
               (X-Worker-Api-Key: 각 요청에서 받은 workerApiKey)
```

트리거 페이로드(`WorkerTriggerReq`)에는 다음이 포함된다:

```json
{
  "batchId": 123,
  "callbackBaseUrl": "http://cali-server-a:8050",
  "workerApiKey": "key-for-callbacks",
  "storageEndpoint": "https://...",
  "storageBucketName": "cali-a-bucket",
  "storageRootDir": "dev",
  "storageAccessKey": "AKID...",
  "storageSecretKey": "secret..."
}
```

즉, **스토리지 접속 정보와 콜백 주소는 요청마다 달라진다** — 멀티 테넌트는 이미 설계에 반영되어 있다.
문제는 **워커로 들어오는 요청 자체를 어떻게 검증하느냐**다.

---

## 4. 옵션 A — 단일 API 키 (Shared Key)

### 개념

워커가 하나의 API 키만 갖는다. 모든 CALI 서버가 이 키를 공유한다.

```properties
# cali-worker application.properties
app.worker.api-key=cali-worker-shared-key-2026
```

```java
// 워커 수신 검증 로직
if (!sharedApiKey.equals(requestApiKey)) {
    return 403 Forbidden;
}
```

모든 CALI 서버(cali, bada-cali, sujeong-cali)는 동일한 키를 사용한다:

```properties
# 각 CALI 서버의 application.properties
app.worker.api-key=cali-worker-shared-key-2026
```

### 장점

- 구현이 매우 단순 (문자열 비교 1줄)
- 설정 파일 관리가 쉬움
- 워커 코드 변경 없이 새 CALI 서버 추가 가능 (키만 공유하면 됨)

### 단점

- 하나의 CALI 서버가 키를 노출하면 **모든 서버가 위험**
- 어떤 CALI 서버가 요청했는지 워커 입장에서 식별 불가 (로깅/추적 약함)
- 특정 서버만 접근을 차단하려면 **키를 교체**해야 하고, 그러면 **모든 서버의 키도 교체**해야 함

### 적합한 상황

- 신뢰 관계가 명확한 소규모 환경 (같은 조직/팀이 운영)
- 초기 개발/테스트 단계
- 서버 수가 적고 키 노출 위험이 낮은 경우

---

## 5. 옵션 B — 화이트리스트 (Per-Tenant Key)

### 개념

워커가 테넌트(CALI 서버)별로 개별 API 키를 관리한다.

```properties
# cali-worker application.properties
app.worker.allowed-keys=key-for-cali,key-for-bada-cali,key-for-sujeong-cali
```

```java
// 워커 수신 검증 로직
Set<String> allowedKeys = Set.of(allowedKeysValue.split(","));
if (!allowedKeys.contains(requestApiKey)) {
    return 403 Forbidden;
}
```

각 CALI 서버는 자신만의 키를 갖는다:

```properties
# cali 서버
app.worker.api-key=key-for-cali

# bada-cali 서버
app.worker.api-key=key-for-bada-cali
```

### 장점

- 한 서버의 키가 노출돼도 **해당 키만 제거**하면 됨 (다른 서버에 영향 없음)
- 요청 헤더 값으로 **어느 서버가 요청했는지 식별** 가능
- 특정 서버 접근 차단이 쉬움 (목록에서 제거)
- 향후 테넌트별 **요청 횟수 제한(Rate Limiting)** 적용 가능

### 단점

- 새 CALI 서버 추가 시 **워커의 설정 파일 수정 + 재시작** 필요
- 키 목록이 길어지면 관리 복잡도 증가

### 적합한 상황

- 서버별 접근 통제가 필요한 경우
- 키 노출 가능성이 있거나, 운영 서버가 많아지는 경우
- 감사(Audit) 로그에서 요청 출처를 구분해야 하는 경우

---

## 6. 옵션 비교 요약

| 기준 | A. 단일 키 | B. 화이트리스트 |
|---|---|---|
| 구현 복잡도 | 낮음 | 보통 |
| 키 노출 피해 범위 | 전체 | 해당 테넌트만 |
| 출처 식별 가능 여부 | 불가 | 가능 (키 → 서버 매핑) |
| 신규 서버 추가 | 키 공유만으로 완료 | 워커 설정 수정 필요 |
| 특정 서버 차단 | 키 전체 교체 필요 | 목록에서 제거 |
| 초기 개발 적합성 | 매우 높음 | 높음 |
| 운영 확장 적합성 | 보통 | 높음 |

---

## 7. 권장 방향 (Claude 제안)

### 초기 구현: 옵션 A (단일 키)로 시작

**이유:**

1. **현재 단계의 목적**은 WSL2 로컬 테스트이고, 접속하는 서버도 1~2개
2. CALI의 기존 `WorkerCallbackController`도 단일 키(`app.worker.api-key`) 방식으로 구현되어 있음 — **기존 설계와 일관성** 유지
3. 개발 모드에서는 키가 비어 있으면 검증 생략하는 패턴도 동일하게 적용 가능

### 코드 구조는 B로 확장 가능하게 설계

단일 키 검증 로직을 인터페이스/메서드로 분리해 두면, 나중에 B로 전환할 때 코드 변경이 최소화된다:

```java
// 이렇게 설계하면 나중에 Set<String>으로 바꾸기 쉽다
private boolean isValidApiKey(String requestKey) {
    if (apiKey == null || apiKey.isBlank()) {
        log.warn("API 키 미설정 — 개발 모드로 검증 생략");
        return true;  // 개발 모드
    }
    return apiKey.equals(requestKey);  // 나중에 allowedKeys.contains(requestKey)로 교체
}
```

### 운영 전환 시점에 B로 업그레이드

- 서버가 3개 이상 되거나
- 키 관리 정책이 필요해지는 시점에 B로 전환

---

## 8. 참고 — IP 화이트리스트와의 병행 사용

API 키 외에 추가 보안 계층으로 **IP 화이트리스트**를 함께 사용할 수 있다.

```properties
app.worker.allowed-ips=192.168.1.10,10.0.0.5
```

요청의 `X-Forwarded-For` 또는 `RemoteAddr`를 확인하여, 등록된 IP에서 온 요청만 허용한다.

장점: API 키가 노출되더라도, 등록된 IP 외에서는 접근 불가
단점: CALI 서버 IP가 바뀌면 워커 설정도 갱신 필요 (클라우드 환경에서 IP 변동 잦음)

현재 프로젝트 단계에서는 구현 부담이 있으므로, 필요 시 나중에 추가하는 것을 권장한다.

---

## 9. 관련 개념 정리

| 용어 | 설명 |
|---|---|
| **S2S (Server to Server)** | 사람(브라우저)이 아닌 서버끼리 직접 통신하는 방식 |
| **테넌트(Tenant)** | 동일한 시스템을 공유하지만 독립된 데이터를 갖는 고객 단위. 여기서는 각 CALI 서버 인스턴스 |
| **멀티 테넌트(Multi-tenant)** | 하나의 워커 서버가 여러 CALI 서버를 고객으로 동시에 서비스하는 구조 |
| **API 게이트웨이** | 여러 서비스 앞에 두는 중간 계층. 인증, 로깅, Rate Limiting 등을 일괄 처리. 규모가 커지면 고려 |
| **Rate Limiting** | 특정 클라이언트의 요청 횟수를 제한하는 정책 (예: 분당 10건) |
| **헤더 기반 인증** | `Authorization`, `X-Api-Key` 등 HTTP 헤더에 인증 정보를 담는 방식 |
