# 성적서 워커 애플리케이션 구현 전 실전 학습 문서
## Part A — 전체 아키텍처 / WSL2 / Linux / Java 운영 / 통신 설계 (1~6장)

> 대상 독자: Java/Spring 웹 개발 경험 있음 / Linux 운영·네트워크·배포는 깊지 않음
> 목적: cali-worker 구현 전 기술 지형 전체 파악
> 기준 프로젝트: CALI 교정관리 + cali-worker 성적서 작업 서버

---

# 1장. 전체 아키텍처 이해

## 1-1. CALI와 워커 앱의 역할 분리

### 개념 정의

CALI는 사용자 요청을 받아 응답을 반환하는 **메인 웹 애플리케이션**이다.
cali-worker는 CALI로부터 트리거를 받아 **무거운 파일 처리 작업을 대신 수행하는 별도 서버**다.

```
[사용자 브라우저]
      │
      │  HTTP 요청 (클릭, 폼 전송)
      ▼
┌─────────────────────────────────────┐
│           CALI (메인 앱)             │
│  Spring Boot 3.5.x / Thymeleaf      │
│  포트: 8050                          │
│                                     │
│  ┌──────────┐   ┌──────────────┐    │
│  │  DB(MySQL)│   │  NCP Storage │    │
│  │  상태 관리 │   │  파일 저장    │    │
│  └──────────┘   └──────────────┘    │
└─────────────────────────────────────┘
      │
      │  REST API 트리거 (POST /api/jobs/execute)
      ▼
┌─────────────────────────────────────┐
│        cali-worker (작업 서버)       │
│  Spring Boot / Apache POI            │
│  포트: 8060                          │
│                                     │
│  [샘플 다운로드] → [데이터 삽입]      │
│  → [origin.xlsx 생성] → [업로드]     │
│  → [CALI 콜백]                      │
└─────────────────────────────────────┘
```

### 왜 역할을 분리하는가

웹 요청-응답 사이클은 기본적으로 **동기 처리**다.
사용자가 버튼을 누르면 서버는 처리를 끝낼 때까지 응답을 보내지 못한다.

엑셀 파일 생성은 다음과 같은 이유로 요청-응답 안에 넣으면 안 된다:

| 문제 | 설명 |
|---|---|
| 처리 시간 | 성적서 1건 처리에 수 초 ~ 수십 초 소요 가능 |
| 다건 일괄 | 20건 선택 시 전체 완료까지 수 분 소요 가능 |
| 브라우저 제한 | 브라우저는 보통 30~60초 이후 타임아웃 |
| 자원 경합 | 파일 처리는 CPU/메모리 집약적 → 메인 앱 성능 저하 |
| 독립 실패 처리 | 1건 실패가 전체 요청 실패로 이어지면 안 됨 |

### 이번 프로젝트에서의 역할 분리 기준

```
CALI가 하는 일:
  - 사용자 인증 / 세션 관리
  - batch + item DB 생성 및 상태 관리
  - 작업서버 트리거 발송
  - 브라우저 Polling 응답
  - 콜백 수신 후 DB 상태 갱신

cali-worker가 하는 일:
  - 트리거 수신 및 처리 큐 관리
  - 스토리지에서 샘플 파일 다운로드
  - Apache POI로 엑셀 데이터 삽입
  - 완성 파일 스토리지 업로드
  - CALI에 item별 콜백 전송
```

---

## 1-2. 브라우저 요청-응답과 백그라운드 작업의 차이

### 동기 처리 모델 (일반 HTTP)

```
브라우저     CALI
   │          │
   │──요청──▶│
   │          │ ← 처리 중 (사용자는 기다림)
   │◀──응답──│
```

- 서버가 응답을 보내기 전까지 연결이 유지됨
- 처리 시간이 길면 타임아웃 발생
- 처리 도중 브라우저가 닫히면 요청 자체가 중단될 수 있음

### 비동기 백그라운드 처리 모델 (이번 구조)

```
브라우저          CALI              cali-worker
   │               │                    │
   │──배치생성──▶ │                    │
   │◀──즉시응답── │                    │
   │               │──트리거────────▶  │
   │               │                   │ 작업 처리 중
   │──polling──▶  │                   │
   │◀──상태응답── │                   │
   │               │ ◀──콜백─────────  │
   │──polling──▶  │                   │
   │◀──완료응답── │                   │
```

핵심 포인트:
- CALI는 batch + item을 DB에 생성한 뒤 **즉시 201 응답을 반환**
- 작업은 cali-worker가 **독립적으로 처리**
- 브라우저가 닫혀도 작업은 계속됨
- 브라우저는 주기적으로 Polling해서 진행상황을 확인

---

## 1-3. batch / item 구조가 왜 필요한가

### 문제: 성적서 n건을 어떻게 추적할 것인가

성적서 20건을 일괄 작성한다고 하자.
각각의 성적서는 **독립적으로 성공하거나 실패**할 수 있다.
이걸 단일 API 호출로 처리하면:

- 전체가 성공해야만 "성공" 응답 → 1건 실패로 19건도 없던 일이 됨
- 개별 상태 추적 불가
- 재시도 단위 불명확

