# Backend(Spring Boot) - Claude 작업 규칙

## 1) 핵심 구조/주의사항

### (중요) Dual Controller 패턴
백엔드는 “같은 클래스명”을 서로 다른 패키지에 생성 가능

- `com.bada.cali.controller.*` : Thymeleaf SSR 페이지 컨트롤러(뷰 네임 반환)
- `com.bada.cali.api.*` : REST API 컨트롤러(JSON 반환)

컨트롤러 수정 시 **반드시 어떤 패키지인지 확인**

### 패키지 구조(`com.bada.cali`)
- `entity/` : JPA 엔티티(도메인 모델)
- `repository/` : Spring Data JPA
  - `repository/projection/` : 목록/조회용 Projection 인터페이스
- `service/` : 비즈니스 로직(`*ServiceImpl.java`, 별도 인터페이스 없음)
- `dto/` : DTO(요청/응답)
- `mapper/` : MapStruct 매퍼
- `config/` : Security/스토리지/예외 처리 등
- `common/enums/` : 업무 enum

### DB 설계에서 변경할 부분이 있으면 제안할 것
- DB 스키마는 docs/db/schema.sql을 단일 기준(source of truth)으로 참고
- DDL 변경은 사전 승인 없이는 금지.

---

## 2) REST API 규칙
- 조회: GET
- 등록: POST
- 수정: PATCH(부분 수정 우선) / PUT(전체 교체일 때만)
- 삭제: DELETE
- 등록/수정을 하나의 엔드포인트에서 분기 처리하고 있다면 **REST 규칙에 맞게 분리**

### URI 규칙(권장)
- 복수형 자원: `/api/.../members`
- 단건: `/api/.../members/{id}`
- 하위 자원: `/api/.../members/{id}/roles`

### 상태코드(권장)
- POST 생성: 201 (+ 필요 시 Location)
- DELETE 성공: 204 또는 200(프로젝트 표준에 따름)
- 검증 실패: 400(표준 에러 응답)
- 인증/인가: 401/403

---

## 3) Swagger(OpenAPI) 규칙
- `@Controller` / `@RestController`는 Swagger 사용을 전제로 합니다.
- 최소 권장:
  - Controller 메서드: `@Operation`(요약/설명)
  - DTO 필드: `@Schema`
  - 파라미터: `@Parameter`(필요 시)
- 표준 에러 응답(ErrorResponse)이 있으면 문서에 포함시키는 방향을 우선
- 운영 노출 정책(Swagger UI 접근)은 임의로 변경하지 않습니다(사전 승인).

---

## 4) 트랜잭션 원칙
- 기본 원칙
  - 조회 로직은 `@Transactional`을 기본적으로 사용하지 않습니다.
  - 등록/수정/삭제는 `@Transactional` 사용.
- JPA 현실 규칙(예외 기준)
  - 조회는 projection/fetch join/DTO 매핑으로 Lazy 이슈를 피하는 것을 우선합니다.
  - 정말 필요할 때만 `@Transactional(readOnly = true)`를 제한적으로 허용합니다.
- 트랜잭션 경계는 Service 레이어에 둡니다(Controller 남발 금지).

---

## 5) 등록/수정/삭제 로그 정책
- 등록/수정/삭제는 모두 로그를 남깁니다.
- 저장소: `log` 테이블 (스키마 변경은 사전 승인)
- `refTableId`
  - 등록: 생성된 ID
  - 수정: 수정된 ID
  - 삭제: 삭제된 ID
- `contents`
  - 고유번호 목록 포함: `고유번호 - [1, 11, 14]`
- 민감정보/개인정보/토큰/비번은 contents에 절대 기록 금지.
- 대량 처리에서 병목이면 개선안 제안까지만(비동기/배치 구조 변경은 사전 승인).

---

## 6) DTO / Entity 분리 규칙
- DTO와 Entity는 명확히 분리합니다.
- DTO 구성(권장)
  - 엔티티별 대표 DTO: `MemberDTO`
  - 요청/응답: Inner class
    - `MemberDTO.MemberCreateReq`
    - `MemberDTO.MemberUpdateReq`
    - `MemberDTO.MemberDetailRes`
- 간단한 요청/응답은 `record` 사용 가능.
- 목록 조회 응답은 Projection 인터페이스 활용:
  - 위치: `repository/projection`
  - 네이밍: `{Domain}{UseCase}Row` (예: `MemberListRow`)
- Toast Grid 전용 응답 DTO는 필요 시 별도 분리하되, 상속/계층은 과도하게 만들지 않습니다.

---

## 7) 검증/에러 처리
- 요청 DTO는 Bean Validation(@NotNull 등) + Controller에서 `@Valid`.
- 에러 응답은 가능한 한 표준 포맷으로 통일합니다.
- 401/403은 프론트에서 처리 가능한 형태(코드/메시지)로 내려줍니다.
- 예외를 무시하지 말고, “의미 있는 메시지 + 로그”로 처리합니다(스택트레이스 노출 금지).

---

## 8) 주석 작성 원칙(과도 주석 방지)
- 필수: 공개 API(Controller), 복잡한 규칙, 트랜잭션 경계/경계조건/주의점
- 선택: 단순 CRUD(코드 자체가 충분히 설명되면 생략 가능)
- 주석은 “무엇”보다 “왜/제약/주의점/경계조건” 중심

---

## 9) 보안 관련 메모(임의 변경 금지)
- 로그인 페이지: `/member/login`
- 로그인 처리 URL: `/api/member/login` (Security 필터에서 처리, 컨트롤러 아님)
- 공개 경로/권한/DELETE 제한 등은 `SecurityConfig`를 확인한 뒤 작업합니다.
- 보안 정책 변경은 사전 승인 없이는 금지합니다.

---

## 10) 타임리프 기반 화면
- 부트스트랩을 활용하여 반응형으로 동작하도록 구현
- 타임리프 레이아웃 구조를 최대한 활용하되, 구조에서 개선방향이 있다면 제안해줄 것