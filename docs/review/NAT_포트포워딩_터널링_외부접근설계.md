# NAT, 포트포워딩, 터널링 — 외부에서 내 서버에 접근하는 법

> 계기: CALI(NCP 개발서버) → 워커(WSL2 또는 홈서버) 트리거 요청 시,
> "WSL2는 NAT 뒤에 있어서 직접 접근이 안 된다"는 말이 무슨 뜻인지 이해하기 위해 작성.

---

## 1. 선행 개념: IP 주소가 부족하다는 문제

인터넷에 연결된 모든 기기는 고유한 IP 주소가 있어야 통신할 수 있다.
그런데 전 세계 기기 수에 비해 IPv4 주소(예: `192.168.x.x`)의 수는 약 43억 개뿐이고,
이미 오래전에 부족해졌다.

이 문제를 해결하기 위해 등장한 개념이 **NAT**이다.

---

## 2. NAT (Network Address Translation) 이란?

**NAT = 여러 기기가 "하나의 공인 IP"를 공유하는 기술**

### 공인 IP vs 사설 IP

| 구분 | 예시 | 특징 |
|---|---|---|
| **공인 IP** (Public IP) | `203.x.x.x`, `175.x.x.x` | 인터넷에서 직접 접근 가능. 전 세계 유일 |
| **사설 IP** (Private IP) | `192.168.x.x`, `10.x.x.x`, `172.16~31.x.x` | 내부망에서만 사용. 외부에서 직접 접근 불가 |

### 집 네트워크 구조

```
인터넷
  │
  │  공인 IP: 123.45.67.89  ← ISP(KT, LG 등)가 부여
  │
[공유기 / 라우터]  ← NAT 장비
  │
  ├── 내 PC      : 192.168.0.10  (사설 IP)
  ├── 핸드폰     : 192.168.0.11  (사설 IP)
  ├── WSL2       : 172.20.x.x    (사설 IP, 또 다른 가상 네트워크)
  └── 미니PC     : 192.168.0.20  (사설 IP)
```

### NAT이 하는 일

내 PC(192.168.0.10)가 구글(142.x.x.x)에 요청을 보낼 때:

```
[내 PC] ──요청──▶ [공유기] ──요청──▶ [구글]
192.168.0.10:5000    공인IP:5000      142.x.x.x
```

1. 내 PC가 `192.168.0.10:5000`에서 요청을 보냄
2. 공유기가 "발신자 주소"를 `공인IP:5000`으로 바꿔서 내보냄 (주소 변환)
3. 구글이 `공인IP:5000`으로 응답을 보냄
4. 공유기가 이걸 다시 `192.168.0.10:5000`으로 전달

> **핵심**: 구글 입장에서 요청자는 `공인IP`로만 보인다. 내 PC의 `192.168.0.10`은 모른다.

### 왜 "외부에서 직접 접근이 안 되나"

외부(NCP 서버)에서 내 WSL2에 접근하려면 목적지 주소가 필요하다.
그런데 WSL2의 IP는 `172.20.x.x`라는 **사설 IP**다.

```
[NCP 서버] ──요청 시도──▶ ???
              목적지: 172.20.x.x:8080
              → 이 주소는 인터넷 어디에도 없다
              → 라우팅 불가, 패킷 유실
```

사설 IP는 "그 네트워크 안에서만" 의미가 있는 주소다.
외부에서 사설 IP로 직접 요청을 보내는 건 "서울에서 '우리 동네 편의점'이라는 주소로 택배 보내기"와 같다.

---

## 3. WSL2의 네트워크 구조 — 이중 NAT

WSL2는 일반 사설 IP보다 한 단계 더 복잡하다.

```
인터넷
  │
  │  공인 IP: 123.45.67.89
  │
[공유기] — NAT 1단계
  │
  └── 윈도우 PC: 192.168.0.10
        │
        └── [Hyper-V 가상 스위치] — NAT 2단계 (윈도우가 만드는 가상 공유기)
              │
              └── WSL2: 172.20.x.x   ← 여기에 워커 서버가 실행됨
```

공인 IP에서 WSL2까지 가려면 **2개의 NAT을 통과**해야 한다.

- NAT 1단계: `공인IP → 윈도우 PC IP` — 공유기가 처리 (포트포워딩으로 해결 가능)
- NAT 2단계: `윈도우 PC IP → WSL2 IP` — 윈도우 Hyper-V가 처리