### 해결: batch + item 분리

```
사용자 버튼 클릭 1회 = batch 1건 (report_job_batch)
선택된 성적서 n건 = item n건 (report_job_item)
```

```sql
report_job_batch (id=101, status=PROGRESS, total=20, success=15, fail=2)
  ↑
  ├── report_job_item (id=1001, batch_id=101, report_id=501, status=SUCCESS)
  ├── report_job_item (id=1002, batch_id=101, report_id=502, status=FAIL)
  ├── report_job_item (id=1003, batch_id=101, report_id=503, status=PROGRESS, step=FILLING_DATA)
  ...
```

이 구조 덕분에:
- 전체 진행률 = (success_count + fail_count) / total_count
- 개별 성적서 상태 = item.status + item.step
- 1건 실패가 다른 건에 영향 없음
- 재시도 시 fail된 item만 재처리 가능

---

## 1-4. DB 테이블이 큐 역할을 대체하는 이유

### 큐(Queue)란 무엇인가

큐는 "처리할 작업 목록"을 저장하는 구조다.
Redis, Kafka, RabbitMQ 같은 전용 메시지 브로커가 일반적인 큐 시스템이다.

```
Producer → [작업A, 작업B, 작업C] → Consumer
```

### 이번 프로젝트에서 DB가 큐 역할을 하는 방법

```
CALI (Producer)
  → report_job_item INSERT (status=READY)
  → cali-worker에 batchId 전달

cali-worker (Consumer)
  → GET /api/worker/batches/{batchId} 호출
  → READY 상태의 item 목록 조회
  → 순서대로 처리
```

### 왜 지금은 전용 큐 없이도 가능한가

| 조건 | 전용 큐 없이 가능 여부 |
|---|---|
| 작업 서버가 1대 | ✅ 가능 — 동시 처리 경합 없음 |
| 처리 속도가 빠름 | ✅ 가능 — 큐에 쌓일 만큼 빠르게 소진 |
| 재시도 로직이 단순 | ✅ 가능 — DB 상태로 충분히 관리 |
| 작업 중복 실행 없음 | ✅ 가능 — 트리거 1회 → 처리 1회 |

### 언제 전용 큐가 필요해지는가

- 작업 서버가 2대 이상으로 확장될 때 (경합 문제)
- 트리거 요청이 매우 빠르게 쌓일 때
- 작업 재시도 정책이 복잡해질 때
- 처리 순서 보장이 엄격히 필요할 때

---

## 1-5. 이번 프로젝트 전체 시퀀스 다이어그램

```
[사용자]          [CALI]              [DB]         [cali-worker]   [NCP Storage]
   │                │                   │                │               │
   │ 성적서 선택 클릭 │                   │                │               │
   │──POST /api/report/jobs/batches──▶  │                │               │
   │                │── batch INSERT ──▶│                │               │
   │                │── items INSERT ──▶│                │               │
   │                │── report.write_status=PROGRESS ──▶│               │
   │◀── 201 Created ─│                  │                │               │
   │                │                   │                │               │
   │                │──POST /api/jobs/execute──────────▶│               │
   │                │◀── 202 Accepted ─────────────────  │               │
   │                │                   │                │               │
   │                │                   │                │─ sample 다운──▶│
   │                │                   │                │◀── sample.xlsx─│
   │                │                   │                │               │
   │ Polling 시작    │                   │                │  item별 처리   │
   │──GET /api/report/jobs/batches/{id}──▶              │               │
   │◀── 진행상황 ────│                   │                │               │
   │                │                   │                │               │
   │                │◀──POST /api/worker/items/{id}/callback (PROGRESS)  │
   │                │── item status 갱신─▶               │               │
   │                │                   │                │               │
   │                │                   │                │─ origin.xlsx 업로드 ──▶│
   │                │◀──POST /api/worker/items/{id}/callback (SUCCESS)   │
   │                │── report.write_status=SUCCESS ────▶│               │
   │                │── batch.success_count++ ──────────▶│               │
   │                │                   │                │               │
   │ Polling        │                   │                │               │
   │──GET /api/report/jobs/batches/{id}──▶              │               │
   │◀── 완료 ────────│                   │               │               │
```

---

# 2장. WSL2 이해

## 2-1. WSL2란 무엇인가

### 개념 정의

WSL(Windows Subsystem for Linux)은 Windows 위에서 Linux를 실행할 수 있게 해주는 기술이다.
WSL2는 WSL의 2세대로, **실제 Linux 커널을 경량 VM 형태로 실행**한다.

```
┌───────────────────────────────────────────┐
│                  Windows 11               │
│                                           │
│  ┌─────────────────────────────────────┐  │
│  │    Hyper-V 기반 경량 VM              │  │
│  │  ┌──────────────────────────────┐   │  │
│  │  │     Linux 커널 (실제 커널)    │   │  │
│  │  │  ┌────────────────────────┐  │   │  │
│  │  │  │  Ubuntu / Debian 등    │  │   │  │
│  │  │  │  사용자 공간(userspace) │  │   │  │
│  │  │  │   bash, apt, java 등   │  │   │  │
│  │  │  └────────────────────────┘  │   │  │
│  │  └──────────────────────────────┘   │  │
│  └─────────────────────────────────────┘  │
└───────────────────────────────────────────┘
```

