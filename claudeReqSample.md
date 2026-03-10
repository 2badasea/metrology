# [기능명] 작업 요청서

> 이 파일은 Claude Code에 새 페이지/기능 작업을 지시할 때 사용하는 템플릿입니다.
> 필요 없는 섹션은 삭제하거나 "해당 없음"으로 표시하세요.
 > Claude Code에 전달하는 것을 전제로 작성되었습니다.

---

## 0. 용어 정의

| 용어 | 설명 |
|------|------|
| 샘플 | 교정 대상 기기. 중분류·소분류·기기명으로 분류됨 |
| 중분류 / 소분류 | `item_code` 테이블 참조 (code_level = MIDDLE / SMALL) |
| 파일 | 샘플에 첨부되는 Excel 파일(.xls/.xlsx/.xlsm). `file_info` 테이블에 연결 |

---

## 1. DB 스키마

### 1-1. 신규 테이블: `sample`

```sql
CREATE TABLE `sample` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT,
  `name`                varchar(200) NOT NULL          COMMENT '기기명',
  `middle_item_code_id` bigint       DEFAULT NULL      COMMENT 'item_code.id (code_level=MIDDLE)',
  `small_item_code_id`  bigint       DEFAULT NULL      COMMENT 'item_code.id (code_level=SMALL)',
  `is_visible`          enum('y','n') NOT NULL DEFAULT 'y' COMMENT 'y=노출(삭제안됨), n=소프트삭제',
  `create_datetime`     datetime     NOT NULL DEFAULT (now()),
  `create_member_id`    bigint       NOT NULL,
  `update_datetime`     datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `update_member_id`    bigint       DEFAULT NULL,
  `delete_datetime`     datetime     DEFAULT NULL,
  `delete_member_id`    bigint       DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `is_visible` (`is_visible`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='샘플(기기)관리';
```

### 1-2. 파일 연결 방식

기존 `file_info` 테이블 재사용. 별도 컬럼 추가 없음.

```
file_info.ref_table_name = 'sample'
file_info.ref_table_id   = sample.id
```

### 1-3. 버전 파일

- `docs/db/versions/v_YYMMDD.sql` 에 위 DDL만 (delta) 작성
- `docs/db/schema.sql` (전체 스키마) 에도 추가

---

## 2. 백엔드 구현

> 패키지: `com.bada.cali`

### 2-1. Entity — `Sample.java`

- `@Entity`, `@Table(name = "sample")`
- 필드: `id`, `name`, `middleItemCodeId(Long)`, `smallItemCodeId(Long)`, `isVisible(YnType)`
- Audit 필드: `createDatetime`, `createMemberId`, `updateDatetime`, `updateMemberId`, `deleteDatetime`, `deleteMemberId`
- `isVisible` 기본값: `YnType.Y`
- `@ManyToOne` 연관관계 **사용하지 않음** (id만 보유)

### 2-2. Projection — `SampleListRow.java` (`repository/projection/`)

목록 그리드에 필요한 필드만 조회.

```java
public interface SampleListRow {
    Long getId();
    String getName();
    Long getMiddleItemCodeId();
    String getMiddleCodeNum();   // item_code.code_num (중분류)
    String getMiddleCodeName();  // item_code.code_name (중분류)
    Long getSmallItemCodeId();
    String getSmallCodeNum();    // item_code.code_num (소분류)
    String getSmallCodeName();   // item_code.code_name (소분류)
}
```

### 2-3. DTO — `SampleDTO.java`

Inner class 구성:

```
SampleDTO
├── GetListReq     : page, pageSize, middleItemCodeId, smallItemCodeId, keyword (기기명 검색)
├── SampleSaveReq  : middleItemCodeId(Long), smallItemCodeId(Long), name(String) — @NotNull, @NotBlank 필수
├── DeleteReq      : ids(List<Long>)
└── DuplicateCheckRes : exists(boolean), existingId(Long, nullable)
```

### 2-4. DTO — `FileInfoDTO.FileListRes`

파일 목록에 필요한 필드 (기존 파일이면 확인 후 재사용, 없으면 신규 추가):

```
id(Long), originName(String), writerName(String), createDatetime(LocalDateTime)
```

### 2-5. Repository — `SampleRepository.java`

필요 메서드:

```
① Page<SampleListRow> findSampleList(middleItemCodeId, smallItemCodeId, keyword, Pageable)
   → nativeQuery, is_visible='y' 조건, item_code 2회 LEFT JOIN
② Optional<Sample> findByIdAndIsVisible(Long id, YnType isVisible)
③ boolean existsByMiddleItemCodeIdAndSmallItemCodeIdAndNameAndIsVisible(...)
④ Optional<Sample> findByMiddleItemCodeIdAndSmallItemCodeIdAndNameAndIsVisible(...)  ← 중복 체크 후 기존 ID 반환용
⑤ @Modifying void softDeleteByIds(List<Long> ids, LocalDateTime deleteDatetime, Long deleteMemberId)
```

