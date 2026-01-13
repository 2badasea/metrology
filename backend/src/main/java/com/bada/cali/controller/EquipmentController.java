package com.bada.cali.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Log4j2
@RequestMapping("/equipment")
public class EquipmentController {
	
	// 표준장비관리
	@GetMapping(value = "/equipmentManage")
	public String equipmentManage(Model model) {
		model.addAttribute("title", "표준장비관리");
		return "equipment/equipmentManage";
	}
	
	// 표준장비 등록/수정 호출 [모달]
	@PostMapping(value = "/equipmentModify")
	public String equipmentModify(Model model) {
		// TODO 장비이력에 대한 기능 추가할 때, 아래 변수 true변경하거나 코드 없애기 (html내 타임리프 표현식도제거)
		model.addAttribute("useHistory", false);
		return "equipment/equipmentModify";
	}
	
	// 표준장비 조회 모달 호출
	@PostMapping(value = "/searchEquipmentList")
	public String searchEquipmentList() {
		return "equipment/searchEquipmentList";
	}
	
}
