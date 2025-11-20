package com.bada.cali.dto;

import lombok.*;

// 회원가입 요청 DTO
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RegMemberJoinDTO {
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

