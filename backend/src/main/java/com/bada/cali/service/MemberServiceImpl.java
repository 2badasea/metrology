package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.AuthType;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.MemberDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.*;
import com.bada.cali.mapper.AgentManagerMapper;
import com.bada.cali.mapper.AgentMapper;
import com.bada.cali.mapper.MemberCodeAuthMapper;
import com.bada.cali.mapper.MemberMapper;
import com.bada.cali.repository.*;
import com.bada.cali.repository.projection.GetMemberInfoPr;
import com.bada.cali.repository.projection.MemberCodeAuthPr;
import com.bada.cali.repository.projection.MemberListPr;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Log4j2
@AllArgsConstructor
public class MemberServiceImpl {
	private final MemberRepository memberRepository;
	private final AgentRepository agentRepository;
	private final AgentManagerRepository agentManagerRepository;
	private final LogRepository logRepository;
	private final FileServiceImpl fileServiceImpl;
	private final MemberCodeAuthRepository memberCodeAuthRepository;
	private final MemberCodeAuthMapper memberCodeAuthMapper;
	
	private final AgentManagerMapper agentManagerMapper;
	private final AgentMapper agentMapper;
	
	private final PasswordEncoder passwordEncoder;
	private final MemberMapper memberMapper;
	
	/**
	 * 로그인 성공 처리 (로그인 카운트 초기화, 마지막 로그인 일시 갱신, 비밀번호 만료 경고, 이력 저장)
	 *
	 * @param loginId  로그인 아이디
	 * @param clientIp 클라이언트 IP
	 * @return 응답 코드(1: 일반, 2: 관리자)와 환영 메시지
	 */
	@Transactional
	public ResMessage<Object> processLoginSuccess(String loginId, String clientIp) {
		Member loginMember = memberRepository.findByLoginId(loginId, YnType.y)
				.orElseThrow(() -> new IllegalStateException("Successful authentication but user not found"));

		LocalDateTime now = LocalDateTime.now();
		String resMsg = loginMember.getName() + "님 로그인을 환영합니다.";
		int resCode = 1;

		// 로그인 성공 시 실패 카운트 초기화
		if (loginMember.getLoginCount() > 0) {
			memberRepository.updateMemberLoginCount(loginMember.getId(), 0);
		}

		// 마지막 로그인 일시 업데이트
		memberRepository.updateLastLogin(loginMember.getId());

		// 마지막 비밀번호 변경일시 체크(3개월)
		LocalDateTime lastPwdUpdate = loginMember.getLastPwdUpdated();
		if (lastPwdUpdate != null && now.isAfter(lastPwdUpdate.plusMonths(3))) {
			resMsg += "<br>비밀번호를 마지막으로 변경한 지<br>3개월이 지났습니다.<br>변경을 권장드립니다.";
		}

		// 관리자 계정 구분
		if (loginMember.getAuth() == AuthType.admin) {
			resCode = 2;
		}

		// 로그인 성공 이력 저장
		Log successLog = Log.builder()
				.logIp(clientIp)
				.workerName(loginMember.getName())
				.logContent("[로그인 성공] - " + loginMember.getName() + " 님이 로그인 하셨습니다.")
				.logType("l")
				.refTable("member")
				.refTableId(loginMember.getId())
				.createDatetime(now)
				.createMemberId(loginMember.getId())
				.build();
		logRepository.save(successLog);

		return new ResMessage<>(resCode, resMsg, null);
	}

