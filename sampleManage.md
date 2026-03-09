# [샘플관리 메뉴(sampleManage)] 작업 요청서

## 참고
- 이미 페이지와 그리드 구조는 생성되어 있음
- 내가 정의한 기능을 구현하면서 기존의 구조를 수정할 필요가 있다면 제안할 것
---

## 1. 개요

- **기능명**: 샘플관리 메뉴 및 기능 정의
- **메뉴 경로**: 기준정보관리 > 샘플관리
- **URL 경로**: `/sample/sampleManage`
- **페이지 유형**: 독립 페이지
- **접근 권한**: 전체

---

## 2. DB 스키마

> schema.sql 기준으로 작성. DDL 형태로 작성하면 오독 방지에 좋음.

```sql
CREATE TABLE sample (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL COMMENT '샘플명',
    status         TINYINT       NOT NULL DEFAULT 0 COMMENT '상태 (0: 활성, 1: 비활성)',
    remark         VARCHAR(500)  NULL COMMENT '비고',
	`is_visible` ENUM('y','n') NOT NULL DEFAULT 'y' COMMENT '삭제 유무 - y: 노출됨(삭제되지 않음),  n: 삭제처리(노출되지 않음)' COLLATE 'utf8mb4_0900_ai_ci',
	`create_datetime` DATETIME NOT NULL DEFAULT (now()) COMMENT '등록일시',
	`create_member_id` BIGINT NOT NULL COMMENT '등록자',
	`update_datetime` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
	`update_member_id` BIGINT NULL DEFAULT NULL COMMENT '수정자',
	`delete_datetime` DATETIME NULL DEFAULT NULL COMMENT '삭제일시',
	`delete_member_id` BIGINT NULL DEFAULT NULL COMMENT '삭제자',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `is_visible` (`is_visible`) USING BTREE,
);
```

**기존 테이블과의 연관관계**

| 컬럼 | 참조 테이블 | 참조 컬럼 | 설명 |
|------|------------|----------|------|
| create_member_id | member | id | 등록자 |
| (추가 FK가 있으면 여기에) | | | |

---

## 3. 화면 구성

### 3-1. 레이아웃

- (예: 상단 검색 필터 + 하단 Toast Grid 목록)
- (예: 목록 행 클릭 시 등록/수정 페이지로 이동 or 모달 오픈)

### 3-2. 검색 필터

| 필드명 | UI 타입 | 옵션/설명 |
|--------|---------|----------|
| 상태   | select  | 전체 / 활성 / 비활성 |
| 검색어 | text    | 샘플명 검색 |

### 3-3. 그리드 컬럼 (목록)

| 헤더 | 필드명 | 너비 | 정렬 | 비고 |
|------|--------|------|------|------|
| 샘플명 | name | - | center | 클릭 시 상세 이동 |
| 상태   | status | 100 | center | 활성/비활성 포맷터 |
| 등록일 | createDatetime | 160 | center | |

### 3-4. 등록/수정 폼 필드

| 필드명 | UI 타입 | 필수 | 유효성 | 설명 |
|--------|---------|------|--------|------|
| 샘플명 | text    | Y    | 최대 100자 | |
| 상태   | select  | Y    | - | 활성/비활성 |
| 비고   | textarea | N   | 최대 500자 | |

### 3-5. 버튼 구성

| 버튼명 | 위치 | 동작 | 권한 제한 |
|--------|------|------|----------|
| 등록   | 목록 우상단 | 등록 페이지 이동 | - |
| 삭제   | 목록 우상단 | 체크된 행 삭제 | admin |
| 저장   | 등록/수정 폼 | API 호출 후 목록 복귀 | - |
| 취소   | 등록/수정 폼 | 목록으로 이동 | - |


---

## 4. 비즈니스 규칙 / 특이사항

- (예: 이미 사용 중인 샘플은 삭제 불가 → 400 에러 반환)
- (예: 상태가 '비활성'이면 목록에서 회색 처리)
- (예: 파일 첨부 기능 포함 — 확장자 제한: jpg, png, pdf / 최대 10MB)
- (예: 등록/수정/삭제 시 log 테이블 기록 필요)

---

## 5. 참고 / 기타

- (예: 기존 품목관리(item) 페이지 구조를 참고해서 만들어줘)
- (예: 목록 페이지는 모달이 아닌 독립 페이지로 구성)
- (예: 특정 컬럼은 관리자만 편집 가능)
