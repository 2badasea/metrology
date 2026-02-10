package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.ItemDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.projection.ItemFeeHistoryList;
import com.bada.cali.repository.projection.ItemList;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.ItemServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("ApiItemController")
@Log4j2
@RequestMapping("/api/item")
@AllArgsConstructor
public class ItemController {
	
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
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<ItemFeeHistoryList>>> getItemFeeHistory(@PathVariable Long itemId) {
		
		TuiGridDTO.ResData<ItemFeeHistoryList> rows = itemService.getItemFeeHistory(itemId);
		
		TuiGridDTO.Res<TuiGridDTO.ResData<ItemFeeHistoryList>> body = new TuiGridDTO.Res<>(true, rows);
		
		return ResponseEntity.ok(body);
	}
	
	// 품목 등록/수정
	@PostMapping(value = "/saveItem")
	public ResponseEntity<ResMessage<?>> saveItem(
			@RequestBody ItemDTO.SaveItemData saveItemData,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		log.info("품목 등록/수정 호출");
		
		ResMessage<?> resMessage = itemService.saveItem(saveItemData, user);
		
		return ResponseEntity.ok(resMessage);
	}
	
	// 품목 복사
	@PostMapping("/copyItem")
	public ResponseEntity<ResMessage<?>> copyItem(
			@RequestParam Long id,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		ResMessage<?> resMessage = itemService.copyItem(id, user);
		return ResponseEntity.ok(resMessage);
	}
	
	// 품목삭제
	@DeleteMapping("/deleteItem")
	public ResponseEntity<ResMessage<?>> deleteItem(
			@RequestBody List<ItemDTO.DeleteItemData> deleteItem,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		ResMessage<?> resMessage = itemService.deleteItem(deleteItem, user);
		return ResponseEntity.ok(resMessage);
	}
	
}
