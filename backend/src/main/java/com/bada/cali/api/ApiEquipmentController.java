package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.EquipmentDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.projection.EquipmentListPr;
import com.bada.cali.service.EquipmentServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Log4j2
@AllArgsConstructor
@RequestMapping(value = "/api/equipment")
public class ApiEquipmentController {
	
	private final EquipmentServiceImpl equipmentService;
	
	// 표준장비 분야코드 정보 가져오기
	@GetMapping(value = "/getEquipmentField")
	public ResponseEntity<ResMessage<List<EquipmentDTO.EquipFieldData>>> getEquipmentField(@RequestParam(value = "isUse", required = false) YnType isUse) {
		ResMessage<List<EquipmentDTO.EquipFieldData>> resMessage = equipmentService.getEquipmentField(isUse);
		
		return ResponseEntity.ok(resMessage);
	}
	
	// 표준장비관리 리스트 가져오기
	@GetMapping(value = "/getEquipmentList")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<EquipmentListPr>>> getEquipmentList(EquipmentDTO.GetLiseReq getLiseReq) {
		
		// 서비스 계층에서 pagination 정보와 함께 데이터를 가져온다.
		TuiGridDTO.ResData<EquipmentListPr> gridData = equipmentService.getEquipmentList(getLiseReq);
		
		TuiGridDTO.Res<TuiGridDTO.ResData<EquipmentListPr>> body = new TuiGridDTO.Res<>(true, gridData);
		
		return ResponseEntity.ok(body);
	}
}
