# [실무자결재] 작업 요청서

## 1. 작업 배경
- 접수내역에서 성적서를 등록한 뒤, 이제 실제 교정을 진행하고 데이터를 입력한 뒤 실무자가 결재를 진행하여 기술책임자에게 상신할 수 있는 중간 매개체인 '실무자결재' 페이지가 필요합니다.
- 이 페이지에선 '성적서작성'을 통하여 각 중분류/소분류별 샘플파일을 대상으로 교정 raw데이터를 입력한 상태의 엑셀파일을 생성하고, 이 엑셀파일을 결재(업로드) 함으로써 실무자의 서명이미지가 삽입된 엑셀파일과 PDF로 변환된 파일이 생성되는 역할이 주로 이루어집니다.

---

## 2. 작업 원칙
- 본 요청서를 먼저 확인한 뒤, **확인받아야 할 사항이 있다면 반드시 피드백을 요청할 것**
- **작업 단위를 분리하여 단계적으로 진행할 것**
- 1차 작업과 2차 작업을 분리하고 있으며, '================' 형태로 작업 구분하고 있음
- '성적서작성'은 일단 그리드 좌측 위에 버튼만 구현해놓을 것 (클릭 시 gToast를 통해 '구현 준비중입니다' 띄우기)
  - 성적서작성은 별도의 모달 페이지이므로 별도의 기획이 필요하기 때문

---

### 참고사항
- 실무자결재의 메뉴명은 work_approval 이며 교정관리 하위 메뉴에 존재. menu 테이블에 row 데이터로 존재
  - menu 테이블의 url 컬럼값: /cali/work_approval
  - 페이지를 반환하는 컨트롤러 핸들러도 없으며, 관련 html, js 파일도 없음

### DB 변경사항 (1차 작업 전 선행 완료)
- `report` 테이블에 `expect_complete_date DATE NULL` 컬럼 추가 (완료예정일) → v_260317.sql
- `report` 테이블의 `report_status` 컬럼 COMMENT에 `CANCEL: 취소` 추가 → v_260317.sql
- Java `ReportStatus` enum에 `CANCEL` 추가 완료

---

# 1차 작업

## 1. 화면 구성
- 토스트 그리드를 통해서 리스트 표시
  - 표시데이터: 성적서(report 테이블 기준. 자체성적서 SELF만 표시)
  - 기본 표시 25건
  - rowHeight는 auto
  - 그리드 좌측 위 '행 수' 필터링을 통해 25 / 50 / 100 / 200 설정할 수 있도록 할 것
  - 삭제된 것은 표시하지 않음 (soft delete: is_visible = 'y' 조건)
  - 정렬:
      1. 접수 cali_order_id 기준 내림차순
      2. 공인(ACCREDDIT) → 비공인(UNACCREDDIT) → 시험(TESTING) 순
      3. 성적서번호 오름차순 — 성적서번호는 하이픈 2개로 구성 (예: 접수번호-접수번호-순번). 1·2번째 영역은 접수번호와 동일하므로 세 번째 영역(순번)을 숫자로 추출하여 정렬
  - 그리드 header 구성 (순서대로)
    - checkbox, 관리번호, 소분류, 접수일(교정접수일), 완료예정일, 성적서번호, 신청업체, 성적서발행처, 기기명, 기기번호, 제작회사, 형식, 진행상태, 작성자, 실무자, 기술책임자, 업로드
  - 그리드 컬럼별 개행처리 여부
    - 신청업체, 성적서발행처, 기기명, 기기번호, 제작회사, 형식 열의 경우 whiteSpace: 'pre-line' 옵션 추가
  - 텍스트 정렬은 모두 align: center 적용
  - 작성자 / 실무자 / 기술책임자: 이름(name)만 표시. member 테이블 JOIN 필요

- 검색필터 1 (report_status 컬럼 기준)
  - 위치: 그리드 좌측 위
  - 형식: 부트스트랩 드랍다운 형식
  - 옵션종류:
    1. 진행상태 (아무 조건 없음. 기본 선택. 모두 표시)
    2. 대기           WAIT
    3. 실무자반려     WORK_RETURN
    4. 기술책임자반려  APPROV_RETURN
    5. 재업로드       REUPLOAD
    6. 수리           REPAIR
    7. 불가           IMPOSSIBLE
    8. 취소           CANCEL
    9. 완료           COMPLETE

- 검색필터 2 (work_status 컬럼 기준)
  - 위치: 그리드 좌측 위
  - 형식: 부트스트랩 드랍다운 형식
  - 기본 선택: IDLE (IDLE 상태인 것만 표시)
  - 옵션종류:
    1. 결재상태   IDLE
    2. 결재대기   READY
    3. 결재진행중  PROGRESS
    4. 결재실패   FAIL
    5. 결재완료   SUCCESS

- 검색필터 3
  - 위치: 그리드 좌측 위
  - 형식: 부트스트랩 드랍다운 형식
  - 옵션종류: 전체 (기본. 구분 없이 모두 표시), 공인, 비공인, 시험

- 검색필터 4
  - 위치: 그리드 좌측 위
  - 형식: select 박스 형식
  - 옵션종류: 중분류, 소분류 (orderDetails.html, orderDetails.js 참조)

- 검색 (키워드, 옵션)
  - 위치: 그리드 우측 위
  - 형식: select(옵션), input(키워드)
  - 옵션종류: 성적서번호, 신청업체, 성적서발행처, 실무자, 기술책임자, 기기명, 형식, 제작회사, 기기번호, 관리번호

- 버튼1
  - 위치: 그리드 좌측 위
  - 버튼명: 성적서대기변경
  - 역할과 기능: 기술책임자에게 상신된 성적서를 되돌릴 수 있음. 단 기술책임자 결재가 이루어지지 않은 상태여야 함.
    - 우선은 gToast를 통해서 '구현 준비중입니다.' 표시

- 버튼2
  - 위치: 그리드 좌측 위
  - 버튼명: 통합수정
  - 역할과 기능: 그리드에 checkbox로 선택된 성적서들로 하여금 정보를 통합수정 가능
    - 우선은 gToast를 통해서 '구현 준비중입니다.' 표시

- 버튼3
  - 위치: 그리드 좌측 위
  - 버튼명: 성적서 다중결재
  - 역할과 기능: 클릭 시, 업로드 창이 열리며 성적서 파일을 업로드하면 일괄적으로 결재가 진행된다.(기술책임자에게 상신)
    - 우선은 gToast를 통해서 '구현 준비중입니다.' 표시

- 업로드 컬럼 (그리드 마지막 컬럼)
  - 버튼 형태로 표시
  - 클릭 시: input[type=file] 파일 선택 창 열림
    - 엑셀 파일(.xlsx, .xls)만 허용. 다른 형식 선택 시 gToast로 안내 후 중단
    - 파일 선택 후 gMessage(결재 / 취소) 확인창 표시
    - 결재(isConfirmed) 클릭 시: 현재는 gToast로 '구현 준비중입니다.' 표시
  - 드래그 앤 드롭: 셀 위로 파일 1개 드래그 가능
    - 엑셀 파일만 허용. 다른 형식 드롭 시 gToast로 안내 후 중단
    - 드롭 후 동일한 gMessage(결재 / 취소) 확인창 표시
    - 결재(isConfirmed) 클릭 시: 현재는 gToast로 '구현 준비중입니다.' 표시
  - 업로드한 파일이 셀에 표시되는 것이 아님 (결재 프로세스 연동 목적)


