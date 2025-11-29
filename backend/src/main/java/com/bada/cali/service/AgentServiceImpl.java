package com.bada.cali.service;

import com.bada.cali.common.YnType;
import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.AgentManagerDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.Agent;
import com.bada.cali.entity.AgentManager;
import com.bada.cali.entity.Log;
import com.bada.cali.mapper.AgentManagerMapper;
import com.bada.cali.mapper.AgentMapper;
import com.bada.cali.repository.AgentManagerRepository;
import com.bada.cali.repository.AgentRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.MemberRepository;
import com.bada.cali.security.CustomUserDetails;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
@AllArgsConstructor
public class AgentServiceImpl {
	
	private final AgentRepository agentRepository;
	private final AgentManagerRepository agentManagerRepository;
	private final MemberRepository memberRepository;
	private final LogRepository logRepository;
	// mapper
	private final AgentMapper agentMapper;        // dto <--> entity 간의 변환용 mapstruct
	private final AgentManagerMapper agentManagerMapper;
	
	// 업체관리 리스트 가져오기
	@Transactional
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
		Page<Agent> pageResult = agentRepository.searchAgents(isVisible, isClose, searchType, keyword, pageable);
		
		// entity -> DTO 변환
		List<AgentDTO.AgentRowData> rows = pageResult.getContent().stream()
				.map(agentMapper::toAgentRowDataFromEntity).toList();
		
		// 페이지네이션
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(req.getPage())
				.totalCount((int) pageResult.getTotalElements())
				.build();
		
		// 최종 return
		return TuiGridDTO.ResData.<AgentDTO.AgentRowData>builder()
				.contents(rows)
				.pagination(pagination)
				.build();
	}
	
	// 개별 업체 정보
	@Transactional
	public AgentDTO.AgentRowData getAgentInfo(Integer id) {
		YnType isVisible = YnType.y;    // 삭제되지 않은 것만 표기
		// entity에서 가져오자마자 DTO로 변환 후 api contorller로 리턴
		return agentMapper.toAgentRowDataFromEntity(agentRepository.findByIsVisibleAndId(isVisible, id));
	}
	
	// 업체 삭제
	// NOTE 삭제된 업체명들을 리스트에 담아서 리턴 -> 브라우저엔 삭제된 업체명을 알려주도록 변경
	@Transactional
	public List<String> deleteAgent(
			AgentDTO.DelAgentReq delAgentReq,
			CustomUserDetails user    // 컨트롤러에서 애너테이션을 명시했기 때문에 여기선 필요없음
	) {
		log.info("=========AgentServiceImpl.deleteAgent ============");
		log.info(delAgentReq.toString());    // 단순 toString()의 경우, 해시값 리턴
		
		// 삭제대상 업체 id
		List<Integer> agentIds = delAgentReq.getIds();
		// 값이 없는 경우 리턴
		if (agentIds == null || agentIds.isEmpty()) {
			throw new IllegalArgumentException("삭제할 업체가 없습니다.");
		}
		
		// 사용자 id(CustomUserDetails가 가지고 있는 인증 정보principal)
		int userId = user.getId();
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
		List<Integer> deleteAgentIds = targetNames.stream().map(Agent::getId).toList();
		
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
	@Transactional
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
		List<Integer> updateIds = req.getIds();
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
	@Transactional
	public TuiGridDTO.ResData<AgentManagerDTO.AgentManagerRowData> getAgentManagerList(AgentManagerDTO.GetListReq req) {
		
//		int pageIndex = req.getPage() - 1;    // JPA는 0-based
//		int pageSize = req.getPerPage();
		
//		Pageable pageable = PageRequest.of(pageIndex, pageSize); // Pageable 객체
		YnType isVisible = req.getIsVisible();
		YnType mainYn = YnType.y;
		int agentId = req.getAgentId();
		
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
	
}
