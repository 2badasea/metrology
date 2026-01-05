package com.bada.cali.service;

import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.ItemDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.ItemRepository;
import com.bada.cali.repository.projection.ItemList;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Log4j2
@AllArgsConstructor
public class ItemServiceImpl {
	
	private final ItemRepository itemRepository;
	
	// 품목관리 리스트 반환
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<ItemList> getItemList(ItemDTO.ListReq req) {
		
		int pageIndex = req.getPage() - 1;
		int perPage = req.getPerPage();
		
		PageRequest pageRequest = PageRequest.of(pageIndex, perPage);
		
		Long middleItemCodeId = req.getMiddleItemCodeId();
		if (middleItemCodeId != null && middleItemCodeId == 0L) {
			middleItemCodeId = null;
		}
		Long smallItemCodeId = req.getSmallItemCodeId();
		if (smallItemCodeId != null && smallItemCodeId == 0L) {
			smallItemCodeId = null;
		}
		
		YnType isInhousePossible = req.getIsInhousePossible(); // 빈 문자열리 넘어온 경우 DTO에 매핑하는 과정에서 null이 됨
		
		// 넘어오는 데이터 (중분류, 소분류, 당사가능여부, 검색타입, 키워드)
		String searchType = req.getSearchType();
		if (searchType == null || searchType.isEmpty()) {
			searchType = "all";
		}
		String keyword = req.getKeyword();
		keyword = keyword == null ? "" : keyword.trim();
		
		Page<ItemList> pageResult = itemRepository.getItemList(middleItemCodeId, smallItemCodeId, isInhousePossible, searchType, keyword, pageRequest);
		List<ItemList> rows = pageResult.getContent();
		
		// 페이지네이션
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(req.getPage())
				.totalCount((int) pageResult.getTotalElements())
				.build();
		
		// 최종 리턴
		return TuiGridDTO.ResData.<ItemList>builder()
				.contents(rows)
				.pagination(pagination).build() ;
	}
}
