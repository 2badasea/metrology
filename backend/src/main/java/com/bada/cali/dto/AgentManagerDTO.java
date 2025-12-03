package com.bada.cali.dto;

import com.bada.cali.common.YnType;
import lombok.Getter;
import lombok.Setter;

public class AgentManagerDTO {
	
	// 네임스페이스 용도. 생성자로서 호출 방지
	private AgentManagerDTO(){};
	
	// 업체관리 등록/수정 모달 내 그리드에서 리스트 요청
	@Setter
	@Getter
	public static class GetListReq {
		// 업체관리 정보에서 넘어오는 값
		private Integer agentId;				// 업체id
		private YnType isVisible;			// y: 존재하는 것만 호출
	}
	
	@Setter
	@Getter
	public static class AgentManagerRowData {
		private Integer id;			// 고유id
		private YnType mainYn;	// 대표여부
		private Integer agentId;	// 업체id
		private String name;	// 담당자 이름
		private String email;	// 담당자 이메일
		private String tel;		// 담당자 연락처
	}
	
	// 업체 조회 시, 담당자 데이터 조회
	
	
}