`FileInfoRepository`에 아래 메서드 추가 (없으면):

```
List<FileInfoDTO.FileListRes> getFileInfosWithJoinOrderByDateDesc(
    String refTableName, Long refTableId, YnType isVisible)
→ JPQL, FileInfo JOIN Member, createDatetime DESC 정렬
```

### 2-6. Service — `SampleServiceImpl.java`

| 메서드 | 트랜잭션 | 설명 |
|--------|---------|------|
| `getSampleList(GetListReq)` | readOnly | 목록 페이징 → TuiGridDTO.Res 반환 |
| `getSampleFiles(Long sampleId)` | readOnly | 파일 목록 최신순, sampleId 존재 검증 |
| `checkDuplicate(middleId, smallId, name)` | readOnly | 동일 조합 존재 여부 + existingId 반환 |
| `createSample(SampleSaveReq, MultipartFile, user)` | - | 등록 + 파일 저장(선택) + log(i) |
| `updateSample(Long id, SampleSaveReq, MultipartFile, user)` | - | 수정 + 파일 추가(선택) + log(u) |
| `deleteSamples(DeleteReq, user)` | - | 소프트삭제(sample + 연관 file_info) + log(d) |
| `deleteSampleFile(Long fileId, user)` | - | 파일 소프트삭제 + log(d) |

파일 저장 경로: `sample/{sampleId}/`
파일 타입 제한: `.xls / .xlsx / .xlsm`
로그 contents 예시: `고유번호 - [1, 5, 12]`

### 2-7. REST Controller — `api/SampleController.java`

`@RequestMapping("/api/sample")`

| Method | URI | 설명 | 응답코드 |
|--------|-----|------|---------|
| GET | `/list` | 목록 조회 (TuiGrid dataSource) | 200 |
| GET | `/{sampleId}/files` | 파일 목록 | 200, 404 |
| GET | `/checkDuplicate?middleItemCodeId=&smallItemCodeId=&name=` | 중복 확인 | 200 |
| POST | `/` (multipart) | 등록 | 201 |
| PATCH | `/{sampleId}` (multipart) | 수정 | 200, 404 |
| DELETE | `/` (request body) | 일괄 삭제 (ADMIN) | 200 |
| DELETE | `/files/{fileId}` | 파일 삭제 (ADMIN) | 200, 404 |

> DELETE는 `SecurityConfig`에서 ADMIN 전용으로 이미 제한됨. 별도 권한 체크 불필요.

Swagger `@Tag`, `@Operation`, `@ApiResponses` 필수 (`CLAUDE.md B3` 참고).

---

## 3. 프론트엔드 구현

### 3-1. Thymeleaf 페이지

- 파일: `templates/basic/sampleManage.html`
- SSR Controller(`controller/BasicController.java`)에 라우팅 추가:
  `GET /basic/sampleManage` → `"basic/sampleManage"`

### 3-2. JS 파일

- 파일: `static/js/basic/sampleManage.js`
- 상단 `console.log('++ basic/sampleManage.js');` 유지

---

## 4. 화면 레이아웃

