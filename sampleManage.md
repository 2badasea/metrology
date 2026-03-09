# [샘플관리 메뉴(sampleManage)] 작업 요청서

## 참고
- 이미 페이지와 그리드 구조는 생성되어 있음(sampleManage.js, sampleManage.html	)
- 내가 정의한 기능을 구현하면서 기존의 구조를 수정할 필요가 있다면 제안할 것
- DB스키마 구조나 '작업 요청서' 자체에 대한 구성이나 형식, 나의 지시 매너톤에 대해서 피드백 사항이 있다면 나한테 충고를 해줄 것.

## 1. 개요

- **기능명**: 샘플관리 메뉴 및 기능 정의
- **메뉴 경로**: 기준정보관리 > 샘플관리
- **URL 경로**: `/sample/sampleManage`
- **페이지 유형**: 독립 페이지
- **접근 권한**: 전체

---

## 2. DB 스키마

> /docs/db/ 아래 최신 기준으로 작성. DDL 형태로 작성하면 오독 방지에 좋음.
> 샘플(sample) 스키마 구조를 참고하여 샘플을 위한 entity 클래스 생성할 것

```sql
CREATE TABLE sample (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`           VARCHAR(200)  NOT NULL COMMENT '샘플명(기기명)',
  	`middle_item_code_id` bigint DEFAULT NULL COMMENT '중분류코드',
  	`small_item_code_id` bigint DEFAULT NULL COMMENT '소분류코드',	
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
- 하나의 sample 테이블은 N개의 샘플파일로 구성되어 있음
- file_info 테이블에서 ref_table_id 값으로 sample의 id를 바라보고 있음. 
- file_info 테이블의 ref_table_name은 sample
- 첨부파일 저장 시 dir은  'sample/[sample 테이블의 id]/'로 구성

---

## 3. 화면 구성

### 3-1. 레이아웃
- 왼쪽 그리드는 sample 관련
- 오른쪽 그리드는 각 sample별 file_info 관련
-  왼쪽 그리드의 우측상단엔 중분류코드, 중분류명, 소분류코드, 소분류명, 기기명 key를 통해서 검색 가능. '전체선택 시' 위 5가지에 대해 전체검색 진행
-  왼쪽(sample)은 토스트 그리드 페이지네이션을 활용하되 페이지당 25개가 표시. 우측은 페이지네이션 없이 해당 영역에 왼쪽에서 선택한 샘플에 대한 file들이 모두 표시되었으면 좋겠어. 좌측은 중분류코드/소분류코드/기기명 기준으로 정렬이 되어야 하고, 우측은 파일이 등록된 최신순으로 정렬되었으면 해. 가장 최근에 등록한 게 가장 첫 번째 row에 표시되면 돼. 
- 우측 그리드의 등록자는 file_info를 생성한 create_member_id의 값을 member테이블과 조인해서 등록자를 보여주면 되고, file_info의 create_datetime 값을 등록일시 열에 표시하면 돼. 파일명은 확장자까지 포함해서 표시해줘
  


### 3-2. 검색 필터
- 위 3.1 레이아웃을 참고하되, itemManage.html과 itemManage.js, equipmentManage.js, equipmentManage.html 소스들을 참조해서 어떻게 그리드에서 검색조건을 넘기는지 살펴보면 돼.
- 우측 하단 그리드의 바로 위 검색은 '파일명'을 검색(file_info 테이블)한다고 보면 돼.

### 3-3. 그리드 컬럼 (목록)
- 현재 정의된 그리드 열 참조 (header명)
- 중분류(select)를 변경하면 변경된 중분류를 바라보는 소분류가 소분류(select)의 option으로 구성되면 돼. option은 '중분류코드 중분류명' 형태로 표시가 되면 되고, 스크립트 파일을 보면 최초 페이지가 렌더링 되고 나서 중분류와 해당 중분류를 바라보는 소분류가 key, value로 세팅되어 있을거야. 해당 변수를 확인해서 change 이벤트를 걸어주면 돼. 


### 3-5. 버튼 구성

