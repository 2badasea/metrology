package com.bada.cali.service;

import com.bada.cali.common.YnType;
import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.Agent;
import com.bada.cali.entity.Log;
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
	private final AgentMapper agentMapper;        // dto <--> entity 간의 변환용 mapstruct
	
	// 업체관리 리스트 가져오기
	@Transactional
	public TuiGridDTO.Response<AgentDTO.AgentRowData> getAgentList(AgentDTO.GetListReq req) {
		
		int pageIndex = req.getPage() - 1;     // JPA는 0-based
		int pageSize = req.getPerPage();
		
		// Pageable 객체
		Pageable pageable = PageRequest.of(pageIndex, pageSize);
		
		String searchType = req.getSearchType();    // 검색타입
		String keyword = req.getKeyword();            // 검색키워드
		YnType isVisible = YnType.y;                // 기본적으로 보이는 것만 노출(is_visible = 'y')
		
		// 결과를 담을 객체
		Page<Agent> pageResult;
		
		// 검색 키워드가 존재하지 않는 경우, 삭제된 업체 제외 모두 표시
		if (keyword == null || keyword.isBlank()) {
			pageResult = agentRepository.findByIsVisible(isVisible, pageable);
		} else {
			// 검색 키워드 및 검색 타입에 따른 구분
			if (searchType == null || searchType.isBlank()) {
				searchType = "all";        // 기본값을 전쳉검색으로
			}
			switch (searchType) {
				case "name" -> {
					pageResult = agentRepository.findByIsVisibleAndNameContaining(isVisible, keyword, pageable);
				}
				case "agentNum" -> {
					pageResult = agentRepository.findByIsVisibleAndAgentNumContaining(isVisible, keyword, pageable);
				}
				case "addr" -> {
					pageResult = agentRepository.findByIsVisibleAndAddrContaining(isVisible, keyword, pageable);
				}
				default -> {
					// 혹시 이상한 값 들어오면 전체검색으로 수행
					pageResult = agentRepository.searchAllVisible(isVisible, keyword, pageable);
				}
			}
		}
		
		// entity -> DTO 변환
		List<AgentDTO.AgentRowData> rows = pageResult.getContent().stream()
				.map(agentMapper::toAgentRowDataFromEntity).toList();
		log.info("=============== rows ==============");
		log.info(rows.toString());
		
		// 페이지네이션
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(req.getPage())
				.totalCount((int) pageResult.getTotalElements())
				.build();
		
		// 최종 return
		return TuiGridDTO.Response.<AgentDTO.AgentRowData>builder()
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
	
	
}
