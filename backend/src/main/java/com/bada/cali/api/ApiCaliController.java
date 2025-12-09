package com.bada.cali.api;

import com.bada.cali.dto.AgentDTO;
import com.bada.cali.dto.CaliDTO;
import com.bada.cali.dto.TuiGridDTO;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cali")
@Log4j2
@RequiredArgsConstructor
public class ApiCaliController {
	
	// 교정접수 리스트 가져오기
	// NOTE 리턴타입 제네릭 제대로 명시할 것
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<?>>> getOrderList(@ModelAttribute CaliDTO.GetOrderListReq req) {
		
		return null;
	}
}