```
┌─────────────────────────────────────────────────────────────┐
│ 전체 row (m-0 p-1)                                           │
│ ┌───────────────────────┐  ┌───────────────────────────────┐ │
│ │ 좌측 카드 (col-lg-6)   │  │ 우측 카드 (col-lg-6)          │ │
│ │                       │  │                               │ │
│ │ [삭제]         [검색바] │  │              [신규] [저장]    │ │
│ │ ┌─────────────────┐   │  │ ┌─────────────────────────┐  │ │
│ │ │ 샘플 목록 그리드  │   │  │ │  등록/수정 폼 (table)    │  │ │
│ │ │ (서버사이드 페이징)│   │  │ │  중분류 셀렉트           │  │ │
│ │ │                 │   │  │ │  소분류 셀렉트           │  │ │
│ │ └─────────────────┘   │  │ │  기기명 input           │  │ │
│ │                       │  │ │  파일 업로드 input       │  │ │
│ │                       │  │ └─────────────────────────┘  │ │
│ │                       │  │                               │ │
│ │                       │  │ [파일명 검색 input] [조회]     │ │
│ │                       │  │ ┌─────────────────────────┐  │ │
│ │                       │  │ │ 파일 목록 그리드 (클라이언트)│  │ │
│ └───────────────────────┘  └───────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. 좌측: 샘플 목록 그리드

### 5-1. 검색 영역 (카드 상단)

| 위치 | 컨트롤 | 설명 |
|------|--------|------|
| 좌 | `[삭제]` 버튼 (btn-danger) | 체크된 행 삭제 |
| 우 | 중분류 셀렉트 (100px) | `item_code` (MIDDLE) 전체 목록, 첫 옵션: `value="" 중분류전체` |
| 우 | 소분류 셀렉트 (100px) | 중분류 선택 시 연동 갱신, 첫 옵션: `value="" 소분류전체` |
| 우 | 키워드 input (200px) | 기기명 검색 (`name="keyword"`) |
| 우 | `[검색]` 버튼 (submit) | 그리드 reload |

### 5-2. 그리드 컬럼

| header | name | width | align | 비고 |
|--------|------|-------|-------|------|
| (체크박스) | — | 40 | center | `type:'checkbox'` |
| 번호 | id | 60 | center | |
| 중분류 | middleCodeName | 100 | center | |
| 소분류 | smallCodeName | 100 | center | |
| 기기명 | name | — | left | `resizable:true` |

- 서버사이드 페이징: `dataSource` 방식, 기본 10행/페이지
- 행 클릭: 우측 폼에 데이터 로드 + 파일 그리드 로드
- **같은 행 재클릭 시**: 선택 해제 + 우측 폼 초기화 (토글 동작)
- 선택된 행: CSS 클래스 `gridFocused` 추가

---

## 6. 우측: 등록/수정 폼

### 6-1. 폼 필드

| 구분 | 필드명 | 필수 | 설명 |
|------|--------|------|------|
| 중분류 | middleItemCodeId | 필수 | `value="0"` = 미선택 |
| 소분류 | smallItemCodeId | 필수 | 중분류 선택 후 연동 갱신 |
| 기기명 | name | 필수 | placeholder: "기기명을 입력해주세요" |
| 파일 업로드 | (file input) | 선택 | `.xls/.xlsx/.xlsm` 단일 파일 |

> `multiple` 속성 **사용하지 않음** (단일 파일만)

### 6-2. [신규] 버튼

- 우측 폼 전체 초기화
- 좌측 그리드 행 선택 해제 (gridFocused 제거)
- 파일 그리드 비우기, 파일 키워드 초기화

### 6-3. [저장] 버튼 — 저장 로직 흐름

```
[저장] 클릭
  ↓
입력값 검증 (gErrorHandler 사용)
  - 중분류 미선택 → "중분류를 선택해주세요"
  - 소분류 미선택 → "소분류를 선택해주세요"
  - 기기명 빈값  → "기기명을 입력해주세요"
  ↓
최종 저장 confirm (gMessage)
  ↓ 확인
  ├─ [수정 모드] selectedSampleId 존재
  │     → PATCH /api/sample/{id}  (multipart: data + file?)
  │     → 성공 시 좌측 그리드 reload + 해당 행 다시 선택
  │
  └─ [등록 모드] selectedSampleId 없음
        → GET /api/sample/checkDuplicate?middleItemCodeId=&smallItemCodeId=&name=
          ↓
          ├─ 중복 없음 → POST /api/sample
          │               → 성공 시 좌측 그리드 reload + 신규 행 선택
          │
          └─ 중복 있음 + 파일 있음
                → "동일 기기명이 존재합니다. 해당 기기에 파일을 추가하겠습니까?" confirm
                  ↓ 확인
                  → PATCH /api/sample/{existingId} (파일만 추가)
                  → 성공 시 그리드 reload + existingId 행 선택
          └─ 중복 있음 + 파일 없음
                → gErrorHandler("이미 등록된 샘플입니다.")
```

> API 호출 방식: `fetch` + `FormData`
> `data` 파트: `new Blob([JSON.stringify(req)], {type: 'application/json'})`
> `file` 파트: 파일 input 값 (없으면 append 생략)

---

## 7. 우측: 파일 목록 그리드

### 7-1. 특성

- **클라이언트사이드** 렌더링 (`resetData` 방식)
- `allFiles` 배열에 전체 목록 캐시 → 키워드 필터는 이 배열에서 수행
- 샘플 행 클릭 시: `GET /api/sample/{sampleId}/files` 호출 → `allFiles` 저장 → 그리드 렌더링

### 7-2. 컬럼

| header | name | width | align | 비고 |
|--------|------|-------|-------|------|
| 파일명 | originName | — | left | 클릭 시 다운로드: `window.location.href = /api/file/fileDownload/{id}` |
| 업로드 일시 | createDatetime | 140 | center | formatter: 날짜 배열/문자열 양쪽 대응 |
| 등록자 | writerName | 80 | center | |
| 삭제 | — | 60 | center | `[삭제]` 버튼, ADMIN만 표시 |

> `createDatetime` Jackson 기본 직렬화는 배열 형태(`[2026,3,9,14,30,0]`)일 수 있음.
> formatter에서 배열/ISO문자열 양쪽 처리 필요.

### 7-3. 파일 키워드 검색

- `input.fileKeyword` + `[조회]` 버튼
- `allFiles`를 `originName.includes(keyword)` 로 필터 후 `resetData` 호출
- 서버 호출 없음 (클라이언트 필터)

### 7-4. 파일 삭제 흐름

```
[삭제] 클릭
  ↓