> WSL2를 재시작할 때마다 `172.20.x.x` 주소가 바뀌는 것도 이 구조 때문이다.

### 반면 홈서버(미니PC)는?

홈서버는 윈도우 → 가상화 계층 없이 공유기에 직접 연결된다.

```
[공유기] — NAT 1단계만 존재
  │
  └── 미니PC(홈서버): 192.168.0.20  ← NAT 1단계만 통과하면 됨
```

NAT이 1단계뿐이라 해결 방법이 훨씬 단순하다.

---

## 4. 해결 방법 A — 포트포워딩 (홈서버에 적합)

**포트포워딩 = "공인IP의 특정 포트로 들어오는 요청을 사설 IP로 전달하라"는 공유기 규칙**

```
[NCP 서버] ──요청──▶ [공유기 123.45.67.89:8080]
                              │ 포트포워딩 규칙:
                              │ 8080 → 192.168.0.20:8080
                              ▼
                        [홈서버(미니PC): 192.168.0.20:8080]
```

### 공유기 포트포워딩 설정 방법

1. 공유기 관리자 페이지 접속 (보통 `192.168.0.1` 또는 `192.168.1.1`)
2. "포트포워딩" 또는 "가상 서버" 메뉴
3. 규칙 추가:
   - 외부 포트: 8080 (NCP가 요청할 포트)
   - 내부 IP: 192.168.0.20 (홈서버 사설 IP)
   - 내부 포트: 8080 (워커가 실제로 열려 있는 포트)

### DDNS (Dynamic DNS) — 공인 IP가 바뀌는 문제 해결

가정집의 공인 IP는 ISP가 주기적으로 바꿀 수 있다 (동적 IP).
NCP 서버에서 워커 URL을 `http://123.45.67.89:8080`으로 설정해뒀는데,
공인 IP가 바뀌면 연결이 끊어진다.

**DDNS = "내 동적 IP를 고정 도메인으로 접근할 수 있게 해주는 서비스"**

```
공인 IP가 바뀌어도:
  my-home-server.ddns.net → 항상 현재 공인 IP를 가리킴
```

- 무료 서비스: No-IP, DuckDNS, Cloudflare DDNS
- 공유기 자체 DDNS 기능을 제공하기도 함 (ipTIME: `xxxx.iptime.org`)

### 홈서버 설정 체크리스트

```
[ ] 공유기 포트포워딩 설정 (외부 포트 → 홈서버 IP:포트)
[ ] DDNS 등록 (or 고정 공인 IP 사용)
[ ] 홈서버 방화벽에서 해당 포트 허용 (ufw allow 8080 등)
[ ] CALI의 app.worker.url = http://[DDNS 도메인]:8080
```

---

## 5. 해결 방법 B — 터널링 (WSL2에 적합)

WSL2는 이중 NAT 구조라 포트포워딩만으로는 번거롭다.
(공유기 포트포워딩 + 윈도우 포트 프록시 두 단계가 필요)

**터널링 = "내 로컬 포트를 공개 URL로 연결해주는 중계 서비스"**

터널링 서비스가 중간에 중계 서버를 두고, 연결을 양방향으로 이어준다.

```
[NCP 서버] ──요청──▶ [터널링 중계 서버 (ngrok/cloudflare)]
                              │  (이미 연결된 터널 통해 전달)
                              ▼
                    [WSL2 내 워커: localhost:8080]
```

터널링 프로그램이 WSL2 안에서 중계 서버와 **아웃바운드(밖으로 나가는) 연결**을 유지하고 있다.
NAT는 아웃바운드 연결은 허용하기 때문에 이 연결이 가능하다.
외부 요청이 오면 중계 서버가 이 기존 연결을 통해 전달한다.

### ngrok 사용법 (가장 빠름)

```bash
# WSL2에서 설치
curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc
echo "deb https://ngrok-agent.s3.amazonaws.com buster main" | sudo tee /etc/apt/sources.list.d/ngrok.list
sudo apt update && sudo apt install ngrok

# 계정 인증 (ngrok.com에서 authtoken 발급)
ngrok authtoken [YOUR_TOKEN]

# 워커(8080 포트)를 외부로 노출
ngrok http 8080
```

실행하면:
```
Forwarding  https://abc123.ngrok-free.app -> http://localhost:8080
```