=======================================================================================================================
# 2차 작업 요청서 (성적서작성 모달 구현) start

### 성적서작성 기능 개요
- 성적서별로 중분류/소분류가 지정되어 있음
- 샘플관리(sample 테이블)에 중분류/소분류별 엑셀 샘플 파일이 스토리지에 존재
- 성적서작성 프로세스:
  1. 해당 성적서의 중분류/소분류에 맞는 샘플 엑셀을 스토리지에서 다운로드
  2. raw 데이터(신청업체, 성적서발행처, 온도, 습도, 실무자, 기술책임자 등) 삽입
  3. 완성된 엑셀을 스토리지에 저장 → 이것이 **원본성적서** 개념
  4. 우선은 이 작업요청서를 기반으로 화면 구성만 모두 구현할 것

## 성적서작성 모달 호출 과정
- 실무자결재(workApproval) 메뉴에서 리스트에서 체크박스로 선택하거나 아니면 성적서수정 모달(reportModify)에서 하단의 버튼을 통해서 호출할 거야. 성적서수정 모달에도 '성적서작성' 버튼을 추가해야 해. '저장', '닫기' 버튼이 있는 영역에서 왼쪽 끝에 위치했으면 좋겟어. 
- 성적서 수정모달에서 '성적서작성' 모달을 호출하기 위해선 해당 성적서에 중분류/소분류가 저장되어 있어야 해. 그리고 개별 성적서수정 모달에서 호출하니까 단 건(1건)에 대해서 성적서작성을 실행하겟지? 
- 리스트에선 체크박스에서 선택한 갯수만큼 성적서작성을 호출할 거야.(단, 소분류코드가 모두 동일한지 스크립트 단에서 검증 필요)
- 여튼 성적서작성 호출에 문제가 없는 경우 모달을 호출하게 되는데, 이 화면을 구현하는 게 이번 작업의 핵심이야.

## 성적서작성 모달 UI
- 모달이지만 화면은 상단에 검색하는 영역과 하단엔 검색결과가 표시되는 그리드 영역으로 구성돼. 
- '성적서작성' 호출 시, [기준정보관리 > 샘플관리(sampleManage)] 메뉴에 보면 각 소분류별로 샘플성적서를 업로드할 수 잇어. 샘플관리 메뉴의 좌측 부분은 중분류/소분류가 동일해도 돼. 단 기기명은 달라야 하거든. 여튼 '성적서작성' 호출 시, 선택된 성적서들의 소분류번호와 일치하는 샘플 성적서를 모두 표시해야 해. 
- 표시할 때 열은 샘플관리 메뉴에 잇는 '기기명', '파일명', '등록자', '등록일시'가 표시되었으면 해. (중분류와 소분류는 표시할 필요가 없어. 사용자가 소분류를 아는 상태로 성적서작성을 호출하니까)
  - file_info 테이블도 그렇고, item_code, sample 테이블도 같이 조회를 해야 할 거야 아마.
- 이때 정렬 순서는 최신순으로 저장된 것들이 가장 위쪽에 표시되도록 하면 돼. 
- 추가적으로 검색창은 '전체선택' 이 기본값이고 해당 select박스에 '기기명', '파일명', '등록자' option으로 구성되어 있어. 그리고 select박스 우측엔 keyword를 입력할 수 있지. 
- 기본은 '전체선택'이 된 상태로 해당 소분류의 샘플성적서가 모두 표시되면 돼(일단 리스트에 표시만해줘. 클릭에 대한 이벤트는 UI확정후에 진행할 거야.)
- 모달의 타이틀에 '성적서 작성 [소분류코드 - ?????]' 형태였으면 좋겟어. 
- 리스트에 마우스를 올리면 cursor pointer가 표시되어야 하고
- 모달 버튼은 '닫기'만 존재하면 돼. 


# 2차 작업 요청서 End
---

## 성적서 작성 및 결재 애플리케이션 구현 전 사전 피드백 확인 및 정리