### 핵심 특성

- 실제 Linux 커널이 실행됨 (에뮬레이션이 아님)
- Windows 파일시스템과 Linux 파일시스템을 서로 접근 가능
- Windows 앱과 Linux 앱이 같은 네트워크 인터페이스를 공유

---

## 2-2. WSL1 vs WSL2 비교

| 항목 | WSL1 | WSL2 |
|---|---|---|
| Linux 커널 | 없음 (시스템 콜 변환) | 실제 Linux 커널 |
| 구현 방식 | 번역 레이어 | 경량 Hyper-V VM |
| 파일 I/O 속도 (Linux 내) | 느림 | 빠름 |
| 파일 I/O 속도 (Windows 마운트) | 빠름 | 느림 |
| 호환성 | 일부 시스템 콜 미지원 | 거의 완벽한 Linux 호환 |
| 네트워크 | Windows와 동일 | NAT 방식 (기본) 또는 mirrored |
| 메모리 사용 | 적음 | VM 오버헤드 있음 |
| Docker 지원 | 제한적 | 완벽 지원 |
| Java 실행 | 가능하나 제한 있음 | 완전한 Linux Java |
| systemd | 지원 안 함 | 지원 (WSL 0.67.6 이상) |

### 판단 기준

Java 워커 앱을 Linux 환경에서 실행할 때:
- **WSL1**: systemd 없음, 일부 Java 라이브러리 동작 이상 → 적합하지 않음
- **WSL2**: 실제 Linux 커널 → Java 앱 완전 동작, systemd 사용 가능 → 적합

---

## 2-3. 왜 이번 프로젝트에서 WSL2가 적절한가

### 개발 단계 배포 환경으로서의 WSL2

```
개발 환경 (현재)
  Windows PC → WSL2 → Ubuntu → cali-worker JAR
  장점: 추가 서버 비용 없음, Windows 개발 환경과 공존

운영 환경 (예정)
  미니PC 홈서버 → Ubuntu → cali-worker JAR
  장점: 실제 Linux 서버, 별도 하드웨어

클라우드 환경 (확장 시)
  퍼블릭 클라우드 → Linux 인스턴스 → cali-worker JAR
```

WSL2는 **"실제 Linux에 배포했을 때의 동작을 로컬에서 미리 검증"**할 수 있는 환경이다.
WSL2에서 잘 동작하면, 홈서버(Ubuntu)로 이동할 때 설정만 바꾸면 된다.

### 이번 프로젝트 기준 WSL2의 역할

- cali-worker를 WSL2 Ubuntu에 JAR 배포
- CALI (Windows, 포트 8050) → WSL2 cali-worker (포트 8060) 통신
- NCP 스토리지는 외부 API이므로 WSL2에서 직접 접근

---

## 2-4. Windows와 Linux가 어떻게 공존하는가

### 파일시스템 공유

```
Windows 쪽:          Linux 쪽:
C:\BadaDev\cali  =  /mnt/c/BadaDev/cali  (Linux에서 Windows 파일 접근)
                     /home/user/          (Linux 전용 영역, 빠름)
```

- Windows에서 Linux 파일에 접근: `\\wsl$\Ubuntu\home\user\`
- Linux에서 Windows 파일에 접근: `/mnt/c/`, `/mnt/d/`

### 권장 파일 위치

| 파일 종류 | 저장 위치 | 이유 |
|---|---|---|
| cali-worker JAR | Linux 홈디렉터리 (`/home/user/`) | Linux 파일 I/O 성능 |
| application.properties | Linux 홈디렉터리 | 빠른 읽기 |
| 임시 작업 파일 | Linux 내 디렉터리 | 파일 처리 성능 |
| 소스코드 | Windows (`C:\`) | IDE 개발 편의성 |

### 포트 공유

WSL2 기본 설정(NAT mode)에서 Windows의 localhost는 WSL2와 연결된다.
즉, WSL2에서 8060 포트로 앱을 실행하면
Windows의 브라우저에서 `localhost:8060`으로 접근 가능하다.

---

## 2-5. WSL2의 장점과 한계

### 장점

- **실제 Linux 커널**: Java 앱, 시스템 명령어 모두 Linux 기준으로 동작
- **systemd 지원** (최신 버전): 서비스로 등록하여 자동 시작 가능
- **Docker 완벽 지원**: Docker Desktop이 WSL2 백엔드를 사용
- **Windows와 파일 공유**: 개발 시 편리한 파일 접근
- **낮은 진입장벽**: 별도 서버 비용 없이 Linux 환경 확보

### 한계

- **네트워크 복잡성**: 외부(같은 공유기의 다른 PC)에서 접근하려면 추가 설정 필요
- **재시작 시 IP 변동**: WSL2 NAT IP는 재시작마다 바뀔 수 있음
- **Windows 마운트 파일 성능**: `/mnt/c/`를 통해 Windows 파일 접근 시 느림
- **메모리 제한**: Windows와 메모리를 공유하므로 과도한 사용 시 Windows 성능 저하
- **공개 서버 부적합**: 외부에서 직접 접근하는 서비스에는 부적합

---

# 3장. WSL2 네트워크와 포트 구조

## 3-1. WSL2 네트워크 모드 두 가지

### NAT mode (기본)

```
인터넷 (외부)
    │
    ▼
