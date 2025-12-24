package com.bada.cali.dto;

import com.bada.cali.common.enums.DocType;
import com.bada.cali.common.enums.OrderType;
import com.bada.cali.common.enums.ReportType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

// 성적서 관련 DTO 클래스 집합
public class ReportDTO {
	
	// 'ReportDTO'는 네임스페이스. 임의 생성자 호출 방지
	private ReportDTO () {};
	
	// 성적서 등록 시 넘어오는 파라미터
	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	public static class addReportReq {
		
		private String hierarchyType;	// 계층구조(부모, 자식), entity에 없음
		private Long caliFee;		// 교정수수료
		private DocType docType;		// 문서타입
		private Integer itemCaliCycle;		// 교정주기
		private String itemName;		// 품목명
		private String itemFormat;		// 형식
		private String itemNum;			// 기기번호
		private String itemMakeAgent;			// 제작회사
		private Long itemId;			// 품몸 고유 id
		private Long middleItemCodeId;	// 중분류 코드 id
		private Long smallItemCodeId;	// 소분류 코드 id
		private OrderType orderType;	// 접수구분 (공인, 비공인, 시험)
		private String remark;			// 비고
		private ReportType reportType;		// 성적서타입 (자체-SELF, 대행-AGCY)
		
		// 자식성적서
		private List<addReportReq> child;
	}
}
