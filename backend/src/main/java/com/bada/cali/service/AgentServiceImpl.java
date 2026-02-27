package com.bada.cali.service;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.AgentManagerDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.Agent;
import com.bada.cali.entity.AgentManager;
import com.bada.cali.entity.Log;
import com.bada.cali.entity.Member;
import com.bada.cali.mapper.AgentManagerMapper;
import com.bada.cali.mapper.AgentMapper;
import com.bada.cali.repository.*;
import com.bada.cali.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@AllArgsConstructor
public class AgentServiceImpl {
	
	private final AgentRepository agentRepository;
	private final AgentManagerRepository agentManagerRepository;
	private final MemberRepository memberRepository;
	private final LogRepository logRepository;
	private final FileServiceImpl fileServiceImpl;
	// mapper
	private final AgentMapper agentMapper;        // dto <--> entity 간의 변환용 mapstruct
	private final AgentManagerMapper agentManagerMapper;
	private final PasswordEncoder passwordEncoder;
	
	// 업체관리 리스트 가져오기
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<AgentDTO.AgentRowData> getAgentList(AgentDTO.GetListReq req) {
		
		int pageIndex = req.getPage() - 1;     // JPA는 0-based
		int pageSize = req.getPerPage();
		
		Pageable pageable = PageRequest.of(pageIndex, pageSize);    // Pageable 객체
		YnType isVisible = YnType.y;                // 기본적으로 보이는 것만 노출(is_visible = 'y')
		
		YnType isClose = null;    // ""(전체선택) -> null(조건제외)
		String isCloseParam = req.getIsClose();    // "" | 'y' | 'n'
		if ("y".equalsIgnoreCase(isCloseParam)) {
			isClose = YnType.y;
		} else if ("n".equalsIgnoreCase(isCloseParam)) {
			isClose = YnType.n;
		}
		
		// 업체형태
		Integer agentFlag = req.getAgentFlag();
		// 없으면 기본적으로 0으로 주기
		if (agentFlag == null) {
			agentFlag = 0;
		}
		
		// searchType: ""(전체선택) -> null(조건제외)
		String searchType = req.getSearchType();    // 검색타입
		if (searchType == null || searchType.isBlank()) {
			searchType = "all";
		}
		searchType = switch (searchType) {
			case "all", "name", "agentNum", "addr" -> searchType;
			default -> "all";
		};
		String keyword = req.getKeyword();            // 검색키워드
		keyword = (keyword == null) ? "" : keyword.trim();
		
		// 분기 없이 1회 호출
		Page<AgentDTO.AgentRowData> pageResult = agentRepository.searchAgents(isVisible, isClose, searchType, keyword, agentFlag, YnType.y ,pageable);
		
		// NOTE 쿼리 한 번에 업체 담당자까지 가져옴으로 바로 dto타입으로 조회한다.
		List<AgentDTO.AgentRowData> rows = pageResult.getContent();
		// // entity -> DTO 변환
		// List<AgentDTO.AgentRowData> rows = pageResult.getContent().stream()
		// 		.map(agentMapper::toAgentRowDataFromEntity).toList();    // toList로 불변객체 생성
		
		// 페이지네이션
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(req.getPage())
				.totalCount((int) pageResult.getTotalElements())
				.build();
		
		// 최종 return
		return TuiGridDTO.ResData.<AgentDTO.AgentRowData>builder()
				.contents(rows)        // entity -> dto로 변환된 데이터 모두
				.pagination(pagination)
				.build();
	}
	
