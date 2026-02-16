# CALI(교정관리) - Claude Code 작업 규칙 (Root)

이 문서는 Claude Code가 이 저장소에서 작업할 때 지켜야 하는 **공통 규칙/금지사항/완료 기준(DoD)** 정의
`backend/CLAUDE.md`, `frontend/CLAUDE.md`가 더 구체 규칙이며, 충돌 시 **더 하위 규칙이 우선**

---

## 1) 프로젝트 개요(아키텍처)
CALI는 하이브리드 구조의 교정관리 웹 애플리케이션입니다.

- **Backend**: Spring Boot 3.5.x (Java 17) + REST API + Thymeleaf SSR 페이지
- **Frontend(Admin)**: React SPA (Vite) / `/admin/` 경로에서 제공
- **DB**: MySQL 8.x
- **파일 스토리지**: Naver Cloud Platform Object Storage (S3 호환, AWS SDK v2)

> 버전/포트/설정은 바뀔 수 있습니다. 애매하면 `backend/build.gradle`, `frontend/package.json`, `vite.config.js`를 먼저 확인합니다.

---

## 2) 빌드/실행 명령(기본)
### Backend
```bash
cd backend
./gradlew clean bootJar          # JAR 빌드
./gradlew bootRun                # 개발 서버 실행(기본: 8050)
./gradlew test                   # 테스트(JUnit 5)
```

### Frontend(Admin)
```bash
cd frontend
npm install
npm run dev                      # 개발 서버 실행(기본: 3000, /api -> :8050 프록시)
npm run build                    # dist/ 빌드
npm run lint                     # ESLint
```

---

## 3) 작업 범위 원칙(가장 중요)
- **요청받은 범위만 수정**합니다. “같이 고치면 좋아 보이는 것”은 **제안만** 하고 임의 리팩토링 금지.
- 변경은 작은 단위로 쪼개서 **diff가 과도하게 커지지 않게** 합니다.
- “UI만” 요청이면 **API/비즈니스/DB/연동 로직은 구현하지 않습니다**(UI/구조/그리드 정의까지만).

---

## 4) 사전 승인 없이는 절대 하지 말 것(금지/주의)
다음 항목은 반드시 사용자 승인 후 진행합니다.

- DB 스키마/DDL 변경(테이블/컬럼/인덱스/제약)
- 보안 정책 변경(인증/인가, CSRF, CORS, Security 설정 큰 변경)
- 의존성 추가/버전 변경(backend: `build.gradle`, frontend: `package.json`)
- 코어 동작/공통 계약 변경(전역 라우팅, 공통 레이아웃 규칙, 공통 유틸 함수 계약 변경 등)

필요해 보이면 먼저:
1) 필요 이유 2) 대안 3) 영향 범위 를 요약해서 승인 받습니다.

---

## 5) Git 규칙(필수)
- `git commit`, `git push`, merge/rebase는 **절대 자동 실행 금지**.
- 작업 종료 시 항상 아래를 제공:
  - 변경 파일 목록(경로)
  - 핵심 변경 요약(3~7줄)
  - `git status` 기준으로 커밋 가능 상태 안내
  - 검증 방법(빌드/실행/화면 체크)
  - 권장 커밋 메시지(제안만)

---

## 6) 네이밍/코드 컨벤션(공통)
- 신규 코드/함수/변수는 **camelCase** 사용.
- 스네이크 케이스(언더바 포함) 신규 도입 금지.
- 기존 스네이크 케이스 정리는 **단계적 마이그레이션** 원칙:
  - 전역 치환 금지
  - 호환 래퍼 제공(예: `gAjax()`가 내부에서 기존 `g_ajax()` 호출)

---

## 7) 보안/개인정보/품질
- 비밀정보(키/토큰/비번/내부 URL/IP)는 코드/로그/문서에 절대 하드코딩 금지.
- 사용자에게 스택트레이스/내부 정보 노출 금지.
- 변경 후 가능한 범위에서 **검증 방법**을 함께 제시합니다.

---

## 8) (선택) 인프라 메모(환경별/민감 가능)
- CI/CD, 배포 경로, 버킷명, 서버 정보 등은 **환경별로 달라질 수 있는 민감 정보**입니다.
- 저장소 공개 가능성이 있으면 상세값은 일반화하거나 별도 문서로 분리합니다.

---

## 9) 작업 완료 기준(DoD)
모든 작업 응답은 마지막에 반드시 포함합니다.

- Changed files: (경로 목록)
- Summary: (핵심 변경 3~7줄)
- How to verify:
  - (backend) 빌드/실행/간단 점검
  - (frontend) 실행/화면 점검
- Notes/Risks: (주의점/추가 확인 필요 사항)

## 10) 사용 언어, 라이브러리 및 프레임워크
- JAVA의 경우 17버전이니까, 그이상 버전에서 나온 문법은 사용하지 않을 것
- 프론트엔드의 경우, 어드민페이민을 제외하고는 제이쿼리 위주로 작성. 필요 시 순수 JS스크립트를 사용하되, ES6버전 문법까지만 허용
- 어드민페이지의 경우 리액트 및 순수JS 사용할 것. (타입스크립트 사용X)
- JPA와 QUERYDSL를 위주로 사용하되, 사용자의 직접적인 요청이 있을 때만 mybatis 사용할 것

---