| 버튼명 | 위치 | 동작 | 권한 제한 |
|--------|------|------|----------|
| 1. 삭제   | sample그리드 왼쪽 위 | 삭제 진행 gMessage확인(confirm). gMesasge를 띄우기 전에 해당 샘플을 바라보는, 즉 해당 샘플과 연간된 file_info가 존재하는지 확인 후 존재하면 파일도 같이 지워진다고 안내할 것. 바라보는 파일이 없는 경우 단순 gMesasge(confirm) | 전체 |
| 2. -   | 우측 그리드(file) 열 '-'  | 삭제하기 전 확인받기 gMesasge(confirm) | 전체 |
| 3. 신규   | 화면 우측상단 | '신규' 클릭 시, 우측의 검색창, 중분류(select), 소분류(select), 기기명, 파일업로드(input type=file), 파일 검색(input) 모두 입력값 초기화 | 전체 |
| 4. 저장   | 화면 우측상단 '신규' 버튼 우측 | 아래 상세 기능 설명 참고| 전체 |
- 4번 저장 기능에 대해서 상세하게 풀게
  - 왼쪽 그리드에 선택되어 focus가 된 row존재 시(itemCode페이지를 참조해보면 선택된 행에 대해 색상을 다르게 하는 방식이 잇음. 선택된 행을 재선택하게 될 경우 포커스가 해제되고 색상도 돌아옴) 우측 '중분류', '소분류' select박스가 그에 맞게 선택되고, 기기명도 입력된다. 이 상태에서 '저장'을 하게 되는 것은 수정(update)임. 저장할 때 업로드한 파일이 존재할 경우 선택된 샘플을 바라보는(ref_table_id) file_info, 즉 파일 데이터가 쌓이는 구조
  - '신규'를 클릭하게 되면 우측에 선택 및 입력된 항목이 모두 초기화된다. 이 상태에서 다시 중분류, 소분류, 기기명을 입력해서 저장을 하게 되면 동일한 명의 샘플이 존재하는지 체크하게 됨. 이때 입력값(기기명input)은 좌우 공백을 제거한 상태로 체크 및 저장/수정할 데이터로써 서버에 넘어가게 됨.
  - 저장은 무조건 gMessage(confirm)을 통해서 확인을 받고 진행하며, 위에 언급한 것과 같이 중분류, 소분류, 기기명이 중복되면 중복되는 게 존재하기에 저장이 안 된다고 할 것. 단 이때 중복되더라도 업로드한 파일이 존재한다면 중복되는 샘플의 하위 file로 저장하게 된다(항상 사용자에게 confirm을 받고 진행할 것)

---

## 4. 비즈니스 규칙 / 특이사항
- 삭제는 샘플과 파일 모두 soft-delete 정책. 단 log는 남겨야 함. 
- 왼쪽 그리드에서 샘플을 선택하게 될 경우, 우측의 중분류와 소분류 select박스, '기기명', 그리고 해당 샘플을 바라보는 파일들이 모두 표시가 되도록 해야 해. 변경되면 변경된 것으로 보여주어야 하고. 
- 파일의 용량은 한 개씩만 업로드가 가능하며 최대 50MB 크기로 가능해. 그리고 확장자는 무조건 엑셀만 업로드 및 저장이 가능해 (업로드 및 저장 시 체크해주고, 엉뚱한 게 올라오면 해당 input type=file을 초기화시켜준다.)
- 왼쪽 샘플을 삭제할 때, 혹시나 해당 샘플을 바라보는 file들이(is_visible = y 기준) 존재한다면 샘플파일이 존재함에도 불구하고 삭제할 것이냐는 메시지를 추가해서 삭제에 대한 confirm(gMesasge) 받을 것

---

## 5. 참고 / 기타

- 중분류코드, 소분류코드 데이터셋을 초기화하는 부분은 itemManage.js, itemManage.html을 참고하면 됨
- 그리드에서 선택한 row에 대해 포커싱 및 색상 변경에 대한 부분과 부모/자식 관계를 가지는 그리드 간의 연동된 이벤트와 관련해서는 품목코드(itemCode.js, itmeCode.html 파일 참고)
- 