```markdown
# 성적서작성/성적서결재 작업 설계 검토 정리

아래 내용은 사용자가 제시한 **“나의 성적서 결재 관련 구상이야. 피드백을 줘.”** 이후의 질문과, 그에 대한 검토/권장 방향을 **컨텍스트가 유지되도록 충분한 설명을 포함해** 정리한 문서다.  
이 문서는 이후 Claude Code 등으로 2차 검토를 받을 수 있도록, 질문사항이 누락되지 않게 원문 의도를 최대한 살려 정리하는 것을 목표로 한다.

---

# 1. 사용자 구상 원문 정리

## 1-1. 사전정보

- 성적서는 **중분류/소분류 정보**를 가지고 있다.
- 각 **소분류별로 해당 분야에 맞는 성적서 샘플 파일**이 존재한다.

---

## 1-2. 성적서 작성 기능 구상

### 1) 리스트에서 성적서 선택 후 `성적서작성` 버튼 클릭
- 사용자는 **1건 또는 n건**에 대해 리스트에서 체크박스로 선택한 뒤 `성적서작성` 버튼을 클릭한다.
- 이때 선택된 성적서들은:
  - 반드시 **모두 같은 소분류**여야 하고
  - **소분류 값이 비어있으면 안 된다**
- 따라서 버튼 클릭 시 이 조건을 검증하려고 한다.

---

### 2) 소분류가 모두 존재하면 `성적서 작성` 모달 호출
- 선택된 대상들의 소분류 정보가 모두 존재하면, `성적서 작성` 모달을 띄운다.
- 모달에서는 `샘플관리` 메뉴에서 해당 소분류로 관리되는 **성적서 샘플 파일 목록**을 모두 표시한다.
- 이 성적서 샘플 파일들은 모두 **스토리지에 저장**되어 있다.

---

### 3) 샘플 선택 시 `성적서생성` 작업 진행
- 사용자가 목록에서 대상 성적서 샘플을 선택하면 `성적서생성` 작업을 진행한다.
- 이 작업은 **WSL2 또는 홈서버 등의 별도 서버**에서 이루어진다.
- 단계적으로 WSL2 -> 미니PC로 구축한 홈서버 -> PUBLIC CLOUD(aws, ncp) 테스트 예정
- 작업 서버에서는 다음 흐름을 생각하고 있다.

#### 현재 구상된 흐름
1. 스토리지에 저장되어 있는 성적서 샘플 파일을 내려받는다.
2. 성적서별 UUID명으로 디렉토리를 생성한다.
3. 해당 디렉토리 내에서 샘플 파일을 기반으로 작업한다.
4. 선택된 성적서들의 데이터를 모두 `데이터시트`에 삽입한 엑셀 파일을 생성한다.
5. 이 엑셀 파일을 스토리지에  
   `[성적서id]/report_origin.xlsx`  
   형태로 저장한다.
6. 스토리지에 저장 성공 후 `file_info` 테이블에 데이터를 생성하려고 한다.
   - `ref_table_id = 성적서id`
   - `ref_table_name = 'report'`
   - `type = 'origin'`

---

### 4) 개별 성적서 작성 성공 시 후처리
- 개별 성적서 작성이 성공하면:
  - 작업했던 서버의 임시 UUID 디렉토리를 삭제하고
  - `writer_member_id`를 작업한 사람의 ID로 update 하려고 한다.
  - 성적서의 writer_member_id를 업데이트 하는 시점에 대해서 피드백 필요

---

## 1-3. 사용자 추가 궁금증 원문 정리

### 궁금 1
- WSL2에서 엑셀 파일이 생성되잖아?
- 이 파일을 로컬 PC 내의  
  `C드라이브/데이터시트/접수번호/[성적서번호].xlsx`  
  형태로 생기게 할 수도 있을까?
- 화면 우측 상단에 다운로드 표시 없이, 자연스럽게 로컬의 해당 경로에도 파일이 생겼으면 한다.

---

### 질문 1. 트랜잭션 범위
- 1건 작업에 실패하더라도 그 건만 실패로 처리하고,
- 다음 성적서 작업은 계속 진행되었으면 좋겠다.
- 이 경우 트랜잭션 범위를 어떻게 잡는 것이 좋을까?

---

### 질문 2. 실시간 완료 확인 / 로딩창
- 웹앱에서 `성적서작성` 작업 명령을 내린 후,
- 실시간으로 개별 성적서 작업이 완료되었는지 확인할 방법이 있을까?
- 이 작업 명령을 내리고 나서는 적어도 해당 페이지에서는 다른 작업들을 못 하게 로딩창이라도 띄워 놓는 게 나을까?

---

### 질문 3. 브라우저 종료 / 비정상 동작 시 처리
- 사용자가 갑자기 브라우저 창을 닫거나,
- 비정상적인 움직임을 보이면 어떻게 되는가?
- 성적서 작업 서버에 100건의 성적서작성 명령을 내린 상태라면,
- 이런 상황들에 대해 어떻게 제어하는 것이 맞을까?

---

### 질문 4. 실시간 진행상황 표시 방식
- 브라우저(웹앱)와 성적서 작업 서버 간의 실시간 진행상황을
- 화면에 어떻게 표시해줄지 고민이다.
- 어차피 Claude에게 시킬 예정이지만, 어떤 방안들이 있을지 궁금하다.

---

### 질문 5. 성적서결재 쪽 확장
- 위 내용은 `성적서작성` 작업에 대한 이야기이지만,
- `성적서결재(실무자결재/기술책임자결재)`는 개별 성적서결재 상태가
  - `idle`
  - `ready`
  - `progress`
  - `success/fail`
  로 실시간 업데이트된다.
- 작업 요청을 내리면 일단 `ready`
- 개별 성적서에 대한 작업을 시작하면 `progress`
- 최종 `success/fail`
- 그 다음 성적서에 대해 다시 상태 업데이트  
  이런 식으로 진행된다.
- `성적서작성`보다 더 많은 진행상황이 나뉘는 편이다. (성적서작성은 애초에 진행상태를 관리하지 않는다.)
- 그래서 일단 `성적서작성`부터 정책을 잘 정해두면,  
  성적서결재도 수월하게 이어갈 수 있지 않을까 고민 중이다.

---

# 2. 전체 피드백 요약

전체 방향은 좋다.  
특히 다음 점들이 적절하다.

- 소분류별 샘플 파일 관리
- 무거운 엑셀 작업을 별도 서버/워커로 분리
- 스토리지와 `file_info`를 통해 산출물 추적
- 향후 성적서결재까지 고려한 상태 관리 의식

다만 현재 구상은 **“기능 흐름”** 중심으로는 괜찮지만,  
실제로 운영 가능한 형태로 가기 위해서는 아래 4가지를 먼저 구조화하는 것이 중요하다.

1. **배치 작업(batch)과 개별 작업(item)을 분리**
2. **개별 성적서 단위로 독립 처리**
3. **브라우저와 무관하게 서버 작업이 계속되는 비동기 구조**
4. **성적서작성/성적서결재에 공통 적용 가능한 상태 설계**

즉, 사용자는 한 번에 `성적서작성` 버튼을 누르지만,  
시스템 내부적으로는 다음처럼 보는 것이 더 적절하다.

- 사용자 액션 1번 = **배치 작업 1건**
- 선택된 성적서 n건 = **개별 작업 n건**

이 구조로 가야 이후의 실시간 진행상황, 부분 실패, 재시도, 결재 확장이 자연스럽다.

---

# 3. 세부 피드백

## 3-1. 체크박스 선택 후 같은 소분류 검증

이 검증은 꼭 필요하다.  
다만 **프론트엔드와 백엔드 둘 다 검증**하는 것이 좋다.

### 이유
- 프론트 검증:
  - 사용자에게 빠른 피드백 제공
- 백엔드 검증:
  - 최종 정합성 보장
  - 비정상 요청 방지

### 추가로 같이 검토할 검증 항목
- 이미 작성 중인 성적서인지
- 이미 작성 완료된 성적서를 재작성 허용할지 여부
- 선택한 샘플 파일이 해당 소분류에 유효한지

---

## 3-2. 성적서생성 작업 방식

현재 구상대로도 작업은 가능하지만,  
보다 안정적인 구조를 위해서는 다음 관점으로 재정리하는 것이 좋다.

### 현재 구상
- 사용자가 성적서 n건 선택
- 샘플 선택
- 별도 서버에서 바로 파일 생성 작업 진행

### 권장 구조
- 사용자가 성적서 n건 선택
- 샘플 선택
- **성적서작성 배치 작업(batch)** 생성
- 배치 내부에 **개별 성적서 작업(item)** n건 생성
- 워커는 item 단위로 처리

즉 예를 들면:

- batch 1개 생성
- report A → item 1
- report B → item 2
- report C → item 3

이렇게 분해해서 처리하는 게 맞다.

### 이 구조의 장점
- 1건 실패해도 나머지 계속 진행 가능
- 진행률 계산이 쉬움
- UI에 상태 표시하기 쉬움
- 재시도/재처리 설계가 쉬움
- 이후 결재 작업과 구조를 통일하기 좋음

---

## 3-3. `report_origin.xlsx`는 성적서별 파일로 보는 것이 자연스러움

현재 문장 중  
“선택된 성적서들의 데이터를 모두 데이터시트에 삽입한 엑셀파일”  
이라는 표현은 해석 여지가 있다.

### 해석 가능성
1. 성적서 1건마다 개별 origin 파일 생성
2. 선택된 여러 성적서를 한 파일에 모두 넣는 방식

여기서는 **1건마다 개별 origin 파일 생성**으로 보는 것이 더 적절하다.

즉:

- 성적서 1건 = origin.xlsx 1개
- 성적서 1건 = 추후 결재 대상 1건
- 성적서 1건 = 개별 파일 추적 가능

이 구조가 이후 수정, 재작성, 결재, 이력 추적에 가장 유리하다.

예시 저장 경로:
- `/report/{reportId}/report_origin.xlsx`

---

## 3-4. `file_info` 생성 시점

질문에서 “스토리지 저장 전 or 저장 성공 후 생성”이라고 고민했지만,  
권장 방향은 명확하다.

### 권장
- **반드시 스토리지 업로드 성공 후 `file_info` 생성**

### 이유
파일이 실제로 존재하지 않는데 `file_info`만 먼저 생성되면:
- DB와 스토리지 정합성이 깨질 수 있음
- 업로드 실패 시 잘못된 레코드가 남을 수 있음

### 권장 순서
1. 파일 생성
2. 스토리지 업로드 성공
3. `file_info` insert
4. 보고서/작성자 등 관련 정보 update

---

## 3-5. `writer_member_id` 업데이트 시점

이 값은 “누가 이 성적서 작성 작업을 수행했는가”를 의미하므로,  
**개별 성적서 작성이 성공한 시점에 update** 하는 것이 자연스럽다.

가능하다면 아래 정보도 함께 관리하면 좋다.

- `writer_member_id`
- `writer_at`
- `last_generated_batch_id`

이렇게 해두면 추적성이 좋아진다.

---

# 4. WSL2에서 로컬 C드라이브 저장 가능 여부

사용자 질문:

> WSL2에서 생성된 엑셀 파일을  
> `C드라이브/데이터시트/접수번호/[성적서번호].xlsx`  
> 형태로 자연스럽게 로컬 경로에 생성할 수 있을까?  
> 브라우저 다운로드 표시 없이?

## 답변 요약

**같은 PC 안에서 WSL2 워커가 실행 중인 경우에는 가능하다.**

이유는 이것이 **브라우저 다운로드가 아니라**,  
**WSL2 내부 프로세스가 Windows 파일시스템에 직접 파일을 쓰는 방식**이기 때문이다.

예를 들어 Windows의 `C:\...` 경로는 WSL2에서 보통 `/mnt/c/...` 로 접근 가능하다.

즉 다음과 같이 쓸 수 있다.

- Windows 경로:  
  `C:\데이터시트\접수번호\12345.xlsx`
- WSL2 내부 경로:  
  `/mnt/c/데이터시트/접수번호/12345.xlsx`

이 방식이면 브라우저 우측 상단에 다운로드 알림 없이 파일이 생길 수 있다.

## 단, 중요한 한계
이건 **워커가 로컬 PC 안에서 실행될 때만 유효**하다.

### 가능
- 로컬 PC의 WSL2 워커 → 로컬 C드라이브 저장 가능

### 불가능 또는 부적절
- 홈서버 → 내 PC C드라이브 직접 저장 불가
- AWS/NCP → 내 PC C드라이브 직접 저장 불가

즉, 이 방식은 **운영 기준 구조**로 삼기보다는  
**로컬 개발/디버깅용 편의 기능**으로 생각하는 것이 맞다.

## 권장 방향
- 운영 기준: **스토리지 저장**
- 로컬 디버깅 옵션: 필요 시 `/mnt/c/...` 동시 저장 허용

즉 설정값으로 분리하는 것이 바람직하다.

예:
- `localDebugSaveEnabled = true`
- `localDebugBasePath = /mnt/c/데이터시트`

---

# 5. 질문별 답변 정리

---

## 5-1. 질문 1 — 트랜잭션 범위는 어떻게 잡는 것이 좋은가?

사용자 의도:
- 1건 실패하더라도 그 건만 실패 처리
- 다음 성적서 작업은 계속 진행

## 권장 답변
이 경우 **개별 성적서(item) 단위 트랜잭션**으로 처리하는 것이 가장 적절하다.

### 권장 구조
- 배치 전체를 하나의 큰 트랜잭션으로 묶지 않는다.
- **성적서 1건 처리 = 1개 트랜잭션**
- 배치 상태 집계는 별도의 짧은 트랜잭션으로 관리한다.

### 흐름 예시
1. batch 생성
2. item n개 생성
3. 워커가 item 1개를 가져감
4. item 상태를 `PROGRESS`로 변경
5. 파일 생성, 업로드, DB 갱신 수행
6. 성공 시 `SUCCESS`
7. 실패 시 `FAIL`
8. 다음 item 진행
9. 마지막에 batch 성공/실패 건수 집계

### 왜 이렇게 해야 하나?
배치 전체를 하나의 트랜잭션으로 묶으면:
- 1건 실패 시 전체 롤백 위험
- 파일 업로드와 DB 롤백 경계가 어긋남
- 장시간 트랜잭션으로 락 문제가 생길 수 있음

따라서 이 작업은 **배치 단위가 아니라 item 단위 독립 처리**가 맞다.

---

## 5-2. 질문 2 — 웹앱에서 실시간 완료 확인이 가능한가? 로딩창은 어떻게?

사용자 의도:
- 작업 시작 후 개별 성적서 완료 여부를 실시간으로 보고 싶음
- 작업 중 페이지를 잠가야 하는지 고민

## 답변 요약
실시간 완료 확인은 가능하다.  
방법은 크게 3가지다.

### 방법 A. Polling
- 프론트가 일정 주기(예: 1~2초)로 상태 조회 API 호출
- `/job-batches/{id}` 또는 `/report-jobs/{batchId}` 조회

#### 장점
- 구현이 가장 단순
- 안정적
- 초기 도입에 적합

#### 단점
- 완전한 실시간은 아님

---

### 방법 B. SSE(Server-Sent Events)
- 서버가 진행상황을 계속 브라우저로 push

#### 장점
- WebSocket보다 간단
- 진행률 표시와 궁합이 좋음

#### 단점
- 연결 관리 필요

---

### 방법 C. WebSocket
- 가장 실시간성이 좋고 양방향 통신 가능

#### 장점
- 풍부한 인터랙션 가능

#### 단점
- 지금 단계에서는 다소 과할 수 있음

---

## 현재 단계에서의 추천
- 초기 구현: **Polling 또는 SSE**
- 특히 지금 프로젝트 상황에서는 **Polling부터 시작**해도 충분히 현실적이다.

---

## 로딩창 정책에 대한 권장
전체 화면을 막는 강한 로딩창은 권장하지 않는다.

### 이유
- 작업이 길어질 수 있음
- 사용성이 떨어짐
- 브라우저 닫기/이동 시 혼란이 생김

### 더 나은 방식
- 현재 페이지에서 **관련 버튼만 비활성화**
- 진행상황 표시 패널/모달/우측 사이드바 제공
- 사용자는 다른 페이지로 이동 가능
- 나중에 다시 와도 배치 상태를 조회 가능

즉, “작업 중이라 아무것도 못 하게 막는 화면”보다는  
**비동기 작업 관리자 UI**에 가깝게 가는 것이 좋다.

---

## 5-3. 질문 3 — 브라우저를 닫거나 비정상적인 움직임이 있으면 어떻게 되는가?

사용자 의도:
- 100건의 작업이 서버에 전달된 뒤, 사용자가 창을 닫아버리면 어떻게 되는지
- 이를 어떻게 제어해야 하는지 궁금

## 권장 답변
**브라우저가 닫혀도 서버 작업은 계속 진행되는 구조**로 설계하는 것이 맞다.

### 이유
- 브라우저는 요청 발행자일 뿐
- 실제 작업은 별도 서버/워커가 수행
- UI 연결과 작업 수행은 분리되어야 안정적

즉:

- 브라우저 종료 = 화면 연결 종료
- 작업 서버 = 계속 처리
- 나중에 다시 접속하면 상태 조회 가능

---

## 취소 정책에 대한 권장
사용자 의도와 별개로, 명시적인 “작업 취소” 기능은 별도 정책으로 설계하는 것이 좋다.

### 추천 정책
- `READY` 상태(아직 시작 안 함)인 item은 취소 가능
- `PROGRESS` 상태(이미 처리 중)인 item은 현재 단계 완료 후 종료 여부 결정
- batch 상태는 `CANCEL_REQUESTED` → `CANCELED` 흐름으로 관리

이렇게 해야 작업 중간에 파일이나 상태가 어정쩡하게 깨지지 않는다.

---

## 5-4. 질문 4 — 실시간 진행상황은 화면에 어떻게 표시할 수 있을까?

사용자 의도:
- 브라우저(웹앱)와 작업 서버 간 진행상황 표시 방안이 궁금함

## 권장 UI 구성 요소

### 1) 배치 진행률 표시
예:
- 총 100건
- 성공 15건
- 실패 2건
- 진행중 1건
- 대기 82건

진행률 바(progress bar)와 숫자 요약을 함께 표시하면 좋다.

### 2) 개별 성적서 상태 목록
예:
- 2026-001 — 성공
- 2026-002 — 성공
- 2026-003 — 실패(샘플 데이터 누락)
- 2026-004 — 진행중

### 3) 현재 처리 중인 대상 표시
예:
- 현재 처리 중: `성적서번호 2026-004`
- 현재 단계: `이미지 삽입 중`

### 4) 실패 메시지 표시
- 단순 FAIL 뱃지보다
- 왜 실패했는지 요약 메시지가 보여야 재처리 판단이 쉬움

---

## 표시 방식 추천
모달도 가능하지만, 작업이 길어질 수 있으므로 다음이 더 낫다.

- 우측 패널
- 별도 “작업 현황” 팝업
- 상단 진행상황 패널
- 별도 작업 현황 페이지

즉, 단발성 confirm 모달보다는  
**작업 모니터링 UI** 개념으로 가는 것이 더 적절하다.

---

## 5-5. 질문 5 — 성적서작성보다 결재는 상태가 더 많은데, 지금부터 어떤 정책을 잡는 게 좋을까?

사용자 의도:
- 성적서결재는 상태가 더 세분화되어 있음
- 지금 성적서작성 정책을 잘 잡아두면 결재도 수월할 것 같음

## 답변 요약
맞다.  
오히려 지금 시점에서 **공통 상태 구조를 먼저 정의해두는 것**이 가장 중요하다.

---

## 권장: 공통 상태 + 작업별 세부 단계(step) 분리

### 공통 상태(status)
- `IDLE`
- `READY`
- `PROGRESS`
- `SUCCESS`
- `FAIL`
- `CANCEL_REQUESTED`
- `CANCELED`

### 작업별 세부 단계(step)
#### 성적서작성 예시
- `VALIDATING`
- `DOWNLOADING_TEMPLATE`
- `FILLING_DATA`
- `INSERTING_IMAGES`
- `UPLOADING_ORIGIN`
- `DONE`

#### 실무자결재 예시
- `PREPARING`
- `LOADING_ORIGIN`
- `APPLYING_WORKER_SIGN`
- `GENERATING_PDF`
- `UPLOADING_RESULT`
- `DONE`

#### 기술책임자결재 예시
- `PREPARING`
- `LOADING_ORIGIN`
- `APPLYING_MANAGER_SIGN`
- `GENERATING_PDF`
- `UPLOADING_RESULT`
- `DONE`

---

## 이 구조의 장점
- `status`는 공통으로 쓰고
- 작업마다 상세 단계(`step`)만 다르게 가져갈 수 있음
- UI에서도
  - 상태 뱃지: `READY / PROGRESS / SUCCESS / FAIL`
  - 상세 문구: `이미지 삽입 중`, `PDF 변환 중`
  형태로 보여주기 쉬움
- 작성/결재 기능을 같은 프레임워크로 확장 가능

즉,  
지금 성적서작성부터 이 틀로 설계하면,  
이후 성적서결재까지 자연스럽게 연결될 가능성이 높다.

---

# 6. 추가로 권장하는 데이터 모델 방향

지금 구상을 구현 가능한 구조로 끌어내리기 위해,  
최소한 개념적으로는 아래 두 축을 먼저 잡아두는 것이 좋다.

## 6-1. 배치 작업 테이블 개념
예: `report_job_batch`

가능한 관리 항목:
- `id`
- `job_type`
- `request_member_id`
- `sample_file_id`
- `subcategory_id`
- `total_count`
- `success_count`
- `fail_count`
- `status`
- `requested_at`
- `started_at`
- `ended_at`

---

## 6-2. 개별 작업 테이블 개념
예: `report_job_item`

가능한 관리 항목:
- `id`
- `batch_id`
- `report_id`
- `status`
- `step`
- `message`
- `retry_count`
- `started_at`
- `ended_at`

---

## 이 구조를 권장한 이유
이렇게 해야:

- 성적서작성 배치 1건 안에 여러 report 작업을 넣을 수 있고
- 각 report별 성공/실패/진행 상황을 실시간으로 추적할 수 있으며
- 결재 작업도 같은 구조에 태울 수 있다.

즉,  
“성적서작성 버튼 클릭”은 단순 UI 이벤트가 아니라  
**batch + item으로 분해되는 비동기 작업 모델**로 봐야 한다.

---

# 7. 최종 권장 정책

## 7-1. 성적서작성 정책
- 같은 소분류만 일괄 작성 가능
- 버튼 클릭 시 프론트 + 백 모두 검증
- 샘플 선택 후 batch job 생성
- 개별 성적서별 item job 생성
- item 단위 독립 처리
- origin.xlsx는 성적서별 저장
- `file_info`는 업로드 성공 후 생성
- `writer_member_id`는 개별 성공 시 갱신
- 브라우저 종료와 무관하게 서버 작업 지속
- 프론트는 Polling 또는 SSE로 진행상황 표시
- 전체 화면 락보다 작업 현황 UI 제공

---

## 7-2. 파일 처리 정책
- 작업 서버에서는 요청 1건마다 UUID 디렉토리 사용
- 작업 디렉토리 안에서 파일 생성/수정/삭제 수행
- 성공 작업은 빠르게 정리
- 실패 작업은 디버깅을 위해 일정 시간 보관
- 운영 파일은 스토리지를 기준으로 관리
- 로컬 PC 저장은 개발 편의용 옵션으로만 제공

---

## 7-3. 향후 결재 확장 정책
- 지금부터 batch/item 구조로 설계
- 공통 `status` + 작업별 `step` 구조 도입
- 성적서작성과 성적서결재를 같은 틀에서 확장

---

# 8. 한 줄 정리

이번 검토의 핵심은 다음과 같다.

- 현재 구상은 좋은 출발점이다.
- 다만 단순 파일 생성 흐름이 아니라  
  **배치 작업 + 개별 작업 모델**,  
  **item 단위 트랜잭션**,  
  **브라우저와 분리된 비동기 서버 작업**,  
  **공통 상태 머신 설계**  
  까지 포함해서 구조화해야 이후 `성적서작성`과 `성적서결재`를 안정적으로 확장할 수 있다.

---

# 9. Claude Code 2차 검증 시 함께 보면 좋은 포인트

Claude Code에게 검토를 맡길 때는 아래 관점으로 추가 검증을 받으면 좋다.

1. batch / item 구조가 현재 프로젝트 테이블 구조에 자연스럽게 녹아드는지
2. `report`, `file_info`, 작성/결재 상태 컬럼 간 책임 분리가 적절한지
3. 성적서작성과 성적서결재를 공통 상태 머신으로 묶을 수 있는지
4. Polling / SSE / WebSocket 중 현재 프로젝트 단계에서 어떤 선택이 가장 적절한지
5. WSL2 → 홈서버 → 퍼블릭 클라우드 확장 시 파일 경로/스토리지/작업 디렉토리 전략이 유지 가능한지
6. “브라우저 종료 후에도 작업 지속” 정책에 맞는 API / UI / 상태 저장 방식이 적절한지
7. 재작성/재결재/실패 재시도 정책을 어디까지 공통화할 수 있는지

---
```

