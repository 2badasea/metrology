package com.bada.cali.service;

import com.bada.cali.common.enums.CodeLevel;
import com.bada.cali.dto.ItemCodeDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.ItemCodeRepository;
import com.bada.cali.repository.projection.ItemCodeList;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Log4j2
@AllArgsConstructor
public class ItemCodeServiceImpl {
	
	private final ItemCodeRepository itemCodeRepository;
	
	// 분류코드관리 리스트
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<ItemCodeList> getItemCodeList(ItemCodeDTO.ItemCodeListReq req) {
		// 그리드에서 넘어온 파라미터 (extends)
		int page = req.getPage() -1 ;
		int perPage = req.getPerPage();
		
		// 페이지네이션을 위한 객체
		PageRequest pageRequest = PageRequest.of(page, perPage);
		// codelevel과 parentId로 모두 분류한다.
		Long parentId = req.getParentId();
		if (parentId != null && parentId == 0) {
			parentId = null;
		}
		
		CodeLevel codeLevel = req.getCodeLevel();
		log.info("확인");
		log.info("coeLevel: {}", codeLevel);
		List<ItemCodeList> pageResult = itemCodeRepository.searchItemCodeList(parentId, codeLevel, pageRequest);
		
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
				.page(req.getPage())
				.totalCount(pageResult.size())
				.build();
		
		return TuiGridDTO.ResData.<ItemCodeList>builder()
				.contents(pageResult)
				.pagination(pagination)
				.build();
	}
	
	
}
