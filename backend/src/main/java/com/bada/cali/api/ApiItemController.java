package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.ItemDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.projection.ItemFeeHistory;
import com.bada.cali.repository.projection.ItemList;
import com.bada.cali.service.ItemServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Log4j2
@RequestMapping("/api/item")
@AllArgsConstructor
public class ApiItemController {
	
	private final ItemServiceImpl itemService;
	
	// 품목 리스트
	@GetMapping(value = "/getItemList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<ItemList>>> getItemList(
			@ModelAttribute ItemDTO.ListReq req
	) {
		// 리스트 데이터 및 페이지 정보를 담은 데이터 가져옴
		TuiGridDTO.ResData<ItemList> resGridData = itemService.getItemList(req);
		// tui grid api 형식에 맞춘 형태로 리턴
		TuiGridDTO.Res<TuiGridDTO.ResData<ItemList>> body = new TuiGridDTO.Res<>(true, resGridData);
		
		return ResponseEntity.ok(body);
	}
	
	// 개별 품목 정보 가져오기
	@GetMapping("/getItemInfo/{id}")
	public ResponseEntity<ResMessage<ItemDTO.ItemData>> getItemInfo(@PathVariable Long id) {
		log.info("getItemInfo id: {}", id);
		
		ResMessage<ItemDTO.ItemData> resMessage = itemService.getItemInfo(id);
		return ResponseEntity.ok(resMessage);
	}
	
	// 교정수수료 히스토리 세팅
	@GetMapping("/getItemFeeHistory/{itemId}")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<ItemFeeHistory>>> getItemFeeHistory(@PathVariable Long itemId) {
		
		TuiGridDTO.ResData<ItemFeeHistory> rows = itemService.getItemFeeHistory(itemId);
		
		TuiGridDTO.Res<TuiGridDTO.ResData<ItemFeeHistory>> body = new TuiGridDTO.Res<>(true, rows);
		
		return ResponseEntity.ok(body);
	}
}