---

## Claude Code 2차 검증 결과 — 최종 합의 사항 (2026-03-20)

> GPT 피드백 문서를 기반으로 Claude Code와 추가 검토한 결과물.
> 이후 구현 시 이 내용을 기준으로 진행한다.

---

### 1. 기술 스택 및 통신 방식 확정

| 항목 | 확정 내용 |
|---|---|
| 성적서작성 앱 기술 스택 | Java (Apache POI) 기반 별도 워커 서버 |
| 통신 방식 | 옵션A: CALI → 작업서버 REST API 호출 (트리거) |
| 진행상황 조회 | Polling (초기 구현 기준, 이후 SSE 확장 고려) |
| 큐(Queue) 도입 | 현재 미도입. batch/item 테이블이 큐 역할 대체. 나중에 작업서버 내부에만 도입 가능 |

#### 트리거 페이로드 설계 원칙
- CALI → 작업서버 요청 시: `batchId` + 공통 설정값(스토리지 접속정보, envInfo) 포함
- item별 처리 데이터(report 정보 등)는 작업서버가 CALI API를 호출해서 조회
- 스토리지 접속키 등 민감값은 DB(`report_job_batch`)에 저장하지 않음

---

### 2. 파일 처리 정책 확정

#### 전체 흐름
```
[성적서작성]
  sample.xlsx(스토리지) → raw데이터 삽입 → origin.xlsx → 스토리지 저장
  file_info: type='origin', extension='xlsx' 생성
  report: write_status=SUCCESS, write_member_id, write_datetime 업데이트

[실무자결재] (write_status=SUCCESS 이거나 직접 업로드한 경우만 가능)
  origin.xlsx → 실무자 서명 삽입 → 스토리지 저장
  file_info: type='signed', extension='xlsx' / 'pdf' 각 1건 생성
  report: work_status=SUCCESS, work_member_id, work_datetime 업데이트

[기술책임자결재] (work_status=SUCCESS인 경우만 가능)
  signed.xlsx → 기술책임자 서명 + QR코드 + 발행일자 삽입 → 스토리지 저장
  file_info: 기존 signed 파일 soft delete(is_visible='n') + 새 signed 행 insert (type 동일)
  report: approval_status=SUCCESS, approval_member_id, approval_datetime,
          report_status=COMPLETE 업데이트
```