[공유기 / 라우터]
    │
    ▼
[Windows PC - IP: 192.168.1.100]
    │ 내부 가상 네트워크
    ▼
[WSL2 VM - IP: 172.x.x.x]
    │
    ▼
cali-worker (port 8060)
```

- WSL2는 별도의 내부 IP를 가짐 (예: 172.20.0.1)
- Windows와 WSL2 사이에 NAT 계층 존재
- **Windows에서 WSL2 접근**: `localhost` 또는 WSL2 내부 IP로 가능
- **외부에서 WSL2 접근**: Windows의 포트 포워딩 설정 필요

### mirrored mode (Windows 11 22H2 이상, WSL 2.0 이상)

```
[Windows PC] ─── [WSL2 VM]
두 환경이 같은 네트워크 인터페이스를 공유
Windows IP = WSL2 IP (동일하게 보임)
```

- 더 단순한 네트워크 구조
- `~/.wslconfig`에서 `networkingMode=mirrored` 설정으로 활성화
- 외부에서 Windows IP로 직접 WSL2 포트에 접근 가능

```ini
# %USERPROFILE%\.wslconfig
[wsl2]
networkingMode=mirrored
```

---

## 3-2. localhost 접근 방법

### Windows → WSL2 접근

```bash
# CALI (Windows, 8050)가 cali-worker (WSL2, 8060)에 트리거 전송
# 설정값:
app.worker.url=http://localhost:8060

# NAT mode에서도 Windows → WSL2는 localhost로 동작
# (WSL2가 자동으로 Windows의 localhost를 포워딩해줌)
```

### WSL2 → Windows 접근 (역방향)

NAT mode에서 WSL2가 Windows 앱에 접근하려면 Windows IP가 필요하다:

```bash
# WSL2 안에서 Windows IP 확인
ip route show | grep -i default | awk '{ print $3}'
# 또는
cat /etc/resolv.conf | grep nameserver | awk '{print $2}'
```

이 이슈가 바로 콜백 URL 설정에 영향을 준다:

```properties
# CALI가 콜백을 받으려면 WSL2에서 Windows의 CALI 8050에 접근해야 함
# cali-worker의 callbackBaseUrl 수신 시 처리:
# app.cali.callback-base-url=http://localhost:8050
# ※ WSL2 → Windows 방향이므로 NAT에서 localhost가 작동 안 할 수 있음
# → Windows IP(host.docker.internal 또는 ip route로 확인한 IP) 사용 필요
```

**실무 해결책**: mirrored mode 사용하거나, WSL2에서 `host.docker.internal`로 접근

---

## 3-3. 바인딩 주소: 127.0.0.1 vs 0.0.0.0

Java 앱이 어떤 주소로 Listen하느냐가 접근 가능 범위를 결정한다.

```properties
# 127.0.0.1 바인딩 (loopback만)
server.address=127.0.0.1
# → 같은 머신에서만 접근 가능
# → WSL2 내부에서만 접근 가능, Windows에서 불가

# 0.0.0.0 바인딩 (모든 인터페이스)
server.address=0.0.0.0  # Spring Boot 기본값
# → 같은 머신 + 네트워크 상의 다른 머신도 접근 가능
# → Windows에서 WSL2 앱에 접근 가능
```

**이번 cali-worker 기본 설정**: Spring Boot 기본(0.0.0.0)을 사용하면
Windows의 CALI가 `localhost:8060`으로 접근 가능하다.

---

## 3-4. 외부에서 WSL2에 접근시키기

### 상황: 같은 공유기의 다른 PC가 WSL2 앱에 접근해야 할 때

1. **WSL2 앱이 0.0.0.0으로 Listen 중**이어야 함
2. **Windows 방화벽**에서 해당 포트 인바운드 허용 필요
3. **포트 포워딩** (NAT mode 시): netsh 명령으로 Windows가 외부 요청을 WSL2로 전달

```powershell
# Windows PowerShell (관리자)
# WSL2의 8060 포트를 외부에서 접근 가능하게
netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=8060 connectaddress=<WSL2_IP> connectport=8060

# 방화벽 규칙 추가
netsh advfirewall firewall add rule name="WSL2 cali-worker" dir=in action=allow protocol=TCP localport=8060
```

**mirrored mode 사용 시**: 이 설정 불필요. Windows 방화벽만 허용하면 됨.

---

## 3-5. 홈서버/공유기/NAT/포트포워딩

### 홈서버로 이전 시 네트워크 구조

```
인터넷 (외부)
    │
    ▼
공유기 (NAT, 공인IP 1개)
  │
  ├── Windows PC (192.168.1.100) → CALI
  └── 미니PC (192.168.1.200)    → cali-worker (Ubuntu)
```

같은 공유기 내부: CALI가 `http://192.168.1.200:8060`으로 워커 접근 가능
외부에서 접근: 공유기 포트포워딩 설정 필요