"삭제하시겠습니까?" confirm (gMessage)
  ↓ 확인
DELETE /api/file/files/{fileId}
  ↓ 성공
allFiles에서 해당 id 제거 → resetData (서버 재호출 없음)
```

---

## 8. 중분류/소분류 셀렉트 연동 규칙

### 8-1. 데이터 소스

- `GET /api/basic/itemCodeSet` → `{ middleList: [...], smallList: [...] }`
- 페이지 로드 시 1회 호출, 결과를 `middleItemCodeSet`, `smallItemCodeSet` 변수에 캐시

### 8-2. 연동 방식

```
middleItemCodeSet: [{ id, codeNum, codeName }, ...]
smallItemCodeSet:  { [middleId]: [{ id, codeNum, codeName }, ...], ... }
```

- 중분류 변경 시: 해당 `middleId` 키에 해당하는 소분류 목록으로 소분류 셀렉트 갱신
- 좌측 검색 폼과 우측 입력 폼의 셀렉트를 **동시에** 동일 데이터로 초기화

### 8-3. 소분류 미선택 처리

- 중분류 변경 시 소분류는 첫 옵션(`value="0" 선택하세요` 또는 `value="" 소분류전체`)으로 reset
- 중분류 미선택 시 소분류 셀렉트 비활성화 또는 전체 소분류 표시

---

## 9. 보안 / 권한

| 기능 | 제한 |
|------|------|
| 목록 조회, 파일 조회 | 로그인 사용자 전체 |
| 등록, 수정 | 로그인 사용자 전체 |
| 샘플 삭제 | ADMIN만 (`SecurityConfig` DELETE /api/** 제한) |
| 파일 삭제 | ADMIN만 (동일) |

> 파일 삭제 버튼: ADMIN이 아닌 경우 `[삭제]` 버튼 렌더링 안 함 (또는 클릭 시 toast 경고).
> Thymeleaf에서 `sec:authorize` 또는 JS에서 서버 응답 403 처리 중 선택.

---

## 10. 에러 처리 기준

| 상황 | 처리 방식 |
|------|---------|
| 입력값 검증 실패 | `gErrorHandler(msg)` (toast) |
| API 응답 에러 (4xx/5xx) | `gApiErrorHandler(err)` (modal) |
| fetch 에러 캐치를 위해 | `if (!res.ok) throw res;` 패턴 적용 |

---

## 11. 작업 순서 (권장)

1. DB: `sample` 테이블 DDL 작성 (delta + schema.sql 동기화)
2. Backend: `Sample.java` 엔티티
3. Backend: `SampleListRow.java` Projection
4. Backend: `SampleDTO.java` DTO
5. Backend: `SampleRepository.java` + `FileInfoRepository` 메서드 추가
6. Backend: `SampleServiceImpl.java`
7. Backend: `api/SampleController.java`
8. Frontend: `sampleManage.html` (레이아웃)
9. Frontend: `sampleManage.js` (기능)
10. SSR Controller에 라우팅 추가 (이미 있으면 skip)

---

## 12. 완료 검증 체크리스트

- [ ] 중분류 선택 → 소분류 연동 갱신
- [ ] 기기명 키워드 검색 → 그리드 페이징 동작
- [ ] 행 클릭 → 우측 폼/파일 그리드 로드
- [ ] 같은 행 재클릭 → 폼 초기화 (토글)
- [ ] 신규 등록 → 중복 없음 → 저장 성공
- [ ] 신규 등록 → 중복 있음 + 파일 → 기존 건에 파일 추가 confirm 흐름
- [ ] 신규 등록 → 중복 있음 + 파일 없음 → 에러 toast
- [ ] 수정 → 저장 성공
- [ ] 파일 업로드 (Excel) → 파일 그리드 반영
- [ ] 파일명 클릭 → 다운로드
- [ ] 파일 키워드 검색 → 클라이언트 필터 동작
- [ ] ADMIN: 샘플 삭제 → 연관 파일도 소프트삭제
- [ ] ADMIN: 파일 삭제 → 그리드 즉시 반영 (서버 재호출 없음)
- [ ] 비ADMIN: 삭제 버튼 접근 차단