#### file_info.type 값 정의

| type | extension | 설명 |
|---|---|---|
| `origin` | xlsx | 원본 성적서 (성적서작성 결과 or 직접 업로드) |
| `signed` | xlsx | 최신 서명 삽입 엑셀 (실무자 or 기술책임자 결재 후) |
| `signed` | pdf | 최신 서명 삽입 PDF |
| NULL | 해당없음 | 기타 첨부파일 (sample 등) |

- **`origin` 교체 방식**: 재업로드 시 기존 origin → soft delete(`is_visible='n'`) + 새 origin insert
- **`signed` 교체 방식 (방식B 채택)**: 기술책임자결재 시 기존 signed → soft delete + 새 signed insert. 이력 보존 + 기존 테이블 soft delete 패턴 일관성 유지.

#### 결재 진입 조건
- `write_status = SUCCESS` **또는** origin 파일이 file_info에 존재(직접 업로드) → 실무자결재 가능
- `work_status = SUCCESS` → 기술책임자결재 가능

#### 실무자결재 페이지 액션 구분
- **업로드**: 원본 파일만 교체 (결재 미진행). **다건 일괄 업로드 가능**.
- **결재**: 원본 기반으로 서명 삽입 + signed 파일 생성. **다건 일괄 결재 가능**.