```
공유기 포트포워딩 설정:
외부 포트 8060 → 내부 192.168.1.200:8060
```

---

# 4장. Linux 서버 기초

## 4-1. Linux 디렉터리 구조

Java 앱 배포 시 알아야 하는 핵심 디렉터리:

```
/
├── home/
│   └── ubuntu/          # 사용자 홈디렉터리
│       ├── cali-worker.jar
│       └── application-prod.properties
├── opt/
│   └── cali/            # 선택적 배포 위치 (CALI 메인 앱 기준)
│       ├── cali-worker.jar
│       └── work/        # 임시 작업 디렉터리
├── var/
│   └── log/             # 시스템 로그
├── etc/
│   └── systemd/
│       └── system/      # systemd 서비스 파일 위치
├── usr/
│   └── lib/
│       └── jvm/         # Java 설치 위치
├── tmp/                 # 임시 파일 (재시작 시 삭제됨)
└── proc/                # 프로세스 정보 (가상 파일시스템)
```

### 임시 디렉터리 주의사항

`/tmp`는 시스템 재시작 시 삭제된다.
cali-worker의 작업 임시 디렉터리는 `/tmp` 대신 별도 경로를 사용해야 한다:

```
추천: /opt/cali-worker/work/{UUID}/
이유: 시스템 재시작 후에도 실패한 작업 파일 보존 가능
```

---

## 4-2. 파일 권한 개념

### 권한 표기법

```bash
ls -la /opt/cali/
# drwxr-xr-x 2 ubuntu ubuntu 4096 Mar 20 10:00 work/
# -rw-r--r-- 1 ubuntu ubuntu 51234 Mar 20 10:00 cali-worker.jar
```

```
d rwx r-x r-x
│ │   │   └── others (그 외 사용자): 읽기만 가능
│ │   └────── group (같은 그룹): 읽기+실행 가능
│ └────────── owner (소유자): 읽기+쓰기+실행 가능
└──────────── 파일 타입 (d=디렉터리, -=파일)
```

### cali-worker 실행 시 권한 고려사항

```bash
# JAR 파일은 실행 권한 필요
chmod +x /opt/cali-worker/cali-worker.jar

# 작업 디렉터리는 쓰기 권한 필요
mkdir -p /opt/cali-worker/work
chown -R ubuntu:ubuntu /opt/cali-worker/work
chmod 755 /opt/cali-worker/work
```

---

## 4-3. 프로세스, 서비스, 포트 확인

### 프로세스 확인

```bash
# 실행 중인 Java 프로세스 확인
ps aux | grep java

# 특정 포트를 사용하는 프로세스 확인
ss -tlnp | grep 8060
# 또는
lsof -i :8060
```

### 포트 사용 현황

```bash
# 모든 Listen 중인 포트 확인
ss -tlnp

# 결과 예시:
# State    Recv-Q Send-Q Local Address:Port Peer Address:Port Process
# LISTEN   0      128          0.0.0.0:8060      0.0.0.0:*   users:(("java",pid=1234,fd=8))
```

---

## 4-4. systemd와 systemctl

### systemd란

Linux 서비스 관리 시스템이다.
Java 앱을 OS 서비스로 등록하면 시스템 시작 시 자동으로 실행되고,
비정상 종료 시 자동으로 재시작된다.

### cali-worker 서비스 파일 예시

```ini
# /etc/systemd/system/cali-worker.service

[Unit]
Description=CALI Worker Application
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/cali-worker
ExecStart=/usr/bin/java -jar /opt/cali-worker/cali-worker.jar \
  --spring.config.additional-location=/opt/cali-worker/application-prod.properties
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=cali-worker

# 환경변수 설정
Environment="JAVA_OPTS=-Xmx512m -Xms256m"

[Install]
WantedBy=multi-user.target
```

### systemctl 명령어

```bash
# 서비스 등록 (서비스 파일 수정 후)
sudo systemctl daemon-reload

# 서비스 시작
sudo systemctl start cali-worker

# 시스템 부팅 시 자동 시작 활성화
sudo systemctl enable cali-worker

# 서비스 상태 확인
sudo systemctl status cali-worker

# 서비스 중지
sudo systemctl stop cali-worker

# 서비스 재시작
sudo systemctl restart cali-worker

# 서비스 로그 확인 (최근 100줄)
journalctl -u cali-worker -n 100 --no-pager

# 서비스 로그 실시간 확인
journalctl -u cali-worker -f
```

---

## 4-5. journalctl 로그 확인

```bash
# 서비스 로그 전체
journalctl -u cali-worker

# 마지막 N줄
journalctl -u cali-worker -n 50

# 특정 시간 이후 로그
journalctl -u cali-worker --since "2026-03-20 10:00:00"

# 실시간 팔로우
journalctl -u cali-worker -f

# 오류만 필터
journalctl -u cali-worker -p err

# 특정 키워드 검색
journalctl -u cali-worker | grep "batchId=101"
```

---

## 4-6. 운영 중 꼭 알아야 할 기본 명령어

### 시스템 자원 확인

