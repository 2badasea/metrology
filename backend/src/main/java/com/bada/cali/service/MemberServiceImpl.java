package com.bada.cali.service;

import com.bada.cali.common.YnType;
import com.bada.cali.dto.MemberDTO;
import com.bada.cali.entity.Agent;
import com.bada.cali.entity.AgentManager;
import com.bada.cali.entity.Log;
import com.bada.cali.entity.Member;
import com.bada.cali.mapper.AgentManagerMapper;
import com.bada.cali.mapper.AgentMapper;
import com.bada.cali.mapper.MemberMapper;
import com.bada.cali.repository.AgentManagerRepository;
import com.bada.cali.repository.AgentRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.MemberRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Log4j2
@AllArgsConstructor
public class MemberServiceImpl {
	private final MemberRepository memberRepository;
	private final AgentRepository agentRepository;
	private final AgentManagerRepository agentManagerRepository;
	private final LogRepository logRepository;
	
	private final AgentManagerMapper agentManagerMapper;
	private final AgentMapper agentMapper;
	
	private final PasswordEncoder passwordEncoder;
	private final MemberMapper memberMapper;
	
	/**
	 * 사용자 계정(loginId) 중복 체크
	 *
	 * @param loginId
	 * @param refPage
	 * @return
	 */
	@Transactional(readOnly = true)    // 조회만 함.
	public MemberDTO.DuplicateLoginIdRes chkDuplicateLoginId(String loginId, String refPage) {
		
		Optional<Member> optMember = memberRepository.findByLoginId(loginId, YnType.y);
		// 없는 경우 바로 리턴
		if (optMember.isEmpty()) {
			return new MemberDTO.DuplicateLoginIdRes(false, null, null);
		}
		
		Member member = optMember.get();
		
		// 기본값: 중복은 맞지만 업체 정보는 안 실어보냄(업체정보가 아예 없는 의미라면 빈 문자열 보다는 null이 의미에 가까움)
		String name = null;
		String addr = null;
		
		// 중복 존재
		// refPage의 경우 null일 경우 NPE 예외 발생 가능성 -> 아래처럼 문자열 상수를 왼쪽에 두면 null 이어도 안전
		if ("memberJoin".equalsIgnoreCase(refPage)) {
			// 회원가입에서 조회한 경우, 업체 정보까지 조회 후 리턴
			Long agentId = optMember.get().getAgentId();
			if (agentId > 0) {
				Optional<Agent> optAgent = agentRepository.findById(agentId);
				if (optAgent.isPresent()) {
					name = optAgent.get().getName();
					addr = optAgent.get().getAddr();
				}
			}
		}
		return new MemberDTO.DuplicateLoginIdRes(true, name, addr);
	}
	
	/**
	 * 회원가입 로직 수행
	 * 1. agent 테이블에 업체정보 우선 삽입 (create_type 확인)
	 * 2. 생성된 agent_id를 바탕으로 member에 사업자번호(agentNum)을 loginId로 하는 데이터 생성
	 * 3. 생성된 agent_id를 바탕으로 업체 담당자(Agent_manager) 테이블에 데이터 삽입
	 * 4. 생성되는 데이터에 대해선 모두 로그에 남길 것
	 * TODO 추후 slack api를 활용해서 관리자가 신규가입 업체를 확인할 수 있도록 한다.
	 *
	 * @param memberJoinReq
	 * @return
	 */
	@Transactional
	public MemberDTO.MemberJoinRes memberJoin(MemberDTO.MemberJoinReq memberJoinReq) {
		log.info("arrive at memberJoin service layer");
		
		// 1) insert agent
		Agent insertAgentEntity = agentMapper.toAgentFromMemberJoinReq(memberJoinReq);
		insertAgentEntity.setCreateType("join");	// 기본값 basic이 아닌 join으로 설정(mapper에서 @mapping 방법도 존재)
		insertAgentEntity.setAgentFlag(1);		// 기본가입 시, 신청업체로 간주
		insertAgentEntity.setCreateDatetime(LocalDateTime.now());	// 넣어주지 않으면 NULL를 insert하는 과정에서 DB의 NOT NULL제약에 걸림
		// save()는 성공하면 예외없이 엔티티를 그대로 리턴. 실패 시, 예외를 던짐 (null이나 false를 리턴하지 않음)
		// 이때 예외는 컨트롤러까지 전파 => 전역예외에서 캐치하여 에러페이지 or JSON 응답으로 가공해서 반환
		Agent savedAgent = agentRepository.save(insertAgentEntity);
		// 생성된 업체 id
		Long agentId = savedAgent.getId();
		
		// 2) inset agent_manager (업체 담당자 삽입)
		// agentId 값 할당
		AgentManager insertManagerEntity = agentManagerMapper.toEntityFromMemberJoinReq(memberJoinReq);
		insertManagerEntity.setAgentId(agentId);	// 업체id 세팅
		insertManagerEntity.setMainYn(YnType.y);	// 처음 등록된 담당자가 대표담당자가 되도록 값 변경
		insertManagerEntity.setCreateDatetime(LocalDateTime.now());
		AgentManager savedAgentManager = agentManagerRepository.save(insertManagerEntity);
		
		// 3) insert member
		Member insertMemberEntity = memberMapper.toMemberFromMemberJoin(memberJoinReq);
		insertMemberEntity.setJoinDate(LocalDate.now().toString());						// 가입일
		insertMemberEntity.setPwd(passwordEncoder.encode(memberJoinReq.getPwd()));		// 인코딩된 비밀번호
		insertMemberEntity.setAgentId(agentId);		// 업체 고유id
		insertMemberEntity.setCreateDatetime(LocalDateTime.now());
		// formatter 클래스를 이용한 방식
//		LocalDate today = LocalDate.now();
//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//		String todayStr = today.format(formatter);   // 예) "2025-11-21"
		Member savedMember =  memberRepository.save(insertMemberEntity);
		
		// 로그를 남긴다.
		String logContent = "[회원가입] 사용자 ID: " + savedMember.getLoginId() + ", 업체명: " + savedAgent.getName();
		Log insertSuccessLog = Log.builder()
				.logType("i")
				.refTable("member")
				.refTableId(savedMember.getId())
				.createDatetime(LocalDateTime.now())
				.createMemberId(0L)
				.logContent(logContent)
				.build();
		logRepository.save(insertSuccessLog);
		
		return new MemberDTO.MemberJoinRes(1, "가입신청이 완료되었습니다.");
	}
}
