package com.bada.cali.service;

import com.bada.cali.common.YnType;
import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.CaliOrder;
import com.bada.cali.mapper.CaliOrderMapper;
import com.bada.cali.repository.CaliOrderRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.security.CustomUserDetails;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Log4j2
@AllArgsConstructor
public class CaliOrderServiceImpl {
	
	private final LogRepository logRepository;
	private final CaliOrderRepository caliOrderRepository;
	private final CaliOrderMapper caliOrderMapper;
	
	// 교정접수 리스트 가져오기
	@Transactional(readOnly = true)		// 조회 전용이기에 readonly = true 명시
	public TuiGridDTO.ResData<CaliDTO.OrderRowData> getOrderList(CaliDTO.GetOrderListReq request) {
		
		int pageIndex = request.getPage() - 1;
		int pageSize = request.getPerPage();
		
		Pageable pageable = PageRequest.of(pageIndex, pageSize);    // Pageable 객체
		
		// 날짜 데이터는 문자열로 넘어오더라도 datetime으로 변경해서 리파지토리에 보내기 (JPQL에서 function 사용 방지 - DB종속적인 구조 방지)
		LocalDateTime startDateTime = null;
		LocalDateTime endDateTime = null;
		
		// 값이 존재하는지 파악 후 datetime 값으로 변경
		if (request.getOrderStartDate() != null && !request.getOrderStartDate().isBlank()) {
			LocalDate startDate = LocalDate.parse(request.getOrderStartDate());
			startDateTime = startDate.atStartOfDay();
		}
		if (request.getOrderEndDate() != null && !request.getOrderEndDate().isBlank()) {
			LocalDate endDate = LocalDate.parse(request.getOrderEndDate());
			endDateTime = endDate.atStartOfDay();
		}
		
		// 세금계산서 발행여부
		YnType isTax = null;
		String isTaxFlag = request.getIsTax();
		if ("y".equals(isTaxFlag)) {
			isTax = YnType.y;
		} else if ("n".equals(isTaxFlag)) {
			isTax = YnType.n;
		}
		
		// 교정유형
		String caliType = request.getCaliType();
		if (caliType == null || caliType.isEmpty()) {
			caliType = "all";
		}
		caliType = switch (caliType) {
			case "all", "standard", "site" -> caliType;
			default -> "all";
		};
		
		// 진행상태 체크
		String statusType = request.getStatusType();
		if (statusType == null || statusType.isEmpty()) {
			statusType = "all";
		}
		statusType = switch (statusType) {
			case "all", "wait", "complete", "hold", "cancel" -> statusType;
			default -> "all";
		};
		
		// 검색타입 및 키워드 체크
		// searchType: ""(전체선택) -> null(조건제외)
		String searchType = request.getSearchType();    // 검색타입
		if (searchType == null || searchType.isBlank()) {
			searchType = "all";
		}
		searchType = switch (searchType) {
			case "all", "orderNum", "custAgent", "reportAgent", "reportAgentAddr", "remark" -> searchType;
			default -> "all";
		};
		String keyword = request.getKeyword();            // 검색키워드
		keyword = (keyword == null) ? "" : keyword.trim();
		
		YnType isVisible = YnType.y;        // 기본적으로 삭제된 건 노출하지 않음.
		// 분기처리 없이 데이터 가져오기
		Page<CaliOrder> pageResult = caliOrderRepository.searchOrders(isVisible, startDateTime, endDateTime, isTax, caliType, statusType, searchType, keyword, pageable);
		
		
		// entity -> DTO 변환
		List<CaliDTO.OrderRowData> rows = pageResult.getContent().stream().map(caliOrderMapper::toOrderDataFromEntity).toList();
		
		// 페이지네이션
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(request.getPage())
				.totalCount((int) pageResult.getTotalElements())
				.build();
		
		// 최종 return
		return TuiGridDTO.ResData.<CaliDTO.OrderRowData>builder()
				.contents(rows)
				.pagination(pagination)
				.build();
	}
	
}