```bash
# CPU, 메모리 실시간 모니터링
top
# 또는 더 보기 좋은 htop (별도 설치 필요: apt install htop)
htop

# 메모리 사용량
free -h
# 결과: total used free available
#        8.0G  3.2G 4.8G       4.5G

# 디스크 사용량
df -h
# 각 마운트 포인트별 사용 현황

# 특정 디렉터리 사용량
du -sh /opt/cali-worker/work/
```

### 네트워크 확인

```bash
# 현재 IP 주소 확인
ip addr show

# 라우팅 테이블
ip route show

# 연결 상태 확인
ss -tlnp                    # Listen 중인 포트
ss -tnp | grep ESTABLISHED  # 연결 중인 소켓
```

### Java 앱 관련

```bash
# Java 버전 확인
java -version

# 실행 중인 Java 앱 목록 및 인수
jps -v

# Java 앱 힙 사용량 (pid 필요)
jstat -gc <pid> 5s

# GC 로그 활성화 (서비스 파일에 추가)
# Environment="JAVA_OPTS=-Xmx512m -Xlog:gc:file=/opt/cali-worker/gc.log"
```

---

# 5장. Java 워커 애플리케이션 운영 방식

## 5-1. Java 앱을 WSL2에서 실행하는 방법

### 방법 1: foreground 실행 (개발/테스트용)

```bash
# 기본 실행
java -jar cali-worker.jar

# 외부 설정파일 포함 실행
java -jar cali-worker.jar \
  --spring.config.additional-location=/opt/cali-worker/application-prod.properties

# 특정 프로파일로 실행
java -jar cali-worker.jar --spring.profiles.active=prod
```

- 장점: 로그가 바로 보임, 즉시 Ctrl+C로 종료 가능
- 단점: 터미널 종료 시 앱도 종료됨

### 방법 2: background 실행 (nohup)

```bash
# nohup으로 백그라운드 실행
nohup java -jar cali-worker.jar \
  --spring.config.additional-location=/opt/cali-worker/application-prod.properties \
  > /opt/cali-worker/app.log 2>&1 &

# 실행된 PID 확인
echo $!  # 바로 직전 백그라운드 프로세스 PID

# 또는 파일에 저장
nohup java -jar cali-worker.jar ... > app.log 2>&1 &
echo $! > /opt/cali-worker/app.pid

# 종료
kill $(cat /opt/cali-worker/app.pid)
```

- 장점: 터미널 종료 후에도 계속 실행
- 단점: 수동 관리 필요, 시스템 재시작 시 자동 시작 안 됨

### 방법 3: systemd 서비스 (운영 권장)

4장에서 설명한 서비스 파일 등록 방식.

- 장점: 자동 시작, 자동 재시작, journalctl 통합 로그
- 단점: 초기 설정 필요, WSL2에서 systemd 활성화 필요

---

## 5-2. foreground / background / systemd service 비교

| 항목 | foreground | nohup background | systemd service |
|---|---|---|---|
| 사용 시점 | 개발/디버그 | 임시 운영 | 운영 환경 |
| 터미널 종료 후 | 앱 종료 | 계속 실행 | 계속 실행 |
| 시스템 재시작 후 | 앱 없음 | 앱 없음 | 자동 시작 |
| 비정상 종료 시 | 없음 | 없음 | 자동 재시작 |
| 로그 관리 | 터미널 출력 | 파일 리다이렉트 | journalctl |
| WSL2에서 사용 | ✅ 쉬움 | ✅ 가능 | ⚠️ systemd 활성화 필요 |

### WSL2에서 systemd 활성화

```bash
# /etc/wsl.conf 파일 수정
sudo nano /etc/wsl.conf
```

```ini
[boot]
systemd=true
```

```powershell
# Windows PowerShell에서 WSL2 재시작
wsl --shutdown
# 그 후 WSL2 다시 시작
```

---

## 5-3. 재시작 정책

### 비정상 종료 상황

- Java 앱이 OutOfMemoryError로 죽은 경우
- 처리 도중 예외 처리 안 된 예외로 앱 전체 종료
- WSL2 자체 재시작

### systemd 재시작 설정

```ini
[Service]
Restart=on-failure      # 비정상 종료 시만 재시작
# Restart=always        # 항상 재시작 (정상 종료 포함)
RestartSec=10           # 재시작 전 대기 시간 (초)
StartLimitInterval=60   # 이 기간 동안 (초)
StartLimitBurst=3       # 이 횟수 이상 재시작 시 포기
```

### 재시작 시 처리 중이던 작업 처리

cali-worker가 item을 처리하던 도중 죽으면:
- 해당 item은 PROGRESS 상태로 DB에 남아있음
- 재시작 후 해당 배치의 트리거를 다시 받지 않으면 처리 재개 불가

**현실적 해결책**: cali-worker 재시작 시 PROGRESS 상태의 item을 찾아 FAIL로 처리
또는 CALI가 일정 시간 후 응답 없는 batch를 FAIL로 처리

---

## 5-4. 환경변수와 설정 분리

### Spring Boot 설정 우선순위 (높은 순)

```
1. 커맨드라인 인수          --server.port=8060
2. 환경변수                 SERVER_PORT=8060
3. 외부 설정파일            /opt/cali-worker/application-prod.properties
4. JAR 내부 설정파일        application.properties (번들)
```

