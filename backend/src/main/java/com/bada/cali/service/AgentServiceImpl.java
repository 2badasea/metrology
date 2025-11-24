package com.bada.cali.service;

import com.bada.cali.common.YnType;
import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.Agent;
import com.bada.cali.mapper.AgentMapper;
import com.bada.cali.repository.AgentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Log4j2
@AllArgsConstructor
public class AgentServiceImpl {
	
	private final AgentRepository agentRepository;	// DAO 주입
	private final AgentMapper agentMapper;		// dto <--> entity 간의 변환용 mapstruct
	
	// 업체관리 리스트 가져오기
	@Transactional
	public TuiGridDTO.Response<AgentDTO.AgentRowData> getAgentList(AgentDTO.GetListReq req) {
		
		int pageIndex = req.getPage() - 1;	 // JPA는 0-based
		int pageSize = req.getPerPage();
		
		// Pageable 객체
		Pageable pageable = PageRequest.of(pageIndex, pageSize);
		
		String searchType = req.getSearchType();	// 검색타입
		String keyword = req.getKeyword();			// 검색키워드
		YnType isVisible = YnType.y;				// 기본적으로 보이는 것만 노출(is_visible = 'y')
		
		// 결과를 담을 객체
		Page<Agent> pageResult;
		
		// 검색 키워드가 존재하지 않는 경우, 삭제된 업체 제외 모두 표시
		if (keyword == null || keyword.isBlank()) {
			pageResult = agentRepository.findByIsVisible(isVisible, pageable);
		} else {
			// 검색 키워드 및 검색 타입에 따른 구분
			if (searchType == null || searchType.isBlank()) {
				searchType = "all";		// 기본값을 전쳉검색으로
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
		YnType isVisible = YnType.y;	// 삭제되지 않은 것만 표기
		
		// entity에서 가져오자마자 DTO로 변환 후 api contorller로 리턴
		return agentMapper.toAgentRowDataFromEntity(agentRepository.findByIsVisibleAndId(isVisible, id));
	}
}