---

### 3. DB 변경 확정 사항 (구현 전 승인 필요)

#### 3-1. `file_info` 테이블 — `type` 컬럼 추가
```sql
ALTER TABLE file_info
  ADD COLUMN `type` VARCHAR(50) NULL
    COMMENT '파일 역할 구분 (origin: 원본성적서 / signed: 서명삽입본. 성적서 관련에만 사용, 나머지 NULL)'
  AFTER `content_type`;
```

#### 3-2. `report` 테이블 — `write_status` 컬럼 추가
```sql
-- write_status 컬럼 추가 (work_status 앞에 위치)
ALTER TABLE report
  ADD COLUMN `write_status` VARCHAR(50) NOT NULL DEFAULT 'IDLE'
    COMMENT '성적서작성 진행상태 (IDLE: 미작성, PROGRESS: 작성중, SUCCESS: 작성완료, FAIL: 작성실패)'
  BEFORE `work_status`;
```

3대 상태 컬럼 패턴:
```
write_status:     IDLE → PROGRESS → SUCCESS / FAIL   (성적서작성)
work_status:      IDLE → READY → PROGRESS → SUCCESS / FAIL   (실무자결재)
approval_status:  IDLE → READY → PROGRESS → SUCCESS / FAIL   (기술책임자결재)
```

#### 3-3. `report_job_batch` 테이블 신설
```sql
CREATE TABLE `report_job_batch` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT,
  `job_type`          VARCHAR(30) NOT NULL
    COMMENT 'WRITE: 성적서작성 / WORK_APPROVAL: 실무자결재 / MANAGER_APPROVAL: 기술책임자결재',
  `request_member_id` BIGINT      NOT NULL  COMMENT '작업 요청자 member.id',
  `sample_id`         BIGINT      DEFAULT NULL  COMMENT 'WRITE 타입에만 사용. sample.id',
  `total_count`       INT         NOT NULL DEFAULT 0  COMMENT '전체 처리 대상 건수',
  `success_count`     INT         NOT NULL DEFAULT 0  COMMENT '성공 건수',
  `fail_count`        INT         NOT NULL DEFAULT 0  COMMENT '실패 건수',
  `status`            VARCHAR(30) NOT NULL DEFAULT 'READY'
    COMMENT 'READY / PROGRESS / SUCCESS / FAIL / CANCEL_REQUESTED / CANCELED',
  `create_datetime`   DATETIME    NOT NULL DEFAULT (now()),
  `start_datetime`    DATETIME    DEFAULT NULL  COMMENT '작업 서버에서 처리 시작한 시각',
  `end_datetime`      DATETIME    DEFAULT NULL  COMMENT '배치 전체 처리 완료 시각',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='성적서 작업 배치 (성적서작성/결재 작업의 묶음 단위)';
```

