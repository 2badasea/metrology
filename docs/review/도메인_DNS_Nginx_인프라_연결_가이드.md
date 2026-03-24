# 도메인 · DNS · Nginx · 인프라 연결 가이드

> **작성 계기**: 가비아에서 도메인을 구입하고, NCP(네이버 클라우드) 서버에 연결하는 과정에서 접한 개념들을 CS 학습 목적으로 정리한 문서.
> 도메인 등록, DNS, 네임서버, Nginx 리버스 프록시, HTTPS(Let's Encrypt), Cloudflare까지 순서대로 다룬다.

---

## 목차

1. [사전 지식 — 인터넷 주소 체계](#1-사전-지식--인터넷-주소-체계)
2. [도메인 등록 구조 — Registrar와 Registry](#2-도메인-등록-구조--registrar와-registry)
3. [DNS란 무엇인가](#3-dns란-무엇인가)
4. [네임서버(NS)와 DNS 위임](#4-네임서버ns와-dns-위임)
5. [주요 DNS 레코드 종류](#5-주요-dns-레코드-종류)
6. [DNS 전파(Propagation)](#6-dns-전파propagation)
7. [NCP 서버와 ACG(방화벽)](#7-ncp-서버와-acg방화벽)
8. [Nginx 리버스 프록시](#8-nginx-리버스-프록시)
9. [HTTPS — Let's Encrypt + Certbot](#9-https--lets-encrypt--certbot)
10. [Cloudflare — DNS + CDN + 보안](#10-cloudflare--dns--cdn--보안)
11. [전체 흐름 요약](#11-전체-흐름-요약)
12. [자주 겪는 오류와 해결법](#12-자주-겪는-오류와-해결법)
13. [참고 지식](#13-참고-지식)

---

## 1. 사전 지식 — 인터넷 주소 체계

인터넷에서 서버는 **IP 주소**로 식별된다. 예를 들어 `110.10.20.30` 같은 숫자 조합이 실제 서버의 위치를 나타낸다. 그런데 사람이 숫자를 기억하기 어렵기 때문에, 숫자 대신 `cali-dev.site` 같은 **도메인 이름**을 사용한다.

도메인 이름을 IP 주소로 변환해주는 시스템이 바로 **DNS(Domain Name System)**이다. 브라우저에서 URL을 입력하면 가장 먼저 DNS에 "이 이름의 IP가 뭐야?"라고 물어보고, 응답받은 IP로 실제 요청을 보내는 구조다.

```
사용자 입력: https://cali-dev.site
       ↓
DNS 조회: cali-dev.site → 110.10.20.30
       ↓
HTTP 요청: 110.10.20.30 서버로 연결
```

---

## 2. 도메인 등록 구조 — Registrar와 Registry

도메인을 "구입"한다는 것은 정확히는 **일정 기간 동안 해당 이름의 독점 사용권을 임대**하는 것이다. 도메인 등록 체계는 계층 구조로 이루어져 있다.

### 계층 구조

| 계층 | 명칭 | 역할 | 예시 |
|------|------|------|------|
| 최상위 | ICANN | 인터넷 주소 전체 정책 관리 | - |
| TLD 관리 | Registry | `.com`, `.kr` 등 최상위 도메인 관리 | Verisign(`.com`), KRNIC(`.kr`) |
| 판매 | Registrar | 사용자에게 도메인 판매 | 가비아, Namecheap, GoDaddy |
| 사용자 | - | 도메인 임대 및 사용 | 우리 |

### 주요 등록기관 비교

| 등록기관 | 특징 | 추천 상황 |
|---------|------|---------|
| **가비아** | 국내, 한국어, `.co.kr` 최저가(연 ~4,400원) | 국내 서비스, 처음 시작 |
| **Namecheap** | `.com` 저렴(첫해 $6~9), 깔끔한 UI | 영문 서비스, `.com` 원할 때 |
| **Cloudflare Registrar** | 원가 판매(마진 0), 가장 저렴 | 이미 Cloudflare 쓰는 경우 |
| **GoDaddy** | 유명하지만 갱신 요금 비쌈 | 비추천 |

> **팁**: 도메인 등록기관(Registrar)과 DNS 관리 주체는 분리할 수 있다. 가비아에서 구입하더라도 DNS는 Cloudflare에서 관리하는 식의 조합이 일반적이다.

---

## 3. DNS란 무엇인가

DNS는 인터넷의 전화번호부에 비유할 수 있다. 전화번호부가 이름 → 전화번호를 알려주듯, DNS는 도메인 이름 → IP 주소를 알려준다.

### DNS 조회 과정 (재귀 조회)

브라우저에서 `cali-dev.site`를 입력하면 다음과 같은 과정이 일어난다.

```
① 브라우저 캐시 확인 → 없으면
② OS의 로컬 DNS 캐시 확인 → 없으면
③ ISP의 리졸버(Recursive Resolver)에 질의
④ 리졸버가 Root DNS 서버에 질의 → ".site를 누가 관리해?"
⑤ Root DNS → TLD DNS(.site 관리 서버) 주소 응답
⑥ 리졸버가 TLD DNS에 질의 → "cali-dev.site 네임서버가 뭐야?"
⑦ TLD DNS → 권한 네임서버(Authoritative NS) 주소 응답
⑧ 리졸버가 권한 네임서버에 질의 → "A레코드가 뭐야?"
⑨ 권한 네임서버 → IP 주소 응답
⑩ 브라우저가 해당 IP로 HTTP 요청
```

이 전체 과정이 보통 수십 ms 안에 완료된다. 또한 각 단계에서 TTL 시간 동안 캐시에 저장되므로, 두 번째 방문부터는 훨씬 빠르다.

---

## 4. 네임서버(NS)와 DNS 위임

**네임서버(Name Server)**는 특정 도메인의 DNS 정보를 실제로 보관하고 응답해주는 서버다. 도메인을 등록할 때 "이 도메인의 DNS 정보는 어느 서버가 관리하냐"를 지정하는 것이 네임서버 설정이다.

### 가비아 네임서버 vs 타사 네임서버

가비아에서 도메인 구입 시 두 가지 옵션이 나온다.

| 옵션 | 의미 | DNS 관리 위치 |
|------|------|-------------|
| **가비아 네임서버** | 가비아 DNS 서버가 이 도메인을 관리 | 가비아 DNS 관리 화면 |
| **타사 네임서버** | Cloudflare 등 외부 DNS가 관리 | 해당 타사 서비스 화면 |

처음 시작하는 경우라면 **가비아 네임서버**를 선택하고 나중에 필요하면 Cloudflare로 이전하면 된다. 네임서버는 언제든지 변경할 수 있으며, 변경 후 전파까지 최대 48시간이 소요된다.

### Cloudflare로 위임하는 경우

```
1. Cloudflare 가입 → 도메인 추가
2. Cloudflare가 제공하는 NS 주소 확인 (예: xxx.ns.cloudflare.com)
3. 가비아 도메인 관리 → 네임서버를 Cloudflare 값으로 변경
4. 이후 DNS 레코드 관리는 Cloudflare 대시보드에서
```

---

## 5. 주요 DNS 레코드 종류

DNS에는 다양한 레코드 타입이 있다. 각각의 목적이 다르므로 상황에 맞게 사용한다.

| 레코드 | 용도 | 예시 |
|--------|------|------|
| **A** | 도메인 → IPv4 주소 | `cali-dev.site` → `110.10.20.30` |
| **AAAA** | 도메인 → IPv6 주소 | `cali-dev.site` → `2001:db8::1` |
| **CNAME** | 도메인 → 다른 도메인(별칭) | `www` → `cali-dev.site` |
| **MX** | 메일 서버 지정 | 이메일 수신 서버 |
| **TXT** | 텍스트 정보 | 도메인 소유 인증, SPF 설정 등 |
| **NS** | 이 도메인의 네임서버 | `ns1.gabia.net` |
| **TTL** | 레코드 캐시 유지 시간(초) | 300 = 5분 |

### 실제 설정 예시 (NCP 서버 연결)

| 타입 | 호스트 | 값 | TTL |
|------|--------|-----|-----|
| A | `@` | 서버 공인 IP | 300 |
| A | `www` | 서버 공인 IP | 300 |

- `@`는 루트 도메인(`cali-dev.site`) 자체를 의미한다.
- `www`는 `www.cali-dev.site`를 의미한다.
- TTL은 초기에 300(5분)으로 짧게 설정하고, 안정화되면 3600(1시간)으로 늘려도 된다.

---

## 6. DNS 전파(Propagation)

DNS 레코드를 변경하면 **즉시 전 세계에 적용되지 않는다**. 각 DNS 서버가 이전 레코드를 TTL 시간 동안 캐시에 보관하기 때문이다. 이 캐시가 만료되고 새 레코드를 다시 받아오는 과정이 전파(Propagation)다.

- **일반적인 전파 시간**: 30분 ~ 1시간
- **최대**: 48시간 (TTL이 길게 설정된 경우)

### 전파 확인 방법

```bash
# 내 PC에서 확인
nslookup cali-dev.site

# 내 PC DNS 캐시 초기화 (Windows)
ipconfig /flushdns

# 내 PC DNS 캐시 초기화 (Mac/Linux)
sudo dscacheutil -flushcache   # Mac
sudo systemd-resolve --flush-caches  # Linux
```

**외부에서 확인**: [https://dnschecker.org](https://dnschecker.org) — 전 세계 수십 개 DNS 서버에서 동시에 조회 결과를 확인할 수 있다.

---

## 7. NCP 서버와 ACG(방화벽)

NCP(Naver Cloud Platform)는 AWS와 유사한 국내 클라우드 서비스다. 서버에 외부에서 접근하려면 두 가지가 필요하다.

### 공인 IP

NCP 서버는 기본적으로 사설 IP만 가진다. 외부 인터넷에서 접근하려면 **공인 IP를 별도로 신청**해야 한다.

```
NCP 콘솔 → Server → 공인 IP 관리 → 공인 IP 신청 → 서버에 할당
```

### ACG (Access Control Group)

AWS의 Security Group과 동일한 개념이다. 서버로 들어오는/나가는 트래픽을 포트 단위로 제어한다.

| 방향 | 프로토콜 | 포트 | 허용 IP | 용도 |
|------|---------|------|---------|------|
| 인바운드 | TCP | 22 | 관리자 IP | SSH 접속 |
| 인바운드 | TCP | 80 | 0.0.0.0/0 | HTTP |
| 인바운드 | TCP | 443 | 0.0.0.0/0 | HTTPS |
| 인바운드 | TCP | 8050 | 127.0.0.1 | 앱 서버 (내부만) |

> **보안 원칙**: 앱 서버 포트(8050 등)는 외부에 직접 노출하지 않는다. Nginx가 80/443을 받아서 내부로 전달하는 구조가 올바르다.

---

## 8. Nginx 리버스 프록시

### 리버스 프록시란

클라이언트는 Nginx에게 요청을 보내고, Nginx가 대신 백엔드 서버(Spring Boot 등)에 요청을 전달하는 구조다. 클라이언트 입장에서는 Nginx만 보인다.

```
클라이언트 → Nginx(80/443) → Spring Boot(8050)
                ↑
          리버스 프록시
```

### 왜 쓰는가

- 백엔드 포트(8050)를 외부에 노출하지 않아도 됨
- SSL 종료(HTTPS → HTTP 변환)를 Nginx에서 처리
- 여러 앱 서버를 하나의 도메인으로 서빙 가능
- 정적 파일 처리, 로드밸런싱, 캐싱 등 부가 기능

### 설정 예시

```nginx
server {
    listen 80;
    server_name cali-dev.site www.cali-dev.site;

    location / {
        proxy_pass http://127.0.0.1:8050;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 주요 헤더 설명

| 헤더 | 설명 |
|------|------|
| `Host` | 원래 요청의 도메인명 전달 |
| `X-Real-IP` | 실제 클라이언트 IP 전달 |
| `X-Forwarded-For` | 프록시를 거친 경우 원본 IP 체인 |
| `X-Forwarded-Proto` | 원래 프로토콜(http/https) 전달 |

Spring Boot에서 실제 클라이언트 IP를 로그에 남기거나 처리하려면 이 헤더들이 필요하다.

### 502 Bad Gateway

Nginx가 정상 동작하지만 백엔드에 연결하지 못할 때 발생한다.

| 원인 | 확인 방법 |
|------|---------|
| Spring Boot가 꺼져있음 | `ps aux \| grep java` |
| 포트 불일치 | `sudo ss -tlnp \| grep java` |
| proxy_pass 주소 오류 | `curl http://127.0.0.1:8050` |

---

## 9. HTTPS — Let's Encrypt + Certbot

### HTTPS가 필요한 이유

HTTP는 데이터를 평문으로 전송한다. HTTPS는 TLS(Transport Layer Security) 프로토콜로 암호화하여 전송한다. 현대 웹에서는 HTTPS가 사실상 필수다.

- 데이터 도청 방지
- 브라우저 "주의 요함" 경고 없음
- SEO 점수 향상
- HTTP/2 사용 가능 (성능 향상)

### Let's Encrypt

무료 SSL 인증서를 발급해주는 비영리 기관이다. 인증서 유효기간은 90일이며, Certbot이 자동으로 갱신해준다.

### Certbot 설치 및 발급

```bash
# 설치
sudo apt install certbot python3-certbot-nginx

# 인증서 발급 + Nginx 자동 설정
sudo certbot --nginx -d cali-dev.site -d www.cali-dev.site

# 자동 갱신 테스트
sudo certbot renew --dry-run
```

발급 완료 후 Nginx 설정이 자동으로 HTTPS 블록으로 변경된다.

### 인증서 갱신

Certbot은 설치 시 cron 또는 systemd timer를 자동 등록한다. 별도 작업 없이 만료 30일 전부터 자동 갱신을 시도한다.

```bash
# 갱신 타이머 확인
sudo systemctl list-timers | grep certbot
```

---

## 10. Cloudflare — DNS + CDN + 보안

### Cloudflare란

Cloudflare는 전 세계 300개 이상의 데이터센터를 운영하는 CDN/보안/DNS 서비스다. 무료 플랜만으로도 상당한 기능을 제공한다.

### 주요 기능 (무료 플랜 기준)

| 기능 | 설명 |
|------|------|
| **DNS 관리** | 빠른 DNS 응답, 직관적인 UI |
| **CDN** | 정적 자원을 Cloudflare 엣지 서버에서 캐싱 |
| **DDoS 방어** | 대규모 공격 자동 차단 |
| **SSL/TLS** | Cloudflare ↔ 클라이언트 구간 자동 HTTPS |
| **Firewall Rules** | IP, 국가, User-Agent 기반 차단 |
| **Analytics** | 트래픽 통계 |

### 프록시 모드 (주황색 구름 ☁️)

Cloudflare DNS에서 A레코드 옆의 구름 아이콘을 주황색으로 설정하면 **프록시 모드**가 활성화된다. 이 경우 실제 서버 IP가 외부에 노출되지 않고 Cloudflare IP만 보인다.

```
클라이언트 → Cloudflare 엣지 → 내 서버
                 ↑
         실제 IP 숨김 + DDoS 방어
```

회색 구름이면 DNS만 처리하고 트래픽은 직접 서버로 간다.

### Cloudflare를 쓰는 시나리오

```
1. 가비아에서 도메인 구입
2. Cloudflare에 도메인 추가
3. 가비아 네임서버를 Cloudflare NS로 변경
4. Cloudflare에서 A레코드 등록 + 프록시 모드 ON
5. Cloudflare가 HTTPS, DDoS 방어, CDN 자동 처리
```

### 가비아 DNS vs Cloudflare DNS 비교

| 항목 | 가비아 DNS | Cloudflare DNS |
|------|-----------|---------------|
| 가격 | 무료 | 무료 |
| 응답 속도 | 보통 | 빠름 (글로벌 엣지) |
| DDoS 방어 | 없음 | 기본 제공 |
| CDN | 없음 | 기본 제공 |
| 프록시(IP 숨김) | 없음 | 지원 |
| UI | 한국어 | 영문 |
| 난이도 | 쉬움 | 약간 복잡 |

---

## 11. 전체 흐름 요약

실제 서비스가 동작하는 전체 경로를 한눈에 보면 다음과 같다.

```
사용자 브라우저에서 https://cali-dev.site 입력
         ↓
DNS 조회 (가비아 또는 Cloudflare)
  cali-dev.site → NCP 공인 IP (110.x.x.x)
         ↓
NCP ACG 인바운드 규칙 통과 (443 포트 허용)
         ↓
Nginx (443, SSL 종료)
  TLS 인증서 검증 → HTTP로 변환
         ↓
Nginx 리버스 프록시
  proxy_pass → http://127.0.0.1:8050
         ↓
Spring Boot 애플리케이션 (8050)
  요청 처리 → 응답 반환
         ↓
역순으로 응답이 사용자에게 전달
```

---

## 12. 자주 겪는 오류와 해결법

### nslookup에서 "Non-existent domain"

| 원인 | 해결 |
|------|------|
| DNS 전파 미완료 | 30분~1시간 대기 |
| A레코드 저장 누락 | 가비아 DNS 설정 재확인 및 저장 |
| 내 PC 캐시 | `ipconfig /flushdns` 후 재시도 |

dnschecker.org에서 외부 확인 먼저 하는 것이 빠른 진단 방법이다.

### 502 Bad Gateway

Nginx는 정상이지만 백엔드 연결 실패를 의미한다.

```bash
# 1. 앱 프로세스 확인
ps aux | grep java

# 2. 포트 확인
sudo ss -tlnp | grep 8050

# 3. 내부 직접 요청 테스트
curl http://127.0.0.1:8050

# 4. Nginx 에러 로그 확인
sudo tail -n 50 /var/log/nginx/error.log
```

### 503 Service Unavailable

Nginx 자체가 과부하 상태이거나 upstream 연결 큐가 가득 찬 경우다.

### 504 Gateway Timeout

백엔드 응답이 너무 느릴 때 발생한다. Nginx의 `proxy_read_timeout` 값을 늘리거나 백엔드 성능을 확인한다.

---

## 13. 참고 지식

### OSI 7계층과 관련 위치

| 계층 | 이름 | 관련 기술 |
|------|------|---------|
| 7 | 응용 | HTTP, HTTPS, DNS |
| 6 | 표현 | TLS/SSL 암호화 |
| 4 | 전송 | TCP (포트 개념) |
| 3 | 네트워크 | IP 주소 |

### 포트 번호 관례

| 포트 | 프로토콜 | 용도 |
|------|---------|------|
| 80 | HTTP | 일반 웹 |
| 443 | HTTPS | 암호화 웹 |
| 22 | SSH | 서버 원격 접속 |
| 3306 | MySQL | DB |
| 8080 | - | 개발용 HTTP (관례) |

### TTL (Time To Live)

DNS에서의 TTL은 "이 레코드를 캐시에 몇 초 동안 보관하라"는 지시다. IP 변경이 예상되면 TTL을 낮게 유지하고, 안정화되면 높여서 DNS 질의 부하를 줄인다.

### HTTPS 인증서 종류

| 종류 | 발급 비용 | 특징 |
|------|---------|------|
| **DV (Domain Validation)** | 무료~저렴 | 도메인 소유만 확인. Let's Encrypt |
| **OV (Organization Validation)** | 유료 | 조직 실재 확인 포함 |
| **EV (Extended Validation)** | 고가 | 가장 엄격한 심사, 브라우저 주소창 초록색 |

일반 웹 서비스에는 DV 인증서(Let's Encrypt)로 충분하다.

### CDN (Content Delivery Network)

전 세계 여러 곳에 분산된 서버에 콘텐츠를 캐싱해두고, 사용자와 가장 가까운 서버에서 응답하는 구조다. 정적 파일(이미지, JS, CSS) 로딩 속도를 크게 향상시킨다. Cloudflare는 CDN 기능을 무료로 제공한다.