	// 개별 업체 정보
	@Transactional(readOnly = true)
	public AgentDTO.AgentRowData getAgentInfo(Long id) {
		YnType isVisible = YnType.y;    // 삭제되지 않은 것만 표기
		// entity에서 가져오자마자 DTO로 변환 후 api contorller로 리턴
		
		// 업체정보
		Agent agent = agentRepository.findByIsVisibleAndId(isVisible, id)
				.orElseThrow(() -> new EntityNotFoundException("업체를 찾을 수 없습니다."));
		AgentDTO.AgentRowData agentRowData = agentMapper.toAgentRowDataFromEntity(agent);
		
		// 첨부파일 존재하는지 확인 (refTableName, refTableId)
		int fileCount = fileServiceImpl.getFileInfos("agent", id).size();
		agentRowData.setFileCnt((long) fileCount);    // 첨부파일 개수 추가
		
		return agentRowData;
	}
	
	// 업체 삭제
	// NOTE 삭제된 업체명들을 리스트에 담아서 리턴 -> 브라우저엔 삭제된 업체명을 알려주도록 변경
	@Transactional
	public List<String> deleteAgent(
			AgentDTO.DelAgentReq delAgentReq,
			CustomUserDetails user    // 컨트롤러에서 애너테이션을 명시했기 때문에 여기선 필요없음
	) {
		
		// 삭제대상 업체 id
		List<Long> agentIds = delAgentReq.getIds();
		// 값이 없는 경우 리턴
		if (agentIds == null || agentIds.isEmpty()) {
			throw new IllegalArgumentException("삭제할 업체가 없습니다.");
		}
		
		// 사용자 id(CustomUserDetails가 가지고 있는 인증 정보principal)
		Long userId = user.getId();
		String userName = user.getName();
		LocalDateTime now = LocalDateTime.now();    // 삭제된 시간은 모두 동일하게.
		
		// 삭제전 삭제 대상 업체명 미리 확인
		List<Agent> targetNames = agentRepository.findAllByIdInAndIsVisible(agentIds, YnType.y);
		if (targetNames == null || targetNames.isEmpty()) {
			throw new IllegalArgumentException("삭제 가능한 업체가 없습니다.");    // 이미 삭제 or 없는 id
		}
		
		// 삭제대상 업체명
		// NOTE 컬렉션을 대상으로 stram API를 사용하는 방식 정의
		List<String> names = targetNames.stream().map(Agent::getName).toList();
		// 삭제대상 id (스트림으로 한번 더 조회. 동시성 이슈 확인)
		List<Long> deleteAgentIds = targetNames.stream().map(Agent::getId).toList();
		
		// 1. 업체 삭제 (return 값을 받을 필요는 없음. 에러가 발생한 경우, 트랜잭션과 함께 전역예외가 캐치)
		int delAgentCount = agentRepository.delAgentByIds(deleteAgentIds, YnType.n, now, userId);
		// 동시성/중복요청 등으로 인해 일부만 반영되는 상황을 강하게 실패 처리하고 싶으면 예외를 던짐
		if (delAgentCount != deleteAgentIds.size()) {
			throw new IllegalStateException("업체 삭제 중 일부가 반영되지 않았습니다.");
		}
		
		// 2. 업체 담당자 삭제
		agentManagerRepository.delAgentManagerByAgentIds(deleteAgentIds, YnType.n, now, userId);
		// 3. 사용자 삭제
		memberRepository.delMemberByAgentIds(deleteAgentIds, YnType.n, now, userId);
		
		
		int deletedCount = deleteAgentIds.size();
		String idList = deleteAgentIds.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(", "));
		String nameList = String.join(", ", names);
		String logContent = String.format(
				"[업체삭제] 삭제자: %s | 삭제건수: %d | 업체고유번호: [%s] | 업체명: [%s]",
				userName, deletedCount, idList, nameList
		);
		
		// 삭제에 대한 이력을 남긴다.
		Log deleteSuccessLog = Log.builder()
				.logType("d")
				.refTable("agent")
				.createDatetime(now)
				.workerName(userName)
//				.refTableId(0) // entity 객체에 @Builder.Default 선언 (기본값 0)
				.createMemberId(userId)
				.logContent(logContent)
				.build();
		logRepository.save(deleteSuccessLog);
		
