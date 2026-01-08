package com.bada.cali.service;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.EquipmentDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.EquipmentFieldRepository;
import com.bada.cali.repository.EquipmentRepository;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.repository.projection.EquipmentListPr;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Log4j2
public class EquipmentServiceImpl {
	
	private final EquipmentRepository equipmentRepository;
	private final EquipmentFieldRepository equipmentFieldRepository;
	private final LogRepository logRepository;
	
	// 표준장비의 분야 데이터를 가져온다
	@Transactional(readOnly = true)
	public ResMessage<List<EquipmentDTO.EquipFieldData>> getEquipmentField(YnType isUse) {
		int resCode = 0;
		String resMsg = "";
		
		YnType isVisible = YnType.y;
		List<EquipmentDTO.EquipFieldData> resData = equipmentFieldRepository.getEquipmentField(isVisible, isUse);
		
		resCode = 1;
		return new ResMessage<>(resCode, resMsg, resData);
	}
	
	// 표준장비관리 리스트 가져오기
	@Transactional(readOnly = true)
	public TuiGridDTO.ResData<EquipmentListPr> getEquipmentList(EquipmentDTO.GetLiseReq req) {
		
		int pageIndex = req.getPage() - 1;
		int perPage = req.getPerPage();
		
		PageRequest pageRequest = PageRequest.of(pageIndex, perPage);
		
		// 검색타입, 검색키워드 및 검색필터 가공
		YnType isVisible = YnType.y;
		YnType isUse = req.getIsUse();		// 선택된 값이 없으면 NULL
		YnType isDispose = req.getIsDispose(); // 선택된 값이 없으면 NULL
		Long equipmentFieldId = req.getEquipmentFieldId();	// 분야 id
		equipmentFieldId = equipmentFieldId == 0 ? null : equipmentFieldId;
		
		String searchType = req.getSearchType();
		if (searchType == null || searchType.isBlank()) {
			searchType = "all";
		}
		String keyword = req.getKeyword();
		keyword = (keyword == null) ? "" : keyword.trim();
		
		Page<EquipmentListPr> pageResult = equipmentRepository.getEquipmentList(
				isVisible, isUse, isDispose, equipmentFieldId, searchType, keyword, pageRequest
		);
		
		TuiGridDTO.Pagination pagination = TuiGridDTO.Pagination.builder()
						.page(req.getPage())
						.totalCount((int) pageResult.getTotalElements())
						.build();
		
		return TuiGridDTO.ResData.<EquipmentListPr>builder()
				.contents(pageResult.getContent())
				.pagination(pagination)
				.build();
	}
}
