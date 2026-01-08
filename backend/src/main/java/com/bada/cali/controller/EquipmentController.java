package com.bada.cali.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
	
}
