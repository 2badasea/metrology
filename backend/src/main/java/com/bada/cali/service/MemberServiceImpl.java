package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.YnType;
import com.bada.cali.dto.ResDuplicateLoginIdDTO;
import com.bada.cali.entity.Agent;
import com.bada.cali.entity.Member;
import com.bada.cali.repository.AgentRepository;
import com.bada.cali.repository.MemberRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Log4j2
@AllArgsConstructor
public class MemberServiceImpl {
	private final MemberRepository memberRepository;
	private final AgentRepository agentRepository;
	
	/**
	 * 사용자 계정(loginId) 중복 체크
	 *
	 * @param loginId
	 * @param refPage
	 * @return
	 */
	@Transactional(readOnly = true)    // 조회만 함.
	public ResDuplicateLoginIdDTO chkDuplicateLoginId(String loginId, String refPage) {
		
		Optional<Member> optMember = memberRepository.findByLoginId(loginId, YnType.y);
		// 없는 경우 바로 리턴
		if (optMember.isEmpty()) {
			return new ResDuplicateLoginIdDTO(false, null, null);
		}
		
		Member member = optMember.get();
		
		// 기본값: 중복은 맞지만 업체 정보는 안 실어보냄(업체정보가 아예 없는 의미라면 빈 문자열 보다는 null이 의미에 가까움)
		String name = null;
		String addr = null;
		
		// 중복 존재
		// refPage의 경우 null일 경우 NPE 예외 발생 가능성 -> 아래처럼 문자열 상수를 왼쪽에 두면 null 이어도 안전
		if ("memberJoin".equalsIgnoreCase(refPage)) {
			// 회원가입에서 조회한 경우, 업체 정보까지 조회 후 리턴
			int agentId = optMember.get().getAgentId();
			if (agentId > 0) {
				Optional<Agent> optAgent = agentRepository.findById(agentId);
				if (optAgent.isPresent()) {
					name = optAgent.get().getName();
					addr = optAgent.get().getAddr();
				}
			}
		}
		return new ResDuplicateLoginIdDTO(true, name, addr);
	}
}