#### 3-4. `report_job_item` 테이블 신설
```sql
CREATE TABLE `report_job_item` (
  `id`             BIGINT      NOT NULL AUTO_INCREMENT,
  `batch_id`       BIGINT      NOT NULL  COMMENT 'report_job_batch.id',
  `report_id`      BIGINT      NOT NULL  COMMENT 'report.id',
  `status`         VARCHAR(30) NOT NULL DEFAULT 'READY'
    COMMENT 'READY / PROGRESS / SUCCESS / FAIL / CANCELED',
  `step`           VARCHAR(50) DEFAULT NULL
    COMMENT '현재 처리 단계 상세명 (예: DOWNLOADING_TEMPLATE, FILLING_DATA, UPLOADING_ORIGIN 등)',
  `message`        TEXT        DEFAULT NULL  COMMENT '실패 사유 또는 처리 메시지',
  `retry_count`    INT         NOT NULL DEFAULT 0,
  `start_datetime` DATETIME    DEFAULT NULL,
  `end_datetime`   DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='성적서 작업 개별 항목 (batch 내 성적서 1건 = item 1건)';
```

#### step 값 예시 (job_type별)

| job_type | step 흐름 |
|---|---|
| WRITE | DOWNLOADING_TEMPLATE → FILLING_DATA → UPLOADING_ORIGIN → DONE |
| WORK_APPROVAL | LOADING_ORIGIN → APPLYING_SIGN → GENERATING_PDF → UPLOADING → DONE |
| MANAGER_APPROVAL | LOADING_ORIGIN → APPLYING_SIGN → INSERTING_QR → GENERATING_PDF → UPLOADING → DONE |

---

### 4. GPT 피드백 검증 결과 (Claude Code 관점)

| GPT 권장 | Claude Code 검증 결과 |
|---|---|
| batch/item 구조 분리 | ✅ 채택. 지금 잡지 않으면 결재 확장 시 전면 재설계 필요 |
| item 단위 독립 트랜잭션 | ✅ 채택. 1건 실패가 전체 롤백으로 이어지지 않도록 |
| 브라우저와 무관한 서버 작업 지속 | ✅ 채택. 작업서버가 독립 실행, CALI는 상태만 추적 |
| 공통 상태 머신 (IDLE/READY/PROGRESS/SUCCESS/FAIL) | ✅ 채택. write_status / work_status / approval_status 패턴 통일 |
| 큐(Queue) 도입 | 🔄 현재 미도입. batch/item 테이블이 대체. 추후 작업서버 내부에만 도입 가능 |
| file_info 업로드 성공 후 생성 | ✅ 채택 |
| writer_member_id 개별 성공 시 갱신 | ✅ 채택 (write_member_id + write_datetime) |
| Polling 우선 권장 | ✅ 채택 (초기 구현 기준) |

---

### 5. env.sheet_info_setting 활용

- `env` 테이블의 `sheet_info_setting JSON` 컬럼: 성적서 엑셀 셀 위치/형식 설정 저장
- 성적서작성 앱이 실행 시 이 JSON을 참조하여 어느 셀에 어떤 값을 입력할지 결정
- CALI → 작업서버 트리거 요청 시 `envInfo` 항목에 이 JSON을 포함하여 전달

---

## 향후 작업 계획 (대략, 미확정)

> 아래 항목들은 구체적인 논의가 아직 이루어지지 않았으며, 추후 별도로 기획 및 합의 후 진행한다.

---

### Phase 1 — CALI 내 성적서작성/결재 연동 API 구현 (현재 진행 방향)

- `report_job_batch` / `report_job_item` CRUD API 구현
- 배치 생성 API (성적서작성 버튼 클릭 → batch + items 생성 → 작업서버 트리거)
- 작업서버 → CALI item 상태 업데이트 수신 API
- 브라우저용 Polling API (배치/아이템 진행상황 조회)
- 실무자결재 업로드 / 결재 API
- 기술책임자결재 API

---

### Phase 2 — 성적서작업 워커 애플리케이션 구현 (별도 프로젝트, Java + Apache POI)

#### 기본 역할
- CALI로부터 트리거 요청을 받아 성적서 작업(작성/결재)을 수행하는 독립 Java 애플리케이션
- 배포 대상: WSL2(로컬 개발) → 미니PC 홈서버 → 퍼블릭 클라우드 단계적 확장

#### 주요 기능 (미확정, 추후 기획 필요)
- CALI REST API 호출을 통해 batch/item 정보 조회
- 스토리지에서 샘플 파일 다운로드
- Apache POI를 이용한 엑셀 데이터 삽입 (sheet_info_setting JSON 기반 셀 위치 참조)
- 서명 이미지 / QR코드 / 발행일자 삽입 (결재 처리 시)
- LibreOffice 또는 외부 변환 도구를 통한 PDF 변환 (미확정)
- 처리 결과를 스토리지에 업로드 후 CALI에 콜백

#### 통신 프로토콜 (대략적 구상, 미확정)
- CALI → 작업서버: `POST /api/jobs/execute`
  - 포함 정보: batchId, 스토리지 접속 정보, envInfo(회사정보 + sheet_info_setting)
- 작업서버 → CALI: 각 item 처리 결과 콜백
  - 포함 정보: itemId, status, step, message
- 구체적인 프로토콜(필드명, 인증방식 등)은 구현 전 별도 합의 필요

#### 서버 운영 고려사항 (미확정)
- 작업서버 포트 / 인증 방식 (API Key? IP 화이트리스트?)
- 작업 서버 상태 헬스체크 방법
- 로컬 작업 디렉토리 구조 (UUID 기반 임시 디렉토리)
- 실패 처리: 임시 디렉토리 보존 기간, 재시도 정책
- WSL2 → 홈서버 → 클라우드 이전 시 설정 변경 범위

---

### Phase 3 — 기술책임자결재 페이지 구현 (CALI 내)

- 기술책임자 전용 결재 페이지 UI
- 결재 처리 API (work_status=SUCCESS인 성적서만 대상)
- signed 파일 교체 (soft delete + 새 insert)
- approval_status / report_status=COMPLETE 업데이트

---

### Phase 4 — 진행상황 고도화 (선택적)

- Polling → SSE 방식으로 전환 (Spring SseEmitter 활용)
- 작업 현황 모니터링 UI (진행률 바, 개별 성적서 상태 목록)
- 실패 재시도 기능

---

## 구현 진행 현황 (2026-03-20 기준)

---

### ✅ Phase 1 — CALI 내 연동 API 구현 (완료)

#### 1-1. DB 사전 작업 (완료)

| 항목 | 내용 |
|---|---|
| `report.expect_complete_date` | DATE NULL 컬럼 추가 (`v_260317.sql`) |
| `report.write_status` | VARCHAR NOT NULL DEFAULT 'IDLE' 추가. IDLE→PROGRESS→SUCCESS/FAIL 상태 관리 |
| `report_job_batch` | 배치 작업 테이블 신설 (job_type, status, total/success/fail_count 등) |
| `report_job_item` | 개별 item 테이블 신설 (batch_id, report_id, status, step, message 등) |
| `file_info.type` | VARCHAR NULL 추가 (origin / signed 구분) |

