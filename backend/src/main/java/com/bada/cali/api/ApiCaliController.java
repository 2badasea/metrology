package com.bada.cali.api;

import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.LogRepository;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.CaliOrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/caliOrder")
@Log4j2
@RequiredArgsConstructor
public class ApiCaliController {
	
	private final CaliOrderServiceImpl caliOrderService;
	private final LogRepository logRepository;
	
	// 교정접수 리스트 가져오기
	// NOTE 리턴타입 제네릭 제대로 명시할 것
	@GetMapping("/getOrderList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<CaliDTO.OrderRowData>>> getOrderList(
			@ModelAttribute CaliDTO.GetOrderListReq req,
			@AuthenticationPrincipal CustomUserDetails user
			) {
		log.info("============ getOrderList");
		// 리스트 데이터 및 페이지 정보를 담은 데이터 가져옴
		TuiGridDTO.ResData<CaliDTO.OrderRowData> resGridData = caliOrderService.getOrderList(req, user);
		log.info("============ 데이터 리턴");
		// tui grid api 형식에 맞춘 형태로 리턴
		TuiGridDTO.Res<TuiGridDTO.ResData<CaliDTO.OrderRowData>> body = new TuiGridDTO.Res<>(true, resGridData);
		
		return ResponseEntity.ok(body);
	}
}