### 설정 분리 전략

```
JAR 내부 (application.properties):
  - 기본값만 포함
  - 민감정보 절대 포함 금지
  - 예: server.port=8060, app.work-dir=/opt/cali-worker/work

외부 설정파일 (/opt/cali-worker/application-prod.properties):
  - 민감정보 포함
  - git에 커밋 안 함
  - 예: ncp.storage.access-key=..., app.cali.callback-base-url=...
```

```bash
# 실행 시 외부 설정파일 지정
java -jar cali-worker.jar \
  --spring.config.additional-location=file:/opt/cali-worker/application-prod.properties
```

---

## 5-5. Graceful Shutdown

### 개념

앱이 종료 신호를 받았을 때, **현재 처리 중인 요청을 완료한 뒤 종료**하는 방식.

```properties
# application.properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

### 왜 중요한가

cali-worker가 item을 처리하던 도중 종료 신호(`SIGTERM`)를 받으면:
- graceful shutdown 없이: 처리 중단 → item이 PROGRESS 상태로 고착
- graceful shutdown 있을 때: 현재 item 처리 완료 후 종료 → 정상 상태로 마무리

**systemd에서 종료 대기 시간 설정**:
```ini
[Service]
TimeoutStopSec=60   # 60초 내에 종료 완료 안 되면 강제 종료
```

---

# 6장. CALI ↔ 작업서버 통신 설계

## 6-1. 확정된 통신 프로토콜

이미 구현 완료된 CALI 측 API와 확정된 프로토콜을 정리한다.

### 트리거 API (CALI → cali-worker)

```
POST {app.worker.url}/api/jobs/execute
Header: X-Worker-Api-Key: {workerApiKey}
Content-Type: application/json

Body:
{
  "batchId": 101,
  "callbackBaseUrl": "http://192.168.1.100:8050",
  "workerApiKey": "cali-worker-key-dev",
  "storageEndpoint": "https://kr.object.ncloudstorage.com",
  "storageBucketName": "cali",
  "storageRootDir": "dev",
  "storageAccessKey": "...",
  "storageSecretKey": "..."
}
```

### 배치 조회 API (cali-worker → CALI)

```
GET {callbackBaseUrl}/api/worker/batches/{batchId}
Header: X-Worker-Api-Key: {workerApiKey}

Response:
{
  "code": 1,
  "msg": "배치 조회 성공",
  "data": {
    "batchId": 101,
    "jobType": "WRITE",
    "status": "READY",
    "totalCount": 5,
    "sampleId": 10,
    "items": [
      {"itemId": 1001, "reportId": 501, "status": "READY"},
      ...
    ]
  }
}
```

### 콜백 API (cali-worker → CALI)

```
POST {callbackBaseUrl}/api/worker/items/{itemId}/callback
Header: X-Worker-Api-Key: {workerApiKey}
Content-Type: application/json

Body:
{
  "status": "PROGRESS",   // PROGRESS / SUCCESS / FAIL
  "step": "FILLING_DATA", // 현재 처리 단계
  "message": null          // 실패 시 원인 메시지
}
```

---

## 6-2. 인증 방식 — API Key

### 이번 구현의 인증 구조

```
cali-worker가 보내는 요청:
  X-Worker-Api-Key 헤더 포함

CALI(WorkerCallbackController)가 검증:
  - app.worker.api-key가 비어있으면 개발 모드 (검증 생략)
  - 비어있지 않으면 헤더 값과 equals 비교
```

### API Key 인증의 장단점

| 장점 | 단점 |
|---|---|
| 구현 단순 | 탈취 시 전체 노출 |
| 추가 라이브러리 불필요 | 유효기간 없음 |
| 서버간 통신에 적합 | 로테이션 수동 처리 |

### 보완책

- HTTPS 사용 시 헤더 암호화 → 탈취 위험 감소
- 내부 네트워크에서만 통신하도록 IP 제한 (방화벽/Security Config 설정)
- 운영 환경에서는 외부 properties로 API Key 관리 (git 비포함)

---

## 6-3. Timeout과 Retry

### 트리거 타임아웃

```java
// CALI의 RestClient 트리거 호출 (ReportJobBatchServiceImpl)
// 트리거는 응답만 확인하면 되므로 짧은 타임아웃으로 충분
RestClient.create()
    .post()
    .uri(workerUrl + "/api/jobs/execute")
    .header("X-Worker-Api-Key", workerApiKey)
    .contentType(MediaType.APPLICATION_JSON)
    .body(req)
    .retrieve()
    .toBodilessEntity();
// ← 타임아웃 미설정 시 기본값 사용 (연결: 30s, 읽기: 무제한)
```

**개선 권장 (현재 미구현)**:

```java
// 타임아웃이 있는 RestClient 설정
HttpClient httpClient = HttpClient.create()
    .responseTimeout(Duration.ofSeconds(10));
ReactorClientHttpRequestFactory factory =
    new ReactorClientHttpRequestFactory(httpClient);
RestClient restClient = RestClient.builder()
    .requestFactory(factory)
    .build();