#### 1-2. CALI 엔티티 / DTO / 리포지토리 (완료)

- `entity/ReportJobBatch.java` — 배치 엔티티
- `entity/ReportJobItem.java` — item 엔티티 (step, message, retry_count 포함)
- `dto/ReportJobBatchDTO.java` — `CreateWriteBatchReq`, `CreateBatchRes`, `BatchStatusRes`, `ItemStatusRes`, `ItemCallbackReq`, `WorkerTriggerReq`
- `repository/ReportJobBatchRepository.java`
- `repository/ReportJobItemRepository.java` (`findByBatchId`)
- `common/enums/BatchStatus.java`, `JobItemStatus.java`, `JobType.java`

#### 1-3. 배치 생성 API (완료)

- `POST /api/report/jobs/batches`
- 대상 성적서 유효성 검증 (삭제여부, SELF 타입, 소분류 설정, 중복 진행 방지)
- batch(READY) + items(READY) 생성 → report.write_status = PROGRESS
- 작업 로그 기록

#### 1-4. 작업서버 트리거 (완료)

- `ReportJobBatchServiceImpl.triggerWorkerServer(batchId)` 구현
- `POST {app.worker.url}/api/jobs/execute` 호출 (RestClient)
- `app.worker.url` 미설정 시 트리거 생략(개발 모드)
- 트리거 실패 시: batch=FAIL, item=CANCELED, report.write_status=IDLE 복원

#### 1-5. Polling API (완료)

- `GET /api/report/jobs/batches/{batchId}` — 브라우저용. 배치+item 진행상황 반환 (세션 인증)

#### 1-6. 작업서버 콜백 수신 API (완료)

- `WorkerCallbackController` 신규 생성 (`/api/worker/**`, `permitAll` + API 키 검증)
- `GET /api/worker/batches/{batchId}` — 작업서버가 item 목록을 조회
- `POST /api/worker/items/{itemId}/callback` — 작업서버가 item 처리 결과 보고
  - PROGRESS → 배치 시작일시 기록, 배치 READY→PROGRESS 전환
  - SUCCESS → `report.write_status=SUCCESS`, `write_member_id`, `write_datetime` 갱신, batch.successCount++
  - FAIL → `report.write_status=FAIL`, batch.failCount++
  - 모든 item 완료 시 batch 최종 상태(SUCCESS/FAIL) + end_datetime 기록

#### 1-7. 프론트 — 성적서작성 모달 (완료)

- `templates/cali/reportWrite.html` — 소분류별 샘플 목록 표시 모달
- `static/js/cali/reportWrite.js` — 샘플 행 클릭 → 확인 → 배치 생성 API 호출

#### 1-8. 통신 프로토콜 확정 사항

| 항목 | 확정 내용 |
|---|---|
| 트리거 경로 | `POST {app.worker.url}/api/jobs/execute` |
| 트리거 헤더 | `X-Worker-Api-Key: {app.worker.api-key}` |
| 트리거 페이로드 | `batchId`, `callbackBaseUrl`, `workerApiKey`, 스토리지 접속정보 5종 |
| 콜백 경로 | `POST {callbackBaseUrl}/api/worker/items/{itemId}/callback` |
| 콜백 헤더 | `X-Worker-Api-Key: {workerApiKey}` |
| 콜백 페이로드 | `status`, `step`, `message` |
| 배치 조회 | `GET {callbackBaseUrl}/api/worker/batches/{batchId}` |

#### 설정 프로퍼티

```properties
app.worker.url=                          # 비어있으면 트리거 생략 (개발 모드)
app.worker.api-key=cali-worker-key-dev   # 운영 시 외부 properties에서 오버라이드
app.cali.callback-base-url=http://localhost:8050
```

---

### 🔲 Phase 2 — 성적서작업 워커 애플리케이션 구현 (다음 작업)

> 별도 Spring Boot 프로젝트. WSL2에 배포 후 CALI와 통신 테스트.

#### 2-1. 프로젝트 구조 (예정)

- `cali-worker` (가칭) — 독립 Spring Boot 3.x 프로젝트
- 포트: 8060 (미확정)
- 배포 대상: WSL2(로컬 개발) → 미니PC 홈서버 → 퍼블릭 클라우드

#### 2-2. 구현할 기능

| 기능 | 내용 |
|---|---|
| 트리거 수신 | `POST /api/jobs/execute` — batchId + 스토리지 설정 수신, API 키 검증 |
| 배치 정보 조회 | `GET {callbackBaseUrl}/api/worker/batches/{batchId}` 호출 → item 목록 확인 |
| 샘플 파일 다운로드 | NCP 오브젝트 스토리지에서 `sample` 파일 다운로드 |
| 임시 디렉토리 관리 | 배치 단위로 UUID 기반 임시 디렉토리 생성 → 작업 완료 후 정리 |
| 엑셀 데이터 삽입 | Apache POI — 각 성적서 raw 데이터를 샘플 엑셀에 삽입 |
| 스토리지 업로드 | 완성된 `origin.xlsx`를 `{rootDir}/report/{reportId}/report_origin.xlsx` 경로로 저장 |
| CALI 콜백 | 각 item 처리 단계마다 `POST /api/worker/items/{itemId}/callback` 호출 |
| 로컬 저장 (선택) | `app.local.save-enabled=true` 시 `/mnt/c/데이터시트/...` 동시 저장 (개발 편의) |

#### 2-3. 미확정 사항 (구현 전 합의 필요)

- 작업서버가 CALI report 상세 데이터를 조회하는 API 경로 (`GET /api/worker/reports/{reportId}`)
- 엑셀 셀 위치 매핑 방식 (`env.sheet_info_setting` JSON 구조 확정)
- item 처리 실패 시 임시 디렉토리 보존 정책 (즉시 삭제 vs 일정 기간 보존)
- WSL2 내 작업 디렉토리 경로 (`app.worker.work-dir`)

#### 2-4. WSL2 배포 및 통신 테스트 순서 (예정)

1. `cali-worker` 프로젝트 생성 및 기본 구조 구성
2. `POST /api/jobs/execute` 엔드포인트 구현 (트리거 수신 + 응답)
3. WSL2에 JAR 배포, CALI `app.worker.url=http://localhost:8060` 설정
4. CALI → 워커 트리거 → 워커 → CALI 콜백 흐름 엔드투엔드 통신 테스트
5. 실제 엑셀 작업 로직 순차 구현 (스토리지 다운로드 → POI 삽입 → 스토리지 업로드)

---

### 🔲 Phase 1 잔여 — 실무자결재 업로드/결재 API (미구현)

> Phase 2 워커 통신 테스트 이후 진행 예정

- `work_status` 기반 실무자결재 흐름 API
- 원본 파일 업로드(origin 교체) 및 결재(signed 파일 생성) API
- 다건 일괄 처리 지원

---

### 🔲 Phase 3 — 기술책임자결재 페이지 (미구현)

- 기술책임자 전용 결재 페이지 UI
- `work_status=SUCCESS` 조건부 결재 처리
- signed 파일 교체 (soft delete + 새 insert)
- `approval_status=SUCCESS`, `report_status=COMPLETE` 업데이트

