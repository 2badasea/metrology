package com.bada.cali.dto;

import lombok.*;

public class MemberDTO {
	
	// MemberDTO 클래스는 네임스페이스 용도이므로 생성방지
	private MemberDTO() {};
	
	// 회원가입 사업자번호 중복검사
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class DuplicateLoginIdRes {
		private boolean isDuplicate;
		private String agentName;
		private String agentAddr;
	}
	
	
	// 회원가입 요청 DTO
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MemberJoinReq {
		// 사용자계정(업체) & 업체정보(Agent)의 사업자번호(agent_num)
		private String loginId;
		
		// 업체명(agent)
		private String name;
		
		// 업체(agent) 우편번호
		private String agentZipCode;
		
		// 업체(agent) 주소
		private String addr;
		
		// 사용자(member) 비밀번호 (암호화 후 DB에 삽입)
		private String pwd;
		
		// 업체담당자명(agent) & 업체담당자(agent_manager)의 담당자명
		private String manager;
		
		// 업체대표명(agent)
		private String ceo;
		
		// 담당자(agent, agent_manager)
		private String email;
		
		// 담당자 연락처(agent, agent_manager)
		private String phone;
		
		// 업체번호(agent)
		private String agentTel;
	}
	
	// 회원가입 응답(response) DTO
	@Getter
	@AllArgsConstructor
//	@Builder 필드 2개만 있는 경우, 굳이 builder패턴보다는 new연산자로 생성해서 반환할 것
	public static class MemberJoinRes {
		private final int code;		// 생성시점에 불변에 가깝게 동작시키기 위해 final & @Setter 미선언
		private final String msg;
	}
	
	
	// 직원관리 토스트 그리드 dto
	@Setter
	@Getter
	@NoArgsConstructor
	public static class GetMemberListReq extends TuiGridDTO.Request {
		Integer workType;		// 재직여부 (0: 재직, 1: 휴직, 2: 퇴사)
		String searchType;		// 검색타입
		String keyword;			// 검색 키워드
	}
	
	// 직원정보 등록 데이터셋 (직원 수정)
	public record CreateMemberInfo (
		
	) {}
	
}
