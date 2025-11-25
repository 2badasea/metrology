package com.bada.cali.dto;

import com.bada.cali.common.YnType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
		private String searchType;
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
		private Integer id;
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
		private int selfDiscount;
		private int outDiscount;
		private YnType isClose;
	}
	
	// 업체 삭제 요청 DTO
	@Setter
	@Getter
	@NoArgsConstructor
	public static class DelAgentReq {
		private List<Integer> ids;		// 브라우저에서 ids라는 key로 넘어옴
	}
	
	
}