```

### 콜백 Retry

cali-worker가 CALI에 콜백을 보낼 때 실패하면?
- 현재 구현: 실패 시 예외 발생 → item 처리 FAIL 처리
- 개선 가능: 최대 3회 retry (지수 백오프)

```java
// 간단한 retry 로직 (직접 구현)
int maxRetry = 3;
for (int i = 0; i < maxRetry; i++) {
    try {
        callbackToCALI(itemId, status, step, message);
        break;
    } catch (Exception e) {
        if (i == maxRetry - 1) throw e;
        Thread.sleep(1000 * (long) Math.pow(2, i)); // 1s, 2s, 4s
    }
}
```

---

## 6-4. Idempotency (멱등성)

### 개념

같은 요청을 여러 번 보내도 결과가 동일해야 하는 성질.

### 콜백 중복 수신 대응

네트워크 오류로 cali-worker가 같은 콜백을 2번 보내면?

```
item.status = PROGRESS 상태인데 다시 PROGRESS 콜백이 오면?
→ 상태가 이미 PROGRESS이므로 successCount++를 다시 하면 안 됨
```

**현재 구현 검토 포인트**:
```java
// handleItemCallback에서 status별 처리 시
// SUCCESS 콜백을 2번 받으면 successCount가 2 증가하는 문제 가능
// → item.status가 이미 SUCCESS인 경우 처리 건너뛰기 필요
if (item.getStatus() == JobItemStatus.SUCCESS) {
    return; // 이미 처리 완료된 item — 중복 콜백 무시
}
```

---

## 6-5. Health Check

### cali-worker Health Endpoint

Spring Boot Actuator를 포함하면 자동으로 health endpoint 제공:

```properties
# application.properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```

```bash
# 호출
curl http://localhost:8060/actuator/health
# 응답: {"status":"UP"}
```

### CALI에서 워커 상태 확인 (선택적 구현)

트리거 전에 워커가 살아있는지 확인하는 로직:

```java
// 트리거 전 health check (선택적)
try {
    restClient.get()
        .uri(workerUrl + "/actuator/health")
        .retrieve()
        .toBodilessEntity();
} catch (Exception e) {
    throw new RuntimeException("작업서버가 응답하지 않습니다: " + e.getMessage());
}
```

---

## 6-6. batchId / itemId 추적 (Correlation ID)

### 개념

분산 시스템에서 요청 흐름을 추적하기 위해 사용하는 고유 식별자.

### 이번 프로젝트에서의 추적 체계

```
batch_id: 101          ← 사용자 1회 클릭의 식별자
item_id: 1001, 1002 ...← 개별 성적서 처리의 식별자
report_id: 501 ...     ← 실제 성적서 식별자
```

### 로그에 batchId/itemId 포함하기

```java
// cali-worker 로그 예시
log.info("[batch={}][item={}] 처리 시작 - reportId={}", batchId, itemId, reportId);
log.info("[batch={}][item={}] 템플릿 다운로드 완료", batchId, itemId);
log.error("[batch={}][item={}] 처리 실패 - {}", batchId, itemId, e.getMessage());
```

```
로그 출력 예:
2026-03-20 10:30:01 INFO  [batch=101][item=1001] 처리 시작 - reportId=501
2026-03-20 10:30:02 INFO  [batch=101][item=1001] 템플릿 다운로드 완료
2026-03-20 10:30:03 INFO  [batch=101][item=1001] 데이터 삽입 완료
2026-03-20 10:30:04 INFO  [batch=101][item=1001] 스토리지 업로드 완료
2026-03-20 10:30:04 INFO  [batch=101][item=1001] 처리 성공
```

---

## 6-7. 핵심 요약 (1~6장)

### 아키텍처
- CALI = 상태 관리 + 트리거 + Polling 응답
- cali-worker = 파일 처리 실행 + 콜백 전송
- batch/item 구조 = 일괄 처리 추적의 기반

### WSL2
- 실제 Linux 커널 → Java 앱 완전 동작
- NAT mode 기본 → localhost:8060으로 Windows ↔ WSL2 통신
- mirrored mode 사용 시 네트워크 더 단순
- 홈서버 이전 시 설정만 변경하면 됨

### 통신 설계
- 트리거: CALI → cali-worker (REST POST)
- 상태 보고: cali-worker → CALI (REST POST 콜백)
- 조회: cali-worker → CALI (REST GET)
- 인증: X-Worker-Api-Key 헤더

### 초보자가 자주 헷갈리는 포인트
1. **WSL2 내부 IP**: 재시작마다 바뀔 수 있음 → mirrored mode 또는 동적 IP 확인 스크립트 필요
2. **콜백 URL**: cali-worker(WSL2)에서 CALI(Windows)를 가리킬 때 `localhost`가 작동 안 할 수 있음
3. **0.0.0.0 바인딩**: 외부에서 접근하려면 앱이 0.0.0.0으로 Listen해야 함
4. **graceful shutdown**: 처리 중 종료 시 item이 PROGRESS 고착 방지

---

*→ Part B (7~12장): Polling/SSE, 배치/상태머신, 트랜잭션, 풀링, Apache POI, PDF 변환*