	/**
	 * 로그인 실패 처리 (실패 카운트 증가, 이력 저장)
	 * LockedException 등 계정 상태 예외는 Handler에서 처리하며, 이 메서드는 자격증명 실패 시만 호출.
	 *
	 * @param loginId  로그인 아이디
	 * @param clientIp 클라이언트 IP
	 */
	@Transactional
	public void processLoginFailure(String loginId, String clientIp) {
		Optional<Member> optLoginMember = memberRepository.findByLoginId(loginId, YnType.y);
		if (optLoginMember.isEmpty()) {
			log.debug("로그인 실패 - 존재하지 않는 아이디: {}", loginId);
			return;
		}

		Member tryLoginMember = optLoginMember.get();
		// count 0~4: +1 증가, count 5: 잠금 해제 후 재실패이므로 1로 리셋하여 새 카운트 시작
		if (tryLoginMember.getLoginCount() <= 5) {
			int updateCountValue = (tryLoginMember.getLoginCount() <= 4) ? (tryLoginMember.getLoginCount() + 1) : 1;
			memberRepository.updateMemberLoginCount(tryLoginMember.getId(), updateCountValue);
		}

		// 로그인 실패 이력 저장
		Log failLog = Log.builder()
				.logIp(clientIp)
				.workerName("")
				.logContent("[로그인 실패]")
				.logType("l")
				.refTable("member")
				.refTableId(tryLoginMember.getId())
				.createDatetime(LocalDateTime.now())
				.createMemberId(tryLoginMember.getId())
				.build();
		logRepository.save(failLog);
	}

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
		insertAgentEntity.setCreateType("join");    // 기본값 basic이 아닌 join으로 설정(mapper에서 @mapping 방법도 존재)
		insertAgentEntity.setAgentFlag(1);        // 기본가입 시, 신청업체로 간주
		insertAgentEntity.setCreateDatetime(LocalDateTime.now());    // 넣어주지 않으면 NULL를 insert하는 과정에서 DB의 NOT NULL제약에 걸림
		// save()는 성공하면 예외없이 엔티티를 그대로 리턴. 실패 시, 예외를 던짐 (null이나 false를 리턴하지 않음)
		// 이때 예외는 컨트롤러까지 전파 => 전역예외에서 캐치하여 에러페이지 or JSON 응답으로 가공해서 반환
		Agent savedAgent = agentRepository.save(insertAgentEntity);
		// 생성된 업체 id
		Long agentId = savedAgent.getId();
		
		// 2) inset agent_manager (업체 담당자 삽입)
		// agentId 값 할당
		AgentManager insertManagerEntity = agentManagerMapper.toEntityFromMemberJoinReq(memberJoinReq);
		insertManagerEntity.setAgentId(agentId);    // 업체id 세팅
		insertManagerEntity.setMainYn(YnType.y);    // 처음 등록된 담당자가 대표담당자가 되도록 값 변경
		insertManagerEntity.setCreateDatetime(LocalDateTime.now());
		AgentManager savedAgentManager = agentManagerRepository.save(insertManagerEntity);
		
		// 3) insert member
		Member insertMemberEntity = memberMapper.toMemberFromMemberJoin(memberJoinReq);
		insertMemberEntity.setJoinDate(LocalDate.now());                        // 가입일
		insertMemberEntity.setPwd(passwordEncoder.encode(memberJoinReq.getPwd()));        // 인코딩된 비밀번호
		insertMemberEntity.setAgentId(agentId);        // 업체 고유id
		insertMemberEntity.setCreateDatetime(LocalDateTime.now());
		// formatter 클래스를 이용한 방식
//		LocalDate today = LocalDate.now();
//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//		String todayStr = today.format(formatter);   // 예) "2025-11-21"
		Member savedMember = memberRepository.save(insertMemberEntity);
		
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
	
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<MemberListPr> getMemberList(MemberDTO.GetMemberListReq req) {
		log.info("데이터 확인");
		
		int pageIndex = req.getPage() - 1;
		int perPage = req.getPerPage();
		
		PageRequest pageRequest = PageRequest.of(pageIndex, perPage);
		
		// 검색타입 정리
		Integer workType = req.getWorkType();        // 재직여부
		String searchType = req.getSearchType();    // 검색타입
		String keyword = req.getKeyword();            // 키워드
		
		if (searchType == null || searchType.isBlank()) {
			searchType = "all";
		}
		keyword = (keyword == null) ? "" : keyword.trim();
		YnType isVisible = YnType.y;
		
		Page<MemberListPr> pageResult = memberRepository.getMemberList(workType, searchType, keyword, isVisible, pageRequest);
		
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(req.getPage())
				.totalCount((int) pageResult.getTotalElements())
				.build();
		
		return TuiGridDTO.ResData.<MemberListPr>builder()
				.pagination(pagination)
				.contents(pageResult.getContent())
				.build();
	}
	
