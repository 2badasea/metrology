package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.ItemDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.entity.Item;
import com.bada.cali.mapper.ItemMapper;
import com.bada.cali.repository.ItemRepository;
import com.bada.cali.repository.projection.ItemFeeHistory;
import com.bada.cali.repository.projection.ItemList;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Log4j2
@AllArgsConstructor
public class ItemServiceImpl {
	
	private final ItemRepository itemRepository;
	private final ItemMapper itemMapper;
	private final RestClient.Builder builder;
	
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
	
	// 개별 품목정보 반환
	@Transactional(readOnly = true)
	public ResMessage<ItemDTO.ItemData> getItemInfo(Long id) {
		int resCode = 0;
		String resMsg = "";
		
		Item item = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("품목정보가 존재하지 않습니다."));
		resCode = 1;
		// mapstruct를 통해 entity -> record(dto)로 변환
		ItemDTO.ItemData itemData = itemMapper.toRecordFromEntity(item);
		
		return new ResMessage<>(resCode, resMsg, itemData);
	}
	
	// 품목 히스토리 확인
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<ItemFeeHistory> getItemFeeHistory(Long id) {
		
		// 페이징 객체도 필요없이 컨텐츠만 모두 내보낸다.
		List<ItemFeeHistory> itemFeeList = itemRepository.getItemFeeHistory(id);
		
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(0)
				.totalCount(itemFeeList.size())
				.build();
		
		return TuiGridDTO.ResData.<ItemFeeHistory>builder()
				.contents(itemFeeList)
				.pagination(null)
				.build();
		
	}
	
}
