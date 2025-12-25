package com.bada.cali.dto;

import com.bada.cali.common.enums.DocType;
import com.bada.cali.common.enums.OrderType;
import com.bada.cali.common.enums.ReportStatus;
import com.bada.cali.common.enums.ReportType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

// 성적서 관련 DTO 클래스 집합
public class ReportDTO {
	
	// 'ReportDTO'는 네임스페이스. 임의 생성자 호출 방지
	private ReportDTO() {
	}
	
	;
	
	// 성적서 등록 시 넘어오는 파라미터
	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	public static class addReportReq {
		
		private String hierarchyType;    // 계층구조(부모, 자식), entity에 없음
		private Long caliFee;        // 교정수수료
		private DocType docType;        // 문서타입
		private Integer itemCaliCycle;        // 교정주기
		private String itemName;        // 품목명
		private String itemFormat;        // 형식
		private String itemNum;            // 기기번호
		private String itemMakeAgent;            // 제작회사
		private Long itemId;            // 품몸 고유 id
		private Long middleItemCodeId;    // 중분류 코드 id
		private Long smallItemCodeId;    // 소분류 코드 id
		private OrderType orderType;    // 접수구분 (공인, 비공인, 시험)
		private String remark;            // 비고
		private ReportType reportType;        // 성적서타입 (자체-SELF, 대행-AGCY)
		
		// 자식성적서
		private List<addReportReq> child;
	}
	
	// 접수상세내역 리스트에서 넘어온 검색필터 및 키워드
	@Getter
	@Setter
	public static class GetOrderDetailsReq extends TuiGridDTO.Request {
		// TODO 추후, 중분류/소분류 추가되면 필드 추가 선언할 것
		private String searchType;        // 검색타입
		private String keyword;            // 검색키워드
		private String statusType;        // 진행상태
		private OrderType orderType;        // 접수구분
		private Long caliOrderId;			// 접수id
	}
	
	// 클라이언트에 반환할 데이터
	@Setter
	@Getter
	public static class ReportRowData {
		// 중분류코드, 소분류코드 또한 추후에 가져와야 함
		private Long id;	// 성적서 고유 id
		private ReportType reportType;	// 구분(자체, 대행)
		private OrderType orderType;	// 접수타입
		private Long middleItemCodeNum;	// 중분류코드 (NOTE 추후에 join으로 가져와야 함)
		private Long smallItemCodeNum;	// 소분류코드 (NOTE 추후에 join으로 가져와야 함)
		private String reportNum;		// 성적서번호
		private String itemName;		// 기기명
		private String itemFormat;		// 기기 형식
		private String itemNum;			// 기기번호
		private Integer itemCaliCycle;		// 품목교정주기
		private String itemMakeAgent;	// 제작회사
		private String manageNo;		// 관리번호
		private ReportStatus reportStatus;	// 성적서 진행상태 (NOTE cf)요청 파라미터는 statusType이었음)
		private String remark;					// 비고
		private Long caliFee;				// 교정수수료
		private String statusRemark;			// 취소, 불가, 반려 사유
		
		private LocalDateTime approvalDatetime;	// 기술책임자 결재일시
		private LocalDateTime workDatetime;		// 실무자 결재일시
		
		
	}
	
}