	// 회원정보를 등록 수정한다.
	@Transactional
	public ResMessage<Long> memberSave(MemberDTO.SaveMemberInfo req, CustomUserDetails user) {
		int resCode = 0;
		String resMsg = "";
		
		LocalDateTime now = LocalDateTime.now();
		Long userId = user.getId();
		String workerName = user.getName();
		
		Long id = req.id();
		
		String reqPwd = req.pwd();
		if (reqPwd != null && !reqPwd.isBlank()) {
			reqPwd = passwordEncoder.encode(reqPwd);    // 비밀번호 암호화
		}
		
		String saveTypeKr = (id == null || id <= 0) ? "등록" : "수정";
		Member updatedMember = null;
		
		// 등록
		if (id == null || id <= 0) {
			// 로그인 아이디의 중복체크를 진행한다.
			String loginId = req.loginId();
			Optional<Member> optMember = memberRepository.findByLoginId(loginId, YnType.y);
			// 중복되는 게 존재하는 경우 리턴
			if (optMember.isPresent()) {
				resCode = -1;
				resMsg = "중복되는 아이디가 존재합니다.";
				return new ResMessage<>(resCode, resMsg, null);
			}
			
			Member createMember = memberMapper.toMemberByCreateReq(req);
			createMember.setCreateDatetime(now);
			createMember.setCreateMemberId(userId);
			createMember.setLastPwdUpdated(now);    // 마지막 로그인 일시 업데이트
			createMember.setAgentId(0L);        // 자사 직원은 모두 0
			
			updatedMember = memberRepository.save(createMember);
			id = updatedMember.getId();
		}
		// 수정
		else {
			
			Member originMember = memberRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("수정할 회원정보를 찾지 못 했습니다."));
			
			// record값 덮어씌우기
			memberMapper.updateMemberByReq(req, originMember);
			originMember.setUpdateMemberId(userId);
			originMember.setUpdateDatetime(now);
			
			updatedMember = originMember;
		}
		
		// 넘어온 비밀번호 값이 있다면 update
		if (reqPwd != null && !reqPwd.isBlank()) {
			updatedMember.updatePwd(reqPwd);
		}
		
		// 이미지 확인
		MultipartFile memberImage = req.memberImage();
		if (memberImage != null && !memberImage.isEmpty()) {
			// 기존에 이미지가 존재하는 경우, 삭제(soft-delete()후, 새로운 이미지를 추가하는 함수 호출
			fileServiceImpl.softDeleteAndInsert(
					"member",
					id,
					memberImage,
					userId
			);
		}
		
		List<MemberDTO.MemberAuthData> itemAuthData = req.itemAuthData();
		if (!itemAuthData.isEmpty()) {
			// 중분류 코드 일괄 삭제 후, 다시 저장 (List에 담아서 일괄 saveAll처리)
			int resDeleteItemAuth = memberCodeAuthRepository.deleteMemberCodeAuth(id);
			// 반복문을 통해 Entity생성
			List<MemberCodeAuth> saveCoeAuthList = new ArrayList<>();
			
			for (MemberDTO.MemberAuthData data : itemAuthData) {
				saveCoeAuthList.add(memberCodeAuthMapper.toEntity(data, id));
			}
			// 일괄저장
			if (!saveCoeAuthList.isEmpty()) {
				memberCodeAuthRepository.saveAll(saveCoeAuthList);
			}
		}
		resCode = 1;
		resMsg = "저장 성공";

		// 등록/수정 로그 저장
		String logType = (req.id() == null || req.id() <= 0) ? "i" : "u";
		String logContent = "[직원 " + saveTypeKr + "] 고유번호 - [" + id + "]";
		Log saveLog = Log.builder()
				.logType(logType)
				.refTable("member")
				.refTableId(id)
				.logContent(logContent)
				.workerName(workerName)
				.createDatetime(now)
				.createMemberId(userId)
				.build();
		logRepository.save(saveLog);

		return new ResMessage<>(resCode, resMsg, id);
	}
	
	// 회원정보를 가져온다 (직원등록/수정 페이지)
	@Transactional(readOnly = true)
	public ResMessage<MemberDTO.GetMemberInfoSet> getMemberInfo(Long memberId) {
		int resCode = 0;
		String resMsg = "";
		
		YnType isVisible = YnType.y;
		GetMemberInfoPr getMemberInfo = memberRepository.getMemberInfo(memberId, isVisible);
		Long imgFileInfoId = getMemberInfo.getImgFileId();
		String memberImgPath = "";
		if (imgFileInfoId != null && imgFileInfoId > 0) {
			String dir = getMemberInfo.getDir();
			String extension = getMemberInfo.getExtension();
			memberImgPath = fileServiceImpl.getFilePath(
					dir, imgFileInfoId, extension
			);
		}
		
		// 중분류 권한 가져오기
		List<MemberCodeAuthPr> itemAuthData = memberCodeAuthRepository.getMemberCodeAuth(memberId);
		
		MemberDTO.GetMemberInfoSet data = new MemberDTO.GetMemberInfoSet(getMemberInfo, memberImgPath, itemAuthData);
		resCode = 1;
		return new ResMessage<>(resCode, resMsg, data);
	}
	
}
