package com.bada.cali.api;

import com.bada.cali.common.ResMessage;
import com.bada.cali.common.enums.YnType;
import com.bada.cali.dto.EquipmentDTO;
import com.bada.cali.dto.TuiGridDTO;
import com.bada.cali.repository.projection.EquipmentListPr;
import com.bada.cali.repository.projection.UsedEquipmentListPr;
import com.bada.cali.security.CustomUserDetails;
import com.bada.cali.service.EquipmentServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("ApiEquipmentController")
@Log4j2
@AllArgsConstructor
@RequestMapping(value = "/api/equipment")
public class EquipmentController {
	
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
	
	// 표준장비 등록/수정하기
	@PostMapping(value = "/saveEquipment")
	public ResponseEntity<ResMessage<?>> saveEquipment(
			@ModelAttribute EquipmentDTO.EquipmentData equipmentData,
			@AuthenticationPrincipal CustomUserDetails user)
	{
		log.info("표준장비 등록/수정요청!!");
		ResMessage<?> resMessage = equipmentService.saveEquipment(equipmentData, user);
		return ResponseEntity.ok(resMessage);
	}
	
	// 표준장비 데이터 가져오기
	@GetMapping(value = "/getEquipmentInfo/{id}")
	public ResponseEntity<ResMessage<EquipmentDTO.GetEquipInfos>> getEquipmentInfo(@PathVariable Long id) {
		log.info("표준장비 데이터 호출");
		
		ResMessage<EquipmentDTO.GetEquipInfos> resMessage = equipmentService.getEquipmentInfo(id);
		
		return ResponseEntity.ok(resMessage);
	}
	
	// 표준장비 삭제
	@DeleteMapping("/deleteEquipment")
	public ResponseEntity<ResMessage<?>> deleteEquipment(
			@RequestBody EquipmentDTO.DeleteEquipmentReq deleteEquipmentReq,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		ResMessage<?> resMessage = equipmentService.deleteEquipment(deleteEquipmentReq, user);
		return ResponseEntity.ok(resMessage);
	}
	
	// 사용중인 표준장비 데이터 가져오기
	@GetMapping("/getUsedEquipment")
	public ResponseEntity<TuiGridDTO.Res<TuiGridDTO.ResData<UsedEquipmentListPr>>> getUsedEquipment(
			@ModelAttribute EquipmentDTO.GetUsedListReq req
	) {
		log.info("사용중인 표준장비 가져오기");
		
		TuiGridDTO.ResData<UsedEquipmentListPr> gridData = equipmentService.getUsedEquipment(req);
		
		TuiGridDTO.Res<TuiGridDTO.ResData<UsedEquipmentListPr>> body = new TuiGridDTO.Res<>(true, gridData);
		
		return ResponseEntity.ok(body);
	}
	
}
