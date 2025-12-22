package com.bada.cali.dto;

import com.bada.cali.common.enums.CalibrationCycleType;
import com.bada.cali.common.enums.YnType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

// 교정관련 DTO 클래스
public class CaliDTO {
	
	// 'CaliDTO' 클래스는 네임스페이스 용도. 임의로 생성 방지
	private CaliDTO() {
	}
	
	;
	
	// 교정접수 페이지 리스트에서 넘어오는 요청 DTO
	@Getter
	@Setter
	public static class GetOrderListReq extends TuiGridDTO.Request {
		// 검색창에서 넘어오는 값들 정리
		// 검색창에서 넘어오는 값들
		private String orderStartDate;    // 접수일 조회 시작일
		private String orderEndDate;    // 접숭리 조회 종료일
		private String isTax;            // 세금계산서 발행여부
		private String caliType;        // 접수유형 (standard/ site)
		private String statusType;        // 진행상태
		private String searchType;        // 검색타입
		private String keyword;            // 검색 입력 키워드
	}
	
	// 접수 데이터를 반환하기 위한 DTO
	@Getter
	@Setter
	public static class OrderRowData {
		
		// 리스트에 명시적으로 표현되는 데이터
		private Long id;                        // 접수id
		private String priorityType;            // 긴급여부
		private String orderType;                // 접수구분 (공인/비공인/시험 등)
		private String caliType;                // 교정유형 (고정표준실/ 현장교정)
		private String orderNum;                // 접수번호
		private String custAgent;                // 신청업체
		private String reportAgent;                // 성적서발행처
		private String reportAgentAddr;            // 성적서발행처 주소
		private String btripStartDate;            // 출장시작일
		private String btripEndDate;            // 출장종료일
		private String remark;                    // 요청사항
		private YnType isTax;                    // 세금계산서 발행여부
		private Long completionId;                // 완료통보서id
		private String statusType;            // 진행상태 (wait, complete, hold, cancel)
		private String orderDate;                // 접수일
		
		// 추가적으로 가져올 것
		private Long reportTotalCnt;            // 전체성적서 개수
		private Long selfReportCount;            // 자체성적서 개수
		private Long outReportCount;            // 대행성적서 개수
		
		// 접수등록/수정에 모달 데이터
		private String custAgentTel;            // 신청업체 대표번호
		private String custAgentFax;            // 신청업체 FAX
		
		
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	public static class saveCaliOrder {
		
		private Long id;                        // 접수id
		
		@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
		private LocalDateTime btripStartDate;            // 출장시작일
		@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
		private LocalDateTime btripEndDate;            // 출장종료일
		
		private String caliTakeType;            // 접수유형
		private String caliType;                // 교정유형 (고정표준실/ 현장교정)
		private String custAgent;                // 신청업체
		private String custAgentAddr;            // 신청업체 주소 (국문)
		private String custAgentAddrEn;            // 신청업체 주소 (영문)
		private CalibrationCycleType custAgentCaliCycle;        // 신청업체 교정주기
		private String custAgentEn;                // 신청업체 영문명
		private String custAgentFax;            // 신청업체 FAX
		private Long custAgentId;                // 신청업체 id
		private String custAgentTel;            // 신청업체 연락처
		private String custManager;                // 신청업체 담당자
		private String custManagerEmail;        // 신청업체 담당자 이메일
		private String custManagerTel;            // 신청업체 담당자 연락처
		private YnType isTax;                    // 세금계산서 발행여부
		private String orderDate;                // 접수일
		private Long orderManagerId;            // 접수자
		private String priorityType;            // 긴급여부
		private String remark;                    // 요청사항
		private String reportAgent;                // 성적서발행처
		private String reportAgentAddr;            // 성적서발행처 주소
		private String reportAgentAddrEn;        // 성적서발행처 주소 (영문)
		private String reportAgentEn;            // 성적서발행처 영문명
		private Long reportAgentId;            // 성적서발행처 id
		private String reportLang;                // 성적서 발행타입
		private String reportManager;            // 성적서발행처 담당자
		private String reportManagerEmail;        // 성적서발행처 담당자 이메일
		private String reportManagerTel;        // 성적서발행처 담당자 연락처
		private String responsibleManagerId;    // 책임담당자 id
		private String siteAddr;                // 소재지 주소
		private String siteAddrEn;                // 소재지 주소 (영문)
		
		private String orderNum;                // 접수번호
		
	}
	
}
