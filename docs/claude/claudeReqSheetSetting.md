# [성적서시트] 작업 요청서

## 1. 작업 배경
- 실무자결재 메뉴에선 '성적서작성'이라는 기능이 존재합니다. 리스트에서 선택된 성적서(소분류가 모두 동일)를 대상으로 '성적서 작성'버튼 클릭 시, 모달이 호출되어 '분류코드관리(itemCode)' 메뉴에서 해당 소분류로 분류되어 저장되어 있는 엑셀파일들이 표시됩니다.
  이 엑셀파일을 선택하면 해당 샘플 성적서를 대상으로 리스트에서 선택된 성적서 갯수만큼 데이터가 채워진 파일이 생성되어 스토리지에 저장하여, '성적서결재' 작업을 진행시킬 수 있는 파일이 생성되는 작업입니다.
- '성적서시트' 메뉴는 샘플성적서별로 '데이터시트'라는  첫 번째 시트가 존재하는데, 여기에  성적서와 접수에 대한 정보들이 삽입되는 셀의 위치를 지정하는 역할을 합니다(어떤 항목인지는 아래 '화면구성' 목차에서 다시 언급하겠습니다). 셀의 위치는 물론, 포멧형식(예를 들어 날짜는 Y. m. d 인지, Y-m-d인지. 실무자&기술책임자의 이름은 '이 바 다', 형태인지,  '이 바다' 형태인지, '이바다' 형태인지 등)
- 성적서작성을 하게 되면 이 '성적서시트'에 대한 설정을 바탕으로 데이터를 가공해서 '데이터시트'의 각 정해진 위치에 삽입됩니다.

---

## 2. 작업 원칙
- 본 요청서를 먼저 확인한 뒤, **확인받아야 할 사항이 있다면 반드시 피드백을 요청할 것**
- **작업 단위를 분리하여 단계적으로 진행할 것**

---

### 참고사항
- '성적서시트' menu테이블에서 menu_code는 data_sheet_setting이며, url은 /basic/dataSheetSetting 입니다. 이미 테이블에 데이터는 들어가 있습니다.
  - 페이지를 반환하는 컨트롤러 핸들러도 없으며, 관련 html, js 파일도 없음

---
## 1. 화면 구성
- <table> 를 활용해서 항목 구성. 우측 상단에 '저장' 버튼 표시
- 아래 표시항목을 좌우로 카드2개로 형태로 분리
- 표시 항목
  - 헤더(th)영역: 항목명, 항목코드, 셀위치, 형식
    - '형식'의 경우 일부 항목만 존재(존재하는 항목은 같은 tr내에서 형식 갯수만큼 rowSpan분리)
  - 헤더 아래 영역  (순서대로 항목명, 항목코드, 셀위치, 형식에 해당. x는 빈 값으로 둘 것)
    - 신청업체(국)/custAgent/x/
    - 신청업체(영)/custAgentEn/x/x
    - 신청업체주소(국)/custAgentAddr/x/x
    - 신청업체주소(영)/custAgentAddrEn/x/x
    - 접수번호/orderNum/x/x
    - 성적서번호/ reportNum/x/x
    - 중분류코드/middleItemCodeNum/x/x
    - 소분류코드/smallItemcodeNum/x/x
    - 기기명(국)/itemName/x/x
    - 기기명(영)/itemNameEn/x/x
    - 제작회사(국)/makeAgent/x/x
    - 제작회사(영)/makeAgentEn/x/x
    - 형식/format/x/x
    - 기기번호/itemNum/x/x
    - 자산번호/assetNum/x/x
    - 교정장소/caliAddress/x/x
    - 소재지주소(국)/siteAddr/x/x
    - 소재지주소(영)/siteAddrEn/x/x
    - 교정일자/caliDate/x/yyyy-mm-dd, yyyy. mm. dd, yyyy. m. d(selected), yyyy-m-d
    - 교정주기/itemCaliCycle/x/x
    - 승인일자/approvalDate/x/yyyy-mm-dd, yyyy. mm. dd, yyyy. m. d(selected), yyyy-m-d
    - 최저온도/tempMin/x/x
    - 최고온도/tempMax/x/x
    - 최저습도/humMin/x/x
    - 최고습도/humMax/x/x
    - 최저기압/preMin/x/x
    - 최고기압/preMax/x/x
    - 실무자(국)/worker/x/x/indent, first_indent, dense(selected)
    - 실무자(영)/workerEn/x/indent, first_indent, dense(selected)
    - 기술책임자(국)/approval/x/x/indent, first_indent, dense(selected)
    - 기술책임자(영)/approvalEn/x/x/indent, first_indent, dense(selected)
    - 소급성문구(국)/traceStatement/x/
    - 소급성문구(영)/traceStatementEn/x/x
  - 헤더 아래 영역의 경우,  교정일자, 승인일자 , 실무자, 기술책임자의 경우 네 번째 열에 형식이 존재하는데, selected가 기본값으로 선택되어 있으며, 명시된 종류별로 radio로 체크가 가능해야 함.
  - 항목이 많기 때문에 이 페이지의 경우엔 y스크롤이 생겨도 상관없음. 좌우 card로 분리하고 y축 스크롤 여유를 두자.
  
## 2. 화면설명
2.1) 화면에서 입력할 수 있는 공간은 '셀위치'를 의미하는 세 번째 열과 네 번째 '형식'열의 일부 항목(교정주기, 실무자 등 radio타입)만 존재
2.2) '저장'클릭 시, '셀위치'열의 경우 입력값이 셀 형식에 맞는지 검증 필요(예를 들어서 숫자만 들어가는 형태도 안 되고 특수문자도 들어가면 안 됨. 빈 값은 허용)
2.3) [클로드와 대화할 사항]
  - 결국 '성적서 작성' 기능을 동작시키는 경우, 개별 성적서에 저장되어 있는 데이터를 각 항목별로 '셀위치'에 삽입시켜야 하는데, DB엔 env테이블에 'sheet_info_setting' 컬럼을 추가해서 JSON 형태로 저장을 할까 싶은데 어떻게 생각해? 셀에 데이터를 삽입하기 전에 env의 sheet_info_setting 필드값을 조회해서 셀위치와 저장된 형식에 따라서 데이터를 삽입하는 형식이야. 
  - 일단 env테이블에 'sheet_info_setting' 필드를 추가하고 entity 클래스도 수정이 필요할 것 같아.