이 `https://abc123.ngrok-free.app` 주소를 CALI의 `app.worker.url`에 설정하면 끝.

**단점**: 무료 플랜은 URL이 재시작할 때마다 바뀐다. 유료 플랜에서 고정 도메인 제공.

### Cloudflare Tunnel (안정성 높음, 무료)

```bash
# WSL2에서 설치
curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
sudo dpkg -i cloudflared.deb

# 계정 인증 및 터널 생성
cloudflared tunnel login
cloudflared tunnel create cali-worker
cloudflared tunnel route dns cali-worker worker.my-domain.com

# 실행
cloudflared tunnel run --url http://localhost:8080 cali-worker
```

Cloudflare에 도메인이 있으면 `worker.my-domain.com` 형태의 고정 URL을 무료로 사용 가능.

---

## 6. 방법 비교 요약

| 방법 | 대상 | 설정 난이도 | 안정성 | 비용 |
|---|---|---|---|---|
| **포트포워딩 + DDNS** | 홈서버(미니PC) | 중간 | 높음 | 무료 |
| **ngrok** | WSL2, 홈서버 모두 | 쉬움 | 중간 (URL 변경) | 무료(일부), 유료 |
| **Cloudflare Tunnel** | WSL2, 홈서버 모두 | 중간 | 높음 (고정 URL) | 무료 |
| **윈도우 포트 프록시** | WSL2 → 윈도우 경유 | 복잡 | 낮음 (WSL2 재시작 시 IP 변경) | 무료 |

---

## 7. 이 프로젝트(CALI)에서의 실제 적용

### 환경별 `app.worker.url` 설정

| CALI 위치 | 워커 위치 | 설정 방법 |
|---|---|---|
| 로컬 | 로컬 (같은 PC) | `http://localhost:8080` |
| 로컬 | WSL2 | `http://[WSL2 IP]:8080` (WSL2 IP는 `ip addr` 명령으로 확인) |
| NCP 서버 | WSL2 | ngrok 또는 Cloudflare Tunnel URL |
| NCP 서버 | 홈서버(미니PC) | `http://[DDNS 도메인]:8080` (포트포워딩 설정 후) |

### `app.cali.callback-base-url` 설정

워커가 CALI에 콜백할 때 사용하는 CALI 서버 주소.

| CALI 위치 | 설정값 |
|---|---|
| 로컬 (`localhost:8050`) | `http://localhost:8050` (워커도 로컬이면 가능) |
| NCP 서버 | `http://[NCP 서버 공인 IP 또는 도메인]:8050` |

> NCP 서버는 공인 IP가 고정이므로 DDNS가 필요 없다.
> NCP 서버 방화벽(ACG)에서 8050 포트 인바운드 허용 여부를 반드시 확인할 것.

### WSL2 IP 확인 방법

```bash
# WSL2 터미널에서
ip addr show eth0 | grep 'inet ' | awk '{print $2}'
# 또는
hostname -I
```

윈도우 측에서 WSL2 IP 확인:
```powershell
wsl hostname -I
```

---

## 8. 참고: 방화벽도 체크해야 한다

NAT를 해결해도 방화벽이 막으면 안 된다.

| 위치 | 확인할 것 |
|---|---|
| 공유기 | 포트포워딩 외에 방화벽 규칙도 확인 |
| 윈도우 방화벽 | 해당 포트 인바운드 허용 (제어판 → Windows Defender 방화벽) |
| WSL2 내부 | `sudo ufw allow 8080` (ufw가 활성화된 경우) |
| NCP 서버(ACG) | CALI의 8050 포트, 워커가 콜백 보낼 포트 허용 |

---

## 9. 한 줄 요약

- **NAT**: 여러 기기가 하나의 공인 IP를 공유하는 기술. 덕분에 내부 기기는 외부에서 바로 접근 안 됨.
- **WSL2**: NAT가 2겹(공유기 + Hyper-V)이라 더 복잡.
- **홈서버**: NAT 1겹이므로 공유기 포트포워딩 + DDNS로 해결.
- **WSL2에 외부 접근**: ngrok 또는 Cloudflare Tunnel(터널링) 사용이 가장 편함.
- **이 프로젝트 핵심 설정**: `app.worker.url`(CALI가 워커 찾는 주소)과 `app.cali.callback-base-url`(워커가 CALI 찾는 주소) 두 가지.
