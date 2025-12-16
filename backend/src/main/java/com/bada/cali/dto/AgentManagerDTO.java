package com.bada.cali.dto;

import com.bada.cali.common.enums.YnType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AgentManagerDTO {
	
	// 네임스페이스 용도. 생성자로서 호출 방지
	private AgentManagerDTO(){};
	
	// 업체관리 등록/수정 모달 내 그리드에서 리스트 요청
	@Setter
	@Getter
	public static class GetListReq {
		// 업체관리 정보에서 넘어오는 값
		private Long agentId;				// 업체id
		private YnType isVisible;			// y: 존재하는 것만 호출
	}
	
	@Setter
	@Getter
	@NoArgsConstructor	// dto로 바로 반환받기 위한 기본생성자를 사용하기 위해
	public static class AgentManagerRowData {
		private Long id;			// 고유id
		private YnType mainYn;	// 대표여부
		private Long agentId;	// 업체id
		private String name;	// 담당자 이름
		private String email;	// 담당자 이메일
		private String tel;		// 담당자 연락처
		
		public AgentManagerRowData (
				Long id,
				Long agentId,
				String name,
				String tel,
				String email,
				YnType mainYn
		) {
			this.id = id;
			this.name = name;
			this.agentId = agentId;
			this.mainYn = mainYn;
			this.email = email;
			this.tel = tel;
		}
	}
	
}
