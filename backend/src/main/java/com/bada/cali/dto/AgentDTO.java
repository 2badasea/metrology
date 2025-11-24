package com.bada.cali.dto;

import lombok.Getter;
import lombok.Setter;

public class AgentDTO {
	
	// AgentDTO 클래스는 네임스페이스용도. 생성 방지
	private AgentDTO() {
	};
	
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
	
	@Getter
	@Setter
	public static class AgentRowData {
		private Integer id;
		private String createType;
		private String name;
		private String ceo;
		private String agentNum;
		private String addr;
		private String agentTel;
		private String email;
		private String manager;
		private String managerTel;
		private String remark;
	}
	
	
}
