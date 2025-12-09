package com.bada.cali.dto;

import com.bada.cali.common.YnType;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

public class AgentDTO {
	
	// AgentDTO 클래스는 네임스페이스용도. 생성 방지
	private AgentDTO() {
	}
	
	;
	
	// 1. 그리드 DTO를 상속하는 형태로 구현. 도메인이나 페이지별로 넘어오는 값이 다를 수 있으므로
	// => TuiGridDTO.Request를 통해 page, perPage 사용 가능
	// 2. 검색조건은 도메인별로 자유롭게 정의가 가능하게 함(여기선 단일검색조건 & 단일키워드)
	@Getter
	@Setter
	public static class GetListReq extends TuiGridDTO.Request {
		// 검색창에서 넘어오는 값들
		private String isClose;		// '', y, n
		private String searchType;	// '', name, agentNum, addr
		private String keyword;
	}
	
	// 기본 개별 row 데이터 응답용. (유의미한 필드만 나열할 것)
	// 불변 DTO + @Builder 스타일로 할 경우, @Builder 추가 후, setter는 없앤 상태로 private final 선언
	/**
	 * NOTE 응답이지만 필요. mapstruct로 객체 생성 시, new로 기본생성자로 인스턴스를 생성하고
	 * 각 필드에 set메서드를 호출해서 값을 세팅하기 때문. 별도 생성자, 빌더 설정이 없는 상태라면
	 * setter를 선언해주는 것이 필요
	 */
	@Getter
	@Setter
	public static class AgentRowData {
		// 리스트에 데이터 출력할 때
		private Long id;
		private String createType;
		private String groupName;
		private String name;
		private String ceo;
		private String agentNum;
		private String addr;
		private String agentTel;
		private String email;
		private String manager;
		private String managerTel;
		private String remark;
		
		// 개별 업체정보(등록/수정)
		private String nameEn;
		private int agentFlag;
		private String agentZipCode;	// 우편번호
		private String businessType;
		private String businessKind;
		private String addrEn;
		private String phone;
		private String managerEmail;
		private String fax;
		private String accountNumber;
		private String calibrationCycle;
		private BigDecimal selfDiscount;
		private BigDecimal outDiscount;
		private YnType isClose;
		
		// 첨부파일 개수 (업체정보를 리턴할 때 같이 담아준다.
		private Integer fileCnt;
	}
	
	// 업체 삭제 요청 DTO
	@Setter
	@Getter		// 삭제대상 id값들을 객체에서 꺼내기 위해 선언
	@NoArgsConstructor
	public static class DelAgentReq {
		// 스크립트의 배열([]) 데이터는 java에서 List<Long>로 받을 수 있음.
		private List<Long> ids;		// 브라우저에서 ids라는 key로 넘어옴
	}
	
	// 그룹관리 그룹명 변경 요청 DTO
	@Setter
	@Getter
	@NoArgsConstructor
	public static class UpdateGroupNameReq {
		// 값이 존재하지 않으면, 컨트롤러 메서드가 호출되기 '직전'에 막혀서 예외가 던져짐 -> 전예외 차원에서 받게 됨
		@NotEmpty
		private List<Long> ids;
		
		private String groupName;		// 빈 문자열 허용 가능
	}
	
	// 업체 등록/수정 DTO 객체
	@Setter @Getter
	@NoArgsConstructor
	public static class SaveAgentDataReq {
		// 업체 정보
		private Long id;		// 업체 고유 id
		private Integer agentFlag;
		private String name;		// 업체명
		private String nameEn;		// 업체명(영문)
		private YnType isClose;		// 폐업여부 y/n
		private String calibrationCycle;	// 교정주기(자체, 차기, 표기안함)
		private String agentNum;		// 사업자등록번호
		private String ceo;				// 대표
		private String addr;			// 주소
		private String addrEn;			// 주소(영문)
		private String agentZipCode;	// 우편번호
		private BigDecimal selfDiscount;	// 자체할인율 (소수점 1자리까지 적용)
		private BigDecimal outDiscount;		// 대행할인율 (소수점 1자리까지 적용)
		private String businessType;		// 업태
		private String businessKind;		// 종목
		private String agentTel;		// 전화번호
		private String fax;			// 팩스번호
		private String email;		// 업체메일
		private String remark;		// 비고 (LONGTEXT)
		
		// 삭제 대상 업체담당자 정보
		List<Long> delManagerIds;
		
		// 업체 담당자 정보
		List<AgentManagerDTO.AgentManagerRowData> managers;
	}
	
	
}