		// 삭제된 업체 정보를 반환
		return names;
	}
	
	// 업체 그룹관리 그룹명 반환
	@Transactional(readOnly = true)
	public List<String> getGroupName() {
		// 그룹명 조회
		return agentRepository.findAllByIsVisibleAndGroupNameIsNotBlank(YnType.y);
		
		// 1. 그룹명만 반환하면 되기 때문에 List<String>으로 리턴타입 수정
//		List<String> agentList = agentRepository.findAllByIsVisibleAndGroupNameIsNotBlank(YnType.y);
//		return agentList.stream().map(Agent::getGroupName).toList();
		// NOTE isEmpty에 대한 분기처리 필요없음. 비어있으면 그냥 빈 리스트를 반환하기 때문
//		if (!agentList.isEmpty()) {
//			// 반환 후 수정이 필요없기 때문에 toList()
//			return agentList.stream().map(Agent::getGroupName).toList();
//		} else {
//			return null;
//		}
	}
	
	// 그룹명 수정
	@Transactional
	public int updateGroupName(AgentDTO.UpdateGroupNameReq req, CustomUserDetails user) {
		
		// 파라미터값 검증
		List<Long> updateIds = req.getIds();
		if (updateIds == null || updateIds.isEmpty()) {
			throw new IllegalArgumentException("업데이트 대상 id가 존재하지 않습니다.");
		}
		
		// 그룹명은 빈 값을 허용한다.
		String groupName = req.getGroupName().trim();
		
		// update문의 return 값은 영향을 받은 row의 수
		int resUpdate = agentRepository.updateAgentGroupName(updateIds, groupName);
		// 성공 시
		if (resUpdate > 0) {
			
			String ids = updateIds.stream().map(String::valueOf).collect(Collectors.joining(", "));
			String logContent = String.format("[업체관리] 그룹명을 '%s'로 수정하였습니다. 고유번호: [%s]", groupName, ids);
			
			// 이력을 남긴다.
			Log saveLog = Log.builder()
					.logType("u")
					.refTable("agent")
					.createDatetime(LocalDateTime.now())
					.workerName(user.getName())
					.createMemberId(user.getId())
					.logContent(logContent)
					.build();
			// 로그 저장
			logRepository.save(saveLog);
		}
		return resUpdate;
	}
	
	// 업체담당자 리스트 반환하기
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<AgentManagerDTO.AgentManagerRowData> getAgentManagerList(AgentManagerDTO.GetListReq req) {
		
		// 그리드에서 페이지네이션을 사용하지 않는 경우, 아래와 같은 데이터는 모두 필요없음.
//		int pageIndex = req.getPage() - 1;    // JPA는 0-based
//		int pageSize = req.getPerPage();
//		Pageable pageable = PageRequest.of(pageIndex, pageSize); // Pageable 객체
		YnType isVisible = req.getIsVisible();
		YnType mainYn = YnType.y;
		Long agentId = req.getAgentId();
		
		// 데이터 조회
		List<AgentManager> pageResult = agentManagerRepository.searchAgentManagers(isVisible, agentId, mainYn);
		
		// enity -> dto로 변경
		List<AgentManagerDTO.AgentManagerRowData> rows = pageResult.stream()
				.map(agentManagerMapper::toAgentManagerRowDataFromEntity).toList();
		
		// 페이지네이션
//		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
//				.page(req.getPage())
//				.totalCount((int) pageResult.size())
//				.build();
		
		// 최종 return
		return TuiGridDTO.ResData.<AgentManagerDTO.AgentManagerRowData>builder()
				.contents(rows)
//				.pagination(pagination)
				.build();
		
	}
	
	@Transactional
	public int saveAgent(AgentDTO.SaveAgentDataReq saveAgentDataReq, List<MultipartFile> files, CustomUserDetails user) {
		
		LocalDateTime now = LocalDateTime.now();
		
		// 업체 저장
		Long id = saveAgentDataReq.getId();    // null 가능
		boolean isNew = (id == null || id == 0);    // true 시 등록
		String saveTypeTxt = isNew ? "등록" : "수정";
		
		// 등록
		if (isNew) {
			Agent insertAgent = agentMapper.toAgentEntityFromDTO(saveAgentDataReq);
			insertAgent.setCreateType("auto");
			insertAgent.setCreateDatetime(now);
			insertAgent.setCreateMemberId(user.getId());
			Agent savedAgent = agentRepository.save(insertAgent);
			id = savedAgent.getId();    // id 변수에 대입
		}
		// 수정
		else {
			Agent originAgent = agentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("업체를 찾을 수 없습니다."));
			Agent updateAgent = agentMapper.toEntityFromUpdateDTO(saveAgentDataReq, originAgent);
			updateAgent.setUpdateDatetime(now);
			updateAgent.setUpdateMemberId(user.getId());        // 로그인 사용자
			agentRepository.save(updateAgent);
		}
		
		// 업체정보 수정에 대한 이력을 남긴다.
		String logContent = String.format("[업체정보 %s] 고유번호: %d", saveTypeTxt, id);
		Log insertUpdateLog = Log.builder()
				.logType(isNew ? "i" : "u")
				.refTable("agent")
				.refTableId(id)
				.workerName(user.getName())
				.createDatetime(now)
				.createMemberId(user.getId())
				.logContent(logContent)
				.build();
		logRepository.save(insertUpdateLog);
		
		// 업체형태(신청업체) & 사업자번호 존재여부에 따라 업체계정(member) 생성 (createType = 'auto')
		int agentFlag = saveAgentDataReq.getAgentFlag();
		String agentNum = saveAgentDataReq.getAgentNum();
		int chkFlag = 1;
		// 업체형태가 신청업체인 경우 & 사업자번호 항목이 존재하는 경우
		if ((agentFlag & chkFlag) != 0 && (agentNum != null && !agentNum.isEmpty())) {
			Optional<Member> agentUser = memberRepository.findByLoginId(agentNum, YnType.y);
			// 해당 업체정보의 계정이 존재하지 않는 경우, 신규 새성
			if (agentUser.isEmpty()) {
				// builder 객체 활용
				Member saveMember = Member.builder()
						.loginId(agentNum)                    // 로그인아이디(사업자번호)
						.pwd(passwordEncoder.encode("1234"))        // 임시비밀번호
						.createDatetime(now)                    // 생성일시
						.createMemberId(user.getId())            // 생성자
						.tel(saveAgentDataReq.getAgentTel())    // 연락처
						.email(saveAgentDataReq.getEmail())        // 이메일
						.name(saveAgentDataReq.getName())        // 이름(업체명)
						.agentId(id)                        // 업체id
						.build();
				Member resSavedMember = memberRepository.save(saveMember);
				// 신규생성 아이디에 대한 이력을 남긴다. (if문으로 resSavedMember가 null인지 체크 필요X, 실패시 예외 터지게 됨)
				logContent = String.format("[사용자 추가] 업체정보 %s에 따른 신규 계정 생성 고유번호: %d", saveTypeTxt, resSavedMember.getId());
				Log insertMemberLog = Log.builder()
						.logType("i")
						.refTable("member")
						.refTableId(resSavedMember.getId())
						.createDatetime(now)
						.workerName(user.getName())
						.createMemberId(user.getId())
						.logContent(logContent)
						.build();
				logRepository.save(insertMemberLog);
			}
		}
		
		// 업체담당자 삭제 체크
		List<Long> delManagerIds = saveAgentDataReq.getDelManagerIds();
		if (delManagerIds != null && !delManagerIds.isEmpty()) {
			agentManagerRepository.delAgentManagerByIds(delManagerIds, YnType.n, now, user.getId());
			// 삭제된 업체 담당자 id
			String managerIds = delManagerIds.stream().map(String::valueOf).collect(Collectors.joining(","));
			String delManagerLogContent = String.format("[업체담당자 삭제] 업체 정보 %s에 따른 담당자 삭제. 고유번호 - %s", saveTypeTxt, managerIds);
			
			// 삭제된 담당자에 대한 이력을 남긴다.
			Log delManagerLog = Log.builder()
					.logType("d")
					.refTable("agent_manager")
					.createDatetime(now)
					.workerName(user.getName())
					.createMemberId(user.getId())
					.logContent(delManagerLogContent)
					.build();
			logRepository.save(delManagerLog);
		}
		
		// 업체담당자 등록/수정
		List<AgentManagerDTO.AgentManagerRowData> managers = saveAgentDataReq.getManagers();
		// 담당자 정보 존재 시, 등록 or 수정
		if (managers != null && !managers.isEmpty()) {
			// FIX 현재 구조에선, 수정사항이 없는 담당자도 같이 update 대상이 되는 문제가 발생 (프론트에서 구분 필요)
			log.info("=== 담당자 정보 등록/수정 진입");
			// 등록/수정 분기처리를 위해 stream API 보단 for문 사용
			for (AgentManagerDTO.AgentManagerRowData row : managers) {
				
				// 등록
				if (row.getId() == null) {
					AgentManager newEntity = agentManagerMapper.toEntity(row);
					newEntity.setAgentId(id);
					newEntity.setCreateDatetime(now);
					newEntity.setCreateMemberId(user.getId());
					// 저장
					agentManagerRepository.save(newEntity);
				}
				// 수정
				else {
					// 기존 영속성 entity 객체 가져오기
					AgentManager existingEntity = agentManagerRepository.findById(row.getId()).orElseThrow(() -> new EntityNotFoundException("해당 업체 담당자를 찾지 못 했습니다."));
					
					// NOTE 별도로 save() 안 해도, @Transactional 이면 dirty checking으로 업데이트 함
					// mapstruct를 이용하여 값 덮어쓰기
					agentManagerMapper.updateEntity(row, existingEntity);
					existingEntity.setUpdateDatetime(now);
					existingEntity.setUpdateMemberId(user.getId());
				}
			}
		}
		// 첨부파일 확인 및 저장
		if (files != null && !files.isEmpty()) {
			log.info("=== 첨부파일 저장 진입. 파일 개수 {}", files.size());
			
			// FileService에서 모두 처리하도록 한다.
			String refTableName = "agent";
			String dir = String.format("agent/%d/", id);        // 버킷 내 디렉토리명
			
			// file_info 저장 및 스토리지 업로드 진행
			fileServiceImpl.saveFiles(refTableName, id, dir, files, user.getId());
		}
		
		// 예외가 터지지 않았다면 성공이므로 1 리턴
		return 1;
	}
	
	// 업체담당자 조회 모달용 리스트 반환 (대표여부 y 우선 정렬)
	@Transactional(readOnly = true)
	public List<AgentManagerDTO.AgentManagerRowData> getManagerList(Long agentId) {
		return agentManagerRepository.getManagerListOrderByMainYn(agentId);
	}

	// 업체명을 통해서 업체 정보 확인 후 리턴
	@Transactional(readOnly = true)
	public Map<String, String> chkAgentInfo(String agentName) {
		agentName = agentName.replaceAll(" ", "");	// 공백제거
		List<AgentDTO.AgentRowView> agentDatas = agentRepository.chkAgentInfos(agentName);
		
		// 데이터가 존재하는 경우
		if (!agentDatas.isEmpty()) {
			String agentInfosStr = agentDatas.stream().map(a -> "업체명: " + a.getName()
					+ ", 업체주소: " + a.getAddr()
					+ ", 사업자등록번호: " + a.getAgentNum()).collect(Collectors.joining("<br><br>"));
			return Map.of("code", "1", "data", agentInfosStr);
		} else {
			return Map.of("code", "-1");
		}
	}
	
}
